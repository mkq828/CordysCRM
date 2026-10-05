package cn.cordys.crm.integration.qcc.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 线索爬取（功能 10）：企查查企业工商公开信息缓存（平台级，跨租户共享去重）。
 * 企业详情只在此沉淀一份，命中的租户直接从缓存返回，不再重复调用企查查。
 */
@Data
@Table(name = "qcc_enterprise_info")
public class QccEnterpriseInfo extends BaseModel {

    @Schema(description = "企业名称")
    private String name;

    @Schema(description = "纳税人识别号")
    private String taxNo;

    @Schema(description = "统一社会信用代码")
    private String creditCode;

    @Schema(description = "法定代表人")
    private String legalPerson;

    @Schema(description = "经营状态")
    private String status;

    @Schema(description = "注册资本")
    private String registerCapi;

    @Schema(description = "公司规模(人数)")
    private String personScope;

    @Schema(description = "企业规模")
    private String scale;

    @Schema(description = "国标行业")
    private String industry;

    @Schema(description = "所属省份")
    private String areaProvince;

    @Schema(description = "所属城市")
    private String areaCity;

    @Schema(description = "注册电话")
    private String registerPhone;

    @Schema(description = "注册地址")
    private String address;

    @Schema(description = "开户银行")
    private String bankName;

    @Schema(description = "开户行账号")
    private String bankAccount;

    @Schema(description = "工商注册号")
    private String regNo;

    @Schema(description = "数据来源(qcc)")
    private String source;
}
