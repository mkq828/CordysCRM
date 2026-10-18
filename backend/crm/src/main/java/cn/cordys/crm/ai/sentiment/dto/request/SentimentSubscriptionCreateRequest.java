package cn.cordys.crm.ai.sentiment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新建舆情监控订阅请求。
 */
@Data
public class SentimentSubscriptionCreateRequest {

    @Schema(description = "监控对象(品牌/客户/供应商名)，如「华为」")
    private String target;

    @Schema(description = "补充关键词，如「涉诉」")
    private String keyword;
}
