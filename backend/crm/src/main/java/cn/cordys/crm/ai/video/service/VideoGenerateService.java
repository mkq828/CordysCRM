package cn.cordys.crm.ai.video.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmImageRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.ai.video.domain.AiVideoTask;
import cn.cordys.crm.ai.video.dto.request.VideoGenerateRequest;
import cn.cordys.crm.ai.video.dto.response.VideoTaskResponse;
import cn.cordys.crm.ai.video.tts.TtsConfig;
import cn.cordys.crm.ai.video.tts.TtsProvider;
import cn.cordys.crm.ai.video.tts.TtsProviderFactory;
import cn.cordys.crm.ai.video.tts.TtsVoice;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 短视频生成（功能 7 MVP）：主题 → 口播文案(LLM) → TTS 配音 → FFmpeg 合成 → 存附件。
 * 生成链路长（数十秒），走 {@code @Async} 异步任务 + 前端轮询，复用通话复盘/私信画像的模式。
 */
@Slf4j
@Service
public class VideoGenerateService {

    // ==================== 任务状态机 ====================
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SCRIPTING = "SCRIPTING";
    private static final String STATUS_IMAGING = "IMAGING";
    private static final String STATUS_TTS = "TTS";
    private static final String STATUS_SYNTH = "SYNTH";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    private static final String DEFAULT_VOICE = "Cherry";
    private static final String DEFAULT_STYLE = "口播";

    /** 文生图输出尺寸与画面风格后缀（保证竖屏高清、画面无文字，避免与字幕打架） */
    private static final String IMAGE_SIZE = "1080x1920";
    private static final String IMAGE_PROMPT_SUFFIX = "，竖屏9:16构图，高清写实，画面中不要出现任何文字";

    /** 分镜脚本：标题 + 若干分镜（每镜口播文本 + 配图提示词） */
    public record VideoScript(String title, List<VideoSegment> segments) {}

    public record VideoSegment(String text, String image) {}

    private static final ObjectMapper MAPPER = JSON.MAPPER;

    /** 口播脚本生成 system prompt：钩子开头 + 口语短句 + 讲利益，并拆成分镜配图，输出严格 JSON */
    private static final String SCRIPT_SYSTEM_PROMPT = """
            你是短视频口播脚本专家。请根据用户提供的主题（和可选卖点），生成一条适合抖音/视频号竖屏口播的脚本，并拆成分镜。
            写作要求：
            1. 开头 3 秒有钩子，抓注意力；
            2. 口语化短句、有节奏感，像真人说话；
            3. 讲利益、讲场景，不讲空泛参数；
            4. 全篇控制在 80~150 字左右，用中文标点断句，方便配音；
            5. 拆成 3~5 个分镜，每个分镜是口播的一句或几句，并配一句「画面描述」（描述场景/主体/氛围，用于 AI 生成配图，不要出现文字）。
            只输出一个严格 JSON（不要任何多余文字）：
            {"title":"视频标题","segments":[{"text":"口播内容","image":"画面描述"},{"text":"口播内容","image":"画面描述"}]}
            """;

    /** 配套文案 system prompt：出片后按三平台各出一套可直接发布的文案，复用功能 3 的平台写作口径 */
    private static final String COPY_SYSTEM_PROMPT = """
            你是短视频配套文案专家。已有一条竖屏口播视频，请为它生成 3 套「可直接发布」的配套文案，分别适配抖音、小红书、微信朋友圈。
            写作铁律：讲利益不讲功能、具体优于模糊、口语化、别用空话。
            按平台特点：
            - 抖音：标题要有钩子，正文是口播字幕稿，带 2-4 个话题标签。
            - 小红书：种草体、带 emoji、利他干货、真诚人设，带 2-4 个话题标签。
            - 微信朋友圈：软性信任感、场景代入、口语化、像朋友分享，不需要标签（hashtags 输出空数组）。
            只输出一个严格 JSON（不要任何多余文字）：
            {"douyin":{"title":"标题","copy":"正文","hashtags":["#标签"]},"xiaohongshu":{"title":"标题","copy":"正文","hashtags":["#标签"]},"moments":{"title":"标题","copy":"正文","hashtags":[]}}
            """;

