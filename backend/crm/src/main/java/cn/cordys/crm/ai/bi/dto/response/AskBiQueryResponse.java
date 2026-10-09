package cn.cordys.crm.ai.bi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 问数 BI（功能 12）查询结果：标题 + 图表类型 + 列名 + 数据行。
 */
@Data
public class AskBiQueryResponse {

    @Schema(description = "结果标题（LLM 生成或按指标/维度兜底）")
    private String title;

    @Schema(description = "图表类型：number(单值)/bar/line/pie/table")
    private String chartType;

    @Schema(description = "列名（维度 + 指标）")
    private List<String> columns;

    @Schema(description = "数据行（每行与 columns 对齐）")
    private List<List<Object>> rows;
}
