package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 安全事件留证。记录疑似恶意入侵/渗透/攻击的请求证据，供报案取证。
 */
@Data
@Table(name = "sys_security_event")
public class SecurityEvent {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "租户组织ID(未登录攻击为空)")
    private String organizationId;

    @Schema(description = "事件类型(sql_injection/xss/path_traversal/scanner_ua/error_flood)")
    private String eventType;

    @Schema(description = "直连IP(TCP对端，可信)")
    private String remoteAddr;

    @Schema(description = "客户端IP(X-Forwarded-For首段，可伪造)")
    private String clientIp;

    @Schema(description = "完整X-Forwarded-For转发链")
    private String forwardChain;

    @Schema(description = "IP归属地")
    private String ipRegion;

    @Schema(description = "HTTP方法")
    private String method;

    @Schema(description = "请求URI(含query，攻击载荷所在)")
    private String uri;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "Referer")
    private String referer;

    @Schema(description = "HTTP响应状态码")
    private Integer httpStatus;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "事件描述")
    private String detail;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;
}
