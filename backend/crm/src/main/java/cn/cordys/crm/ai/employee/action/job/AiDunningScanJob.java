package cn.cordys.crm.ai.employee.action.job;

import cn.cordys.crm.ai.employee.action.service.AiDunningScanService;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 自动催款扫描定时任务：每天 5:30 逐租户扫描「即将到期/已逾期」的合同与回款计划并生成待确认催款建议。
 * 与 {@code NoticeExpireJob} 共存：后者是被动到期提醒，本任务是「可操作的两段确认 AI 催款建议」。
 */
@Component
@Slf4j
public class AiDunningScanJob {

    @Resource
    private AiDunningScanService scanService;

    @Resource
    private ExtOrganizationMapper extOrganizationMapper;

    @QuartzScheduled(cron = "0 30 5 * * ?")
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
                    log.error("AI 员工催款扫描失败，orgId={}", orgId, e);
                }
            }
            log.info("AI 员工催款扫描完成，共生成 {} 条待确认建议", total);
        } catch (Exception e) {
            log.error("AI 员工催款扫描任务执行失败", e);
        }
    }
}
