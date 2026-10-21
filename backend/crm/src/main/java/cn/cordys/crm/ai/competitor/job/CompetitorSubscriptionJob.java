package cn.cordys.crm.ai.competitor.job;

import cn.cordys.crm.ai.competitor.service.CompetitorSubscriptionService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 竞品动态订阅定时搜索：每天 6:30 对所有启用的订阅联网搜索竞品最新动态，新结果站内信推送。
 */
@Component
@Slf4j
public class CompetitorSubscriptionJob {

    @Resource
    private CompetitorSubscriptionService competitorSubscriptionService;

    @QuartzScheduled(cron = "0 30 6 * * ?")
    public void onEvent() {
        try {
            competitorSubscriptionService.searchAllSubscriptions();
        } catch (Exception e) {
            log.error("竞品动态订阅定时搜索异常", e);
        }
    }
}
