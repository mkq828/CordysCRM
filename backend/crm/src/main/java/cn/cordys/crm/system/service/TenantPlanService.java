package cn.cordys.crm.system.service;

import cn.cordys.common.constants.InternalRole;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.platform.util.PlatformManagerNames;
import cn.cordys.crm.system.constants.TenantPlanStatus;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.domain.SysEdition;
import cn.cordys.crm.system.domain.TenantEdition;
import cn.cordys.crm.system.domain.TenantPlan;
import cn.cordys.crm.system.domain.TenantPlanHistory;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.domain.UserRole;
import cn.cordys.crm.system.dto.request.TenantPlanConfigRequest;
import cn.cordys.crm.system.dto.request.TenantPlanDetailRequest;
import cn.cordys.crm.system.dto.request.TenantPlanOpenRequest;
import cn.cordys.crm.system.dto.request.TenantPlanPageRequest;
import cn.cordys.crm.system.dto.request.TenantPlanToggleRequest;
import cn.cordys.crm.system.dto.request.TenantPlanUpgradeRequest;
import cn.cordys.crm.system.dto.response.TenantPlanConfigResponse;
import cn.cordys.crm.system.dto.response.TenantPlanDetailResponse;
import cn.cordys.crm.system.dto.response.TenantPlanHistoryResponse;
import cn.cordys.crm.system.dto.response.TenantPlanQuoteResponse;
import cn.cordys.crm.system.dto.response.TenantPlanResponse;
import cn.cordys.crm.system.mapper.ExtTenantPlanMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 租户套餐服务（付费用户管理）
 * <p>
 * 负责试用期初始化、套餐开通/续费、到期判断与全局配置。
 * 版本从写死的注册类型枚举改为读 {@link SysEdition}（可配置），开通时写入版本快照。
 * </p>
 */
@Service
public class TenantPlanService {

    private static final String PARAM_FREE_TRIAL_DAYS = "register.freeTrialDays";
    private static final String PARAM_EXPIRE_REMIND_DAYS = "register.expireRemindDays";
    private static final String PARAM_GRACE_DAYS = "register.graceDays";
    private static final int DEFAULT_FREE_TRIAL_DAYS = 180;
    private static final String DEFAULT_EXPIRE_REMIND_DAYS = "30,7";
    private static final int DEFAULT_GRACE_DAYS = 3;
    private static final int DEFAULT_VALIDITY_DAYS = 365;
    private static final String DEFAULT_TRIAL_EDITION = "BASIC";
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final String ACTION_OPEN = "OPEN";
    private static final String ACTION_UPGRADE = "UPGRADE";

    @Resource
    private BaseMapper<TenantPlan> tenantPlanMapper;

    @Resource
    private ExtTenantPlanMapper extTenantPlanMapper;

    @Resource
    private BaseMapper<Parameter> parameterMapper;

    @Resource
    private BaseMapper<User> userMapper;

    @Resource
    private BaseMapper<UserRole> userRoleMapper;

    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

    @Resource
    private BaseMapper<Organization> organizationMapper;

    @Resource
    private BaseMapper<TenantPlanHistory> tenantPlanHistoryMapper;

    @Resource
    private EditionService editionService;

    @Resource
    private AiQuotaService aiQuotaService;

    /**
     * 初始化免费试用套餐（租户开通时调用，试用 = 基础版）
     */
    public void initFreeTrial(String organizationId, String operatorId) {
        long now = System.currentTimeMillis();
        TenantPlan plan = new TenantPlan();
        plan.setId(IDGenerator.nextStr());
        plan.setOrganizationId(organizationId);
        plan.setVersion(DEFAULT_TRIAL_EDITION);
        plan.setStatus(TenantPlanStatus.FREE.getValue());
        plan.setExpireTime(now + getFreeTrialDays() * DAY_MILLIS);
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        plan.setCreateUser(operatorId);
        plan.setUpdateUser(operatorId);
        tenantPlanMapper.insert(plan);
    }

