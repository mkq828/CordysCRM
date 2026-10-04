package cn.cordys.crm.ai.ppt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 方案大纲中的一个区块：背景 / 方案 / 架构 / 实施 / 报价 / 风险。
 * 同时用于大纲结果与「转 PPT」入参，前后端原样回传。
 */
@Data
public class PptSection {

    @Schema(description = "区块标识（background/solution/architecture/implementation/pricing/risk）")
    private String key;

    @Schema(description = "区块标题")
    private String title;

    @Schema(description = "区块正文（Markdown）")
    private String content;
}
