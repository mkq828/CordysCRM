package cn.cordys.crm.ai.model.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AliyunVoiceCloneConstant;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.dto.request.AgentModelPageRequest;
import cn.cordys.crm.ai.model.dto.request.AgentModelSaveRequest;
import cn.cordys.crm.ai.model.dto.response.AgentModelOptionResponse;
import cn.cordys.crm.ai.model.dto.response.AgentModelResponse;
import cn.cordys.crm.ai.model.dto.response.AgentModelStrategyResponse;
import cn.cordys.crm.ai.model.mapper.ExtAgentModelMapper;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 模型配置服务（平台级，agent_model 表）。
 * 模型由 admin 统一配置在默认组织 100001 下，所有租户共享；租户 orgId 仅用于额度/权限判定，不影响模型选择。
 */
@Service
@Slf4j
public class AgentModelService {

    /** 模型配置平台级统一挂在默认组织 100001 下，所有真实租户共享，resolve 一律取该组织、不按租户隔离。 */
    private static final String PLATFORM_MODEL_ORG = "100001";

    /** provider -> 默认 baseUrl（api_url 留空时兜底），仅 OpenAI 兼容协议厂商 */
    private static final Map<String, String> PROVIDER_DEFAULT_BASE_URL = Map.of(
            "OpenAI", "https://api.openai.com/v1",
            "DeepSeek", "https://api.deepseek.com",
            "阿里云", "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "Anthropic", "https://api.anthropic.com/v1",
            "腾讯云", "https://api.hunyuan.cloud.tencent.com/v1",
            "豆包", "https://ark.cn-beijing.volces.com/api/v3",
            "豆包语音", "https://openspeech.bytedance.com/api/v3"
    );

    /** 已接入语音合成（TTS）适配器的 provider 名，用于配音模型回退与校验 */
    private static final Set<String> TTS_PROVIDERS = Set.of("阿里云", "豆包语音");

    @Resource
    private BaseMapper<AgentModel> agentModelMapper;

    @Resource
    private ExtAgentModelMapper extAgentModelMapper;

    @Resource
    private AgentModelStrategyService agentModelStrategyService;

    public Pager<List<AgentModelResponse>> page(AgentModelPageRequest request, String orgId) {
        LocalDate today = LocalDate.now();
        long todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long todayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<AgentModelResponse> list = extAgentModelMapper.selectPage(orgId, request.getKeyword(), todayStart, todayEnd);
        return PageUtils.setPageInfo(page, list);
    }

    public AgentModelResponse get(String id, String orgId) {
        AgentModel model = agentModelMapper.selectByPrimaryKey(id);
        if (model == null || !orgId.equals(model.getOrganizationId())) {
            throw new GenericException("模型不存在");
        }
        AgentModelResponse response = new AgentModelResponse();
        BeanUtils.copyProperties(model, response);
        return response;
    }

    /**
     * 解析当前租户的对话模型候选列表（按路由策略 chatModels 顺序）。
     * 自动降级（fallback）开启时返回全部可用候选，关闭时仅返回首选；策略未配置时回退到第一个启用模型。
     *
     * @return 有序候选模型列表，无可用模型时为空列表
     */
    public List<AgentModel> resolveChatModels(String orgId) {
        AgentModelStrategyResponse strategy = agentModelStrategyService.get();
        return resolveModels(orgId, strategy.getChatModels(), Boolean.TRUE.equals(strategy.getFallback()));
    }

    /**
     * 解析「洞察与评估任务」专用模型候选（路由策略 taskModels 顺序）。
     * 该列用于会话军师等单次分析/洞察场景，与对话模型分离；taskModels 未配置时回退到对话模型，保证存量租户无感。
     */
    public List<AgentModel> resolveTaskModels(String orgId) {
        AgentModelStrategyResponse strategy = agentModelStrategyService.get();
        if (strategy.getTaskModels() == null || strategy.getTaskModels().isEmpty()) {
            return resolveChatModels(orgId);
        }
        return resolveModels(orgId, strategy.getTaskModels(), Boolean.TRUE.equals(strategy.getFallback()));
    }

    private List<AgentModel> resolveModels(String orgId, List<String> modelIds, boolean fallback) {
        List<AgentModel> candidates = resolveByIds(orgId, modelIds);
        // 策略未配置或无可用模型时，回退到第一个启用模型
        if (candidates.isEmpty()) {
            AgentModel criteria = new AgentModel();
            criteria.setOrganizationId(PLATFORM_MODEL_ORG);
            criteria.setEnable(true);
            List<AgentModel> enabled = agentModelMapper.select(criteria);
            if (!enabled.isEmpty()) {
                candidates.add(enabled.get(0));
            }
        }
        // 未开启自动降级时只保留首选
        if (!fallback && !candidates.isEmpty()) {
            return List.of(candidates.get(0));
        }
        return candidates;
    }

