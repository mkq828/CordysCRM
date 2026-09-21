package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 问题反馈提交请求（普通用户）
 * <p>
 * 提交人 / 组织由后端从登录上下文获取，不信任前端传入。
 * </p>
 */
@Data
public class BugReportRequest {

    @Schema(description = "问题描述")
    @NotBlank(message = "问题描述不能为空")
    private String description;

    @Schema(description = "复现步骤")
    private String steps;

    @Schema(description = "截图base64")
    private String screenshot;

    @Schema(description = "页面路由")
    private String route;

    @Schema(description = "系统版本")
    private String version;

    @Schema(description = "角色")
    private String roles;

    @Schema(description = "浏览器User-Agent")
    private String userAgent;

    @Schema(description = "屏幕分辨率")
    private String screen;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "最近错误JSON")
    private String recentErrors;

    @Schema(description = "失败接口JSON")
    private String failedRequests;

    @Schema(description = "链路追踪ID")
    private String traceId;
}
