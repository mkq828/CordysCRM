package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.PlatformConfigRequest;
import cn.cordys.crm.platform.dto.response.PlatformConfigResponse;
import cn.cordys.crm.platform.service.PlatformConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台收款主体配置（仅 admin）
 */
@RestController
@RequestMapping("/platform/config")
@Tag(name = "平台收款设置")
public class PlatformConfigController {

    @Resource
    private PlatformConfigService platformConfigService;

    @GetMapping
    @CsPermission(PermissionConstants.ADMIN_FINANCE_READ)
    @Operation(summary = "平台收款设置-查询")
    public PlatformConfigResponse get() {
        return platformConfigService.get();
    }

    @PostMapping
    @CsPermission(PermissionConstants.ADMIN_FINANCE_WRITE)
    @Operation(summary = "平台收款设置-保存")
    public void update(@Validated @RequestBody PlatformConfigRequest request) {
        platformConfigService.update(request);
    }
}
