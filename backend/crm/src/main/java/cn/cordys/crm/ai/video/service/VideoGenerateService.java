package cn.cordys.crm.ai.video.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
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
import cn.cordys.crm.ai.video.tts.TtsProvider;
import cn.cordys.crm.ai.video.tts.TtsProviderFactory;
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
import java.util.List;

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
    private static final String STATUS_TTS = "TTS";
    private static final String STATUS_SYNTH = "SYNTH";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    private static final String DEFAULT_VOICE = "Cherry";
    private static final String DEFAULT_STYLE = "口播";
    private static final String TTS_PROVIDER = "阿里云";

    private static final ObjectMapper MAPPER = JSON.MAPPER;

    /** 口播脚本生成 system prompt：钩子开头 + 口语短句 + 讲利益，输出严格 JSON */
    private static final String SCRIPT_SYSTEM_PROMPT = """
            你是短视频口播脚本专家。请根据用户提供的主题（和可选卖点），生成一条适合抖音/视频号竖屏口播的脚本。
            写作要求：
            1. 开头 3 秒有钩子，抓注意力；
            2. 口语化短句、有节奏感，像真人说话；
            3. 讲利益、讲场景，不讲空泛参数；
            4. 全篇控制在 80~150 字左右，用中文标点断句，方便配音。
            只输出一个严格 JSON（不要任何多余文字）：
            {"title":"视频标题","script":"口播脚本正文，用中文标点断句"}
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
            String script = generateScript(task.getTitle(), task.getStyle(), extraText, orgId, userId);
            task.setScriptText(script);
            updateTask(task, STATUS_TTS, 45, null, null);

            synthesizeAudio(script, task.getVoice(), orgId, audioFile);
            updateTask(task, STATUS_SYNTH, 75, null, null);

            videoSynthesizer.synthesize(audioFile, videoFile, task.getTitle(), script);

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

    // ==================== 私有：脚本生成 ====================

    private String generateScript(String title, String style, String extraText, String orgId, String userId) {
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
                return parseScript(sb.toString());
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

    /** 解析模型返回 JSON 的 script 字段；解析失败回退整段原文 */
    private String parseScript(String text) {
        String json = extractJson(text);
        if (json == null) {
            return text.trim();
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            String script = root.path("script").asText(null);
            if (StringUtils.isNotBlank(script)) {
                return script.trim();
            }
            String title = root.path("title").asText(null);
            if (StringUtils.isNotBlank(title)) {
                return title.trim();
            }
            return text.trim();
        } catch (Exception e) {
            log.warn("短视频脚本 JSON 解析失败，回退原文", e);
            return text.trim();
        }
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

    // ==================== 私有：TTS / 合成 / 附件 ====================

    private void synthesizeAudio(String script, String voice, String orgId, File audioFile) throws Exception {
        String apiKey = agentModelService.resolveProviderApiKey(orgId, TTS_PROVIDER, "语音合成");
        TtsProvider tts = ttsProviderFactory.get(TTS_PROVIDER);
        byte[] audio = tts.synthesize(script, voice, apiKey);
        Files.write(audioFile.toPath(), audio);
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
