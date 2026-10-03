package cn.cordys.crm.ai.knowledge.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.knowledge.dto.request.AiKnowledgeAskRequest;
import cn.cordys.crm.ai.knowledge.dto.request.AiKnowledgeDocPageRequest;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeAnswerResponse;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeDocResponse;
import cn.cordys.crm.ai.knowledge.service.AiKnowledgeService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 企业知识库接口（登录即可用，问答走额度框架门控）。
 */
@RestController
@RequestMapping("/agent/kb")
@Tag(name = "企业知识库")
public class AiKnowledgeController {

    @Resource
    private AiKnowledgeService aiKnowledgeService;

    @PostMapping("/doc/page")
    @Operation(summary = "知识库-文档分页列表")
    public Pager<List<AiKnowledgeDocResponse>> page(@RequestBody AiKnowledgeDocPageRequest request) {
        return aiKnowledgeService.page(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/doc/upload")
    @Operation(summary = "知识库-上传并解析文档")
    public AiKnowledgeDocResponse upload(@RequestPart("file") MultipartFile file) {
        return aiKnowledgeService.upload(file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/doc/delete/{id}")
    @Operation(summary = "知识库-删除文档")
    public void delete(@PathVariable String id) {
        aiKnowledgeService.delete(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/ask")
    @Operation(summary = "知识库-检索问答")
    public AiKnowledgeAnswerResponse ask(@RequestBody AiKnowledgeAskRequest request) {
        return aiKnowledgeService.ask(request, OrganizationContext.getOrganizationId());
    }
}
