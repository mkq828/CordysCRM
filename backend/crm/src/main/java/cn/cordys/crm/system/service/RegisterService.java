package cn.cordys.crm.system.service;

import cn.cordys.common.constants.InternalRole;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ThirdConfigTypeConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.EncryptUtils;
import cn.cordys.common.util.PasswordUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.RegisterResultCode;
import cn.cordys.crm.system.constants.RegisterType;
import cn.cordys.crm.system.constants.RegisterVerifyStatus;
import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.RegisterApplication;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.domain.UserRole;
import cn.cordys.crm.system.dto.request.RegisterApplicationPageRequest;
import cn.cordys.crm.system.dto.request.RegisterApplyRequest;
import cn.cordys.crm.system.dto.request.RegisterApproveRequest;
import cn.cordys.crm.system.dto.request.RegisterRejectRequest;
import cn.cordys.crm.system.dto.request.RegisterToggleRequest;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.dto.response.RegisterApplicationResponse;
import cn.cordys.crm.system.dto.response.RegisterStatusResponse;
import cn.cordys.crm.system.mapper.ExtRegisterApplicationMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.mapper.OrganizationMapper;
import cn.cordys.crm.system.mapper.RegisterApplicationMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 自助注册服务
 * <p>
 * 负责注册申请提交、审核、以及审核通过后的租户开通。
 * </p>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RegisterService {

    @Resource
    private RegisterApplicationMapper registerApplicationMapper;

    @Resource
    private ExtRegisterApplicationMapper extRegisterApplicationMapper;

    @Resource
    private ExtUserMapper extUserMapper;

    @Resource
    private OrganizationMapper organizationMapper;

    @Resource
    private BaseMapper<User> userMapper;

    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

    @Resource
    private BaseMapper<UserRole> userRoleMapper;

    @Resource
    private BaseMapper<Department> departmentMapper;

    @Resource
    private AttachmentService attachmentService;

    @Resource
    private ModuleService moduleService;

    @Resource
    private NavigationService navigationService;

    @Resource
    private ModuleFormMigrationService moduleFormMigrationService;

    @Resource
    private TenantConfigService tenantConfigService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private TenantPlanService tenantPlanService;

    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    /**
     * 提交注册申请
     * <p>
     * 个人注册免审：提交即开通账号；企业注册仍需人工审核。
     * </p>
     */
    public RegisterStatusResponse apply(RegisterApplyRequest request) {
        // 校验图形验证码
        captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());

        String type = StringUtils.trim(request.getType());
        if (!RegisterType.isValid(type)) {
            throw new GenericException(RegisterResultCode.REGISTER_TYPE_INVALID);
        }
        boolean enterprise = RegisterType.isEnterprise(type);
        String phone = StringUtils.trim(request.getPhone());
        String idCard = StringUtils.trim(request.getIdCard());

        // 手机号全局唯一
        if (extUserMapper.countByPhone(phone, null) > 0
                || extRegisterApplicationMapper.countByPhoneInApplication(phone) > 0) {
            throw new GenericException(RegisterResultCode.PHONE_EXIST);
        }

        String idCardHash = CodingUtils.sha256Hex(idCard);

        String creditCode = null;
        String legalPersonName = null;
        String licenseAttachmentId = null;
        if (enterprise) {
            creditCode = StringUtils.trim(request.getUnifiedSocialCreditCode());
            legalPersonName = StringUtils.trim(request.getLegalPersonName());
            licenseAttachmentId = StringUtils.trim(request.getBusinessLicenseAttachmentId());
            if (StringUtils.isBlank(creditCode) || StringUtils.isBlank(legalPersonName) || StringUtils.isBlank(licenseAttachmentId)) {
                throw new GenericException(RegisterResultCode.LICENSE_REQUIRED);
            }
            // 企业统一社会信用代码唯一
            if (extRegisterApplicationMapper.countByCreditCodeInOrganization(creditCode) > 0
                    || extRegisterApplicationMapper.countByCreditCodeInApplication(creditCode) > 0) {
                throw new GenericException(RegisterResultCode.CREDIT_CODE_EXIST);
            }
        } else {
            // 个人身份证唯一
            if (extRegisterApplicationMapper.countByIdCardHashInUser(idCardHash) > 0
                    || extRegisterApplicationMapper.countByIdCardHashInApplication(idCardHash) > 0) {
                throw new GenericException(RegisterResultCode.ID_CARD_EXIST);
            }
        }

        long now = System.currentTimeMillis();
        RegisterApplication application = new RegisterApplication();
        application.setId(IDGenerator.nextStr());
        application.setType(type);
        application.setName(StringUtils.trim(request.getName()));
        application.setPhone(phone);
        application.setPassword(PasswordUtils.encode(request.getPassword()));
        application.setIdCard(EncryptUtils.aesEncrypt(idCard));
        application.setIdCardHash(idCardHash);
        application.setUnifiedSocialCreditCode(creditCode);
        application.setLegalPersonName(legalPersonName);
        application.setBusinessLicenseAttachmentId(licenseAttachmentId);
        application.setVerifyStatus(RegisterVerifyStatus.PENDING.getValue());
        application.setCreateTime(now);
        application.setUpdateTime(now);
        application.setCreateUser(phone);
        application.setUpdateUser(phone);
        registerApplicationMapper.insert(application);

        // 企业注册需人工审核：站内信通知平台管理员及时处理（避免企业用户流失）
        if (enterprise) {
            commonNoticeSendService.sendNotice(
                    NotificationConstants.Module.SYSTEM,
                    NotificationConstants.Event.ENTERPRISE_REGISTER_APPLY,
                    Map.of("name", application.getName()),
                    InternalUser.ADMIN.getValue(),
                    OrganizationContext.DEFAULT_ORGANIZATION_ID,
                    List.of(InternalUser.ADMIN.getValue()),
                    false);
        }

        // 个人注册免审：提交即开通账号
        if (!enterprise) {
            openAccount(application, phone);
            application.setVerifyStatus(RegisterVerifyStatus.APPROVED.getValue());
            application.setVerifyUser(phone);
            application.setVerifyTime(now);
            application.setUpdateTime(now);
            application.setUpdateUser(phone);
            registerApplicationMapper.updateById(application);
        }

        RegisterStatusResponse response = new RegisterStatusResponse();
        response.setType(type);
        response.setVerifyStatus(application.getVerifyStatus());
        return response;
    }

    /**
     * 查询注册审核状态（公开）
     */
    public RegisterStatusResponse status(String phone) {
        RegisterApplication example = new RegisterApplication();
        example.setPhone(StringUtils.trim(phone));
        List<RegisterApplication> applications = registerApplicationMapper.select(example);
        RegisterApplication latest = applications.stream()
                .max(Comparator.comparing(RegisterApplication::getCreateTime))
                .orElse(null);
        if (latest == null) {
            return null;
        }
        RegisterStatusResponse response = new RegisterStatusResponse();
        response.setType(latest.getType());
        response.setVerifyStatus(latest.getVerifyStatus());
        response.setVerifyRemark(latest.getVerifyRemark());
        return response;
    }

    /**
     * 分页查询申请单
     */
    public List<RegisterApplicationResponse> pageList(RegisterApplicationPageRequest request) {
        // 累计使用天数由 SQL 子查询按 sys_login_log 去重日期统计（实际登录天数），见 ExtRegisterApplicationMapper.pageList
        return extRegisterApplicationMapper.pageList(request);
    }

    /**
     * 企业注册待审核数量（管理端首页待办）
     */
    public long countPendingEnterprise() {
        return extRegisterApplicationMapper.countPendingEnterprise();
    }

    /**
     * 申请单详情（身份证脱敏）
     */
    public RegisterApplicationResponse detail(String id) {
        RegisterApplication application = registerApplicationMapper.selectByPrimaryKey(id);
        if (application == null) {
            throw new GenericException(RegisterResultCode.APPLICATION_NOT_FOUND);
        }
        RegisterApplicationResponse response = BeanUtils.copyBean(new RegisterApplicationResponse(), application);
        response.setIdCard(maskIdCard(EncryptUtils.aesDecrypt(application.getIdCard())));
        return response;
    }

    /**
     * 审核通过：开通租户账号并将申请单置为通过
     */
    public void approve(RegisterApproveRequest request, String operatorId) {
        RegisterApplication application = registerApplicationMapper.selectByPrimaryKey(request.getId());
        if (application == null) {
            throw new GenericException(RegisterResultCode.APPLICATION_NOT_FOUND);
        }
        if (!RegisterVerifyStatus.PENDING.getValue().equals(application.getVerifyStatus())) {
            throw new GenericException(RegisterResultCode.ALREADY_PROCESSED);
        }

        openAccount(application, operatorId);

        long now = System.currentTimeMillis();
        application.setVerifyStatus(RegisterVerifyStatus.APPROVED.getValue());
        application.setVerifyUser(operatorId);
        application.setVerifyTime(now);
        application.setUpdateTime(now);
        application.setUpdateUser(operatorId);
        registerApplicationMapper.updateById(application);
    }

    /**
     * 开通租户账号：创建组织、根部门、管理员用户、组织成员，按注册类型挂角色，并初始化菜单与顶部导航
     */
    private void openAccount(RegisterApplication application, String operatorId) {
        boolean enterprise = RegisterType.isEnterprise(application.getType());
        String orgId = IDGenerator.nextStr();
        String userId = IDGenerator.nextStr();
        long now = System.currentTimeMillis();

        // 1. 创建组织（租户）
        Organization organization = new Organization();
        organization.setId(orgId);
        organization.setName(application.getName());
        organization.setOrgType(application.getType());
        organization.setCreateTime(now);
        organization.setUpdateTime(now);
        organization.setCreateUser(operatorId);
        organization.setUpdateUser(operatorId);
        if (enterprise) {
            organization.setUnifiedSocialCreditCode(application.getUnifiedSocialCreditCode());
            organization.setLegalPersonName(application.getLegalPersonName());
            organization.setLegalPersonIdCard(application.getIdCard());
            organization.setLegalPersonIdCardHash(application.getIdCardHash());
            organization.setBusinessLicenseAttachmentId(application.getBusinessLicenseAttachmentId());
        }
        organizationMapper.insert(organization);

        // 2. 创建根部门
        Department department = new Department();
        department.setId(IDGenerator.nextStr());
        department.setName(organization.getName());
        department.setOrganizationId(orgId);
        department.setParentId("NONE");
        department.setPos(1L);
        department.setResource(ThirdConfigTypeConstants.INTERNAL.name());
        department.setCreateTime(now);
        department.setUpdateTime(now);
        department.setCreateUser(operatorId);
        department.setUpdateUser(operatorId);
        departmentMapper.insert(department);

        // 3. 创建管理员用户
        User user = new User();
        user.setId(userId);
        user.setName(enterprise ? application.getLegalPersonName() : application.getName());
        user.setPhone(application.getPhone());
        user.setPassword(application.getPassword());
        user.setGender(false);
        user.setLanguage(Locale.SIMPLIFIED_CHINESE.toString());
        user.setLastOrganizationId(orgId);
        if (!enterprise) {
            user.setIdCard(application.getIdCard());
            user.setIdCardHash(application.getIdCardHash());
        }
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setCreateUser(operatorId);
        user.setUpdateUser(operatorId);
        userMapper.insert(user);
        // 记录开通的用户ID，管理端用于展示使用天数/最后登录时间/账号启停
        application.setUserId(userId);

        // 4. 创建组织成员
        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setId(IDGenerator.nextStr());
        organizationUser.setOrganizationId(orgId);
        organizationUser.setUserId(userId);
        organizationUser.setDepartmentId(department.getId());
        organizationUser.setEnable(true);
        organizationUser.setCreateTime(now);
        organizationUser.setUpdateTime(now);
        organizationUser.setCreateUser(operatorId);
        organizationUser.setUpdateUser(operatorId);
        organizationUserMapper.insert(organizationUser);

        // 5. 按注册类型直接挂载全局内置角色：企业 → 企业管理员（org_admin，全企业数据）；个人 → 销售专员（sales_staff，仅本人数据）。
        //    直接引用全局内置角色，由平台 admin 在角色列表中统一维护权限，后续新增功能只需给这两个角色勾选即可联动生效。
        if (enterprise) {
            insertUserRole(userId, InternalRole.ORG_ADMIN.getValue(), operatorId, now);
        } else {
            insertUserRole(userId, InternalRole.SALES_STAFF.getValue(), operatorId, now);
        }

        // 6. 转正营业执照附件（绑定到新组织）
        if (enterprise && StringUtils.isNotBlank(application.getBusinessLicenseAttachmentId())) {
            attachmentService.processTemp(new UploadTransferRequest(
                    orgId, orgId, operatorId, List.of(application.getBusinessLicenseAttachmentId())));
        }

        // 7. 初始化菜单、顶部导航与表单配置（否则新租户为空菜单、无表单）
        moduleService.initModule(orgId);
        navigationService.initNavigation(orgId);
        moduleFormMigrationService.initForm(orgId);
        // 8. 播种阶段配置（否则新建商机/合同/订单会因无阶段配置报错）
        tenantConfigService.initStageConfigs(orgId);
        // 9. 初始化免费试用套餐（付费用户管理：记录套餐版本与到期时间）
        tenantPlanService.initFreeTrial(orgId, operatorId);
    }

    /**
     * 插入用户角色关联
     */
    private void insertUserRole(String userId, String roleId, String operatorId, long now) {
        UserRole userRole = new UserRole();
        userRole.setId(IDGenerator.nextStr());
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreateTime(now);
        userRole.setUpdateTime(now);
        userRole.setCreateUser(operatorId);
        userRole.setUpdateUser(operatorId);
        userRoleMapper.insert(userRole);
    }

    /**
     * 审核驳回
     */
    public void reject(RegisterRejectRequest request, String operatorId) {
        RegisterApplication application = registerApplicationMapper.selectByPrimaryKey(request.getId());
        if (application == null) {
            throw new GenericException(RegisterResultCode.APPLICATION_NOT_FOUND);
        }
        if (!RegisterVerifyStatus.PENDING.getValue().equals(application.getVerifyStatus())) {
            throw new GenericException(RegisterResultCode.ALREADY_PROCESSED);
        }
        long now = System.currentTimeMillis();
        application.setVerifyStatus(RegisterVerifyStatus.REJECTED.getValue());
        application.setVerifyRemark(StringUtils.trim(request.getRemark()));
        application.setVerifyUser(operatorId);
        application.setVerifyTime(now);
        application.setUpdateTime(now);
        application.setUpdateUser(operatorId);
        registerApplicationMapper.updateById(application);
    }

    /**
     * 启用/禁用账号：通过控制 sys_organization_user.enable 实现登录拦截（见 UserLoginService.checkUserStatus）。
     */
    public void toggle(RegisterToggleRequest request, String operatorId) {
        RegisterApplication application = registerApplicationMapper.selectByPrimaryKey(request.getId());
        if (application == null) {
            throw new GenericException(RegisterResultCode.APPLICATION_NOT_FOUND);
        }
        if (StringUtils.isBlank(application.getUserId())) {
            throw new GenericException(RegisterResultCode.ACCOUNT_NOT_OPENED);
        }
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<OrganizationUser> queryWrapper = new LambdaQueryWrapper<OrganizationUser>()
                .eq(OrganizationUser::getUserId, application.getUserId());
        List<OrganizationUser> orgUsers = organizationUserMapper.selectListByLambda(queryWrapper);
        for (OrganizationUser orgUser : orgUsers) {
            orgUser.setEnable(request.getEnabled());
            orgUser.setUpdateTime(now);
            orgUser.setUpdateUser(operatorId);
            organizationUserMapper.updateById(orgUser);
        }
    }

    /**
     * 身份证号脱敏：保留前4后4，中间打码
     */
    private String maskIdCard(String idCard) {
        if (StringUtils.isBlank(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}
