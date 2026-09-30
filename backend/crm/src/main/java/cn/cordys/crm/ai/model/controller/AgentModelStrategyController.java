package cn.cordys.crm.ai.model.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.ai.model.dto.request.AgentModelStrategySaveRequest;
import cn.cordys.crm.ai.model.dto.response.AgentModelStrategyResponse;
import cn.cordys.crm.ai.model.service.AgentModelStrategyService;
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
 * AI 模型路由策略（全局单行，agent_model_strategy 表）。
 */
@RestController
@RequestMapping("/agent-model-strategy")
@Tag(name = "AI 模型路由策略")
public class AgentModelStrategyController {

    @Resource
    private AgentModelStrategyService agentModelStrategyService;

    @GetMapping("/get")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    @Operation(summary = "模型路由策略-查询")
    public AgentModelStrategyResponse get() {
        return agentModelStrategyService.get();
    }

    @PostMapping("/config")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    @Operation(summary = "模型路由策略-更新")
    public void config(@RequestBody AgentModelStrategySaveRequest request) {
        agentModelStrategyService.config(request.getChatModels(), request.getTaskModels(), request.getFallback());
    }
}
