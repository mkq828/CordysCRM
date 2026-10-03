package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 分析结果沉淀：生成型 AI 能力把结构化结论按业务对象落此表，客户画像 / 数据大屏只读回读。
 */
@Data
@Table(name = "ai_analysis_result")
public class AiAnalysisResult extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "业务对象类型(customer/clue/opportunity)")
    private String bizType;

    @Schema(description = "业务对象ID(客户/线索/商机主键)")
    private String bizId;

    @Schema(description = "来源能力编码(ai_advisor=会话军师)")
    private String featureCode;

    @Schema(description = "标题(如「会话军师分析」)")
    private String title;

    @Schema(description = "结构化结论JSON(会话军师结构化字段)")
    private String resultJson;

    @Schema(description = "生成所用模型")
    private String modelCode;
}
