package cn.cordys.crm.ai.replica.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 爆款复刻变体（换皮变体）响应。
 */
@Data
public class ReplicaVariantResponse {

    @Schema(description = "变体ID")
    private String id;

    @Schema(description = "变体序号")
    private Integer variantNo;

    @Schema(description = "钩子文案(开头大字)")
    private String hookText;

    @Schema(description = "标题/主题标签文案")
    private String titleText;

    @Schema(description = "字幕样式标识(预设样式 key)")
    private String subtitleStyle;

    @Schema(description = "输出比例: 9:16/16:9/1:1")
    private String ratio;

    @Schema(description = "状态: PENDING/COMPOSING/DONE/FAILED")
    private String status;

    @Schema(description = "输出变体视频附件ID")
    private String attachmentId;

    @Schema(description = "失败原因")
    private String errorMsg;
}
