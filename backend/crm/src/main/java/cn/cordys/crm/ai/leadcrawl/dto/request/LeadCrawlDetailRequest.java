package cn.cordys.crm.ai.leadcrawl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 线索爬取-拉取企业详情请求。
 */
@Data
public class LeadCrawlDetailRequest {

    @NotEmpty
    @Schema(description = "企业全称列表")
    private List<String> names;
}
