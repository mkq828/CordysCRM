package cn.cordys.crm.ai.drama.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.drama.domain.AiDramaAsset;
import cn.cordys.crm.ai.drama.domain.AiDramaEpisode;
import cn.cordys.crm.ai.drama.domain.AiDramaFragment;
import cn.cordys.crm.ai.drama.domain.AiDramaProject;
import cn.cordys.crm.ai.drama.domain.AiDramaScript;
import cn.cordys.crm.ai.drama.dto.request.DramaAddEpisodeRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaEpisodeScriptRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaFragmentsSaveRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaGenerateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaScriptSummaryRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaSeedAssetsRequest;
import cn.cordys.crm.ai.drama.dto.response.DramaAddEpisodeResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaAssetResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaEpisodeResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaEpisodeScriptResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaGenerateStatusResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaPlanFragmentsResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaScriptSummaryResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaSeedAssetsResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaVoicePreviewResponse;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmImageRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.ai.video.provider.SeedanceVideoProvider;
import cn.cordys.crm.ai.video.tts.TtsConfig;
import cn.cordys.crm.ai.video.tts.TtsProvider;
import cn.cordys.crm.ai.video.tts.TtsProviderFactory;
import cn.cordys.crm.ai.volcengine.VolcengineVoiceCredential;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 漫剧：剧本摘要 / 分集剧本的 LLM 编排（Phase 3 剧情大纲）。
 * 异步任务照 {@code VideoGenerateService} 模式：Controller 调同步入口入队状态，再 @Async 落库，前端轮询。
 * 额度只对 LLM 调用记账（featureCode=ai_drama）。
 */
@Slf4j
@Service
public class DramaGenerateService {

    private static final ObjectMapper MAPPER = JSON.MAPPER;

    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    /** 正文过短阈值（去空白后汉字量近似） */
    private static final int MIN_EPISODE_CONTENT_CHARS = 450;
    /** 单集目标篇幅（约 1-1.5 分钟成片） */
    private static final int TARGET_EPISODE_CONTENT_CHARS = 550;
    private static final String EPISODE_SCENE_COUNT_HINT = "2-3 场";
    /** 手动加集标记：自动流水线不填这些空集 */
    private static final String MANUAL_EPISODE_ORIGIN = "manual";
    private static final int MAX_DRAMA_EPISODES = 120;
    /** 摘要生成超过该时长视为卡死，允许重新入队 */
    private static final long SUMMARY_STALE_MILLIS = 25L * 60 * 1000;

    @Resource
    private BaseMapper<AiDramaProject> projectMapper;
    @Resource
    private BaseMapper<AiDramaScript> scriptMapper;
    @Resource
    private BaseMapper<AiDramaAsset> assetMapper;
    @Resource
    private BaseMapper<AiDramaEpisode> episodeMapper;
    @Resource
    private BaseMapper<AiDramaFragment> fragmentMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private FileCommonService fileCommonService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private DramaService dramaService;
    @Resource
    private TtsProviderFactory ttsProviderFactory;
    @Resource
    private DramaVideoComposer dramaVideoComposer;
    @Resource
    private SeedanceVideoProvider seedanceVideoProvider;

    /** 资产抽取/形象生成状态 */
    private static final String SEED_GENERATING = "generating";
    private static final String SEED_DONE = "done";
    private static final String SEED_FAILED = "failed";
    /** 单项目最多抽取场景数 */
    private static final int MAX_SCENE_ASSETS = 40;
    /** 角色/场景/道具生图默认输出尺寸（Seedream 支持档位） */
    private static final Pattern SCENE_HEAD_PATTERN =
            Pattern.compile("^(?:日|夜|晨|黄昏|傍晚|凌晨|清晨|午|晚)?[ \\t]*(?:内|外|内外)[ \\t]+(.+)$");

    // ==================== 剧本摘要 ====================

    public DramaScriptSummaryResponse scriptSummary(DramaScriptSummaryRequest request, String orgId, String userId) {
        AiDramaScript script = requireScript(orgId, request.getProjectId());
        String creative = StringUtils.isNotBlank(request.getCreative())
                ? request.getCreative().trim()
                : StringUtils.trimToNull(script.getSourceText());
        if (creative == null || creative.length() < 20) {
            throw new GenericException("整剧创意至少 20 字，请先完善创意内容");
        }
        script.setSourceText(creative);
        ObjectNode params = paramsNode(script.getParamsJson());
        if (request.getEpisodeCount() != null && request.getEpisodeCount() > 0) {
            params.put("episode_count", request.getEpisodeCount());
        }
        if (StringUtils.isNotBlank(request.getImageStyleId())) {
            params.put("image_style_id", request.getImageStyleId().trim());
        }
        boolean alreadyRunning = STATUS_RUNNING.equals(script.getSummaryStatus())
                && !isStale(params.path("summary_running_at").asLong(0));

        DramaScriptSummaryResponse response = new DramaScriptSummaryResponse();
        if (!alreadyRunning) {
            params.put("summary_running_at", System.currentTimeMillis());
            params.remove("summary_error");
            script.setSummaryStatus(STATUS_RUNNING);
            response.setQueued(true);
        } else {
            response.setQueued(false);
        }
        script.setParamsJson(params.isEmpty() ? null : params.toString());
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
        response.setScript(dramaService.toScriptResponse(script));
        return response;
    }

    @Async
    public void scriptSummaryAsync(String projectId, String orgId, String userId) {
        AiDramaScript script = findScript(orgId, projectId);
        if (script == null) {
            return;
        }
        AiDramaProject project = findProject(orgId, projectId);
        try {
            String creative = script.getSourceText();
            ObjectNode params = paramsNode(script.getParamsJson());
            Integer episodeCount = params.path("episode_count").isNumber()
                    ? params.path("episode_count").asInt() : null;
            String imageStyleId = params.path("image_style_id").asText(null);

            String userMessage = buildSummaryUserMessage(creative, episodeCount, imageStyleId);
            List<LlmMessage> messages = List.of(
                    new LlmMessage("system", SCRIPT_SUMMARY_SYSTEM_PROMPT),
                    new LlmMessage("user", userMessage));
            JsonNode summary = chatJson(messages, orgId, userId);
            if (summary == null || !summary.isObject()) {
                throw new IllegalStateException("AI 返回的剧本摘要不完整");
            }
            if (episodeCount != null && episodeCount > 0) {
                ((ObjectNode) summary).put("episodeCount", episodeCount);
            }

            AiDramaScript current = requireScript(orgId, projectId);
            current.setSummaryJson(summary.toString());
            current.setSummaryStatus(STATUS_COMPLETED);
            ObjectNode currentParams = paramsNode(current.getParamsJson());
            currentParams.remove("summary_error");
            current.setParamsJson(currentParams.isEmpty() ? null : currentParams.toString());
            current.setUpdateTime(System.currentTimeMillis());
            scriptMapper.updateById(current);

            // 用 AI 剧名覆盖「创意截断/过长」的默认标题
            if (project != null) {
                String autoTitle = pickAutoProjectTitle(summary, creative, project.getTitle());
                if (autoTitle != null) {
                    project.setTitle(autoTitle);
                    project.setUpdateTime(System.currentTimeMillis());
                    projectMapper.updateById(project);
                    AiDramaScript forRename = findScript(orgId, projectId);
                    if (forRename != null) {
                        forRename.setName(autoTitle);
                        scriptMapper.updateById(forRename);
                    }
                }
            }
        } catch (Exception e) {
            log.error("漫剧剧本摘要生成失败 projectId={}, orgId={}", projectId, orgId, e);
            markSummaryFailed(orgId, projectId, e);
        }
    }

    // ==================== 分集剧本 ====================

    public DramaEpisodeScriptResponse episodeScript(DramaEpisodeScriptRequest request, String orgId, String userId) {
        AiDramaScript script = requireScript(orgId, request.getProjectId());
        if (StringUtils.isBlank(script.getSummaryJson())) {
            throw new GenericException("请先生成剧本摘要");
        }
        AiDramaProject project = requireProject(orgId, request.getProjectId());
        JsonNode summary = parseJsonOrNull(script.getSummaryJson());
        int total = resolveEpisodeTarget(summary, project.getParamsJson(), script.getParamsJson());
        if (summary != null && summary.isObject() && summary.path("episodeCount").asInt(0) != total) {
            ((ObjectNode) summary).put("episodeCount", total);
            script.setSummaryJson(summary.toString());
        }
        ObjectNode params = paramsNode(script.getParamsJson());
        boolean force = Boolean.TRUE.equals(request.getForce());
        boolean alreadyRunning = STATUS_RUNNING.equals(params.path("episode_content_status").asText(null));
        List<ObjectNode> existing = existingEpisodes(script.getEpisodeContentJson());

        DramaEpisodeScriptResponse response = new DramaEpisodeScriptResponse();
        if (alreadyRunning && !force) {
            response.setQueued(false);
            response.setTotalTarget(total);
            response.setTotalGenerated(countCompletedEpisodes(existing, total));
            response.setDone(false);
            script.setParamsJson(params.isEmpty() ? null : params.toString());
            scriptMapper.updateById(script);
            response.setScript(dramaService.toScriptResponse(script));
            return response;
        }

        params.put("episode_content_status", STATUS_RUNNING);
        params.put("episode_count", total);
        params.remove("episode_content_error");
        if (force && !existing.isEmpty()) {
            // force 重跑：保留集名，清空正文
            script.setEpisodeContentJson(writeEpisodes(clearBodies(existing)));
            ObjectNode progress = params.putObject("episode_content_progress");
            progress.put("done", 0);
            progress.put("total", total);
            existing = existingEpisodes(script.getEpisodeContentJson());
        }
        script.setParamsJson(params.isEmpty() ? null : params.toString());
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);

