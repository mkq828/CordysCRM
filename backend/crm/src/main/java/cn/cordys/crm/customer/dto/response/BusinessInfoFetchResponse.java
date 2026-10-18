package cn.cordys.crm.customer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 获取客户工商信息的结果：已填充字段 + 数据来源。
 * <p>
 * source 取值：{@code qcc}（企查查权威源）、{@code web}（联网检索 + 模型提取，可能不准确）、{@code mixed}（两者都有）。
 * 前端按 source 区分提示口径，联网兜底要提示「可能不准确/可能是同名公司」。
 * </p>
 */
@Data
public class BusinessInfoFetchResponse {

    @Schema(description = "本次成功填充的字段展示名")
    private List<String> filled;

    @Schema(description = "数据来源：qcc / web / mixed")
    private String source;
}
