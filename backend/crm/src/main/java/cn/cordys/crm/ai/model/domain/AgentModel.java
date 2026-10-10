package cn.cordys.crm.ai.model.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 模型配置（agent_model 表，租户级）。
 */
@Data
@Table(name = "agent_model")
public class AgentModel extends BaseModel {

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "模型展示名称")
    private String displayName;

    @Schema(description = "模型名称（API identifier）")
    private String modelName;

    @Schema(description = "模型供应商")
    private String provider;

    @Schema(description = "API请求地址")
    private String apiUrl;

    @Schema(description = "API Key")
    private String apiKey;

    @Schema(description = "火山语音 AppID（豆包语音三件套鉴权）")
    private String appId;

    @Schema(description = "火山语音 AccessToken（豆包语音三件套鉴权）")
    private String accessToken;

    @Schema(description = "火山语音 SecretKey（豆包语音三件套鉴权，HMAC256 签名用）")
    private String secretKey;

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "用户每日调用限制")
    private Long userDailyLimit;

    @Schema(description = "全局每日调用限制")
    private Long globalDailyLimit;

    @Schema(description = "模型参数（JSON）")
    private String modelParams;

    @Schema(description = "声音克隆模型名（阿里云声音复刻 target_model，留空回退默认）")
    private String voiceCloneModel;

    @Schema(description = "模型描述（短视频风格页展示）")
    private String description;

    @Schema(description = "是否推荐模型（1=推荐）")
    private Boolean recommended;
}
