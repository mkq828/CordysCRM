package cn.cordys.crm.ai.video.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.video.dto.request.VideoExpandRequest;
import cn.cordys.crm.ai.video.dto.request.VideoGenerateRequest;
import cn.cordys.crm.ai.video.dto.request.VideoPromptUpdateRequest;
import cn.cordys.crm.ai.video.dto.request.VideoShotUpdateRequest;
import cn.cordys.crm.ai.video.dto.request.VideoVoicePreviewRequest;
import cn.cordys.crm.ai.video.dto.response.VideoExpandResponse;
import cn.cordys.crm.ai.video.dto.response.VideoModelsResponse;
import cn.cordys.crm.ai.video.dto.response.VideoTaskResponse;
import cn.cordys.crm.ai.video.dto.response.VideoTemplateOption;
import cn.cordys.crm.ai.video.dto.response.VideoVoicePreviewResponse;
import cn.cordys.crm.ai.video.service.VideoGenerateService;
import cn.cordys.crm.ai.video.service.VideoTemplateService;
import cn.cordys.crm.ai.video.tts.TtsVoice;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private VideoTemplateService videoTemplateService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通短视频生成")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_VIDEO);
    }

    @GetMapping("/templates")
    @Operation(summary = "查询视频模板库（启用的预置模板，供前端选择）")
    public List<VideoTemplateOption> templates() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoTemplateService.listOptions();
    }

    @GetMapping("/voices")
    @Operation(summary = "查询配音音色列表（跟随已配置的配音模型）")
    public List<TtsVoice> voices() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.voices(orgId);
    }

    @PostMapping("/voices/preview")
    @Operation(summary = "配音音色试听（合成约 5 秒样例，返回 base64 WAV）")
    public VideoVoicePreviewResponse previewVoice(@Valid @RequestBody VideoVoicePreviewRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.previewVoice(orgId, request);
    }

    @GetMapping("/models")
    @Operation(summary = "查询风格页可用的图片/视频模型（图片模型跟随已启用的豆包 Seedream）")
    public VideoModelsResponse models() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.models(orgId);
    }

    @PostMapping("/tasks")
    @Operation(summary = "提交短视频生成任务（异步生成分镜脚本，停在分镜就绪等人确认）")
    public VideoTaskResponse create(@Valid @RequestBody VideoGenerateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        VideoTaskResponse response = videoGenerateService.submit(request, orgId, userId);
        videoGenerateService.generateScriptAsync(response.getId(), orgId, userId);
        return response;
    }

    @PostMapping("/expand")
    @Operation(summary = "主题/文案 AI 扩写（创作摘要的 AI 生成主题 / AI 扩写文案）")
    public VideoExpandResponse expand(@Valid @RequestBody VideoExpandRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.expand(request, orgId, SessionUtils.getUserId());
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

    @PostMapping("/tasks/{id}/cover")
    @Operation(summary = "更换封面（上传本地图片，空则回退模板封面）")
    public VideoTaskResponse setCover(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.setCover(orgId, id, file, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/cover/first-shot")
    @Operation(summary = "用首镜封面（取第一个有配图的分镜画面）")
    public VideoTaskResponse setFirstShotCover(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.setFirstShotCover(orgId, id, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/assets")
    @Operation(summary = "确认分镜，生成画面与配音（异步，停在画面配音就绪）")
    public VideoTaskResponse confirmAssets(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.confirmAssets(orgId, id, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/compose")
    @Operation(summary = "生成视频（异步合成，DONE 后可下载）")
    public VideoTaskResponse compose(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.compose(orgId, id, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/copy")
    @Operation(summary = "重新生成配套文案（成片后配套文案按钮按需调用，失败时重试一次）")
    public VideoTaskResponse regenerateCopy(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.regenerateCopy(orgId, id, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/shots/{index}")
    @Operation(summary = "编辑单个分镜的旁白/画面描述（分镜就绪阶段）")
    public VideoTaskResponse updateShot(@PathVariable String id, @PathVariable int index,
                                        @RequestBody(required = false) VideoShotUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.updateShot(orgId, id, index, request);
    }

    @PostMapping("/tasks/{id}/prompts")
    @Operation(summary = "编辑内置提示词（风格/角色/额外，分镜就绪阶段）")
    public VideoTaskResponse updatePrompts(@PathVariable String id,
                                           @RequestBody(required = false) VideoPromptUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.updatePrompts(orgId, id, request);
    }

    @PostMapping("/tasks/{id}/shots/{index}/regen-image")
    @Operation(summary = "重绘单个分镜画面（用该镜画面描述重新出图并回填附件）")
    public VideoTaskResponse regenShotImage(@PathVariable String id, @PathVariable int index,
                                            @RequestBody(required = false) VideoShotUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.regenShotImage(orgId, id, index, request, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/shots/{index}/preview-voice")
    @Operation(summary = "试听单个分镜口播台词（用任务所选音色合成，返回 base64 WAV）")
    public VideoVoicePreviewResponse previewShotVoice(@PathVariable String id, @PathVariable int index) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.previewShotVoice(orgId, id, index);
    }

    @PostMapping("/tasks/{id}/videos")
    @Operation(summary = "生成镜头视频（AI 视频模式逐镜 Seedance 图生视频，异步，停在镜头视频就绪）")
    public VideoTaskResponse generateVideos(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.generateVideos(orgId, id, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/shots/{index}/regen-video")
    @Operation(summary = "重新生成单个镜头视频（用该镜视频提示词重新调 Seedance，替换附件）")
    public VideoTaskResponse regenShotVideo(@PathVariable String id, @PathVariable int index,
                                            @RequestBody(required = false) VideoShotUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.regenShotVideo(orgId, id, index, request, SessionUtils.getUserId());
    }

    @PostMapping("/tasks/{id}/shots/{index}/image")
    @Operation(summary = "更换单个分镜画面（上传本地图片，压缩后替换配图附件）")
    public VideoTaskResponse uploadShotImage(@PathVariable String id, @PathVariable int index,
                                             @RequestParam("file") MultipartFile file) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return videoGenerateService.uploadShotImage(orgId, id, index, file, SessionUtils.getUserId());
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_VIDEO)) {
            throw new GenericException("当前版本未开通短视频生成，请升级至专业版");
        }
    }
}
