package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.InternalRole;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.CityManagerPerformanceRequest;
import cn.cordys.crm.platform.dto.response.CityManagerPerformanceOverviewResponse;
import cn.cordys.crm.platform.service.CityManagerPerformanceService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台城市经理业绩看板（admin 可看全部/过滤，city_manager 只能看本人）
 */
@RestController
@RequestMapping("/platform/city-manager/performance")
@Tag(name = "平台城市经理业绩看板")
public class CityManagerPerformanceController {

    @Resource
    private CityManagerPerformanceService cityManagerPerformanceService;
    @Resource
    private RoleService roleService;

    @PostMapping("/overview")
    @CsPermission(PermissionConstants.CITY_MANAGER_DASHBOARD_READ)
    @Operation(summary = "城市经理业绩看板-总览")
    public CityManagerPerformanceOverviewResponse overview(@Validated @RequestBody CityManagerPerformanceRequest request) {
        String userId = SessionUtils.getUserId();
        // city_manager 强制只看本人业绩，防止越权查看他人
        if (roleService.getRoleIdsByUserId(userId).contains(InternalRole.CITY_MANAGER.getValue())) {
            request.setManagerId(userId);
        }
        return cityManagerPerformanceService.overview(request);
    }
}
