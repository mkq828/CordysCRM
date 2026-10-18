package cn.cordys.crm.ai.leadcrawl.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadMiningImportRequest;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadMiningSearchRequest;
import cn.cordys.crm.ai.leadcrawl.dto.response.LeadMiningImportResponse;
import cn.cordys.crm.ai.leadcrawl.service.LeadMiningService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 潜客挖掘接口（企业版 lead_crawl 门控，搜索按次记账）。
 */
@RestController
@RequestMapping("/agent/lead-mining")
@Tag(name = "潜客挖掘")
public class LeadMiningController {

    @Resource
    private LeadMiningService leadMiningService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通潜客挖掘")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_LEAD_CRAWL);
    }

    @PostMapping("/search")
    @Operation(summary = "按行业/地域/关键词联网搜索潜在客户名单")
    public List<AdvisorWebSource> search(@RequestBody LeadMiningSearchRequest request) {
        return leadMiningService.search(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), request);
    }

    @PostMapping("/import")
    @Operation(summary = "把一条搜索结果导入线索池")
    public LeadMiningImportResponse importToClue(@RequestBody LeadMiningImportRequest request) {
        return leadMiningService.importToClue(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), request);
    }
}
