package cn.cordys.crm.ai.drama.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.drama.dto.request.DramaAddEpisodeRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaAssetCreateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaAssetUpdateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaEpisodeScriptRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaEpisodeUpdateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaFragmentsSaveRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaGenerateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaNarrationRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaProjectCreateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaProjectUpdateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaScriptSummaryRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaScriptUpdateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaSeedAssetsRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaVoicePreviewRequest;
import cn.cordys.crm.ai.drama.dto.response.DramaAddEpisodeResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaAssetResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaEpisodeResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaEpisodeScriptResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaGenerateStatusResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaPlanFragmentsResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaProjectResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaScriptResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaScriptSummaryResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaSeedAssetsResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaVoicePreviewResponse;
import cn.cordys.crm.ai.drama.service.DramaGenerateService;
import cn.cordys.crm.ai.drama.service.DramaService;
import cn.cordys.crm.ai.video.dto.response.VideoModelsResponse;
import cn.cordys.crm.ai.video.tts.TtsVoice;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 漫剧接口（功能 7 扩展）。已封存：仅平台超级管理员（userId == admin）可访问，
 * 不按租户版本/套餐开放。
 */
@RestController
@RequestMapping("/agent/drama")
@Tag(name = "AI 漫剧")
public class DramaController {

    @Resource
    private DramaService dramaService;
    @Resource
    private DramaGenerateService dramaGenerateService;

    @GetMapping("/available")
    @Operation(summary = "查询 AI 漫剧是否可用（已封存，仅平台 admin 可用）")
    public boolean available() {
        return PlatformSessionUtils.isAdmin();
    }

