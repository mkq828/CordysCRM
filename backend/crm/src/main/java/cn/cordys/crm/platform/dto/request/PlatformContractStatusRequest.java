package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台合同状态流转请求
 */
@Data
public class PlatformContractStatusRequest {

    @Schema(description = "合同ID")
    private String id;

    @Schema(description = "目标状态：DRAFT/PENDING_SIGN/COMPLETED/ARCHIVED/VOIDED")
    private String status;
}
