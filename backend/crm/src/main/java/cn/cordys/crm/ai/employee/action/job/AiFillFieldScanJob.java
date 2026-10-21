package cn.cordys.crm.ai.employee.action.job;

import cn.cordys.crm.ai.employee.action.service.AiFillFieldScanService;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 自动补全数据扫描定时任务：每天 6:00 逐租户扫描「必填字段缺失」客户并生成待确认补全建议。
 */
@Component
@Slf4j
public class AiFillFieldScanJob {

    @Resource
    private AiFillFieldScanService scanService;

    @Resource
    private ExtOrganizationMapper extOrganizationMapper;

    @QuartzScheduled(cron = "0 0 6 * * ?")
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
                    log.error("AI 员工补全数据扫描失败，orgId={}", orgId, e);
                }
            }
            log.info("AI 员工补全数据扫描完成，共生成 {} 条待确认建议", total);
        } catch (Exception e) {
            log.error("AI 员工补全数据扫描任务执行失败", e);
        }
    }
}
