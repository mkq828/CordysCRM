package cn.cordys.crm.ai.leadcrawl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 潜客挖掘导入结果。
 */
@Data
public class LeadMiningImportResponse {

    @Schema(description = "新建线索 ID")
    private String clueId;

    @Schema(description = "新建线索名称")
    private String clueName;
}
