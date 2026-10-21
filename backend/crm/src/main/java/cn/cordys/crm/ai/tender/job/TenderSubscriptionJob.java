package cn.cordys.crm.ai.tender.job;

import cn.cordys.crm.ai.tender.service.TenderSubscriptionService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 标讯订阅定时搜索：每天 6:15 对所有启用的订阅联网搜索招标信息，新结果站内信推送。
 */
@Component
@Slf4j
public class TenderSubscriptionJob {

    @Resource
    private TenderSubscriptionService tenderSubscriptionService;

    @QuartzScheduled(cron = "0 15 6 * * ?")
    public void onEvent() {
        try {
            tenderSubscriptionService.searchAllSubscriptions();
        } catch (Exception e) {
            log.error("标讯订阅定时搜索异常", e);
        }
    }
}
