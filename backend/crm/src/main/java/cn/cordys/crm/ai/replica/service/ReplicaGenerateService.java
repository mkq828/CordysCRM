package cn.cordys.crm.ai.replica.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.callreview.asr.AsrProvider;
import cn.cordys.crm.ai.callreview.asr.AsrProviderFactory;
import cn.cordys.crm.ai.callreview.asr.AsrSentence;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.replica.domain.AiReplicaTask;
import cn.cordys.crm.ai.replica.domain.AiReplicaVariant;
import cn.cordys.crm.ai.replica.dto.ReplicaVariantPlan;
import cn.cordys.crm.ai.replica.dto.request.ReplicaSubmitRequest;
import cn.cordys.crm.ai.replica.dto.response.ReplicaTaskResponse;
import cn.cordys.crm.ai.replica.dto.response.ReplicaVariantResponse;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 爆款复刻（功能 7 扩展 · 换皮变体）服务。
 * <p>
 * 链路：提交参考视频 → 解析（抽帧 + 抽 16kHz WAV + paraformer 句级时间戳转写）→ LLM 生成 N 个换皮变体方案
 * → 逐个合成（原视频画面 scale/crop + 原音轨 + ASS 卡拉OK字幕 + 钩子/标题）。
 * 画面声音 100% 复用原片，AI 只动文字包装层，从根上规避漫剧的 AI 生图崩脸/风控问题。
 * </p>
 */
@Slf4j
@Service
public class ReplicaGenerateService {

    private static final String ASR_PROVIDER = "阿里云";
    private static final int ASR_MAX_POLL = 120;
    private static final int ASR_POLL_INTERVAL_MS = 3000;
    private static final int DEFAULT_VARIANT_COUNT = 3;
    private static final int MAX_VARIANT_COUNT = 5;
    private static final String WORK_DIR_PREFIX = "cordys-replica-";

    private static final String PARSE_PENDING = "PENDING";
    private static final String PARSE_PARSING = "PARSING";
    private static final String PARSE_READY = "READY";
    private static final String PARSE_FAILED = "FAILED";
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_PARSING = "PARSING";
    private static final String TASK_GENERATING = "GENERATING";
    private static final String TASK_READY = "READY";
    private static final String TASK_FAILED = "FAILED";
    private static final String VARIANT_PENDING = "PENDING";
    private static final String VARIANT_COMPOSING = "COMPOSING";
    private static final String VARIANT_DONE = "DONE";
    private static final String VARIANT_FAILED = "FAILED";

    private static final List<String> SUBTITLE_STYLES = List.of("classic", "karaoke_yellow", "karaoke_green", "karaoke_pink");
    private static final List<String> RATIOS = List.of("9:16", "16:9", "1:1");

    @Resource
    private BaseMapper<AiReplicaTask> replicaTaskMapper;
    @Resource
    private BaseMapper<AiReplicaVariant> replicaVariantMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private FileCommonService fileCommonService;
    @Resource
    private AttachmentService attachmentService;
    @Resource
    private ReplicaVideoParser replicaVideoParser;
    @Resource
    private ReplicaVideoComposer replicaVideoComposer;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private AsrProviderFactory asrProviderFactory;

    // ==================== 提交 ====================

