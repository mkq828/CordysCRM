package cn.cordys.crm.platform.dto.response;

import cn.cordys.crm.platform.domain.PlatformContract;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 平台合同响应（含已解析的扫描件列表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformContractResponse extends PlatformContract {

    @Schema(description = "扫描件列表")
    private List<PlatformAttachmentResponse> attachmentList;

    @Schema(description = "签约经理姓名")
    private String signManagerName;

    @Schema(description = "跟进经理姓名")
    private String followManagerName;
}
