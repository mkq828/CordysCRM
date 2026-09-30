package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.request.UiDisplaySaveRequest;
import cn.cordys.crm.system.dto.response.UiDisplayParamResponse;
import cn.cordys.crm.system.service.UiDisplayService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 界面设置（登录页 / 平台主页的图标、logo、背景、slogan 等）。
 */
@RestController
@RequestMapping("/ui/display")
@Tag(name = "界面设置")
public class UiDisplayController {

    @Resource
    private UiDisplayService uiDisplayService;

    @GetMapping("/info")
    @Operation(summary = "界面设置-查询（登录页匿名可读，展示平台级登录页外观）")
    public List<UiDisplayParamResponse> info() {
        return uiDisplayService.info(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/save")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    @Operation(summary = "界面设置-保存")
    public void save(@RequestPart("request") List<UiDisplaySaveRequest> request,
                     @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        uiDisplayService.save(request, files, SessionUtils.getUserId());
    }

    @GetMapping("/preview")
    @Operation(summary = "界面设置-文件预览")
    public ResponseEntity<org.springframework.core.io.Resource> preview(@RequestParam("paramKey") String paramKey,
                                            @RequestParam("organizationId") String organizationId) {
        return uiDisplayService.preview(paramKey, organizationId);
    }
}