        response.setQueued(true);
        response.setTotalTarget(total);
        response.setTotalGenerated(countCompletedEpisodes(existing, total));
        response.setDone(false);
        response.setScript(dramaService.toScriptResponse(script));
        return response;
    }

    @Async
    public void episodeScriptAsync(String projectId, String orgId, String userId, boolean force) {
        AiDramaScript script = findScript(orgId, projectId);
        if (script == null || StringUtils.isBlank(script.getSummaryJson())) {
            log.warn("分集剧本生成跳过：缺少剧本或摘要 projectId={}", projectId);
            return;
        }
        AiDramaProject project = findProject(orgId, projectId);
        try {
            JsonNode summary = parseJsonOrNull(script.getSummaryJson());
            int total = resolveEpisodeTarget(summary,
                    project == null ? null : project.getParamsJson(), script.getParamsJson());
            if (summary != null && summary.isObject()) {
                ((ObjectNode) summary).put("episodeCount", total);
            }
            String creative = StringUtils.defaultString(script.getSourceText(), "").trim();
            List<ObjectNode> existing = existingEpisodes(script.getEpisodeContentJson());
            if (force && !existing.isEmpty()) {
                existing = clearBodies(existing);
            }

            // 1. 集名大纲
            if (!titlesReady(existing, total)) {
                List<ObjectNode> outline = runEpisodeOutline(creative, summary, total, orgId, userId);
                existing = mergeEpisodeBodies(existing, outline);
                persistEpisodeContent(orgId, projectId, existing, paramsWithStatus(total,
                        countCompletedEpisodes(existing, total), STATUS_RUNNING));
            }

            // 2. 循环补齐缺失正文
            int guard = 0;
            while (true) {
                List<Integer> missing = autoMissingEpisodeNumbers(existing, total);
                if (missing.isEmpty()) {
                    break;
                }
                List<ObjectNode> batch = runEpisodeScriptBatch(summary, existing, total, creative, orgId, userId);
                existing = mergeEpisodeBodies(existing, batch);
                persistEpisodeContent(orgId, projectId, existing, paramsWithStatus(total,
                        countCompletedEpisodes(existing, total), STATUS_RUNNING));
                guard++;
                if (guard > Math.max(total * 2, 24)) {
                    throw new IllegalStateException("分集生成未完成（"
                            + countCompletedEpisodes(existing, total) + "/" + total + "）");
                }
                if (batch.isEmpty()) {
                    throw new IllegalStateException("分集生成无进度");
                }
            }

            AiDramaScript done = requireScript(orgId, projectId);
            done.setEpisodeContentJson(writeEpisodes(existing));
            done.setParamsJson(paramsWithStatus(total, total, STATUS_COMPLETED));
            done.setUpdateTime(System.currentTimeMillis());
            scriptMapper.updateById(done);
            ensureEpisodeRows(orgId, projectId, userId);
            log.info("分集剧本全部完成 projectId={} total={}", projectId, total);
        } catch (Exception e) {
            log.error("漫剧分集剧本生成失败 projectId={}, orgId={}", projectId, orgId, e);
            markEpisodeFailed(orgId, projectId, e);
        }
    }

    // ==================== 追加一集 ====================

    public DramaAddEpisodeResponse addEpisode(DramaAddEpisodeRequest request, String orgId, String userId) {
        AiDramaScript script = requireScript(orgId, request.getProjectId());
        if (StringUtils.isBlank(script.getSummaryJson())) {
            throw new GenericException("请先生成剧本摘要");
        }
        AiDramaProject project = requireProject(orgId, request.getProjectId());
        ObjectNode params = paramsNode(script.getParamsJson());
        if (STATUS_RUNNING.equals(params.path("episode_content_status").asText(null))) {
            throw new GenericException("全集剧本正在生成，请完成后再加集");
        }
        List<ObjectNode> existing = existingEpisodes(script.getEpisodeContentJson());
        if (existing.size() >= MAX_DRAMA_EPISODES) {
            throw new GenericException("最多 " + MAX_DRAMA_EPISODES + " 集");
        }
        int next = existing.stream().mapToInt(o -> o.path("episodeNumber").asInt(0)).max().orElse(0) + 1;
        String title = StringUtils.isBlank(request.getTitle()) ? "第 " + next + " 集" : request.getTitle().trim();
        ObjectNode added = MAPPER.createObjectNode();
        added.put("episodeNumber", next);
        added.put("title", title);
        added.put("creative", "");
        added.put("summary", "");
        added.put("body", "");
        added.put("origin", MANUAL_EPISODE_ORIGIN);
        List<ObjectNode> merged = mergeEpisodeBodies(existing, List.of(added));

        JsonNode summary = parseJsonOrNull(script.getSummaryJson());
        int total = resolveEpisodeTarget(summary, project.getParamsJson(), script.getParamsJson());
        total = Math.max(total, Math.max(next, merged.size()));
        if (summary != null && summary.isObject()) {
            ((ObjectNode) summary).put("episodeCount", total);
            script.setSummaryJson(summary.toString());
        }
        params.put("episode_count", total);
        script.setEpisodeContentJson(writeEpisodes(merged));
        script.setParamsJson(params.isEmpty() ? null : params.toString());
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
        ensureEpisodeRows(orgId, request.getProjectId(), userId);

        DramaAddEpisodeResponse response = new DramaAddEpisodeResponse();
        response.setEpisodeNumber(next);
        response.setScript(dramaService.toScriptResponse(script));
        return response;
    }

    /** 重命名分集：同步分集行 name 与分集目录标题（episode_content.title 才是目录真相源） */
    public void renameEpisode(String orgId, String projectId, int episodeNumber, String name) {
        AiDramaScript script = requireScript(orgId, projectId);
        String title = StringUtils.trimToNull(name);
        if (title == null) {
            return;
        }
        List<ObjectNode> existing = existingEpisodes(script.getEpisodeContentJson());
        for (ObjectNode row : existing) {
            if (row.path("episodeNumber").asInt(0) == episodeNumber) {
                row.put("title", title);
            }
        }
        script.setEpisodeContentJson(writeEpisodes(existing));
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
        AiDramaEpisode episode = findEpisodeByNumber(orgId, projectId, episodeNumber);
        if (episode != null) {
            episode.setName(title);
            episode.setUpdateTime(System.currentTimeMillis());
            episodeMapper.updateById(episode);
        }
    }

    /** 删除分集：级联删除分镜，目录移除该集并前移后续集号，保持 1..N 连续 */
    public void deleteEpisode(String orgId, String projectId, int episodeNumber, String userId) {
        AiDramaScript script = requireScript(orgId, projectId);
        AiDramaEpisode episode = findEpisodeByNumber(orgId, projectId, episodeNumber);
        if (episode != null) {
            fragmentMapper.deleteByLambda(new LambdaQueryWrapper<AiDramaFragment>()
                    .eq(AiDramaFragment::getEpisodeId, episode.getId()));
            episodeMapper.deleteByPrimaryKey(episode.getId());
        }
        List<ObjectNode> kept = new ArrayList<>();
        for (ObjectNode row : existingEpisodes(script.getEpisodeContentJson())) {
            int num = row.path("episodeNumber").asInt(0);
            if (num == episodeNumber) {
                continue;
            }
            if (num > episodeNumber) {
                row.put("episodeNumber", num - 1);
            }
            kept.add(row);
        }
        script.setEpisodeContentJson(writeEpisodes(kept));
        ObjectNode params = paramsNode(script.getParamsJson());
        params.put("episode_count", kept.size());
        script.setParamsJson(params.isEmpty() ? null : params.toString());
        JsonNode summary = parseJsonOrNull(script.getSummaryJson());
        if (summary != null && summary.isObject()) {
            ((ObjectNode) summary).put("episodeCount", kept.size());
            script.setSummaryJson(summary.toString());
        }
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
        // 后续分集行 sort_order 前移，与目录保持 1..N 连续
        for (AiDramaEpisode ep : episodeMapper.selectListByLambda(new LambdaQueryWrapper<AiDramaEpisode>()
                .eq(AiDramaEpisode::getProjectId, projectId))) {
            Integer so = ep.getSortOrder();
            if (so != null && so > episodeNumber) {
                ep.setSortOrder(so - 1);
                ep.setUpdateTime(System.currentTimeMillis());
                episodeMapper.updateById(ep);
            }
        }
    }

    /** 按集号查分集行（分集行 sort_order == 目录 episodeNumber） */
    private AiDramaEpisode findEpisodeByNumber(String orgId, String projectId, int episodeNumber) {
        List<AiDramaEpisode> list = episodeMapper.selectListByLambda(new LambdaQueryWrapper<AiDramaEpisode>()
                .eq(AiDramaEpisode::getProjectId, projectId)
                .eq(AiDramaEpisode::getSortOrder, episodeNumber));
        return list.isEmpty() ? null : list.get(0);
    }

    // ==================== 资产抽取（seed_from_script） ====================

    public DramaSeedAssetsResponse seedAssets(DramaSeedAssetsRequest request, String orgId, String userId) {
        String projectId = request.getProjectId();
        AiDramaScript script = requireScript(orgId, projectId);
        if (StringUtils.isBlank(script.getSummaryJson())) {
            throw new GenericException("请先生成剧本摘要");
        }
        AiDramaProject project = requireProject(orgId, projectId);
        ObjectNode params = paramsNode(project.getParamsJson());
        boolean running = SEED_GENERATING.equals(params.path("assets_seed_status").asText(null))
                && !isStale(params.path("assets_seed_generating_at").asLong(0));

        DramaSeedAssetsResponse response = new DramaSeedAssetsResponse();
        response.setAssets(dramaService.listAssets(orgId, projectId));
        if (running) {
            response.setQueued(false);
            response.setStatus(SEED_GENERATING);
            response.setMessage("资产抽取进行中，请稍候刷新");
            return response;
        }
        params.put("assets_seed_status", SEED_GENERATING);
        params.put("assets_seed_generating_at", System.currentTimeMillis());
        params.remove("assets_seed_error");
        project.setParamsJson(params.isEmpty() ? null : params.toString());
        project.setUpdateTime(System.currentTimeMillis());
        projectMapper.updateById(project);
        response.setQueued(true);
        response.setStatus(SEED_GENERATING);
        response.setMessage("资产抽取任务已提交，请稍候刷新");
        return response;
    }

    @Async
    public void seedAssetsAsync(String projectId, String orgId, String userId, boolean refreshPrompts) {
        AiDramaScript script = findScript(orgId, projectId);
        AiDramaProject project = findProject(orgId, projectId);
        if (script == null || project == null) {
            return;
        }
        try {
            JsonNode summary = parseJsonOrNull(script.getSummaryJson());
            if (summary == null || !summary.isObject()) {
                throw new IllegalStateException("剧本摘要缺失");
            }
            String storyType = summary.path("storyType").asText("").trim();
            Map<String, AiDramaAsset> existingByKey = new HashMap<>();
            for (AiDramaAsset asset : listAssets(orgId, projectId)) {
                String name = normalizeAssetName(asset.getName());
                if (name.isEmpty()) {
                    continue;
                }
                existingByKey.putIfAbsent(assetDedupeKey(asset.getType(), name), asset);
            }

            // 1. 角色：摘要人物 + 分集「出场人物」合并保序，跳过音色/声音标注名
            Set<String> characterNames = new LinkedHashSet<>();
            JsonNode characters = summary.path("characters");
            if (characters.isArray()) {
                for (JsonNode ch : characters) {
                    if (!ch.isObject()) {
                        continue;
                    }
                    String name = ch.path("name").asText("").trim();
                    if (!name.isEmpty()) {
                        characterNames.add(name);
                    }
                }
            }
            characterNames.addAll(extractCastNames(script.getEpisodeContentJson()));

            int created = 0;
            for (String name : characterNames) {
                String norm = characterNameForSeed(name);
                if (norm == null || existingByKey.containsKey(assetDedupeKey("character", norm))) {
                    continue;
                }
                JsonNode ch = findCharacter(summary, name, norm);
                AiDramaAsset asset = buildAssetRow(orgId, projectId, "character", "image", norm,
                        buildCharacterParamsJson(ch, norm, storyType), userId);
                assetMapper.insert(asset);
                existingByKey.put(assetDedupeKey("character", norm), asset);
                created++;
            }

            // 2. 场景：从分集正文「内/外景」行抽取
            for (String scene : extractSceneNames(script.getEpisodeContentJson())) {
                String norm = normalizeAssetName(scene);
                if (norm.isEmpty() || existingByKey.containsKey(assetDedupeKey("scene", norm))) {
                    continue;
                }
                AiDramaAsset asset = buildAssetRow(orgId, projectId, "scene", "image", norm,
                        buildSceneParamsJson(norm, storyType), userId);
                assetMapper.insert(asset);
                existingByKey.put(assetDedupeKey("scene", norm), asset);
                created++;
            }

            // 3. 道具：尚无可视化道具时，用 LLM 一次性抽取（失败不阻断）
            boolean hasProp = existingByKey.keySet().stream().anyMatch(k -> k.startsWith("prop|"));
            if (!hasProp && !refreshPrompts) {
                for (String[] prop : extractProps(summary, script.getEpisodeContentJson(), orgId, userId)) {
                    String name = prop[0];
                    String visual = prop[1];
                    String norm = normalizeAssetName(name);
                    if (norm.isEmpty() || existingByKey.containsKey(assetDedupeKey("prop", norm))) {
                        continue;
                    }
                    AiDramaAsset asset = buildAssetRow(orgId, projectId, "prop", "image", norm,
                            buildPropParamsJson(norm, visual), userId);
                    assetMapper.insert(asset);
                    existingByKey.put(assetDedupeKey("prop", norm), asset);
                    created++;
                }
            }

            markSeedDone(orgId, projectId, created);
            log.info("漫剧资产抽取完成 projectId={} created={}", projectId, created);
        } catch (Exception e) {
            log.error("漫剧资产抽取失败 projectId={}, orgId={}", projectId, orgId, e);
            markSeedFailed(orgId, projectId, e);
        }
    }

    // ==================== 资产形象生成（文生图） ====================

    public DramaAssetResponse generateAssetImage(String assetId, String orgId, String userId) {
        AiDramaAsset asset = requireAsset(orgId, assetId);
        if (isAssetGenerating(asset)) {
            return dramaService.toAssetResponse(asset);
        }
        String refId = resolveReferenceAttachmentId(asset);
        ObjectNode params = paramsNode(asset.getParamsJson());
        ObjectNode gen = params.putObject("generation");
        gen.put("status", STATUS_RUNNING);
        if (refId != null) {
            gen.put("reference_attachment_id", refId);
        }
        gen.remove("error");
        asset.setStatus(STATUS_RUNNING);
        asset.setParamsJson(params.isEmpty() ? null : params.toString());
        asset.setUpdateTime(System.currentTimeMillis());
        assetMapper.updateById(asset);
        return dramaService.toAssetResponse(asset);
    }

    @Async
    public void generateAssetImageAsync(String assetId, String orgId, String userId) {
        AiDramaAsset asset = findAsset(orgId, assetId);
        if (asset == null) {
            return;
        }
        List<File> tempFiles = new ArrayList<>();
        File temp = null;
        try {
            List<AgentModel> imageModels = agentModelService.resolveImageModels(orgId);
            if (imageModels.isEmpty()) {
                throw new GenericException("请先在「模型设置」中启用豆包 Seedream 图片模型");
            }
            AgentModel model = imageModels.get(0);
            String prompt = buildAssetImagePrompt(asset);
            String referenceImage = referenceImageDataUri(asset, orgId, tempFiles);
            if (referenceImage != null && "character".equals(asset.getType())) {
                // 只取干净的画面描述（服装/标志物），剔除年龄/长相/发型（以参考图为准）与剧本元数据
                String features = stripFaceDescription(assetVisualPrompt(asset));
                AiDramaScript script = findScript(asset.getOrganizationId(), asset.getProjectId());
                String styleId = script == null ? null
                        : paramsNode(script.getParamsJson()).path("image_style_id").asText(null);
                if (DramaImageStyles.isExplicitPhotoreal(styleId)) {
                    prompt = "严格保持参考图中人物的面部五官、脸型、发型、肤色与年龄感，不要改变人物相貌；"
                            + "服装、场景、光线按以下描述生成："
                            + (features != null ? features : "影视级写实角色形象照，商务正装");
                } else {
                    // 半写实/动漫等非写实画风：参考图锁定长相，按画风重绘（像真人但非照片，规避 Seedance 真人风控）
                    prompt = DramaImageStyles.prompt(styleId)
                            + "。参考图锁定该角色长相与五官轮廓，保持其脸型、发型与年龄感；"
                            + (features != null ? "角色造型：" + features : "半写实角色形象照，商务正装");
                }
                log.info("漫剧角色生图带参考图 assetId={}, styleId={}, referenceDataUriLen={}",
                        assetId, styleId, referenceImage.length());
            } else if (referenceImage != null) {
                log.info("漫剧资产生图带参考图(非角色) assetId={}, type={}", assetId, asset.getType());
            } else {
                log.info("漫剧资产生图无参考图（纯文生图） assetId={}", assetId);
            }
            LlmProvider provider = llmProviderFactory.get(model.getProvider());
            LlmImageRequest request = new LlmImageRequest();
            request.setModel(model.getModelName());
            request.setBaseUrl(StringUtils.defaultIfBlank(model.getApiUrl(),
                    "https://ark.cn-beijing.volces.com/api/v3"));
            request.setApiKey(model.getApiKey());
            request.setSize(imageSizeForAssetRatio(asset));
            request.setPrompt(prompt);
            if (referenceImage != null) {
                request.setImage(List.of(referenceImage));
            }
            byte[] bytes = provider.generateImage(request);
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("图片模型未返回图片数据");
            }
            temp = File.createTempFile("drama-asset", ".png");
            Files.write(temp.toPath(), bytes);
            String attachmentId = saveAssetImage(temp, asset, orgId, userId);

            AiDramaAsset current = requireAsset(orgId, assetId);
            String refId = resolveReferenceAttachmentId(current);
            ObjectNode params = paramsNode(current.getParamsJson());
            ObjectNode gen = params.putObject("generation");
            gen.put("status", STATUS_COMPLETED);
            gen.put("source", "ai");
            if (refId != null) {
                gen.put("reference_attachment_id", refId);
            }
            current.setCoverAttachmentId(attachmentId);
            current.setStatus(STATUS_COMPLETED);
            current.setAssetType(StringUtils.defaultIfBlank(current.getAssetType(), "image"));
            current.setParamsJson(params.isEmpty() ? null : params.toString());
            current.setUpdateTime(System.currentTimeMillis());
            assetMapper.updateById(current);
        } catch (Exception e) {
            log.error("漫剧资产形象生成失败 assetId={}, orgId={}", assetId, orgId, e);
            markAssetFailed(orgId, assetId, e);
        } finally {
            for (File ref : tempFiles) {
                if (ref != null && ref.exists()) {
                    try {
                        Files.deleteIfExists(ref.toPath());
                    } catch (IOException ignored) {
                        // 清理临时文件失败不影响主流程
                    }
                }
            }
            if (temp != null && temp.exists()) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程
                }
            }
        }
    }

    // ==================== 资产形象上传 ====================

    public DramaAssetResponse uploadAssetImage(String assetId, MultipartFile file, String orgId, String userId) {
        AiDramaAsset asset = requireAsset(orgId, assetId);
        if (isAssetGenerating(asset)) {
            throw new GenericException("形象生成中，请稍后再更换图片");
        }
        String ext = resolveImageExt(file);
        if (ext == null) {
            throw new GenericException("仅支持 JPG / PNG / WebP / GIF 图片");
        }
        if (file.getSize() <= 0) {
            throw new GenericException("空文件");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new GenericException("文件不能超过 20MB");
        }
        String attachmentId = saveAssetUpload(file, ext, asset, orgId, userId);
        asset.setCoverAttachmentId(attachmentId);
        asset.setStatus(STATUS_COMPLETED);
        asset.setAssetType(StringUtils.defaultIfBlank(asset.getAssetType(), "image"));
        ObjectNode params = paramsNode(asset.getParamsJson());
        ObjectNode gen = params.putObject("generation");
        gen.put("status", STATUS_COMPLETED);
        gen.put("source", "upload");
        gen.put("reference_attachment_id", attachmentId);
        asset.setParamsJson(params.isEmpty() ? null : params.toString());
        asset.setUpdateTime(System.currentTimeMillis());
        assetMapper.updateById(asset);
        return dramaService.toAssetResponse(asset);
    }

    // ==================== Phase 5：分镜规划 / 保存 / 生成 ====================

    /** 单集分镜上限（含开幕镜） */
    private static final int MAX_FRAGMENTS_PER_EPISODE = 10;
    private static final int FRAGMENT_DURATION_MIN = 4;
    private static final int FRAGMENT_DURATION_MAX = 15;
    private static final int FRAGMENT_MAX_CHARACTERS = 3;
    private static final int FRAGMENT_MAX_PROPS = 2;
    private static final int FRAGMENT_MAX_ASSETS = 6;

    /** 场景背景定版缓存：orgId:sceneAssetId → 定版空背景 data URI，同一场景跨分镜复用，避免每镜重画背景 */
    private final Map<String, String> sceneBackgroundCache = new ConcurrentHashMap<>();

    public DramaPlanFragmentsResponse planFragments(String episodeId, String orgId, String userId) {
        AiDramaEpisode episode = requireEpisode(orgId, episodeId);
        DramaPlanFragmentsResponse response = new DramaPlanFragmentsResponse();
        if (isFragmentPlanning(episode)) {
            response.setQueued(false);
            response.setEpisode(dramaService.getEpisode(orgId, episodeId));
            return response;
        }
        ObjectNode params = paramsNode(episode.getParamsJson());
        params.put("fragment_plan_status", "generating");
        params.put("fragment_plan_generating_at", System.currentTimeMillis());
        params.remove("fragment_plan_error");
        episode.setStatus("planning");
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
        response.setQueued(true);
        response.setEpisode(dramaService.getEpisode(orgId, episodeId));
        return response;
    }

    @Async
    public void planFragmentsAsync(String episodeId, String orgId, String userId) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        try {
            List<AiDramaFragment> drafts = runFragmentPlan(episode, orgId, userId);
            fragmentMapper.deleteByLambda(new LambdaQueryWrapper<AiDramaFragment>()
                    .eq(AiDramaFragment::getEpisodeId, episodeId));
            long now = System.currentTimeMillis();
            int order = 0;
            for (AiDramaFragment draft : drafts) {
                draft.setId(IDGenerator.nextStr());
                draft.setOrganizationId(orgId);
                draft.setEpisodeId(episodeId);
                draft.setSortOrder(order++);
                draft.setCreateUser(userId);
                draft.setUpdateUser(userId);
                draft.setCreateTime(now);
                draft.setUpdateTime(now);
                fragmentMapper.insert(draft);
            }
            AiDramaEpisode current = requireEpisode(orgId, episodeId);
            current.setStatus("completed");
            ObjectNode params = paramsNode(current.getParamsJson());
            params.put("fragment_plan_status", "completed");
            params.remove("fragment_plan_error");
            params.remove("fragment_plan_generating_at");
            current.setParamsJson(params.isEmpty() ? null : params.toString());
            current.setUpdateTime(System.currentTimeMillis());
            episodeMapper.updateById(current);
            log.info("漫剧分镜规划完成 episodeId={} fragments={}", episodeId, drafts.size());
        } catch (Exception e) {
            log.error("漫剧分镜规划失败 episodeId={}, orgId={}", episodeId, orgId, e);
            markFragmentPlanFailed(orgId, episodeId, e);
        }
    }

    /** 保存分镜列表（按 id 增量更新，未提交的分镜删除），前端编辑后落库 */
    public DramaEpisodeResponse saveFragments(String episodeId, DramaFragmentsSaveRequest request,
                                              String orgId, String userId) {
        AiDramaEpisode episode = requireEpisode(orgId, episodeId);
        List<AiDramaFragment> existing = listFragments(episodeId);
        Map<String, AiDramaFragment> byId = new HashMap<>();
        for (AiDramaFragment fragment : existing) {
            byId.put(fragment.getId(), fragment);
        }
        Set<String> keptIds = new HashSet<>();
        long now = System.currentTimeMillis();
        int order = 0;
        for (DramaFragmentsSaveRequest.FragmentItem item : request.getFragments()) {
            String id = StringUtils.trimToNull(item.getId());
            boolean update = id != null && byId.containsKey(id);
            AiDramaFragment fragment = update ? byId.get(id) : new AiDramaFragment();
            if (!update) {
                fragment.setId(IDGenerator.nextStr());
                fragment.setOrganizationId(orgId);
                fragment.setEpisodeId(episodeId);
                fragment.setCreateUser(userId);
                fragment.setCreateTime(now);
            }
            fragment.setSortOrder(order++);
            fragment.setContent(StringUtils.trimToNull(item.getContent()));
            fragment.setNarration(StringUtils.trimToNull(item.getNarration()));
            fragment.setVoice(StringUtils.trimToNull(item.getVoice()));
            fragment.setDurationSec(clampFragmentDuration(item.getDurationSec()));
            fragment.setAssetIdsJson(StringUtils.trimToNull(item.getAssetIdsJson()));
            fragment.setUpdateUser(userId);
            fragment.setUpdateTime(now);
            if (update) {
                fragmentMapper.updateById(fragment);
            } else {
                fragmentMapper.insert(fragment);
            }
            keptIds.add(fragment.getId());
        }
        for (AiDramaFragment fragment : existing) {
            if (!keptIds.contains(fragment.getId())) {
                fragmentMapper.deleteByPrimaryKey(fragment.getId());
            }
        }
        return dramaService.getEpisode(orgId, episodeId);
    }

    /** 生成指定分镜画面（空则整集），同步把目标分镜置 running 后由异步逐镜出图 */
    public DramaGenerateStatusResponse generateFragments(String episodeId, DramaGenerateRequest request,
                                                         String orgId, String userId) {
        requireEpisode(orgId, episodeId);
        List<AiDramaFragment> targets = resolveTargetFragments(episodeId, request.getFragmentIds());
        long now = System.currentTimeMillis();
        for (AiDramaFragment fragment : targets) {
            // 目标分镜一律重置为 running 重新出图，已完成的也要支持「改提示词后重新生成画面」
            fragment.setStatus(STATUS_RUNNING);
            fragment.setUpdateTime(now);
            fragmentMapper.updateById(fragment);
        }
        return generateStatus(episodeId, orgId);
    }

    @Async
    public void generateFragmentsAsync(String episodeId, List<String> fragmentIds, String orgId, String userId) {
        for (AiDramaFragment fragment : resolveTargetFragments(episodeId, fragmentIds)) {
            generateFragmentImage(fragment.getId(), orgId, userId);
        }
    }

    /** 分集生成进度（前端轮询用） */
    public DramaGenerateStatusResponse generateStatus(String episodeId, String orgId) {
        requireEpisode(orgId, episodeId);
        List<AiDramaFragment> fragments = listFragments(episodeId);
        DramaGenerateStatusResponse response = new DramaGenerateStatusResponse();
        response.setEpisodeId(episodeId);
        List<DramaGenerateStatusResponse.FragmentStatus> statuses = new ArrayList<>(fragments.size());
        int done = 0;
        int failed = 0;
        int running = 0;
        for (AiDramaFragment fragment : fragments) {
            String status = StringUtils.defaultIfBlank(fragment.getStatus(), "pending");
            if (STATUS_COMPLETED.equals(status)) {
                done++;
            } else if (STATUS_FAILED.equals(status)) {
                failed++;
            } else if (STATUS_RUNNING.equals(status)) {
                running++;
            }
            DramaGenerateStatusResponse.FragmentStatus fs = new DramaGenerateStatusResponse.FragmentStatus();
            fs.setFragmentId(fragment.getId());
            fs.setStatus(status);
            fs.setCoverAttachmentId(fragment.getCoverAttachmentId());
            fs.setVideoAttachmentId(fragment.getVideoAttachmentId());
            statuses.add(fs);
        }
        response.setDone(done);
        response.setFailed(failed);
        response.setRunning(running);
        response.setTotal(fragments.size());
        response.setFragments(statuses);
        return response;
    }

    // ==================== Phase 6：口播配音 / 合成成片 ====================

    /** 提交口播配音任务：把分集各镜台词合成为音频附件；已运行则返回 false 不重复入队 */
    public boolean generateNarration(String episodeId, String voice, List<String> fragmentIds,
                                     String orgId, String userId) {
        AiDramaEpisode episode = requireEpisode(orgId, episodeId);
        ObjectNode params = paramsNode(episode.getParamsJson());
        if (STATUS_RUNNING.equals(params.path("narration_status").asText(null))) {
            return false;
        }
        String resolvedVoice = StringUtils.trimToNull(voice);
        if (resolvedVoice == null) {
            resolvedVoice = params.path("narration_voice").asText(null);
        }
        if (resolvedVoice != null) {
            params.put("narration_voice", resolvedVoice);
        }
        params.put("narration_status", STATUS_RUNNING);
        params.remove("narration_error");
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
        return true;
    }

    @Async
    public void generateNarrationAsync(String episodeId, List<String> fragmentIds, String voice,
                                       String orgId, String userId) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        String episodeVoice = StringUtils.trimToNull(voice);
        if (episodeVoice == null) {
            episodeVoice = paramsNode(episode.getParamsJson()).path("narration_voice").asText(null);
        }
        int done = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();
        String projectId = episode.getProjectId();
        for (AiDramaFragment fragment : resolveTargetFragments(episodeId, fragmentIds)) {
            String text = StringUtils.trimToNull(fragment.getNarration());
            if (text == null) {
                // 纯画面镜无口播，合成时静音处理，不计入配音进度
                continue;
            }
            // 多人对话：把台词按「角色：台词」拆句，每句用该角色在资产库绑定的音色合成，
            // 未绑定角色/旁白回退本镜默认音色(再退分集默认)，最后按句序拼成一段音频
            String defaultVoice = StringUtils.defaultIfBlank(fragment.getVoice(), episodeVoice);
            List<NarrationLine> lines = parseNarrationLines(text);
            File temp = null;
            try {
                byte[] audio = synthesizeNarrationLines(lines, projectId, orgId, defaultVoice);
                temp = File.createTempFile("drama-narration", ".wav");
                Files.write(temp.toPath(), audio);
                String attachmentId = saveNarrationAttachment(temp, fragment, orgId, userId);
                AiDramaFragment current = findFragment(orgId, fragment.getId());
                if (current != null) {
                    current.setNarrationAttachmentId(attachmentId);
                    current.setUpdateTime(System.currentTimeMillis());
                    fragmentMapper.updateById(current);
                }
                done++;
            } catch (Exception e) {
                failed++;
                log.error("漫剧口播配音失败 fragmentId={}, orgId={}", fragment.getId(), orgId, e);
                if (errors.length() > 0) {
                    errors.append("；");
                }
                errors.append(StringUtils.left(e.getMessage(), 120));
            } finally {
                if (temp != null && temp.exists()) {
                    try {
                        Files.deleteIfExists(temp.toPath());
                    } catch (IOException ignored) {
                        // 清理临时文件失败不影响主流程
                    }
                }
            }
        }
        markNarrationDone(orgId, episodeId, failed, done, errors.toString());
    }

    /** 试听某镜口播：用指定音色合成该镜台词返回 base64 WAV，与正式配音同一降级链。
     *  text 为前端当前编辑框里的台词（可能尚未保存），传了就用它合成，保证「所听即所得」；
     *  不传则回退该镜已保存的台词 */
    public DramaVoicePreviewResponse previewNarration(String fragmentId, String voice, String text, String orgId) {
        AiDramaFragment fragment = findFragment(orgId, fragmentId);
        if (fragment == null) {
            throw new GenericException("漫剧分镜不存在");
        }
        String narration = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(text),
                StringUtils.defaultIfBlank(fragment.getNarration(), "这是一段口播配音试听示例。"));
        // 未显式传音色时回退该镜已保存的音色；试听同样按角色拆句，所听即所得
        String defaultVoice = StringUtils.defaultIfBlank(StringUtils.trimToNull(voice), fragment.getVoice());
        AiDramaEpisode episode = findEpisode(orgId, fragment.getEpisodeId());
        String projectId = episode != null ? episode.getProjectId() : null;
        log.info("漫剧试听 previewNarration fragmentId={}, 传入text={}, 解析narration={}, 默认voice={}",
                fragmentId, StringUtils.left(text, 120), StringUtils.left(narration, 120), defaultVoice);
        byte[] audio = synthesizeNarrationLines(parseNarrationLines(narration), projectId, orgId, defaultVoice);
        DramaVoicePreviewResponse response = new DramaVoicePreviewResponse();
        response.setAudio(Base64.getEncoder().encodeToString(audio));
        response.setVoice(voice);
        return response;
    }

    /** 提交合成成片任务：逐镜静图 + 口播拼成竖屏 mp4；已运行则返回 false 不重复入队 */
    public boolean startCompose(String episodeId, String orgId, String userId) {
        AiDramaEpisode episode = requireEpisode(orgId, episodeId);
        ObjectNode params = paramsNode(episode.getParamsJson());
        if (STATUS_RUNNING.equals(params.path("compose_status").asText(null))) {
            return false;
        }
        params.put("compose_status", STATUS_RUNNING);
        params.remove("compose_error");
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
        return true;
    }

    @Async
    public void composeAsync(String episodeId, String orgId, String userId) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        List<File> tempFiles = new ArrayList<>();
        File finalMp4 = null;
        try {
            List<AiDramaFragment> fragments = listFragments(episodeId);
            List<DramaVideoComposer.Clip> clips = new ArrayList<>(fragments.size());
            for (AiDramaFragment fragment : fragments) {
                File image = fragment.getCoverAttachmentId() == null
                        ? null : downloadAttachmentToFile(fragment.getCoverAttachmentId(), orgId, tempFiles);
                File video = fragment.getVideoAttachmentId() == null
                        ? null : downloadAttachmentToFile(fragment.getVideoAttachmentId(), orgId, tempFiles);
                File audio = fragment.getNarrationAttachmentId() == null
                        ? null : downloadAttachmentToFile(fragment.getNarrationAttachmentId(), orgId, tempFiles);
                if (image == null && video == null && audio == null) {
                    continue;
                }
                double duration = fragment.getDurationSec() == null ? 5 : fragment.getDurationSec();
                clips.add(new DramaVideoComposer.Clip(image, video, audio, duration));
            }
            if (clips.isEmpty()) {
                throw new IllegalStateException("本集还没有可合成的分镜画面，请先生成分镜画面");
            }
            finalMp4 = dramaVideoComposer.compose(clips);
            String attachmentId = saveEpisodeVideo(finalMp4, episode, orgId, userId);
            AiDramaEpisode current = requireEpisode(orgId, episodeId);
            current.setVideoAttachmentId(attachmentId);
            ObjectNode params = paramsNode(current.getParamsJson());
            params.put("compose_status", STATUS_COMPLETED);
            params.remove("compose_error");
            current.setParamsJson(params.isEmpty() ? null : params.toString());
            current.setUpdateTime(System.currentTimeMillis());
            episodeMapper.updateById(current);
            log.info("漫剧成片合成完成 episodeId={} attachmentId={}", episodeId, attachmentId);
        } catch (Exception e) {
            log.error("漫剧成片合成失败 episodeId={}, orgId={}", episodeId, orgId, e);
            markComposeFailed(orgId, episodeId, e);
        } finally {
            if (finalMp4 != null && finalMp4.exists()) {
                try {
                    Files.deleteIfExists(finalMp4.toPath());
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程
                }
            }
            for (File temp : tempFiles) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程
                }
            }
        }
    }

    // ==================== Phase 7：逐镜 Seedance 图生视频 ====================

    /** 提交逐镜视频任务：有画面的分镜调 Seedance 图生视频；已运行则返回 false 不重复入队 */
    public boolean generateFragmentsVideo(String episodeId, DramaGenerateRequest request,
                                          String orgId, String userId) {
        AiDramaEpisode episode = requireEpisode(orgId, episodeId);
        ObjectNode params = paramsNode(episode.getParamsJson());
        if (STATUS_RUNNING.equals(params.path("video_status").asText(null))) {
            return false;
        }
        params.put("video_status", STATUS_RUNNING);
        params.remove("video_error");
        params.remove("video_done");
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
        return true;
    }

    @Async
    public void generateFragmentsVideoAsync(String episodeId, List<String> fragmentIds,
                                            String orgId, String userId) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        List<AiDramaFragment> targets = new ArrayList<>();
        for (AiDramaFragment fragment : resolveTargetFragments(episodeId, fragmentIds)) {
            if (fragment.getCoverAttachmentId() != null) {
                targets.add(fragment);
            }
        }
        try {
            if (targets.isEmpty()) {
                throw new IllegalStateException("本集还没有可出视频的分镜画面，请先生成分镜画面");
            }
            AgentModel videoModel = resolveSeedanceModel(orgId);
            if (videoModel == null) {
                throw new GenericException("请先在「模型设置」中启用豆包 Seedance 视频模型");
            }
            // 逐镜出视频放开 3 并发（Seedance 单镜 1~3 分钟，串行 10 镜会到 30 分钟）
            int failed = 0;
            ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, targets.size()));
            try {
                CompletionService<Boolean> cs = new ExecutorCompletionService<>(pool);
                for (AiDramaFragment fragment : targets) {
                    cs.submit(() -> generateOneFragmentVideo(fragment, videoModel, episode, orgId, userId));
                }
                for (int i = 0; i < targets.size(); i++) {
                    try {
                        if (Boolean.FALSE.equals(cs.take().get())) {
                            failed++;
                        }
                    } catch (Exception e) {
                        failed++;
                        log.error("漫剧逐镜视频任务异常 episodeId={}", episodeId, e);
                    }
                }
            } finally {
                pool.shutdownNow();
            }
            markVideoDone(orgId, episodeId, failed, targets.size() - failed);
        } catch (Exception e) {
            log.error("漫剧逐镜视频生成失败 episodeId={}, orgId={}", episodeId, orgId, e);
            markVideoFailed(orgId, episodeId, e);
        }
    }

    // ==================== 私有：配音与合成辅助 ====================

    /** 配音音频附件落库（wav） */
    private String saveNarrationAttachment(File audio, AiDramaFragment fragment, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String ext = StringUtils.substringAfterLast(audio.getName(), ".").toLowerCase();
        if (!ext.matches("[a-z0-9]{1,5}")) {
            ext = "wav";
        }
        String fileName = "narration-" + fragment.getId() + "." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, fragment.getId(), attachmentId),
                StorageType.LOCAL.name(), fileName);
        fileCommonService.upload(audio, request);
        insertAttachment(attachmentId, fileName, ext, audio.length(), orgId, fragment.getId(), userId);
        return attachmentId;
    }

    /** 成片 mp4 附件落库 */
    private String saveEpisodeVideo(File video, AiDramaEpisode episode, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String ext = StringUtils.substringAfterLast(video.getName(), ".").toLowerCase();
        if (!ext.matches("[a-z0-9]{1,5}")) {
            ext = "mp4";
        }
        String fileName = "episode-" + episode.getId() + "." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, episode.getId(), attachmentId),
                StorageType.LOCAL.name(), fileName);
        fileCommonService.upload(video, request);
        insertAttachment(attachmentId, fileName, ext, video.length(), orgId, episode.getId(), userId);
        return attachmentId;
    }

    /** 从附件系统下载到本地临时文件（失败返回 null，交由上层按「无画面」处理） */
    private File downloadAttachmentToFile(String attachmentId, String orgId, List<File> tempFiles) {
        try {
            Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
            if (attachment == null) {
                return null;
            }
            FileRequest request = new FileRequest(
                    DefaultRepositoryDir.getTransferFileDir(orgId, attachment.getResourceId(), attachmentId),
                    StorageType.LOCAL.name(), attachment.getName());
            String ext = StringUtils.substringAfterLast(attachment.getName(), ".").toLowerCase();
            if (!ext.matches("[a-z0-9]{1,5}")) {
                ext = "bin";
            }
            try (InputStream in = fileCommonService.getFileInputStream(request)) {
                if (in == null) {
                    return null;
                }
                File temp = File.createTempFile("drama-att-", "." + ext);
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tempFiles.add(temp);
                return temp;
            }
        } catch (Exception e) {
            log.warn("漫剧附件下载失败 attachmentId={}", attachmentId, e);
            return null;
        }
    }

    /** 按音色类型选配音模型：克隆音色（阿里 voice_id）只能阿里云合成，系统音色走通用配音模型降级链 */
    private List<AgentModel> resolveTtsModelsForVoice(String orgId, String voice) {
        String cloneModel = agentModelService.resolveVoiceCloneModel(orgId);
        boolean cloneVoice = cloneModel != null && voice != null && voice.startsWith(cloneModel + "-");
        return cloneVoice ? agentModelService.resolveAliyunTtsModels(orgId) : agentModelService.resolveTtsModels(orgId);
    }

    /** 用给定音色合成任意文案，返回 WAV 字节；逐模型降级，全部失败抛异常 */
    private byte[] synthesizeNarration(String text, String voice, String orgId) {
        List<AgentModel> ttsModels = resolveTtsModelsForVoice(orgId, voice);
        Exception lastError = null;
        for (AgentModel model : ttsModels) {
            if (!agentModelService.hasTtsCredential(model)) {
                continue;
            }
            try {
                TtsConfig config = new TtsConfig();
                config.setModel(model.getModelName());
                config.setApiKey(model.getApiKey());
                config.setVolcCredential(VolcengineVoiceCredential.of(
                        model.getAppId(), model.getAccessToken(), model.getSecretKey()));
                config.setModelParams(model.getModelParams());
                config.setVoiceCloneModel(agentModelService.resolveVoiceCloneModel(orgId));
                TtsProvider tts = ttsProviderFactory.get(model.getProvider());
                return tts.synthesize(text, voice, config);
            } catch (Exception e) {
                lastError = e;
                log.warn("漫剧配音合成失败，降级尝试下一个，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "配音生成失败，请确认已配置配音模型" : lastError.getMessage());
    }

    /** 口播台词的一行：说话人（去修饰后的角色名，空为旁白）+ 台词 */
    private record NarrationLine(String role, String text) {}

    /** 说话人前缀：短角色名（可带括号修饰）+ 全角/半角冒号，如「邓总：」「旁白（VO）：」 */
    private static final Pattern SPEAKER = Pattern.compile(
            "([\\u4e00-\\u9fa5A-Za-z0-9·]{1,10}(?:[（(][^（）)]*[）)])?)[：:]");

    /** 把口播台词按行拆成「说话人 + 台词」列表；支持「角色：台词」/「旁白（VO）：台词」/ 无冒号整句旁白。
     *  一行里挤了多个说话人（如「邓总：…。彭经理：…。」）也逐段拆开，保证各用各的音色 */
    private List<NarrationLine> parseNarrationLines(String narration) {
        List<NarrationLine> lines = new ArrayList<>();
        if (StringUtils.isBlank(narration)) {
            return lines;
        }
        for (String seg : narration.split("\\R")) {
            String line = seg.trim();
            if (line.isEmpty()) {
                continue;
            }
            lines.addAll(splitSpeakers(line));
        }
        return lines;
    }

    /** 把一行台词按说话人标记拆成若干「角色 + 台词」；行内无标记则整行算旁白 */
    private List<NarrationLine> splitSpeakers(String line) {
        List<NarrationLine> result = new ArrayList<>();
        Matcher matcher = SPEAKER.matcher(line);
        List<String> roles = new ArrayList<>();
        List<Integer> roleStarts = new ArrayList<>();
        List<Integer> textStarts = new ArrayList<>();
        while (matcher.find()) {
            int start = matcher.start(1);
            // 说话人标记须在行首或紧跟句末标点后，避免把台词里的「他说：…」这类冒号误判成新说话人
            if (start > 0 && !isSentenceEnd(line.charAt(start - 1))) {
                continue;
            }
            roles.add(cleanSpeaker(matcher.group(1)));
            roleStarts.add(start);
            textStarts.add(matcher.end());
        }
        if (roles.isEmpty()) {
            result.add(new NarrationLine("", line));
            return result;
        }
        if (roleStarts.get(0) > 0) {
            String leading = line.substring(0, roleStarts.get(0)).trim();
            if (!leading.isEmpty()) {
                result.add(new NarrationLine("", leading));
            }
        }
        for (int i = 0; i < roles.size(); i++) {
            int start = textStarts.get(i);
            int end = i + 1 < roles.size() ? roleStarts.get(i + 1) : line.length();
            String text = line.substring(start, end).trim();
            if (!text.isEmpty()) {
                result.add(new NarrationLine(roles.get(i), text));
            }
        }
        return result;
    }

    /** 句末标点（含闭合括号/空白）：说话人标记只有紧跟这些字符（或行首）才算真正换人 */
    private static boolean isSentenceEnd(char c) {
        return "。！？；…!?;）)]」』 \t".indexOf(c) >= 0;
    }

    /** 去掉说话人后面的括号修饰（如「彭经理（VO，微信语音感）」→「彭经理」） */
    private String cleanSpeaker(String speaker) {
        if (StringUtils.isBlank(speaker)) {
            return "";
        }
        return speaker.replaceAll("[（(][^）)]*[）)]", "").trim();
    }

    /** 合成一镜口播：单句直接合成，多句按角色音色逐句合成后按句序拼接 */
    private byte[] synthesizeNarrationLines(List<NarrationLine> lines, String projectId, String orgId,
                                            String defaultVoice) {
        if (lines.isEmpty()) {
            throw new GenericException("口播台词为空");
        }
        if (lines.size() == 1) {
            NarrationLine line = lines.get(0);
            return synthesizeNarration(line.text(), resolveLineVoice(line.role(), projectId, defaultVoice), orgId);
        }
        return synthesizeDialogue(lines, projectId, orgId, defaultVoice);
    }

    /** 多说话人对白：逐句用各自角色音色合成，再用 ffmpeg 统一重采样后按序拼接成一段 wav */
    private byte[] synthesizeDialogue(List<NarrationLine> lines, String projectId, String orgId, String defaultVoice) {
        List<File> segFiles = new ArrayList<>();
        File concatOut = null;
        try {
            for (NarrationLine line : lines) {
                String voice = resolveLineVoice(line.role(), projectId, defaultVoice);
                byte[] seg = synthesizeNarration(line.text(), voice, orgId);
                File segFile = File.createTempFile("drama-line-", ".wav");
                Files.write(segFile.toPath(), seg);
                segFiles.add(segFile);
            }
            concatOut = File.createTempFile("drama-concat-", ".wav");
            concatWavs(segFiles, concatOut);
            return Files.readAllBytes(concatOut.toPath());
        } catch (Exception e) {
            throw new GenericException("对白音频拼接失败：" + e.getMessage(), e);
        } finally {
            for (File f : segFiles) {
                try {
                    Files.deleteIfExists(f.toPath());
                } catch (IOException ignored) {
                    // 忽略临时文件清理失败
                }
            }
            if (concatOut != null) {
                try {
                    Files.deleteIfExists(concatOut.toPath());
                } catch (IOException ignored) {
                    // 忽略临时文件清理失败
                }
            }
        }
    }

    /** 某句台词的实际音色：优先角色在资产库绑定的音色，未绑定回退默认音色 */
    private String resolveLineVoice(String role, String projectId, String defaultVoice) {
        return StringUtils.defaultIfBlank(resolveRoleVoice(role, projectId), defaultVoice);
    }

    /** 按角色名查资产库(character)绑定的音色；精确名优先，包含关系兜底；查不到返回 null */
    private String resolveRoleVoice(String role, String projectId) {
        if (StringUtils.isBlank(role) || StringUtils.isBlank(projectId)) {
            return null;
        }
        List<AiDramaAsset> characters = assetMapper.selectListByLambda(new LambdaQueryWrapper<AiDramaAsset>()
                .eq(AiDramaAsset::getProjectId, projectId)
                .eq(AiDramaAsset::getType, "character"));
        for (AiDramaAsset asset : characters) {
            if (role.equals(asset.getName())) {
                return asset.getVoice();
            }
        }
        for (AiDramaAsset asset : characters) {
            String name = asset.getName();
            if (StringUtils.isNotBlank(name) && (name.contains(role) || role.contains(name))) {
                return asset.getVoice();
            }
        }
        return null;
    }

    /** 用 ffmpeg 把多个 wav 统一重采样到 24k 单声道并按序拼接成一段 wav */
    private void concatWavs(List<File> wavs, File output) throws Exception {
        if (wavs.size() == 1) {
            Files.copy(wavs.get(0).toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        for (File wav : wavs) {
            command.add("-i");
            command.add(wav.getAbsolutePath());
        }
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < wavs.size(); i++) {
            filter.append('[').append(i).append(":a]aresample=24000,aformat=sample_fmts=s16:channel_layouts=mono[a")
                    .append(i).append("];");
        }
        for (int i = 0; i < wavs.size(); i++) {
            filter.append("[a").append(i).append(']');
        }
        filter.append("concat=n=").append(wavs.size()).append(":v=0:a=1[out]");
        command.add("-filter_complex");
        command.add(filter.toString());
        command.add("-map");
        command.add("[out]");
        command.add(output.getAbsolutePath());
        runFfmpeg(command);
    }

    /** 执行 ffmpeg，丢弃输出防阻塞，超时强杀，非零退出抛异常 */
    private void runFfmpeg(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        byte[] buffer = new byte[4096];
        try (InputStream in = process.getInputStream()) {
            while (in.read(buffer) != -1) {
                // 丢弃输出，避免缓冲区写满阻塞
            }
        }
        boolean finished = process.waitFor(3, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new GenericException("ffmpeg 执行超时");
        }
        if (process.exitValue() != 0) {
            throw new GenericException("ffmpeg 执行失败(exit=" + process.exitValue() + ")");
        }
    }

    /** 整集配音收尾：全成功 completed，任一失败 failed 并带错误摘要 */
    private void markNarrationDone(String orgId, String episodeId, int failed, int done, String error) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        ObjectNode params = paramsNode(episode.getParamsJson());
        params.put("narration_status", failed > 0 ? STATUS_FAILED : STATUS_COMPLETED);
        params.put("narration_done", done);
        if (failed > 0) {
            params.put("narration_error", StringUtils.left(error, 500));
        } else {
            params.remove("narration_error");
        }
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
    }

    private void markComposeFailed(String orgId, String episodeId, Exception e) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        ObjectNode params = paramsNode(episode.getParamsJson());
        params.put("compose_status", STATUS_FAILED);
        params.put("compose_error", errorText(e));
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
    }

    /** 单镜图生视频：下载分镜画面 → Seedance → 上传回填 videoAttachmentId；成功 true / 失败 false */
    private boolean generateOneFragmentVideo(AiDramaFragment fragment, AgentModel model, AiDramaEpisode episode,
                                             String orgId, String userId) {
        File out = null;
        try {
            List<File> tempFiles = new ArrayList<>();
            File image = downloadAttachmentToFile(fragment.getCoverAttachmentId(), orgId, tempFiles);
            if (image == null) {
                markFragmentVideoFailed(orgId, fragment.getId(), "分镜画面缺失");
                return false;
            }
            String prompt = buildFragmentVideoPrompt(fragment, episode, orgId);
            out = File.createTempFile("drama-video-", ".mp4");
            seedanceVideoProvider.generate(model.getApiUrl(), model.getApiKey(), model.getModelName(), prompt, image, out);
            String attachmentId = saveFragmentVideo(out, fragment, orgId, userId);
            for (File temp : tempFiles) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程
                }
            }
            AiDramaFragment current = findFragment(orgId, fragment.getId());
            if (current != null) {
                current.setVideoAttachmentId(attachmentId);
                ObjectNode params = paramsNode(current.getParamsJson());
                params.put("video_status", STATUS_COMPLETED);
                params.remove("video_error");
                current.setParamsJson(params.isEmpty() ? null : params.toString());
                current.setUpdateTime(System.currentTimeMillis());
                fragmentMapper.updateById(current);
            }
            return true;
        } catch (Exception e) {
            log.error("漫剧分镜视频生成失败 fragmentId={}, orgId={}", fragment.getId(), orgId, e);
            markFragmentVideoFailed(orgId, fragment.getId(), errorText(e));
            return false;
        } finally {
            if (out != null && out.exists()) {
                try {
                    Files.deleteIfExists(out.toPath());
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程
                }
            }
        }
    }

    /** 图生视频提示词：分镜画面描述 + 风格 + 角色锚点，追加运镜要求 */
    private String buildFragmentVideoPrompt(AiDramaFragment fragment, AiDramaEpisode episode, String orgId) {
        return buildFragmentImagePrompt(fragment, episode, orgId) + "。画面缓慢运镜，主体自然动作，影视级动态镜头。";
    }

    /** 解析 Seedance 视频模型（豆包方舟 seedance 系列，取第一个可用） */
    private AgentModel resolveSeedanceModel(String orgId) {
        for (AgentModel model : agentModelService.resolveVideoModels(orgId)) {
            if ("豆包".equals(model.getProvider())
                    && StringUtils.defaultString(model.getModelName()).toLowerCase().contains("seedance")) {
                return model;
            }
        }
        return null;
    }

    /** 镜头视频附件落库（mp4） */
    private String saveFragmentVideo(File video, AiDramaFragment fragment, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String ext = StringUtils.substringAfterLast(video.getName(), ".").toLowerCase();
        if (!ext.matches("[a-z0-9]{1,5}")) {
            ext = "mp4";
        }
        String fileName = "fragment-video-" + fragment.getId() + "." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, fragment.getId(), attachmentId),
                StorageType.LOCAL.name(), fileName);
        fileCommonService.upload(video, request);
        insertAttachment(attachmentId, fileName, ext, video.length(), orgId, fragment.getId(), userId);
        return attachmentId;
    }

    /** 逐镜视频收尾：全成功 completed，任一失败 failed */
    private void markVideoDone(String orgId, String episodeId, int failed, int done) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        ObjectNode params = paramsNode(episode.getParamsJson());
        params.put("video_status", failed > 0 ? STATUS_FAILED : STATUS_COMPLETED);
        params.put("video_done", done);
        if (failed > 0) {
            params.put("video_error", "有 " + failed + " 镜出视频失败");
        } else {
            params.remove("video_error");
        }
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
    }

    private void markVideoFailed(String orgId, String episodeId, Exception e) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        ObjectNode params = paramsNode(episode.getParamsJson());
        params.put("video_status", STATUS_FAILED);
        params.put("video_error", errorText(e));
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
    }

    private void markFragmentVideoFailed(String orgId, String fragmentId, String error) {
        AiDramaFragment fragment = findFragment(orgId, fragmentId);
        if (fragment == null) {
            return;
        }
        ObjectNode params = paramsNode(fragment.getParamsJson());
        params.put("video_status", STATUS_FAILED);
        params.put("video_error", StringUtils.left(error, 500));
        fragment.setParamsJson(params.isEmpty() ? null : params.toString());
        fragment.setUpdateTime(System.currentTimeMillis());
        fragmentMapper.updateById(fragment);
    }

    /** 依脚本分集正文补建/更新 ai_drama_episode 行（分镜挂载在分集行上） */
    private void ensureEpisodeRows(String orgId, String projectId, String userId) {
        AiDramaScript script = findScript(orgId, projectId);
        if (script == null) {
            return;
        }
        Map<Integer, AiDramaEpisode> existing = new HashMap<>();
        for (AiDramaEpisode ep : episodeMapper.selectListByLambda(new LambdaQueryWrapper<AiDramaEpisode>()
                .eq(AiDramaEpisode::getProjectId, projectId))) {
            if (ep.getSortOrder() != null) {
                existing.put(ep.getSortOrder(), ep);
            }
        }
        long now = System.currentTimeMillis();
        for (ObjectNode row : existingEpisodes(script.getEpisodeContentJson())) {
            int number = row.path("episodeNumber").asInt(0);
            if (number < 1) {
                continue;
            }
            String title = row.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = "第 " + number + " 集";
            }
            AiDramaEpisode ep = existing.get(number);
            if (ep == null) {
                ep = new AiDramaEpisode();
                ep.setId(IDGenerator.nextStr());
                ep.setOrganizationId(orgId);
                ep.setProjectId(projectId);
                ep.setName(title);
                ep.setSortOrder(number);
                ep.setCreateUser(userId);
                ep.setUpdateUser(userId);
                ep.setCreateTime(now);
                ep.setUpdateTime(now);
                episodeMapper.insert(ep);
            } else if (!title.equals(StringUtils.defaultString(ep.getName()))) {
                ep.setName(title);
                ep.setUpdateTime(now);
                episodeMapper.updateById(ep);
            }
        }
    }

    // ==================== 私有：分镜规划辅助 ====================

    private boolean isFragmentPlanning(AiDramaEpisode episode) {
        if ("planning".equals(episode.getStatus())) {
            return true;
        }
        return "generating".equals(paramsNode(episode.getParamsJson()).path("fragment_plan_status").asText(null));
    }

    private List<AiDramaFragment> runFragmentPlan(AiDramaEpisode episode, String orgId, String userId) {
        AiDramaScript script = findScript(orgId, episode.getProjectId());
        if (script == null || StringUtils.isBlank(script.getEpisodeContentJson())) {
            throw new GenericException("请先生成分集剧本");
        }
        int epNumber = episode.getSortOrder() == null ? 0 : episode.getSortOrder();
        String body = "";
        for (ObjectNode ep : existingEpisodes(script.getEpisodeContentJson())) {
            if (ep.path("episodeNumber").asInt(0) == epNumber) {
                body = ep.path("body").asText("");
                break;
            }
        }
        if (StringUtils.isBlank(body)) {
            throw new GenericException("本集剧本正文为空，请先生成分集剧本");
        }
        List<AiDramaAsset> assets = listAssets(orgId, episode.getProjectId());
        JsonNode summary = parseJsonOrNull(script.getSummaryJson());
        String userPrompt = buildFragmentPlanUserPrompt(episode, body, assets, summary);
        JsonNode result = chatJson(List.of(
                new LlmMessage("system", FRAGMENT_PLAN_SYSTEM_PROMPT),
                new LlmMessage("user", userPrompt)), orgId, userId);
        JsonNode items = result.path("fragments");
        if (!items.isArray()) {
            items = result.has("shots") ? result.path("shots") : result.path("storyboard");
        }
        if (!items.isArray() || items.isEmpty()) {
            throw new IllegalStateException("AI 分镜结果为空，请重试");
        }
        List<AiDramaFragment> drafts = normalizeFragmentItems(items, assets);
        if (drafts.isEmpty()) {
            throw new IllegalStateException("AI 分镜规范化后为空，请重试");
        }
        return drafts;
    }

    private List<AiDramaFragment> normalizeFragmentItems(JsonNode items, List<AiDramaAsset> assets) {
        List<AiDramaAsset> sceneAssets = new ArrayList<>();
        List<AiDramaAsset> characterAssets = new ArrayList<>();
        List<AiDramaAsset> propAssets = new ArrayList<>();
        for (AiDramaAsset asset : assets) {
            switch (StringUtils.defaultString(asset.getType())) {
                case "character" -> characterAssets.add(asset);
                case "scene" -> sceneAssets.add(asset);
                case "prop" -> propAssets.add(asset);
                default -> { }
            }
        }
        List<AiDramaFragment> drafts = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isObject()) {
                continue;
            }
            String content = item.path("content").asText("").trim();
            String narration = item.path("narration").asText("").trim();
            if (content.isEmpty() && narration.isEmpty()) {
                continue;
            }
            if (drafts.size() >= MAX_FRAGMENTS_PER_EPISODE) {
                break;
            }
            AiDramaFragment fragment = new AiDramaFragment();
            fragment.setContent(StringUtils.trimToNull(content));
            fragment.setNarration(StringUtils.trimToNull(narration));
            fragment.setDurationSec(clampFragmentDuration(
                    item.path("duration_sec").asInt(item.path("durationSec").asInt(0))));
            fragment.setAssetIdsJson(buildAssetIdsJson(item, sceneAssets, characterAssets, propAssets));
            fragment.setStatus("pending");
            drafts.add(fragment);
        }
        return drafts;
    }

    private String buildAssetIdsJson(JsonNode item, List<AiDramaAsset> sceneAssets,
                                     List<AiDramaAsset> characterAssets, List<AiDramaAsset> propAssets) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        AiDramaAsset scene = findAssetByName(sceneAssets,
                item.path("scene_name").asText(item.path("sceneName").asText("")));
        if (scene != null) {
            ids.add(scene.getId());
        }
        int charCount = 0;
        for (String name : coerceNameList(item.path("character_names"))) {
            if (charCount >= FRAGMENT_MAX_CHARACTERS) {
                break;
            }
            AiDramaAsset asset = findAssetByName(characterAssets, name);
            if (asset != null && ids.add(asset.getId())) {
                charCount++;
            }
        }
        int propCount = 0;
        for (String name : coerceNameList(item.path("prop_names"))) {
            if (propCount >= FRAGMENT_MAX_PROPS) {
                break;
            }
            AiDramaAsset asset = findAssetByName(propAssets, name);
            if (asset != null && ids.add(asset.getId())) {
                propCount++;
            }
        }
        if (ids.isEmpty()) {
            return null;
        }
        List<String> limited = new ArrayList<>(ids);
        if (limited.size() > FRAGMENT_MAX_ASSETS) {
            limited = limited.subList(0, FRAGMENT_MAX_ASSETS);
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < limited.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(limited.get(i)).append('"');
        }
        return sb.append(']').toString();
    }

    private List<String> coerceNameList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node == null || node.isNull()) {
            return result;
        }
        if (node.isArray()) {
            for (JsonNode n : node) {
                String name = n.asText("").trim();
                if (!name.isEmpty()) {
                    result.add(name);
                }
            }
        } else {
            for (String part : node.asText("").split("[、，,/|]")) {
                String name = part.trim();
                if (!name.isEmpty()) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    private AiDramaAsset findAssetByName(List<AiDramaAsset> assets, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String target = name.trim();
        for (AiDramaAsset asset : assets) {
            if (target.equals(StringUtils.defaultString(asset.getName()).trim())) {
                return asset;
            }
        }
        return null;
    }

    private int clampFragmentDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return 8;
        }
        return Math.max(FRAGMENT_DURATION_MIN, Math.min(seconds, FRAGMENT_DURATION_MAX));
    }

    // ==================== 私有：分镜出图 ====================

    private void generateFragmentImage(String fragmentId, String orgId, String userId) {
        AiDramaFragment fragment = findFragment(orgId, fragmentId);
        if (fragment == null || !STATUS_RUNNING.equals(fragment.getStatus())) {
            return;
        }
        AiDramaEpisode episode = findEpisode(orgId, fragment.getEpisodeId());
        if (episode == null) {
            return;
        }
        List<File> tempFiles = new ArrayList<>();
        File temp = null;
        try {
            List<AgentModel> imageModels = agentModelService.resolveImageModels(orgId);
            if (imageModels.isEmpty()) {
                throw new GenericException("请先在「模型设置」中启用豆包 Seedream 图片模型");
            }
            AgentModel model = imageModels.get(0);
            String prompt = buildFragmentImagePrompt(fragment, episode, orgId);
            // 分步视觉锚点：先拿场景图「图生图」定版出一张统一空背景（缓存复用），再与各角色形象照一起传给模型合成人物
            String backgroundUri = resolveFragmentBackground(fragment, orgId, model, tempFiles);
            List<String> referenceImages = new ArrayList<>();
            List<String> characterNames = new ArrayList<>();
            for (AiDramaAsset ref : resolveFragmentCharacterReferences(fragment, orgId)) {
                String uri = attachmentDataUri(ref.getCoverAttachmentId(), orgId, tempFiles);
                if (uri != null) {
                    referenceImages.add(uri);
                    characterNames.add(StringUtils.defaultIfBlank(ref.getName(), "角色"));
                }
            }
            LlmProvider provider = llmProviderFactory.get(model.getProvider());
            LlmImageRequest request = new LlmImageRequest();
            request.setModel(model.getModelName());
            request.setBaseUrl(StringUtils.defaultIfBlank(model.getApiUrl(),
                    "https://ark.cn-beijing.volces.com/api/v3"));
            request.setApiKey(model.getApiKey());
            request.setSize("1080x1920");
            request.setPrompt(prompt);
            if (backgroundUri != null || !referenceImages.isEmpty()) {
                // 背景图放第 1 张、人物形象照放后续，按序号点名让模型区分「背景」与「人脸」
                if (backgroundUri != null) {
                    referenceImages.add(0, backgroundUri);
                }
                request.setImage(referenceImages);
                StringBuilder instruction = new StringBuilder();
                if (backgroundUri != null) {
                    instruction.append("参考图第 1 张是背景环境，其空间布局、家具陈设、墙面、地面、光线与视角严格保持该图一致，不要改变场景；");
                }
                if (!characterNames.isEmpty()) {
                    instruction.append("人物 ").append(String.join("、", characterNames))
                            .append(" 的面部五官、脸型、发型、肤色与年龄感严格保持其形象照一致，不要改变人物相貌；");
                }
                // 带参考图时剔除文字里的五官/发型/年龄描述，避免文字与照片打架
                String stripped = stripFaceDescription(prompt);
                request.setPrompt(instruction
                        + "服装、动作、光线、风格按以下描述生成："
                        + (stripped != null ? stripped : prompt));
            }
            byte[] bytes = provider.generateImage(request);
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("图片模型未返回图片数据");
            }
            temp = File.createTempFile("drama-fragment", ".png");
            Files.write(temp.toPath(), bytes);
            String attachmentId = saveFragmentImage(temp, fragment, orgId, userId);

            AiDramaFragment current = findFragment(orgId, fragmentId);
            if (current == null) {
                return;
            }
            current.setCoverAttachmentId(attachmentId);
            current.setStatus(STATUS_COMPLETED);
            current.setUpdateTime(System.currentTimeMillis());
            fragmentMapper.updateById(current);
        } catch (Exception e) {
            log.error("漫剧分镜画面生成失败 fragmentId={}, orgId={}", fragmentId, orgId, e);
            AiDramaFragment current = findFragment(orgId, fragmentId);
            if (current != null) {
                current.setStatus(STATUS_FAILED);
                current.setUpdateTime(System.currentTimeMillis());
                fragmentMapper.updateById(current);
            }
        } finally {
            for (File ref : tempFiles) {
                if (ref != null && ref.exists()) {
                    try {
                        Files.deleteIfExists(ref.toPath());
                    } catch (IOException ignored) {
                        // 清理临时文件失败不影响主流程
                    }
                }
            }
            if (temp != null && temp.exists()) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程
                }
            }
        }
    }

    private String buildFragmentImagePrompt(AiDramaFragment fragment, AiDramaEpisode episode, String orgId) {
        // 画风先行：先定渲染风格，再给镜头内容与角色锚点
        StringBuilder prompt = new StringBuilder();
        AiDramaScript script = findScript(orgId, episode.getProjectId());
        if (script != null) {
            String styleId = paramsNode(script.getParamsJson()).path("image_style_id").asText(null);
            String stylePrompt = DramaImageStyles.prompt(styleId);
            if (StringUtils.isNotBlank(stylePrompt)) {
                prompt.append(stylePrompt).append("。");
            }
        }
        prompt.append(StringUtils.defaultString(fragment.getContent()).trim());
        Map<String, AiDramaAsset> byId = new HashMap<>();
        for (AiDramaAsset asset : listAssets(orgId, episode.getProjectId())) {
            byId.put(asset.getId(), asset);
        }
        for (String assetId : parseAssetIds(fragment.getAssetIdsJson())) {
            AiDramaAsset asset = byId.get(assetId);
            if (asset == null) {
                continue;
            }
            String visual = assetVisualPrompt(asset);
            if (StringUtils.isNotBlank(visual)) {
                prompt.append("。").append(StringUtils.defaultString(asset.getName())).append("：").append(visual);
            }
        }
        return prompt.toString();
    }

    private List<String> parseAssetIds(String assetIdsJson) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(assetIdsJson)) {
            return result;
        }
        JsonNode node = parseJsonOrNull(assetIdsJson);
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                String id = n.asText(null);
                if (StringUtils.isNotBlank(id)) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    private String assetVisualPrompt(AiDramaAsset asset) {
        ObjectNode params = paramsNode(asset.getParamsJson());
        // 生图优先用干净的 visualImage（纯画面描述）；visualPrompt 混入了身份/定位/标签/性格等剧本元数据，不适合直接生图
        String visual = params.path("visualImage").asText("");
        if (StringUtils.isBlank(visual)) {
            visual = params.path("visualPrompt").asText("");
        }
        return visual.trim();
    }

    /** 分镜引用的「角色」资产里已出图的形象照（视觉锚点），最多 3 位与 FRAGMENT_MAX_CHARACTERS 对齐 */
    private List<AiDramaAsset> resolveFragmentCharacterReferences(AiDramaFragment fragment, String orgId) {
        List<AiDramaAsset> refs = new ArrayList<>();
        for (String assetId : parseAssetIds(fragment.getAssetIdsJson())) {
            AiDramaAsset asset = findAsset(orgId, assetId);
            if (asset == null || !"character".equals(asset.getType())
                    || StringUtils.isBlank(asset.getCoverAttachmentId())) {
                continue;
            }
            refs.add(asset);
            if (refs.size() >= FRAGMENT_MAX_CHARACTERS) {
                break;
            }
        }
        return refs;
    }

    /** 分镜引用的「场景」资产里已出图的环境图（背景视觉锚点），保证同一环境跨镜不变 */
    private List<AiDramaAsset> resolveFragmentSceneReferences(AiDramaFragment fragment, String orgId) {
        List<AiDramaAsset> refs = new ArrayList<>();
        for (String assetId : parseAssetIds(fragment.getAssetIdsJson())) {
            AiDramaAsset asset = findAsset(orgId, assetId);
            if (asset == null || !"scene".equals(asset.getType())
                    || StringUtils.isBlank(asset.getCoverAttachmentId())) {
                continue;
            }
            refs.add(asset);
        }
        return refs;
    }

    /** 附件 → Seedream 图生图 base64 data URI（读取失败返回 null） */
    private String attachmentDataUri(String attachmentId, String orgId, List<File> tempFiles) {
        File file = downloadAttachmentToFile(attachmentId, orgId, tempFiles);
        if (file == null) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String ext = StringUtils.substringAfterLast(file.getName(), ".").toLowerCase();
            String mime = "png".equals(ext) ? "image/png" : "image/jpeg";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.warn("漫剧参考图读取失败 attachmentId={}", attachmentId, e);
            return null;
        }
    }

    /** 分镜引用场景 → 定版空背景 data URI（缓存复用；出图失败回退用场景原图） */
    private String resolveFragmentBackground(AiDramaFragment fragment, String orgId, AgentModel model,
            List<File> tempFiles) {
        for (AiDramaAsset scene : resolveFragmentSceneReferences(fragment, orgId)) {
            String cacheKey = orgId + ":" + scene.getId();
            String cached = sceneBackgroundCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            String coverUri = attachmentDataUri(scene.getCoverAttachmentId(), orgId, tempFiles);
            if (coverUri == null) {
                continue;
            }
            String fixed = generateSceneBackground(coverUri, model, orgId);
            if (fixed != null) {
                sceneBackgroundCache.put(cacheKey, fixed);
                return fixed;
            }
            return coverUri;
        }
        return null;
    }

    /** 用场景图「图生图」生成一张无人物、统一视角的定版背景，返回 base64 data URI（失败返回 null） */
    private String generateSceneBackground(String coverUri, AgentModel model, String orgId) {
        try {
            LlmProvider provider = llmProviderFactory.get(model.getProvider());
            LlmImageRequest request = new LlmImageRequest();
            request.setModel(model.getModelName());
            request.setBaseUrl(StringUtils.defaultIfBlank(model.getApiUrl(),
                    "https://ark.cn-beijing.volces.com/api/v3"));
            request.setApiKey(model.getApiKey());
            request.setSize("1080x1920");
            request.setImage(List.of(coverUri));
            request.setPrompt("以参考图为背景，生成一张竖屏空房间背景图，保持房间布局、家具陈设、墙面、地面、"
                    + "光线与视角与参考图完全一致；画面中不要出现任何人物、动物、文字或水印。");
            byte[] bytes = provider.generateImage(request);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return bytesToDataUri(bytes, "image/jpeg");
        } catch (Exception e) {
            log.warn("漫剧场景背景定版失败，回退用原场景图 orgId={}, model={}", orgId, model.getModelName(), e);
            return null;
        }
    }

    private String bytesToDataUri(byte[] bytes, String mime) {
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String saveFragmentImage(File image, AiDramaFragment fragment, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String ext = StringUtils.substringAfterLast(image.getName(), ".").toLowerCase();
        if (!ext.matches("[a-z0-9]{1,5}")) {
            ext = "png";
        }
        String fileName = "fragment-" + fragment.getId() + "." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, fragment.getId(), attachmentId),
                StorageType.LOCAL.name(), fileName);
        fileCommonService.upload(image, request);
        insertAttachment(attachmentId, fileName, ext, image.length(), orgId, fragment.getId(), userId);
        return attachmentId;
    }

    private void markFragmentPlanFailed(String orgId, String episodeId, Exception e) {
        AiDramaEpisode episode = findEpisode(orgId, episodeId);
        if (episode == null) {
            return;
        }
        episode.setStatus(STATUS_FAILED);
        ObjectNode params = paramsNode(episode.getParamsJson());
        params.put("fragment_plan_status", STATUS_FAILED);
        params.put("fragment_plan_error", errorText(e));
        params.remove("fragment_plan_generating_at");
        episode.setParamsJson(params.isEmpty() ? null : params.toString());
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
    }

    private AiDramaEpisode findEpisode(String orgId, String id) {
        AiDramaEpisode episode = episodeMapper.selectByPrimaryKey(id);
        return episode == null || !orgId.equals(episode.getOrganizationId()) ? null : episode;
    }

    private AiDramaEpisode requireEpisode(String orgId, String id) {
        AiDramaEpisode episode = findEpisode(orgId, id);
        if (episode == null) {
            throw new GenericException("漫剧分集不存在");
        }
        return episode;
    }

    private List<AiDramaFragment> listFragments(String episodeId) {
        LambdaQueryWrapper<AiDramaFragment> wrapper = new LambdaQueryWrapper<AiDramaFragment>()
                .eq(AiDramaFragment::getEpisodeId, episodeId);
        wrapper.orderByAsc(AiDramaFragment::getSortOrder);
        return fragmentMapper.selectListByLambda(wrapper);
    }

    private AiDramaFragment findFragment(String orgId, String id) {
        AiDramaFragment fragment = fragmentMapper.selectByPrimaryKey(id);
        return fragment == null || !orgId.equals(fragment.getOrganizationId()) ? null : fragment;
    }

    private List<AiDramaFragment> resolveTargetFragments(String episodeId, List<String> fragmentIds) {
        List<AiDramaFragment> all = listFragments(episodeId);
        if (fragmentIds == null || fragmentIds.isEmpty()) {
            return all;
        }
        Set<String> wanted = new HashSet<>(fragmentIds);
        List<AiDramaFragment> result = new ArrayList<>();
        for (AiDramaFragment fragment : all) {
            if (wanted.contains(fragment.getId())) {
                result.add(fragment);
            }
        }
        return result;
    }

    private String buildFragmentPlanUserPrompt(AiDramaEpisode episode, String body, List<AiDramaAsset> assets,
                                               JsonNode summary) {
        int epNumber = episode.getSortOrder() == null ? 0 : episode.getSortOrder();
        String epLabel = epNumber > 0 ? "第" + epNumber + "集" : "本集";
        String epName = StringUtils.defaultIfBlank(episode.getName(), "未命名");
        StringBuilder sb = new StringBuilder();
        sb.append("剧名：").append(summary == null ? "未命名短剧" : summary.path("seriesTitle").asText("未命名短剧"))
                .append('\n');
        sb.append("集号：").append(epLabel).append('\n');
        sb.append("分集标题：").append(epName).append('\n');
        sb.append("字幕需求：需要字幕").append('\n');
        sb.append('\n');
        sb.append("【开幕镜必须使用的标注信息｜请写入 fragments[0]】").append('\n');
        sb.append("- 集号原文：").append(epLabel).append('\n');
        sb.append("- 集名原文：").append(epName).append('\n');
        if (summary != null) {
            String seriesTitle = summary.path("seriesTitle").asText("").trim();
            String storyType = summary.path("storyType").asText("").trim();
            String coreHook = summary.path("coreHook").asText("").trim();
            String oneLineStory = summary.path("oneLineStory").asText("").trim();
            String synopsis = summary.path("synopsis").asText("").trim();
            if (!seriesTitle.isEmpty()) {
                sb.append("- 剧名原文：").append(seriesTitle).append('\n');
            }
            if (!storyType.isEmpty()) {
                sb.append("- 类型：").append(storyType).append('\n');
            }
            if (!coreHook.isEmpty()) {
                sb.append("- 钩子：").append(coreHook).append('\n');
            }
            if (!oneLineStory.isEmpty()) {
                sb.append("- 一句话故事：").append(oneLineStory).append('\n');
            }
            if (!synopsis.isEmpty()) {
                if (synopsis.length() > 420) {
                    synopsis = synopsis.substring(0, 419) + "…";
                }
                sb.append("- 故事梗概（背景参考）：").append(synopsis).append('\n');
            }
        }
        String bodyText = body.trim();
        int bodyLen = bodyText.replaceAll("\\s", "").length();
        sb.append('\n');
        sb.append("【篇幅硬约束｜必须遵守】").append('\n');
        sb.append("- fragments 数组长度 ≤ ").append(MAX_FRAGMENTS_PER_EPISODE)
                .append("（含开幕镜）；整集成片目标 60–90 秒。").append('\n');
        sb.append("- 同场景、同角色组的连续对白合并为一镜；跳过纯过渡/重复反应。").append('\n');
        sb.append("- 每镜 duration_sec 4–15 秒。").append('\n');
        sb.append("- 每镜最多 3 个出镜角色、2 件道具。").append('\n');
        if (bodyLen > 550) {
            sb.append('\n');
            sb.append("【场记偏长（约 ").append(bodyLen).append(" 字）】规划时请主动压缩：合并场次、删次要动作，仍只输出 ≤")
                    .append(MAX_FRAGMENTS_PER_EPISODE).append(" 条 fragments。").append('\n');
        }
        sb.append('\n');
        sb.append("【分集场记正文】").append('\n').append(bodyText).append('\n');
        sb.append('\n');
        sb.append("【可用资产目录｜拆镜时 scene_name / character_names / prop_names 尽量使用下列名称】").append('\n');
        if (assets.isEmpty()) {
            sb.append("（暂无资产）").append('\n');
        } else {
            for (AiDramaAsset asset : assets) {
                String kind = StringUtils.defaultString(asset.getType());
                String name = StringUtils.defaultString(asset.getName());
                ObjectNode assetParams = paramsNode(asset.getParamsJson());
                String role = assetParams.path("roleType").asText("");
                if (role.isEmpty()) {
                    role = assetParams.path("title").asText("");
                }
                sb.append("- [").append(kind).append("] name=").append(name);
                if (!role.isEmpty()) {
                    sb.append("｜").append(role);
                }
                sb.append('\n');
            }
        }
        sb.append('\n');
        sb.append("请输出 JSON：{\"fragments\":[...]}；第一条必须是开幕镜（is_opening=true，含集号与背景介绍）。");
        return sb.toString();
    }

    private static final String FRAGMENT_PLAN_SYSTEM_PROMPT = """
            你是短剧视频分镜导演，负责把「场记剧本」拆成适合 AI 视频模型逐条生成的分镜。

            ## 输出
            只输出一个 JSON 对象（不要 markdown、不要代码围栏）：
            {
              "fragments": [
                {
                  "duration_sec": 12,
                  "scene_name": "地点名或空字符串",
                  "character_names": ["本镜出镜角色名"],
                  "prop_names": ["本镜出现的道具名"],
                  "is_opening": false,
                  "content": "本镜画面描述（空镜/景别/运镜/动作/结束态）",
                  "narration": "本镜口播台词（角色：对白 或 旁白（VO）：内容），无则空字符串"
                }
              ]
            }

            ## 开幕镜（强制，fragments[0]）
            每集第一条必须是开幕镜，并设 "is_opening": true，用于交代「看什么剧、第几集、本集背景」：
            1. duration_sec 建议 5–10。
            2. content 须写清集号与集名（用用户提供的原文，不要改写数字）、背景介绍（1–3 句世界观/时代/地点氛围）、开场画面（空镜/远景建立主场景气氛）；开幕尽量少口播。
            3. 开幕镜 character_names 通常为空或极少；narration 可为空或一句话带过。

            ## 何时切新镜（满足任一即新开一条 fragment）
            1. 场景切换（地点 / 日夜 / 内外景）。
            2. 主要角色组合明显变化。
            3. 情绪段落切换（铺垫→冲突→反转→收束）。
            4. 叙事职责不同（开幕 ≠ 对白戏 ≠ 纯空镜）。
            5. 累计将超过约 15 秒。

            ## 硬性规则
            1. duration_sec 必须在 4–15 之间，优先 6–15；单镜叙事完整、可单独成片。
            2. 按剧情节奏与场面转换拆镜；整集 fragments 不得超过 10 条（含开幕镜），目标成片 60–90 秒。同场景连续对白优先合并为一镜，删去重复反应/过渡空镜，勿一句一对白一镜。
            3. content 只写画面、动作、景别、运镜，保留角色真实姓名；narration 只写口播（对白/旁白）。纯画面镜 narration 为空字符串。
            4. 禁止输出：### 场标题、出场人物行、【字幕】【BGM】【人物介绍】【片头】【背景介绍】、@asset、@duration。集号叠字等由系统后处理写入。
            5. character_names 只列本镜真正出镜且在 content 里被点到的角色名（与资产目录一致）；群演/兵丁/百姓可省略，每镜最多 3 人。prop_names 只列本镜真正用到的道具，最多 2 件；没有则空数组。每镜视觉资产合计（场景+角色+道具）不超过 6。
            6. 环境建立空镜可独立成镜；对白密集处按情绪段落合并。
            7. 保留关键冲突与转折；次要铺垫、重复情绪镜可合并或省略，不得为凑镜数碎切。
            8. 开幕镜之后不要再重复整集片头；集号只在开幕镜强调一次。

            ## 画面描写密度（强制，解决「分镜描述太少」）
            每条 content 必须信息完整，按公式写满，禁止一句话糊弄：
            主体 + 动作/姿态 + 场景环境 + 景别或运镜 + 结束态（本段结束时画面可见状态）。
            可选补：光影、天气、前后景层次、道具在手中的具体用法。

            ### 禁止的瘦写法（不合格）
            - 「禹站在河边。」「众人惊慌。」「空镜：山崩。」
            - 只有口播、几乎没有画面描写。
            - 把一整场戏压成一句摘要，不写空间关系与动作过程。

            ### 合格示例
            - 「全景：禹立于裂石崖边，右手开山斧贴身、左手定海针斜指岩缝，身后伯益执鞭跟近，应龙半空盘旋碎石飞溅；结束态：针尖已抵入岩缝，浊水自缝中渗出。」
            - 「特写：禹眉峰紧锁，目光盯紧岩缝水纹，定海针缓缓没入；结束态：水纹由细线变为涌动，禹抬眼示意伯益。」
            - 「空镜：黄河浊浪拍击老石，远坡百姓扶老携幼撤离，近岸芦苇被风压平；结束态：高坡人影渐远，河道仍汹涌。」

            ### 对白/旁白
            台词本身保持口语自然；narration 写成「角色：台词」或「旁白（VO）：台词」，禁止把画面动作写进口播。对白镜仍须至少 1 条画面描写（content 非空）。
            """;

    // ==================== 私有：资产抽取辅助 ====================

    private List<AiDramaAsset> listAssets(String orgId, String projectId) {
        return assetMapper.selectListByLambda(new LambdaQueryWrapper<AiDramaAsset>()
                .eq(AiDramaAsset::getProjectId, projectId));
    }

    private AiDramaAsset findAsset(String orgId, String id) {
        AiDramaAsset asset = assetMapper.selectByPrimaryKey(id);
        return asset == null || !orgId.equals(asset.getOrganizationId()) ? null : asset;
    }

    private AiDramaAsset requireAsset(String orgId, String id) {
        AiDramaAsset asset = findAsset(orgId, id);
        if (asset == null) {
            throw new GenericException("漫剧资产不存在");
        }
        return asset;
    }

    private boolean isAssetGenerating(AiDramaAsset asset) {
        if (STATUS_RUNNING.equals(asset.getStatus())) {
            return true;
        }
        ObjectNode params = paramsNode(asset.getParamsJson());
        return STATUS_RUNNING.equals(params.path("generation").path("status").asText(null));
    }

    private AiDramaAsset buildAssetRow(String orgId, String projectId, String type, String assetType,
                                       String name, String paramsJson, String userId) {
        long now = System.currentTimeMillis();
        AiDramaAsset asset = new AiDramaAsset();
        asset.setId(IDGenerator.nextStr());
        asset.setOrganizationId(orgId);
        asset.setProjectId(projectId);
        asset.setType(type);
        asset.setAssetType(assetType);
        asset.setName(name);
        asset.setParamsJson(paramsJson);
        asset.setCreateUser(userId);
        asset.setUpdateUser(userId);
        asset.setCreateTime(now);
        asset.setUpdateTime(now);
        return asset;
    }

    private String normalizeAssetName(String name) {
        String n = name == null ? "" : name.trim();
        return n.replaceAll("[\\s]+", " ").trim();
    }

    private String assetDedupeKey(String type, String name) {
        return StringUtils.defaultString(type, "") + "|" + StringUtils.defaultString(name, "");
    }

    /** 音色/声音标注名不作为角色（如「大禹（声音）」） */
    private String characterNameForSeed(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || n.contains("声音") || n.contains("音色")) {
            return null;
        }
        n = n.replaceAll("[（(][^）)]*[)）]", "").trim();
        return n.isEmpty() ? null : n;
    }

    private JsonNode findCharacter(JsonNode summary, String name, String norm) {
        JsonNode characters = summary.path("characters");
        if (characters.isArray()) {
            for (JsonNode ch : characters) {
                if (!ch.isObject()) {
                    continue;
                }
                String chName = ch.path("name").asText("").trim();
                if (chName.equals(name) || chName.equals(norm)) {
                    return ch;
                }
            }
        }
        return null;
    }

    /** 分集正文「出场人物：A、B、C」行里的角色名单（补摘要遗漏） */
    private Set<String> extractCastNames(String episodeContentJson) {
        Set<String> names = new LinkedHashSet<>();
        List<ObjectNode> episodes = existingEpisodes(episodeContentJson);
        for (ObjectNode ep : episodes) {
            String body = ep.path("body").asText("");
            for (String line : body.split("\\R")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("出场人物") && !trimmed.startsWith("出场角色")) {
                    continue;
                }
                int colon = trimmed.indexOf('：');
                if (colon < 0) {
                    colon = trimmed.indexOf(':');
                }
                if (colon < 0) {
                    continue;
                }
                for (String part : trimmed.substring(colon + 1).split("[、，,;；/|]+")) {
                    String n = part.trim();
                    if (!n.isEmpty()) {
                        names.add(n);
                    }
                }
            }
        }
        return names;
    }

    private List<String> extractSceneNames(String episodeContentJson) {
        List<String> scenes = new ArrayList<>();
        for (ObjectNode ep : existingEpisodes(episodeContentJson)) {
            String body = ep.path("body").asText("");
            for (String line : body.split("\\R")) {
                Matcher matcher = SCENE_HEAD_PATTERN.matcher(line);
                if (!matcher.find()) {
                    continue;
                }
                String scene = matcher.group(1).trim().split("／")[0].split("/")[0].trim();
                if (!scene.isEmpty() && !scenes.contains(scene) && scenes.size() < MAX_SCENE_ASSETS) {
                    scenes.add(scene);
                }
            }
        }
        return scenes;
    }

    /** 道具抽取（best-effort，失败返回空列表不阻断） */
    private List<String[]> extractProps(JsonNode summary, String episodeContentJson, String orgId, String userId) {
        try {
            String user = String.join("\n",
                    "剧本摘要：\n" + formatSummaryText(summary),
                    "",
                    "分集正文（节选）：\n" + formatEpisodeBodiesForProps(episodeContentJson),
                    "",
                    "请抽取 3-8 个可视觉化的关键道具（法器、信物、服饰配饰、标志性物件），"
                            + "每个给出 name 与 visualPrompt（50-120 字生图提示词）。");
            JsonNode data = chatJson(List.of(
                    new LlmMessage("system", PROP_EXTRACT_SYSTEM),
                    new LlmMessage("user", user)), orgId, userId);
            List<String[]> result = new ArrayList<>();
            JsonNode props = data.isArray() ? data : data.path("props");
            if (props.isArray()) {
                for (JsonNode item : props) {
                    if (!item.isObject()) {
                        continue;
                    }
                    String name = item.path("name").asText("").trim();
                    String visual = item.path("visualPrompt").asText("").trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    if (visual.isEmpty()) {
                        visual = "道具：" + name + "，影视级实物特写，材质清晰，构图居中";
                    }
                    result.add(new String[]{name, visual});
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("漫剧道具 LLM 抽取失败 projectId={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String buildCharacterPrompt(JsonNode ch) {
        List<String> parts = new ArrayList<>();
        String visual = ch.path("visualImage").asText("").trim();
        String title = ch.path("title").asText("").trim();
        String roleType = ch.path("roleType").asText("").trim();
        String coreTags = ch.path("coreTags").asText("").trim();
        String personality = ch.path("personality").asText("").trim();
        if (!visual.isEmpty()) {
            parts.add(visual);
        }
        if (!title.isEmpty()) {
            parts.add("身份：" + title);
        }
        if (!roleType.isEmpty()) {
            parts.add("定位：" + roleType);
        }
        if (!coreTags.isEmpty()) {
            parts.add("标签：" + coreTags);
        }
        if (!personality.isEmpty()) {
            parts.add("性格：" + personality);
        }
        return String.join("。", parts);
    }

    private String buildCharacterParamsJson(JsonNode ch, String name, String storyType) {
        ObjectNode params = MAPPER.createObjectNode();
        String prompt;
        if (ch != null && ch.isObject()) {
            prompt = buildCharacterPrompt(ch);
            params.put("visualImage", StringUtils.defaultIfBlank(ch.path("visualImage").asText(""), prompt));
            putIfText(params, "roleType", ch.path("roleType").asText(""));
            putIfText(params, "title", ch.path("title").asText(""));
            putIfText(params, "coreTags", ch.path("coreTags").asText(""));
            putIfText(params, "personality", ch.path("personality").asText(""));
        } else {
            prompt = "角色：" + name + "，短剧人物形象，与剧本剧情一致，五官清晰可辨识"
                    + (storyType.isEmpty() ? "" : "，风格：" + storyType);
            params.put("visualImage", prompt);
        }
        params.put("visualPrompt", prompt);
        params.put("kind", "character");
        params.put("aspectRatio", "3:4");
        return params.toString();
    }

    private String buildSceneParamsJson(String scene, String storyType) {
        ObjectNode params = MAPPER.createObjectNode();
        String prompt = "场景：" + scene + "，画面构图清晰，适合短剧分镜"
                + (storyType.isEmpty() ? "" : "，风格：" + storyType);
        params.put("visualPrompt", prompt);
        params.put("visualImage", prompt);
        params.put("kind", "scene");
        params.put("aspectRatio", "16:9");
        return params.toString();
    }

    private String buildPropParamsJson(String name, String visual) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("visualPrompt", visual);
        params.put("visualImage", visual);
        params.put("kind", "prop");
        params.put("aspectRatio", "1:1");
        return params.toString();
    }

    private void putIfText(ObjectNode params, String key, String value) {
        String v = value == null ? "" : value.trim();
        if (!v.isEmpty()) {
            params.put(key, v);
        }
    }

    private void markSeedDone(String orgId, String projectId, int created) {
        AiDramaProject project = findProject(orgId, projectId);
        if (project == null) {
            return;
        }
        ObjectNode params = paramsNode(project.getParamsJson());
        params.put("assets_seed_status", SEED_DONE);
        params.put("assets_seed_created", created);
        params.remove("assets_seed_error");
        params.remove("assets_seed_generating_at");
        project.setParamsJson(params.isEmpty() ? null : params.toString());
        project.setUpdateTime(System.currentTimeMillis());
        projectMapper.updateById(project);
    }

    private void markSeedFailed(String orgId, String projectId, Exception e) {
        AiDramaProject project = findProject(orgId, projectId);
        if (project == null) {
            return;
        }
        ObjectNode params = paramsNode(project.getParamsJson());
        params.put("assets_seed_status", SEED_FAILED);
        params.put("assets_seed_error", errorText(e));
        params.remove("assets_seed_generating_at");
        project.setParamsJson(params.isEmpty() ? null : params.toString());
        project.setUpdateTime(System.currentTimeMillis());
        projectMapper.updateById(project);
    }

    private void markAssetFailed(String orgId, String assetId, Exception e) {
        AiDramaAsset asset = findAsset(orgId, assetId);
        if (asset == null) {
            return;
        }
        String refId = resolveReferenceAttachmentId(asset);
        ObjectNode params = paramsNode(asset.getParamsJson());
        ObjectNode gen = params.putObject("generation");
        gen.put("status", STATUS_FAILED);
        gen.put("error", errorText(e));
        if (refId != null) {
            gen.put("reference_attachment_id", refId);
        }
        asset.setStatus(STATUS_FAILED);
        asset.setParamsJson(params.isEmpty() ? null : params.toString());
        asset.setUpdateTime(System.currentTimeMillis());
        assetMapper.updateById(asset);
    }

    // ==================== 私有：资产生图提示词与附件 ====================

    /** 解析「用户上传参考图」附件 ID：优先 persisted reference_attachment_id，其次 upload 来源的当前封面 */
    private String resolveReferenceAttachmentId(AiDramaAsset asset) {
        ObjectNode params = paramsNode(asset.getParamsJson());
        String refId = params.path("generation").path("reference_attachment_id").asText(null);
        if (StringUtils.isBlank(refId) && "upload".equals(params.path("generation").path("source").asText(null))) {
            refId = asset.getCoverAttachmentId();
        }
        return StringUtils.isBlank(refId) ? null : refId;
    }

    /** 取「用户上传的参考图」并转成 Seedream 图生图 base64 data URI；无参考图返回 null（退回纯文生图） */
    private String referenceImageDataUri(AiDramaAsset asset, String orgId, List<File> tempFiles) {
        String refId = resolveReferenceAttachmentId(asset);
        if (refId == null) {
            return null;
        }
        File refFile = downloadAttachmentToFile(refId, orgId, tempFiles);
        if (refFile == null) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(refFile.toPath());
            String ext = StringUtils.substringAfterLast(refFile.getName(), ".").toLowerCase();
            String mime = "png".equals(ext) ? "image/png" : "image/jpeg";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.warn("漫剧参考图读取失败 attachmentId={}", refId, e);
            return null;
        }
    }

    /** 带参考图生图时，需要剔除的面部/年龄/发型描述关键词（命中即整句丢弃，避免与参考图冲突） */
    private static final String[] FACE_DESC_KEYWORDS = {
            "岁", "年龄", "年纪", "脸型", "圆脸", "方脸", "长脸", "瓜子脸", "轮廓",
            "眉", "眼神", "眼睛", "眼眸", "鼻", "唇", "嘴",
            "发型", "发丝", "发胶", "发色", "头发", "背头", "长发", "短发", "卷发", "直发", "刘海",
            "胡", "肤色", "肤质", "颧", "下颌", "下巴",
            "身形", "身材", "体型", "身高", "挺拔", "偏瘦", "偏胖", "健壮",
            "相貌", "五官", "长相", "容貌"
    };

    /** 去掉提示词里描述年龄/长相/发型的句子，只留服装/场景/风格（面部以参考图为准，避免文字与照片打架） */
    private String stripFaceDescription(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            return null;
        }
        StringBuilder kept = new StringBuilder();
        for (String sentence : prompt.split("(?<=[。；;])")) {
            String t = sentence == null ? "" : sentence.trim();
            if (t.isEmpty()) {
                continue;
            }
            boolean face = false;
            for (String kw : FACE_DESC_KEYWORDS) {
                if (t.contains(kw)) {
                    face = true;
                    break;
                }
            }
            if (!face) {
                kept.append(t);
            }
        }
        String result = kept.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /** 组装生图提示词：画面风格先行 + 干净的画面描述（visualImage，不含剧本元数据） */
    private String buildAssetImagePrompt(AiDramaAsset asset) {
        String visual = assetVisualPrompt(asset);
        if (StringUtils.isBlank(visual)) {
            visual = (asset.getType() == null ? "资产" : asset.getType()) + "：" + StringUtils.defaultString(asset.getName());
        }
        AiDramaScript script = findScript(asset.getOrganizationId(), asset.getProjectId());
        if (script != null) {
            String styleId = paramsNode(script.getParamsJson()).path("image_style_id").asText(null);
            String stylePrompt = DramaImageStyles.prompt(styleId);
            if (StringUtils.isNotBlank(stylePrompt)) {
                // 画风先行：先定渲染风格，避免长写实描写压过画风
                return stylePrompt + "。画面内容：" + visual;
            }
        }
        return visual;
    }

    /** 资产比例 → Seedream 尺寸（角色 3:4 竖版、场景 16:9 横版、道具 1:1 方图） */
    private String imageSizeForAssetRatio(AiDramaAsset asset) {
        String ratio = paramsNode(asset.getParamsJson()).path("aspectRatio").asText("3:4");
        return switch (ratio) {
            case "16:9" -> "1920x1080";
            case "1:1" -> "1080x1080";
            case "3:4" -> "1080x1440";
            default -> "1080x1920";
        };
    }

    private String saveAssetImage(File image, AiDramaAsset asset, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String ext = StringUtils.substringAfterLast(image.getName(), ".").toLowerCase();
        if (!ext.matches("[a-z0-9]{1,5}")) {
            ext = "png";
        }
        String fileName = "asset-" + asset.getId() + "." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, asset.getId(), attachmentId),
                StorageType.LOCAL.name(), fileName);
        fileCommonService.upload(image, request);
        insertAttachment(attachmentId, fileName, ext, image.length(), orgId, asset.getId(), userId);
        return attachmentId;
    }

    private String saveAssetUpload(MultipartFile file, String ext, AiDramaAsset asset, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String fileName = "asset-" + asset.getId() + "." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, asset.getId(), attachmentId),
                StorageType.LOCAL.name(), fileName);
        fileCommonService.upload(file, request);
        insertAttachment(attachmentId, fileName, ext, file.getSize(), orgId, asset.getId(), userId);
        return attachmentId;
    }

    private void insertAttachment(String attachmentId, String fileName, String ext, long size,
                                  String orgId, String resourceId, String userId) {
        long now = System.currentTimeMillis();
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setName(fileName);
        attachment.setType(ext);
        attachment.setSize(size);
        attachment.setStorage(StorageType.LOCAL.name());
        attachment.setOrganizationId(orgId);
        attachment.setResourceId(resourceId);
        attachment.setCreateTime(now);
        attachment.setCreateUser(userId);
        attachment.setUpdateTime(now);
        attachment.setUpdateUser(userId);
        attachmentMapper.insert(attachment);
    }

    private String resolveImageExt(MultipartFile file) {
        String contentType = StringUtils.defaultString(file.getContentType()).toLowerCase();
        switch (contentType) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            case "image/gif":
                return "gif";
            default:
                break;
        }
        String suffix = StringUtils.substringAfterLast(
                StringUtils.defaultString(file.getOriginalFilename()), ".").toLowerCase();
        return switch (suffix) {
            case "jpg", "jpeg" -> "jpg";
            case "png", "webp", "gif" -> suffix;
            default -> null;
        };
    }

    private String formatEpisodeBodiesForProps(String episodeContentJson) {
        List<ObjectNode> episodes = existingEpisodes(episodeContentJson);
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (ObjectNode ep : episodes) {
            String body = ep.path("body").asText("").trim();
            if (body.isEmpty()) {
                continue;
            }
            if (body.length() > 1200) {
                body = body.substring(0, 1200) + "…";
            }
            sb.append("第 ").append(ep.path("episodeNumber").asInt(0)).append(" 集：\n").append(body).append("\n\n");
            shown++;
            if (shown >= 3) {
                break;
            }
        }
        return sb.toString().stripTrailing();
    }

    // ==================== 私有：LLM 调用 ====================

    private String chatText(List<LlmMessage> messages, String orgId, String userId) {
        List<AgentModel> models = agentModelService.resolveChatModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId, userId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }
        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(llmRequest, model);
                llmRequest.setMessages(messages);

                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_DRAMA, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return sb.toString();
            } catch (Exception e) {
                lastError = e;
                log.warn("漫剧 LLM 调用失败，尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 调 LLM 并解析为 JSON 对象/数组，解析失败抛异常交由上层标记失败 */
    private JsonNode chatJson(List<LlmMessage> messages, String orgId, String userId) {
        String json = extractJson(chatText(messages, orgId, userId));
        if (json == null) {
            throw new IllegalStateException("AI 返回内容不是 JSON，请重试");
        }
        JsonNode node = tryParseJson(json);
        if (node == null) {
            node = tryParseJson(repairTruncatedJson(json));
        }
        if (node == null) {
            throw new IllegalStateException("AI 返回的 JSON 解析失败，请重试");
        }
        return node;
    }

    // ==================== 私有：摘要与分集内容 ====================

    private String buildSummaryUserMessage(String creative, Integer episodeCount, String imageStyleId) {
        StringBuilder sb = new StringBuilder();
        sb.append("原始创意：\n").append(creative == null ? "" : creative.trim());
        List<String> params = new ArrayList<>();
        if (episodeCount != null && episodeCount > 0) {
            params.add("- 目标集数：" + episodeCount + " 集（输出中的 episodeCount 必须与该值完全一致，不得自行修改）");
        }
        if (DramaImageStyles.isValid(imageStyleId)) {
            params.add("- 画面风格：" + DramaImageStyles.label(imageStyleId) + "（" + imageStyleId.trim() + "）");
            String stylePrompt = DramaImageStyles.prompt(imageStyleId);
            if (StringUtils.isNotBlank(stylePrompt)) {
                params.add("  风格说明：" + stylePrompt);
            }
            params.add("  人物 visualImage、故事类型标签与整体美学须符合该画面风格");
        }
        if (!params.isEmpty()) {
            sb.append("\n\n制作参数：\n").append(String.join("\n", params));
        }
        return sb.toString();
    }

    private List<ObjectNode> runEpisodeOutline(String creative, JsonNode summary, int total,
                                               String orgId, String userId) {
        String user = String.join("\n",
                "总集数：" + total + " 集（episodes 数组必须恰好 " + total + " 项）",
                "",
                "原始创意：\n" + (creative == null ? "" : creative.trim()),
                "",
                "剧本摘要：\n" + formatSummaryText(summary),
                "",
                "请输出全部分集的 episodeNumber 与 title。");
        JsonNode data = chatJson(List.of(
                new LlmMessage("system", EPISODE_OUTLINE_SYSTEM),
                new LlmMessage("user", user)), orgId, userId);
        JsonNode episodes = data.isArray() ? data : data.path("episodes");
        if (!episodes.isArray() || episodes.isEmpty()) {
            throw new IllegalStateException("分集大纲返回格式无效");
        }
        List<ObjectNode> result = new ArrayList<>();
        int i = 0;
        for (JsonNode item : episodes) {
            if (!item.isObject()) {
                i++;
                continue;
            }
            int num = item.path("episodeNumber").asInt(i + 1);
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = "第 " + num + " 集";
            }
            ObjectNode row = MAPPER.createObjectNode();
            row.put("episodeNumber", num);
            row.put("title", title);
            row.put("body", "");
            result.add(row);
            i++;
        }
        Set<Integer> have = new HashSet<>();
        for (ObjectNode row : result) {
            have.add(row.path("episodeNumber").asInt(0));
        }
        for (int n = 1; n <= total; n++) {
            if (!have.contains(n)) {
                ObjectNode row = MAPPER.createObjectNode();
                row.put("episodeNumber", n);
                row.put("title", "第 " + n + " 集");
                row.put("body", "");
                result.add(row);
            }
        }
        result.sort(Comparator.comparingInt(o -> o.path("episodeNumber").asInt(0)));
        return result.size() > total ? result.subList(0, total) : result;
    }

    private List<ObjectNode> runEpisodeScriptBatch(JsonNode summary, List<ObjectNode> existing, int total,
                                                   String creative, String orgId, String userId) {
        Map<Integer, String> titleByNum = new HashMap<>();
        for (ObjectNode item : existing) {
            titleByNum.put(item.path("episodeNumber").asInt(0), item.path("title").asText("").trim());
        }
        List<Integer> missing = autoMissingEpisodeNumbers(existing, total);
        if (missing.isEmpty()) {
            return new ArrayList<>();
        }
        int start = missing.get(0);
        String batchTitles = "第 " + start + " 集：" + titleByNum.getOrDefault(start, "第 " + start + " 集");
        String user = String.join("\n",
                "当前任务：撰写第 " + start + " 集（共 1 集）的完整剧本正文",
                "全剧共 " + total + " 集",
                "episodes 输出数组必须恰好 1 项，episodeNumber 为 " + start,
                "本集 content 约 " + TARGET_EPISODE_CONTENT_CHARS + " 汉字（不少于 " + MIN_EPISODE_CONTENT_CHARS
                        + "），含 " + EPISODE_SCENE_COUNT_HINT + "、精简 △ 与台词",
                "",
                "原始创意：\n" + (StringUtils.isBlank(creative) ? "（无额外创意，以摘要为准）" : creative.trim()),
                "",
                "剧本摘要：\n" + formatSummaryText(summary),
                "",
                "全剧分集规划：\n" + formatEpisodeTitleList(existing),
                "",
                "本批次待撰写：\n" + batchTitles,
                "",
                "已有剧集正文：\n" + formatExistingEpisodeContent(existing),
                "",
                "请输出第 " + start + " 集的 content 字段（可附带 title）。");
        JsonNode data = chatJson(List.of(
                new LlmMessage("system", EPISODE_BATCH_CONTENT_SYSTEM),
                new LlmMessage("user", user)), orgId, userId);
        JsonNode episodes = data.isArray() ? data : data.path("episodes");
        if (!episodes.isArray()) {
            throw new IllegalStateException("分集剧本返回格式无效");
        }
        List<ObjectNode> normalized = normalizeBatchEpisodes(episodes, start, start, titleByNum);
        boolean tooShort = normalized.stream()
                .anyMatch(o -> contentCharLen(o.path("body").asText("")) < MIN_EPISODE_CONTENT_CHARS);
        if (tooShort) {
            String retryUser = user + "\n\n上次输出过短。请重写本集，content 约 " + TARGET_EPISODE_CONTENT_CHARS
                    + " 汉字（不少于 " + MIN_EPISODE_CONTENT_CHARS + "），含 " + EPISODE_SCENE_COUNT_HINT
                    + "、精简 △ 与台词，不得压缩成梗概。";
            JsonNode retry = chatJson(List.of(
                    new LlmMessage("system", EPISODE_BATCH_CONTENT_SYSTEM),
                    new LlmMessage("user", retryUser)), orgId, userId);
            JsonNode retryEps = retry.isArray() ? retry : retry.path("episodes");
            if (retryEps.isArray()) {
                normalized = normalizeBatchEpisodes(retryEps, start, start, titleByNum);
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalStateException("模型未返回第 " + start + " 集正文");
        }
        return normalized;
    }

    // ==================== 私有：JSON 工具 ====================

    private ObjectNode paramsNode(String paramsJson) {
        JsonNode node = parseJsonOrNull(paramsJson);
        if (node != null && node.isObject()) {
            return (ObjectNode) node;
        }
        return MAPPER.createObjectNode();
    }

    private JsonNode parseJsonOrNull(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private List<ObjectNode> existingEpisodes(String episodeContentJson) {
        JsonNode root = parseJsonOrNull(episodeContentJson);
        List<ObjectNode> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        JsonNode episodes = root.isArray() ? root : root.path("episodes");
        if (episodes.isArray()) {
            for (JsonNode item : episodes) {
                if (item.isObject()) {
                    result.add((ObjectNode) item);
                }
            }
        }
        return result;
    }

    private String writeEpisodes(List<ObjectNode> episodes) {
        ObjectNode root = MAPPER.createObjectNode();
        root.putArray("episodes").addAll(episodes);
        return root.toString();
    }

    private List<ObjectNode> clearBodies(List<ObjectNode> episodes) {
        List<ObjectNode> result = new ArrayList<>();
        for (ObjectNode item : episodes) {
            int num = item.path("episodeNumber").asInt(0);
            if (num < 1) {
                continue;
            }
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = "第 " + num + " 集";
            }
            ObjectNode row = MAPPER.createObjectNode();
            row.put("episodeNumber", num);
            row.put("title", title);
            row.put("body", "");
            result.add(row);
        }
        return result;
    }

    private List<ObjectNode> mergeEpisodeBodies(List<ObjectNode> existing, List<ObjectNode> batch) {
        Map<Integer, ObjectNode> byNumber = new HashMap<>();
        List<ObjectNode> all = new ArrayList<>(existing);
        all.addAll(batch);
        for (ObjectNode item : all) {
            int num = item.path("episodeNumber").asInt(0);
            if (num < 1) {
                continue;
            }
            String body = item.path("body").asText("");
            String creative = item.path("creative").asText("").trim();
            String summary = item.path("summary").asText("").trim();
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = "第 " + num + " 集";
            }
            String origin = item.path("origin").asText("").trim();
            ObjectNode prev = byNumber.get(num);
            if (prev != null) {
                String prevBody = prev.path("body").asText("");
                String prevCreative = prev.path("creative").asText("").trim();
                String prevSummary = prev.path("summary").asText("").trim();
                String prevTitle = prev.path("title").asText("").trim();
                if (prevBody.trim().length() > body.trim().length()) {
                    body = prevBody;
                }
                if (prevCreative.length() > creative.length()) {
                    creative = prevCreative;
                }
                if (prevSummary.length() > summary.length()) {
                    summary = prevSummary;
                }
                if (creative.isEmpty()) {
                    creative = prevCreative;
                }
                if (summary.isEmpty()) {
                    summary = prevSummary;
                }
                if (title.startsWith("第 ") && !prevTitle.isEmpty()) {
                    title = prevTitle;
                } else if (title.equals("第 " + num + " 集") && !prevTitle.isEmpty()) {
                    title = prevTitle;
                }
                if (origin.isEmpty()) {
                    origin = prev.path("origin").asText("").trim();
                }
            }
            ObjectNode merged = MAPPER.createObjectNode();
            merged.put("episodeNumber", num);
            merged.put("title", title);
            merged.put("body", body);
            if (!creative.isEmpty()) {
                merged.put("creative", creative);
            }
            if (!summary.isEmpty()) {
                merged.put("summary", summary);
            }
            if (MANUAL_EPISODE_ORIGIN.equals(origin)) {
                merged.put("origin", origin);
            }
            byNumber.put(num, merged);
        }
        List<ObjectNode> result = new ArrayList<>(byNumber.values());
        result.sort(Comparator.comparingInt(o -> o.path("episodeNumber").asInt(0)));
        return result;
    }

    private List<Integer> autoMissingEpisodeNumbers(List<ObjectNode> existing, int total) {
        Map<Integer, ObjectNode> byNum = new HashMap<>();
        for (ObjectNode item : existing) {
            int num = item.path("episodeNumber").asInt(0);
            if (num >= 1) {
                byNum.put(num, item);
            }
        }
        List<Integer> missing = new ArrayList<>();
        for (int n = 1; n <= total; n++) {
            ObjectNode item = byNum.get(n);
            if (item == null) {
                missing.add(n);
                continue;
            }
            if (contentCharLen(item.path("body").asText("")) >= MIN_EPISODE_CONTENT_CHARS) {
                continue;
            }
            if (MANUAL_EPISODE_ORIGIN.equals(item.path("origin").asText(""))) {
                continue;
            }
            missing.add(n);
        }
        return missing;
    }

    private int countCompletedEpisodes(List<ObjectNode> episodes, int total) {
        int done = 0;
        for (ObjectNode item : episodes) {
            int num = item.path("episodeNumber").asInt(0);
            if (num >= 1 && num <= total && contentCharLen(item.path("body").asText("")) >= MIN_EPISODE_CONTENT_CHARS) {
                done++;
            }
        }
        return done;
    }

    private int contentCharLen(String text) {
        return (text == null ? "" : text).replaceAll("\\s", "").length();
    }

    private boolean titlesReady(List<ObjectNode> existing, int total) {
        int titled = 0;
        for (ObjectNode item : existing) {
            int num = item.path("episodeNumber").asInt(0);
            if (num < 1 || num > total) {
                continue;
            }
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            if (title.equals("第 " + num + " 集") || title.equals("第" + num + "集")) {
                continue;
            }
            titled++;
        }
        return titled >= total;
    }

    private List<ObjectNode> normalizeBatchEpisodes(JsonNode episodes, int start, int end,
                                                    Map<Integer, String> titleByNum) {
        List<ObjectNode> result = new ArrayList<>();
        int offset = 0;
        for (JsonNode item : episodes) {
            if (!item.isObject()) {
                offset++;
                continue;
            }
            int num = item.path("episodeNumber").asInt(start + offset);
            if (num < start || num > end) {
                offset++;
                continue;
            }
            String body = item.path("content").asText("").trim();
            if (body.isEmpty()) {
                body = item.path("body").asText("").trim();
            }
            if (body.isEmpty()) {
                offset++;
                continue;
            }
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = titleByNum.getOrDefault(num, "第 " + num + " 集");
            }
            ObjectNode row = MAPPER.createObjectNode();
            row.put("episodeNumber", num);
            row.put("title", title);
            row.put("body", body);
            String creative = item.path("creative").asText("").trim();
            String summary = item.path("summary").asText("").trim();
            if (summary.isEmpty()) {
                summary = item.path("synopsis").asText("").trim();
            }
            if (!creative.isEmpty()) {
                row.put("creative", creative);
            }
            if (!summary.isEmpty()) {
                row.put("summary", summary);
            }
            result.add(row);
            offset++;
        }
        return result;
    }

    private int resolveEpisodeTarget(JsonNode summary, String projectParamsJson, String scriptParamsJson) {
        int[] candidates = new int[]{
                paramsNode(projectParamsJson).path("episode_count").asInt(0),
                summary == null ? 0 : summary.path("episodeCount").asInt(0),
                paramsNode(scriptParamsJson).path("episode_count").asInt(0)};
        for (int value : candidates) {
            if (value >= 1) {
                return value;
            }
        }
        return 12;
    }

    // ==================== 私有：持久化 ====================

    private void persistEpisodeContent(String orgId, String projectId, List<ObjectNode> existing,
                                       String paramsJson) {
        AiDramaScript script = requireScript(orgId, projectId);
        script.setEpisodeContentJson(writeEpisodes(existing));
        script.setParamsJson(paramsJson);
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
    }

    private String paramsWithStatus(int total, int done, String status) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("episode_content_status", status);
        params.put("episode_count", total);
        ObjectNode progress = params.putObject("episode_content_progress");
        progress.put("done", done);
        progress.put("total", total);
        return params.toString();
    }

    private void markSummaryFailed(String orgId, String projectId, Exception e) {
        AiDramaScript script = findScript(orgId, projectId);
        if (script == null) {
            return;
        }
        script.setSummaryStatus(STATUS_FAILED);
        ObjectNode params = paramsNode(script.getParamsJson());
        params.put("summary_error", errorText(e));
        script.setParamsJson(params.isEmpty() ? null : params.toString());
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
    }

    private void markEpisodeFailed(String orgId, String projectId, Exception e) {
        AiDramaScript script = findScript(orgId, projectId);
        if (script == null) {
            return;
        }
        ObjectNode params = paramsNode(script.getParamsJson());
        params.put("episode_content_status", STATUS_FAILED);
        params.put("episode_content_error", errorText(e));
        script.setParamsJson(params.isEmpty() ? null : params.toString());
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
    }

    private String errorText(Exception e) {
        return e == null || e.getMessage() == null ? "生成失败，请重试" : StringUtils.left(e.getMessage(), 500);
    }

    // ==================== 私有：格式化与标题 ====================

    private String formatSummaryText(JsonNode summary) {
        if (summary == null) {
            return "（无摘要）";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("剧名：").append(summary.path("seriesTitle").asText("")).append('\n');
        sb.append("集数：").append(summary.path("episodeCount").asText("")).append('\n');
        sb.append("类型：").append(summary.path("storyType").asText("")).append('\n');
        sb.append("受众：").append(summary.path("targetAudience").asText("")).append('\n');
        sb.append("钩子：").append(summary.path("coreHook").asText("")).append('\n');
        sb.append("一句话：").append(summary.path("oneLineStory").asText("")).append('\n');
        sb.append('\n').append("人物：").append('\n');
        JsonNode characters = summary.path("characters");
        if (characters.isArray()) {
            for (JsonNode c : characters) {
                if (!c.isObject()) {
                    continue;
                }
                sb.append("- ").append(c.path("name").asText(""))
                        .append("（").append(c.path("roleType").asText("")).append("/")
                        .append(c.path("title").asText("")).append("）：")
                        .append(c.path("visualImage").asText("")).append("；标签：")
                        .append(c.path("coreTags").asText("")).append("；弧光：")
                        .append(c.path("growthArc").asText("")).append('\n');
            }
        }
        sb.append('\n').append("梗概：").append('\n').append(summary.path("synopsis").asText(""));
        return sb.toString();
    }

    private String formatEpisodeTitleList(List<ObjectNode> episodes) {
        List<ObjectNode> sorted = new ArrayList<>(episodes);
        sorted.sort(Comparator.comparingInt(o -> o.path("episodeNumber").asInt(0)));
        if (sorted.isEmpty()) {
            return "（暂无分集规划）";
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectNode item : sorted) {
            int num = item.path("episodeNumber").asInt(0);
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = "第 " + num + " 集";
            }
            sb.append("第 ").append(num).append(" 集：").append(title).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private String formatExistingEpisodeContent(List<ObjectNode> episodes) {
        List<ObjectNode> completed = new ArrayList<>();
        for (ObjectNode item : episodes) {
            if (StringUtils.isNotBlank(item.path("body").asText("").trim())) {
                completed.add(item);
            }
        }
        completed.sort(Comparator.comparingInt(o -> o.path("episodeNumber").asInt(0)));
        if (completed.isEmpty()) {
            return "（暂无，本批次从开篇写起）";
        }
        List<ObjectNode> tail = completed.size() > 3
                ? completed.subList(completed.size() - 3, completed.size()) : completed;
        StringBuilder sb = new StringBuilder();
        for (ObjectNode item : tail) {
            int num = item.path("episodeNumber").asInt(0);
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = "第 " + num + " 集";
            }
            String body = item.path("body").asText("").trim();
            if (body.length() > 1800) {
                body = body.substring(0, 1800) + "\n…（上文已截断）";
            }
            sb.append(num).append('.').append(title).append("：\n").append(body).append("\n\n");
        }
        return sb.toString().stripTrailing();
    }

    private String pickAutoProjectTitle(JsonNode summary, String creative, String currentTitle) {
        String series = normalizeSeriesTitle(summary.path("seriesTitle").asText(""));
        if (series.isEmpty()) {
            series = normalizeSeriesTitle(summary.path("title").asText(""));
        }
        if (series.isEmpty()) {
            series = normalizeSeriesTitle(summary.path("projectTitle").asText(""));
        }
        if (series.isEmpty()) {
            return null;
        }
        String current = currentTitle == null ? "" : currentTitle.trim();
        String creativePrefix = (creative == null ? "" : creative.trim());
        if (creativePrefix.length() > 20) {
            creativePrefix = creativePrefix.substring(0, 20);
        }
        boolean looksDefault = current.isEmpty()
                || "未命名漫剧".equals(current)
                || current.length() > 36
                || (!creativePrefix.isEmpty() && current.startsWith(creativePrefix))
                || current.endsWith("…");
        return looksDefault ? series : null;
    }

    private String normalizeSeriesTitle(String raw) {
        String title = raw == null ? "" : raw.trim();
        if (title.isEmpty()) {
            return "";
        }
        title = title.replaceAll("^[「『《》“\"'‘’]+|[」』》”\"'‘’]+$", "");
        int newline = title.indexOf('\n');
        if (newline >= 0) {
            title = title.substring(0, newline).trim();
        }
        if (title.length() > 24) {
            title = title.substring(0, 24).replaceAll("[，。；、…·\\-— ]+$", "");
        }
        return title;
    }

    // ==================== 私有：查询 ====================

    private AiDramaProject findProject(String orgId, String projectId) {
        AiDramaProject project = projectMapper.selectByPrimaryKey(projectId);
        return project == null || !orgId.equals(project.getOrganizationId()) ? null : project;
    }

    private AiDramaProject requireProject(String orgId, String projectId) {
        AiDramaProject project = findProject(orgId, projectId);
        if (project == null) {
            throw new GenericException("漫剧项目不存在");
        }
        return project;
    }

    private AiDramaScript findScript(String orgId, String projectId) {
        AiDramaScript criteria = new AiDramaScript();
        criteria.setProjectId(projectId);
        criteria.setOrganizationId(orgId);
        return scriptMapper.selectOne(criteria);
    }

    private AiDramaScript requireScript(String orgId, String projectId) {
        AiDramaScript script = findScript(orgId, projectId);
        if (script == null) {
            throw new GenericException("漫剧剧本不存在");
        }
        return script;
    }

    private boolean isStale(long startedAt) {
        return startedAt <= 0 || System.currentTimeMillis() - startedAt > SUMMARY_STALE_MILLIS;
    }

    private boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }

    private String blockedMessage(String status) {
        if (AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)) {
            return "调用过于频繁，请稍后再试";
        }
        if (AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status)) {
            return "AI 服务暂不可用，请联系管理员";
        }
        return "AI 额度已用完，请加购或升级";
    }

    private JsonNode tryParseJson(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String repairTruncatedJson(String json) {
        int brace = 0;
        int bracket = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                brace++;
            } else if (c == '}') {
                brace--;
            } else if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket--;
            }
        }
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < bracket; i++) {
            suffix.append(']');
        }
        for (int i = 0; i < brace; i++) {
            suffix.append('}');
        }
        return suffix.isEmpty() ? json : json + suffix;
    }

    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('\n');
            if (start < 0) {
                return null;
            }
            t = t.substring(start + 1);
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
            t = t.trim();
        }
        int begin = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (begin < 0 || end < 0 || end <= begin) {
            return null;
        }
        return t.substring(begin, end + 1);
    }

    // ==================== Prompt 常量 ====================

    private static final String SCRIPT_SUMMARY_SYSTEM_PROMPT = """
            你是专业的短剧/网剧剧本策划，负责把用户提供的原始创意、故事大纲或灵感，整理成可直接用于立项与编剧开工的结构化「剧本摘要」。

            输出要求：
            1. 忠实于用户创意，可合理补全细节，但不要擅自改掉核心设定、主线与结局
            2. 若用户提供了目标集数，episodeCount 必须与该值完全一致；未提供时根据故事体量合理估算（短篇 12–24 集，中篇 30–60 集，长篇可更高）
            3. 若用户提供了画面风格，人物 visualImage 须体现该风格的视觉美学，storyType 可融合风格相关标签
            4. storyType、coreHook 用「+」连接多个标签，风格参考：古风奇幻+神话后传+反乌托邦
            5. targetAudience 简洁，如：男频 / 大众、女频 / 青年 等
            6. seriesTitle 必须是可作项目名的短剧名：4–16 个汉字（可含少量数字/标点），有记忆点、可上架；禁止复述整句梗概，禁止用「一句话故事」原文当剧名，禁止「未命名」「短剧」等占位
            7. oneLineStory 一句话说清主线 + 最大反转或钩子（与 seriesTitle 不同：前者是剧名，后者是卖点句）
            8. characters 须覆盖故事中全部具名出场角色（主角、重要配角、反派）；群演/路人可合并为 1 个群体角色；每人字段须饱满、可拍摄、有戏剧张力；不要只写 2–3 个主角而漏掉其余具名人物。禁止把「音色 / 声音 / 旁白音色」或带（声音）（音色）后缀的名字写成角色；旁白若需出场可写「某某旁白」本体，不要单独建「某某（声音）」
            9. 人物小传中 growthArc 必须用「阶段A -> 阶段B -> 阶段C」格式
            10. synopsis 用一段完整中文叙述故事，从世界观、矛盾、结盟、高潮、结局到余韵，长度 200–400 字
            11. 语言统一使用简体中文，偏影视策划文档风格，避免空泛形容词堆砌
            12. 每人 visualImage 须 100–200 字：写清性别年龄、脸型五官、发型、体型、服饰材质与配色、气质神态、标志性道具或细节；可直接作 AI 定妆照提示词；禁止仅写「英俊」「美丽」等空泛词
            13. characters 建议 5–12 人；确有大量具名配角时宁可多列，也不要省略会反复出场的名字

            必须输出严格 JSON 对象（不要 markdown、不要代码围栏），字段：
            {
              "episodeCount": number,
              "seriesTitle": string,
              "storyType": string,
              "targetAudience": string,
              "coreHook": string,
              "oneLineStory": string,
              "characters": [
                {
                  "name": string,
                  "title": string,
                  "roleType": string,
                  "visualImage": string,
                  "coreTags": string,
                  "identityBackground": string,
                  "growthExperience": string,
                  "personality": string,
                  "relationships": string,
                  "growthArc": string
                }
              ],
              "synopsis": string
            }""";

    private static final String EPISODE_OUTLINE_SYSTEM = """
            你是专业的短剧/网剧编剧策划，负责根据原始创意与剧本摘要，规划全部分集的「集数 + 集名」大纲。

            输出要求：
            1. 必须严格按照用户给定的总集数生成，episodes 数组长度必须与总集数完全一致
            2. episodeNumber 从 1 开始连续递增，不得跳号、不得重复
            3. 每集 title 为 4-12 个汉字的集名，概括本集核心事件或冲突钩子，风格参考：金箍碎佛规、罪臣之子承玄圭
            4. 全剧分集须覆盖剧本摘要中的起承转合：前期立人设与世界观、中期升级矛盾与反转、后期高潮与结局，节奏适合短剧连载
            5. 相邻集名之间要有因果衔接与追剧钩子，避免重复套路
            6. 语言使用简体中文

            必须输出严格 JSON：
            {"episodes":[{"episodeNumber":1,"title":"集名"}, ...]}""";

    private static final String EPISODE_BATCH_CONTENT_SYSTEM = """
            你是专业的短剧/网剧编剧，负责根据原始创意、剧本摘要、分集规划与已有剧集正文，撰写指定集数的拍摄剧本正文。

            输出要求：
            1. 每次任务只输出用户指定批次范围内的集数，episodes 数组长度必须与批次集数完全一致
            2. episodeNumber 须与用户指定的集数一一对应，不得遗漏、不得额外生成
            3. 须携带并参考「已有剧集正文」保持剧情、人设与世界观连贯；首批次无已有正文时从第 1 集开篇写起
            4. 批次内各集之间须有因果衔接，末集结尾留追剧钩子
            5. 必须输出对象格式：{"episodes":[{"episodeNumber":数字,"title":"集名","creative":"本集创意","summary":"本集摘要","content":"..."}]}，不要直接输出数组
            6. creative 为本集 80-200 字创意梗概；summary 为本集 120-300 字剧情摘要；content 为正文主字段；不要把正文写得过短

            格式要求（每集 content 须严格遵守）：
            1. 按场次组织，场号格式为 ### 场{集数}-{场次}，如第 1 集第 2 场：### 场1-2
            2. 场头下一行写时间内外景，如：日 内 灵山大雄宝殿 / 夜 外 妖寨大门外 / 晨外 羽山刑场
            3. 下一行写：出场人物：角色A、角色B（只写可出镜人物名；不要写「某某（声音）」「某某音色」等音色标注）
            4. 动作用 △ 开头，独占一行；动作须具体可拍（景别、调度、道具、表情），禁止一句带过
            5. 台词格式：角色名（情绪/vo/os/动作）：台词内容；旁白用 vo，内心独白用 os；台词要有潜台词与冲突；括号内写情绪/vo/os，不要写「声音」「音色」
            6. 关键镜头可用【空镜：描述】收尾一场或一段
            7. content 内不要输出「第X集」或「X.集名：」标题行，只输出场戏正文
            8. 每集 2-3 场；每场 2-3 段 △ 动作与 2-3 句台词（或对白+vo）；整集 content 约 450-600 汉字，节奏紧凑、不注水
            9. 语言使用简体中文，偏影视剧本风格，动作与台词可拍摄、有张力""";

    private static final String PROP_EXTRACT_SYSTEM = """
            你是短剧/网剧的道具策划，负责从剧本摘要与分集正文中抽取可视觉化、可出图的关键道具。

            输出要求：
            1. 只抽取对剧情有推动、会反复出现或具有象征意义的道具（法器、信物、服饰配饰、标志性物件等），3-8 个
            2. 不要抽取普通布景（桌椅、天空、街道），不要抽取人物本身
            3. 每个道具的 name 用 2-6 字中文短语；visualPrompt 用 50-120 字描述外形、材质、配色与构图，可直接作生图提示词
            4. 语言使用简体中文

            必须输出严格 JSON：
            {"props":[{"name":"道具名","visualPrompt":"生图提示词"}]}""";
}
