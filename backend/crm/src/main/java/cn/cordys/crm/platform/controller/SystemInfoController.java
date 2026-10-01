package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.platform.dto.request.SystemInfoRequest;
import cn.cordys.crm.platform.dto.response.SystemInfoResponse;
import cn.cordys.crm.platform.service.SystemInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台系统信息（关于弹窗）：运营方名称，系统设置权限维护
 */
@RestController
@RequestMapping("/platform/system-info")
@Tag(name = "平台系统信息")
public class SystemInfoController {

    @Resource
    private SystemInfoService systemInfoService;

    @GetMapping
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    @Operation(summary = "平台系统信息-查询")
    public SystemInfoResponse get() {
        return systemInfoService.get();
    }

    @PostMapping
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    @Operation(summary = "平台系统信息-保存")
    public void update(@RequestBody SystemInfoRequest request) {
        systemInfoService.update(request);
    }
}
