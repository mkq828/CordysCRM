package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 问题反馈
 * <p>
 * 普通用户在前端「反馈问题」悬浮按钮提交，admin 在后台查看并下载 .md 修复。
 * </p>
 */
@Data
@Table(name = "sys_bug_report")
public class BugReport {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "提交人id")
    private String userId;

    @Schema(description = "提交人名称")
    private String userName;

    @Schema(description = "角色")
    private String roles;

    @Schema(description = "页面路由")
    private String route;

    @Schema(description = "系统版本")
    private String version;

    @Schema(description = "浏览器User-Agent")
    private String userAgent;

    @Schema(description = "屏幕分辨率")
    private String screen;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "复现步骤")
    private String steps;

    @Schema(description = "截图base64")
    private String screenshot;

    @Schema(description = "最近错误JSON")
    private String recentErrors;

    @Schema(description = "失败接口JSON")
    private String failedRequests;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "状态 PENDING/DONE")
    private String status;

    @Schema(description = "处理时间")
    private Long handleTime;

    @Schema(description = "处理人")
    private String handleUser;

    @Schema(description = "处理备注")
    private String handleRemark;

    @Schema(description = "提交时间")
    private Long createTime;
}
