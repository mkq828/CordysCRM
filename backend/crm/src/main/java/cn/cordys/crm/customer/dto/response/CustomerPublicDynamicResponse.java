package cn.cordys.crm.customer.dto.response;

import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 客户画像「公开动态」：联网检索到的新闻 / 舆情来源，供前端渲染可点击出处与可展开正文。
 */
@Data
public class CustomerPublicDynamicResponse {

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "新闻动态来源（联网搜索；未配 key 或失败时为空列表）")
    private List<AdvisorWebSource> newsSources;

    @Schema(description = "舆情动态来源（联网搜索；未配 key 或失败时为空列表）")
    private List<AdvisorWebSource> sentimentSources;
}
