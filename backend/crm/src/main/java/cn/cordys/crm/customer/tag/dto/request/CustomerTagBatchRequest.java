package cn.cordys.crm.customer.tag.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量查询客户已打标签的请求（客户列表页一次性取回当前页各客户的标签）。
 */
@Data
public class CustomerTagBatchRequest {

    @Schema(description = "客户ID列表")
    private List<String> customerIds;
}
