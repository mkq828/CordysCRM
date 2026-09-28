package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.PlatformInvoiceActionRequest;
import cn.cordys.crm.platform.dto.request.PlatformInvoicePageRequest;
import cn.cordys.crm.platform.dto.request.PlatformInvoiceSaveRequest;
import cn.cordys.crm.platform.dto.response.PlatformInvoiceResponse;
import cn.cordys.crm.platform.service.PlatformInvoiceService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台发票（平台账·开票，仅 admin）
 */
@RestController
@RequestMapping("/platform/invoice")
@Tag(name = "平台发票")
public class PlatformInvoiceController {

    @Resource
    private PlatformInvoiceService platformInvoiceService;

    @PostMapping("/list")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_READ)
    @Operation(summary = "平台发票-列表")
    public Pager<List<PlatformInvoiceResponse>> pageList(@Validated @RequestBody PlatformInvoicePageRequest request) {
        return platformInvoiceService.pageList(request);
    }

    @PostMapping("/add")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_WRITE)
    @Operation(summary = "平台发票-新增")
    public void add(@Validated @RequestBody PlatformInvoiceSaveRequest request) {
        platformInvoiceService.add(request, SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_WRITE)
    @Operation(summary = "平台发票-编辑")
    public void update(@Validated @RequestBody PlatformInvoiceSaveRequest request) {
        platformInvoiceService.update(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_WRITE)
    @Operation(summary = "平台发票-删除")
    public void delete(@PathVariable String id) {
        platformInvoiceService.delete(id);
    }

    @PostMapping("/invoice")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_WRITE)
    @Operation(summary = "平台发票-开票")
    public void invoice(@Validated @RequestBody PlatformInvoiceActionRequest request) {
        platformInvoiceService.invoice(request, SessionUtils.getUserId());
    }

    @PostMapping("/void")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_WRITE)
    @Operation(summary = "平台发票-作废")
    public void voidInvoice(@Validated @RequestBody PlatformInvoiceActionRequest request) {
        platformInvoiceService.voidInvoice(request, SessionUtils.getUserId());
    }
}