    @Resource
    private BaseMapper<AiVideoTask> aiVideoTaskMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private TtsProviderFactory ttsProviderFactory;
    @Resource
    private FileCommonService fileCommonService;
    @Resource
    private AttachmentService attachmentService;
    @Resource
    private VideoSynthesizer videoSynthesizer;

    /**
     * 提交任务：落库 PENDING 后立即返回。真正生成由 Controller 跨 Bean 调用 {@link #generateAsync}（@Async）。
     */
    public VideoTaskResponse submit(VideoGenerateRequest request, String orgId, String userId) {
        long now = System.currentTimeMillis();
        AiVideoTask task = new AiVideoTask();
        task.setId(IDGenerator.nextStr());
        task.setOrganizationId(orgId);
        task.setTitle(request.getTitle().trim());
        task.setVoice(StringUtils.defaultIfBlank(request.getVoice(), DEFAULT_VOICE));
        task.setStyle(StringUtils.defaultIfBlank(request.getStyle(), DEFAULT_STYLE));
        task.setStatus(STATUS_PENDING);
        task.setProgress(0);
        task.setCreateUser(userId);
        task.setUpdateUser(userId);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        aiVideoTaskMapper.insert(task);
        return toResponse(task);
    }

    /**
     * 异步生成：脚本 → TTS → 合成 → 附件。全程 try/catch，失败归 FAILED 并回显 errorMsg。
     * 注意异步线程拿不到请求上下文，orgId/userId 均由调用方显式传入。
     */
    @Async
    public void generateAsync(String taskId, String orgId, String userId, String extraText) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        File workDir = workDir(taskId);
        workDir.mkdirs();
        File audioFile = new File(workDir, "audio.wav");
        File videoFile = new File(workDir, "output.mp4");
        try {
            updateTask(task, STATUS_SCRIPTING, 10, null, null);
            VideoScript script = generateScript(task.getTitle(), task.getStyle(), extraText, orgId, userId);
            String scriptText = fullScript(script);
            task.setScriptText(scriptText);
            // 配套文案（抖音/小红书/朋友圈）：与功能 3 打通，失败不阻断主流程
            task.setCopyJson(generateCompanionCopy(task.getTitle(), scriptText, orgId, userId));
            updateTask(task, STATUS_IMAGING, 35, null, null);

            List<File> images = generateSceneImages(script, orgId, workDir);
            updateTask(task, STATUS_TTS, 60, null, null);

            synthesizeAudio(scriptText, task.getVoice(), orgId, audioFile);
            updateTask(task, STATUS_SYNTH, 80, null, null);

            List<VideoSynthesizer.Scene> scenes = buildScenes(script, images);
            videoSynthesizer.synthesize(audioFile, videoFile, task.getTitle(), scenes);

            String attachmentId = saveAttachment(videoFile, task, orgId, userId);
            updateTask(task, STATUS_DONE, 100, attachmentId, null);
        } catch (Exception e) {
            log.error("短视频生成失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        } finally {
            deleteQuietly(audioFile);
            deleteQuietly(videoFile);
            workDir.delete();
        }
    }

    public List<VideoTaskResponse> list(String orgId) {
        return aiVideoTaskMapper.selectListByLambda(new LambdaQueryWrapper<AiVideoTask>()
                        .eq(AiVideoTask::getOrganizationId, orgId)
                        .orderByDesc(AiVideoTask::getCreateTime)).stream()
                .map(this::toResponse)
                .toList();
    }

