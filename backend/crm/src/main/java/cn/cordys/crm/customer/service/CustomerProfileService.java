package cn.cordys.crm.customer.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.domain.AiAnalysisResult;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.search.DeepSeekSearchClient;
import cn.cordys.crm.ai.service.AiAnalysisResultService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerProfileResponse;
import cn.cordys.crm.customer.dto.response.CustomerPublicDynamicResponse;
import cn.cordys.crm.customer.dto.response.ProfileCountAmount;
import cn.cordys.crm.customer.dto.response.ProfileFollowSummary;
import cn.cordys.crm.customer.mapper.ExtCustomerProfileMapper;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 客户画像（功能 13）：客户详情页的 360° 只读聚合 + 会话军师 AI 洞察回读。
 * 查看型：不调模型、不计量，只读 {@code ai_analysis_result} 与客户关联业务数据。
 */
@Service
@Slf4j
public class CustomerProfileService {

    @Resource
    private CustomerService customerService;
    @Resource
    private EditionService editionService;
    @Resource
    private AiAnalysisResultService aiAnalysisResultService;
    @Resource
    private ExtCustomerProfileMapper customerProfileMapper;
    @Resource
    private DeepSeekSearchClient deepSeekSearchClient;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;

    /**
     * 查询客户画像。先做行级权限校验（本人/下属/协作），再聚合 360° 数据与最新 AI 洞察。
     */
    public CustomerProfileResponse profile(String customerId, String userId, String orgId) {
        CustomerGetResponse customer = customerService.getWithDataPermissionCheck(customerId, userId, orgId);

        CustomerProfileResponse response = new CustomerProfileResponse();
        response.setAvailable(editionService.hasFeature(orgId, AiQuotaConstant.AI_CUSTOMER_PROFILE));
        // 私信画像快照独立于「客户画像」升级门槛：采集→线索→客户带入后始终回读，保证闭环不断
        response.setDmProfile(loadDmProfile(customerId));
        if (!Boolean.TRUE.equals(response.getAvailable())) {
            // 未开通专业版+：只返回可用性，不聚合业务数据（前端渲染升级空态）
            return response;
        }

        response.setCustomerName(customer.getName());
        response.setOwnerName(customer.getOwnerName());
        response.setFollowTime(customer.getFollowTime());
        response.setFollowerName(customer.getFollowerName());

        ProfileFollowSummary follow = customerProfileMapper.followSummary(customerId, orgId);
        response.setFollow(follow);

        ProfileCountAmount opportunity = customerProfileMapper.opportunitySummary(customerId, orgId);
        response.setOpportunity(opportunity);
        List<cn.cordys.crm.customer.dto.response.ProfileOpportunityStageCount> stages =
                customerProfileMapper.opportunityStageDistribution(customerId, orgId);
        response.setOpportunityStages(stages);

        response.setOrder(customerProfileMapper.orderSummary(customerId, orgId));

        ProfileCountAmount contract = customerProfileMapper.contractSummary(customerId, orgId);
        response.setContract(contract);

        BigDecimal paidAmount = customerProfileMapper.paidAmount(customerId, orgId);
        response.setPaidAmount(paidAmount == null ? BigDecimal.ZERO : paidAmount);
        response.setPaymentRate(calcPaymentRate(response.getPaidAmount(), contract));

        response.setAiInsight(loadAiInsight(customerId, orgId));
        return response;
    }

    /** 回款率（%）= 已回款 / 合同总额，合同总额为 0 时返回 0 */
    private BigDecimal calcPaymentRate(BigDecimal paidAmount, ProfileCountAmount contract) {
        BigDecimal contractAmount = contract == null || contract.getAmount() == null
                ? BigDecimal.ZERO : contract.getAmount();
        if (contractAmount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return paidAmount.multiply(BigDecimal.valueOf(100))
                .divide(contractAmount, 2, RoundingMode.HALF_UP);
    }

    /** 回读该客户最新一条 AI 分析结论（会话军师沉淀），反序列化成结构化对象 */
    private Object loadAiInsight(String customerId, String orgId) {
        AiAnalysisResult latest = aiAnalysisResultService.latest(orgId, "customer", customerId);
        if (latest == null || StringUtils.isBlank(latest.getResultJson())) {
            return null;
        }
        try {
            return JSON.parseObject(latest.getResultJson());
        } catch (Exception e) {
            log.warn("解析客户画像 AI 洞察失败, customerId={}", customerId, e);
            return null;
        }
    }

    /** 回读线索转客户时带入的私信画像快照，反序列化成结构化对象 */
    private Object loadDmProfile(String customerId) {
        String json = customerService.getDmProfileJson(customerId);
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.warn("解析客户私信画像失败, customerId={}", customerId, e);
            return null;
        }
    }

    /**
     * 客户画像「公开动态」：联网搜索新闻 / 舆情各一轮，返回可点击出处。搜索按次计量（复用 ai_customer_profile 编码），
     * 未配 key 或搜索失败时对应列表为空、不阻断主流程。
     */
    public CustomerPublicDynamicResponse publicDynamics(String customerId, String userId, String orgId) {
        CustomerGetResponse customer = customerService.getWithDataPermissionCheck(customerId, userId, orgId);
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_CUSTOMER_PROFILE)) {
            throw new GenericException("当前版本未开通「客户画像」功能");
        }
        String name = customer.getName();
        if (StringUtils.isBlank(name)) {
            throw new GenericException("请先填写客户名称");
        }

        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }

        String keyword = name.trim();
        List<AdvisorWebSource> news = deepSeekSearchClient.search(keyword + " 公司 新闻 最新动态");
        recordSearch(orgId, userId);
        List<AdvisorWebSource> sentiment = deepSeekSearchClient.search(keyword + " 舆情 负面 投诉");
        recordSearch(orgId, userId);

        CustomerPublicDynamicResponse response = new CustomerPublicDynamicResponse();
        response.setCustomerName(name);
        response.setNewsSources(news);
        response.setSentimentSources(sentiment);
        return response;
    }

    /** 1 次联网搜索折算 token 记账到客户画像功能编码，未配 key 或搜索失败仍记账（按次计费口径一致） */
    private void recordSearch(String orgId, String userId) {
        aiQuotaService.record(orgId, AiQuotaConstant.AI_CUSTOMER_PROFILE, AiQuotaConstant.SEARCH_MODEL_CODE,
                getSearchTokensPerCall(), 0L, userId);
    }

    /** 1 次联网搜索折算 token 数（可配置，默认 1000），配置非法时回退默认值 */
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