    /**
     * 提交任务：读参考视频临时附件 → 写 workDir + 转正为正式附件 → 落库 PENDING。
     * 真正的解析/生成/合成由 Controller 跨 Bean 调用 {@link #processAsync}（@Async）触发。
     */
    public ReplicaTaskResponse submit(ReplicaSubmitRequest request, String orgId, String userId) {
        ResponseEntity<org.springframework.core.io.Resource> res = attachmentService.getResource(request.getReferenceAttachmentId());
        if (res == null || res.getBody() == null) {
            throw new GenericException("参考视频不存在或已删除，请重新上传");
        }
        String sourceName = resolveFileName(res);
        String ext = extOf(sourceName);

        AiReplicaTask task = new AiReplicaTask();
        task.setId(IDGenerator.nextStr());
        task.setOrganizationId(orgId);
        task.setTitle(StringUtils.defaultIfBlank(request.getTitle(), baseName(sourceName)));
        task.setVariantCount(clampCount(request.getVariantCount()));
        task.setParseStatus(PARSE_PENDING);
        task.setStatus(TASK_PENDING);
        task.setCreateTime(System.currentTimeMillis());
        task.setCreateUser(userId);
        task.setUpdateTime(System.currentTimeMillis());
        task.setUpdateUser(userId);
        replicaTaskMapper.insert(task);

        File workDir = workDir(task.getId());
        workDir.mkdirs();
        File reference = new File(workDir, "reference");
        try (InputStream in = res.getBody().getInputStream()) {
            Files.copy(in, reference.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new GenericException("读取参考视频失败，请重新上传");
        }
        task.setReferenceAttachmentId(saveAttachment(reference, "reference." + ext, ext, task.getId(), orgId, userId));
        task.setUpdateTime(System.currentTimeMillis());
        replicaTaskMapper.updateById(task);
        return toResponse(task, orgId);
    }

    // ==================== 异步流水线 ====================

    /**
     * 完整流水线：解析 → 生成变体方案 → 逐个合成。串行编排，每个变体状态独立更新，前端轮询感知进度。
     */
    @Async
    public void processAsync(String taskId, String orgId, String userId) {
        AiReplicaTask task = replicaTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        try {
            parse(task, orgId, userId);
            generateVariants(task, orgId, userId);
            composeVariants(task, orgId, userId);
        } catch (Exception e) {
            log.error("爆款复刻任务处理失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        }
    }

    /** 重新生成变体：删除旧变体 → 重新生成方案 → 重新合成（由 Controller 跨 Bean 触发）。 */
    @Async
    public void regenerateAsync(String taskId, String orgId, String userId) {
        AiReplicaTask task = replicaTaskMapper.selectByPrimaryKey(taskId);
        if (task == null || !PARSE_READY.equals(task.getParseStatus())) {
            return;
        }
        try {
            List<AiReplicaVariant> old = variantsOf(task.getId());
            for (AiReplicaVariant v : old) {
                deleteAttachmentQuietly(v.getAttachmentId());
                replicaVariantMapper.deleteByPrimaryKey(v.getId());
            }
            if (task.getVariantCount() == null || task.getVariantCount() <= 0) {
                task.setVariantCount(old.isEmpty() ? DEFAULT_VARIANT_COUNT : Math.min(old.size(), MAX_VARIANT_COUNT));
            }
            generateVariants(task, orgId, userId);
            composeVariants(task, orgId, userId);
        } catch (Exception e) {
            log.error("爆款复刻重新生成变体失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        }
    }

    /** 解析：ffprobe 取时长 + ffmpeg 抽封面帧、抽 16kHz WAV → paraformer 句级时间戳转写 → parse_json 落库。 */
    private void parse(AiReplicaTask task, String orgId, String userId) throws Exception {
        task.setParseStatus(PARSE_PARSING);
        task.setStatus(TASK_PARSING);
        task.setUpdateTime(System.currentTimeMillis());
        replicaTaskMapper.updateById(task);

        File workDir = workDir(task.getId());
        File reference = new File(workDir, "reference");
        ReplicaVideoParser.ParseResult parsed = replicaVideoParser.parse(reference, workDir);
        String coverId = saveAttachment(parsed.coverImage(), "cover.jpg", "jpg", task.getId(), orgId, userId);
        List<AsrSentence> sentences = transcribe(parsed.audioWav(), orgId);
        if (sentences.isEmpty()) {
            throw new GenericException("未识别到有效语音内容，请上传带口播的参考视频");
        }

        Map<String, Object> parseJson = new LinkedHashMap<>();
        parseJson.put("coverAttachmentId", coverId);
        parseJson.put("durationMs", (long) (parsed.durationSeconds() * 1000));
        parseJson.put("sentences", sentences.stream()
                .map(s -> Map.of("text", s.text(), "beginMs", s.beginMs(), "endMs", s.endMs()))
                .toList());
        task.setParseJson(JSON.toJSONString(parseJson));
        task.setParseStatus(PARSE_READY);
        task.setUpdateTime(System.currentTimeMillis());
        replicaTaskMapper.updateById(task);
    }

    /** 生成变体方案：LLM 基于转写文本产出 N 个换皮方案，落 ai_replica_variant（PENDING）。 */
    private void generateVariants(AiReplicaTask task, String orgId, String userId) {
        task.setStatus(TASK_GENERATING);
        task.setUpdateTime(System.currentTimeMillis());
        replicaTaskMapper.updateById(task);

        int count = task.getVariantCount() == null ? DEFAULT_VARIANT_COUNT : Math.min(Math.max(task.getVariantCount(), 1), MAX_VARIANT_COUNT);
        String transcript = joinSentences(task);
        List<LlmMessage> messages = buildVariantMessages(transcript, count);
        String text = chatText(messages, orgId, userId, AiQuotaConstant.AI_REPLICA);
        List<ReplicaVariantPlan> plans = parseVariantPlans(text, count);
        if (plans.isEmpty()) {
            throw new GenericException("变体方案生成失败，请重试");
        }

        long now = System.currentTimeMillis();
        for (int i = 0; i < plans.size(); i++) {
            ReplicaVariantPlan p = plans.get(i);
            AiReplicaVariant v = new AiReplicaVariant();
            v.setId(IDGenerator.nextStr());
            v.setTaskId(task.getId());
            v.setOrganizationId(orgId);
            v.setVariantNo(i + 1);
            v.setHookText(p.hookText());
            v.setTitleText(p.titleText());
            v.setSubtitleStyle(p.subtitleStyle());
            v.setRatio(p.ratio());
            v.setStatus(VARIANT_PENDING);
            v.setCreateTime(now);
            v.setCreateUser(userId);
            v.setUpdateTime(now);
            v.setUpdateUser(userId);
            replicaVariantMapper.insert(v);
        }
        // 注意：此处不再把任务标为 READY。变体只是「方案已生成」，视频尚未合成；
        // 若此刻标 READY，前端会误判为完成并停止轮询，变体卡片会永远停在「合成中」。READY 推迟到 composeVariants 全部合成完。
    }

    /** 逐个合成变体：单个失败不影响其他变体，标记为 FAILED，任务保持 READY。 */
    private void composeVariants(AiReplicaTask task, String orgId, String userId) {
        List<AiReplicaVariant> variants = variantsOf(task.getId());
        List<AsrSentence> sentences = parseSentences(task);
        File source = new File(workDir(task.getId()), "reference");
        for (AiReplicaVariant v : variants) {
            if (!VARIANT_PENDING.equals(v.getStatus())) {
                continue;
            }
            composeOne(task, v, sentences, source, orgId, userId);
        }
        // 全部变体合成完毕（单个失败只标记该变体 FAILED，不阻断整体），此时任务才算真正完成。
        task.setStatus(TASK_READY);
        task.setUpdateTime(System.currentTimeMillis());
        replicaTaskMapper.updateById(task);
    }

    private void composeOne(AiReplicaTask task, AiReplicaVariant v, List<AsrSentence> sentences, File source,
                            String orgId, String userId) {
        v.setStatus(VARIANT_COMPOSING);
        v.setUpdateTime(System.currentTimeMillis());
        replicaVariantMapper.updateById(v);
        File output = new File(workDir(task.getId()), "variant-" + v.getVariantNo() + ".mp4");
        try {
            ReplicaVariantPlan plan = new ReplicaVariantPlan(v.getHookText(), v.getTitleText(), v.getSubtitleStyle(), v.getRatio());
            replicaVideoComposer.compose(source, sentences, plan, output);
            String attachmentId = saveAttachment(output, "variant-" + v.getVariantNo() + ".mp4", "mp4", task.getId(), orgId, userId);
            v.setAttachmentId(attachmentId);
            v.setStatus(VARIANT_DONE);
            v.setErrorMsg(null);
            v.setUpdateTime(System.currentTimeMillis());
            replicaVariantMapper.updateById(v);
        } catch (Exception e) {
            log.error("爆款复刻变体合成失败 variantId={}, taskId={}", v.getId(), task.getId(), e);
            v.setStatus(VARIANT_FAILED);
            v.setErrorMsg(truncate(e.getMessage()));
            v.setUpdateTime(System.currentTimeMillis());
            replicaVariantMapper.updateById(v);
        } finally {
            deleteQuietly(output);
        }
    }

    // ==================== 查询 ====================

    public List<ReplicaTaskResponse> list(String orgId) {
        return replicaTaskMapper.selectListByLambda(new LambdaQueryWrapper<AiReplicaTask>()
                        .eq(AiReplicaTask::getOrganizationId, orgId)
                        .orderByDesc(AiReplicaTask::getCreateTime)).stream()
                .map(t -> toResponse(t, orgId))
                .toList();
    }

    public ReplicaTaskResponse get(String orgId, String id) {
        return toResponse(requireTask(orgId, id), orgId);
    }

    // ==================== 删除 ====================

    public void delete(String orgId, String taskId) {
        AiReplicaTask task = requireTask(orgId, taskId);
        deleteAttachmentQuietly(task.getReferenceAttachmentId());
        deleteAttachmentQuietly(coverAttachmentId(task));
        for (AiReplicaVariant v : variantsOf(taskId)) {
            deleteAttachmentQuietly(v.getAttachmentId());
            replicaVariantMapper.deleteByPrimaryKey(v.getId());
        }
        replicaTaskMapper.deleteByPrimaryKey(taskId);
        deleteDirQuietly(workDir(taskId));
    }

    public void deleteVariant(String orgId, String variantId) {
        AiReplicaVariant v = replicaVariantMapper.selectByPrimaryKey(variantId);
        if (v == null || !orgId.equals(v.getOrganizationId())) {
            throw new GenericException("变体不存在");
        }
        deleteAttachmentQuietly(v.getAttachmentId());
        replicaVariantMapper.deleteByPrimaryKey(variantId);
    }

    // ==================== 私有：转写 ====================

    /** 阿里云 paraformer 句级时间戳转写：提交 + 轮询，返回逐句时间戳。 */
    private List<AsrSentence> transcribe(File wav, String orgId) throws Exception {
        String apiKey = agentModelService.resolveProviderApiKey(orgId, ASR_PROVIDER);
        if (StringUtils.isBlank(apiKey)) {
            throw new GenericException("未配置阿里云语音转写密钥，请先在「模型设置」中配置阿里云模型");
        }
        AsrProvider provider = asrProviderFactory.get(ASR_PROVIDER);
        byte[] bytes = Files.readAllBytes(wav.toPath());
        String fileUrl = provider.resolveLocalFileUrl(bytes, "audio.wav", "audio/wav", apiKey);
        String asrTaskId = provider.submit(fileUrl, apiKey);
        for (int i = 0; i < ASR_MAX_POLL; i++) {
            List<AsrSentence> sentences = provider.pollSentences(asrTaskId, apiKey);
            if (sentences != null) {
                return sentences;
            }
            Thread.sleep(ASR_POLL_INTERVAL_MS);
        }
        throw new GenericException("语音转写超时，请稍后重试");
    }

    // ==================== 私有：LLM ====================

    /** 生成型 LLM 调用：额度四步（checkQuota → checkModelDailyLimit → chatStream → record），候选模型逐个降级。 */
    private String chatText(List<LlmMessage> messages, String orgId, String userId, String featureCode) {
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
                LlmChatRequest req = new LlmChatRequest();
                req.setModel(model.getModelName());
                req.setBaseUrl(model.getApiUrl());
                req.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(req, model);
                req.setMessages(messages);
                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(req, sb::append);
                aiQuotaService.record(orgId, featureCode, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return sb.toString();
            } catch (Exception e) {
                lastError = e;
                log.warn("爆款复刻 LLM 调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildVariantMessages(String transcript, int count) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system",
                "你是短视频爆款复刻的文案包装专家。给定一段真实视频的口播转写，为同一段视频设计 " + count + " 个「换皮」包装变体。"
                        + "画面和声音完全不变，你只设计文字包装层：\n"
                        + "1. hookText：开头 0~3 秒的大字钩子，抓眼球，≤12 字。\n"
                        + "2. titleText：全片顶部常驻的主题标签，≤16 字。\n"
                        + "3. subtitleStyle：字幕样式，从 classic / karaoke_yellow / karaoke_green / karaoke_pink 中选，各变体尽量错开。\n"
                        + "4. ratio：输出比例，从 9:16 / 16:9 / 1:1 中选，各变体尽量错开。\n"
                        + "只输出 JSON，格式：{\"variants\":[{\"hookText\":\"...\",\"titleText\":\"...\",\"subtitleStyle\":\"...\",\"ratio\":\"...\"}]}"));
        messages.add(new LlmMessage("user", "口播转写：\n" + transcript + "\n\n请生成 " + count + " 个换皮变体。"));
        return messages;
    }

    private List<ReplicaVariantPlan> parseVariantPlans(String text, int count) {
        String json = extractJson(text);
        if (json == null) {
            return List.of();
        }
        try {
            JsonNode arr = JSON.MAPPER.readTree(json).path("variants");
            if (!arr.isArray()) {
                return List.of();
            }
            List<ReplicaVariantPlan> plans = new ArrayList<>();
            for (JsonNode n : arr) {
                if (plans.size() >= count) {
                    break;
                }
                String hook = StringUtils.defaultString(n.path("hookText").asText("")).trim();
                String title = StringUtils.defaultString(n.path("titleText").asText("")).trim();
                if (hook.isEmpty() && title.isEmpty()) {
                    continue;
                }
                plans.add(new ReplicaVariantPlan(hook, title,
                        normalizeStyle(n.path("subtitleStyle").asText("")),
                        normalizeRatio(n.path("ratio").asText(""))));
            }
            return plans;
        } catch (Exception e) {
            log.warn("爆款复刻变体方案解析失败", e);
            return List.of();
        }
    }

    private String normalizeStyle(String s) {
        return SUBTITLE_STYLES.contains(s) ? s : "classic";
    }

    private String normalizeRatio(String s) {
        return RATIOS.contains(s) ? s : "9:16";
    }

    // ==================== 私有：解析 JSON ====================

    private List<AsrSentence> parseSentences(AiReplicaTask task) {
        try {
            JsonNode arr = JSON.MAPPER.readTree(StringUtils.defaultIfBlank(task.getParseJson(), "{}")).path("sentences");
            List<AsrSentence> result = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String text = n.path("text").asText("");
                    if (StringUtils.isNotBlank(text)) {
                        result.add(new AsrSentence(text, n.path("beginMs").asLong(0), n.path("endMs").asLong(0)));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String joinSentences(AiReplicaTask task) {
        StringBuilder sb = new StringBuilder();
        for (AsrSentence s : parseSentences(task)) {
            sb.append(s.text());
        }
        return sb.toString();
    }

    private String coverAttachmentId(AiReplicaTask task) {
        try {
            return JSON.MAPPER.readTree(StringUtils.defaultIfBlank(task.getParseJson(), "{}"))
                    .path("coverAttachmentId").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 私有：附件 ====================

    /** 上传文件并落附件系统，返回附件ID。 */
    private String saveAttachment(File file, String fileName, String ext, String resourceId, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, resourceId, attachmentId),
                StorageType.LOCAL.name(),
                fileName);
        fileCommonService.upload(file, request);

        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setName(fileName);
        attachment.setType(ext);
        attachment.setSize(file.length());
        attachment.setStorage(StorageType.LOCAL.name());
        attachment.setOrganizationId(orgId);
        attachment.setResourceId(resourceId);
        attachment.setCreateTime(System.currentTimeMillis());
        attachment.setCreateUser(userId);
        attachment.setUpdateTime(System.currentTimeMillis());
        attachment.setUpdateUser(userId);
        attachmentMapper.insert(attachment);
        return attachmentId;
    }

    private void deleteAttachmentQuietly(String attachmentId) {
        if (StringUtils.isBlank(attachmentId)) {
            return;
        }
        try {
            attachmentService.delete(attachmentId);
        } catch (Exception ignored) {
            // 附件可能已被清理，忽略
        }
    }

    // ==================== 私有：响应映射 ====================

    private ReplicaTaskResponse toResponse(AiReplicaTask task, String orgId) {
        ReplicaTaskResponse r = new ReplicaTaskResponse();
        r.setId(task.getId());
        r.setTitle(task.getTitle());
        r.setReferenceAttachmentId(task.getReferenceAttachmentId());
        r.setParseStatus(task.getParseStatus());
        r.setStatus(task.getStatus());
        r.setErrorMsg(task.getErrorMsg());
        r.setCreateTime(task.getCreateTime());
        try {
            JsonNode json = JSON.MAPPER.readTree(StringUtils.defaultIfBlank(task.getParseJson(), "{}"));
            r.setCoverAttachmentId(json.path("coverAttachmentId").asText(null));
            long durationMs = json.path("durationMs").asLong(0);
            r.setDurationMs(durationMs > 0 ? durationMs : null);
        } catch (Exception ignored) {
            // parse_json 尚未落库或解析失败，封面/时长留空
        }
        r.setVariants(variantsOf(task.getId()).stream().map(this::toVariantResponse).toList());
        return r;
    }

    private ReplicaVariantResponse toVariantResponse(AiReplicaVariant v) {
        ReplicaVariantResponse r = new ReplicaVariantResponse();
        r.setId(v.getId());
        r.setVariantNo(v.getVariantNo());
        r.setHookText(v.getHookText());
        r.setTitleText(v.getTitleText());
        r.setSubtitleStyle(v.getSubtitleStyle());
        r.setRatio(v.getRatio());
        r.setStatus(v.getStatus());
        r.setAttachmentId(v.getAttachmentId());
        r.setErrorMsg(v.getErrorMsg());
        return r;
    }

    // ==================== 私有：工具 ====================

    private AiReplicaTask requireTask(String orgId, String id) {
        AiReplicaTask task = replicaTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        return task;
    }

    private List<AiReplicaVariant> variantsOf(String taskId) {
        LambdaQueryWrapper<AiReplicaVariant> wrapper = new LambdaQueryWrapper<AiReplicaVariant>()
                .eq(AiReplicaVariant::getTaskId, taskId);
        wrapper.orderByAsc(AiReplicaVariant::getVariantNo);
        return replicaVariantMapper.selectListByLambda(wrapper);
    }

    private void failTask(String taskId, String message) {
        AiReplicaTask task = replicaTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        if (PARSE_PARSING.equals(task.getParseStatus())) {
            task.setParseStatus(PARSE_FAILED);
        }
        task.setStatus(TASK_FAILED);
        task.setErrorMsg(truncate(message));
        task.setUpdateTime(System.currentTimeMillis());
        replicaTaskMapper.updateById(task);
    }

    private File workDir(String taskId) {
        return new File(System.getProperty("java.io.tmpdir"), WORK_DIR_PREFIX + taskId);
    }

    private int clampCount(Integer count) {
        if (count == null) {
            return DEFAULT_VARIANT_COUNT;
        }
        return Math.min(Math.max(count, 1), MAX_VARIANT_COUNT);
    }

    private String resolveFileName(ResponseEntity<?> res) {
        String disposition = res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (StringUtils.isNotBlank(disposition)) {
            int idx = disposition.indexOf("filename*=UTF-8''");
            if (idx >= 0) {
                return URLDecoder.decode(disposition.substring(idx + "filename*=UTF-8''".length()).split(";")[0].trim(),
                        StandardCharsets.UTF_8);
            }
            int fn = disposition.indexOf("filename=");
            if (fn >= 0) {
                return disposition.substring(fn + "filename=".length()).split(";")[0].trim().replace("\"", "");
            }
        }
        return "reference.mp4";
    }

    private String extOf(String fileName) {
        String ext = StringUtils.substringAfterLast(fileName, ".").toLowerCase();
        return ext.matches("[a-z0-9]{1,5}") ? ext : "mp4";
    }

    private String baseName(String fileName) {
        return StringUtils.substringBeforeLast(fileName, ".");
    }

    private String truncate(String s) {
        if (s == null) {
            return "处理失败，请稍后重试";
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
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

    /** 剥 ```json ... ``` 包裹，取首个 { 到末个 } 的 JSON 片段；无合法片段返回 null */
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

    private void deleteQuietly(File f) {
        if (f != null && f.exists()) {
            f.delete();
        }
    }

    private void deleteDirQuietly(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteQuietly(c);
            }
        }
        dir.delete();
    }
}
