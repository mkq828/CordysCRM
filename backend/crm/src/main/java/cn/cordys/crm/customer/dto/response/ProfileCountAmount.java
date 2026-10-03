package cn.cordys.crm.customer.dto.response;

import cn.cordys.common.util.BigDecimalNoTrailingZeroSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户画像 360° 聚合：数量 + 金额合计（商机/订单/合同复用）
 */
@Data
public class ProfileCountAmount {

    private Long count;

    @JsonSerialize(using = BigDecimalNoTrailingZeroSerializer.class)
    private BigDecimal amount;
}
