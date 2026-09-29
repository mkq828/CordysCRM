package cn.cordys.crm.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台端 AI 额度-租户额度列表请求
 */
@Data
public class AiTenantQuotaListRequest {

    @Schema(description = "关键字(租户名/组织ID 模糊匹配)")
    private String keyword;
}
