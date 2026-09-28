package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台回款核销撤回请求
 */
@Data
public class PlatformRevokeRequest {

    @Schema(description = "回款记录ID")
    private String id;

    @Schema(description = "撤回备注")
    private String remark;
}
