package cn.cordys.crm.ai.employee.action.job;

import cn.cordys.crm.ai.employee.action.service.AiFollowUpScanService;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 自动跟进扫描定时任务：每天 8:00 逐租户扫描「久未联系」客户并生成待确认跟进建议。
 * 无登录态，显式传 orgId；单个租户失败不影响其它租户。
 */
@Component
@Slf4j
public class AiFollowUpScanJob {

    @Resource
    private AiFollowUpScanService scanService;

    @Resource
    private ExtOrganizationMapper extOrganizationMapper;

    @QuartzScheduled(cron = "0 0 8 * * ?")
    public void onEvent() {
        try {
            Set<String> orgIds = extOrganizationMapper.selectAllOrganizationIds();
            if (orgIds == null || orgIds.isEmpty()) {
                return;
            }
            int total = 0;
            for (String orgId : orgIds) {
                try {
                    total += scanService.scan(orgId, null);
                } catch (Exception e) {
                    log.error("AI 员工跟进扫描失败，orgId={}", orgId, e);
                }
            }
            log.info("AI 员工跟进扫描完成，共生成 {} 条待确认建议", total);
        } catch (Exception e) {
            log.error("AI 员工跟进扫描任务执行失败", e);
        }
    }
}
