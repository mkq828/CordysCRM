package cn.cordys.crm.ai.leadcrawl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 线索爬取-企业信息（搜索列表项 / 详情均复用，未拉详情时仅 name 有值）。
 */
@Data
public class LeadCrawlEnterpriseResponse {

    @Schema(description = "企业名称")
    private String name;

    @Schema(description = "是否命中平台缓存（true=直接复用已缓存信息，未再调用企查查）")
    private Boolean fromCache;

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
}
