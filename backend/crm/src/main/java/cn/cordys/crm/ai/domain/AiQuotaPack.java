package cn.cordys.crm.ai.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 额度包商品：admin 维护，按功能卖「单项额度包」；featureCode 为空串 = 全功能通用包。
 */
@Data
@Table(name = "ai_quota_pack")
public class AiQuotaPack extends BaseModel {

    @Schema(description = "AI 功能编码，空串=全功能通用包")
    private String featureCode;

    @Schema(description = "额度包名称")
    private String name;

    @Schema(description = "次数")
    private Integer calls;

    @Schema(description = "价格(分)")
    private Long priceFen;

    @Schema(description = "状态:1启用 0停用")
    private Integer status;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
