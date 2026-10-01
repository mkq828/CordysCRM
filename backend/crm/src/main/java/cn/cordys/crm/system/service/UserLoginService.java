package cn.cordys.crm.system.service;

import cn.cordys.common.constants.InternalRole;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ThirdDetailType;
import cn.cordys.common.dto.RoleDataScopeDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.request.LoginRequest;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.PasswordUtils;
import cn.cordys.common.util.ServletUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.IpRegionService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.LoginType;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.constants.RegisterResultCode;
import cn.cordys.crm.system.domain.*;
import cn.cordys.crm.system.dto.ThirdAuthConfigDTO;
import cn.cordys.crm.platform.constants.PlatformCityManagerStatus;
import cn.cordys.crm.platform.domain.PlatformCityManager;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.crm.system.mapper.ExtRegisterApplicationMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import cn.cordys.security.UserDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户登录服务
 * <p>
 * 提供用户认证、登录、密码验证等相关功能
 * </p>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserLoginService {
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

    @Resource
    private ExtOrganizationMapper extOrganizationMapper;

    @Resource
    private ExtUserMapper extUserMapper;

    @Resource
    private ExtRegisterApplicationMapper extRegisterApplicationMapper;

    @Resource
    private RoleService roleService;

    @Resource
    private BaseMapper<LoginLog> loginLogMapper;

    @Resource
    private IpRegionService ipRegionService;

    @Resource
    private BaseMapper<Department> departmentMapper;

    @Resource
    private PermissionCache permissionCache;

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private TenantPlanService tenantPlanService;

    @Resource
    private BaseMapper<PlatformCityManager> cityManagerMapper;

    /**
     * 用户登录
     *
     * @param request 登录请求
     *
     * @return 会话用户信息
     *
     * @throws AuthenticationException 登录失败时抛出相应异常
     */
    public SessionUser login(LoginRequest request) {
        String username = StringUtils.trim(request.getUsername());
        String password = StringUtils.trim(request.getPassword());

        // 校验图形验证码
        captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());

        // 注册申请被驳回后，登录时直接提示驳回原因（此时无账号，避免误报「账号或密码错误」）
        UserDTO existingUser = extUserMapper.selectByPhoneOrEmail(username);
        if (existingUser == null) {
            String rejectRemark = extRegisterApplicationMapper.selectLatestRejectRemarkByPhone(username);
            if (StringUtils.isNotBlank(rejectRemark)) {
                throw new GenericException(RegisterResultCode.REGISTER_REJECTED, rejectRemark);
            }
        }

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        try {
            subject.login(token);

            if (!subject.isAuthenticated()) {
                throw new GenericException(Translator.get("login_fail"));
            }

            // 登录成功，记录会话用户并添加登录日志
            SessionUser sessionUser = SessionUtils.getUser();
            SessionUtils.putUser(sessionUser);
            recordLoginLog(request);
            // 单点登录：登记当前会话，踢掉旧会话（同一账号只保留一个有效会话）
            SessionUtils.recordLogin(sessionUser.getId(), SessionUtils.getSessionId());

            return sessionUser;
        } catch (ExcessiveAttemptsException e) {
            throw new ExcessiveAttemptsException(Translator.get("password_is_incorrect"));
        } catch (LockedAccountException e) {
            throw new LockedAccountException(Translator.get("password_is_incorrect"));
        } catch (DisabledAccountException e) {
            // 账号被禁用(checkUserStatus)与套餐到期(checkSubscription)都会抛 DisabledAccountException：
            // 前者消息已经是「密码错误」，后者是「服务已到期」。这里不能统一覆盖成「密码错误」，
            // 否则到期账号登录会被误提示成密码错误，用户以为是自己输错了密码。
            throw new DisabledAccountException(e.getMessage());
        } catch (ExpiredCredentialsException e) {
            throw new ExpiredCredentialsException(Translator.get("password_is_incorrect"));
        } catch (AuthenticationException e) {
            throw new AuthenticationException(e.getMessage());
        } catch (UnauthorizedException e) {
            throw new UnauthorizedException(Translator.get("password_is_incorrect") + e.getMessage());
        }
    }

    /**
     * 认证用户并获取用户详细信息
     *
     * @param userKey 用户标识（用户名/手机号/邮箱）
     *
     * @return 用户详细信息
     *
     * @throws AuthenticationException 如果用户不存在或被禁用
     */
    public UserDTO authenticateUser(String userKey) {
        // 获取用户信息
        UserDTO userDTO = Optional.ofNullable(extUserMapper.selectByPhoneOrEmail(userKey))
                .orElseThrow(() -> new AuthenticationException(Translator.get("password_is_incorrect")));

        // 平台员工（admin/city_manager）跳过租户成员禁用检查；城市经理按员工在职状态校验
        if (!isPlatformUser(userDTO.getId())) {
            checkUserStatus(userDTO);
        } else if (hasCityManagerRole(userDTO.getId())) {
            checkCityManagerStatus(userDTO.getId());
        }

        // 获取用户所属组织列表
        Set<String> orgIds = getUserOrganizations(userDTO.getId());

        // 确定当前使用的组织ID
        String organizationId = determineOrganizationId(userDTO, orgIds);

        // 平台员工不检查套餐到期（到期禁止登录仅针对付费租户）
        if (!isPlatformUser(userDTO.getId())) {
            checkSubscription(userDTO, organizationId);
        }

        // 设置用户权限和角色信息
        setupUserPermissions(userDTO, organizationId, orgIds);

        //默认密码检查
        checkDefaultPwd(userDTO);

        return userDTO;
    }

    /**
     * 检查默认密码
     *
     * @param userDTO
     */
    private void checkDefaultPwd(UserDTO userDTO) {
        String defaultPwd = "";
        if (Strings.CI.equals(userDTO.getId(), InternalUser.ADMIN.getValue())) {
            defaultPwd = "Mkq283012";
        } else {
            defaultPwd = PasswordUtils.DEFAULT_PASSWORD;
        }

        if (StringUtils.isNotBlank(defaultPwd) && PasswordUtils.matches(defaultPwd, userDTO.getPassword())) {
            userDTO.setDefaultPwd(true);
        }

    }

    /**
     * 检查用户密码是否正确
     *
     * @param userId   用户ID
     * @param password 密码
     *
     * @return 密码是否正确
     *
     * @throws GenericException 如果用户ID或密码为空
     */
    public boolean checkUserPassword(String userId, String password) {
        if (StringUtils.isBlank(userId)) {
            throw new GenericException(Translator.get("user_name_is_null"));
        }
        if (StringUtils.isBlank(password)) {
            throw new GenericException(Translator.get("password_is_null"));
        }

        String stored = extUserMapper.selectPasswordById(userId);
        if (!PasswordUtils.matches(password, stored)) {
            return false;
        }
        // 历史 MD5 密文 → 惰性升级为 bcrypt
        if (!PasswordUtils.isBcrypt(stored)) {
            extUserMapper.updateUserPassword(PasswordUtils.encode(password), userId);
        }
        return true;
    }

    /**
     * 检查移动端认证配置
     *
     * @param organizationId 组织ID
     *
     * @throws AuthenticationException 如果未配置移动端认证或配置无效
     */
    public void checkMobileAuthConfig(String organizationId) {
        // 如果不是移动端请求，直接返回
        if (!isMobileRequest()) {
            return;
        }

        // 检查组织认证配置
        OrganizationConfig orgConfig = getOrganizationAuthConfig(organizationId);
        if (orgConfig == null) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }

        // 检查企业微信认证配置
        List<OrganizationConfigDetail> enabledConfigs = getEnabledWeComOauthConfigs(orgConfig.getId());
        if (CollectionUtils.isEmpty(enabledConfigs)) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }

        // 验证配置内容
        validateAuthConfig(enabledConfigs.getFirst());
    }

    /**
     * 检查用户状态是否正常
     */
    private void checkUserStatus(UserDTO userDTO) {
        LambdaQueryWrapper<OrganizationUser> queryWrapper = new LambdaQueryWrapper<OrganizationUser>()
                .eq(OrganizationUser::getUserId, userDTO.getId())
                .eq(OrganizationUser::getEnable, true);

        if (StringUtils.isNotBlank(userDTO.getLastOrganizationId())) {
            queryWrapper.eq(OrganizationUser::getOrganizationId, userDTO.getLastOrganizationId());
        }

        List<OrganizationUser> orgUsers = organizationUserMapper.selectListByLambda(queryWrapper);
        if (CollectionUtils.isEmpty(orgUsers)) {
            throw new DisabledAccountException(Translator.get("password_is_incorrect"));
        }

        // 设置用户部门信息
        setUserDepartmentInfo(userDTO, orgUsers.getFirst());
    }

    /**
     * 检查租户套餐是否到期（到期禁止登录，保留数据）
     *
     * @param organizationId 本次登录使用的组织ID
     */
    private void checkSubscription(UserDTO userDTO, String organizationId) {
        if (StringUtils.isBlank(organizationId)) {
            return;
        }
        TenantPlan plan = tenantPlanService.getByOrganizationId(organizationId);
        // 无套餐记录（存量老租户未初始化）放行，避免误伤
        if (plan == null) {
            return;
        }
        if (tenantPlanService.isExpired(plan)) {
            throw new DisabledAccountException(Translator.get("account.expired"));
        }
        // 宽限期内放行登录，仅前端横幅提示
        if (tenantPlanService.isInGrace(plan)) {
            userDTO.setPlanInGrace(true);
            userDTO.setPlanExpireTime(plan.getExpireTime());
        }
    }

    /**
     * 设置用户的部门信息
     */
    private void setUserDepartmentInfo(UserDTO userDTO, OrganizationUser orgUser) {
        userDTO.setDepartmentId(orgUser.getDepartmentId());

        Optional.ofNullable(departmentMapper.selectByPrimaryKey(orgUser.getDepartmentId()))
                .ifPresent(department -> userDTO.setDepartmentName(department.getName()));
    }

    /**
     * 确定用户当前使用的组织ID
     */
    private String determineOrganizationId(UserDTO userDTO, Set<String> orgIds) {
        String orgId = OrganizationContext.getOrganizationId();

        if (StringUtils.isBlank(orgId) && CollectionUtils.isNotEmpty(orgIds)) {
            // 上下文中无组织ID时，优先使用用户最后访问的组织，否则取第一个可用组织
            return orgIds.contains(userDTO.getLastOrganizationId())
                    ? userDTO.getLastOrganizationId()
                    : orgIds.iterator().next();
        }

        return orgId;
    }

    /**
     * 设置用户的权限和角色信息
     */
    private void setupUserPermissions(UserDTO userDTO, String organizationId, Set<String> orgIds) {
        // 设置用户角色
        List<RoleDataScopeDTO> roleOptions = roleService.getRoleOptions(userDTO.getId(), organizationId);
        userDTO.setRoles(roleOptions);

        // 更新最后登录的组织ID
        userDTO.setLastOrganizationId(organizationId);

        // 设置用户权限
        userDTO.setPermissionIds(permissionCache.getPermissionIds(userDTO.getId(), organizationId));

        // 设置用户所属的所有组织
        userDTO.setOrganizationIds(orgIds);
    }

    /**
     * 获取用户所属的所有组织ID
     */
    private Set<String> getUserOrganizations(String userId) {
        // 管理员可以访问所有组织
        if (isAdminUser(userId)) {
            return extOrganizationMapper.selectAllOrganizationIds();
        }

        // 城市经理是平台员工，不归属租户组织，挂默认组织避免 OrganizationContext 抛 FORBIDDEN
        if (hasCityManagerRole(userId)) {
            return Set.of(OrganizationContext.DEFAULT_ORGANIZATION_ID);
        }

        // 普通用户只能访问已授权且启用的组织
        return organizationUserMapper.select(createOrgUserExample(userId)).stream()
                .map(OrganizationUser::getOrganizationId)
                .collect(Collectors.toSet());
    }

    /**
     * 创建组织用户查询条件
     */
    private OrganizationUser createOrgUserExample(String userId) {
        OrganizationUser example = new OrganizationUser();
        example.setUserId(userId);
        example.setEnable(true);
        return example;
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(LoginRequest request) {
        LoginLog log = new LoginLog();
        log.setId(IDGenerator.nextStr());
        log.setLoginAddress(request.getLoginAddress());
        log.setLoginCity(ipRegionService.searchCity(request.getLoginAddress()));
        log.setOperator(SessionUtils.getUserId());
        log.setCreateTime(System.currentTimeMillis());
        log.setPlatform(determinePlatform());

        loginLogMapper.insert(log);
    }

    /**
     * 获取组织认证配置
     */
    private OrganizationConfig getOrganizationAuthConfig(String organizationId) {
        return extOrganizationConfigMapper.getOrganizationConfig(
                organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
    }

    /**
     * 获取启用的企业微信OAuth配置
     */
    private List<OrganizationConfigDetail> getEnabledWeComOauthConfigs(String configId) {
        return extOrganizationConfigDetailMapper
                .getEnableOrganizationConfigDetails(configId, List.of(ThirdDetailType.WECOM_SYNC.name(), ThirdDetailType.DINGTALK_SYNC.name(), ThirdDetailType.LARK_SYNC.name()));
    }

    /**
     * 验证认证配置是否有效
     */
    private void validateAuthConfig(OrganizationConfigDetail configDetail) {
        String content = new String(configDetail.getContent(), StandardCharsets.UTF_8);
        ThirdAuthConfigDTO authConfig = JSON.parseObject(content, ThirdAuthConfigDTO.class);

        if (authConfig == null) {
            throw new AuthenticationException(Translator.get("auth.setting.no.exists"));
        }
    }

    /**
     * 根据User-Agent确定登录平台类型
     */
    private String determinePlatform() {
        return isMobileRequest() ? LoginType.MOBILE.getName() : LoginType.WEB.getName();
    }

    /**
     * 判断是否为移动端请求
     */
    private boolean isMobileRequest() {
        String userAgent = ServletUtils.getUserAgent();
        return StringUtils.isNotBlank(userAgent) && isMobileUserAgent(userAgent);
    }

    /**
     * 判断是否为管理员用户
     */
    private boolean isAdminUser(String userId) {
        return Strings.CS.equals(userId, InternalUser.ADMIN.getValue());
    }

    /**
     * 判断是否为平台员工（admin 或城市经理）
     */
    private boolean isPlatformUser(String userId) {
        return isAdminUser(userId) || hasCityManagerRole(userId);
    }

    /**
     * 判断用户是否拥有城市经理角色
     */
    private boolean hasCityManagerRole(String userId) {
        return roleService.getRoleIdsByUserId(userId).contains(InternalRole.CITY_MANAGER.getValue());
    }

    /**
     * 校验城市经理账号在职状态（离职禁用则禁止登录）
     */
    private void checkCityManagerStatus(String userId) {
        PlatformCityManager manager = cityManagerMapper.selectByPrimaryKey(userId);
        if (manager != null && PlatformCityManagerStatus.DISABLED.name().equals(manager.getStatus())) {
            throw new DisabledAccountException(Translator.get("password_is_incorrect"));
        }
    }

    /**
     * 判断是否为移动端User-Agent
     */
    private boolean isMobileUserAgent(String userAgent) {
        return userAgent.contains("miniprogram") ||
                userAgent.contains("MicroMessenger") ||
                userAgent.contains("Android") ||
                userAgent.contains("iOS") ||
                userAgent.contains("Mobile") ||
                userAgent.contains("MQQBrowser") ||
                userAgent.contains("Mobile Safari") ||
                userAgent.contains("iPhone") ||
                userAgent.contains("iPad") ||
                userAgent.contains("ipod");
    }
}