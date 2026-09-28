package cn.cordys.crm.platform.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.constants.InternalRole;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.PasswordUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.platform.constants.PlatformCityManagerStatus;
import cn.cordys.crm.platform.domain.PlatformCityManager;
import cn.cordys.crm.platform.dto.request.CityManagerAddRequest;
import cn.cordys.crm.platform.dto.request.CityManagerAssignRequest;
import cn.cordys.crm.platform.dto.request.CityManagerOrgEditRequest;
import cn.cordys.crm.platform.dto.request.CityManagerPageRequest;
import cn.cordys.crm.platform.dto.request.CityManagerReassignRequest;
import cn.cordys.crm.platform.dto.response.CityManagerOrgResponse;
import cn.cordys.crm.platform.dto.response.CityManagerResponse;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.domain.UserRole;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 城市经理服务：账号 CRUD + 租户归属分配 + 离职禁用/二次分配（业绩口径以租户表 sign/follow_manager_id 为准）
 */
@Service
public class CityManagerService {

    @Resource
    private BaseMapper<PlatformCityManager> cityManagerMapper;
    @Resource
    private BaseMapper<Organization> organizationMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<UserRole> userRoleMapper;

    /**
     * 新增城市经理（三写：sys_user + sys_user_role + platform_city_manager）
     */
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.CITY_MANAGER, type = LogType.ADD, resourceName = "城市经理")
    public void add(CityManagerAddRequest request, String operatorId) {
        String phone = StringUtils.trim(request.getPhone());
        // 手机号是登录标识，需唯一，避免登录歧义
        List<User> exist = DataAccessLayer.with(User.class).selectListByLambda(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (CollectionUtils.isNotEmpty(exist)) {
            throw new GenericException(Translator.get("phone.exist"));
        }

        long now = System.currentTimeMillis();
        String userId = IDGenerator.nextStr();

        User user = new User();
        user.setId(userId);
        user.setName(StringUtils.trim(request.getName()));
        user.setPhone(phone);
        user.setPassword(PasswordUtils.encode(StringUtils.isBlank(request.getPassword())
                ? PasswordUtils.DEFAULT_PASSWORD : request.getPassword()));
        user.setGender(false);
        user.setLanguage(Locale.SIMPLIFIED_CHINESE.toString());
        user.setLastOrganizationId(OrganizationContext.DEFAULT_ORGANIZATION_ID);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setCreateUser(operatorId);
        user.setUpdateUser(operatorId);
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setId(IDGenerator.nextStr());
        userRole.setUserId(userId);
        userRole.setRoleId(InternalRole.CITY_MANAGER.getValue());
        userRole.setCreateTime(now);
        userRole.setUpdateTime(now);
        userRole.setCreateUser(operatorId);
        userRole.setUpdateUser(operatorId);
        userRoleMapper.insert(userRole);

        PlatformCityManager manager = new PlatformCityManager();
        manager.setId(userId);
        manager.setName(StringUtils.trim(request.getName()));
        manager.setPhone(phone);
        manager.setStatus(PlatformCityManagerStatus.ENABLED.name());
        manager.setCreateTime(now);
        manager.setUpdateTime(now);
        manager.setCreateUser(operatorId);
        manager.setUpdateUser(operatorId);
        cityManagerMapper.insert(manager);
    }

    /**
     * 分页查询（内存分页 + 名下签约/跟进客户数）
     */
    public Pager<List<CityManagerResponse>> pageList(CityManagerPageRequest request) {
        List<PlatformCityManager> all = cityManagerMapper.selectListByLambda(
                new LambdaQueryWrapper<PlatformCityManager>().orderByDesc(PlatformCityManager::getCreateTime));

        // 按租户表统计每个经理名下的签约/跟进客户数（业绩口径来源）
        List<Organization> orgs = DataAccessLayer.with(Organization.class).selectListByLambda(new LambdaQueryWrapper<Organization>());
        Map<String, Long> signedCount = orgs.stream()
                .filter(o -> StringUtils.isNotBlank(o.getSignManagerId()))
                .collect(Collectors.groupingBy(Organization::getSignManagerId, Collectors.counting()));
        Map<String, Long> followCount = orgs.stream()
                .filter(o -> StringUtils.isNotBlank(o.getFollowManagerId()))
                .collect(Collectors.groupingBy(Organization::getFollowManagerId, Collectors.counting()));

        List<PlatformCityManager> filtered = all.stream()
                .filter(c -> StringUtils.isBlank(request.getStatus()) || request.getStatus().equals(c.getStatus()))
                .filter(c -> StringUtils.isBlank(request.getKeyword())
                        || (c.getName() != null && c.getName().contains(request.getKeyword()))
                        || (c.getPhone() != null && c.getPhone().contains(request.getKeyword())))
                .toList();

        Pager<List<PlatformCityManager>> paged = paginate(filtered, request.getCurrent(), request.getPageSize());
        List<CityManagerResponse> responses = paged.getList().stream().map(c -> {
            CityManagerResponse response = new CityManagerResponse();
            response.setId(c.getId());
            response.setName(c.getName());
            response.setPhone(c.getPhone());
            response.setStatus(c.getStatus());
            response.setSignedCount(signedCount.getOrDefault(c.getId(), 0L));
            response.setFollowCount(followCount.getOrDefault(c.getId(), 0L));
            response.setCreateTime(c.getCreateTime());
            return response;
        }).toList();

        Pager<List<CityManagerResponse>> result = new Pager<>();
        result.setList(responses);
        result.setTotal(paged.getTotal());
        result.setPageSize(paged.getPageSize());
        result.setCurrent(paged.getCurrent());
        return result;
    }

    /**
     * 禁用（离职）：置 DISABLED + 踢下线
     */
    @OperationLog(module = LogModule.CITY_MANAGER, type = LogType.UPDATE, resourceName = "城市经理离职禁用", resourceId = "{#id}")
    public void disable(String id, String operatorId) {
        PlatformCityManager manager = cityManagerMapper.selectByPrimaryKey(id);
        if (manager == null) {
            throw new GenericException(Translator.get("city.manager.not.exist"));
        }
        manager.setStatus(PlatformCityManagerStatus.DISABLED.name());
        manager.setUpdateTime(System.currentTimeMillis());
        manager.setUpdateUser(operatorId);
        cityManagerMapper.updateById(manager);
        // 踢下线，阻止其继续使用平台接口
        SessionUtils.kickOutUser(id);
    }

    /**
     * 分配租户归属（签约/跟进经理）
     */
    @OperationLog(module = LogModule.CITY_MANAGER, type = LogType.UPDATE, resourceName = "城市经理租户归属分配", resourceId = "{#request.organizationId}")
    public void assignOrg(CityManagerAssignRequest request, String operatorId) {
        Organization org = organizationMapper.selectByPrimaryKey(request.getOrganizationId());
        if (org == null) {
            throw new GenericException(Translator.get("organization.not.exist"));
        }
        org.setSignManagerId(request.getSignManagerId());
        org.setFollowManagerId(request.getFollowManagerId());
        org.setUpdateTime(System.currentTimeMillis());
        org.setUpdateUser(operatorId);
        organizationMapper.updateById(org);
    }

    /**
     * 离职二次分配：只改跟进经理 follow_manager_id，不改签约经理 sign_manager_id（业绩口径定死）
     */
    @OperationLog(module = LogModule.CITY_MANAGER, type = LogType.UPDATE, resourceName = "城市经理二次分配")
    public void reassign(CityManagerReassignRequest request, String operatorId) {
        String toManagerId = request.getToManagerId();
        PlatformCityManager target = cityManagerMapper.selectByPrimaryKey(toManagerId);
        if (target == null || PlatformCityManagerStatus.DISABLED.name().equals(target.getStatus())) {
            throw new GenericException(Translator.get("city.manager.not.available"));
        }

        List<String> orgIds = request.getOrganizationIds();
        if (CollectionUtils.isEmpty(orgIds)) {
            return;
        }
        long now = System.currentTimeMillis();
        List<Organization> orgs = DataAccessLayer.with(Organization.class).selectByIds(orgIds);
        for (Organization org : orgs) {
            org.setFollowManagerId(toManagerId);
            org.setUpdateTime(now);
            org.setUpdateUser(operatorId);
            organizationMapper.updateById(org);
        }
    }

    /**
     * 城市经理编辑本人归属租户基本信息（不含归属经理/演示标记，归属仅 admin 可改）
     */
    @OperationLog(module = LogModule.CITY_MANAGER, type = LogType.UPDATE, resourceName = "城市经理编辑租户资料", resourceId = "{#request.organizationId}")
    public void editMyOrg(CityManagerOrgEditRequest request, String currentUserId) {
        Organization org = organizationMapper.selectByPrimaryKey(request.getOrganizationId());
        if (org == null) {
            throw new GenericException(Translator.get("organization.not.exist"));
        }
        boolean owned = currentUserId.equals(org.getSignManagerId()) || currentUserId.equals(org.getFollowManagerId());
        if (!owned) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        org.setName(StringUtils.trim(request.getName()));
        org.setUnifiedSocialCreditCode(request.getUnifiedSocialCreditCode());
        org.setLegalPersonName(request.getLegalPersonName());
        org.setBusinessLicenseAttachmentId(request.getBusinessLicenseAttachmentId());
        org.setUpdateTime(System.currentTimeMillis());
        org.setUpdateUser(currentUserId);
        organizationMapper.updateById(org);
    }

    /**
     * 城市经理本人名下租户（签约或跟进）
     */
    public List<CityManagerOrgResponse> myOrgs(String currentUserId) {
        return DataAccessLayer.with(Organization.class).selectListByLambda(
                        new LambdaQueryWrapper<Organization>().orderByDesc(Organization::getCreateTime)).stream()
                .filter(o -> currentUserId.equals(o.getSignManagerId()) || currentUserId.equals(o.getFollowManagerId()))
                .map(this::toOrgResponse)
                .toList();
    }

    /**
     * 平台侧租户下拉（admin 分配用，排除默认组织）
     */
    public List<CityManagerOrgResponse> orgOptions() {
        return DataAccessLayer.with(Organization.class).selectListByLambda(
                        new LambdaQueryWrapper<Organization>().orderByDesc(Organization::getCreateTime)).stream()
                .filter(o -> !OrganizationContext.DEFAULT_ORGANIZATION_ID.equals(o.getId()))
                .map(this::toOrgResponse)
                .toList();
    }

    private CityManagerOrgResponse toOrgResponse(Organization o) {
        CityManagerOrgResponse response = new CityManagerOrgResponse();
        response.setId(o.getId());
        response.setName(o.getName());
        response.setOrgType(o.getOrgType());
        response.setSignManagerId(o.getSignManagerId());
        response.setFollowManagerId(o.getFollowManagerId());
        return response;
    }

    private <T> Pager<List<T>> paginate(List<T> list, int current, int pageSize) {
        int page = current <= 0 ? 1 : current;
        int size = pageSize <= 0 ? 20 : pageSize;
        int total = list.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        Pager<List<T>> pager = new Pager<>();
        pager.setList(list.subList(from, to));
        pager.setTotal(total);
        pager.setPageSize(size);
        pager.setCurrent(page);
        return pager;
    }
}
