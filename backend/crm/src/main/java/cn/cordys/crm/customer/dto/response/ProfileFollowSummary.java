package cn.cordys.crm.customer.dto.response;

import lombok.Data;

/**
 * 客户画像 360° 聚合：跟进汇总（总数 + 最近一条内容/时间）
 */
@Data
public class ProfileFollowSummary {

    private Long count;

    private String latestContent;

    private Long latestFollowTime;
}
