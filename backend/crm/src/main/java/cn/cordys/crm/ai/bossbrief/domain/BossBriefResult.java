package cn.cordys.crm.ai.bossbrief.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 老板晨报结果：主题搜索到的当日要闻，按 URL 去重落库。
 */
@Data
@Table(name = "ai_boss_brief_result")
public class BossBriefResult extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "关注主题ID")
    private String topicId;

    @Schema(description = "要闻标题")
    private String title;

    @Schema(description = "来源链接")
    private String url;

    @Schema(description = "摘要")
    private String snippet;

    @Schema(description = "正文(可空)")
    private String content;
}
