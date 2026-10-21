package cn.cordys.crm.ai.employee.action.job;

import cn.cordys.crm.ai.employee.action.service.AiTagScanService;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 自动打标签扫描定时任务：每天 5:45 逐租户扫描「久未联系」客户并生成待确认打标签建议。
 */
@Component
@Slf4j
public class AiTagScanJob {

    @Resource
    private AiTagScanService scanService;

    @Resource
    private ExtOrganizationMapper extOrganizationMapper;

    @QuartzScheduled(cron = "0 45 5 * * ?")
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
                    log.error("AI 员工打标签扫描失败，orgId={}", orgId, e);
                }
            }
            log.info("AI 员工打标签扫描完成，共生成 {} 条待确认建议", total);
        } catch (Exception e) {
            log.error("AI 员工打标签扫描任务执行失败", e);
        }
    }
}
