package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.system.dto.request.TenantPlanConfigRequest;
import cn.cordys.crm.system.dto.request.TenantPlanDemoRequest;
import cn.cordys.crm.system.dto.request.TenantPlanDetailRequest;
import cn.cordys.crm.system.dto.request.TenantPlanOpenRequest;
import cn.cordys.crm.system.dto.request.TenantPlanPageRequest;
import cn.cordys.crm.system.dto.request.TenantPlanToggleRequest;
import cn.cordys.crm.system.dto.request.TenantPlanUpgradeRequest;
import cn.cordys.crm.system.dto.response.TenantPlanConfigResponse;
import cn.cordys.crm.system.dto.response.TenantPlanDetailResponse;
import cn.cordys.crm.system.dto.response.TenantPlanResponse;
import cn.cordys.crm.system.service.TenantPlanService;
import cn.cordys.security.SessionUtils;
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
 * 租户套餐控制器（付费用户管理）
 */
@RestController
@RequestMapping("/tenant/plan")
@Tag(name = "付费用户")
public class TenantPlanController {

    @Resource
    private TenantPlanService tenantPlanService;

    /**
     * 租户套餐分页列表（管理端）
     */
    @PostMapping("/list")
    @Operation(summary = "付费用户-列表查询")
    @RequiresPermissions(PermissionConstants.PAID_USER_READ)
    public Pager<List<TenantPlanResponse>> pageList(@Validated @RequestBody TenantPlanPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, tenantPlanService.pageList(request));
    }

    /**
     * 租户详情（管理端）
     */
    @PostMapping("/detail")
    @Operation(summary = "付费用户-租户详情")
    @RequiresPermissions(PermissionConstants.PAID_USER_READ)
    public TenantPlanDetailResponse detail(@Validated @RequestBody TenantPlanDetailRequest request) {
        return tenantPlanService.detail(request);
    }

    /**
     * 开通/续费套餐（管理端）
     */
    @PostMapping("/open")
    @Operation(summary = "付费用户-开通/续费")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void open(@Validated @RequestBody TenantPlanOpenRequest request) {
        tenantPlanService.open(request, SessionUtils.getUserId());
    }

    /**
     * 个人版升级企业版（管理端）
     */
    @PostMapping("/upgrade")
    @Operation(summary = "付费用户-升级企业版")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void upgrade(@Validated @RequestBody TenantPlanUpgradeRequest request) {
        tenantPlanService.upgrade(request, SessionUtils.getUserId());
    }

    /**
     * 付费租户账号启用/禁用（管理端）
     */
    @PostMapping("/toggle")
    @Operation(summary = "付费用户-启用/禁用")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void toggle(@Validated @RequestBody TenantPlanToggleRequest request) {
        tenantPlanService.toggle(request, SessionUtils.getUserId());
    }

    /**
     * 付费租户演示标记（管理端）
     */
    @PostMapping("/demo")
    @Operation(summary = "付费用户-演示标记")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void toggleDemo(@Validated @RequestBody TenantPlanDemoRequest request) {
        tenantPlanService.toggleDemo(request.getOrganizationId(), request.getDemo(), SessionUtils.getUserId());
    }

    /**
     * 全局配置查询（管理端）
     */
    @GetMapping("/config")
    @Operation(summary = "付费用户-全局配置查询")
    @RequiresPermissions(PermissionConstants.PAID_USER_READ)
    public TenantPlanConfigResponse getConfig() {
        return tenantPlanService.getConfig();
    }

    /**
     * 全局配置更新（管理端）
     */
    @PostMapping("/config")
    @Operation(summary = "付费用户-全局配置更新")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void updateConfig(@Validated @RequestBody TenantPlanConfigRequest request) {
        tenantPlanService.updateConfig(request);
    }
}
