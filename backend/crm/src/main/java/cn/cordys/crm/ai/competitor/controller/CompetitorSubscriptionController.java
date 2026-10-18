package cn.cordys.crm.ai.competitor.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.competitor.domain.CompetitorResult;
import cn.cordys.crm.ai.competitor.domain.CompetitorSubscription;
import cn.cordys.crm.ai.competitor.dto.request.CompetitorSubscriptionCreateRequest;
import cn.cordys.crm.ai.competitor.service.CompetitorSubscriptionService;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
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
 * 竞品动态接口（企业版 lead_crawl 门控，搜索按次记账，新动态站内信推订阅人）。
 */
@RestController
@RequestMapping("/agent/competitor-monitor")
@Tag(name = "竞品动态")
public class CompetitorSubscriptionController {

    @Resource
    private CompetitorSubscriptionService competitorSubscriptionService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通竞品动态")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_LEAD_CRAWL);
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前租户的竞品动态订阅列表")
    public List<CompetitorSubscription> list() {
        return competitorSubscriptionService.list(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/create")
    @Operation(summary = "新建竞品动态订阅")
    public CompetitorSubscription create(@RequestBody CompetitorSubscriptionCreateRequest request) {
        return competitorSubscriptionService.create(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(),
                request);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除竞品动态订阅及其结果")
    public void delete(@RequestParam String id) {
        competitorSubscriptionService.delete(OrganizationContext.getOrganizationId(), id);
    }

    @PostMapping("/toggle")
    @Operation(summary = "启停竞品动态订阅")
    public void toggle(@RequestParam String id, @RequestParam boolean enabled) {
        competitorSubscriptionService.toggle(OrganizationContext.getOrganizationId(), id, enabled);
    }

    @PostMapping("/search-now")
    @Operation(summary = "立即搜索某条订阅，返回新发现的竞品动态")
    public List<CompetitorResult> searchNow(@RequestParam String id) {
        return competitorSubscriptionService.searchNow(OrganizationContext.getOrganizationId(), id);
    }

    @GetMapping("/results")
    @Operation(summary = "查询某条订阅的历史竞品动态")
    public List<CompetitorResult> results(@RequestParam String id) {
        return competitorSubscriptionService.results(OrganizationContext.getOrganizationId(), id);
    }
}
