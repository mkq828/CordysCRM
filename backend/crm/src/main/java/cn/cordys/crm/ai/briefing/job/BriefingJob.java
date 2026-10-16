package cn.cordys.crm.ai.briefing.job;

import cn.cordys.crm.ai.briefing.domain.AiBriefing;
import cn.cordys.crm.ai.briefing.service.BriefingService;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 经营大屏「晨报/周报」定时生成。晨报每天 8 点、周报每周一 8 点，逐租户生成并落库。
 * 无登录态，仅落库供首屏「AI 经营简报」卡读取；不逐租户发站内信（租户老板无唯一账号可定位）。
 */
@Component
@Slf4j
public class BriefingJob {

    @Resource
    private BriefingService briefingService;
    @Resource
    private ExtOrganizationMapper extOrganizationMapper;

    @QuartzScheduled(cron = "0 0 8 * * ?")
    public void dailyBriefing() {
        run(AiBriefing.TYPE_DAILY);
    }

    @QuartzScheduled(cron = "0 0 8 ? * MON")
    public void weeklyBriefing() {
        run(AiBriefing.TYPE_WEEKLY);
    }

    private void run(String type) {
        try {
            Set<String> orgIds = extOrganizationMapper.selectAllOrganizationIds();
            if (orgIds == null || orgIds.isEmpty()) {
                return;
            }
            int success = 0;
            for (String orgId : orgIds) {
                try {
                    briefingService.generate(orgId, type, null);
                    success++;
                } catch (Exception e) {
                    log.error("生成经营简报失败，orgId={}, type={}", orgId, type, e);
                }
            }
            log.info("经营简报定时生成完成，type={}, 成功 {}/{}", type, success, orgIds.size());
        } catch (Exception e) {
            log.error("经营简报定时任务执行失败，type={}", type, e);
        }
    }
}
