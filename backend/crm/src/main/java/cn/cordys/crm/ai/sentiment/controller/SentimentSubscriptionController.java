package cn.cordys.crm.ai.sentiment.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.sentiment.domain.SentimentResult;
import cn.cordys.crm.ai.sentiment.domain.SentimentSubscription;
import cn.cordys.crm.ai.sentiment.dto.request.SentimentSubscriptionCreateRequest;
import cn.cordys.crm.ai.sentiment.service.SentimentSubscriptionService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 舆情监控接口（企业版 lead_crawl 门控，搜索按次记账，新舆情/风险信号站内信推订阅人）。
 */
@RestController
@RequestMapping("/agent/sentiment-monitor")
@Tag(name = "舆情监控")
public class SentimentSubscriptionController {

    @Resource
    private SentimentSubscriptionService sentimentSubscriptionService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通舆情监控")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_LEAD_CRAWL);
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前租户的舆情监控订阅列表")
    public List<SentimentSubscription> list() {
        return sentimentSubscriptionService.list(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/create")
    @Operation(summary = "新建舆情监控订阅")
    public SentimentSubscription create(@RequestBody SentimentSubscriptionCreateRequest request) {
        return sentimentSubscriptionService.create(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(),
                request);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除舆情监控订阅及其结果")
    public void delete(@RequestParam String id) {
        sentimentSubscriptionService.delete(OrganizationContext.getOrganizationId(), id);
    }

    @PostMapping("/toggle")
    @Operation(summary = "启停舆情监控订阅")
    public void toggle(@RequestParam String id, @RequestParam boolean enabled) {
        sentimentSubscriptionService.toggle(OrganizationContext.getOrganizationId(), id, enabled);
    }

    @PostMapping("/search-now")
    @Operation(summary = "立即搜索某条订阅，返回新发现的舆情/风险信号")
    public List<SentimentResult> searchNow(@RequestParam String id) {
        return sentimentSubscriptionService.searchNow(OrganizationContext.getOrganizationId(), id);
    }

    @GetMapping("/results")
    @Operation(summary = "查询某条订阅的历史舆情/风险信号")
    public List<SentimentResult> results(@RequestParam String id) {
        return sentimentSubscriptionService.results(OrganizationContext.getOrganizationId(), id);
    }
}
