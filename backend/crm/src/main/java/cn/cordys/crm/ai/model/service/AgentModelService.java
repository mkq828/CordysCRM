package cn.cordys.crm.ai.model.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
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
        boolean fallback = Boolean.TRUE.equals(strategy.getFallback());

        List<AgentModel> candidates = new ArrayList<>();
        if (strategy.getChatModels() != null) {
            for (String id : strategy.getChatModels()) {
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
}
