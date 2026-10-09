package cn.cordys.crm.ai.advisor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 军师查数问答请求：一句大白话（例如「张三最近跟进到哪了」）。
 */
@Data
public class AdvisorQueryRequest {

    @Schema(description = "销售的问题（大白话）")
    private String question;

    @Schema(description = "会话 id（可选，不传则新建会话）")
    private String conversationId;
}
