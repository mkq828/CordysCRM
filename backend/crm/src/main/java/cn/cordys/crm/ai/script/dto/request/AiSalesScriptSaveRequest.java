package cn.cordys.crm.ai.script.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 销售话术新增/更新请求。
 */
@Data
public class AiSalesScriptSaveRequest {

    @Schema(description = "话术ID（更新时必填）")
    private String id;

    @Schema(description = "话术分类")
    private String category;

    @Schema(description = "话术标题")
    private String title;

    @Schema(description = "话术内容")
    private String content;

    @Schema(description = "出处/来源")
    private String source;
}
