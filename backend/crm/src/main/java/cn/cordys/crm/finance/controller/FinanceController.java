package cn.cordys.crm.finance.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.finance.dto.request.FinancePageRequest;
import cn.cordys.crm.finance.dto.request.FinanceRevokeRequest;
import cn.cordys.crm.finance.dto.request.FinanceVerifyRequest;
import cn.cordys.crm.finance.dto.response.FinanceCustomerGroupResponse;
import cn.cordys.crm.finance.dto.response.FinanceOverviewResponse;
import cn.cordys.crm.finance.service.FinanceService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 财务模块
 */
@Tag(name = "财务模块")
@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Resource
    private FinanceService financeService;
    @Resource
    private ContractPaymentRecordService contractPaymentRecordService;

    @PostMapping("/overview")
    @CsPermission(PermissionConstants.FINANCE_READ)
    @Operation(summary = "应收概览统计")
    public FinanceOverviewResponse overview() {
        return financeService.overview(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @CsPermission(PermissionConstants.FINANCE_READ)
    @Operation(summary = "应收列表（客户分组/合同/回款明细）")
    public PagerWithOption<List<FinanceCustomerGroupResponse>> page(@Validated @RequestBody FinancePageRequest request) {
        return financeService.page(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/verify")
    @RequiresPermissions(PermissionConstants.FINANCE_VERIFY)
    @Operation(summary = "回款核销")
    public void verify(@Validated @RequestBody FinanceVerifyRequest request) {
        contractPaymentRecordService.verify(request, SessionUtils.getUserId());
    }

    @PostMapping("/revoke")
    @RequiresPermissions(PermissionConstants.FINANCE_VERIFY)
    @Operation(summary = "回款核销撤回")
    public void revoke(@Validated @RequestBody FinanceRevokeRequest request) {
        contractPaymentRecordService.revoke(request, SessionUtils.getUserId());
    }
}
