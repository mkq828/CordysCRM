package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.system.dto.request.EditionSaveRequest;
import cn.cordys.crm.system.dto.request.FeatureSaveRequest;
import cn.cordys.crm.system.dto.response.EditionResponse;
import cn.cordys.crm.system.dto.response.FeatureResponse;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 版本与套餐配置控制器（复用付费用户权限）
 */
@RestController
@RequestMapping("/edition")
@Tag(name = "版本与套餐配置")
public class EditionController {

    @Resource
    private EditionService editionService;

    /**
     * 版本列表（全量）
     */
    @GetMapping("/list")
    @Operation(summary = "版本-列表")
    @RequiresPermissions(PermissionConstants.PAID_USER_READ)
    public List<EditionResponse> listEditions() {
        return editionService.listEditions();
    }

    /**
     * 启用中的版本（开通/续费/平台合同下拉用）
     * 城市经理建合同需要读套餐下拉，故在付费用户读权限之外额外放行 CITY_MANAGER_CONTRACT_READ。
     */
    @GetMapping("/options")
    @Operation(summary = "版本-启用列表")
    @RequiresPermissions(value = { PermissionConstants.PAID_USER_READ,
            PermissionConstants.CITY_MANAGER_CONTRACT_READ }, logical = Logical.OR)
    public List<EditionResponse> listEnabledEditions() {
        return editionService.listEnabledEditions();
    }

    /**
     * 保存版本（新增/编辑，含功能归属）
     */
    @PostMapping("/save")
    @Operation(summary = "版本-保存")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void saveEdition(@Validated @RequestBody EditionSaveRequest request) {
        if (StringUtils.isBlank(request.getId())) {
            editionService.addEdition(request, SessionUtils.getUserId());
        } else {
            editionService.updateEdition(request, SessionUtils.getUserId());
        }
    }

    /**
     * 删除版本
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "版本-删除")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void deleteEdition(@PathVariable("id") String id) {
        editionService.deleteEdition(id);
    }

    /**
     * 功能列表（全量）
     */
    @GetMapping("/feature/list")
    @Operation(summary = "功能-列表")
    @RequiresPermissions(PermissionConstants.PAID_USER_READ)
    public List<FeatureResponse> listFeatures() {
        return editionService.listFeatures();
    }

    /**
     * 保存功能（新增/编辑）
     */
    @PostMapping("/feature/save")
    @Operation(summary = "功能-保存")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void saveFeature(@Validated @RequestBody FeatureSaveRequest request) {
        if (StringUtils.isBlank(request.getId())) {
            editionService.addFeature(request, SessionUtils.getUserId());
        } else {
            editionService.updateFeature(request, SessionUtils.getUserId());
        }
    }

    /**
     * 删除功能
     */
    @PostMapping("/feature/delete/{id}")
    @Operation(summary = "功能-删除")
    @RequiresPermissions(PermissionConstants.PAID_USER_UPDATE)
    public void deleteFeature(@PathVariable("id") String id) {
        editionService.deleteFeature(id);
    }

    /**
     * 版本归属的功能ID集合
     */
    @GetMapping("/mapping/{editionId}")
    @Operation(summary = "版本-功能归属查询")
    @RequiresPermissions(PermissionConstants.PAID_USER_READ)
    public List<String> listFeatureIdsByEditionId(@PathVariable("editionId") String editionId) {
        return editionService.listFeatureIdsByEditionId(editionId);
    }
}
