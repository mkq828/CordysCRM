package cn.cordys.crm.ai.tool.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmImageRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.ai.tool.domain.AiToolTask;
import cn.cordys.crm.ai.tool.dto.request.AiToolImageRequest;
import cn.cordys.crm.ai.tool.dto.request.AiToolTaskPageRequest;
import cn.cordys.crm.ai.tool.dto.request.AiToolTextRequest;
import cn.cordys.crm.ai.tool.dto.request.AiToolTranscodeRequest;
import cn.cordys.crm.ai.tool.dto.response.AiToolTaskResponse;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AI 工具中心（创作中心子模块）：文案改写 / 爆款标题 / 口播脚本走流式对话，文生图 / 视频转格式走异步任务。
 * 复用额度→模型→provider→计费四步链路，统一按 {@code ai_tool} 记账；纯生成型，输出落附件系统。
 */
@Service
@Slf4j
public class AiToolService {

    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_TRANSCODE = "TRANSCODE";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    /** 文生图输出尺寸与画面风格后缀（竖屏高清、无文字无人物，避免与后期字幕打架） */
    private static final String DEFAULT_IMAGE_SIZE = "1080x1920";
    private static final String IMAGE_PROMPT_SUFFIX = "，竖屏9:16构图，高清写实电影质感，画面中不要出现任何文字";

    /** 文案改写 system prompt：重写为可直接发布的口语文案 */
    private static final String REWRITE_SYSTEM_PROMPT = """
            你是资深文案改写专家。请把用户提供的原文改写成一条更优质、可直接发布的文案。
            写作铁律：
            1. 讲利益不讲功能：说读者能得到的改变/好处，而不是罗列参数。
            2. 具体优于模糊：用场景、数字、对比代替「高效、优质、专业」这类空话。
            3. 保留原文核心信息与卖点，不无中生有。
            若用户指定了风格/平台，严格按该风格改写。
            直接输出改写后的文案，不要任何解释、不要「好的」等客套话，不要用代码块包裹。
            """;

    /** 爆款标题 system prompt：输出多个钩子感标题 */
    private static final String TITLE_SYSTEM_PROMPT = """
            你是爆款标题专家。请根据用户提供的主题/内容，生成 5~8 个高点击率标题。
            要求：
            1. 3 秒钩子、爆点前置、制造好奇或冲突，避免「高效、优质、专业」这类空话。
            2. 若用户指定平台（抖音/小红书/朋友圈），按平台调性写；抖音强调冲突与钩子，小红书利他干货，朋友圈像朋友分享。
            3. 标题长短结合，涵盖悬念型、利益型、数字型、对比型。
            每行输出一个标题（可加序号），不要任何解释、不要代码块包裹。
            """;

    /** 口播脚本 system prompt：竖屏口播稿 */
    private static final String SCRIPT_SYSTEM_PROMPT = """
            你是短视频口播脚本专家。请根据用户提供的主题/内容，生成一条适合抖音/视频号竖屏口播的脚本。
            写作要求：
            1. 开头 3 秒有钩子，抓注意力；
            2. 口语化短句、有节奏感，像真人说话；
            3. 讲利益、讲场景，不讲空泛参数；
            4. 全篇控制在 90~160 字，用中文标点断句方便配音；
            5. 结尾给一句互动引导（点赞/关注/评论）。
            直接输出脚本正文，不要任何解释、不要代码块包裹。
            """;

    @Resource
    private BaseMapper<AiToolTask> aiToolTaskMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private FileCommonService fileCommonService;
    @Resource
    private AttachmentService attachmentService;
    @Resource
    private FfmpegUtil ffmpegUtil;

    // ==================== 文案类工具（流式） ====================

    /** 文案类工具流式生成：返回全文文本与用量，控制器负责把 chunk 转发为 SSE。 */
    public AiStreamResult<String> generateText(String orgId, AiToolTextRequest request, Consumer<String> onChunk) {
        String type = StringUtils.defaultIfBlank(request.getType(), "").trim().toUpperCase();
        return switch (type) {
            case "TITLE", "爆款标题" -> doGenerate(orgId, buildMessages(TITLE_SYSTEM_PROMPT, request), onChunk);
            case "SCRIPT", "口播脚本" -> doGenerate(orgId, buildMessages(SCRIPT_SYSTEM_PROMPT, request), onChunk);
            default -> doGenerate(orgId, buildMessages(REWRITE_SYSTEM_PROMPT, request), onChunk);
        };
    }

