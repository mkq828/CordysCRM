package cn.cordys.crm.ai.leadcrawl.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadMiningImportRequest;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadMiningSearchRequest;
import cn.cordys.crm.ai.leadcrawl.dto.response.LeadMiningImportResponse;
import cn.cordys.crm.ai.search.DeepSeekSearchClient;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.ClueAddRequest;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 潜客挖掘：按「行业 + 地域 + 关键词」联网搜索潜在客户名单，支持一键导入线索池。
 * 门控与额度复用 {@code lead_crawl}（同为线索/商机类）；搜索按次折算 token 记账。
 */
@Service
@Slf4j
public class LeadMiningService {

    private static final String CLUE_SOURCE_INTERNAL_KEY = "clueSource";
    private static final String CLUE_DEMAND_INTERNAL_KEY = "clueDemand";
    private static final String CLUE_SOURCE_LEAD_MINING = "潜客挖掘";

    @Resource
    private DeepSeekSearchClient deepSeekSearchClient;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private EditionService editionService;
    @Resource
    private ClueService clueService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;

    public List<AdvisorWebSource> search(String orgId, String userId, LeadMiningSearchRequest request) {
        checkAvailable(orgId);
        String query = buildQuery(request);
        if (StringUtils.isBlank(query)) {
            throw new GenericException("请至少填写行业、地域或关键词");
        }

        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }

        List<AdvisorWebSource> sources = deepSeekSearchClient.search(query);
        recordSearch(orgId, userId);
        return sources;
    }

    public LeadMiningImportResponse importToClue(String orgId, String userId, LeadMiningImportRequest request) {
        checkAvailable(orgId);
        if (StringUtils.isBlank(request.getName())) {
            throw new GenericException("请先填写公司名称");
        }

        ClueAddRequest clueRequest = new ClueAddRequest();
        clueRequest.setName(request.getName().trim());

        List<BaseModuleFieldValue> fields = new ArrayList<>();
        String sourceFieldId = resolveFieldId(orgId, CLUE_SOURCE_INTERNAL_KEY);
        if (sourceFieldId != null) {
            fields.add(new BaseModuleFieldValue(sourceFieldId, CLUE_SOURCE_LEAD_MINING));
        }
        String demandFieldId = resolveFieldId(orgId, CLUE_DEMAND_INTERNAL_KEY);
        if (demandFieldId != null) {
            fields.add(new BaseModuleFieldValue(demandFieldId, buildDemand(request)));
        }
        if (!fields.isEmpty()) {
            clueRequest.setModuleFields(fields);
        }

        Clue clue = clueService.add(clueRequest, userId, orgId);
        LeadMiningImportResponse response = new LeadMiningImportResponse();
        response.setClueId(clue == null ? null : clue.getId());
        response.setClueName(clue == null ? null : clue.getName());
        return response;
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_LEAD_CRAWL)) {
            throw new GenericException("当前版本未开通「潜客挖掘」功能");
        }
    }

    private String buildQuery(LeadMiningSearchRequest request) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getIndustry())) {
            parts.add(request.getIndustry().trim());
        }
        if (StringUtils.isNotBlank(request.getRegion())) {
            parts.add(request.getRegion().trim());
        }
        if (StringUtils.isNotBlank(request.getKeyword())) {
            parts.add(request.getKeyword().trim());
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(" ", parts) + " 公司 潜在客户 名录";
    }

    /** 摘要 + 来源链接写入「客户需求」自定义字段，作为线索背景留存 */
    private String buildDemand(LeadMiningImportRequest request) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(request.getSnippet())) {
            sb.append("摘要：").append(request.getSnippet().trim());
        }
        if (StringUtils.isNotBlank(request.getUrl())) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("来源：").append(request.getUrl().trim());
        }
        return sb.toString();
    }

    private String resolveFieldId(String orgId, String internalKey) {
        try {
            return moduleFormService.getAllFields(FormKey.CLUE.getKey(), orgId).stream()
                    .filter(field -> internalKey.equals(field.getInternalKey()))
                    .map(BaseField::getId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("解析线索字段 {} 失败", internalKey, e);
            return null;
        }
    }

    /** 1 次联网搜索折算 token 记账到潜客挖掘功能编码，未配 key 或搜索失败仍记账（按次计费口径一致） */
    private void recordSearch(String orgId, String userId) {
        aiQuotaService.record(orgId, AiQuotaConstant.AI_LEAD_CRAWL, AiQuotaConstant.SEARCH_MODEL_CODE,
                getSearchTokensPerCall(), 0L, userId);
    }

    private long getSearchTokensPerCall() {
        Parameter parameter = parameterMapper.selectByPrimaryKey(AiQuotaConstant.PARAM_SEARCH_TOKENS_PER_CALL);
        if (parameter == null || StringUtils.isBlank(parameter.getParamValue())) {
            return AiQuotaConstant.DEFAULT_SEARCH_TOKENS_PER_CALL;
        }
        try {
            return Long.parseLong(parameter.getParamValue().trim());
        } catch (NumberFormatException e) {
            return AiQuotaConstant.DEFAULT_SEARCH_TOKENS_PER_CALL;
        }
    }

    private boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }

    private String blockedMessage(String status) {
        if (AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)) {
            return "调用过于频繁，请稍后再试";
        }
        if (AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status)) {
            return "AI 服务暂不可用，请联系管理员";
        }
        return "AI 额度已用完，请加购或升级";
    }
}
