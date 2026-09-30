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

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "用户每日调用限制")
    private Long userDailyLimit;

    @Schema(description = "全局每日调用限制")
    private Long globalDailyLimit;

    @Schema(description = "模型参数（JSON）")
    private String modelParams;
}
