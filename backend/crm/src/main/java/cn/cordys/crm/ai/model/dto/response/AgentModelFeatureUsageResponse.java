package cn.cordys.crm.ai.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 功能 → 模型用途的只读矩阵响应（模型设置「模型应用场景」）。
 */
@Data
public class AgentModelFeatureUsageResponse {

    @Schema(description = "功能编码（sys_feature.feature_code）")
    private String featureCode;

    @Schema(description = "功能名称")
    private String featureName;

    @Schema(description = "备注（预留/只读聚合等，非必填）")
    private String remark;

    @Schema(description = "模型用途列表（功能多用途时多项，无模型功能为空）")
    private List<Usage> usages;

    /** 单个用途及解析到的模型候选链（首选在前，含降级链） */
    @Data
    public static class Usage {

        @Schema(description = "用途路由编码")
        private String route;

        @Schema(description = "用途名称")
        private String routeName;

        @Schema(description = "解析到的模型列表，空表示未配置")
        private List<ModelRef> models;
    }

    /** 模型摘要（不暴露密钥等敏感字段） */
    @Data
    public static class ModelRef {

        @Schema(description = "模型展示名称")
        private String displayName;

        @Schema(description = "模型名称（API identifier）")
        private String modelName;

        @Schema(description = "模型供应商")
        private String provider;
    }
}
