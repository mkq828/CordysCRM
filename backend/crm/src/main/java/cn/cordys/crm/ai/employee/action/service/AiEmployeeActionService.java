package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.AiEmployeeActionPayload;
import cn.cordys.crm.ai.employee.action.dto.response.AgentActionApproveResponse;
import cn.cordys.crm.ai.employee.action.dto.response.AgentActionSuggestionResponse;
import cn.cordys.crm.ai.employee.action.dto.response.AiEmployeeActionResponse;
import cn.cordys.crm.ai.employee.action.mapper.ExtAiEmployeeActionMapper;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 待确认动作的查询与处理。数据范围与「本人+下级」对齐（admin 看全部）；确认/驳回均二次复核权限。
 */
@Service
public class AiEmployeeActionService {

    @Resource
    private BaseMapper<AiEmployeeAction> actionMapper;

    @Resource
    private ExtAiEmployeeActionMapper extAiEmployeeActionMapper;

    @Resource
    private DataScopeService dataScopeService;

    @Resource
    private BaseService baseService;

    @Resource
    private AiAgentActionExecutor executor;

    public List<AiEmployeeActionResponse> listPending(String orgId, String userId) {
        List<AiEmployeeAction> actions = extAiEmployeeActionMapper.selectPending(orgId);
        Set<String> scope = resolveScope(orgId, userId);
        List<AiEmployeeAction> visible = actions.stream()
                .filter(a -> isInScope(a, scope))
                .toList();
        List<String> ownerIds = visible.stream()
                .map(AiEmployeeAction::getOwnerId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, String> ownerNameMap = ownerIds.isEmpty() ? Map.of() : baseService.getUserNameMap(ownerIds);
        return visible.stream().map(a -> toResponse(a, ownerNameMap)).toList();
    }

    /** 智慧工作台「AI 跟进建议」：投影跟进类待确认动作，内存分页（PENDING 数据量小） */
    public Pager<List<AgentActionSuggestionResponse>> listSuggestions(String orgId, String userId,
                                                                      BasePageRequest request) {
        List<AiEmployeeActionResponse> pending = listPending(orgId, userId);
        List<AiEmployeeActionResponse> followUps = pending.stream()
                .filter(a -> AiEmployeeAction.ACTION_FOLLOW_UP_CREATE.equals(a.getActionType()))
                .toList();
        return paginate(followUps, request, this::toSuggestion);
    }

    /** 智慧工作台「AI 方案审核」：投影非跟进类待确认动作，内存分页 */
    public Pager<List<AgentActionApproveResponse>> listApprovals(String orgId, String userId,
                                                                 BasePageRequest request) {
        List<AiEmployeeActionResponse> pending = listPending(orgId, userId);
        List<AiEmployeeActionResponse> approvals = pending.stream()
                .filter(a -> !AiEmployeeAction.ACTION_FOLLOW_UP_CREATE.equals(a.getActionType()))
                .toList();
        return paginate(approvals, request, this::toApproval);
    }

    private <T> Pager<List<T>> paginate(List<AiEmployeeActionResponse> all, BasePageRequest request,
                                        Function<AiEmployeeActionResponse, T> mapper) {
        int current = request == null || request.getCurrent() <= 0 ? 1 : request.getCurrent();
        int pageSize = request == null || request.getPageSize() <= 0 ? 10 : request.getPageSize();
        int from = (current - 1) * pageSize;
        List<T> page = all.stream().skip(from).limit(pageSize).map(mapper).toList();
        return new Pager<>(page, all.size(), pageSize, current);
    }

    private AgentActionSuggestionResponse toSuggestion(AiEmployeeActionResponse a) {
        AgentActionSuggestionResponse r = new AgentActionSuggestionResponse();
        r.setId(a.getId());
        r.setTopic(StringUtils.defaultIfBlank(a.getCustomerName(), a.getResourceName()));
        r.setSummary(StringUtils.defaultIfBlank(a.getContent(), a.getSuggestion()));
        r.setOwnerName(a.getOwnerName());
        return r;
    }

    private AgentActionApproveResponse toApproval(AiEmployeeActionResponse a) {
        AgentActionApproveResponse r = new AgentActionApproveResponse();
        r.setId(a.getId());
        r.setTopic(titleOf(a));
        r.setSummary(summaryOf(a));
        r.setType(a.getActionType());
        r.setOwnerName(a.getOwnerName());
        return r;
    }

    /** 标题：催款/线索流转以资源名为准，其余以客户名为准（与前端 AiActionCard 同口径） */
    private String titleOf(AiEmployeeActionResponse a) {
        if (AiEmployeeAction.ACTION_DUNNING.equals(a.getActionType())
                || AiEmployeeAction.ACTION_CLUE_FLOW.equals(a.getActionType())) {
            return StringUtils.defaultIfBlank(a.getResourceName(), a.getCustomerName());
        }
        return StringUtils.defaultIfBlank(a.getCustomerName(), a.getResourceName());
    }

    /** 摘要：按动作类型从 payload 提取最核心文本，类型文案由前端 i18n 映射 */
    private String summaryOf(AiEmployeeActionResponse a) {
        Map<String, Object> payload = parsePayload(a.getPayload());
        switch (a.getActionType()) {
            case AiEmployeeAction.ACTION_DUNNING:
                return firstNonBlank(str(payload.get("dunningContent")), str(payload.get("suggestion")));
            case AiEmployeeAction.ACTION_TAG:
                return firstNonBlank(str(payload.get("reason")), str(payload.get("tagName")));
            case AiEmployeeAction.ACTION_CLUE_FLOW: {
                String reason = str(payload.get("reason"));
                if (StringUtils.isNotBlank(reason)) {
                    return reason;
                }
                String stageName = ClueStatus.getByKey(str(payload.get("targetStage")));
                return StringUtils.defaultIfBlank(stageName, str(payload.get("targetStage")));
            }
            case AiEmployeeAction.ACTION_FILL_FIELD:
                return fillFieldSummary(payload);
            default:
                return a.getContent();
        }
    }

    /** 补全数据摘要：拼接「字段名：建议值」，缺字段名时回退字段ID */
    private String fillFieldSummary(Map<String, Object> payload) {
        Object fieldsObj = payload.get("fields");
        if (!(fieldsObj instanceof List<?> fields)) {
            return null;
        }
        return fields.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .map(item -> {
                    Map<?, ?> m = (Map<?, ?>) item;
                    String name = str(m.get("fieldName"));
                    String key = StringUtils.isNotBlank(name) ? name : str(m.get("fieldId"));
                    return key + "：" + StringUtils.defaultString(str(m.get("suggestedValue")));
                })
                .collect(Collectors.joining("；"));
    }

    private Map<String, Object> parsePayload(String json) {
        Map<String, Object> map = JSON.parseToMap(json);
        return map == null ? Map.of() : map;
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(String actionId, String orgId, String userId) {
        AiEmployeeAction action = loadPending(actionId, orgId);
        checkInScope(action, orgId, userId);
        // 权限校验/执行失败时整段回滚，动作保持 PENDING，不产生半条跟进记录
        executor.execute(action, orgId, userId);
        long now = System.currentTimeMillis();
        action.setStatus(AiEmployeeAction.STATUS_EXECUTED);
        action.setConfirmUser(userId);
        action.setConfirmTime(now);
        action.setUpdateUser(userId);
        action.setUpdateTime(now);
        actionMapper.updateById(action);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(String actionId, String orgId, String userId) {
        AiEmployeeAction action = loadPending(actionId, orgId);
        checkInScope(action, orgId, userId);
        long now = System.currentTimeMillis();
        action.setStatus(AiEmployeeAction.STATUS_REJECTED);
        action.setConfirmUser(userId);
        action.setConfirmTime(now);
        action.setUpdateUser(userId);
        action.setUpdateTime(now);
        actionMapper.updateById(action);
    }

    private AiEmployeeAction loadPending(String actionId, String orgId) {
        AiEmployeeAction action = actionMapper.selectByPrimaryKey(actionId);
        if (action == null || !orgId.equals(action.getOrganizationId())) {
            throw new GenericException("待确认动作不存在");
        }
        if (!AiEmployeeAction.STATUS_PENDING.equals(action.getStatus())) {
            throw new GenericException("该动作已处理，请勿重复操作");
        }
        return action;
    }

    /** admin 看全部；其余看「本人+下级」名下的待确认动作 */
    private Set<String> resolveScope(String orgId, String userId) {
        if (InternalUser.ADMIN.getValue().equals(userId)) {
            return null;
        }
        return dataScopeService.getUserIdsWithSubordinates(userId, orgId);
    }

    private boolean isInScope(AiEmployeeAction action, Set<String> scope) {
        return scope == null || (action.getOwnerId() != null && scope.contains(action.getOwnerId()));
    }

    private void checkInScope(AiEmployeeAction action, String orgId, String userId) {
        if (InternalUser.ADMIN.getValue().equals(userId)) {
            return;
        }
        Set<String> scope = dataScopeService.getUserIdsWithSubordinates(userId, orgId);
        if (action.getOwnerId() == null || !scope.contains(action.getOwnerId())) {
            throw new GenericException("无权处理该待确认动作");
        }
    }

    private AiEmployeeActionResponse toResponse(AiEmployeeAction action, Map<String, String> ownerNameMap) {
        AiEmployeeActionResponse response = new AiEmployeeActionResponse();
        response.setId(action.getId());
        response.setActionType(action.getActionType());
        response.setResourceType(action.getResourceType());
        response.setResourceId(action.getResourceId());
        response.setResourceName(action.getResourceName());
        response.setCustomerId(action.getCustomerId());
        response.setCustomerName(action.getCustomerName());
        response.setOwnerId(action.getOwnerId());
        response.setOwnerName(ownerNameMap.get(action.getOwnerId()));
        response.setStatus(action.getStatus());
        response.setCreateTime(action.getCreateTime());
        response.setPayload(action.getPayload());
        AiEmployeeActionPayload payload = AiEmployeeActionPayload.parse(action.getPayload());
        response.setContent(payload.getContent());
        response.setFollowMethod(payload.getFollowMethod());
        response.setSuggestion(payload.getSuggestion());
        return response;
    }
}