    private List<LlmMessage> buildMessages(String systemPrompt, AiToolTextRequest request) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", systemPrompt));
        StringBuilder sb = new StringBuilder();
        sb.append("原文/主题：\n").append(request.getContent().trim());
        if (StringUtils.isNotBlank(request.getOption())) {
            sb.append("\n\n补充要求（风格/平台等）：").append(request.getOption().trim());
        }
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 纯文本流式生成链路：解析模型 → 额度预检 → 候选降级循环 → 计费，返回全文文本。 */
    private AiStreamResult<String> doGenerate(String orgId, List<LlmMessage> messages, Consumer<String> onChunk) {
        List<AgentModel> models = agentModelService.resolveChatModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }

        AtomicBoolean emitted = new AtomicBoolean(false);
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

                StringBuilder full = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    full.append(chunk);
                    onChunk.accept(chunk);
                });
                aiQuotaService.record(orgId, AiQuotaConstant.AI_TOOL, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return new AiStreamResult<>(full.toString().trim(), usage);
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("AI 工具生成模型调用中途失败，provider={}, model={}",
                            model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("AI 工具生成模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("AI 工具生成全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    // ==================== 文生图（异步） ====================

    public AiToolTaskResponse submitImage(AiToolImageRequest request, String orgId, String userId) {
        Map<String, Object> params = Map.of(
                "prompt", request.getPrompt().trim(),
                "size", StringUtils.defaultIfBlank(request.getSize(), DEFAULT_IMAGE_SIZE));
        return submit(TYPE_IMAGE, snippet(request.getPrompt()), JSON.toJSONString(params), orgId, userId);
    }

    @Async
    public void generateImageAsync(String taskId, String orgId, String userId) {
        AiToolTask task = aiToolTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        File workDir = workDir(taskId);
        workDir.mkdirs();
        File imageFile = new File(workDir, "output.png");
        try {
            updateStatus(task, STATUS_PROCESSING, 30, null, null);
            List<AgentModel> models = agentModelService.resolveImageModels(orgId);
            if (models.isEmpty()) {
                throw new GenericException("请先在「模型设置」中配置并启用豆包 Seedream 文生图模型");
            }
            AgentModel model = models.get(0);
            Map<String, Object> params = parseParams(task.getParams());
            LlmProvider provider = llmProviderFactory.get(model.getProvider());
            LlmImageRequest imageRequest = new LlmImageRequest();
            imageRequest.setModel(model.getModelName());
            imageRequest.setBaseUrl(StringUtils.defaultIfBlank(model.getApiUrl(), "https://ark.cn-beijing.volces.com/api/v3"));
            imageRequest.setApiKey(model.getApiKey());
            imageRequest.setSize(String.valueOf(params.getOrDefault("size", DEFAULT_IMAGE_SIZE)));
            imageRequest.setPrompt(params.get("prompt") + IMAGE_PROMPT_SUFFIX);

            byte[] bytes = provider.generateImage(imageRequest);
            Files.write(imageFile.toPath(), bytes);
            updateStatus(task, STATUS_PROCESSING, 80, null, null);

            String attachmentId = saveAttachment(imageFile, task, "output.png", "png", orgId, userId);
            updateStatus(task, STATUS_DONE, 100, attachmentId, null);
        } catch (Exception e) {
            log.error("文生图任务失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        } finally {
            deleteQuietly(imageFile);
            workDir.delete();
        }
    }

    // ==================== 视频转格式（异步） ====================

    public AiToolTaskResponse submitTranscode(AiToolTranscodeRequest request, String orgId, String userId) {
        String format = normalizeFormat(request.getFormat());
        String name = deriveOutputName(request.getSourceName(), format);
        Map<String, Object> params = Map.of(
                "sourceAttachmentId", request.getSourceAttachmentId().trim(),
                "format", format);
        return submit(TYPE_TRANSCODE, name, JSON.toJSONString(params), orgId, userId);
    }

    @Async
    public void generateTranscodeAsync(String taskId, String orgId, String userId) {
        AiToolTask task = aiToolTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        File workDir = workDir(taskId);
        workDir.mkdirs();
        File sourceFile = new File(workDir, "source");
        File outputFile = null;
        try {
            updateStatus(task, STATUS_PROCESSING, 20, null, null);
            Map<String, Object> params = parseParams(task.getParams());
            String sourceAttachmentId = String.valueOf(params.get("sourceAttachmentId"));
            String format = String.valueOf(params.getOrDefault("format", "mp4"));
            outputFile = new File(workDir, "output." + format);

            ResponseEntity<org.springframework.core.io.Resource> resource = attachmentService.getResource(sourceAttachmentId);
            if (resource == null || resource.getBody() == null) {
                throw new GenericException("源视频不存在或已删除");
            }
            try (InputStream in = resource.getBody().getInputStream()) {
                Files.copy(in, sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            updateStatus(task, STATUS_PROCESSING, 40, null, null);

            ffmpegUtil.transcode(sourceFile, outputFile, format);
            updateStatus(task, STATUS_PROCESSING, 90, null, null);

            String attachmentId = saveAttachment(outputFile, task, task.getName(), format, orgId, userId);
            updateStatus(task, STATUS_DONE, 100, attachmentId, null);
        } catch (Exception e) {
            log.error("视频转格式任务失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        } finally {
            deleteQuietly(sourceFile);
            deleteQuietly(outputFile);
            workDir.delete();
        }
    }

    // ==================== 任务查询 ====================

    public List<AiToolTaskResponse> list(String orgId) {
        return aiToolTaskMapper.selectListByLambda(new LambdaQueryWrapper<AiToolTask>()
                        .eq(AiToolTask::getOrganizationId, orgId)
                        .orderByDesc(AiToolTask::getCreateTime)).stream()
                .map(this::toResponse)
                .toList();
    }

    public Pager<List<AiToolTaskResponse>> page(String orgId, AiToolTaskPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<AiToolTaskResponse> list = aiToolTaskMapper.selectListByLambda(new LambdaQueryWrapper<AiToolTask>()
                        .eq(AiToolTask::getOrganizationId, orgId)
                        .orderByDesc(AiToolTask::getCreateTime)).stream()
                .map(this::toResponse)
                .toList();
        return PageUtils.setPageInfo(page, list);
    }

    public AiToolTaskResponse get(String orgId, String id) {
        AiToolTask task = aiToolTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        return toResponse(task);
    }

    public void delete(String orgId, String id) {
        AiToolTask task = aiToolTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        if (StringUtils.isNotBlank(task.getAttachmentId())) {
            attachmentService.delete(task.getAttachmentId());
        }
        aiToolTaskMapper.deleteByPrimaryKey(id);
    }

    // ==================== 私有：任务/附件/工具 ====================

    private AiToolTaskResponse submit(String type, String name, String params, String orgId, String userId) {
        long now = System.currentTimeMillis();
        AiToolTask task = new AiToolTask();
        task.setId(IDGenerator.nextStr());
        task.setOrganizationId(orgId);
        task.setType(type);
        task.setName(name);
        task.setParams(params);
        task.setStatus(STATUS_PENDING);
        task.setProgress(0);
        task.setCreateUser(userId);
        task.setUpdateUser(userId);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        aiToolTaskMapper.insert(task);
        return toResponse(task);
    }

    private Map<String, Object> parseParams(String params) {
        Map<String, Object> map = JSON.parseToMap(params);
        return map == null ? Map.of() : map;
    }

    private String normalizeFormat(String format) {
        String ext = format == null ? "mp4" : format.trim().toLowerCase();
        return FfmpegUtil.FORMATS.contains(ext) ? ext : "mp4";
    }

    /** 转格式后的文件名沿用源文件名，仅替换扩展名，便于下载后直接播放。 */
    private String deriveOutputName(String sourceName, String format) {
        String base = sourceName;
        if (StringUtils.isNotBlank(base)) {
            int dot = base.lastIndexOf('.');
            if (dot > 0) {
                base = base.substring(0, dot);
            }
        }
        return (StringUtils.isBlank(base) ? "视频" : base) + "." + format;
    }

    private String saveAttachment(File file, AiToolTask task, String fileName, String type, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, task.getId(), attachmentId),
                StorageType.LOCAL.name(),
                fileName);
        fileCommonService.upload(file, request);

        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setName(fileName);
        attachment.setType(type);
        attachment.setSize(file.length());
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

    private void updateStatus(AiToolTask task, String status, int progress, String attachmentId, String errorMsg) {
        task.setStatus(status);
        task.setProgress(progress);
        if (attachmentId != null) {
            task.setAttachmentId(attachmentId);
        }
        if (errorMsg != null) {
            task.setErrorMsg(errorMsg);
        }
        task.setUpdateTime(System.currentTimeMillis());
        aiToolTaskMapper.updateById(task);
    }

    private void failTask(String taskId, String message) {
        AiToolTask task = aiToolTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(STATUS_FAILED);
        task.setErrorMsg(StringUtils.left(StringUtils.defaultIfBlank(message, "处理失败"), 500));
        task.setUpdateTime(System.currentTimeMillis());
        aiToolTaskMapper.updateById(task);
    }

    private AiToolTaskResponse toResponse(AiToolTask task) {
        AiToolTaskResponse response = new AiToolTaskResponse();
        response.setId(task.getId());
        response.setType(task.getType());
        response.setName(task.getName());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setAttachmentId(task.getAttachmentId());
        response.setErrorMsg(task.getErrorMsg());
        response.setCreateTime(task.getCreateTime());
        return response;
    }

    private File workDir(String taskId) {
        return new File(System.getProperty("java.io.tmpdir"), "cordys-tool-" + taskId);
    }

    private String snippet(String text) {
        String t = StringUtils.defaultIfBlank(text, "文生图").trim();
        return t.length() > 20 ? t.substring(0, 20) + "…" : t;
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
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