    @GetMapping("/models")
    @Operation(summary = "查询可用的图片/视频模型（复用短视频模型设置）")
    public VideoModelsResponse models() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.models(orgId);
    }

    @GetMapping("/voices")
    @Operation(summary = "查询配音音色列表（含本租户克隆音色）")
    public List<TtsVoice> voices() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.voices(orgId);
    }

    // ==================== 项目 ====================

    @GetMapping("/projects")
    @Operation(summary = "项目列表")
    public List<DramaProjectResponse> listProjects() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.listProjects(orgId);
    }

    @PostMapping("/projects")
    @Operation(summary = "新建漫剧项目（同时建空剧本）")
    public DramaProjectResponse createProject(@Valid @RequestBody DramaProjectCreateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.createProject(request, orgId, SessionUtils.getUserId());
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "项目详情（含剧本）")
    public DramaProjectResponse getProject(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.getProject(orgId, id);
    }

    @PutMapping("/projects/{id}")
    @Operation(summary = "更新项目")
    public DramaProjectResponse updateProject(@PathVariable String id,
                                              @Valid @RequestBody DramaProjectUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.updateProject(orgId, id, request);
    }

    @DeleteMapping("/projects/{id}")
    @Operation(summary = "删除项目（级联删除剧本/分集/分镜/资产）")
    public void deleteProject(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        dramaService.deleteProject(orgId, id);
    }

    // ==================== 剧本 ====================

    @GetMapping("/projects/{projectId}/script")
    @Operation(summary = "查询剧本")
    public DramaScriptResponse getScript(@PathVariable String projectId) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.getScript(orgId, projectId);
    }

    @PutMapping("/projects/{projectId}/script")
    @Operation(summary = "更新剧本（创意/摘要/分集内容）")
    public DramaScriptResponse updateScript(@PathVariable String projectId,
                                            @Valid @RequestBody DramaScriptUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.updateScript(orgId, projectId, request);
    }

    // ==================== 生成（剧本摘要 / 分集剧本） ====================

    @PostMapping("/script-summary")
    @Operation(summary = "生成剧情摘要（异步，前端轮询剧本 summaryStatus）")
    public DramaScriptSummaryResponse scriptSummary(@Valid @RequestBody DramaScriptSummaryRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        DramaScriptSummaryResponse response = dramaGenerateService.scriptSummary(request, orgId, userId);
        if (response.isQueued()) {
            dramaGenerateService.scriptSummaryAsync(request.getProjectId(), orgId, userId);
        }
        return response;
    }

    @PostMapping("/episode-script")
    @Operation(summary = "生成分集剧本（异步，前端轮询剧本 paramsJson.episode_content_status）")
    public DramaEpisodeScriptResponse episodeScript(@Valid @RequestBody DramaEpisodeScriptRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        DramaEpisodeScriptResponse response = dramaGenerateService.episodeScript(request, orgId, userId);
        if (response.isQueued()) {
            dramaGenerateService.episodeScriptAsync(request.getProjectId(), orgId, userId,
                    Boolean.TRUE.equals(request.getForce()));
        }
        return response;
    }

    @PostMapping("/add-episode")
    @Operation(summary = "追加一集（同步写入分集剧本）")
    public DramaAddEpisodeResponse addEpisode(@Valid @RequestBody DramaAddEpisodeRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaGenerateService.addEpisode(request, orgId, SessionUtils.getUserId());
    }

    // ==================== 分集 ====================

    @GetMapping("/projects/{projectId}/episodes")
    @Operation(summary = "分集列表（含各集分镜）")
    public List<DramaEpisodeResponse> listEpisodes(@PathVariable String projectId) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.listEpisodes(orgId, projectId);
    }

    @GetMapping("/episodes/{id}")
    @Operation(summary = "分集详情（含分镜）")
    public DramaEpisodeResponse getEpisode(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.getEpisode(orgId, id);
    }

    @PutMapping("/episodes/{id}")
    @Operation(summary = "更新分集")
    public DramaEpisodeResponse updateEpisode(@PathVariable String id,
                                              @Valid @RequestBody DramaEpisodeUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.updateEpisode(orgId, id, request);
    }

    @PutMapping("/projects/{projectId}/episodes/{episodeNumber}")
    @Operation(summary = "重命名分集（同步分集目录标题）")
    public void renameEpisode(@PathVariable String projectId, @PathVariable int episodeNumber,
                              @Valid @RequestBody DramaEpisodeUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        dramaGenerateService.renameEpisode(orgId, projectId, episodeNumber, request.getName());
    }

    @DeleteMapping("/projects/{projectId}/episodes/{episodeNumber}")
    @Operation(summary = "删除分集（级联删除分镜并前移后续集号）")
    public void deleteEpisode(@PathVariable String projectId, @PathVariable int episodeNumber) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        dramaGenerateService.deleteEpisode(orgId, projectId, episodeNumber, SessionUtils.getUserId());
    }

    // ==================== 分镜（规划 / 保存 / 生成） ====================

    @PostMapping("/episodes/{id}/plan-fragments")
    @Operation(summary = "规划分镜（异步 LLM，前端轮询分集 status）")
    public DramaPlanFragmentsResponse planFragments(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        DramaPlanFragmentsResponse response = dramaGenerateService.planFragments(id, orgId, userId);
        if (response.isQueued()) {
            dramaGenerateService.planFragmentsAsync(id, orgId, userId);
        }
        return response;
    }

    @PutMapping("/episodes/{id}/fragments")
    @Operation(summary = "保存分集分镜列表（按 id 增量更新，未提交的分镜删除）")
    public DramaEpisodeResponse saveFragments(@PathVariable String id,
                                              @Valid @RequestBody DramaFragmentsSaveRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaGenerateService.saveFragments(id, request, orgId, SessionUtils.getUserId());
    }

    @PostMapping("/episodes/{id}/generate")
    @Operation(summary = "生成分镜画面（异步 Seedream 文生图，前端轮询 generate-status）")
    public DramaGenerateStatusResponse generateFragments(@PathVariable String id,
                                                         @RequestBody DramaGenerateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        DramaGenerateStatusResponse response = dramaGenerateService.generateFragments(id, request, orgId, userId);
        if (response.getRunning() > 0) {
            dramaGenerateService.generateFragmentsAsync(id, request.getFragmentIds(), orgId, userId);
        }
        return response;
    }

    @GetMapping("/episodes/{id}/generate-status")
    @Operation(summary = "查询分集生成进度（轮询用）")
    public DramaGenerateStatusResponse generateStatus(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaGenerateService.generateStatus(id, orgId);
    }

    // ==================== Phase 6：口播配音 / 合成成片 ====================

    @PostMapping("/episodes/{id}/voice")
    @Operation(summary = "生成分集口播配音（异步，前端轮询分集 narrationStatus）")
    public DramaEpisodeResponse generateNarration(@PathVariable String id,
                                                  @RequestBody(required = false) DramaNarrationRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        String voice = request == null ? null : request.getVoice();
        List<String> fragmentIds = request == null ? null : request.getFragmentIds();
        if (dramaGenerateService.generateNarration(id, voice, fragmentIds, orgId, userId)) {
            dramaGenerateService.generateNarrationAsync(id, fragmentIds, voice, orgId, userId);
        }
        return dramaService.getEpisode(orgId, id);
    }

    @PostMapping("/fragments/{fragmentId}/voice-preview")
    @Operation(summary = "试听分镜口播（用指定音色合成该镜台词，返回 base64 WAV）")
    public DramaVoicePreviewResponse previewNarration(@PathVariable String fragmentId,
                                                      @RequestBody(required = false) DramaVoicePreviewRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String voice = request == null ? null : request.getVoice();
        String text = request == null ? null : request.getText();
        return dramaGenerateService.previewNarration(fragmentId, voice, text, orgId);
    }

    @PostMapping("/episodes/{id}/compose")
    @Operation(summary = "合成成片（异步，前端轮询分集 composeStatus，完成后下载 videoAttachmentId）")
    public DramaEpisodeResponse compose(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        if (dramaGenerateService.startCompose(id, orgId, userId)) {
            dramaGenerateService.composeAsync(id, orgId, userId);
        }
        return dramaService.getEpisode(orgId, id);
    }

    // ==================== Phase 7：逐镜图生视频 ====================

    @PostMapping("/episodes/{id}/generate-video")
    @Operation(summary = "逐镜生成 Seedance 图生视频（异步，前端轮询分集 videoStatus）")
    public DramaEpisodeResponse generateVideo(@PathVariable String id,
                                              @RequestBody(required = false) DramaGenerateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        List<String> fragmentIds = request == null ? null : request.getFragmentIds();
        if (dramaGenerateService.generateFragmentsVideo(id, request, orgId, userId)) {
            dramaGenerateService.generateFragmentsVideoAsync(id, fragmentIds, orgId, userId);
        }
        return dramaService.getEpisode(orgId, id);
    }

    // ==================== 资产 ====================

    @GetMapping("/projects/{projectId}/assets")
    @Operation(summary = "资产列表")
    public List<DramaAssetResponse> listAssets(@PathVariable String projectId) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.listAssets(orgId, projectId);
    }

    @GetMapping("/assets")
    @Operation(summary = "全部资产列表（跨项目，供全局资产库页）")
    public List<DramaAssetResponse> listAllAssets() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.listAllAssets(orgId);
    }

    @PostMapping("/assets")
    @Operation(summary = "新建资产")
    public DramaAssetResponse createAsset(@Valid @RequestBody DramaAssetCreateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.createAsset(orgId, request, SessionUtils.getUserId());
    }

    @PutMapping("/assets/{id}")
    @Operation(summary = "更新资产")
    public DramaAssetResponse updateAsset(@PathVariable String id,
                                          @Valid @RequestBody DramaAssetUpdateRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaService.updateAsset(orgId, id, request);
    }

    @DeleteMapping("/assets/{id}")
    @Operation(summary = "删除资产")
    public void deleteAsset(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        dramaService.deleteAsset(orgId, id);
    }

    @PostMapping("/assets/seed-from-script")
    @Operation(summary = "从剧本抽取资产（角色/场景/道具，异步，前端轮询项目 paramsJson.assets_seed_status）")
    public DramaSeedAssetsResponse seedAssets(@Valid @RequestBody DramaSeedAssetsRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        DramaSeedAssetsResponse response = dramaGenerateService.seedAssets(request, orgId, userId);
        if (response.isQueued()) {
            dramaGenerateService.seedAssetsAsync(request.getProjectId(), orgId, userId,
                    Boolean.TRUE.equals(request.getRefreshPrompts()));
        }
        return response;
    }

    @PostMapping("/assets/{id}/generate")
    @Operation(summary = "生成资产形象图（异步 Seedream 文生图，前端轮询资产 status）")
    public DramaAssetResponse generateAssetImage(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        DramaAssetResponse response = dramaGenerateService.generateAssetImage(id, orgId, userId);
        if (response.getStatus() != null && response.getStatus().equals("running")) {
            dramaGenerateService.generateAssetImageAsync(id, orgId, userId);
        }
        return response;
    }

    @PostMapping("/assets/{id}/upload")
    @Operation(summary = "上传资产形象图（本地图片）")
    public DramaAssetResponse uploadAssetImage(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dramaGenerateService.uploadAssetImage(id, file, orgId, SessionUtils.getUserId());
    }

    private void checkAvailable(String orgId) {
        // AI 漫剧已封存，仅平台超级管理员可用；orgId 保留参数以对齐各调用点签名
        if (!PlatformSessionUtils.isAdmin()) {
            throw new GenericException("AI 漫剧功能已封存，暂未对外开放");
        }
    }
}
