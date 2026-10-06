package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.customer.dto.response.CustomerProfileResponse;
import cn.cordys.crm.customer.service.CustomerProfileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 只读工具：查单个客户全景（跟进/商机/订单/合同/回款统计 + AI 洞察），内部已做行级权限校验。 */
@Service
public class GetCustomerOverviewTool implements AiAgentTool {

    @Resource
    private CustomerProfileService customerProfileService;

    @Override
    public String name() {
        return "get_customer_overview";
    }

    @Override
    public String description() {
        return "查单个客户的 360° 全景：基础信息、跟进次数、商机/订单/合同数量与金额、回款率、已有 AI 洞察。"
                + "参数 customerId=客户 id（先由 search_customers 查到）。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        Object cid = args == null ? null : args.get("customerId");
        String customerId = cid == null ? null : String.valueOf(cid);
        if (customerId == null || customerId.isBlank()) {
            return JSON.toJSONString(Map.of("error", "缺少 customerId 参数"));
        }
        CustomerProfileResponse profile = customerProfileService.profile(customerId, userId, orgId);
        if (profile == null || !Boolean.TRUE.equals(profile.getAvailable())) {
            return JSON.toJSONString(Map.of("error", "无权查看该客户，或该租户未开通客户画像（专业版）"));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerName", profile.getCustomerName());
        result.put("ownerName", profile.getOwnerName());
        result.put("follow", profile.getFollow());
        result.put("opportunity", profile.getOpportunity());
        result.put("opportunityStages", profile.getOpportunityStages());
        result.put("order", profile.getOrder());
        result.put("contract", profile.getContract());
        result.put("paidAmount", profile.getPaidAmount());
        result.put("paymentRate", profile.getPaymentRate());
        result.put("aiInsight", profile.getAiInsight());
        return JSON.toJSONString(result);
    }
}
