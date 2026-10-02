package cn.cordys.crm.ai.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.dto.request.SalesAdvisorAnalyzeRequest;
import cn.cordys.crm.ai.dto.response.SalesAdvisorAnalyzeResponse;
import cn.cordys.crm.ai.service.SalesAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 销售会话军师接口（登录即可用，额度框架门控）。
 */
@RestController
@RequestMapping("/agent/advisor")
@Tag(name = "AI 销售会话军师")
public class SalesAdvisorController {

    @Resource
    private SalesAdvisorService salesAdvisorService;

    @PostMapping("/analyze")
    @Operation(summary = "分析销售会话（粘贴文本 / 截图 OCR），返回结构化分析结果")
    public SalesAdvisorAnalyzeResponse analyze(@RequestBody SalesAdvisorAnalyzeRequest request) {
        return salesAdvisorService.analyze(OrganizationContext.getOrganizationId(), request);
    }
}
