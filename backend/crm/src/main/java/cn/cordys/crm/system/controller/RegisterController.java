package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.system.dto.request.RegisterApplicationPageRequest;
import cn.cordys.crm.system.dto.request.RegisterApplyRequest;
import cn.cordys.crm.system.dto.request.RegisterApproveRequest;
import cn.cordys.crm.system.dto.request.RegisterRejectRequest;
import cn.cordys.crm.system.dto.request.RegisterStatusRequest;
import cn.cordys.crm.system.dto.response.RegisterApplicationResponse;
import cn.cordys.crm.system.dto.response.RegisterStatusResponse;
import cn.cordys.crm.system.service.RegisterService;
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
 * 注册控制器，负责自助注册申请与后台审核。
 */
@RestController
@RequestMapping("/register")
@Tag(name = "注册")
public class RegisterController {

    @Resource
    private RegisterService registerService;

    /**
     * 提交注册申请（公开）
     */
    @PostMapping("/apply")
    @Operation(summary = "提交注册申请")
    public void apply(@Validated @RequestBody RegisterApplyRequest request) {
        registerService.apply(request);
    }

    /**
     * 查询注册审核状态（公开）
     */
    @PostMapping("/status")
    @Operation(summary = "查询注册审核状态")
    public RegisterStatusResponse status(@Validated @RequestBody RegisterStatusRequest request) {
        return registerService.status(request.getPhone());
    }

    /**
     * 注册申请单分页列表（管理端）
     */
    @PostMapping("/application/list")
    @Operation(summary = "注册申请-列表查询")
    @RequiresPermissions(PermissionConstants.SYS_REGISTER_AUDIT_READ)
    public Pager<List<RegisterApplicationResponse>> pageList(@Validated @RequestBody RegisterApplicationPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, registerService.pageList(request));
    }

    /**
     * 注册申请单详情（管理端）
     */
    @GetMapping("/application/detail/{id}")
    @Operation(summary = "注册申请-详情")
    @RequiresPermissions(PermissionConstants.SYS_REGISTER_AUDIT_READ)
    public RegisterApplicationResponse detail(@PathVariable String id) {
        return registerService.detail(id);
    }

    /**
     * 注册申请审核通过（管理端）
     */
    @PostMapping("/application/approve")
    @Operation(summary = "注册申请-审核通过")
    @RequiresPermissions(PermissionConstants.SYS_REGISTER_AUDIT_APPROVE)
    public void approve(@Validated @RequestBody RegisterApproveRequest request) {
        registerService.approve(request, SessionUtils.getUserId());
    }

    /**
     * 注册申请驳回（管理端）
     */
    @PostMapping("/application/reject")
    @Operation(summary = "注册申请-驳回")
    @RequiresPermissions(PermissionConstants.SYS_REGISTER_AUDIT_REJECT)
    public void reject(@Validated @RequestBody RegisterRejectRequest request) {
        registerService.reject(request, SessionUtils.getUserId());
    }
}
