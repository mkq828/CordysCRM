package cn.cordys.crm.dashboard.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 大客户久未联系预警的原始行（负责人姓名由服务层富化）。
 */
@Data
public class StaleBigCustomerRow {

    private String customerId;

    private String customerName;

    private String owner;

    private Long followTime;

    private BigDecimal contractAmount;
}
