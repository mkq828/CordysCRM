package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.system.dto.request.TenantPlanApplicationApproveRequest;
import cn.cordys.crm.system.dto.request.TenantPlanApplicationPageRequest;
import cn.cordys.crm.system.dto.response.TenantPlanApplicationResponse;
import cn.cordys.crm.system.service.TenantPlanApplicationService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户续费/升级申请（管理端核销）
 */
@RestController
@RequestMapping("/tenant/plan/application")
@Tag(name = "续费/升级申请")
public class TenantPlanApplicationController {

    @Resource
    private TenantPlanApplicationService tenantPlanApplicationService;

    /**
     * 申请分页列表（管理端）
     */
    @PostMapping("/list")
    @Operation(summary = "续费/升级申请-列表")
    @RequiresPermissions(PermissionConstants.PAID_USER_READ)
    public Pager<List<TenantPlanApplicationResponse>> pageList(@Validated @RequestBody TenantPlanApplicationPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, tenantPlanApplicationService.pageList(request));
    }

    /**
     * 核销（确认收款后开通/升级套餐）
     */
    @PostMapping("/approve")
    @Operation(summary = "续费/升级申请-核销")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void approve(@Validated @RequestBody TenantPlanApplicationApproveRequest request) {
        tenantPlanApplicationService.approve(request, SessionUtils.getUserId());
    }

    /**
     * 驳回申请
     */
    @PostMapping("/cancel")
    @Operation(summary = "续费/升级申请-驳回")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void cancel(@Validated @RequestBody TenantPlanApplicationApproveRequest request) {
        tenantPlanApplicationService.cancel(request, SessionUtils.getUserId());
    }
}
