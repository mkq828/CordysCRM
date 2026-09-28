package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台合同保存请求（新增/编辑）
 */
@Data
public class PlatformContractSaveRequest {

    @Schema(description = "主键(编辑时传)")
    private String id;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "统一社会信用代码")
    private String creditCode;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系方式")
    private String contactPhone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "套餐版本编码")
    private String editionCode;

    @Schema(description = "合同金额(应收)")
    private BigDecimal amount;

    @Schema(description = "订阅时长(天)")
    private Integer validityDays;

    @Schema(description = "签署方式：OFFLINE线下签/ONLINE线上签")
    private String signType;

    @Schema(description = "签约城市经理ID(可选，首次分配时自动回填租户签约经理)")
    private String signManagerId;

    @Schema(description = "扫描件附件ID(逗号分隔)")
    private String attachmentIds;

    @Schema(description = "备注")
    private String remark;
}
