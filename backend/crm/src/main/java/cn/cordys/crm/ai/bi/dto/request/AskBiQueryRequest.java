package cn.cordys.crm.ai.bi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 问数 BI（功能 12）查询请求：一句大白话数据问题。
 */
@Data
public class AskBiQueryRequest {

    @NotBlank(message = "请输入您想问的数据问题")
    @Schema(description = "大白话数据问题，如「上月各客户回款金额 Top10」")
    private String question;
}
