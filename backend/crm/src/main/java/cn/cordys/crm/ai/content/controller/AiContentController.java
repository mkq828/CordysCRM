package cn.cordys.crm.ai.content.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.content.dto.request.AiContentGenerateRequest;
import cn.cordys.crm.ai.content.dto.response.AiContentGenerateResponse;
import cn.cordys.crm.ai.content.service.AiContentGenerateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 获客内容接口（登录即可用，额度框架门控）。
 */
@RestController
@RequestMapping("/agent/content")
@Tag(name = "AI 获客内容")
public class AiContentController {

    @Resource
    private AiContentGenerateService aiContentGenerateService;

    @PostMapping("/generate")
    @Operation(summary = "生成获客内容（选题/文案/配图文案）")
    public AiContentGenerateResponse generate(@RequestBody AiContentGenerateRequest request) {
        return aiContentGenerateService.generate(OrganizationContext.getOrganizationId(), request);
    }
}
