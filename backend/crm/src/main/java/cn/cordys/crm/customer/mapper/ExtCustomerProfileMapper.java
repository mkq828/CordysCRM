package cn.cordys.crm.customer.mapper;

import cn.cordys.crm.customer.dto.response.ProfileCountAmount;
import cn.cordys.crm.customer.dto.response.ProfileFollowSummary;
import cn.cordys.crm.customer.dto.response.ProfileOpportunityStageCount;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户画像 360° 只读聚合（跟进/商机/订单/合同/回款），按客户维度汇总。
 */
public interface ExtCustomerProfileMapper {

    ProfileFollowSummary followSummary(@Param("customerId") String customerId, @Param("orgId") String orgId);

    ProfileCountAmount opportunitySummary(@Param("customerId") String customerId, @Param("orgId") String orgId);

    List<ProfileOpportunityStageCount> opportunityStageDistribution(@Param("customerId") String customerId,
                                                                    @Param("orgId") String orgId);

    ProfileCountAmount orderSummary(@Param("customerId") String customerId, @Param("orgId") String orgId);

    ProfileCountAmount contractSummary(@Param("customerId") String customerId, @Param("orgId") String orgId);

    BigDecimal paidAmount(@Param("customerId") String customerId, @Param("orgId") String orgId);
}
