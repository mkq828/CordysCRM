package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 安全事件分页查询请求。安全事件可能来自未登录攻击，不按租户隔离，仅 admin 查询。
 */
@Data
public class SecurityEventRequest extends BasePageRequest {

    @Schema(description = "事件类型(sql_injection/xss/path_traversal/scanner_ua/error_flood)")
    private String eventType;

    @Schema(description = "IP(模糊匹配直连IP或客户端IP)")
    private String ip;

    @Schema(description = "开始时间(毫秒)")
    private Long startTime;

    @Schema(description = "结束时间(毫秒)")
    private Long endTime;
}
