package cn.cordys.crm.platform.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台回款分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformPaymentRecordPageRequest extends BasePageRequest {

    @Schema(description = "平台合同ID")
    private String contractId;

    @Schema(description = "核销状态(PENDING/DONE)")
    private String verificationStatus;
}
