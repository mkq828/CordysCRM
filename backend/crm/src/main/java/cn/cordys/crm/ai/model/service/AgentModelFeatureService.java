package cn.cordys.crm.ai.model.service;

import cn.cordys.crm.ai.model.constant.AiFeatureRouteRegistry;
import cn.cordys.crm.ai.model.constant.AiFeatureRouteRegistry.AiFeatureRoute;
import cn.cordys.crm.ai.model.constant.ModelRoute;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.dto.response.AgentModelFeatureUsageResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 「模型应用场景」只读矩阵服务：按功能路由注册表逐个调用 {@link AgentModelService} 的 resolve* 方法，
 * 汇总每个 AI 功能当前解析到的模型（供应商 + 首选/备用链）。只读，不改路由、不改配置。
 */
@Service
public class AgentModelFeatureService {

    @Resource
    private AgentModelService agentModelService;

    public List<AgentModelFeatureUsageResponse> listFeatureUsage(String orgId) {
        return AiFeatureRouteRegistry.FEATURES.stream()
                .map(feature -> toUsage(feature, orgId))
                .toList();
    }

    private AgentModelFeatureUsageResponse toUsage(AiFeatureRoute feature, String orgId) {
        AgentModelFeatureUsageResponse response = new AgentModelFeatureUsageResponse();
        response.setFeatureCode(feature.featureCode());
        response.setFeatureName(feature.featureName());
        response.setRemark(feature.remark());
        response.setUsages(feature.routes().stream()
                .map(route -> toRouteUsage(route, orgId))
                .toList());
        return response;
    }

    private AgentModelFeatureUsageResponse.Usage toRouteUsage(ModelRoute route, String orgId) {
        List<AgentModel> models = resolve(route, orgId);
        AgentModelFeatureUsageResponse.Usage usage = new AgentModelFeatureUsageResponse.Usage();
        usage.setRoute(route.name());
        usage.setRouteName(route.getLabel());
        usage.setModels(models.stream().map(this::toModelRef).toList());
        return usage;
    }

    private AgentModelFeatureUsageResponse.ModelRef toModelRef(AgentModel model) {
        AgentModelFeatureUsageResponse.ModelRef ref = new AgentModelFeatureUsageResponse.ModelRef();
        ref.setDisplayName(model.getDisplayName());
        ref.setModelName(model.getModelName());
        ref.setProvider(model.getProvider());
        return ref;
    }

    private List<AgentModel> resolve(ModelRoute route, String orgId) {
        return switch (route) {
            case CHAT -> agentModelService.resolveChatModels(orgId);
            case TASK -> agentModelService.resolveTaskModels(orgId);
            case IMAGE -> agentModelService.resolveImageModels(orgId);
            case VIDEO -> agentModelService.resolveVideoModels(orgId);
            case TTS -> agentModelService.resolveTtsModels(orgId);
            case VOICE_CLONE -> agentModelService.resolveAliyunTtsModels(orgId);
        };
    }
}
