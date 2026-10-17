package cn.cordys.crm.customer.tag.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 客户标签字典（租户级）。标签名在租户内唯一，AI 自动打标签与人工打标签共用此字典。
 */
@Data
@Table(name = "customer_tag")
public class CustomerTag extends BaseModel {

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "标签名")
    private String name;

    @Schema(description = "标签颜色（十六进制）")
    private String color;

    @Schema(description = "标签分类")
    private String category;

    @Schema(description = "是否内置标签（1=内置，不可删）")
    private Boolean builtIn;
}
