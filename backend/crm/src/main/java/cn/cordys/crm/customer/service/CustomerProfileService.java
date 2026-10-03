package cn.cordys.crm.customer.service;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.domain.AiAnalysisResult;
import cn.cordys.crm.ai.service.AiAnalysisResultService;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerProfileResponse;
import cn.cordys.crm.customer.dto.response.ProfileCountAmount;
import cn.cordys.crm.customer.dto.response.ProfileFollowSummary;
import cn.cordys.crm.customer.mapper.ExtCustomerProfileMapper;
import cn.cordys.crm.system.service.EditionService;
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

    /**
     * 查询客户画像。先做行级权限校验（本人/下属/协作），再聚合 360° 数据与最新 AI 洞察。
     */
    public CustomerProfileResponse profile(String customerId, String userId, String orgId) {
        CustomerGetResponse customer = customerService.getWithDataPermissionCheck(customerId, userId, orgId);

        CustomerProfileResponse response = new CustomerProfileResponse();
        response.setAvailable(editionService.hasFeature(orgId, AiQuotaConstant.AI_CUSTOMER_PROFILE));
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
}
