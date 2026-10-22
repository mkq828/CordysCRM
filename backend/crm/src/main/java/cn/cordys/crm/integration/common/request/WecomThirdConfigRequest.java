package cn.cordys.crm.integration.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WecomThirdConfigRequest {

    @Schema(description = "应用ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String agentId;

    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;

    @Schema(description = "企业ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String corpId;

    @Schema(description = "同步用户", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean startEnable;

    @Schema(description = "群机器人 Webhook 地址（填了就走群机器人推送，无需企业ID/应用ID/密钥/可信IP）")
    private String webhookUrl;
}