    public VideoTaskResponse get(String orgId, String id) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        return toResponse(task);
    }

    public void delete(String orgId, String id) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        if (StringUtils.isNotBlank(task.getAttachmentId())) {
            attachmentService.delete(task.getAttachmentId());
        }
        aiVideoTaskMapper.deleteByPrimaryKey(id);
    }

    /**
     * 查询当前配音模型支持的音色列表（前端下拉用）。
     * 跟随「模型设置」里 admin 配置的配音模型，换服务商后音色自动切换，无需改前端。
     */
    public List<TtsVoice> voices(String orgId) {
        for (AgentModel model : agentModelService.resolveTtsModels(orgId)) {
            try {
                return ttsProviderFactory.get(model.getProvider()).voices();
            } catch (Exception ignored) {
                // 该模型非 TTS provider 或异常，尝试下一个候选
            }
        }
        return List.of();
    }

    // ==================== 私有：脚本生成 ====================

    private VideoScript generateScript(String title, String style, String extraText, String orgId, String userId) {
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
                llmRequest.setMessages(buildScriptMessages(title, style, extraText));

                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_VIDEO, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return parseVideoScript(sb.toString(), title);
            } catch (Exception e) {
                lastError = e;
                log.warn("短视频脚本生成模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("短视频脚本生成全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildScriptMessages(String title, String style, String extraText) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SCRIPT_SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("主题：").append(title).append('\n');
        if (StringUtils.isNotBlank(style)) {
            sb.append("风格：").append(style.trim()).append('\n');
        }
        if (StringUtils.isNotBlank(extraText)) {
            sb.append("卖点/补充素材：").append(extraText.trim()).append('\n');
        }
        sb.append("请生成口播脚本。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 解析模型返回 JSON 的分镜字段；无合法分镜时回退整段原文作为单分镜（无配图） */
    private VideoScript parseVideoScript(String text, String fallbackTitle) {
        String json = extractJson(text);
        if (json != null) {
            try {
                JsonNode root = MAPPER.readTree(json);
                String title = StringUtils.defaultIfBlank(root.path("title").asText(null), fallbackTitle);
                JsonNode segs = root.path("segments");
                if (segs.isArray() && !segs.isEmpty()) {
                    List<VideoSegment> list = new ArrayList<>();
                    for (JsonNode seg : segs) {
                        String segText = seg.path("text").asText(null);
                        if (StringUtils.isNotBlank(segText)) {
                            list.add(new VideoSegment(segText.trim(), StringUtils.defaultString(seg.path("image").asText(null)).trim()));
                        }
                    }
                    if (!list.isEmpty()) {
                        return new VideoScript(title, list);
                    }
                }
                // 兼容旧格式：只有 script 字段
                String script = root.path("script").asText(null);
                if (StringUtils.isNotBlank(script)) {
                    return new VideoScript(title, List.of(new VideoSegment(script.trim(), "")));
                }
            } catch (Exception e) {
                log.warn("短视频分镜 JSON 解析失败，回退原文", e);
            }
        }
        return new VideoScript(fallbackTitle, List.of(new VideoSegment(text.trim(), "")));
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

    // ==================== 私有：配套文案 ====================

    /** 出片配套文案：调对话模型出抖音/小红书/朋友圈三套，返回 JSON 字符串；任一环节失败返回 null，不阻断主流程 */
    private String generateCompanionCopy(String title, String scriptText, String orgId, String userId) {
        if (StringUtils.isBlank(scriptText)) {
            return null;
        }
        try {
            List<AgentModel> models = agentModelService.resolveChatModels(orgId);
            for (AgentModel model : models) {
                try {
                    aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                    LlmProvider provider = llmProviderFactory.get(model.getProvider());
                    LlmChatRequest llmRequest = new LlmChatRequest();
                    llmRequest.setModel(model.getModelName());
                    llmRequest.setBaseUrl(model.getApiUrl());
                    llmRequest.setApiKey(model.getApiKey());
                    agentModelService.applyModelParams(llmRequest, model);
                    llmRequest.setMessages(buildCopyMessages(title, scriptText));

                    StringBuilder sb = new StringBuilder();
                    LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                    aiQuotaService.record(orgId, AiQuotaConstant.AI_VIDEO, model.getModelName(),
                            usage.getInputTokens(), usage.getOutputTokens(), userId);
                    return parseCompanionCopy(sb.toString());
                } catch (Exception e) {
                    log.warn("短视频配套文案生成失败，尝试下一个候选，provider={}, model={}",
                            model.getProvider(), model.getModelName(), e);
                }
            }
        } catch (Exception e) {
            log.warn("短视频配套文案生成异常，跳过", e);
        }
        return null;
    }

    private List<LlmMessage> buildCopyMessages(String title, String scriptText) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", COPY_SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("视频主题：").append(StringUtils.defaultIfBlank(title, "").trim()).append('\n');
        sb.append("口播文案：\n").append(scriptText.trim()).append('\n');
        sb.append("请生成三套配套文案。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 校验并规范化配套文案 JSON：至少含 douyin/xiaohongshu/moments 之一才视为有效，否则返回 null */
    private String parseCompanionCopy(String text) {
        String json = extractJson(text);
        if (json == null) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (!root.isObject()) {
                return null;
            }
            boolean hasAny = root.has("douyin") || root.has("xiaohongshu") || root.has("moments");
            return hasAny ? root.toString() : null;
        } catch (Exception e) {
            log.warn("短视频配套文案 JSON 解析失败，跳过", e);
            return null;
        }
    }

    // ==================== 私有：配图 ====================

    /** 分镜正文拼接为完整口播文案（配音用） */
    private String fullScript(VideoScript script) {
        return script.segments().stream().map(VideoSegment::text).collect(java.util.stream.Collectors.joining());
    }

    /**
     * 为每个分镜生成配图（豆包 Seedream 文生图）。未配置配图模型或某一镜生成失败时该镜返回 null，
     * 合成阶段回退纯色背景，保证「有图更好、无图也能成片」。
     */
    private List<File> generateSceneImages(VideoScript script, String orgId, File workDir) {
        int size = script.segments().size();
        List<File> images = new ArrayList<>(Collections.nCopies(size, null));
        List<AgentModel> imageModels = agentModelService.resolveImageModels(orgId);
        if (imageModels.isEmpty()) {
            return images;
        }
        AgentModel model = imageModels.get(0);
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (StringUtils.isNotBlank(script.segments().get(i).image())) {
                indexes.add(i);
            }
        }
        if (indexes.isEmpty()) {
            return images;
        }
        // 文生图单张约 2 分钟，逐张串行会拖到近 10 分钟；最多 4 并发并行生成，缩短等待
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(indexes.size(), 4));
        try {
            List<Future<File>> futures = new ArrayList<>(indexes.size());
            for (int index : indexes) {
                VideoSegment segment = script.segments().get(index);
                futures.add(pool.submit(() -> generateOneImage(
                        model, segment.image(), new File(workDir, "scene-" + index + ".png"))));
            }
            for (int i = 0; i < indexes.size(); i++) {
                try {
                    images.set(indexes.get(i), futures.get(i).get());
                } catch (Exception e) {
                    log.warn("短视频配图生成失败，第 {} 镜回退纯色背景，provider={}, model={}",
                            indexes.get(i), model.getProvider(), model.getModelName(), e);
                }
            }
        } finally {
            pool.shutdown();
        }
        return images;
    }

    /** 单张分镜配图：调 Seedream 文生图并落盘；失败返回 null（回退纯色背景） */
    private File generateOneImage(AgentModel model, String imagePrompt, File target) {
        try {
            LlmProvider provider = llmProviderFactory.get(model.getProvider());
            LlmImageRequest request = new LlmImageRequest();
            request.setModel(model.getModelName());
            request.setBaseUrl(StringUtils.defaultIfBlank(model.getApiUrl(), "https://ark.cn-beijing.volces.com/api/v3"));
            request.setApiKey(model.getApiKey());
            request.setSize(IMAGE_SIZE);
            request.setPrompt(imagePrompt + IMAGE_PROMPT_SUFFIX);
            byte[] bytes = provider.generateImage(request);
            Files.write(target.toPath(), bytes);
            return target;
        } catch (Exception e) {
            log.warn("短视频配图生成失败，回退纯色背景，provider={}, model={}",
                    model.getProvider(), model.getModelName(), e);
            return null;
        }
    }

    /** 组装合成器的分镜列表（文本 + 配图文件，无图时 image=null） */
    private List<VideoSynthesizer.Scene> buildScenes(VideoScript script, List<File> images) {
        List<VideoSegment> segments = script.segments();
        List<VideoSynthesizer.Scene> scenes = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            scenes.add(new VideoSynthesizer.Scene(segments.get(i).text(), images.get(i)));
        }
        return scenes;
    }

    // ==================== 私有：TTS / 合成 / 附件 ====================

    private void synthesizeAudio(String script, String voice, String orgId, File audioFile) throws Exception {
        List<AgentModel> ttsModels = agentModelService.resolveTtsModels(orgId);
        Exception lastError = null;
        for (AgentModel model : ttsModels) {
            if (StringUtils.isBlank(model.getApiKey())) {
                continue;
            }
            try {
                TtsConfig config = new TtsConfig();
                config.setModel(model.getModelName());
                config.setApiKey(model.getApiKey());
                config.setModelParams(model.getModelParams());
                TtsProvider tts = ttsProviderFactory.get(model.getProvider());
                byte[] audio = tts.synthesize(script, voice, config);
                Files.write(audioFile.toPath(), audio);
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("短视频 TTS 合成失败，降级尝试下一个，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("短视频 TTS 合成全部失败，候选数={}", ttsModels.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "请先在「模型设置」配置并启用配音模型" : lastError.getMessage());
    }

    private String saveAttachment(File videoFile, AiVideoTask task, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String fileName = safeFileName(task.getTitle()) + ".mp4";
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, task.getId(), attachmentId),
                StorageType.LOCAL.name(),
                fileName);
        fileCommonService.upload(videoFile, request);

        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setName(fileName);
        attachment.setType("mp4");
        attachment.setSize(videoFile.length());
        attachment.setStorage(StorageType.LOCAL.name());
        attachment.setOrganizationId(orgId);
        attachment.setResourceId(task.getId());
        attachment.setCreateTime(System.currentTimeMillis());
        attachment.setCreateUser(userId);
        attachment.setUpdateTime(System.currentTimeMillis());
        attachment.setUpdateUser(userId);
        attachmentMapper.insert(attachment);
        return attachmentId;
    }

    // ==================== 私有：状态/工具 ====================

    private void updateTask(AiVideoTask task, String status, int progress, String attachmentId, String errorMsg) {
        task.setStatus(status);
        task.setProgress(progress);
        if (attachmentId != null) {
            task.setAttachmentId(attachmentId);
        }
        if (errorMsg != null) {
            task.setErrorMsg(errorMsg);
        }
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
    }

    private void failTask(String taskId, String message) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(STATUS_FAILED);
        task.setErrorMsg(StringUtils.left(StringUtils.defaultIfBlank(message, "生成失败"), 500));
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
    }

    private File workDir(String taskId) {
        return new File(System.getProperty("java.io.tmpdir"), "cordys-video-" + taskId);
    }

    private String safeFileName(String title) {
        String base = StringUtils.defaultIfBlank(title, "video");
        String cleaned = base.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
        return StringUtils.isBlank(cleaned) ? "video" : cleaned;
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private VideoTaskResponse toResponse(AiVideoTask task) {
        VideoTaskResponse response = new VideoTaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setVoice(task.getVoice());
        response.setStyle(task.getStyle());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setAttachmentId(task.getAttachmentId());
        response.setCopyJson(task.getCopyJson());
        response.setErrorMsg(task.getErrorMsg());
        response.setCreateTime(task.getCreateTime());
        return response;
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
}
