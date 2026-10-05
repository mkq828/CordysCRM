package cn.cordys.crm.ai.leadcrawl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 线索爬取-导入线索池结果。
 */
@Data
public class LeadCrawlImportResponse {

    @Schema(description = "成功导入条数")
    private int successCount;

    @Schema(description = "导入失败的企业名称（如联系人电话重复、数据缺失等）")
    private List<String> failedNames;
}
