package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同回款记录
 * @author song-cc-rock
 */

@Data
@Table(name = "contract_payment_record")
public class ContractPaymentRecord extends BaseModel {

	@Schema(description = "回款记录名称")
	private String name;

	@Schema(description = "回款编号")
	private String no;

	@Schema(description = "负责人")
	private String owner;

	@Schema(description = "合同ID")
	private String contractId;

	@Schema(description = "回款计划ID")
	private String paymentPlanId;

	@Schema(description = "回款金额")
	private BigDecimal recordAmount;

	@Schema(description = "回款时间")
	private Long recordEndTime;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "核销状态 PENDING待核销/DONE已完成")
	private String verificationStatus;

	@Schema(description = "核销人")
	private String verifyUser;

	@Schema(description = "核销时间")
	private Long verifyTime;

	@Schema(description = "核销备注")
	private String verifyRemark;

	@Schema(description = "收款证明附件ID(逗号分隔)")
	private String verifyProof;

	@Schema(description = "撤回人")
	private String revokeUser;

	@Schema(description = "撤回时间")
	private Long revokeTime;

	@Schema(description = "撤回备注")
	private String revokeRemark;
}
