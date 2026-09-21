package cn.cordys.crm.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务合同聚合行（mapper 内部使用）
 */
@Data
public class FinanceContractRow {

    private String contractId;

    private String contractName;

    private String contractNumber;

    private String customerId;

    private String customerName;

    private BigDecimal amount;

    private BigDecimal verifiedAmount;

    private Long createTime;

    private Long customerCreateTime;
}
