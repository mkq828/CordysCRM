package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.BankAccount;
import cn.cordys.crm.contract.dto.request.BankAccountAddRequest;
import cn.cordys.crm.contract.dto.request.BankAccountPageRequest;
import cn.cordys.crm.contract.dto.request.BankAccountUpdateRequest;
import cn.cordys.crm.contract.dto.response.BankAccountListResponse;
import cn.cordys.crm.contract.service.BankAccountService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "收款账户")
@RestController
@RequestMapping("/contract/bank-account")
public class BankAccountController {

    @Resource
    private BankAccountService bankAccountService;


    @GetMapping("/module/form")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return bankAccountService.getBusinessFormConfig();
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.BANK_ACCOUNT_ADD)
    @Operation(summary = "创建")
    public BankAccount add(@Validated @RequestBody BankAccountAddRequest request) {
        return bankAccountService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.BANK_ACCOUNT_UPDATE)
    @Operation(summary = "更新")
    public BankAccount update(@Validated @RequestBody BankAccountUpdateRequest request) {
        return bankAccountService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.BANK_ACCOUNT_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        bankAccountService.delete(id);
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.BANK_ACCOUNT_READ)
    @Operation(summary = "列表")
    public Pager<List<BankAccountListResponse>> list(@Validated @RequestBody BankAccountPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        return bankAccountService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.BANK_ACCOUNT_READ)
    @Operation(summary = "详情")
    public BankAccountListResponse get(@PathVariable("id") String id) {
        return bankAccountService.get(id);
    }
}
