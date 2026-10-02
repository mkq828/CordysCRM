package cn.cordys.crm.ai.script.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 销售话术库（ai_sales_script 表，租户级）。租户沉淀自己的销售话术/常见异议应答，供 AI 检索改写。
 */
@Data
@Table(name = "ai_sales_script")
public class AiSalesScript extends BaseModel {

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "话术分类（开场白/价格异议/竞品对比/促单逼单等）")
    private String category;

    @Schema(description = "话术标题")
    private String title;

    @Schema(description = "话术内容")
    private String content;

    @Schema(description = "出处/来源")
    private String source;
}
