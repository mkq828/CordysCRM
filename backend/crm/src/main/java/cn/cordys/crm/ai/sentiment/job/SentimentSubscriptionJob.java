package cn.cordys.crm.ai.sentiment.job;

import cn.cordys.crm.ai.sentiment.service.SentimentSubscriptionService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 舆情监控订阅定时搜索：每天 8:40 对所有启用的订阅联网搜索负面舆情/风险信号，新结果站内信推送。
 */
@Component
@Slf4j
public class SentimentSubscriptionJob {

    @Resource
    private SentimentSubscriptionService sentimentSubscriptionService;

    @QuartzScheduled(cron = "0 40 8 * * ?")
    public void onEvent() {
        try {
            sentimentSubscriptionService.searchAllSubscriptions();
        } catch (Exception e) {
            log.error("舆情监控订阅定时搜索异常", e);
        }
    }
}
