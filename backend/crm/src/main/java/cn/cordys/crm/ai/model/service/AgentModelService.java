package cn.cordys.crm.ai.model.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
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

/**
 * AI 模型配置服务（租户级，agent_model 表）。
 */
@Service
@Slf4j
public class AgentModelService {

    /** provider -> 默认 baseUrl（api_url 留空时兜底），仅 OpenAI 兼容协议厂商 */
    private static final Map<String, String> PROVIDER_DEFAULT_BASE_URL = Map.of(
            "OpenAI", "https://api.openai.com/v1",
            "DeepSeek", "https://api.deepseek.com",
            "阿里云", "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "Anthropic", "https://api.anthropic.com/v1",
            "腾讯云", "https://api.hunyuan.cloud.tencent.com/v1",
            "豆包", "https://ark.cn-beijing.volces.com/api/v3"
    );

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
        List<AgentModel> candidates = new ArrayList<>();
        if (modelIds != null) {
            for (String id : modelIds) {
                AgentModel model = agentModelMapper.selectByPrimaryKey(id);
                if (model != null && Boolean.TRUE.equals(model.getEnable()) && orgId.equals(model.getOrganizationId())) {
                    candidates.add(model);
                }
            }
        }
        // 策略未配置或无可用模型时，回退到第一个启用模型
        if (candidates.isEmpty()) {
            AgentModel criteria = new AgentModel();
            criteria.setOrganizationId(orgId);
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
     * 取租户某 provider 下第一个启用且已填 apiKey 的模型密钥，供非对话能力（如语音转写 ASR）复用。
     * 未配置或未启用时抛异常，提示先到模型设置开通。
     */
    public String resolveProviderApiKey(String orgId, String provider) {
        AgentModel criteria = new AgentModel();
        criteria.setOrganizationId(orgId);
        criteria.setProvider(provider);
        criteria.setEnable(true);
        for (AgentModel model : agentModelMapper.select(criteria)) {
            if (StringUtils.isNotBlank(model.getApiKey())) {
                return model.getApiKey();
            }
        }
        throw new GenericException("请先在「模型设置」中配置并启用「" + provider + "」模型，用于语音转写");
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
        model.setEnable(request.getEnable() == null ? Boolean.TRUE : request.getEnable());
        model.setUserDailyLimit(request.getUserDailyLimit());
        model.setGlobalDailyLimit(request.getGlobalDailyLimit());
        model.setModelParams(request.getModelParams());
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
        model.setEnable(request.getEnable() == null ? Boolean.TRUE : request.getEnable());
        model.setUserDailyLimit(request.getUserDailyLimit());
        model.setGlobalDailyLimit(request.getGlobalDailyLimit());
        model.setModelParams(request.getModelParams());
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
        if (isDoubaoSeed(model)) {
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
