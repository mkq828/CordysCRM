package cn.cordys.crm.platform.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台合同：平台与租户之间的签约合同（平台账·应收）
 */
@Data
@Table(name = "platform_contract")
public class PlatformContract extends BaseModel {

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "租户主体-企业全称(快照)")
    private String orgName;

    @Schema(description = "租户主体-统一社会信用代码")
    private String creditCode;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系方式")
    private String contactPhone;

    @Schema(description = "租户主体-地址")
    private String address;

    @Schema(description = "套餐版本编码(快照)")
    private String editionCode;

    @Schema(description = "套餐版本名称(快照)")
    private String editionName;

    @Schema(description = "合同金额(应收)")
    private BigDecimal amount;

    @Schema(description = "订阅时长(天)")
    private Integer validityDays;

    @Schema(description = "签署方式：OFFLINE线下签/ONLINE线上签")
    private String signType;

    @Schema(description = "签约城市经理(占位，P3填充)")
    private String signManagerId;

    @Schema(description = "当前跟进城市经理(占位，P3填充)")
    private String followManagerId;

    @Schema(description = "状态：DRAFT草稿/PENDING_SIGN待签署/COMPLETED已完成/ARCHIVED已归档/VOIDED已作废")
    private String status;

    @Schema(description = "扫描件附件ID(逗号分隔)")
    private String attachmentIds;

    @Schema(description = "备注")
    private String remark;
}
