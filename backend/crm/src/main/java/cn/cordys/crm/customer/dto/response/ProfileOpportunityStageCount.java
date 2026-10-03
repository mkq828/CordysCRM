package cn.cordys.crm.customer.dto.response;

import lombok.Data;

/**
 * 客户画像 360° 聚合：商机阶段分布（阶段名 + 数量）
 */
@Data
public class ProfileOpportunityStageCount {

    private String stage;

    private Long count;
}
