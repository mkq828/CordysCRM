package cn.cordys.crm.ai.dmprofile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 已绑定的私信平台账号（不回显 Cookie）。
 */
@Data
public class DmAccountResponse {

    @Schema(description = "账号主键")
    private String id;

    @Schema(description = "平台")
    private String platform;

    @Schema(description = "账号昵称")
    private String accountName;

    @Schema(description = "账号ID(抖音号)")
    private String uniqueId;

    @Schema(description = "绑定状态: LOGGED_IN/CRAWLING/EXPIRED/FAILED")
    private String status;

    @Schema(description = "最近采集时间(毫秒)")
    private Long lastCrawlTime;

    @Schema(description = "最近失败原因(状态为 FAILED/EXPIRED 时返回)")
    private String lastError;
}
