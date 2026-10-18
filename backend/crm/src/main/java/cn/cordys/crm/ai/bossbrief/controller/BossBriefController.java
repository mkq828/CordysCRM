package cn.cordys.crm.ai.bossbrief.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.bossbrief.domain.BossBriefResult;
import cn.cordys.crm.ai.bossbrief.domain.BossBriefTopic;
import cn.cordys.crm.ai.bossbrief.dto.request.BossBriefTopicCreateRequest;
import cn.cordys.crm.ai.bossbrief.service.BossBriefService;
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
 * 老板晨报接口（企业版 lead_crawl 门控，搜索按次记账，新当日要闻站内信推订阅人）。
 */
@RestController
@RequestMapping("/agent/boss-brief")
@Tag(name = "老板晨报")
public class BossBriefController {

    @Resource
    private BossBriefService bossBriefService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通老板晨报")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_LEAD_CRAWL);
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前租户的老板晨报关注主题列表")
    public List<BossBriefTopic> list() {
        return bossBriefService.list(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/create")
    @Operation(summary = "新建老板晨报关注主题")
    public BossBriefTopic create(@RequestBody BossBriefTopicCreateRequest request) {
        return bossBriefService.create(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), request);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除老板晨报关注主题及其结果")
    public void delete(@RequestParam String id) {
        bossBriefService.delete(OrganizationContext.getOrganizationId(), id);
    }

    @PostMapping("/toggle")
    @Operation(summary = "启停老板晨报关注主题")
    public void toggle(@RequestParam String id, @RequestParam boolean enabled) {
        bossBriefService.toggle(OrganizationContext.getOrganizationId(), id, enabled);
    }

    @PostMapping("/search-now")
    @Operation(summary = "立即生成某条主题的当日要闻，返回新发现的结果")
    public List<BossBriefResult> searchNow(@RequestParam String id) {
        return bossBriefService.searchNow(OrganizationContext.getOrganizationId(), id);
    }

    @GetMapping("/results")
    @Operation(summary = "查询某条主题的历史当日要闻")
    public List<BossBriefResult> results(@RequestParam String id) {
        return bossBriefService.results(OrganizationContext.getOrganizationId(), id);
    }
}
