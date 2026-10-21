package cn.cordys.crm.ai.bossbrief.job;

import cn.cordys.crm.ai.bossbrief.service.BossBriefService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 老板晨报定时生成：每天 7:00 对所有启用的主题联网搜索当日要闻，新结果站内信推送（上班前生成好）。
 */
@Component
@Slf4j
public class BossBriefJob {

    @Resource
    private BossBriefService bossBriefService;

    @QuartzScheduled(cron = "0 0 7 * * ?")
    public void onEvent() {
        try {
            bossBriefService.searchAllTopics();
        } catch (Exception e) {
            log.error("老板晨报定时生成异常", e);
        }
    }
}
