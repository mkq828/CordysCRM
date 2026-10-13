package cn.cordys.crm.ai.replica.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 爆款复刻变体：一条参考视频产出的一个换包装变体（换钩子/标题/字幕样式/比例，画面声音复用原视频）。
 */
@Data
@Table(name = "ai_replica_variant")
public class AiReplicaVariant extends BaseModel {

    @Schema(description = "所属任务ID")
    private String taskId;

    @Schema(description = "租户组织ID")
    private String organizationId;

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
