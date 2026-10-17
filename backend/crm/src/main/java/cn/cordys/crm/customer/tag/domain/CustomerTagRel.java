package cn.cordys.crm.customer.tag.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 客户-标签关联。同一客户与同一标签至多一条（UNIQUE 约束），来源区分人工与 AI。
 */
@Data
@Table(name = "customer_tag_rel")
public class CustomerTagRel extends BaseModel {

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "标签ID")
    private String tagId;

    @Schema(description = "来源（manual/AI）")
    private String source;
}
