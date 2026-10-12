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
import cn.cordys.crm.ai.video.domain.VideoTemplateConfig;
import cn.cordys.crm.ai.video.dto.request.VideoExpandRequest;
import cn.cordys.crm.ai.video.dto.request.VideoGenerateRequest;
import cn.cordys.crm.ai.video.dto.request.VideoPromptUpdateRequest;
import cn.cordys.crm.ai.video.dto.request.VideoShotUpdateRequest;
import cn.cordys.crm.ai.video.dto.request.VideoVoicePreviewRequest;
import cn.cordys.crm.ai.video.dto.response.VideoExpandResponse;
import cn.cordys.crm.ai.video.dto.response.VideoModelOption;
import cn.cordys.crm.ai.video.dto.response.VideoModelsResponse;
import cn.cordys.crm.ai.video.dto.response.VideoTaskResponse;
import cn.cordys.crm.ai.video.dto.response.VideoVoicePreviewResponse;
import cn.cordys.crm.ai.video.provider.SeedanceVideoProvider;
import cn.cordys.crm.ai.video.tts.TtsConfig;
import cn.cordys.crm.ai.video.tts.TtsProvider;
import cn.cordys.crm.ai.video.tts.TtsProviderFactory;
import cn.cordys.crm.ai.video.tts.TtsVoice;
import cn.cordys.crm.ai.volcengine.VolcengineVoiceCredential;
import cn.cordys.crm.ai.voiceclone.domain.AiCloneVoice;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

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
    private static final String STATUS_SCRIPT_READY = "SCRIPT_READY";
    private static final String STATUS_IMAGING = "IMAGING";
    private static final String STATUS_TTS = "TTS";
    private static final String STATUS_ASSETS_READY = "ASSETS_READY";
    private static final String STATUS_SYNTH = "SYNTH";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_VIDEOING = "VIDEOING";
    private static final String STATUS_VIDEOS_READY = "VIDEOS_READY";

    private static final String MODE_VIDEO = "video";
    private static final String MODE_IMAGE_TEXT = "image_text";

    private static final String VIDEO_TYPE_TALK = "talk";
    private static final String VIDEO_TYPE_PRODUCT = "product";
    private static final String VIDEO_TYPE_SCENARIO = "scenario";
    private static final String VIDEO_TYPE_COMIC = "comic";

    private static final String DEFAULT_VOICE = "Cherry";

    /** 成片比例 → Seedream 出图尺寸（宽x高），仅保留 Seedream 支持且产品需要的三档 */
    private static String imageSizeForRatio(String ratio) {
        return switch (normalizeRatio(ratio)) {
            case "16:9" -> "1920x1080";
            case "1:1" -> "1024x1024";
            default -> "1080x1920";
        };
    }

    /** 成片比例 → 合成画布宽高（宽, 高） */
    private static int[] dimsForRatio(String ratio) {
        return switch (normalizeRatio(ratio)) {
            case "16:9" -> new int[]{1920, 1080};
            case "1:1" -> new int[]{1080, 1080};
            default -> new int[]{1080, 1920};
        };
    }

    /** 空值或非法比例回退竖屏 9:16 */
    private static String normalizeRatio(String ratio) {
        String r = StringUtils.defaultIfBlank(ratio, "9:16").trim();
        return switch (r) {
            case "16:9", "1:1" -> r;
            default -> "9:16";
        };
    }

    /** 画面通用约束（所有模板共享，避免字幕打架/人物肖像），模板只提供 imageStyle 风格部分 */
    private static final String IMAGE_NO_TEXT_SUFFIX =
            "，画面中不要出现任何文字，不要出现人物与正面人脸（可用产品特写、场景氛围、细节空镜、手部动作替代）";

    /** 产品讲解画面约束：画面必须出现产品主体，避免掉回空镜，仍不出现文字与真人面部 */
    private static final String IMAGE_PRODUCT_SUFFIX =
            "，画面必须突出产品主体（产品特写、使用场景、细节展示），不要出现任何文字，避免出现真人面部";

    /** 情景剧画面约束：画面必须包含统一角色与具体场景动作，用于讲故事推进剧情 */
    private static final String IMAGE_SCENARIO_SUFFIX =
            "，画面必须包含 1~2 位固定角色与具体场景动作（人物全身或半身、有动作与情绪），用于推进剧情，不要出现任何文字";

    /** 分镜脚本：标题 + 若干分镜（每镜口播文本 + 配图提示词） */
    public record VideoScript(String title, List<VideoSegment> segments) {}

    /**
     * 分镜：text=口播、image=画面描述、imageAttachmentId=配图附件ID、videoPrompt=视频提示词、videoAttachmentId=镜头视频附件ID。
     * 配图/视频附件ID与视频提示词在对应生成阶段回填，未生成时为空串。
     */
    public record VideoSegment(String text, String image, String imageAttachmentId, String videoPrompt,
            String videoAttachmentId) {}

    /** 结构化创作简报：用户直接粘贴「口播文案 + 画面素材 + 字幕」三段的完整简报 */
    private record StructuredBrief(String narration, List<String> imageHints, List<String> captionLines) {}

    private static final ObjectMapper MAPPER = JSON.MAPPER;

    /** 口播脚本生成 system prompt：钩子开头 + 口语短句 + 讲利益，并拆成分镜配图，输出严格 JSON */
    private static final String SCRIPT_SYSTEM_PROMPT = """
            你是短视频口播脚本专家。请根据用户提供的主题（和可选卖点），生成一条适合抖音/视频号竖屏口播的脚本，并拆成分镜。
            写作要求：
            1. 开头 3 秒有钩子，抓注意力；
            2. 口语化短句、有节奏感，像真人说话；
            3. 讲利益、讲场景，不讲空泛参数；
            4. 全篇控制在 90~160 字左右，用中文标点断句，方便配音；
            5. 按内容需要自动决定分镜数量（一般 4~8 个，内容多可适当增加），每个分镜是口播的一句或几句，并配一句「画面描述」。
            画面描述要求：用于 AI 生成配图，必须是产品特写、场景氛围、细节空镜或手部动作（如切菜、倒茶、展示产品），像 B-roll 镜头；不要出现人物、人脸、主播，不要出现文字。
            只输出一个严格 JSON（不要任何多余文字）：
            {"title":"视频标题","segments":[{"text":"口播内容","image":"画面描述"},{"text":"口播内容","image":"画面描述"}]}
            """;

    /** 产品讲解脚本 system prompt：按卖点逐条切镜，画面必须出现产品主体 */
    private static final String SCRIPT_SYSTEM_PROMPT_PRODUCT = """
            你是短视频产品讲解脚本专家。请根据用户提供的主题与卖点，生成一条抖音/视频号竖屏产品讲解脚本，并拆成分镜。
            写作要求：
            1. 开头 3 秒有钩子，点出用户痛点；
            2. 口语化短句、有节奏感，像真人讲解；
            3. 按卖点逐条讲解，每条卖点讲清「给用户带来什么利益」，不讲空泛参数；
            4. 全篇控制在 90~160 字左右，用中文标点断句，方便配音；
            5. 按内容需要自动决定分镜数量（一般 4~8 个，内容多可适当增加），每个分镜是口播的一句或几句，并配一句「画面描述」。
            画面描述要求：用于 AI 生成配图，画面必须突出产品主体（产品特写、使用场景、细节展示），像产品宣传镜头；不要出现真人面部，不要出现文字。
            只输出一个严格 JSON（不要任何多余文字）：
            {"title":"视频标题","segments":[{"text":"口播内容","image":"画面描述"},{"text":"口播内容","image":"画面描述"}]}
            """;

    /** 情景剧脚本 system prompt：钩子→冲突/痛点→解决→行动，画面带固定角色推进剧情 */
    private static final String SCRIPT_SYSTEM_PROMPT_SCENARIO = """
            你是短视频情景剧脚本专家。请根据用户提供的主题，生成一条抖音/视频号竖屏情景剧脚本，用故事传播内容，并拆成分镜。
            写作要求：
            1. 结构完整：开头钩子 → 冲突/痛点 → 解决 → 行动号召；
            2. 1~2 位固定角色贯穿全片，角色有身份、有情绪，通过对话或动作推进剧情；
            3. 口语化短句、有画面感，适合配音；
            4. 全篇控制在 90~160 字左右，用中文标点断句；
            5. 按内容需要自动决定分镜数量（一般 4~8 个，内容多可适当增加），每个分镜是口播的一句或几句，并配一句「画面描述」。
            画面描述要求：用于 AI 生成配图，画面必须包含固定角色与具体场景动作（人物全身或半身、有动作与情绪），像短剧镜头；不要出现文字。
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

    /** 画面描述 system prompt：给定已定稿的口播分镜，只补配图画面描述，不改动口播文字，输出严格 JSON 数组 */
    private static final String IMAGE_DESC_SYSTEM_PROMPT = """
            你是短视频画面描述专家。给定视频主题与若干段口播文案，为每一段各生成一句「画面描述」，用于 AI 文生图。
            要求：产品特写、场景氛围、细节空镜或手部动作（如切菜、倒茶、展示产品），像 B-roll 镜头；不要出现人物、人脸、主播；不要出现文字。
            只输出一个严格 JSON 数组（不要任何多余文字），数组长度必须与口播段落数一致：
            ["画面描述1","画面描述2","画面描述3"]
            """;

    /** 产品讲解画面描述 system prompt：画面必须出现产品主体 */
    private static final String IMAGE_DESC_SYSTEM_PROMPT_PRODUCT = """
            你是短视频画面描述专家。给定视频主题与若干段口播文案，为每一段各生成一句「画面描述」，用于 AI 文生图。
            要求：画面必须突出产品主体（产品特写、使用场景、细节展示），像产品宣传镜头；不要出现真人面部；不要出现文字。
            只输出一个严格 JSON 数组（不要任何多余文字），数组长度必须与口播段落数一致：
            ["画面描述1","画面描述2","画面描述3"]
            """;

    /** 情景剧画面描述 system prompt：画面必须包含固定角色与场景动作 */
    private static final String IMAGE_DESC_SYSTEM_PROMPT_SCENARIO = """
            你是短视频画面描述专家。给定视频主题与若干段口播文案，为每一段各生成一句「画面描述」，用于 AI 文生图。
            要求：画面必须包含 1~2 位固定角色与具体场景动作（人物全身或半身、有动作与情绪），像短剧镜头，用于推进剧情；不要出现文字。
            只输出一个严格 JSON 数组（不要任何多余文字），数组长度必须与口播段落数一致：
            ["画面描述1","画面描述2","画面描述3"]
            """;

    /** 主题扩写 system prompt：把粗略想法扩写成更具体、适合做科普短片的主题，返回标题+主题 */
    private static final String EXPAND_THEME_SYSTEM_PROMPT = """
            你是短视频选题专家。把用户给的粗略想法扩写成一条更具体、更适合做科普短片的主题。
            要求：受众明确、含核心知识点、口语自然，一句话到两句话；标题 8~16 字、有钩子。
            只输出一个严格 JSON（不要任何多余文字）：
            {"title":"简短标题","content":"扩写后的主题"}
            """;

    /** 文案扩写 system prompt：把主题扩写成一条完整口播文案，返回标题+文案 */
    private static final String EXPAND_SCRIPT_SYSTEM_PROMPT = """
            你是短视频口播文案专家。把用户给的主题扩写成一条完整的口播文案，适合抖音/视频号竖屏科普。
            要求：开头 3 秒钩子、口语短句有节奏、讲利益讲场景、全篇 90~160 字、用中文标点断句；标题 8~16 字、有钩子。
            只输出一个严格 JSON（不要任何多余文字）：
            {"title":"简短标题","content":"完整口播文案"}
            """;

    /** 风格提示词 system prompt：根据用户口播内容生成一句画面风格提示词（风格页「AI 生成风格提示词」） */
    private static final String STYLE_PROMPT_SYSTEM_PROMPT = """
            你是短视频画面风格提示词专家。根据用户提供的主题/口播内容，生成一句「画面风格提示词」，用于文生图统一画风。
            要求：中文、20~40 字、只描述整体视觉风格与色调（如「国风水墨、暖色、留白、手绘质感」「明亮通透的都市实拍、浅色调」），
            不写人物姓名、不写镜头号、不写具体画面内容。
            语境约束：面向企业/公司客户（B2B、写字楼办公场景），不得出现「门店、店铺、实体店、到店、引流、招商、加盟、零售、档口、摊点」等零售门店词汇。
            只输出一个严格 JSON（不要任何多余文字）：
            {"content":"画面风格提示词"}
            """;

    /** 内置提示词 system prompt：根据主题一次生成 风格/角色/额外 三条提示词（分镜台「换一批」） */
    private static final String PROMPTS_SYSTEM_PROMPT = """
            你是短视频分镜提示词专家。根据用户提供的主题/口播内容，一次生成三条提示词，用于统一画面、角色与补充要求。
            要求：
            - stylePrompt 风格提示词：中文、20~40 字，只描述整体视觉风格与色调（如「国风水墨、暖色、留白、手绘质感」），不写人物姓名、不写镜头号、不写具体画面内容。
            - characterPrompt 角色提示词：中文、15~40 字，描述是否出镜、出镜人物或角色的统一形象与气质（如「不出镜，仅出现手部特写」或「一位亲和的女讲解员，浅色衬衫，正面半身」），无明确需求时写「AI 按模板与主题决定」。
            - extraPrompt 额外要求：中文、15~40 字，补充画面或文案的附加约束（如「画面避免出现文字与 logo」「结尾留 2 秒品牌引导」），无额外要求时写「无」。
            语境约束：面向企业/公司客户（B2B、写字楼办公场景），三条提示词均不得出现「门店、店铺、实体店、到店、引流、招商、加盟、零售、档口、摊点」等零售门店词汇，语境统一为办公/工厂/产品交付。
            只输出一个严格 JSON（不要任何多余文字）：
            {"stylePrompt":"…","characterPrompt":"…","extraPrompt":"…"}
            """;

    /** 音色试听样例文案（约 5 秒，风格页「试听」按钮） */
    private static final String PREVIEW_TEXT = "大家好，这是当前音色的试听效果，用于短视频配音讲解。";

    @Resource
    private BaseMapper<AiVideoTask> aiVideoTaskMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private BaseMapper<AiCloneVoice> aiCloneVoiceMapper;
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
    @Resource
    private VideoTemplateService videoTemplateService;
    @Resource
    private SeedanceVideoProvider seedanceVideoProvider;

    /**
     * 提交任务：落库 PENDING 后立即返回。真正生成由 Controller 跨 Bean 调用 {@link #generateScriptAsync}（@Async）。
     */
    public VideoTaskResponse submit(VideoGenerateRequest request, String orgId, String userId) {
        long now = System.currentTimeMillis();
        AiVideoTask task = new AiVideoTask();
        task.setId(IDGenerator.nextStr());
        task.setOrganizationId(orgId);
        task.setTitle(request.getTitle().trim());
        task.setSourceText(StringUtils.defaultIfBlank(request.getSourceText(), request.getTitle()));
        task.setVoice(StringUtils.defaultIfBlank(request.getVoice(), DEFAULT_VOICE));
        String templateKey = videoTemplateService.resolveKey(request.getTemplateKey());
        task.setTemplateKey(templateKey);
        task.setStyle(videoTemplateService.resolveName(templateKey));
        task.setRatio(normalizeRatio(request.getRatio()));
        task.setMode(normalizeMode(request.getMode()));
        task.setVideoType(normalizeVideoType(request.getVideoType()));
        task.setImageModelId(StringUtils.trimToNull(request.getImageModelId()));
        task.setVideoModelId(StringUtils.trimToNull(request.getVideoModelId()));
        task.setStylePrompt(StringUtils.trimToNull(request.getStylePrompt()));
        task.setCharacterPrompt(StringUtils.trimToNull(request.getCharacterPrompt()));
        task.setExtraPrompt(StringUtils.trimToNull(request.getExtraText()));
        task.setStatus(STATUS_PENDING);
        task.setProgress(0);
        task.setCreateUser(userId);
        task.setUpdateUser(userId);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        aiVideoTaskMapper.insert(task);
        return toResponse(task);
    }

    /** 成片方式归一化：仅 video / image_text 合法，其余回退 video */
    private String normalizeMode(String mode) {
        String m = StringUtils.defaultIfBlank(mode, MODE_VIDEO).trim();
        return MODE_IMAGE_TEXT.equals(m) ? MODE_IMAGE_TEXT : MODE_VIDEO;
    }

    /** 成片类型归一化：product/scenario/comic 之外一律回退 talk（口播科普） */
    private String normalizeVideoType(String videoType) {
        String t = StringUtils.defaultIfBlank(videoType, VIDEO_TYPE_TALK).trim();
        return switch (t) {
            case VIDEO_TYPE_PRODUCT, VIDEO_TYPE_SCENARIO, VIDEO_TYPE_COMIC -> t;
            default -> VIDEO_TYPE_TALK;
        };
    }

    /** 按成片类型选口播脚本 system prompt */
    private String scriptSystemPrompt(String videoType) {
        return switch (videoType) {
            case VIDEO_TYPE_PRODUCT -> SCRIPT_SYSTEM_PROMPT_PRODUCT;
            case VIDEO_TYPE_SCENARIO -> SCRIPT_SYSTEM_PROMPT_SCENARIO;
            default -> SCRIPT_SYSTEM_PROMPT;
        };
    }

    /** 按成片类型选画面描述 system prompt（现成脚本补画面描述用） */
    private String imageDescSystemPrompt(String videoType) {
        return switch (videoType) {
            case VIDEO_TYPE_PRODUCT -> IMAGE_DESC_SYSTEM_PROMPT_PRODUCT;
            case VIDEO_TYPE_SCENARIO -> IMAGE_DESC_SYSTEM_PROMPT_SCENARIO;
            default -> IMAGE_DESC_SYSTEM_PROMPT;
        };
    }

    /** 按成片类型选文生图/图生视频画面约束后缀 */
    private String imageConstraintSuffix(String videoType) {
        return switch (videoType) {
            case VIDEO_TYPE_PRODUCT -> IMAGE_PRODUCT_SUFFIX;
            case VIDEO_TYPE_SCENARIO -> IMAGE_SCENARIO_SUFFIX;
            default -> IMAGE_NO_TEXT_SUFFIX;
        };
    }

    /**
     * 异步生成分镜脚本：只生成口播分镜 + 画面描述 + 配套文案，落库后停在 SCRIPT_READY 等人确认。
     * 与 printfilm 的「确认分镜」阶段一致：脚本可编辑，用户点「确认分镜」才进入出图配音。
     */
    @Async
    public void generateScriptAsync(String taskId, String orgId, String userId) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        try {
            VideoTemplateConfig template = videoTemplateService.resolveConfig(task.getTemplateKey());
            updateTask(task, STATUS_SCRIPTING, 10, null, null);
            String source = StringUtils.defaultIfBlank(task.getSourceText(), task.getTitle());
            VideoScript script = buildScript(source, task.getVideoType(), template,
                    task.getStylePrompt(), task.getCharacterPrompt(), task.getExtraPrompt(), orgId, userId);
            String scriptText = fullScript(script.segments());
            task.setScriptText(scriptText);
            // 分镜台展示用：先把「口播分镜 + 画面描述」落库（配图附件ID后续回填）
            task.setStoryboardJson(storyboardToJson(script.segments()));
            // 配套文案（抖音/小红书/朋友圈）：与功能 3 打通，失败不阻断主流程
            task.setCopyJson(generateCompanionCopy(task.getTitle(), scriptText, orgId, userId));
            // 内置提示词预生成：进入分镜台时 风格/角色/额外 三条 chip 直接显示，无需点开编辑弹窗再生成
            fillPromptsIfBlank(task, orgId, userId);
            updateTask(task, STATUS_SCRIPT_READY, 30, null, null);
        } catch (Exception e) {
            log.error("短视频分镜脚本生成失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        }
    }

    /**
     * 内置提示词预生成：仅补齐空白项（风格/角色/额外），已填的保留；失败不阻断脚本就绪，
     * 分镜台仍可通过编辑弹窗「换一批」补齐。
     */
    private void fillPromptsIfBlank(AiVideoTask task, String orgId, String userId) {
        boolean styleBlank = StringUtils.isBlank(task.getStylePrompt());
        boolean characterBlank = StringUtils.isBlank(task.getCharacterPrompt());
        boolean extraBlank = StringUtils.isBlank(task.getExtraPrompt());
        if (!styleBlank && !characterBlank && !extraBlank) {
            return;
        }
        try {
            VideoExpandResponse prompts = generatePrompts(
                    StringUtils.defaultIfBlank(task.getSourceText(), task.getTitle()), orgId, userId);
            if (styleBlank && StringUtils.isNotBlank(prompts.getStylePrompt())) {
                task.setStylePrompt(prompts.getStylePrompt());
            }
            if (characterBlank && StringUtils.isNotBlank(prompts.getCharacterPrompt())) {
                task.setCharacterPrompt(prompts.getCharacterPrompt());
            }
            if (extraBlank && StringUtils.isNotBlank(prompts.getExtraPrompt())) {
                task.setExtraPrompt(prompts.getExtraPrompt());
            }
        } catch (Exception e) {
            log.warn("短视频内置提示词预生成失败，保留已填项，taskId={}", task.getId(), e);
        }
    }

    /**
     * 确认分镜：把任务从 SCRIPT_READY 推进到出图配音（异步）。
     */
    public VideoTaskResponse confirmAssets(String orgId, String id, String userId) {
        AiVideoTask task = requireTask(orgId, id);
        if (!STATUS_SCRIPT_READY.equals(task.getStatus())) {
            throw new GenericException("当前状态不可生成画面，请先等待分镜脚本就绪");
        }
        generateAssetsAsync(id, orgId, userId);
        return toResponse(task);
    }

    /**
     * 异步出图配音：按分镜 JSON（用户可能编辑过）出图 + 整片配音，落库后停在 ASSETS_READY 等人确认合成。
     * 中间产物（分镜配图、配音 wav）留在 workDir，供下一步合成读取，合成完成后统一清理。
     */
    @Async
    public void generateAssetsAsync(String taskId, String orgId, String userId) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        File workDir = workDir(taskId);
        workDir.mkdirs();
        File audioFile = new File(workDir, "audio.wav");
        try {
            VideoTemplateConfig template = videoTemplateService.resolveConfig(task.getTemplateKey());
            // 从分镜 JSON 重建分镜（确认阶段可编辑旁白/画面描述，务必用最新内容出图配音）
            List<VideoSegment> segments = parseStoryboardSegments(task.getStoryboardJson());
            if (segments.isEmpty()) {
                throw new GenericException("暂无分镜，请先生成分镜脚本");
            }
            String scriptText = fullScript(segments);
            updateTask(task, STATUS_IMAGING, 40, null, null);
            // 逐镜出图：生成一张就上传一张并回写分镜 JSON，前端轮询即可逐张回显；
            // 出图放开为 3 并发（单镜 ~20-30s，串行太慢），上传/回写在单线程内按完成顺序串行，避免并发写库
            List<AgentModel> imageModels = agentModelService.resolveImageModels(orgId);
            if (imageModels.isEmpty()) {
                throw new GenericException("请先在「模型设置」中配置并启用图片模型");
            }
            AgentModel imageModel = preferModel(imageModels, task.getImageModelId());
            List<Integer> imageIndexes = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                if (StringUtils.isNotBlank(segments.get(i).image())) {
                    imageIndexes.add(i);
                }
            }
            List<VideoSegment> current = new ArrayList<>(segments);
            if (!imageIndexes.isEmpty()) {
                ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, imageIndexes.size()));
                try {
                    CompletionService<Integer> cs = new ExecutorCompletionService<>(pool);
                    for (int index : imageIndexes) {
                        VideoSegment seg = segments.get(index);
                        cs.submit(() -> {
                            File generated = generateOneImage(imageModel, seg.image(), template.imageStyle(),
                                    task.getStylePrompt(), task.getVideoType(),
                                    new File(workDir, "scene-" + index + ".png"), imageSizeForRatio(task.getRatio()));
                            return generated == null ? null : index;
                        });
                    }
                    for (int n = 0; n < imageIndexes.size(); n++) {
                        Future<Integer> f = cs.take();
                        Integer index = f.get();
                        if (index == null) {
                            continue; // 该镜生成失败回退纯色背景，不计入附件
                        }
                        VideoSegment seg = segments.get(index);
                        String attachmentId = saveImageAttachment(new File(workDir, "scene-" + index + ".png"),
                                task, index, orgId, userId);
                        current.set(index, new VideoSegment(seg.text(), seg.image(), attachmentId, seg.videoPrompt(),
                                seg.videoAttachmentId()));
                        task.setStoryboardJson(storyboardToJson(current));
                        task.setUpdateTime(System.currentTimeMillis());
                        aiVideoTaskMapper.updateById(task);
                    }
                } finally {
                    pool.shutdownNow();
                }
            }
            task.setScriptText(scriptText);
            updateTask(task, STATUS_TTS, 60, null, null);

            synthesizeAudio(scriptText, task.getVoice(), orgId, audioFile);
            // 片尾口播「关注主页·了解更多」也配音：合成时放在片尾卡，与成片结尾对齐
            synthesizeClosingAudio(template, task, orgId, new File(workDir, "closing.wav"));
            updateTask(task, STATUS_ASSETS_READY, 80, null, null);
        } catch (Exception e) {
            log.error("短视频画面配音生成失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
            deleteQuietly(audioFile);
            deleteDirQuietly(workDir);
        }
    }

    /**
     * 生成视频：把任务从 ASSETS_READY 推进到 FFmpeg 合成（异步）。合成中/已完成幂等返回，避免重复点击报错。
     */
    public VideoTaskResponse compose(String orgId, String id, String userId) {
        AiVideoTask task = requireTask(orgId, id);
        String status = task.getStatus();
        if (STATUS_SYNTH.equals(status) || STATUS_DONE.equals(status)) {
            return toResponse(task);
        }
        if (!STATUS_ASSETS_READY.equals(status) && !STATUS_VIDEOS_READY.equals(status)) {
            throw new GenericException("当前状态不可合成视频，请先确认画面与配音（或镜头视频）");
        }
        // 先同步置为合成中再异步合成：响应立即回显 SYNTH，前端隐藏按钮并推进到合成预览
        updateTask(task, STATUS_SYNTH, 95, null, null);
        composeAsync(id, orgId, userId);
        return toResponse(task);
    }

    /**
     * 异步合成：读分镜 JSON + workDir 里的分镜配图/配音/逐镜视频，FFmpeg 合成成片后存附件并清理中间产物。
     * 「AI 视频」模式用已确认的逐镜视频拼片；缺镜头或「静图成片」回退 Ken Burns 静图成片。
     */
    @Async
    public void composeAsync(String taskId, String orgId, String userId) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        File workDir = workDir(taskId);
        File audioFile = new File(workDir, "audio.wav");
        File videoFile = new File(workDir, "output.mp4");
        try {
            VideoTemplateConfig template = videoTemplateService.resolveConfig(task.getTemplateKey());
            List<VideoSegment> segments = parseStoryboardSegments(task.getStoryboardJson());
            if (segments.isEmpty()) {
                throw new GenericException("暂无分镜，无法合成");
            }
            if (!audioFile.exists()) {
                throw new GenericException("配音缺失，请先生成画面与配音");
            }
            int[] dims = dimsForRatio(task.getRatio());
            List<File> images = rebuildSceneImages(segments, workDir);
            List<File> shots = rebuildShotVideos(segments, workDir);
            File cover = resolveCoverFile(task, segments, images, workDir, orgId);
            // 未显式设置封面（或封面附件下载失败）时，回退到首镜配图做标题卡，
            // 避免「蓝黑底白字」纯色标题卡：用户没点「用首镜封面」也能得到首镜封面。
            if (cover == null) {
                cover = firstSceneImage(images);
            }
            File closingAudio = new File(workDir, "closing.wav");
            if (!closingAudio.exists()) {
                closingAudio = null;
            }
            boolean shotsComplete = MODE_VIDEO.equals(task.getMode()) && !shots.isEmpty()
                    && shots.stream().allMatch(f -> f != null && f.exists());
            if (shotsComplete) {
                synthesizeFromShots(task, segments, shots, audioFile, videoFile, template, dims, cover, closingAudio);
            } else {
                synthesizeKenBurns(task, segments, images, audioFile, videoFile, template, dims, cover, closingAudio);
            }

            String attachmentId = saveAttachment(videoFile, task, orgId, userId);
            updateTask(task, STATUS_DONE, 100, attachmentId, null);
            // 出片即释放：分镜配图与逐镜视频仅在生成中回显，成片后删除附件并清空附件ID
            releaseSceneAssets(task);
        } catch (Exception e) {
            log.error("短视频合成失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        } finally {
            deleteQuietly(audioFile);
            deleteQuietly(videoFile);
            deleteDirQuietly(workDir);
        }
    }

    /**
     * 重新生成配套文案（第六步「配套文案」按钮按需调用）：分镜生成阶段文案可能因额度/模型瞬时失败为空，
     * 这里按当前口播全文重试一次，落库并返回任务，前端拿到 copyJson 后打开弹窗。
     */
    public VideoTaskResponse regenerateCopy(String orgId, String id, String userId) {
        AiVideoTask task = requireTask(orgId, id);
        String scriptText = StringUtils.isNotBlank(task.getScriptText())
                ? task.getScriptText()
                : fullScript(parseStoryboardSegments(task.getStoryboardJson()));
        String copy = generateCompanionCopy(task.getTitle(), scriptText, orgId, userId);
        if (copy == null) {
            throw new GenericException("配套文案生成失败，请确认已配置对话模型且额度充足");
        }
        task.setCopyJson(copy);
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
        return toResponse(task);
    }

    /**
     * 生成镜头视频（第五步门控）：「AI 视频」模式从画面配音就绪推进到逐镜 Seedance 图生视频（异步）。
     * 未配置/未选择 Seedance 视频模型时直接报错（静图成片模式不进此步，直接合成）。
     */
    public VideoTaskResponse generateVideos(String orgId, String id, String userId) {
        AiVideoTask task = requireTask(orgId, id);
        if (!STATUS_ASSETS_READY.equals(task.getStatus())) {
            throw new GenericException("当前状态不可生成镜头视频，请先确认画面与配音");
        }
        AgentModel videoModel = resolveVideoModel(orgId, task.getVideoModelId());
        if (!isSeedance(videoModel)) {
            throw new GenericException("所选视频模型不支持图生视频，请改用「静图成片」或联系管理员配置豆包 Seedance 视频模型");
        }
        updateTask(task, STATUS_VIDEOING, 85, null, null);
        generateVideosAsync(id, orgId, userId);
        return toResponse(task);
    }

    /**
     * 异步逐镜出视频：按分镜 JSON（含用户可能替换过的配图）逐镜调 Seedance 图生视频，
     * 落盘 shot-{i}.mp4 并上传附件回填 videoPrompt/videoAttachmentId，全部处理完停在 VIDEOS_READY 等人确认。
     */
    @Async
    public void generateVideosAsync(String taskId, String orgId, String userId) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            return;
        }
        File workDir = workDir(taskId);
        try {
            AgentModel videoModel = resolveVideoModel(orgId, task.getVideoModelId());
            if (!isSeedance(videoModel)) {
                throw new GenericException("所选视频模型不支持图生视频");
            }
            List<VideoSegment> segments = parseStoryboardSegments(task.getStoryboardJson());
            if (segments.isEmpty()) {
                throw new GenericException("暂无分镜，无法生成镜头视频");
            }
            List<File> images = rebuildSceneImages(segments, workDir);
            List<VideoSegment> updated = new ArrayList<>(segments);
            List<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                VideoSegment seg = segments.get(i);
                File img = images.get(i);
                if (StringUtils.isNotBlank(seg.text()) && img != null && img.exists()) {
                    indexes.add(i);
                }
            }
            // 逐镜出视频放开为 3 并发（Seedance 单镜 1~3 分钟，串行 6 镜会到 20 分钟）；
            // 每生成一镜就上传一镜并回写分镜 JSON，上传/回写在单线程内按完成顺序串行，前端轮询逐镜回显
            if (!indexes.isEmpty()) {
                ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, indexes.size()));
                try {
                    CompletionService<Integer> cs = new ExecutorCompletionService<>(pool);
                    for (int index : indexes) {
                        VideoSegment seg = segments.get(index);
                        cs.submit(() -> {
                            File shot = new File(workDir, "shot-" + index + ".mp4");
                            String prompt = effectiveVideoPrompt(seg, task);
                            seedanceVideoProvider.generate(videoModel.getApiUrl(), videoModel.getApiKey(),
                                    videoModel.getModelName(), prompt, images.get(index), shot);
                            return index;
                        });
                    }
                    for (int n = 0; n < indexes.size(); n++) {
                        Integer index = cs.take().get();
                        VideoSegment seg = segments.get(index);
                        String prompt = effectiveVideoPrompt(seg, task);
                        String videoAttachmentId = saveVideoAttachment(new File(workDir, "shot-" + index + ".mp4"),
                                task, index, orgId, userId);
                        updated.set(index, new VideoSegment(seg.text(), seg.image(), seg.imageAttachmentId(), prompt,
                                videoAttachmentId));
                        task.setStoryboardJson(storyboardToJson(updated));
                        task.setUpdateTime(System.currentTimeMillis());
                        aiVideoTaskMapper.updateById(task);
                    }
                } finally {
                    pool.shutdownNow();
                }
            }
            updateTask(task, STATUS_VIDEOS_READY, 90, null, null);
        } catch (Exception e) {
            log.error("短视频逐镜视频生成失败 taskId={}, orgId={}", taskId, orgId, e);
            failTask(taskId, e.getMessage());
        }
    }

    /**
     * 重新生成单镜视频：用该镜当前视频提示词（可随请求一并更新）重新调 Seedance 出视频并替换附件，
     * 供分镜台在镜头视频阶段修正不满意的镜头；仅镜头视频就绪阶段可操作。
     */
    public VideoTaskResponse regenShotVideo(String orgId, String id, int index, VideoShotUpdateRequest request,
                                            String userId) {
        AiVideoTask task = requireTask(orgId, id);
        if (!STATUS_VIDEOS_READY.equals(task.getStatus())) {
            throw new GenericException("当前状态不可重新生成镜头视频，请先生成镜头视频");
        }
        List<VideoSegment> segments = new ArrayList<>(parseStoryboardSegments(task.getStoryboardJson()));
        if (index < 0 || index >= segments.size()) {
            throw new GenericException("分镜序号越界");
        }
        VideoSegment seg = segments.get(index);
        File workDir = workDir(id);
        workDir.mkdirs();
        File img = rebuildSceneImages(segments, workDir).get(index);
        if (img == null || !img.exists()) {
            throw new GenericException("该镜暂无配图，请先在画面配音阶段生成或更换画面");
        }
        String videoPrompt = request != null && StringUtils.isNotBlank(request.getVideoPrompt())
                ? request.getVideoPrompt().trim() : seg.videoPrompt();
        String prompt = StringUtils.isNotBlank(videoPrompt) ? videoPrompt : effectiveVideoPrompt(seg, task);
        try {
            AgentModel videoModel = resolveVideoModel(orgId, task.getVideoModelId());
            if (!isSeedance(videoModel)) {
                throw new GenericException("所选视频模型不支持图生视频");
            }
            File shot = new File(workDir, "shot-" + index + ".mp4");
            seedanceVideoProvider.generate(videoModel.getApiUrl(), videoModel.getApiKey(),
                    videoModel.getModelName(), prompt, img, shot);
            String videoAttachmentId = saveVideoAttachment(shot, task, index, orgId, userId);
            String oldVideoAttachmentId = seg.videoAttachmentId();
            segments.set(index, new VideoSegment(seg.text(), seg.image(), seg.imageAttachmentId(), prompt,
                    videoAttachmentId));
            task.setStoryboardJson(storyboardToJson(segments));
            task.setUpdateTime(System.currentTimeMillis());
            aiVideoTaskMapper.updateById(task);
            deleteAttachmentUnlessCover(oldVideoAttachmentId, task.getCoverAttachmentId());
            return toResponse(task);
        } catch (Exception e) {
            log.error("短视频单镜视频重生成失败 taskId={}, index={}, orgId={}", id, index, orgId, e);
            throw new GenericException(e.getMessage() == null ? "镜头视频生成失败，请重试" : e.getMessage());
        }
    }

    /**
     * 更换某镜画面（第四步）：用户上传本地图片，压缩（等比缩到最长边 2048、JPEG 质量 0.9，肉眼无损）后
     * 落库替换配图附件并回写 workDir 供合成使用；仅画面配音就绪阶段可操作。
     */
    public VideoTaskResponse uploadShotImage(String orgId, String id, int index, MultipartFile file, String userId) {
        AiVideoTask task = requireTask(orgId, id);
        if (!STATUS_ASSETS_READY.equals(task.getStatus())) {
            throw new GenericException("当前状态不可更换画面，仅画面配音就绪阶段可操作");
        }
        if (file == null || file.isEmpty()) {
            throw new GenericException("请选择要上传的图片");
        }
        List<VideoSegment> segments = new ArrayList<>(parseStoryboardSegments(task.getStoryboardJson()));
        if (index < 0 || index >= segments.size()) {
            throw new GenericException("分镜序号越界");
        }
        VideoSegment seg = segments.get(index);
        File workDir = workDir(id);
        workDir.mkdirs();
        try {
            File compressed = compressImage(file, workDir, index);
            String newAttachmentId = saveImageAttachment(compressed, task, index, orgId, userId);
            String oldAttachmentId = seg.imageAttachmentId();
            segments.set(index, new VideoSegment(seg.text(), seg.image(), newAttachmentId, seg.videoPrompt(),
                    seg.videoAttachmentId()));
            task.setStoryboardJson(storyboardToJson(segments));
            task.setUpdateTime(System.currentTimeMillis());
            aiVideoTaskMapper.updateById(task);
            deleteAttachmentUnlessCover(oldAttachmentId, task.getCoverAttachmentId());
            return toResponse(task);
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.error("短视频更换画面失败 taskId={}, index={}, orgId={}", id, index, orgId, e);
            throw new GenericException("图片处理失败，请换一张图片重试");
        }
    }

    /**
     * 更新单个分镜：分镜就绪可改旁白与画面描述；画面配音就绪只改画面描述；镜头视频就绪只改视频提示词。
     * 其余字段保留（配图/镜头视频附件ID），改旁白时重算口播全文。
     */
    public VideoTaskResponse updateShot(String orgId, String id, int index, VideoShotUpdateRequest request) {
        AiVideoTask task = requireTask(orgId, id);
        boolean scriptReady = STATUS_SCRIPT_READY.equals(task.getStatus());
        boolean assetsReady = STATUS_ASSETS_READY.equals(task.getStatus());
        boolean videosReady = STATUS_VIDEOS_READY.equals(task.getStatus());
        if (!scriptReady && !assetsReady && !videosReady) {
            throw new GenericException("仅分镜就绪、画面配音就绪或镜头视频就绪阶段可编辑");
        }
        List<VideoSegment> segments = new ArrayList<>(parseStoryboardSegments(task.getStoryboardJson()));
        if (index < 0 || index >= segments.size()) {
            throw new GenericException("分镜序号越界");
        }
        VideoSegment seg = segments.get(index);
        String text = scriptReady && request != null && StringUtils.isNotBlank(request.getText())
                ? request.getText().trim() : seg.text();
        boolean imageEditable = scriptReady || assetsReady;
        String image = imageEditable && request != null && StringUtils.isNotBlank(request.getImage())
                ? request.getImage().trim() : seg.image();
        String videoPrompt = videosReady && request != null && StringUtils.isNotBlank(request.getVideoPrompt())
                ? request.getVideoPrompt().trim() : seg.videoPrompt();
        segments.set(index, new VideoSegment(text, image, seg.imageAttachmentId(), videoPrompt, seg.videoAttachmentId()));
        task.setStoryboardJson(storyboardToJson(segments));
        if (scriptReady) {
            task.setScriptText(fullScript(segments));
        }
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
        return toResponse(task);
    }

    /**
     * 重绘单个分镜画面：用该镜画面描述（可在请求里一并更新）重新出图并回填附件，替换旧配图，
     * 供分镜台在画面配音阶段修正不满意的画面；仅改画面描述不触发整片重配音。
     */
    public VideoTaskResponse regenShotImage(String orgId, String id, int index, VideoShotUpdateRequest request, String userId) {
        AiVideoTask task = requireTask(orgId, id);
        if (!STATUS_SCRIPT_READY.equals(task.getStatus()) && !STATUS_ASSETS_READY.equals(task.getStatus())) {
            throw new GenericException("当前状态不可重绘画面，仅分镜就绪或画面配音就绪阶段可操作");
        }
        List<VideoSegment> segments = new ArrayList<>(parseStoryboardSegments(task.getStoryboardJson()));
        if (index < 0 || index >= segments.size()) {
            throw new GenericException("分镜序号越界");
        }
        VideoSegment seg = segments.get(index);
        String text = request != null && StringUtils.isNotBlank(request.getText())
                ? request.getText().trim() : seg.text();
        String image = request != null && StringUtils.isNotBlank(request.getImage())
                ? request.getImage().trim() : seg.image();
        if (StringUtils.isBlank(image)) {
            throw new GenericException("该镜暂无画面描述，请先编辑画面提示词");
        }

        List<AgentModel> imageModels = agentModelService.resolveImageModels(orgId);
        if (imageModels.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用图片模型");
        }
        VideoTemplateConfig template = videoTemplateService.resolveConfig(task.getTemplateKey());
        AgentModel model = preferModel(imageModels, task.getImageModelId());
        File workDir = workDir(id);
        workDir.mkdirs();
        File generated = generateOneImage(model, image, template.imageStyle(), task.getStylePrompt(), task.getVideoType(),
                new File(workDir, "scene-" + index + ".png"), imageSizeForRatio(task.getRatio()));
        if (generated == null) {
            throw new GenericException("画面生成失败，请重试");
        }

        String oldAttachmentId = seg.imageAttachmentId();
        String newAttachmentId = saveImageAttachment(generated, task, index, orgId, userId);
        segments.set(index, new VideoSegment(text, image, newAttachmentId, seg.videoPrompt(), seg.videoAttachmentId()));
        task.setStoryboardJson(storyboardToJson(segments));
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
        // 旧配图清理（封面若复用了旧图则保留，由分镜/封面统一清理）
        if (StringUtils.isNotBlank(oldAttachmentId) && !oldAttachmentId.equals(task.getCoverAttachmentId())) {
            try {
                attachmentService.delete(oldAttachmentId);
            } catch (Exception ignored) {
                // 附件可能已被清理，忽略
            }
        }
        return toResponse(task);
    }

    /**
     * 更新内置提示词（风格/角色/额外）：仅分镜就绪阶段可编辑，出图与脚本生成会消费这三者。
     */
    public VideoTaskResponse updatePrompts(String orgId, String id, VideoPromptUpdateRequest request) {
        AiVideoTask task = requireTask(orgId, id);
        if (!STATUS_SCRIPT_READY.equals(task.getStatus())) {
            throw new GenericException("仅分镜就绪阶段可编辑提示词，请确认后再生成画面与配音");
        }
        if (request != null) {
            if (request.getStylePrompt() != null) {
                task.setStylePrompt(StringUtils.trimToNull(request.getStylePrompt()));
            }
            if (request.getCharacterPrompt() != null) {
                task.setCharacterPrompt(StringUtils.trimToNull(request.getCharacterPrompt()));
            }
            if (request.getExtraPrompt() != null) {
                task.setExtraPrompt(StringUtils.trimToNull(request.getExtraPrompt()));
            }
        }
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
        return toResponse(task);
    }

    public List<VideoTaskResponse> list(String orgId) {
        return aiVideoTaskMapper.selectListByLambda(new LambdaQueryWrapper<AiVideoTask>()
                        .eq(AiVideoTask::getOrganizationId, orgId)
                        .orderByDesc(AiVideoTask::getCreateTime)).stream()
                .map(this::toResponse)
                .toList();
    }

    public VideoTaskResponse get(String orgId, String id) {
        return toResponse(requireTask(orgId, id));
    }

    public void delete(String orgId, String id) {
        AiVideoTask task = requireTask(orgId, id);
        if (StringUtils.isNotBlank(task.getAttachmentId())) {
            attachmentService.delete(task.getAttachmentId());
        }
        deleteStoryboardAssets(task);
        deleteCoverQuietly(task.getCoverAttachmentId(), task);
        deleteDirQuietly(workDir(id));
        aiVideoTaskMapper.deleteByPrimaryKey(id);
    }

    /** 按租户 + 任务ID 校验归属后返回任务，不存在/越权统一报「任务不存在」 */
    private AiVideoTask requireTask(String orgId, String id) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        return task;
    }

    /**
     * 更换封面：上传一张本地图片作为项目封面（附件系统），空则回退模板封面。
     */
    public VideoTaskResponse setCover(String orgId, String id, MultipartFile file, String userId) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new GenericException("请选择封面图片");
        }
        String oldCover = task.getCoverAttachmentId();
        String coverId = saveCoverAttachment(file, task, orgId, userId);
        task.setCoverAttachmentId(coverId);
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
        deleteCoverQuietly(oldCover, task);
        return toResponse(task);
    }

    /**
     * 用首镜封面：取第一个有配图的分镜画面作为封面（分镜配图仍被分镜台引用，仅记录引用）。
     */
    public VideoTaskResponse setFirstShotCover(String orgId, String id, String userId) {
        AiVideoTask task = aiVideoTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("任务不存在");
        }
        String firstShotId = firstShotAttachmentId(task);
        if (StringUtils.isBlank(firstShotId)) {
            throw new GenericException("暂无可用镜头画面，请先生成分镜配图");
        }
        String oldCover = task.getCoverAttachmentId();
        task.setCoverAttachmentId(firstShotId);
        task.setUpdateTime(System.currentTimeMillis());
        aiVideoTaskMapper.updateById(task);
        deleteCoverQuietly(oldCover, task);
        return toResponse(task);
    }

    /**
     * 查询当前配音模型支持的音色列表（前端下拉用）。
     * 跟随「模型设置」里 admin 配置的配音模型，换服务商后音色自动切换，无需改前端。
     * 多个配音模型时汇总所有服务商音色（按 value 去重），合成/试听阶段按模型列表逐个降级。
     */
    public List<TtsVoice> voices(String orgId) {
        Map<String, TtsVoice> byValue = new LinkedHashMap<>();
        for (AgentModel model : agentModelService.resolveTtsModels(orgId)) {
            // 只列出凭据齐全的配音模型的音色：豆包语音需三件套，未配齐时其系统音色会合成失败，
            // 直接不展示，避免用户在配音下拉里选到必然失败的音色。
            if (!agentModelService.hasTtsCredential(model)) {
                continue;
            }
            try {
                for (TtsVoice voice : ttsProviderFactory.get(model.getProvider()).voices()) {
                    byValue.putIfAbsent(voice.getValue(), voice);
                }
            } catch (Exception ignored) {
                // 该模型非 TTS provider 或异常，尝试下一个候选
            }
        }
        // 追加本租户已复刻成功的克隆音色（功能 11）：speakerId 即 custom_speaker_id，可直接作为 TTS speaker 合成
        for (AiCloneVoice clone : listSuccessCloneVoices(orgId)) {
            byValue.putIfAbsent(clone.getSpeakerId(), new TtsVoice(clone.getSpeakerId(), clone.getName(), null));
        }
        log.info("[drama] voices orgId={} resolvedModels={} voiceCount={}",
                orgId, agentModelService.resolveTtsModels(orgId).stream().map(AgentModel::getId).toList(),
                byValue.size());
        return new ArrayList<>(byValue.values());
    }

    /** 查本租户复刻成功、由阿里云产出的克隆音色（供短视频配音下拉） */
    private List<AiCloneVoice> listSuccessCloneVoices(String orgId) {
        return aiCloneVoiceMapper.selectListByLambda(new LambdaQueryWrapper<AiCloneVoice>()
                .eq(AiCloneVoice::getOrganizationId, orgId)
                .eq(AiCloneVoice::getProvider, "阿里云")
                .eq(AiCloneVoice::getStatus, "SUCCESS"));
    }

    /**
     * 风格页「模型」区块：图片模型取「模型设置」里已启用的豆包 Seedream（文生图），视频模型取豆包 Seedance（图生视频）。
     * 名称/描述/是否推荐均来自模型设置，前端不写死目录；未配置的视频模型为空时前端展示「暂未开放」。
     */
    public VideoModelsResponse models(String orgId) {
        List<VideoModelOption> imageModels = new ArrayList<>();
        for (AgentModel model : agentModelService.resolveImageModels(orgId)) {
            imageModels.add(toModelOption(model));
        }
        List<VideoModelOption> videoModels = new ArrayList<>();
        for (AgentModel model : agentModelService.resolveVideoModels(orgId)) {
            videoModels.add(toModelOption(model));
        }
        VideoModelsResponse response = new VideoModelsResponse();
        response.setImageModels(imageModels);
        response.setVideoModels(videoModels);
        return response;
    }

    private VideoModelOption toModelOption(AgentModel model) {
        VideoModelOption option = new VideoModelOption();
        option.setId(StringUtils.defaultIfBlank(model.getId(), ""));
        option.setLabel(StringUtils.defaultIfBlank(model.getDisplayName(), model.getModelName()));
        option.setDescription(StringUtils.defaultString(model.getDescription()));
        option.setRecommended(Boolean.TRUE.equals(model.getRecommended()));
        return option;
    }

    /**
     * 音色试听：用样例文案合成约 5 秒音频，返回 base64（WAV）供前端解码播放。
     * 与正式配音同一条降级链，保证试听与实际成片音色一致。
     */
    public VideoVoicePreviewResponse previewVoice(String orgId, VideoVoicePreviewRequest request) {
        return synthesizeVoicePreview(orgId, request.getVoice(), PREVIEW_TEXT);
    }

    /**
     * 试听单个分镜的口播台词：用任务所选音色合成该镜台词，返回 base64 WAV，
     * 供分镜台「画面与配音」阶段逐镜试听，与实际成片音色一致。
     */
    public VideoVoicePreviewResponse previewShotVoice(String orgId, String id, int index) {
        AiVideoTask task = requireTask(orgId, id);
        // 试听属于画面与配音阶段：分镜确认阶段（第三步）尚未配音，不提供试听
        if (!STATUS_ASSETS_READY.equals(task.getStatus())) {
            throw new GenericException("仅画面配音就绪阶段可试听，请先确认分镜并生成画面与配音");
        }
        List<VideoSegment> segments = parseStoryboardSegments(task.getStoryboardJson());
        if (index < 0 || index >= segments.size()) {
            throw new GenericException("分镜序号越界");
        }
        String text = segments.get(index).text();
        if (StringUtils.isBlank(text)) {
            throw new GenericException("该镜暂无台词");
        }
        return synthesizeVoicePreview(orgId, task.getVoice(), text);
    }

    /** 按音色类型选配音模型：克隆音色（阿里 voice_id）只能由阿里云合成，系统音色走通用配音模型降级链 */
    private List<AgentModel> resolveTtsModelsForVoice(String orgId, String voice) {
        String cloneModel = agentModelService.resolveVoiceCloneModel(orgId);
        boolean cloneVoice = voice != null && voice.startsWith(cloneModel + "-");
        return cloneVoice ? agentModelService.resolveAliyunTtsModels(orgId) : agentModelService.resolveTtsModels(orgId);
    }

    /** 用给定音色合成任意文案（约样例或某镜台词），返回 base64 WAV；逐模型降级 */
    private VideoVoicePreviewResponse synthesizeVoicePreview(String orgId, String voice, String text) {
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
                byte[] audio = tts.synthesize(text, voice, config);
                VideoVoicePreviewResponse response = new VideoVoicePreviewResponse();
                response.setAudio(Base64.getEncoder().encodeToString(audio));
                return response;
            } catch (Exception e) {
                lastError = e;
                log.warn("短视频配音合成失败，降级尝试下一个，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("短视频配音合成全部失败，候选数={}", ttsModels.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "试听生成失败，请确认已配置配音模型" : lastError.getMessage());
    }

    /**
     * 创作摘要里的 AI 扩写：mode=theme 生成更具体的主题，mode=script 扩写为完整口播文案，
     * mode=style 根据口播内容生成画面风格提示词（风格页「AI 生成风格提示词」）。
     * 返回 title（供项目名称自动填充）+ content（供输入框填充）。
     */
    public VideoExpandResponse expand(VideoExpandRequest request, String orgId, String userId) {
        String topic = request.getTopic().trim();
        if (StringUtils.isBlank(topic)) {
            throw new GenericException("请先输入主题或文案");
        }
        String mode = StringUtils.defaultIfBlank(request.getMode(), "theme");
        if ("prompts".equalsIgnoreCase(mode)) {
            return generatePrompts(topic, orgId, userId);
        }
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
                llmRequest.setMessages(buildExpandMessages(topic, mode));

                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_VIDEO, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return parseExpand(sb.toString(), topic);
            } catch (Exception e) {
                lastError = e;
                log.warn("短视频主题/文案扩写失败，尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("短视频主题/文案扩写全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /**
     * 一次生成 风格/角色/额外 三条内置提示词（分镜台「换一批」与脚本就绪时预生成共用），
     * 复用主题/文案扩写的模型降级链与额度扣减口径。
     */
    private VideoExpandResponse generatePrompts(String topic, String orgId, String userId) {
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
                llmRequest.setMessages(buildExpandMessages(topic, "prompts"));

                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_VIDEO, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return parsePromptExpand(sb.toString());
            } catch (Exception e) {
                lastError = e;
                log.warn("短视频内置提示词生成失败，尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("短视频内置提示词生成全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildExpandMessages(String topic, String mode) {
        String system = EXPAND_THEME_SYSTEM_PROMPT;
        String instruction = "扩写主题";
        if ("script".equalsIgnoreCase(mode)) {
            system = EXPAND_SCRIPT_SYSTEM_PROMPT;
            instruction = "扩写文案";
        } else if ("style".equalsIgnoreCase(mode)) {
            system = STYLE_PROMPT_SYSTEM_PROMPT;
            instruction = "生成画面风格提示词";
        } else if ("prompts".equalsIgnoreCase(mode)) {
            system = PROMPTS_SYSTEM_PROMPT;
            instruction = "生成风格、角色、额外三条提示词";
        }
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", system));
        messages.add(new LlmMessage("user", "输入：" + topic + "\n请" + instruction + "。"));
        return messages;
    }

    /** 解析内置提示词 JSON（mode=prompts），解析失败时返回空提示词由前端回退模板/默认文案 */
    private VideoExpandResponse parsePromptExpand(String text) {
        String json = extractJson(text);
        if (json != null) {
            try {
                JsonNode root = MAPPER.readTree(json);
                VideoExpandResponse response = new VideoExpandResponse();
                response.setStylePrompt(normalizePromptValue(root.path("stylePrompt").asText(null)));
                response.setCharacterPrompt(normalizePromptValue(root.path("characterPrompt").asText(null)));
                response.setExtraPrompt(normalizePromptValue(root.path("extraPrompt").asText(null)));
                return response;
            } catch (Exception e) {
                log.warn("短视频内置提示词 JSON 解析失败，回退空提示词", e);
            }
        }
        return new VideoExpandResponse();
    }

    /** 内置提示词归一化：LLM 返回「无」等占位值时视为空，前端回退模板/默认文案而非展示占位字 */
    private static String normalizePromptValue(String value) {
        String s = StringUtils.defaultIfBlank(value, "").trim();
        if ("无".equals(s) || "无。".equals(s) || "无额外要求".equals(s) || "无额外要求。".equals(s)) {
            return "";
        }
        return s;
    }

    /** 解析扩写结果 JSON，title/content 缺失时回退输入原文 */
    private VideoExpandResponse parseExpand(String text, String fallback) {
        String json = extractJson(text);
        if (json != null) {
            try {
                JsonNode root = MAPPER.readTree(json);
                String title = root.path("title").asText(null);
                String content = root.path("content").asText(null);
                if (StringUtils.isNotBlank(content)) {
                    VideoExpandResponse response = new VideoExpandResponse();
                    response.setTitle(StringUtils.defaultIfBlank(title, fallback));
                    response.setContent(content.trim());
                    return response;
                }
            } catch (Exception e) {
                log.warn("短视频扩写 JSON 解析失败，回退原文", e);
            }
        }
        VideoExpandResponse response = new VideoExpandResponse();
        response.setTitle(fallback);
        response.setContent(fallback);
        return response;
    }

    // ==================== 私有：脚本生成 ====================

    /**
     * 组装口播脚本：用户已提供完整口播文案时直接采用（只补配图画面描述，不重写），否则走 AI 全量生成。
     * 避免「做成视频」跳转带入的既有文案被模型重写后丢掉钩子与卖点。
     */
    private VideoScript buildScript(String title, String videoType, VideoTemplateConfig template, String stylePrompt,
                                    String characterPrompt, String extraText, String orgId, String userId) {
        // 结构化简报（口播文案 + 画面素材 + 字幕）优先：直接采用口播文案，画面素材作为画面方向提示
        StructuredBrief brief = parseStructuredBrief(extraText);
        List<String> provided = brief != null
                ? splitScriptText(brief.narration())
                : splitProvidedScript(extraText);
        if (!provided.isEmpty()) {
            AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId, userId);
            if (isBlocked(quota.getStatus())) {
                throw new GenericException(blockedMessage(quota.getStatus()));
            }
            String imageHint = brief != null && !brief.imageHints().isEmpty()
                    ? String.join("\n", brief.imageHints()) : null;
            List<String> imageDescs = generateImageDescriptions(title, videoType, provided, imageHint, orgId, userId);
            if (imageDescs.size() == provided.size()) {
                List<VideoSegment> segments = new ArrayList<>(provided.size());
                for (int i = 0; i < provided.size(); i++) {
                    segments.add(new VideoSegment(provided.get(i), imageDescs.get(i), "", "", ""));
                }
                return new VideoScript(title, segments);
            }
            // 画面描述生成失败时回退全量生成，保证仍有配图
        }
        return generateScript(title, videoType, template, stylePrompt, characterPrompt, extraText, orgId, userId);
    }

    /**
     * 解析结构化创作简报：含「口播文案」与「画面素材」段落才视为结构化，返回 null 表示非结构化。
     * 支持 markdown 标题或直接以关键字开头的段落；口播文案取正文，画面素材/字幕去掉序号或项目符号。
     */
    private StructuredBrief parseStructuredBrief(String extraText) {
        if (StringUtils.isBlank(extraText)) {
            return null;
        }
        String text = extraText.trim();
        if (!text.contains("口播") || !text.contains("画面")) {
            return null;
        }
        StringBuilder narration = new StringBuilder();
        List<String> imageHints = new ArrayList<>();
        List<String> captionLines = new ArrayList<>();
        String section = "";
        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String keyword = line.replaceAll("^#+\\s*", "").trim();
            if (keyword.startsWith("口播文案") || keyword.startsWith("口播内容")) {
                section = "narration";
                continue;
            }
            if (keyword.startsWith("画面素材") || keyword.startsWith("视频画面") || keyword.startsWith("画面")) {
                section = "image";
                continue;
            }
            if (keyword.startsWith("字幕") || keyword.startsWith("配图文案") || keyword.startsWith("封面")) {
                section = "caption";
                continue;
            }
            switch (section) {
                case "narration" -> narration.append(line);
                case "image" -> {
                    String hint = line.replaceFirst("^\\s*(\\d+[.、．]|[-•·])\\s*", "").trim();
                    if (StringUtils.isNotBlank(hint)) {
                        imageHints.add(hint);
                    }
                }
                case "caption" -> {
                    String cap = line.replaceFirst("^\\s*([-•·✅❌]|\\d+[.、．])\\s*", "").trim();
                    if (StringUtils.isNotBlank(cap)) {
                        captionLines.add(cap);
                    }
                }
                default -> { /* 无标题行时忽略 */ }
            }
        }
        if (StringUtils.isBlank(narration.toString())) {
            return null;
        }
        return new StructuredBrief(narration.toString(), imageHints, captionLines);
    }

    /** 把一段口播文案按句末标点拆成分镜（过多时按数量均摊合并，控制配图张数） */
    private List<String> splitScriptText(String scriptText) {
        if (StringUtils.isBlank(scriptText)) {
            return List.of();
        }
        String[] sentences = scriptText.trim().split("(?<=[。！？；])");
        List<String> parts = new ArrayList<>();
        for (String s : sentences) {
            String seg = s.trim();
            if (StringUtils.isNotBlank(seg)) {
                parts.add(seg);
            }
        }
        return parts.isEmpty() ? List.of() : mergeSegments(parts, 10);
    }

    /** 把补充素材当作完整口播文案拆分：足够长且含句末标点才视为现成脚本，否则返回空（走 AI 生成） */
    private List<String> splitProvidedScript(String extraText) {
        if (StringUtils.isBlank(extraText)) {
            return List.of();
        }
        String t = extraText.trim();
        if (t.length() < 40 || !t.matches(".*[。！？；].*")) {
            return List.of();
        }
        return splitScriptText(t);
    }

    /** 分镜过多时按数量均摊合并，控制配图张数与节奏 */
    private List<String> mergeSegments(List<String> parts, int max) {
        if (parts.size() <= max) {
            return parts;
        }
        List<String> merged = new ArrayList<>(max);
        int n = parts.size();
        for (int i = 0; i < max; i++) {
            int start = i * n / max;
            int end = (i + 1) * n / max;
            merged.add(String.join("", parts.subList(start, Math.min(end, n))));
        }
        return merged;
    }

    private VideoScript generateScript(String title, String videoType, VideoTemplateConfig template, String stylePrompt,
                                       String characterPrompt, String extraText, String orgId, String userId) {
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
                llmRequest.setMessages(buildScriptMessages(title, videoType, template.scriptStyle(), stylePrompt, characterPrompt, extraText));

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

    private List<LlmMessage> buildScriptMessages(String title, String videoType, String scriptStyle, String stylePrompt,
                                                 String characterPrompt, String extraText) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", scriptSystemPrompt(videoType)));
        StringBuilder sb = new StringBuilder();
        sb.append("主题：").append(title).append('\n');
        if (StringUtils.isNotBlank(scriptStyle)) {
            sb.append("口播语气：").append(scriptStyle.trim()).append('\n');
        }
        if (StringUtils.isNotBlank(stylePrompt)) {
            sb.append("画面风格：").append(stylePrompt.trim()).append('\n');
        }
        if (StringUtils.isNotBlank(characterPrompt)) {
            sb.append("角色设定：").append(characterPrompt.trim()).append('\n');
        }
        if (StringUtils.isNotBlank(extraText)) {
            sb.append("卖点/补充素材：").append(extraText.trim()).append('\n');
        }
        sb.append("请生成口播脚本。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /**
     * 解析模型返回 JSON 的分镜字段。模型流式输出偶尔在末尾被截断（少一个 } 或 ]），
     * 先直接解析，失败再补齐缺失括号后解析；仍失败则抛出，交由上层降级重试下一个候选模型，
     * 避免把半截 JSON 当成单条分镜回显。仅当模型返回纯文本口播（无 JSON）时才回退整段原文。
     */
    private VideoScript parseVideoScript(String text, String fallbackTitle) {
        String json = extractJson(text);
        if (json == null) {
            return new VideoScript(fallbackTitle, List.of(new VideoSegment(text.trim(), "", "", "", "")));
        }
        JsonNode root = tryParseJson(json);
        if (root == null) {
            root = tryParseJson(repairTruncatedJson(json));
        }
        if (root == null) {
            log.warn("短视频分镜 JSON 解析失败，交由上层降级重试下一个候选模型，json 前缀={}", StringUtils.left(json, 120));
            throw new IllegalStateException("AI 返回的分镜脚本不完整，请稍后重试");
        }
        String title = StringUtils.defaultIfBlank(root.path("title").asText(null), fallbackTitle);
        JsonNode segs = root.path("segments");
        if (segs.isArray() && !segs.isEmpty()) {
            List<VideoSegment> list = new ArrayList<>();
            for (JsonNode seg : segs) {
                String segText = seg.path("text").asText(null);
                if (StringUtils.isNotBlank(segText)) {
                    list.add(new VideoSegment(segText.trim(),
                            StringUtils.defaultString(seg.path("image").asText(null)).trim(), "", "", ""));
                }
            }
            if (!list.isEmpty()) {
                return new VideoScript(title, list);
            }
        }
        // 兼容旧格式：只有 script 字段
        String script = root.path("script").asText(null);
        if (StringUtils.isNotBlank(script)) {
            return new VideoScript(title, List.of(new VideoSegment(script.trim(), "", "", "", "")));
        }
        log.warn("短视频分镜 JSON 缺少 segments/script 字段，交由上层降级重试下一个候选模型，json 前缀={}", StringUtils.left(json, 120));
        throw new IllegalStateException("AI 返回的分镜脚本格式不正确，请稍后重试");
    }

    /** 尝试解析 JSON，失败返回 null（不抛异常，交由调用方决定降级或补齐后重试） */
    private JsonNode tryParseJson(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 补齐被截断 JSON 缺失的右括号。模型流式输出在末尾被截断时，段数组或外层对象常少一个 ] 或 }；
     * 按括号配平规则从内到外补全（忽略字符串内部字符与转义），补后交回 tryParseJson 重新解析。
     */
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
    private String fullScript(List<VideoSegment> segments) {
        return segments.stream().map(VideoSegment::text).collect(java.util.stream.Collectors.joining());
    }

    /** 为已定稿的口播分镜补画面描述：一次对话调用返回与分镜数一致的描述数组；失败返回空（回退全量生成） */
    private List<String> generateImageDescriptions(String title, String videoType, List<String> segments,
                                                   String imageHint, String orgId, String userId) {
        if (segments.isEmpty()) {
            return List.of();
        }
        try {
            List<AgentModel> models = agentModelService.resolveChatModels(orgId);
            for (AgentModel model : models) {
                try {
                    aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                    LlmProvider provider = llmProviderFactory.get(model.getProvider());
                    LlmChatRequest req = new LlmChatRequest();
                    req.setModel(model.getModelName());
                    req.setBaseUrl(model.getApiUrl());
                    req.setApiKey(model.getApiKey());
                    agentModelService.applyModelParams(req, model);
                    req.setMessages(buildImageDescMessages(title, videoType, segments, imageHint));

                    StringBuilder sb = new StringBuilder();
                    LlmUsage usage = provider.chatStream(req, sb::append);
                    aiQuotaService.record(orgId, AiQuotaConstant.AI_VIDEO, model.getModelName(),
                            usage.getInputTokens(), usage.getOutputTokens(), userId);
                    List<String> descs = parseImageDescriptions(sb.toString(), segments.size());
                    if (descs.size() == segments.size()) {
                        return descs;
                    }
                } catch (Exception e) {
                    log.warn("短视频画面描述生成失败，尝试下一个候选，provider={}, model={}",
                            model.getProvider(), model.getModelName(), e);
                }
            }
        } catch (Exception e) {
            log.warn("短视频画面描述生成异常，跳过", e);
        }
        return List.of();
    }

    private List<LlmMessage> buildImageDescMessages(String title, String videoType, List<String> segments,
                                                    String imageHint) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", imageDescSystemPrompt(videoType)));
        StringBuilder sb = new StringBuilder();
        sb.append("视频主题：").append(StringUtils.defaultIfBlank(title, "").trim()).append('\n');
        sb.append("口播分镜（共 ").append(segments.size()).append(" 段）：\n");
        for (int i = 0; i < segments.size(); i++) {
            sb.append(i + 1).append(". ").append(segments.get(i).trim()).append('\n');
        }
        if (StringUtils.isNotBlank(imageHint)) {
            sb.append("用户指定的画面方向（尽量遵循，按顺序对应上面各段）：\n").append(imageHint).append('\n');
        }
        sb.append("请为每一段各生成一句画面描述。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 解析画面描述 JSON 数组；仅当为数组且条数一致时才采用 */
    private List<String> parseImageDescriptions(String text, int expected) {
        String json = extractJson(text);
        if (json == null) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }
            List<String> descs = new ArrayList<>();
            for (JsonNode node : root) {
                String d = node.asText(null);
                if (StringUtils.isNotBlank(d)) {
                    descs.add(d.trim());
                }
            }
            return descs.size() == expected ? descs : List.of();
        } catch (Exception e) {
            log.warn("短视频画面描述 JSON 解析失败，跳过", e);
            return List.of();
        }
    }

    /** 按所选模型ID把对应模型排到候选首位（未匹配则保留推荐），仍保留降级链 */
    private AgentModel preferModel(List<AgentModel> models, String modelId) {
        if (StringUtils.isBlank(modelId) || models.isEmpty()) {
            return models.get(0);
        }
        for (AgentModel m : models) {
            if (modelId.equals(m.getId())) {
                return m;
            }
        }
        return models.get(0);
    }

    /** 单张分镜配图：调 Seedream 文生图并落盘；失败返回 null（回退纯色背景） */
    private File generateOneImage(AgentModel model, String imagePrompt, String imageStyle, String stylePrompt,
                                  String videoType, File target, String size) {
        try {
            LlmProvider provider = llmProviderFactory.get(model.getProvider());
            LlmImageRequest request = new LlmImageRequest();
            request.setModel(model.getModelName());
            request.setBaseUrl(StringUtils.defaultIfBlank(model.getApiUrl(), "https://ark.cn-beijing.volces.com/api/v3"));
            request.setApiKey(model.getApiKey());
            request.setSize(size);
            request.setPrompt(imagePrompt + StringUtils.defaultString(imageStyle)
                    + StringUtils.defaultString(stylePrompt) + imageConstraintSuffix(videoType));
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
    private List<VideoSynthesizer.Scene> buildScenes(List<VideoSegment> segments, List<File> images) {
        List<VideoSynthesizer.Scene> scenes = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            scenes.add(new VideoSynthesizer.Scene(segments.get(i).text(), images.get(i)));
        }
        return scenes;
    }

    /** Ken Burns 静图成片（原合成路径，无 Seedance 视频模型或成片方式为「静图成片」时走这里） */
    private void synthesizeKenBurns(AiVideoTask task, List<VideoSegment> segments, List<File> images,
                                    File audioFile, File videoFile, VideoTemplateConfig template, int[] dims,
                                    File cover, File closingAudio)
            throws Exception {
        List<VideoSynthesizer.Scene> scenes = buildScenes(segments, images);
        videoSynthesizer.synthesize(audioFile, videoFile, task.getTitle(), scenes, template, dims[0], dims[1],
                cover, closingAudio);
    }

    /** 取合成阶段所用视频模型：未配置或未选择返回 null（走 Ken Burns 静图成片） */
    private AgentModel resolveVideoModel(String orgId, String videoModelId) {
        List<AgentModel> models = agentModelService.resolveVideoModels(orgId);
        if (models.isEmpty()) {
            return null;
        }
        return preferModel(models, videoModelId);
    }

    /** 仅豆包 Seedance 系列走 Ark 图生视频；MiniMax 等暂不支持，回退 Ken Burns */
    private boolean isSeedance(AgentModel model) {
        return model != null && "豆包".equals(model.getProvider())
                && model.getModelName() != null
                && model.getModelName().toLowerCase().contains("seedance");
    }

    /**
     * Seedance 图生视频拼片：按镜调方舟 content-generation 出视频，全部成功则用视频拼片合成，
     * 任一镜失败（含无配图）整体回退 Ken Burns，保证「AI 视频」不成片也不至于半成品。
     */
    /** 用已生成的逐镜视频拼片合成（镜头视频就绪后），逐镜视频在生成阶段已存附件与 workDir */
    private void synthesizeFromShots(AiVideoTask task, List<VideoSegment> segments, List<File> shots,
                                     File audioFile, File videoFile, VideoTemplateConfig template, int[] dims,
                                     File cover, File closingAudio)
            throws Exception {
        List<VideoSynthesizer.ClipScene> clips = new ArrayList<>();
        for (int i = 0; i < segments.size() && i < shots.size(); i++) {
            File shot = shots.get(i);
            if (shot != null && shot.exists()) {
                clips.add(new VideoSynthesizer.ClipScene(segments.get(i).text(), shot));
            }
        }
        videoSynthesizer.synthesizeFromClips(audioFile, videoFile, task.getTitle(), clips, template, dims[0], dims[1],
                cover, closingAudio);
    }

    /** 合成阶段按 workDir 里的 shot-{index}.mp4 重建逐镜视频文件列表（缺失/失败为 null，回退 Ken Burns） */
    private List<File> rebuildShotVideos(List<VideoSegment> segments, File workDir) {
        List<File> shots = new ArrayList<>(Collections.nCopies(segments.size(), null));
        for (int i = 0; i < segments.size(); i++) {
            File shot = new File(workDir, "shot-" + i + ".mp4");
            shots.set(i, shot.exists() ? shot : null);
        }
        return shots;
    }

    /** 图生视频提示词：优先用已编辑的视频提示词，否则用画面描述 + 风格 + 按成片类型的画面约束 */
    private String effectiveVideoPrompt(VideoSegment seg, AiVideoTask task) {
        String base = StringUtils.isNotBlank(seg.videoPrompt()) ? seg.videoPrompt() : seg.image();
        return base + StringUtils.defaultString(task.getStylePrompt()) + imageConstraintSuffix(task.getVideoType());
    }

    // ==================== 私有：TTS / 合成 / 附件 ====================

    private void synthesizeAudio(String script, String voice, String orgId, File audioFile) throws Exception {
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

    /** 片尾口播配音：模板未配片尾文案或合成失败时静默跳过，片尾卡保持静音不影响成片 */
    private void synthesizeClosingAudio(VideoTemplateConfig template, AiVideoTask task, String orgId, File closingFile) {
        String closingText = StringUtils.trimToNull(template.closingText());
        if (closingText == null) {
            return;
        }
        try {
            // 片尾文案里的「·」是视觉分隔符，TTS 会把它读成「点」（如「关注主页·了解更多」被读成「点点了解更多」），
            // 配音前替换成逗号停顿；片尾卡的视觉文字仍保留原文案，不受影响。
            synthesizeAudio(closingText.replaceAll("\\s*[·・•]\\s*", "，"), task.getVoice(), orgId, closingFile);
        } catch (Exception e) {
            log.warn("短视频片尾口播配音失败，片尾卡保持静音，closingText={}", closingText, e);
            deleteQuietly(closingFile);
        }
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

    /** 上传逐镜视频文件并落附件系统，返回附件ID（供镜头视频阶段回显与合成） */
    private String saveVideoAttachment(File shot, AiVideoTask task, int index, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String fileName = "shot-" + (index + 1) + ".mp4";
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, task.getId(), attachmentId),
                StorageType.LOCAL.name(),
                fileName);
        fileCommonService.upload(shot, request);

        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setName(fileName);
        attachment.setType("mp4");
        attachment.setSize(shot.length());
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

    /**
     * 压缩用户上传的本地图片（不牺牲肉眼画质）：等比缩到最长边 2048 内，无透明通道写 JPEG(质量 0.9)、
     * 有透明通道写 PNG，落盘 workDir/scene-{index}，供后续合成与回显。
     */
    private File compressImage(MultipartFile file, File workDir, int index) throws Exception {
        BufferedImage src = ImageIO.read(file.getInputStream());
        if (src == null) {
            throw new GenericException("无法识别的图片格式，请上传 JPG/PNG 等常见图片");
        }
        boolean hasAlpha = src.getColorModel().hasAlpha();
        int maxDim = 2048;
        int w = src.getWidth();
        int h = src.getHeight();
        int longest = Math.max(w, h);
        BufferedImage out = src;
        if (longest > maxDim) {
            double scale = (double) maxDim / longest;
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(nw, nh,
                    hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(scaled, 0, 0, null);
            g.dispose();
            out = resized;
        }
        File target;
        if (hasAlpha) {
            target = new File(workDir, "scene-" + index + ".png");
            ImageIO.write(out, "png", target);
        } else {
            target = new File(workDir, "scene-" + index + ".jpg");
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(target)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.9f);
                writer.write(null, new IIOImage(out, null, null), param);
            } finally {
                writer.dispose();
            }
        }
        return target;
    }

    // ==================== 私有：分镜落库与配图附件 ====================

    /** 分镜序列化：text/image/imageAttachmentId/videoPrompt/videoAttachmentId，附件ID与视频提示词在对应阶段回填，失败为空串 */
    private String storyboardToJson(List<VideoSegment> segments) {
        List<Map<String, String>> items = new ArrayList<>(segments.size());
        for (VideoSegment seg : segments) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("text", seg.text());
            item.put("image", seg.image());
            item.put("imageAttachmentId", StringUtils.defaultString(seg.imageAttachmentId()));
            item.put("videoPrompt", StringUtils.defaultString(seg.videoPrompt()));
            item.put("videoAttachmentId", StringUtils.defaultString(seg.videoAttachmentId()));
            items.add(item);
        }
        try {
            return MAPPER.writeValueAsString(items);
        } catch (Exception e) {
            log.warn("短视频分镜序列化失败，回退空数组", e);
            return "[]";
        }
    }

    private String saveImageAttachment(File image, AiVideoTask task, int index, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String ext = StringUtils.substringAfterLast(image.getName(), ".").toLowerCase();
        if (!ext.matches("[a-z0-9]{1,5}")) {
            ext = "png";
        }
        String fileName = "scene-" + (index + 1) + "." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, task.getId(), attachmentId),
                StorageType.LOCAL.name(),
                fileName);
        fileCommonService.upload(image, request);

        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setName(fileName);
        attachment.setType(ext);
        attachment.setSize(image.length());
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

    /** 上传封面图片并落附件系统，返回附件ID */
    private String saveCoverAttachment(MultipartFile cover, AiVideoTask task, String orgId, String userId) {
        String attachmentId = IDGenerator.nextStr();
        String original = StringUtils.defaultIfBlank(cover.getOriginalFilename(), "cover.jpg");
        String ext = StringUtils.substringAfterLast(original, ".").toLowerCase();
        if (!ext.matches("[a-z0-9]{1,5}")) {
            ext = "jpg";
        }
        String fileName = "cover." + ext;
        FileRequest request = new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(orgId, task.getId(), attachmentId),
                StorageType.LOCAL.name(),
                fileName);
        fileCommonService.upload(cover, request);

        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setName(fileName);
        attachment.setType(ext);
        attachment.setSize(cover.getSize());
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

    /** 取第一个有配图的分镜附件ID（用首镜封面用），无则返回 null */
    private String firstShotAttachmentId(AiVideoTask task) {
        try {
            JsonNode arr = MAPPER.readTree(StringUtils.defaultIfBlank(task.getStoryboardJson(), "[]"));
            if (!arr.isArray()) {
                return null;
            }
            for (JsonNode item : arr) {
                String aid = item.path("imageAttachmentId").asText(null);
                if (StringUtils.isNotBlank(aid)) {
                    return aid;
                }
            }
        } catch (Exception e) {
            log.warn("解析首镜附件失败 taskId={}", task.getId(), e);
        }
        return null;
    }

    /** 清理封面附件：若封面仍被分镜配图引用则保留（由分镜统一清理），否则删除 */
    private void deleteCoverQuietly(String coverId, AiVideoTask task) {
        if (StringUtils.isBlank(coverId)) {
            return;
        }
        try {
            JsonNode arr = MAPPER.readTree(StringUtils.defaultIfBlank(task.getStoryboardJson(), "[]"));
            if (arr.isArray()) {
                for (JsonNode item : arr) {
                    if (coverId.equals(item.path("imageAttachmentId").asText(null))) {
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
            // 解析失败按可删除处理
        }
        try {
            attachmentService.delete(coverId);
        } catch (Exception ignored) {
            // 附件可能已被清理，忽略
        }
    }

    /** 删除任务时顺带清理分镜配图与逐镜视频附件，避免遗留孤儿文件 */
    private void deleteStoryboardAssets(AiVideoTask task) {
        try {
            JsonNode arr = MAPPER.readTree(StringUtils.defaultIfBlank(task.getStoryboardJson(), "[]"));
            if (!arr.isArray()) {
                return;
            }
            for (JsonNode item : arr) {
                deleteAttachmentUnlessCover(item.path("imageAttachmentId").asText(null), task.getCoverAttachmentId());
                deleteAttachmentUnlessCover(item.path("videoAttachmentId").asText(null), task.getCoverAttachmentId());
            }
        } catch (Exception e) {
            log.warn("清理分镜配图/逐镜视频附件失败 taskId={}", task.getId(), e);
        }
    }

    /** 删除单个附件（封面复用时保留），失败忽略 */
    private void deleteAttachmentUnlessCover(String attachmentId, String coverAttachmentId) {
        if (StringUtils.isBlank(attachmentId) || attachmentId.equals(coverAttachmentId)) {
            return;
        }
        try {
            attachmentService.delete(attachmentId);
        } catch (Exception ignored) {
            // 附件可能已被清理，忽略
        }
    }

    /**
     * 出片后释放分镜配图与逐镜视频：删除附件，并把分镜 JSON 里的 imageAttachmentId/videoAttachmentId 清空，
     * 保留口播文字、画面描述与视频提示词，成片后不再回显中间素材。
     */
    private void releaseSceneAssets(AiVideoTask task) {
        deleteStoryboardAssets(task);
        try {
            JsonNode arr = MAPPER.readTree(StringUtils.defaultIfBlank(task.getStoryboardJson(), "[]"));
            if (!arr.isArray()) {
                return;
            }
            List<Map<String, String>> items = new ArrayList<>();
            for (JsonNode item : arr) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("text", item.path("text").asText(""));
                m.put("image", item.path("image").asText(""));
                m.put("imageAttachmentId", "");
                m.put("videoPrompt", item.path("videoPrompt").asText(""));
                m.put("videoAttachmentId", "");
                items.add(m);
            }
            task.setStoryboardJson(MAPPER.writeValueAsString(items));
            task.setUpdateTime(System.currentTimeMillis());
            aiVideoTaskMapper.updateById(task);
        } catch (Exception e) {
            log.warn("释放分镜配图/逐镜视频失败 taskId={}", task.getId(), e);
        }
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

    /** 递归删除目录（含分镜配图等中间产物），忽略失败 */
    private void deleteDirQuietly(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDirQuietly(child);
            }
        }
        dir.delete();
    }

    /** 从分镜 JSON 重建分镜列表（text/image/imageAttachmentId/videoPrompt/videoAttachmentId），供出图/配音/出视频/合成复用 */
    private List<VideoSegment> parseStoryboardSegments(String storyboardJson) {
        try {
            JsonNode arr = MAPPER.readTree(StringUtils.defaultIfBlank(storyboardJson, "[]"));
            if (!arr.isArray()) {
                return List.of();
            }
            List<VideoSegment> segments = new ArrayList<>();
            for (JsonNode item : arr) {
                segments.add(new VideoSegment(
                        StringUtils.defaultString(item.path("text").asText(null)),
                        StringUtils.defaultString(item.path("image").asText(null)),
                        StringUtils.defaultString(item.path("imageAttachmentId").asText(null)),
                        StringUtils.defaultString(item.path("videoPrompt").asText(null)),
                        StringUtils.defaultString(item.path("videoAttachmentId").asText(null))));
            }
            return segments;
        } catch (Exception e) {
            log.warn("短视频分镜 JSON 解析失败，回退空分镜", e);
            return List.of();
        }
    }

    /** 合成阶段按 workDir 里的 scene-{index}.png/.jpg 重建配图文件列表（缺失/失败为 null，回退纯色背景） */
    private List<File> rebuildSceneImages(List<VideoSegment> segments, File workDir) {
        List<File> images = new ArrayList<>(Collections.nCopies(segments.size(), null));
        for (int i = 0; i < segments.size(); i++) {
            File png = new File(workDir, "scene-" + i + ".png");
            if (png.exists()) {
                images.set(i, png);
                continue;
            }
            File jpg = new File(workDir, "scene-" + i + ".jpg");
            images.set(i, jpg.exists() ? jpg : null);
        }
        return images;
    }

    /** 取封面图片文件供合成：封面复用了某镜配图时直接用该镜 workDir 里的场景文件，独立上传的封面从附件系统下载 */
    private File resolveCoverFile(AiVideoTask task, List<VideoSegment> segments, List<File> images,
                                  File workDir, String orgId) {
        String coverId = task.getCoverAttachmentId();
        if (StringUtils.isBlank(coverId)) {
            return null;
        }
        for (int i = 0; i < segments.size() && i < images.size(); i++) {
            if (coverId.equals(segments.get(i).imageAttachmentId()) && images.get(i) != null) {
                return images.get(i);
            }
        }
        return downloadCoverFile(coverId, task, workDir, orgId);
    }

    /** 取首个存在的分镜配图文件（未设封面时的标题卡回退），无则返回 null 走纯色标题卡 */
    private File firstSceneImage(List<File> images) {
        for (File image : images) {
            if (image != null && image.exists()) {
                return image;
            }
        }
        return null;
    }

    /** 把独立上传的封面附件下载到 workDir，失败返回 null（标题卡回退纯色背景） */
    private File downloadCoverFile(String attachmentId, AiVideoTask task, File workDir, String orgId) {
        try {
            Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
            if (attachment == null) {
                return null;
            }
            FileRequest request = new FileRequest(
                    DefaultRepositoryDir.getTransferFileDir(orgId, task.getId(), attachmentId),
                    StorageType.LOCAL.name(),
                    attachment.getName());
            String ext = StringUtils.substringAfterLast(attachment.getName(), ".").toLowerCase();
            if (!ext.matches("[a-z0-9]{1,5}")) {
                ext = "jpg";
            }
            File target = new File(workDir, "cover-" + attachmentId + "." + ext);
            try (InputStream in = fileCommonService.getFileInputStream(request)) {
                if (in == null) {
                    return null;
                }
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (Exception e) {
            log.warn("短视频封面下载失败，标题卡回退纯色背景，coverId={}", attachmentId, e);
            return null;
        }
    }

    private VideoTaskResponse toResponse(AiVideoTask task) {
        VideoTaskResponse response = new VideoTaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setSourceText(task.getSourceText());
        response.setVoice(task.getVoice());
        response.setStyle(task.getStyle());
        response.setTemplateKey(task.getTemplateKey());
        response.setMode(task.getMode());
        response.setVideoType(task.getVideoType());
        response.setImageModelId(task.getImageModelId());
        response.setVideoModelId(task.getVideoModelId());
        response.setStylePrompt(task.getStylePrompt());
        response.setCharacterPrompt(task.getCharacterPrompt());
        response.setExtraPrompt(task.getExtraPrompt());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setAttachmentId(task.getAttachmentId());
        response.setCoverAttachmentId(task.getCoverAttachmentId());
        response.setCopyJson(task.getCopyJson());
        response.setStoryboardJson(task.getStoryboardJson());
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
