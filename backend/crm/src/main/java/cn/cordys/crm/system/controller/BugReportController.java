package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.request.BugReportListRequest;
import cn.cordys.crm.system.dto.request.BugReportRequest;
import cn.cordys.crm.system.dto.response.BugReportDetailResponse;
import cn.cordys.crm.system.dto.response.BugReportResponse;
import cn.cordys.crm.system.service.BugReportService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问题反馈控制器
 * <p>
 * 普通用户提交，admin 查看列表 / 详情并下载 .md 修复。
 * </p>
 */
@RestController
@RequestMapping("/bug-report")
@Tag(name = "问题反馈")
public class BugReportController {

    @Resource
    private BugReportService bugReportService;

    @PostMapping("/submit")
    @Operation(summary = "提交问题反馈（所有登录用户）")
    public void submit(@Validated @RequestBody BugReportRequest request) {
        bugReportService.submit(request);
    }

    @PostMapping("/list")
    @Operation(summary = "问题反馈-列表查询")
    @RequiresPermissions(PermissionConstants.BUG_REPORT_READ)
    public Pager<List<BugReportResponse>> list(@Validated @RequestBody BugReportListRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, bugReportService.list(request, OrganizationContext.getOrganizationId()));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "问题反馈-详情")
    @RequiresPermissions(PermissionConstants.BUG_REPORT_READ)
    public BugReportDetailResponse detail(@PathVariable String id) {
        return bugReportService.getDetail(id, OrganizationContext.getOrganizationId());
    }
}
