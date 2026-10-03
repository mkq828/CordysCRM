package cn.cordys.crm.ai.conversation.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.conversation.dto.request.AgentConversationPageRequest;
import cn.cordys.crm.ai.conversation.dto.request.AgentConversationRenameRequest;
import cn.cordys.crm.ai.conversation.dto.response.AgentConversationDetailResponse;
import cn.cordys.crm.ai.conversation.dto.response.AgentConversationResponse;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 会话记录接口（登录即可用）。承载 AI 中心左侧历史：列表 / 详情回放 / 删除 / 重命名。
 */
@RestController
@RequestMapping("/agent-conversation")
@Tag(name = "AI 会话记录")
public class AgentConversationController {

    @Resource
    private AgentConversationService agentConversationService;

    @PostMapping("/page")
    @Operation(summary = "分页获取当前用户某能力下的会话列表")
    public Pager<List<AgentConversationResponse>> page(@RequestBody AgentConversationPageRequest request) {
        return agentConversationService.page(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取会话详情（会话信息 + 消息列表）")
    public AgentConversationDetailResponse detail(@PathVariable String id) {
        return agentConversationService.detail(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除会话及其全部消息")
    public void delete(@PathVariable String id) {
        agentConversationService.delete(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PutMapping("/rename/{id}")
    @Operation(summary = "重命名会话")
    public void rename(@PathVariable String id, @RequestBody AgentConversationRenameRequest request) {
        agentConversationService.rename(id, request.getTitle(), OrganizationContext.getOrganizationId(),
                SessionUtils.getUserId());
    }
}
