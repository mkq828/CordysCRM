package cn.cordys.crm.ai.leadcrawl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 线索爬取-企业搜索请求。
 */
@Data
public class LeadCrawlSearchRequest {

    @NotBlank
    @Schema(description = "搜索关键字（企业名称/行业/地区等）")
    private String keyword;
}
