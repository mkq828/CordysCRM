package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 平台回款核销请求
 */
@Data
public class PlatformVerifyRequest {

    @Schema(description = "回款记录ID")
    private String id;

    @Schema(description = "核销备注")
    private String remark;

    @Schema(description = "收款证明附件ID列表")
    private List<String> proofAttachmentIds;
}
