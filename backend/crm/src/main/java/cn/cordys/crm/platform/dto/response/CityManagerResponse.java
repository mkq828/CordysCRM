package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 城市经理响应（含名下签约/跟进客户数）
 */
@Data
public class CityManagerResponse {

    @Schema(description = "城市经理ID(=sys_user.id)")
    private String id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "绑定的演示账号用户ID")
    private String demoUserId;

    @Schema(description = "绑定的演示账号姓名")
    private String demoUserName;

    @Schema(description = "绑定的演示账号手机号")
    private String demoPhone;

    @Schema(description = "状态(ENABLED/DISABLED)")
    private String status;

    @Schema(description = "签约客户数")
    private Long signedCount;

    @Schema(description = "跟进客户数")
    private Long followCount;

    @Schema(description = "创建时间")
    private Long createTime;
}