    /**
     * 解析「配音模型」候选（路由策略 ttsModels 顺序），供功能 7 短视频生成 TTS 使用。
     * ttsModels 未配置时回退到第一个启用且 provider 支持语音合成的模型（兼容历史阿里云直连），
     * 保证存量租户无感。TTS 不做「任意第一个启用模型」回退，避免误用对话模型合成语音。
     */
    public List<AgentModel> resolveTtsModels(String orgId) {
        AgentModelStrategyResponse strategy = agentModelStrategyService.get();
        List<String> ids = strategy.getTtsModels();
        if (ids != null && !ids.isEmpty()) {
            List<AgentModel> resolved = resolveByIds(orgId, ids);
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(PLATFORM_MODEL_ORG);
        criteria.setEnable(true);
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if (TTS_PROVIDERS.contains(model.getProvider()) && hasTtsCredential(model)) {
                return List.of(model);
            }
        }
        return List.of();
    }

    /**
     * 解析「声音克隆音色试听」用的阿里云配音模型。克隆 voice_id 只能由阿里云合成（不能回退到豆包语音），
     * 故不复用 {@link #resolveTtsModels}（其回退在策略未配置时只返回首个 TTS 模型，可能是豆包语音），
     * 直接取所有启用且含 apiKey 的阿里云模型，供逐个降级试听。
     */
    public List<AgentModel> resolveAliyunTtsModels(String orgId) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(PLATFORM_MODEL_ORG);
        criteria.setProvider("阿里云");
        criteria.setEnable(true);
        List<AgentModel> result = new ArrayList<>();
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if (hasTtsCredential(model)) {
                result.add(model);
            }
        }
        return result;
    }

    /**
     * 解析「文生图」模型（功能 7 短视频配图用）。当前按约定识别：豆包方舟的 seedream 系列模型
     * （modelName 含 seedream）即配图模型，admin 在模型设置里新增一条 provider=豆包、modelName=doubao-seedream-* 即可生效。
     * 未配置时返回空列表，业务侧回退纯色背景，不影响成片。
     */
    public List<AgentModel> resolveImageModels(String orgId) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(PLATFORM_MODEL_ORG);
        criteria.setEnable(true);
        List<AgentModel> result = new ArrayList<>();
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if ("豆包".equals(model.getProvider())
                    && StringUtils.defaultString(model.getModelName()).toLowerCase().contains("seedream")
                    && StringUtils.isNotBlank(model.getApiKey())) {
                result.add(model);
            }
        }
        return result;
    }

    /**
     * 解析「图生视频」模型（功能 7 短视频成片，成片方式为「AI 视频」时使用）。识别两类：豆包方舟 seedance 系列，
     * 以及 MiniMax H 系列（经 OpenAI 兼容口接入、供应商记为「阿里云」，modelName 含 minimax）。
     * 未配置时返回空列表，风格页「视频模型」区块展示「暂未开放」。
     */
    public List<AgentModel> resolveVideoModels(String orgId) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(PLATFORM_MODEL_ORG);
        criteria.setEnable(true);
        List<AgentModel> result = new ArrayList<>();
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if (isVideoModel(model) && StringUtils.isNotBlank(model.getApiKey())) {
                result.add(model);
            }
        }
        return result;
    }

    /** 识别「图生视频」模型：豆包 seedance 系列，或 MiniMax H 系列。 */
    private boolean isVideoModel(AgentModel model) {
        String provider = model.getProvider();
        String name = StringUtils.defaultString(model.getModelName()).toLowerCase();
        return ("豆包".equals(provider) && name.contains("seedance")) || name.contains("minimax");
    }

    private List<AgentModel> resolveByIds(String orgId, List<String> modelIds) {
        List<AgentModel> candidates = new ArrayList<>();
        if (modelIds != null) {
            for (String id : modelIds) {
                AgentModel model = agentModelMapper.selectByPrimaryKey(id);
                if (model != null && Boolean.TRUE.equals(model.getEnable()) && PLATFORM_MODEL_ORG.equals(model.getOrganizationId())) {
                    candidates.add(model);
                }
            }
        }
        return candidates;
    }

    /**
     * 取租户某 provider 下第一个启用且已填 apiKey 的模型密钥，供非对话能力（如语音转写 ASR）复用。
     * 未配置或未启用时抛异常，提示先到模型设置开通。
     */
    public String resolveProviderApiKey(String orgId, String provider) {
        return resolveProviderApiKey(orgId, provider, "语音转写");
    }

    /**
     * 取租户某 provider 下第一个启用且已填 apiKey 的模型密钥，供非对话能力（ASR/TTS）复用。
     * purpose 用于拼错误提示里的用途（语音转写 / 语音合成）。
     */
    public String resolveProviderApiKey(String orgId, String provider, String purpose) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(PLATFORM_MODEL_ORG);
        criteria.setProvider(provider);
        criteria.setEnable(true);
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if (StringUtils.isNotBlank(model.getApiKey())) {
                return model.getApiKey();
            }
        }
        throw new GenericException("请先在「模型设置」中配置并启用「" + provider + "」模型，用于" + purpose);
    }

    /**
     * 解析「向量化(embedding)」模型：取阿里云（千问）provider 下第一个启用且已填 apiKey 的模型，
     * 复用其 baseUrl/apiKey 调 text-embedding-v3。未配置时返回 null，知识库降级关键词检索。
     */
    public AgentModel resolveEmbeddingModel(String orgId) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(PLATFORM_MODEL_ORG);
        criteria.setProvider("阿里云");
        criteria.setEnable(true);
        AgentModel fallback = null;
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if (StringUtils.isBlank(model.getApiKey())) {
                continue;
            }
            // text-embedding-v3 只挂在 DashScope 百炼端点，优先取真正走百炼的模型，
            // 避免误取 provider 记为「阿里云」但 apiUrl 指向其它 OpenAI 兼容口（如 MiniMax）的模型
            if (isDashScopeUrl(model.getApiUrl())) {
                return model;
            }
            if (fallback == null) {
                fallback = model;
            }
        }
        return fallback;
    }

    /** 是否 DashScope 百炼兼容端点（text-embedding-v3 的宿主） */
    private boolean isDashScopeUrl(String apiUrl) {
        return StringUtils.isNotBlank(apiUrl) && apiUrl.contains("dashscope.aliyuncs.com");
    }

    /**
     * 解析「声音克隆」目标模型名：阿里云 provider 下第一条已配置 voiceCloneModel 的启用模型；
     * 未配置时回退默认 {@link AliyunVoiceCloneConstant#QWEN_TTS_VC_MODEL}。
     * 阿里复刻模型会随版本下线，admin 在「模型设置」改声音克隆模型名即可，无需改代码重发版。
     */
    public String resolveVoiceCloneModel(String orgId) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(PLATFORM_MODEL_ORG);
        criteria.setProvider("阿里云");
        criteria.setEnable(true);
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if (StringUtils.isNotBlank(model.getVoiceCloneModel())) {
                return model.getVoiceCloneModel();
            }
        }
        return AliyunVoiceCloneConstant.QWEN_TTS_VC_MODEL;
    }

    /**
     * 判断配音模型是否具备可用凭证：豆包语音用三件套（AppID/AccessToken/SecretKey），其余 provider 用 apiKey。
     * 供配音/试听模型筛选与降级共用，避免误用空凭据的模型。
     */
    public boolean hasTtsCredential(AgentModel model) {
        if (model == null) {
            return false;
        }
        if ("豆包语音".equals(model.getProvider())) {
            return StringUtils.isNotBlank(model.getAppId())
                    && StringUtils.isNotBlank(model.getAccessToken())
                    && StringUtils.isNotBlank(model.getSecretKey());
        }
        return StringUtils.isNotBlank(model.getApiKey());
    }

    public List<AgentModelOptionResponse> options(String orgId) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(orgId);
        criteria.setEnable(true);
        return agentModelMapper.select(criteria).stream()
                .map(m -> {
                    AgentModelOptionResponse option = new AgentModelOptionResponse();
                    option.setId(m.getId());
                    option.setName(m.getDisplayName());
                    option.setIdAsString(m.getId());
                    return option;
                })
                .toList();
    }

    public void add(AgentModelSaveRequest request, String orgId, String userId) {
        long now = System.currentTimeMillis();
        AgentModel model = new AgentModel();
        model.setId(IDGenerator.nextStr());
        model.setOrganizationId(orgId);
        model.setDisplayName(request.getDisplayName());
        model.setModelName(request.getModelName());
        model.setProvider(request.getProvider());
        model.setApiUrl(resolveApiUrl(request.getProvider(), request.getApiUrl()));
        model.setApiKey(request.getApiKey());
        model.setAppId(request.getAppId());
        model.setAccessToken(request.getAccessToken());
        model.setSecretKey(request.getSecretKey());
        model.setEnable(request.getEnable() == null ? Boolean.TRUE : request.getEnable());
        model.setUserDailyLimit(request.getUserDailyLimit());
        model.setGlobalDailyLimit(request.getGlobalDailyLimit());
        model.setModelParams(request.getModelParams());
        model.setVoiceCloneModel(request.getVoiceCloneModel());
        model.setDescription(request.getDescription());
        model.setRecommended(Boolean.TRUE.equals(request.getRecommended()));
        model.setCreateUser(userId);
        model.setUpdateUser(userId);
        model.setCreateTime(now);
        model.setUpdateTime(now);
        agentModelMapper.insert(model);
    }

    public void update(AgentModelSaveRequest request, String orgId, String userId) {
        AgentModel model = agentModelMapper.selectByPrimaryKey(request.getId());
        if (model == null || !orgId.equals(model.getOrganizationId())) {
            throw new GenericException("模型不存在");
        }
        model.setDisplayName(request.getDisplayName());
        model.setModelName(request.getModelName());
        model.setProvider(request.getProvider());
        model.setApiUrl(resolveApiUrl(request.getProvider(), request.getApiUrl()));
        model.setApiKey(request.getApiKey());
        model.setAppId(request.getAppId());
        model.setAccessToken(request.getAccessToken());
        model.setSecretKey(request.getSecretKey());
        model.setEnable(request.getEnable() == null ? Boolean.TRUE : request.getEnable());
        model.setUserDailyLimit(request.getUserDailyLimit());
        model.setGlobalDailyLimit(request.getGlobalDailyLimit());
        model.setModelParams(request.getModelParams());
        model.setVoiceCloneModel(request.getVoiceCloneModel());
        model.setDescription(request.getDescription());
        model.setRecommended(Boolean.TRUE.equals(request.getRecommended()));
        model.setUpdateUser(userId);
        model.setUpdateTime(System.currentTimeMillis());
        agentModelMapper.updateById(model);
    }

    public void delete(String id, String orgId) {
        AgentModel model = agentModelMapper.selectByPrimaryKey(id);
        if (model == null || !orgId.equals(model.getOrganizationId())) {
            throw new GenericException("模型不存在");
        }
        agentModelMapper.deleteByPrimaryKey(id);
    }

    public void switchEnable(String id, String orgId) {
        AgentModel model = agentModelMapper.selectByPrimaryKey(id);
        if (model == null || !orgId.equals(model.getOrganizationId())) {
            throw new GenericException("模型不存在");
        }
        model.setEnable(!Boolean.TRUE.equals(model.getEnable()));
        model.setUpdateTime(System.currentTimeMillis());
        agentModelMapper.updateById(model);
    }

    private String resolveApiUrl(String provider, String apiUrl) {
        if (StringUtils.isNotBlank(apiUrl)) {
            return apiUrl;
        }
        String defaultUrl = PROVIDER_DEFAULT_BASE_URL.get(provider);
        if (defaultUrl == null) {
            throw new GenericException("请填写 API 请求地址");
        }
        return defaultUrl;
    }

    /**
     * 把模型配置里的 model_params（temperature / top_p / max_tokens）解析并应用到对话请求。
     * 字段缺失或解析失败时静默跳过，模型使用厂商默认参数。
     *
     * @param request 对话请求，就地填充采样参数
     * @param model   租户模型配置
     */
    public void applyModelParams(LlmChatRequest request, AgentModel model) {
        // 豆包 Seed 系列默认关闭深度思考：该系列默认 thinking=enabled，短问答都要先空转数秒、
        // 长 JSON 任务空转可达上百秒，业务场景追求响应速度，故默认关闭；仍可显式配置 model_params.thinking 覆盖。
        if (isDoubaoSeed(model) || isDeepSeekReasoning(model)) {
            request.setThinking(Map.of("type", "disabled"));
        }
        String params = model.getModelParams();
        if (StringUtils.isBlank(params)) {
            return;
        }
        try {
            Map<String, Object> map = JSON.parseToMap(params);
            request.setTemperature(asDouble(map.get("temperature")));
            request.setTopP(asDouble(map.get("top_p")));
            request.setMaxTokens(asInteger(map.get("max_tokens")));
            Object thinking = map.get("thinking");
            if (thinking instanceof Map<?, ?> thinkingMap) {
                request.setThinking((Map<String, Object>) thinkingMap);
            }
        } catch (Exception e) {
            log.warn("解析模型参数失败，model={}, params={}", model.getModelName(), params, e);
        }
    }

    /** 豆包 Seed 系列（doubao-seed-*）是深度思考模型，默认思考耗时极大，需按业务关闭 */
    private boolean isDoubaoSeed(AgentModel model) {
        return "豆包".equals(model.getProvider())
                && StringUtils.isNotBlank(model.getModelName())
                && model.getModelName().startsWith("doubao-seed");
    }

    /** DeepSeek V4 系列（deepseek-flash / deepseek-v4-pro）同为推理模型，默认 high 档先空转推理，需按业务关闭 */
    private boolean isDeepSeekReasoning(AgentModel model) {
        return StringUtils.isNotBlank(model.getModelName())
                && model.getModelName().startsWith("deepseek-");
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.doubleValue() : Double.valueOf(value.toString());
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString());
    }
}
