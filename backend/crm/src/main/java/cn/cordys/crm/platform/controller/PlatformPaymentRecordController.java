package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.PlatformPaymentRecordPageRequest;
import cn.cordys.crm.platform.dto.request.PlatformPaymentRecordSaveRequest;
import cn.cordys.crm.platform.dto.request.PlatformRevokeRequest;
import cn.cordys.crm.platform.dto.request.PlatformVerifyRequest;
import cn.cordys.crm.platform.dto.response.PlatformPaymentRecordResponse;
import cn.cordys.crm.platform.service.PlatformPaymentRecordService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台回款（平台账·现金流，仅 admin）
 */
@RestController
@RequestMapping("/platform/payment-record")
@Tag(name = "平台回款")
public class PlatformPaymentRecordController {

    @Resource
    private PlatformPaymentRecordService platformPaymentRecordService;

    @PostMapping("/list")
    @CsPermission(PermissionConstants.CITY_MANAGER_PAYMENT_READ)
    @Operation(summary = "平台回款-列表")
    public Pager<List<PlatformPaymentRecordResponse>> pageList(@Validated @RequestBody PlatformPaymentRecordPageRequest request) {
        return platformPaymentRecordService.pageList(request);
    }

    @PostMapping("/add")
    @CsPermission(PermissionConstants.CITY_MANAGER_PAYMENT_WRITE)
    @Operation(summary = "平台回款-新增")
    public void add(@Validated @RequestBody PlatformPaymentRecordSaveRequest request) {
        platformPaymentRecordService.add(request, SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @CsPermission(PermissionConstants.CITY_MANAGER_PAYMENT_WRITE)
    @Operation(summary = "平台回款-编辑")
    public void update(@Validated @RequestBody PlatformPaymentRecordSaveRequest request) {
        platformPaymentRecordService.update(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @CsPermission(PermissionConstants.CITY_MANAGER_PAYMENT_WRITE)
    @Operation(summary = "平台回款-删除")
    public void delete(@PathVariable String id) {
        platformPaymentRecordService.delete(id, SessionUtils.getUserId());
    }

    @PostMapping("/verify")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_VERIFY)
    @Operation(summary = "平台回款-核销（首笔核销自动开通企业版）")
    public void verify(@Validated @RequestBody PlatformVerifyRequest request) {
        platformPaymentRecordService.verify(request, SessionUtils.getUserId());
    }

    @PostMapping("/revoke")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_VERIFY)
    @Operation(summary = "平台回款-撤回核销")
    public void revoke(@Validated @RequestBody PlatformRevokeRequest request) {
        platformPaymentRecordService.revoke(request, SessionUtils.getUserId());
    }
}
