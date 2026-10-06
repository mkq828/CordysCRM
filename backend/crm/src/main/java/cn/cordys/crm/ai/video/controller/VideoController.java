package cn.cordys.crm.ai.video.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.video.dto.request.VideoGenerateRequest;
import cn.cordys.crm.ai.video.dto.response.VideoTaskResponse;
import cn.cordys.crm.ai.video.service.VideoGenerateService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 短视频生成接口（功能 7 MVP，专业版+ G3 门控，脚本走 LLM 挂 G2 额度）。
 * 生成走异步任务，前端每 3s 轮询 /tasks 感知进度。
 */
@RestController
@RequestMapping("/agent/video")
@Tag(name = "短视频生成")
public class VideoController {

    @Resource
    private VideoGenerateService videoGenerateService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通短视频生成")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_VIDEO);
    }

    @PostMapping("/tasks")
    @Operation(summary = "提交短视频生成任务（异步）")
    public VideoTaskResponse create(@Valid @RequestBody VideoGenerateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        VideoTaskResponse response = videoGenerateService.submit(request, orgId, userId);
        videoGenerateService.generateAsync(response.getId(), orgId, userId, request.getExtraText());
        return response;
    }

    @GetMapping("/tasks")
    @Operation(summary = "任务列表（轮询进度）")
    public List<VideoTaskResponse> list() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.list(orgId);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "任务详情")
    public VideoTaskResponse get(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.get(orgId, id);
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除任务（同时清理附件）")
    public void delete(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        videoGenerateService.delete(orgId, id);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_VIDEO)) {
            throw new GenericException("当前版本未开通短视频生成，请升级至专业版");
        }
    }
}
