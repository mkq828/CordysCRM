package cn.cordys.crm.ai.leadcrawl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 线索爬取-导入线索池请求。
 */
@Data
public class LeadCrawlImportRequest {

    @NotEmpty
    @Schema(description = "要导入线索池的企业全称列表")
    private List<String> names;
}
