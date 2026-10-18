package cn.cordys.crm.ai.tender.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.tender.domain.TenderResult;
import cn.cordys.crm.ai.tender.domain.TenderSubscription;
import cn.cordys.crm.ai.tender.dto.request.TenderSubscriptionCreateRequest;
import cn.cordys.crm.ai.tender.service.TenderSubscriptionService;
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
 * 标讯订阅接口（企业版 lead_crawl 门控，搜索按次记账，新结果站内信推订阅人）。
 */
@RestController
@RequestMapping("/agent/tender-subscription")
@Tag(name = "标讯订阅")
public class TenderSubscriptionController {

    @Resource
    private TenderSubscriptionService tenderSubscriptionService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通标讯订阅")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_LEAD_CRAWL);
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前租户的标讯订阅列表")
    public List<TenderSubscription> list() {
        return tenderSubscriptionService.list(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/create")
    @Operation(summary = "新建标讯订阅")
    public TenderSubscription create(@RequestBody TenderSubscriptionCreateRequest request) {
        return tenderSubscriptionService.create(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), request);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除标讯订阅及其结果")
    public void delete(@RequestParam String id) {
        tenderSubscriptionService.delete(OrganizationContext.getOrganizationId(), id);
    }

    @PostMapping("/toggle")
    @Operation(summary = "启停标讯订阅")
    public void toggle(@RequestParam String id, @RequestParam boolean enabled) {
        tenderSubscriptionService.toggle(OrganizationContext.getOrganizationId(), id, enabled);
    }

    @PostMapping("/search-now")
    @Operation(summary = "立即搜索某条订阅，返回新发现的招标结果")
    public List<TenderResult> searchNow(@RequestParam String id) {
        return tenderSubscriptionService.searchNow(OrganizationContext.getOrganizationId(), id);
    }

    @GetMapping("/results")
    @Operation(summary = "查询某条订阅的历史招标结果")
    public List<TenderResult> results(@RequestParam String id) {
        return tenderSubscriptionService.results(OrganizationContext.getOrganizationId(), id);
    }
}
