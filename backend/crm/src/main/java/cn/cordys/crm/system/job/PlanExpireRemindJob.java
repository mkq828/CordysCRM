package cn.cordys.crm.system.job;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.TenantPlanStatus;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.TenantPlan;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.TenantPlanService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 租户套餐到期提醒监听器
 * <p>
 * 每天 8 点执行：将已到期套餐翻为 EXPIRED，并在到期前 N 天（可配）给平台管理员发送站内信提醒。
 * </p>
 */
@Component
@Slf4j
public class PlanExpireRemindJob {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    @Resource
    private TenantPlanService tenantPlanService;

    @Resource
    private BaseMapper<TenantPlan> tenantPlanMapper;

    @Resource
    private BaseMapper<Organization> organizationMapper;

    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    /**
     * 定时检查租户套餐到期情况并提醒平台管理员
     */
    @QuartzScheduled(cron = "0 0 8 * * ?")
    public void onEvent() {
        try {
            long now = System.currentTimeMillis();
            List<Integer> remindDays = tenantPlanService.getExpireRemindDays();
            List<TenantPlan> plans = tenantPlanMapper.selectAll(null);

            List<String> expiringNames = new ArrayList<>();
            for (TenantPlan plan : plans) {
                if (plan.getExpireTime() == null) {
                    continue;
                }
                // 已到期（含宽限期）：翻转状态为 EXPIRED 并禁用租户管理员账号
                if (tenantPlanService.isExpired(plan)) {
                    if (!TenantPlanStatus.EXPIRED.getValue().equals(plan.getStatus())) {
                        plan.setStatus(TenantPlanStatus.EXPIRED.getValue());
                        plan.setUpdateTime(now);
                        tenantPlanMapper.updateById(plan);
                        tenantPlanService.disableTenantAccounts(plan.getOrganizationId(), InternalUser.ADMIN.getValue());
                    }
                    continue;
                }
                // 即将到期：命中配置的提醒天数
                long remainingDays = (long) Math.ceil((plan.getExpireTime() - now) / (double) DAY_MILLIS);
                if (remindDays.contains((int) remainingDays)) {
                    Organization organization = organizationMapper.selectByPrimaryKey(plan.getOrganizationId());
                    if (organization != null) {
                        expiringNames.add(organization.getName());
                    }
                }
            }

            if (!expiringNames.isEmpty()) {
                commonNoticeSendService.sendNotice(
                        NotificationConstants.Module.SYSTEM,
                        NotificationConstants.Event.PLAN_EXPIRE_REMIND,
                        Map.of("name", String.join("、", expiringNames)),
                        InternalUser.ADMIN.getValue(),
                        OrganizationContext.DEFAULT_ORGANIZATION_ID,
                        List.of(InternalUser.ADMIN.getValue()),
                        false);
            }
        } catch (Exception e) {
            log.error("租户套餐到期提醒执行失败", e);
        }
    }
}
