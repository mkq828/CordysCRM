package cn.cordys.crm.ai.script.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 销售话术列表/详情响应。
 */
@Data
public class AiSalesScriptResponse {

    @Schema(description = "话术ID")
    private String id;

    @Schema(description = "话术分类")
    private String category;

    @Schema(description = "话术标题")
    private String title;

    @Schema(description = "话术内容")
    private String content;

    @Schema(description = "出处/来源")
    private String source;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "更新人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;
}
