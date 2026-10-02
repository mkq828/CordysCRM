package cn.cordys.crm.ai.script.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 话术检索推荐结果：改写后的话术 + 出处 + 原文对照。
 */
@Data
public class ScriptRecommendResponse {

    @Schema(description = "话术标题")
    private String title;

    @Schema(description = "改写后的话术")
    private String content;

    @Schema(description = "出处/来源")
    private String source;

    @Schema(description = "原话术内容（对照）")
    private String originalContent;
}
