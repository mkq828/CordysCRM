package cn.cordys.crm.ai.callreview.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 通话复盘回调配置：回调地址 + 密钥 + 字段映射。
 */
@Data
public class CallReviewConfigResponse {

    @Schema(description = "回调AppKey")
    private String appKey;

    @Schema(description = "回调签名密钥")
    private String secretKey;

    @Schema(description = "回调地址(供外呼供应商配置)")
    private String callbackUrl;

    @Schema(description = "标准字段->供应商字段名映射")
    private Map<String, String> fieldMapping;

    @Schema(description = "是否启用回调")
    private Boolean enable;
}
