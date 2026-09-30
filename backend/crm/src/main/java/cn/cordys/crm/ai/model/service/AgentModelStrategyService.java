package cn.cordys.crm.ai.model.service;

import cn.cordys.crm.ai.model.domain.AgentModelStrategy;
import cn.cordys.crm.ai.model.dto.response.AgentModelStrategyResponse;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * AI 模型路由策略服务（agent_model_strategy 表，全局单行）。
 */
@Service
public class AgentModelStrategyService {

    private static final String STRATEGY_ID = "DEFAULT";

    @Resource
    private BaseMapper<AgentModelStrategy> strategyMapper;

    public AgentModelStrategyResponse get() {
        AgentModelStrategy strategy = strategyMapper.selectByPrimaryKey(STRATEGY_ID);
        AgentModelStrategyResponse response = new AgentModelStrategyResponse();
        if (strategy != null) {
            response.setChatModels(split(strategy.getChatModels()));
            response.setTaskModels(split(strategy.getTaskModels()));
            response.setFallback(Boolean.TRUE.equals(strategy.getFallback()));
        } else {
            response.setChatModels(List.of());
            response.setTaskModels(List.of());
            response.setFallback(true);
        }
        return response;
    }

    public void config(List<String> chatModels, List<String> taskModels, Boolean fallback) {
        AgentModelStrategy strategy = strategyMapper.selectByPrimaryKey(STRATEGY_ID);
        if (strategy == null) {
            strategy = new AgentModelStrategy();
            strategy.setId(STRATEGY_ID);
            strategy.setChatModels(join(chatModels));
            strategy.setTaskModels(join(taskModels));
            strategy.setFallback(fallback == null ? Boolean.TRUE : fallback);
            strategyMapper.insert(strategy);
        } else {
            strategy.setChatModels(join(chatModels));
            strategy.setTaskModels(join(taskModels));
            strategy.setFallback(fallback == null ? Boolean.TRUE : fallback);
            strategyMapper.updateById(strategy);
        }
    }

    private String join(List<String> ids) {
        return ids == null ? null : String.join(",", ids.stream().filter(StringUtils::isNotBlank).toList());
    }

    private List<String> split(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).filter(StringUtils::isNotBlank).toList();
    }
}
