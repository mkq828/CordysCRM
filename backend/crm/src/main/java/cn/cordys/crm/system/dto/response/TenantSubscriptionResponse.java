package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.platform.dto.response.PlatformContractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 租户订阅信息（套餐 + 当前生效合同），个人中心自助展示。
 */
@Data
public class TenantSubscriptionResponse {

    @Schema(description = "套餐版本编码(BASIC/PRO/ENTERPRISE)")
    private String version;

    @Schema(description = "套餐版本名称")
    private String versionName;

    @Schema(description = "套餐状态(FREE试用/ACTIVE生效/EXPIRED到期)")
    private String status;

    @Schema(description = "到期时间(毫秒)")
    private Long expireTime;

    @Schema(description = "剩余天数(宽限期内为负)")
    private Long remainDays;

    @Schema(description = "是否处于宽限期")
    private Boolean inGrace;

    @Schema(description = "当前生效合同(无合同为 null)")
    private PlatformContractResponse contract;
}
