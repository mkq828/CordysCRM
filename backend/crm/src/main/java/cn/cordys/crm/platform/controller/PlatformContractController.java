package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.PlatformContractPageRequest;
import cn.cordys.crm.platform.dto.request.PlatformContractSaveRequest;
import cn.cordys.crm.platform.dto.request.PlatformContractStatusRequest;
import cn.cordys.crm.platform.dto.response.PlatformContractOptionResponse;
import cn.cordys.crm.platform.dto.response.PlatformContractResponse;
import cn.cordys.crm.platform.dto.response.PlatformOrgOptionResponse;
import cn.cordys.crm.platform.service.PlatformContractService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台合同（平台账·应收，仅 admin）
 */
@RestController
@RequestMapping("/platform/contract")
@Tag(name = "平台合同")
public class PlatformContractController {

    @Resource
    private PlatformContractService platformContractService;

    @PostMapping("/list")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_READ)
    @Operation(summary = "平台合同-列表")
    public Pager<List<PlatformContractResponse>> pageList(@Validated @RequestBody PlatformContractPageRequest request) {
        return platformContractService.pageList(request);
    }

    @GetMapping("/get/{id}")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_READ)
    @Operation(summary = "平台合同-详情")
    public PlatformContractResponse get(@PathVariable String id) {
        return platformContractService.get(id);
    }

    @GetMapping("/org-options")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_READ)
    @Operation(summary = "平台合同-租户下拉选项")
    public List<PlatformOrgOptionResponse> orgOptions() {
        return platformContractService.listOrgOptions();
    }

    @GetMapping("/options")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_READ)
    @Operation(summary = "平台合同-下拉选项")
    public List<PlatformContractOptionResponse> options() {
        return platformContractService.listContractOptions();
    }

    @PostMapping("/add")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_WRITE)
    @Operation(summary = "平台合同-新增")
    public void add(@Validated @RequestBody PlatformContractSaveRequest request) {
        platformContractService.add(request, SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_WRITE)
    @Operation(summary = "平台合同-编辑")
    public void update(@Validated @RequestBody PlatformContractSaveRequest request) {
        platformContractService.update(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_WRITE)
    @Operation(summary = "平台合同-删除")
    public void delete(@PathVariable String id) {
        platformContractService.delete(id, SessionUtils.getUserId());
    }

    @PostMapping("/status")
    @CsPermission(PermissionConstants.CITY_MANAGER_CONTRACT_WRITE)
    @Operation(summary = "平台合同-状态流转")
    public void changeStatus(@Validated @RequestBody PlatformContractStatusRequest request) {
        platformContractService.changeStatus(request, SessionUtils.getUserId());
    }
}
