package cn.cordys.crm.ai.attribution.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.attribution.dto.request.AttributionRequest;
import cn.cordys.crm.ai.attribution.dto.response.AttributionResponse;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.dashboard.dto.response.RegionDetailResponse;
import cn.cordys.crm.dashboard.dto.response.SalesOverviewResponse;
import cn.cordys.crm.dashboard.dto.response.StageDetailResponse;
import cn.cordys.crm.dashboard.service.BusinessScreenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营大屏「经营归因」：复用大屏已富化的下钻数据 → 塞进 prompt → 用洞察型模型生成「为什么」结论。
 * 与 AI 经营助手同口径：免费全租户·不计量（不查额度、不记账）。
 */
@Service
@Slf4j
public class AttributionService {

    public static final String KIND_OVERVIEW = "overview";
    public static final String KIND_SALES = "sales";
    public static final String KIND_REGION = "region";
    public static final String KIND_STAGE = "stage";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SYSTEM_PROMPT = "你是一名资深企业经营分析师。请根据给定的经营数据，"
            + "用通俗中文诊断这个维度为什么会呈现这样的结果。请严格按以下两段式输出，"
            + "每段用 Markdown 编号列表（1. 2. 3.）分点，需要对比时用 Markdown 表格（| 指标 | 本期 | 上期 |），"
            + "用 **加粗** 突出关键数字，不要使用 # 标题：\n"
            + "现状：\n1. <当前数据表现>\n2. <最可能的原因>\n"
            + "建议：\n1. <可落地的行动建议>\n"
            + "全文控制在 500 字以内。";

    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private BusinessScreenService businessScreenService;

    public AttributionResponse attribute(String orgId, AttributionRequest request) {
        String kind = StringUtils.defaultIfBlank(request.getKind(), KIND_OVERVIEW);
        Map<String, Object> data = buildContext(orgId, kind, request.getKey(), request.getRange());

        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }

        String dataJson;
        try {
            dataJson = OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            dataJson = "{}";
        }
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", SYSTEM_PROMPT),
                new LlmMessage("user", "分析维度：" + label(kind) + "\n\n经营数据（JSON）：\n" + dataJson));

        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                String content = callModel(model, messages);
                AttributionResponse response = new AttributionResponse();
                response.setContent(content);
                return response;
            } catch (Exception e) {
                lastError = e;
                log.warn("经营归因模型调用失败，降级尝试下一个，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("经营归因全部模型失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 按维度组装已富化的下钻数据（只取结论所需关键指标，避免 prompt 过长）。 */
    private Map<String, Object> buildContext(String orgId, String kind, String key, String range) {
        Map<String, Object> context = new LinkedHashMap<>();
        switch (kind) {
            case KIND_SALES -> {
                SalesOverviewResponse sales = businessScreenService.salesDetail(orgId, key, range);
                context.put("销售负责人", key);
                context.put("核心指标", sales.getKpi());
                context.put("合同阶段分布", sales.getStageDist());
                context.put("近12月合同趋势", sales.getContractTrend());
            }
            case KIND_REGION -> {
                RegionDetailResponse region = businessScreenService.regionDetail(orgId, key, range);
                context.put("省级区域", key);
                context.put("核心指标", region.getKpi());
                context.put("区域销售分布", region.getSalesDist());
            }
            case KIND_STAGE -> {
                StageDetailResponse stage = businessScreenService.stageDetail(orgId, key, range);
                context.put("合同阶段", stage.getStageName());
                context.put("合同数", stage.getCount());
                context.put("合同金额", stage.getTotalAmount());
            }
            default -> {
                var overview = businessScreenService.overview(orgId, range);
                context.put("核心指标", overview.getKpi());
                context.put("转化漏斗", overview.getFunnel());
                context.put("合同阶段分布", overview.getContractStageDist());
                context.put("近12月合同趋势", overview.getContractTrend());
            }
        }
        return context;
    }

    private String label(String kind) {
        return switch (kind) {
            case KIND_SALES -> "销售个人业绩";
            case KIND_REGION -> "区域业绩";
            case KIND_STAGE -> "合同阶段转化";
            default -> "企业整体经营";
        };
    }

    /** 同步调用模型拿到完整文本（免费不计量，不做额度校验/记账）。 */
    private String callModel(AgentModel model, List<LlmMessage> messages) throws Exception {
        LlmProvider provider = llmProviderFactory.get(model.getProvider());
        LlmChatRequest llmRequest = new LlmChatRequest();
        llmRequest.setModel(model.getModelName());
        llmRequest.setBaseUrl(model.getApiUrl());
        llmRequest.setApiKey(model.getApiKey());
        agentModelService.applyModelParams(llmRequest, model);
        llmRequest.setMessages(messages);

        StringBuilder sb = new StringBuilder();
        provider.chatStream(llmRequest, sb::append);
        return sb.toString();
    }
}
