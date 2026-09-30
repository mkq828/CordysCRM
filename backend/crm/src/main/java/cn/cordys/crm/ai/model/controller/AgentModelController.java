package cn.cordys.crm.ai.model.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.model.dto.request.AgentModelPageRequest;
import cn.cordys.crm.ai.model.dto.request.AgentModelSaveRequest;
import cn.cordys.crm.ai.model.dto.response.AgentModelOptionResponse;
import cn.cordys.crm.ai.model.dto.response.AgentModelResponse;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 模型设置（租户级，复用原版「模型设置」界面）。
 */
@RestController
@RequestMapping("/agent-model")
@Tag(name = "AI 模型设置")
public class AgentModelController {

    @Resource
    private AgentModelService agentModelService;

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    @Operation(summary = "模型设置-列表查询")
    public Pager<List<AgentModelResponse>> page(@RequestBody AgentModelPageRequest request) {
        return agentModelService.page(request, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    @Operation(summary = "模型设置-获取模型详情")
    public AgentModelResponse get(@PathVariable String id) {
        return agentModelService.get(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/options")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    @Operation(summary = "模型设置-查询可用模型选项")
    public List<AgentModelOptionResponse> options() {
        return agentModelService.options(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_ADD)
    @Operation(summary = "模型设置-添加模型")
    public void add(@RequestBody AgentModelSaveRequest request) {
        agentModelService.add(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    @Operation(summary = "模型设置-更新模型")
    public void update(@RequestBody AgentModelSaveRequest request) {
        agentModelService.update(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_DELETE)
    @Operation(summary = "模型设置-删除模型")
    public void delete(@PathVariable String id) {
        agentModelService.delete(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/switch/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    @Operation(summary = "模型设置-切换模型状态")
    public void switchEnable(@PathVariable String id) {
        agentModelService.switchEnable(id, OrganizationContext.getOrganizationId());
    }
}
