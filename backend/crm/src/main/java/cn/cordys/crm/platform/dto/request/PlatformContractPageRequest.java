package cn.cordys.crm.platform.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台合同分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformContractPageRequest extends BasePageRequest {

    @Schema(description = "关键字(合同编号/租户名称)")
    private String keyword;

    @Schema(description = "状态(DRAFT/PENDING_SIGN/COMPLETED/ARCHIVED/VOIDED)")
    private String status;
}
