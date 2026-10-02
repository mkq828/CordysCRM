package cn.cordys.crm.ai.script.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.script.dto.request.AiSalesScriptPageRequest;
import cn.cordys.crm.ai.script.dto.request.AiSalesScriptRetrieveRequest;
import cn.cordys.crm.ai.script.dto.request.AiSalesScriptSaveRequest;
import cn.cordys.crm.ai.script.dto.response.AiSalesScriptResponse;
import cn.cordys.crm.ai.script.dto.response.ScriptRecommendResponse;
import cn.cordys.crm.ai.script.service.AiSalesScriptService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 销售话术库接口（登录即可用，检索走额度框架门控）。
 */
@RestController
@RequestMapping("/agent/script")
@Tag(name = "销售话术库")
public class AiSalesScriptController {

    @Resource
    private AiSalesScriptService aiSalesScriptService;

    @PostMapping("/page")
    @Operation(summary = "话术库-分页列表")
    public Pager<List<AiSalesScriptResponse>> page(@RequestBody AiSalesScriptPageRequest request) {
        return aiSalesScriptService.page(request, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/categories")
    @Operation(summary = "话术库-分类选项")
    public List<String> categories() {
        return aiSalesScriptService.categories(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @Operation(summary = "话术库-新增话术")
    public void add(@RequestBody AiSalesScriptSaveRequest request) {
        aiSalesScriptService.add(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @Operation(summary = "话术库-更新话术")
    public void update(@RequestBody AiSalesScriptSaveRequest request) {
        aiSalesScriptService.update(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "话术库-删除话术")
    public void delete(@PathVariable String id) {
        aiSalesScriptService.delete(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/retrieve")
    @Operation(summary = "话术库-检索并改写推荐话术")
    public List<ScriptRecommendResponse> retrieve(@RequestBody AiSalesScriptRetrieveRequest request) {
        return aiSalesScriptService.retrieve(request, OrganizationContext.getOrganizationId());
    }
}
