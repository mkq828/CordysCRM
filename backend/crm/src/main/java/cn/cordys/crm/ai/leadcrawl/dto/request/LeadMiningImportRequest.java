package cn.cordys.crm.ai.leadcrawl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 潜客挖掘导入请求：把一条联网搜索结果导入线索池。
 */
@Data
public class LeadMiningImportRequest {

    @Schema(description = "公司名称")
    private String name;

    @Schema(description = "来源链接")
    private String url;

    @Schema(description = "搜索结果摘要")
    private String snippet;
}