    /**
     * 分页查询租户套餐（含剩余天数）
     */
    public List<TenantPlanResponse> pageList(TenantPlanPageRequest request) {
        List<TenantPlanResponse> list = extTenantPlanMapper.pageList(request);
        long now = System.currentTimeMillis();
        // 签约/跟进经理名按租户表实时解析（业绩口径以 sys_organization 为准）
        Map<String, PlatformManagerNames.ManagerNames> managerNames = PlatformManagerNames.resolve(
                list.stream().map(TenantPlanResponse::getOrganizationId).toList());
        for (TenantPlanResponse item : list) {
            if (item.getExpireTime() != null) {
                item.setRemainingDays((long) Math.ceil((item.getExpireTime() - now) / (double) DAY_MILLIS));
            }
            PlatformManagerNames.ManagerNames mn = managerNames.get(item.getOrganizationId());
            item.setSignManagerName(mn == null ? null : mn.signManagerName());
            item.setFollowManagerName(mn == null ? null : mn.followManagerName());
        }
        return list;
    }

    /**
     * 开通/续费套餐（admin 手动）
     * <p>
     * 版本按编码读 {@link SysEdition}，到期时间缺省按版本有效期计算；同时写入版本快照。
     * </p>
     */
    public void open(TenantPlanOpenRequest request, String operatorId) {
        TenantPlan plan = tenantPlanMapper.selectByPrimaryKey(request.getId());
        if (plan == null) {
            throw new GenericException(Translator.get("tenant.plan.not_found"));
        }
        SysEdition edition = editionService.getEditionByCode(request.getVersion());
        if (edition == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        long now = System.currentTimeMillis();
        String oldVersion = plan.getVersion();
        Long oldExpire = plan.getExpireTime();
        boolean fromTrial = TenantPlanStatus.FREE.getValue().equals(plan.getStatus());
        plan.setVersion(edition.getCode());
        plan.setStatus(TenantPlanStatus.ACTIVE.getValue());
        // 续费顺延：未过期从原到期日顺延，已过期/过宽限期则从当天重算；试用期转付费从当天起算不叠加试用剩余
        long expireTime = request.getExpireTime() != null
                ? request.getExpireTime()
                : (fromTrial ? now : (oldExpire != null && oldExpire > now ? oldExpire : now))
                        + (long) getValidityDays(edition) * DAY_MILLIS;
        plan.setExpireTime(expireTime);
        plan.setRemark(StringUtils.trim(request.getRemark()));
        plan.setUpdateTime(now);
        plan.setUpdateUser(operatorId);
        tenantPlanMapper.updateById(plan);

        PriceResult priceResult = resolveOpenPrice(edition, plan.getOrganizationId());
        editionService.writeSnapshot(plan.getOrganizationId(), edition.getCode(), expireTime, priceResult.price(), operatorId);
        recordHistory(plan.getOrganizationId(), ACTION_OPEN, oldVersion, edition.getCode(), priceResult.price(),
                expireTime, StringUtils.trim(request.getRemark()), priceResult.detail(), operatorId, now);

        if (!edition.getCode().equals(oldVersion)) {
            syncAdminRole(plan.getOrganizationId(), operatorId);
        }
    }

    /**
     * 升级套餐：切换目标版本（版本编码读 {@link SysEdition}）
     */
    public void upgrade(TenantPlanUpgradeRequest request, String operatorId) {
        TenantPlan plan = tenantPlanMapper.selectByPrimaryKey(request.getId());
        if (plan == null) {
            throw new GenericException(Translator.get("tenant.plan.not_found"));
        }
        SysEdition edition = editionService.getEditionByCode(request.getEditionCode());
        if (edition == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        long now = System.currentTimeMillis();
        String oldVersion = plan.getVersion();
        Long oldExpire = plan.getExpireTime();
        plan.setVersion(edition.getCode());
        plan.setStatus(TenantPlanStatus.ACTIVE.getValue());
        // 升级：从核对当天重新起算目标版本有效期（原剩余时长作废，剩余价值已在补差价中折抵）
        long expireTime = now + (long) getValidityDays(edition) * DAY_MILLIS;
        plan.setUpdateTime(now);
        plan.setUpdateUser(operatorId);
        tenantPlanMapper.updateById(plan);

        PriceResult priceResult = resolveUpgradePrice(edition, oldVersion, oldExpire, plan.getOrganizationId());
        editionService.writeSnapshot(plan.getOrganizationId(), edition.getCode(), expireTime, priceResult.price(), operatorId);
        recordHistory(plan.getOrganizationId(), ACTION_UPGRADE, oldVersion, edition.getCode(), priceResult.price(),
                expireTime, null, priceResult.detail(), operatorId, now);
        syncAdminRole(plan.getOrganizationId(), operatorId);
    }

    /**
     * 按组织开通/续费企业版（平台回款核销后自动调用）
     * <p>
     * 与 {@link #open} 复用同一套逻辑：按编码读 {@link SysEdition}、到期时间顺延（未过期从原到期日续、否则从当天起算）、
     * 写版本快照、同步管理员角色。区别是这里按 organization_id 定位租户，无套餐记录时自动建档。
     * </p>
     */
    public void activateByOrganization(String organizationId, String editionCode, Integer validityDays, String operatorId) {
        SysEdition edition = editionService.getEditionByCode(editionCode);
        if (edition == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        long now = System.currentTimeMillis();
        TenantPlan plan = getByOrganizationId(organizationId);
        boolean isNew = plan == null;
        if (plan == null) {
            plan = new TenantPlan();
            plan.setId(IDGenerator.nextStr());
            plan.setOrganizationId(organizationId);
            plan.setCreateTime(now);
            plan.setCreateUser(operatorId);
        }
        String oldVersion = plan.getVersion();
        Long oldExpire = plan.getExpireTime();
        boolean fromTrial = TenantPlanStatus.FREE.getValue().equals(plan.getStatus());
        plan.setVersion(edition.getCode());
        plan.setStatus(TenantPlanStatus.ACTIVE.getValue());
        int days = validityDays != null ? validityDays : getValidityDays(edition);
        // 试用期转付费：从核对当天起算，不叠加试用剩余；付费期续费：从原到期日顺延
        plan.setExpireTime(fromTrial
                ? now + (long) days * DAY_MILLIS
                : (oldExpire != null && oldExpire > now ? oldExpire : now) + (long) days * DAY_MILLIS);
        plan.setUpdateTime(now);
        plan.setUpdateUser(operatorId);
        if (isNew) {
            tenantPlanMapper.insert(plan);
        } else {
            tenantPlanMapper.updateById(plan);
        }

        PriceResult priceResult = resolveOpenPrice(edition, organizationId);
        editionService.writeSnapshot(organizationId, edition.getCode(), plan.getExpireTime(), priceResult.price(), operatorId);
        recordHistory(organizationId, ACTION_OPEN, oldVersion, edition.getCode(), priceResult.price(),
                plan.getExpireTime(), null, priceResult.detail(), operatorId, now);

        if (!edition.getCode().equals(oldVersion)) {
            syncAdminRole(organizationId, operatorId);
        }
    }

    /**
     * 续费/升级报价（租户自助选择版本时实时计算）
     * <p>
     * 同版本 = 续费（年价）；升更高版本 = 升级补差；降级 = 不补差。复用 {@link #resolveOpenPrice} / {@link #resolveUpgradePrice}。
     * </p>
     */
    public TenantPlanQuoteResponse quote(String organizationId, String targetEditionCode) {
        SysEdition target = editionService.getEditionByCode(targetEditionCode);
        if (target == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        long now = System.currentTimeMillis();
        TenantPlan plan = getByOrganizationId(organizationId);
        String currentCode = plan == null ? null : plan.getVersion();
        Long oldExpire = plan == null ? null : plan.getExpireTime();
        SysEdition current = editionService.getEditionByCode(currentCode);

        TenantPlanQuoteResponse response = new TenantPlanQuoteResponse();
        response.setCurrentVersion(currentCode);
        response.setCurrentVersionName(current == null ? currentCode : current.getName());
        response.setTargetVersion(target.getCode());
        response.setTargetVersionName(target.getName());
        response.setValidityDays(getValidityDays(target));
        response.setRemainDays(oldExpire != null && oldExpire > now
                ? (long) Math.ceil((oldExpire - now) / (double) DAY_MILLIS)
                : 0);

        int currentSort = current == null || current.getSort() == null ? 0 : current.getSort();
        int targetSort = target.getSort() == null ? 0 : target.getSort();
        PriceResult priceResult;
        if (currentCode != null && targetSort < currentSort) {
            response.setType("DOWNGRADE");
            priceResult = new PriceResult(BigDecimal.ZERO, "降级不补差，成交价 ¥0");
        } else if (currentCode != null && targetSort > currentSort) {
            response.setType("UPGRADE");
            priceResult = resolveUpgradePrice(target, currentCode, oldExpire, organizationId);
        } else {
            response.setType("RENEW");
            priceResult = resolveOpenPrice(target, organizationId);
        }
        response.setAmount(priceResult.price());
        response.setPriceDetail(priceResult.detail());
        return response;
    }

    /**
     * 按组织升级套餐（租户自助升级核销后调用，与 {@link #upgrade} 复用同一套逻辑，区别是按 organization_id 定位）
     */
    public void upgradeByOrganization(String organizationId, String targetEditionCode, String operatorId) {
        SysEdition edition = editionService.getEditionByCode(targetEditionCode);
        if (edition == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        TenantPlan plan = getByOrganizationId(organizationId);
        if (plan == null) {
            throw new GenericException(Translator.get("tenant.plan.not_found"));
        }
        long now = System.currentTimeMillis();
        String oldVersion = plan.getVersion();
        Long oldExpire = plan.getExpireTime();
        plan.setVersion(edition.getCode());
        plan.setStatus(TenantPlanStatus.ACTIVE.getValue());
        // 升级：从核对当天重新起算目标版本有效期（原剩余时长作废，剩余价值已在补差价中折抵）
        long expireTime = now + (long) getValidityDays(edition) * DAY_MILLIS;
        plan.setUpdateTime(now);
        plan.setUpdateUser(operatorId);
        tenantPlanMapper.updateById(plan);

        PriceResult priceResult = resolveUpgradePrice(edition, oldVersion, oldExpire, organizationId);
        editionService.writeSnapshot(organizationId, edition.getCode(), expireTime, priceResult.price(), operatorId);
        recordHistory(organizationId, ACTION_UPGRADE, oldVersion, edition.getCode(), priceResult.price(),
                expireTime, null, priceResult.detail(), operatorId, now);
        syncAdminRole(organizationId, operatorId);
    }

    /**
     * 付费租户账号启用/禁用：控制管理员用户的 sys_organization_user.enable（登录拦截见 UserLoginService.checkUserStatus）
     */
    public void toggle(TenantPlanToggleRequest request, String operatorId) {
        setAdminAccountsEnabled(request.getOrganizationId(), request.getEnabled(), operatorId);
    }

    /**
     * 付费租户演示标记：置 sys_organization.is_demo。
     * is_demo=1 的租户不计入全局营收看板与城市经理业绩看板（演示数据不进账）。
     */
    public void toggleDemo(String organizationId, Boolean demo, String operatorId) {
        Organization organization = organizationMapper.selectByPrimaryKey(organizationId);
        if (organization == null) {
            throw new GenericException(Translator.get("organization.not.exist"));
        }
        organization.setDemo(demo);
        organization.setUpdateTime(System.currentTimeMillis());
        organization.setUpdateUser(operatorId);
        organizationMapper.updateById(organization);
    }

    /**
     * 到期自动禁用租户管理员账号（定时任务调用）
     */
    public void disableTenantAccounts(String organizationId, String operatorId) {
        setAdminAccountsEnabled(organizationId, false, operatorId);
    }

    /**
     * 控制租户管理员账号启用/禁用：改 sys_organization_user.enable（登录拦截见 UserLoginService.checkUserStatus）
     */
    private void setAdminAccountsEnabled(String organizationId, boolean enabled, String operatorId) {
        User adminUser = getAdminUser(organizationId);
        if (adminUser == null) {
            return;
        }
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<OrganizationUser> queryWrapper = new LambdaQueryWrapper<OrganizationUser>()
                .eq(OrganizationUser::getUserId, adminUser.getId());
        List<OrganizationUser> orgUsers = organizationUserMapper.selectListByLambda(queryWrapper);
        for (OrganizationUser orgUser : orgUsers) {
            orgUser.setEnable(enabled);
            orgUser.setUpdateTime(now);
            orgUser.setUpdateUser(operatorId);
            organizationUserMapper.updateById(orgUser);
        }
    }

    /**
     * 定位租户管理员用户（sys_user.last_organization_id = 组织ID）
     */
    private User getAdminUser(String organizationId) {
        List<User> users = userMapper.selectListByLambda(new LambdaQueryWrapper<User>()
                .eq(User::getLastOrganizationId, organizationId));
        return users.isEmpty() ? null : users.getFirst();
    }

    /**
     * 同步管理员角色：统一为租户管理员（org_admin），与组织类型、版本解耦
     */
    private void syncAdminRole(String organizationId, String operatorId) {
        User adminUser = getAdminUser(organizationId);
        if (adminUser == null) {
            return;
        }
        long now = System.currentTimeMillis();
        userRoleMapper.deleteByLambda(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, adminUser.getId()));
        String roleId = InternalRole.ORG_ADMIN.getValue();
        UserRole userRole = new UserRole();
        userRole.setId(IDGenerator.nextStr());
        userRole.setUserId(adminUser.getId());
        userRole.setRoleId(roleId);
        userRole.setCreateTime(now);
        userRole.setUpdateTime(now);
        userRole.setCreateUser(operatorId);
        userRole.setUpdateUser(operatorId);
        userRoleMapper.insert(userRole);
    }

    /**
     * 按组织查询套餐（无则返回 null）
     */
    public TenantPlan getByOrganizationId(String organizationId) {
        List<TenantPlan> plans = tenantPlanMapper.selectListByLambda(new LambdaQueryWrapper<TenantPlan>()
                .eq(TenantPlan::getOrganizationId, organizationId));
        return plans.isEmpty() ? null : plans.getFirst();
    }

    /**
     * 租户详情（付费用户详情页：组织信息 + 管理员 + 当前套餐 + AI 配额 + 开通/续费/升级历史）
     */
    public TenantPlanDetailResponse detail(TenantPlanDetailRequest request) {
        String organizationId = request.getOrganizationId();
        Organization organization = organizationMapper.selectByPrimaryKey(organizationId);
        if (organization == null) {
            throw new GenericException(Translator.get("organization.not.exist"));
        }
        TenantPlan plan = getByOrganizationId(organizationId);
        TenantEdition edition = editionService.getByOrganizationId(organizationId);
        User admin = getAdminUser(organizationId);
        long now = System.currentTimeMillis();

        TenantPlanDetailResponse response = new TenantPlanDetailResponse();
        response.setOrganizationId(organizationId);
        response.setOrgName(organization.getName());
        response.setOrgType(organization.getOrgType());
        response.setUnifiedSocialCreditCode(organization.getUnifiedSocialCreditCode());
        response.setLegalPersonName(organization.getLegalPersonName());
        response.setDemo(organization.getDemo());
        if (admin != null) {
            response.setAdminName(admin.getName());
            response.setPhone(admin.getPhone());
        }
        response.setEnabled(resolveEnabled(admin));

        if (plan != null) {
            response.setVersion(plan.getVersion());
            response.setStatus(plan.getStatus());
            response.setExpireTime(plan.getExpireTime());
            if (plan.getExpireTime() != null) {
                response.setRemainingDays((long) Math.ceil((plan.getExpireTime() - now) / (double) DAY_MILLIS));
            }
        }
        if (edition != null) {
            response.setEditionName(edition.getEditionName());
            response.setPrice(edition.getPrice());
        }
        response.setAiQuota(aiQuotaService.getMonthlyQuota(organizationId));
        response.setAiUsedCalls(aiQuotaService.getUsedCalls(organizationId));
        response.setHistories(listHistories(organizationId));
        return response;
    }

    /**
     * 记录开通/续费/升级历史
     */
    private void recordHistory(String organizationId, String action, String fromVersion, String toVersion,
                               BigDecimal price, Long expireTime, String remark, String priceDetail,
                               String operatorId, long now) {
        TenantPlanHistory history = new TenantPlanHistory();
        history.setId(IDGenerator.nextStr());
        history.setOrganizationId(organizationId);
        history.setAction(action);
        history.setFromVersion(fromVersion);
        history.setToVersion(toVersion);
        history.setPrice(price);
        history.setExpireTime(expireTime);
        history.setRemark(remark);
        history.setPriceDetail(priceDetail);
        history.setCreateTime(now);
        history.setUpdateTime(now);
        history.setCreateUser(operatorId);
        history.setUpdateUser(operatorId);
        tenantPlanHistoryMapper.insert(history);
    }

    /**
     * 查询开通/续费/升级历史（按时间倒序）
     */
    public List<TenantPlanHistoryResponse> listHistories(String organizationId) {
        List<TenantPlanHistory> list = tenantPlanHistoryMapper.selectListByLambda(new LambdaQueryWrapper<TenantPlanHistory>()
                .eq(TenantPlanHistory::getOrganizationId, organizationId)
                .orderByDesc(TenantPlanHistory::getCreateTime));
        List<TenantPlanHistoryResponse> result = new ArrayList<>();
        for (TenantPlanHistory item : list) {
            TenantPlanHistoryResponse row = new TenantPlanHistoryResponse();
            row.setAction(item.getAction());
            row.setFromVersion(item.getFromVersion());
            row.setToVersion(item.getToVersion());
            row.setFromVersionName(editionName(item.getFromVersion()));
            row.setToVersionName(editionName(item.getToVersion()));
            row.setPrice(item.getPrice());
            row.setPriceDetail(item.getPriceDetail());
            row.setExpireTime(item.getExpireTime());
            row.setRemark(item.getRemark());
            row.setCreateTime(item.getCreateTime());
            row.setCreateUser(item.getCreateUser());
            result.add(row);
        }
        return result;
    }

    /**
     * 版本编码 → 版本名称（读不到时回退编码本身）
     */
    private String editionName(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        SysEdition edition = editionService.getEditionByCode(code);
        return edition == null ? code : edition.getName();
    }

    /**
     * 租户管理员账号是否启用
     */
    private Boolean resolveEnabled(User admin) {
        if (admin == null) {
            return null;
        }
        List<OrganizationUser> orgUsers = organizationUserMapper.selectListByLambda(new LambdaQueryWrapper<OrganizationUser>()
                .eq(OrganizationUser::getUserId, admin.getId()));
        return orgUsers.isEmpty() ? null : orgUsers.getFirst().getEnable();
    }

    /**
     * 套餐是否已到期（含宽限期，过宽限期才算硬到期）
     */
    public boolean isExpired(TenantPlan plan) {
        if (plan == null || plan.getExpireTime() == null) {
            return false;
        }
        return plan.getExpireTime() + (long) getGraceDays() * DAY_MILLIS <= System.currentTimeMillis();
    }

    /**
     * 套餐是否处于宽限期（到期后、硬到期前）
     */
    public boolean isInGrace(TenantPlan plan) {
        if (plan == null || plan.getExpireTime() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        return plan.getExpireTime() <= now && now < plan.getExpireTime() + (long) getGraceDays() * DAY_MILLIS;
    }

    /**
     * 获取全局配置
     */
    public TenantPlanConfigResponse getConfig() {
        TenantPlanConfigResponse response = new TenantPlanConfigResponse();
        response.setFreeTrialDays(getFreeTrialDays());
        String remindDays = getParam(PARAM_EXPIRE_REMIND_DAYS);
        response.setExpireRemindDays(StringUtils.isBlank(remindDays) ? DEFAULT_EXPIRE_REMIND_DAYS : remindDays);
        response.setGraceDays(getGraceDays());
        return response;
    }

    /**
     * 更新全局配置
     */
    public void updateConfig(TenantPlanConfigRequest request) {
        setParam(PARAM_FREE_TRIAL_DAYS, String.valueOf(request.getFreeTrialDays()));
        if (StringUtils.isNotBlank(request.getExpireRemindDays())) {
            setParam(PARAM_EXPIRE_REMIND_DAYS, StringUtils.trim(request.getExpireRemindDays()));
        }
        if (request.getGraceDays() != null) {
            setParam(PARAM_GRACE_DAYS, String.valueOf(request.getGraceDays()));
        }
    }

    /**
     * 免费试用天数
     */
    public int getFreeTrialDays() {
        String value = getParam(PARAM_FREE_TRIAL_DAYS);
        if (StringUtils.isBlank(value)) {
            return DEFAULT_FREE_TRIAL_DAYS;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_FREE_TRIAL_DAYS;
        }
    }

    /**
     * 到期宽限天数（缺省 3）
     */
    public int getGraceDays() {
        String value = getParam(PARAM_GRACE_DAYS);
        if (StringUtils.isBlank(value)) {
            return DEFAULT_GRACE_DAYS;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_GRACE_DAYS;
        }
    }

    /**
     * 到期提醒天数列表（如 [30, 7]）
     */
    public List<Integer> getExpireRemindDays() {
        String value = getParam(PARAM_EXPIRE_REMIND_DAYS);
        if (StringUtils.isBlank(value)) {
            value = DEFAULT_EXPIRE_REMIND_DAYS;
        }
        List<Integer> days = new ArrayList<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .forEach(day -> {
                    try {
                        days.add(Integer.parseInt(day));
                    } catch (NumberFormatException ignored) {
                        // 忽略非法配置项
                    }
                });
        return days;
    }

    /**
     * 版本有效期天数（缺省 365）
     */
    private int getValidityDays(SysEdition edition) {
        return edition.getValidityDays() == null ? DEFAULT_VALIDITY_DAYS : edition.getValidityDays();
    }

    /**
     * 开通价格：首次付费（尚无版本快照）用「首年促销价」（无则回退年价），续费/复购用「年价」。
     */
    private PriceResult resolveOpenPrice(SysEdition edition, String organizationId) {
        boolean firstYear = editionService.getByOrganizationId(organizationId) == null;
        BigDecimal promo = edition.getFirstYearPrice();
        if (firstYear && promo != null && promo.signum() > 0) {
            return new PriceResult(promo, "首次开通·" + edition.getName() + "首年促销价 ¥" + money(promo));
        }
        return new PriceResult(edition.getYearPrice(),
                "续费·" + edition.getName() + "年价 ¥" + money(edition.getYearPrice()));
    }

    /**
     * 升级补差：升级价 = 目标版本年价 − 剩余折合（当前版年价 × 剩余天数 / 365），升级后重新起算目标版本有效期。
     * 无剩余时长（已到期/无套餐）则按首年促销价；成交价不低于 0。
     */
    private PriceResult resolveUpgradePrice(SysEdition target, String oldVersionCode, Long oldExpire, String organizationId) {
        SysEdition current = editionService.getEditionByCode(oldVersionCode);
        String currentName = current != null ? current.getName() : oldVersionCode;
        BigDecimal from = current != null && current.getYearPrice() != null ? current.getYearPrice() : BigDecimal.ZERO;
        BigDecimal to = target.getYearPrice() != null ? target.getYearPrice() : BigDecimal.ZERO;
        long now = System.currentTimeMillis();
        long remaining = (oldExpire != null && oldExpire > now)
                ? (long) Math.ceil((oldExpire - now) / (double) DAY_MILLIS)
                : 0;
        if (remaining <= 0) {
            return resolveOpenPrice(target, organizationId);
        }
        BigDecimal credit = from.multiply(BigDecimal.valueOf(remaining))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
        BigDecimal price = to.subtract(credit).max(BigDecimal.ZERO);
        String detail = "升级价 = " + target.getName() + "年价 ¥" + money(to)
                + " − 剩余折合 ¥" + money(credit)
                + "（" + currentName + "年价 ¥" + money(from)
                + " × 剩余 " + remaining + " 天 ÷ 365）= ¥" + money(price)
                + "，升级后重新起算 " + getValidityDays(target) + " 天";
        return new PriceResult(price, detail);
    }

    /**
     * 金额去尾零展示（3980.00 → 3980）
     */
    private String money(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 成交价 + 计算过程（供历史记录落库与详情/个人中心展示）
     */
    private record PriceResult(BigDecimal price, String detail) {
    }

    private String getParam(String key) {
        Parameter parameter = parameterMapper.selectByPrimaryKey(key);
        return parameter == null ? null : parameter.getParamValue();
    }

    private void setParam(String key, String value) {
        parameterMapper.deleteByPrimaryKey(key);
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        parameter.setType("text");
        parameterMapper.insert(parameter);
    }
}
