package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.AiEmployeeActionPayload;
import cn.cordys.crm.follow.constants.FollowUpPlanType;
import cn.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 待确认动作的派发执行。安全边界：写库前再次复核客户更新权限（角色权限位 + 数据权限 + 审批状态权限），
 * 与手动对话场景 {@code AiAgentTaskService.applyUpdate} 同口径；后续动作类型在此逐个扩展。
 */
@Service
public class AiAgentActionExecutor {

    @Resource
    private ResourcePermissionService resourcePermissionService;

    @Resource
    private FollowUpRecordService followUpRecordService;

    public void execute(AiEmployeeAction action, String orgId, String userId) {
        if (AiEmployeeAction.ACTION_FOLLOW_UP_CREATE.equals(action.getActionType())) {
            executeFollowUpCreate(action, orgId, userId);
        } else {
            throw new IllegalArgumentException("不支持的动作类型: " + action.getActionType());
        }
    }

    private void executeFollowUpCreate(AiEmployeeAction action, String orgId, String userId) {
        // 两段确认的最后一段：确认执行时服务端重新校验权限，防止未经确认或越权写库
        resourcePermissionService.checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                action.getCustomerId(), FormKeyConstants.CUSTOMER, userId, orgId);

        AiEmployeeActionPayload payload = AiEmployeeActionPayload.parse(action.getPayload());

        // 跟进记录归属客户负责人：AI 建议本就是给负责人的，确认执行（含管理员代确认）时
        // 负责人/创建人/更新人都归属该负责人，避免把客户「跟进人」误改成确认操作者（如 admin）。
        String owner = StringUtils.defaultIfBlank(action.getOwnerId(), userId);

        FollowUpRecordAddRequest request = new FollowUpRecordAddRequest();
        request.setCustomerId(action.getCustomerId());
        request.setType(FollowUpPlanType.CUSTOMER.name());
        request.setContent(StringUtils.defaultIfBlank(payload.getContent(), "AI 跟进建议已执行"));
        request.setFollowMethod(toFollowMethodCode(payload.getFollowMethod()));
        request.setFollowTime(System.currentTimeMillis());
        request.setOwner(owner);

        // add 内部会自动 handleFollowTimeAndFollower 更新客户最新跟进时间与跟进人
        followUpRecordService.add(request, owner, orgId);
    }

    /** 跟进方式：中文标签映射为存储编码（1 到访 / 2 电话），默认电话 */
    private String toFollowMethodCode(String label) {
        if (StringUtils.isNotBlank(label)
                && (label.contains("到访") || label.contains("拜访") || label.contains("上门") || label.contains("面谈"))) {
            return "1";
        }
        return "2";
    }
}
