package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.AiEmployeeActionPayload;
import cn.cordys.crm.ai.employee.action.dto.response.AiEmployeeActionResponse;
import cn.cordys.crm.ai.employee.action.mapper.ExtAiEmployeeActionMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        response.setCustomerId(action.getCustomerId());
        response.setCustomerName(action.getCustomerName());
        response.setOwnerId(action.getOwnerId());
        response.setOwnerName(ownerNameMap.get(action.getOwnerId()));
        response.setStatus(action.getStatus());
        response.setCreateTime(action.getCreateTime());
        AiEmployeeActionPayload payload = AiEmployeeActionPayload.parse(action.getPayload());
        response.setContent(payload.getContent());
        response.setFollowMethod(payload.getFollowMethod());
        response.setSuggestion(payload.getSuggestion());
        return response;
    }
}
