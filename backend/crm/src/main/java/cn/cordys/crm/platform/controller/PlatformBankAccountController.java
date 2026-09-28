package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.PlatformBankAccountSaveRequest;
import cn.cordys.crm.platform.dto.response.PlatformBankAccountResponse;
import cn.cordys.crm.platform.service.PlatformBankAccountService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台收款账号：列表 city_manager 可读（登记回款时自动带出收款账户），维护（保存）仅 admin。
 */
@RestController
@RequestMapping("/platform/bank-account")
@Tag(name = "平台收款账号")
public class PlatformBankAccountController {

    @Resource
    private PlatformBankAccountService platformBankAccountService;

    @GetMapping("/list")
    @CsPermission(PermissionConstants.CITY_MANAGER_PAYMENT_READ)
    @Operation(summary = "平台收款账号-列表")
    public List<PlatformBankAccountResponse> list() {
        return platformBankAccountService.list();
    }

    @PostMapping("/save")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_WRITE)
    @Operation(summary = "平台收款账号-保存")
    public void save(@Validated @RequestBody List<PlatformBankAccountSaveRequest> requests) {
        platformBankAccountService.save(requests, SessionUtils.getUserId());
    }
}
