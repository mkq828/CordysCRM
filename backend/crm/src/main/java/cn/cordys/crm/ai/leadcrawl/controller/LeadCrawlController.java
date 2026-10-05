package cn.cordys.crm.ai.leadcrawl.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.controller.BaseAiStreamController;
import cn.cordys.crm.ai.controller.SseEventWriter;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.leadcrawl.dto.request.CompetitorInsightRequest;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadCrawlDetailRequest;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadCrawlImportRequest;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadCrawlSearchRequest;
import cn.cordys.crm.ai.leadcrawl.dto.response.CompetitorInsightResponse;
import cn.cordys.crm.ai.leadcrawl.dto.response.LeadCrawlEnterpriseResponse;
import cn.cordys.crm.ai.leadcrawl.dto.response.LeadCrawlImportResponse;
import cn.cordys.crm.ai.leadcrawl.service.CompetitorInsightService;
import cn.cordys.crm.ai.leadcrawl.service.LeadCrawlService;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 线索爬取 + 竞品/行业洞察接口（功能 10，企业版 G3 门控）。
 * 线索爬取走企查查 API + 平台缓存，不计量额度；竞品洞察走任务模型流式生成，挂 G2 额度。
 */
@RestController
@RequestMapping("/agent/lead-crawl")
@Tag(name = "线索爬取与竞品洞察")
public class LeadCrawlController extends BaseAiStreamController {

    @Resource
    private LeadCrawlService leadCrawlService;
    @Resource
    private CompetitorInsightService competitorInsightService;
    @Resource
    private AgentConversationService agentConversationService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通线索爬取与竞品洞察")
    public boolean available() {
        // 功能未完成（未接入真实企查查 Key 验证），暂仅管理员可见，不对外开放
        if (!PlatformSessionUtils.isAdmin()) {
            return false;
        }
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_LEAD_CRAWL);
    }

    @PostMapping("/search")
    @Operation(summary = "按关键字搜索企业名单")
    public List<LeadCrawlEnterpriseResponse> search(@Valid @RequestBody LeadCrawlSearchRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return leadCrawlService.search(orgId, request.getKeyword());
    }

    @PostMapping("/detail")
    @Operation(summary = "拉取企业完整工商信息（命中缓存直接返回）")
    public List<LeadCrawlEnterpriseResponse> detail(@Valid @RequestBody LeadCrawlDetailRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return leadCrawlService.fetchDetail(orgId, request.getNames());
    }

    @PostMapping("/import")
    @Operation(summary = "把选中企业导入线索池")
    public LeadCrawlImportResponse importToPool(@Valid @RequestBody LeadCrawlImportRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return leadCrawlService.importToPool(orgId, SessionUtils.getUserId(), request);
    }

    @PostMapping(value = "/insight/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成竞品/行业洞察简报")
    public void insightStream(@RequestBody CompetitorInsightRequest request, HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        PrintWriter writer = beginSse(response);

        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(orgId, userId, AiQuotaConstant.AI_LEAD_CRAWL,
                request.getConversationId(), title(request.getCompetitor(), "竞品洞察"));
        agentConversationService.saveUserMessage(conversationId, runId, orgId, userId, userContent(request), null);

        SseEventWriter.write(writer, "run", null, runData(conversationId, runId, assistantMessageId));

        StringBuilder narrative = new StringBuilder();
        try {
            AiStreamResult<CompetitorInsightResponse> streamResult = competitorInsightService.insight(orgId, request, chunk -> {
                narrative.append(chunk);
                try {
                    SseEventWriter.write(writer, "chunk", conversationId, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            LlmUsage usage = streamResult.usage();
            CompetitorInsightResponse result = streamResult.result();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    narrative.toString(), JSON.toJSONString(result), "done", usage);
            SseEventWriter.write(writer, "done", null, doneData(conversationId, runId, assistantMessageId, usage, result));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    message, null, "error", null);
            SseEventWriter.write(writer, "error", null, Map.of("message", message));
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private void checkAvailable(String orgId) {
        // 功能未完成（未接入真实企查查 Key 验证），暂仅管理员可见，不对外开放
        if (!PlatformSessionUtils.isAdmin()) {
            throw new GenericException("线索爬取与竞品洞察暂未开放，仅管理员可见");
        }
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_LEAD_CRAWL)) {
            throw new GenericException("当前版本未开通线索爬取与竞品洞察，请升级至企业版");
        }
    }

    private String userContent(CompetitorInsightRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("竞品：").append(StringUtils.trimToEmpty(request.getCompetitor()));
        sb.append("；行业：").append(StringUtils.trimToEmpty(request.getIndustry()));
        if (StringUtils.isNotBlank(request.getFocus())) {
            sb.append("；关注点：").append(request.getFocus().trim());
        }
        return sb.toString();
    }
}
