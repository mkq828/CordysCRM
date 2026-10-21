package cn.cordys.crm.system.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
import cn.cordys.crm.system.domain.JobExecLog;
import cn.cordys.crm.system.dto.request.JobExecLogRequest;
import cn.cordys.crm.system.service.JobExecLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 定时任务执行日志查询，仅平台超级管理员可访问。
 */
@RestController
@RequestMapping("/system/job-exec-log")
@Tag(name = "定时任务执行日志")
public class JobExecLogController {

    @Resource
    private JobExecLogService jobExecLogService;

    private void checkAdmin() {
        if (!PlatformSessionUtils.isAdmin()) {
            throw new GenericException("仅平台超级管理员可查看定时任务执行日志");
        }
    }

    @PostMapping("/list")
    @Operation(summary = "定时任务执行日志-列表查询(仅admin)")
    public Pager<List<JobExecLog>> list(@RequestBody JobExecLogRequest request) {
        checkAdmin();
        return jobExecLogService.list(request);
    }
}
