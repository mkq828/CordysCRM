package cn.cordys.crm.finance.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 财务应收列表请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FinancePageRequest extends BasePageRequest {

    @Schema(description = "关键词（客户名称/合同名称/合同编号）")
    private String keyword;
}
