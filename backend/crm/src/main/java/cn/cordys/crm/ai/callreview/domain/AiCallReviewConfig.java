package cn.cordys.crm.ai.callreview.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 智能通话复盘回调配置（租户级）：appKey/secretKey 用于通用回调鉴权，
 * fieldMapping 把供应商回调里的字段名映射到 CRM 标准字段。密钥第一版明文存储（与系统其余第三方配置一致）。
 */
@Data
@Table(name = "ai_call_review_config")
public class AiCallReviewConfig extends BaseModel {

    @Schema(description = "租户组织ID(唯一)")
    private String organizationId;

    @Schema(description = "回调AppKey(拼进回调地址)")
    private String appKey;

    @Schema(description = "回调签名密钥(HMAC-SHA256)")
    private String secretKey;

    @Schema(description = "标准字段->供应商字段名映射JSON")
    private String fieldMapping;

    @Schema(description = "是否启用回调")
    private Boolean enable;
}
