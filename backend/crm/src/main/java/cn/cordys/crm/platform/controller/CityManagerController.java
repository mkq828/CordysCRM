package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.CityManagerAddRequest;
import cn.cordys.crm.platform.dto.request.CityManagerAssignRequest;
import cn.cordys.crm.platform.dto.request.CityManagerOrgEditRequest;
import cn.cordys.crm.platform.dto.request.CityManagerPageRequest;
import cn.cordys.crm.platform.dto.request.CityManagerReassignRequest;
import cn.cordys.crm.platform.dto.response.CityManagerOrgResponse;
import cn.cordys.crm.platform.dto.response.CityManagerResponse;
import cn.cordys.crm.platform.service.CityManagerService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台城市经理（账号 CRUD + 租户归属分配 + 离职二次分配，仅 admin；本人名下租户 city_manager 可读）
 */
@RestController
@RequestMapping("/platform/city-manager")
@Tag(name = "平台城市经理")
public class CityManagerController {

    @Resource
    private CityManagerService cityManagerService;

    @PostMapping("/add")
    @CsPermission(PermissionConstants.CITY_MANAGER_MANAGE)
    @Operation(summary = "城市经理-新增")
    public void add(@Validated @RequestBody CityManagerAddRequest request) {
        cityManagerService.add(request, SessionUtils.getUserId());
    }

    @PostMapping("/list")
    @CsPermission(PermissionConstants.CITY_MANAGER_MANAGE)
    @Operation(summary = "城市经理-列表")
    public Pager<List<CityManagerResponse>> pageList(@Validated @RequestBody CityManagerPageRequest request) {
        return cityManagerService.pageList(request);
    }

    @GetMapping("/disable/{id}")
    @CsPermission(PermissionConstants.CITY_MANAGER_MANAGE)
    @Operation(summary = "城市经理-离职禁用")
    public void disable(@PathVariable String id) {
        cityManagerService.disable(id, SessionUtils.getUserId());
    }

    @PostMapping("/assign")
    @CsPermission(PermissionConstants.CITY_MANAGER_MANAGE)
    @Operation(summary = "城市经理-租户归属分配")
    public void assignOrg(@Validated @RequestBody CityManagerAssignRequest request) {
        cityManagerService.assignOrg(request, SessionUtils.getUserId());
    }

    @PostMapping("/reassign")
    @CsPermission(PermissionConstants.CITY_MANAGER_MANAGE)
    @Operation(summary = "城市经理-离职二次分配")
    public void reassign(@Validated @RequestBody CityManagerReassignRequest request) {
        cityManagerService.reassign(request, SessionUtils.getUserId());
    }

    @GetMapping("/org-options")
    @CsPermission(PermissionConstants.CITY_MANAGER_MANAGE)
    @Operation(summary = "城市经理-租户下拉选项")
    public List<CityManagerOrgResponse> orgOptions() {
        return cityManagerService.orgOptions();
    }

    @GetMapping("/my-orgs")
    @CsPermission(PermissionConstants.CITY_MANAGER_ORG_READ)
    @Operation(summary = "城市经理-本人名下租户")
    public List<CityManagerOrgResponse> myOrgs() {
        return cityManagerService.myOrgs(SessionUtils.getUserId());
    }

    @PostMapping("/org/edit")
    @CsPermission(PermissionConstants.CITY_MANAGER_ORG_EDIT)
    @Operation(summary = "城市经理-编辑本人归属租户基本信息")
    public void editMyOrg(@Validated @RequestBody CityManagerOrgEditRequest request) {
        cityManagerService.editMyOrg(request, SessionUtils.getUserId());
    }
}
