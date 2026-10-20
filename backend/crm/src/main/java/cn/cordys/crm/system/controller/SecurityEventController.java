package cn.cordys.crm.system.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
import cn.cordys.crm.system.domain.SecurityEvent;
import cn.cordys.crm.system.dto.request.SecurityEventRequest;
import cn.cordys.crm.system.service.SecurityEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 安全事件留证查询/导出，仅平台超级管理员可访问。
 */
@RestController
@RequestMapping("/security/event")
@Tag(name = "安全事件")
public class SecurityEventController {

    @Resource
    private SecurityEventService securityEventService;

    private void checkAdmin() {
        if (!PlatformSessionUtils.isAdmin()) {
            throw new GenericException("仅平台超级管理员可查看安全事件");
        }
    }

    @PostMapping("/list")
    @Operation(summary = "安全事件-列表查询(仅admin)")
    public Pager<List<SecurityEvent>> list(@RequestBody SecurityEventRequest request) {
        checkAdmin();
        return securityEventService.list(request);
    }

    @GetMapping("/export")
    @Operation(summary = "安全事件-导出CSV(仅admin)")
    public void export(SecurityEventRequest request, HttpServletResponse response) throws IOException {
        checkAdmin();
        byte[] csv = securityEventService.toCsv(securityEventService.listForExport(request))
                .getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"security-events.csv\"");
        // UTF-8 BOM，避免 Excel 打开中文乱码
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        response.getOutputStream().write(csv);
    }
}
