package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台附件响应
 */
@Data
public class PlatformAttachmentResponse {

    @Schema(description = "附件ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "大小")
    private Long size;
}
