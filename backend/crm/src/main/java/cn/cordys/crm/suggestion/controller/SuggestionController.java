package cn.cordys.crm.suggestion.controller;

import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.suggestion.dto.request.SuggestionAddRequest;
import cn.cordys.crm.suggestion.dto.request.SuggestionCommentAddRequest;
import cn.cordys.crm.suggestion.dto.request.SuggestionPageRequest;
import cn.cordys.crm.suggestion.dto.request.SuggestionStatusRequest;
import cn.cordys.crm.suggestion.dto.response.SuggestionCommentDTO;
import cn.cordys.crm.suggestion.dto.response.SuggestionDTO;
import cn.cordys.crm.suggestion.dto.response.SuggestionVoteResponse;
import cn.cordys.crm.suggestion.service.SuggestionService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 需求反馈控制器（公开，所有登录用户可看/投票/讨论）。
 */
@RestController
@RequestMapping("/suggestion")
@Tag(name = "需求反馈")
public class SuggestionController {

    @Resource
    private SuggestionService suggestionService;

    @PostMapping("/page")
    @Operation(summary = "建议分页列表")
    public Pager<List<SuggestionDTO>> page(@Validated @RequestBody SuggestionPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, suggestionService.page(request));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "建议详情")
    public SuggestionDTO detail(@PathVariable String id) {
        return suggestionService.detail(id);
    }

    @PostMapping("/add")
    @Operation(summary = "提交建议")
    public Map<String, String> add(@Validated @RequestBody SuggestionAddRequest request) {
        return Map.of("id", suggestionService.add(request, SessionUtils.getUserId()));
    }

    @PostMapping("/vote/{id}")
    @Operation(summary = "点赞/取消点赞")
    public SuggestionVoteResponse vote(@PathVariable String id) {
        return suggestionService.vote(id, SessionUtils.getUserId());
    }

    @GetMapping("/comment/list/{suggestionId}")
    @Operation(summary = "评论列表")
    public List<SuggestionCommentDTO> listComments(@PathVariable String suggestionId) {
        return suggestionService.listComments(suggestionId);
    }

    @PostMapping("/comment/add")
    @Operation(summary = "发表评论")
    public void addComment(@Validated @RequestBody SuggestionCommentAddRequest request) {
        suggestionService.addComment(request, SessionUtils.getUserId());
    }

    @PostMapping("/status")
    @Operation(summary = "更新状态（仅admin）")
    public void updateStatus(@Validated @RequestBody SuggestionStatusRequest request) {
        suggestionService.updateStatus(request, SessionUtils.getUserId());
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除建议（作者或admin）")
    public void delete(@PathVariable String id) {
        suggestionService.delete(id, SessionUtils.getUserId());
    }
}
