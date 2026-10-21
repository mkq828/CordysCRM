package cn.cordys.crm.system.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
import cn.cordys.crm.system.domain.SysErrorLog;
import cn.cordys.crm.system.dto.request.SysErrorLogRequest;
import cn.cordys.crm.system.service.SysErrorLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统错误日志查询，仅平台超级管理员可访问。
 */
@RestController
@RequestMapping("/system/error-log")
@Tag(name = "系统错误日志")
public class SysErrorLogController {

    @Resource
    private SysErrorLogService sysErrorLogService;

    private void checkAdmin() {
        if (!PlatformSessionUtils.isAdmin()) {
            throw new GenericException("仅平台超级管理员可查看系统错误日志");
        }
    }

    @PostMapping("/list")
    @Operation(summary = "系统错误日志-列表查询(仅admin)")
    public Pager<List<SysErrorLog>> list(@RequestBody SysErrorLogRequest request) {
        checkAdmin();
        return sysErrorLogService.list(request);
    }
}
