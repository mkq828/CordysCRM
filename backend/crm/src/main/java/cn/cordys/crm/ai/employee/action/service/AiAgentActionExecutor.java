package cn.cordys.crm.ai.employee.action.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.AiEmployeeActionPayload;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.dto.request.ClueStatusUpdateRequest;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.follow.constants.FollowUpPlanType;
import cn.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.customer.tag.service.CustomerTagService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Resource
    private CustomerTagService customerTagService;

    @Resource
    private ClueService clueService;

    @Resource
    private CustomerService customerService;

    public void execute(AiEmployeeAction action, String orgId, String userId) {
        switch (action.getActionType()) {
            case AiEmployeeAction.ACTION_FOLLOW_UP_CREATE -> executeFollowUpCreate(action, orgId, userId);
            case AiEmployeeAction.ACTION_DUNNING -> executeDunning(action, orgId, userId);
            case AiEmployeeAction.ACTION_TAG -> executeTag(action, orgId, userId);
            case AiEmployeeAction.ACTION_CLUE_FLOW -> executeClueFlow(action, orgId, userId);
            case AiEmployeeAction.ACTION_FILL_FIELD -> executeFillField(action, orgId, userId);
            default -> throw new IllegalArgumentException("不支持的动作类型: " + action.getActionType());
        }
    }

    /** 自动补全数据：确认执行时把建议值走 agentInvoke 部分合并落库（只补不删，与对话场景 applyUpdate 同口径） */
    private void executeFillField(AiEmployeeAction action, String orgId, String userId) {
        resourcePermissionService.checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                action.getResourceId(), FormKeyConstants.CUSTOMER, userId, orgId);

        Map<String, Object> payload = JSON.parseToMap(action.getPayload());
        Object fieldsObj = payload == null ? null : payload.get("fields");
        if (!(fieldsObj instanceof List<?> fields)) {
            throw new IllegalArgumentException("没有可补全的字段");
        }
        List<BaseModuleFieldValue> moduleFields = new ArrayList<>();
        for (Object item : fields) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            String fieldId = m.get("fieldId") == null ? null : m.get("fieldId").toString();
            Object value = m.get("suggestedValue");
            if (StringUtils.isBlank(fieldId) || value == null || StringUtils.isBlank(value.toString())) {
                continue;
            }
            moduleFields.add(new BaseModuleFieldValue(fieldId, value.toString()));
        }
        if (moduleFields.isEmpty()) {
            throw new IllegalArgumentException("没有可补全的字段");
        }
        CustomerUpdateRequest update = new CustomerUpdateRequest();
        update.setId(action.getResourceId());
        update.setAgentInvoke(true);
        update.setModuleFields(moduleFields);
        customerService.update(update, userId, orgId);
    }

    /** 线索流转：确认执行时把线索流转到目标阶段（只做阶段流转，不做转商机） */
    private void executeClueFlow(AiEmployeeAction action, String orgId, String userId) {
        resourcePermissionService.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE,
                action.getResourceId(), FormKeyConstants.CLUE, userId, orgId);

        Map<String, Object> payload = JSON.parseToMap(action.getPayload());
        Object targetStage = payload == null ? null : payload.get("targetStage");
        if (targetStage == null || StringUtils.isBlank(targetStage.toString())) {
            throw new IllegalArgumentException("目标阶段为空");
        }
        String stage = targetStage.toString().trim();
        // 服务端兜底校验阶段合法，防止 payload 被篡改成非法编码
        if (ClueStatus.getByKey(stage) == null) {
            throw new IllegalArgumentException("目标阶段非法: " + stage);
        }
        ClueStatusUpdateRequest request = new ClueStatusUpdateRequest();
        request.setId(action.getResourceId());
        request.setStage(stage);
        clueService.updateStatus(request, userId, orgId);
    }

    /** 自动打标签：确认执行时把建议标签落到客户（标签不存在则按名创建，来源=AI） */
    private void executeTag(AiEmployeeAction action, String orgId, String userId) {
        resourcePermissionService.checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                action.getResourceId(), FormKeyConstants.CUSTOMER, userId, orgId);

        Map<String, Object> payload = JSON.parseToMap(action.getPayload());
        Object tagName = payload == null ? null : payload.get("tagName");
        if (tagName == null || StringUtils.isBlank(tagName.toString())) {
            throw new IllegalArgumentException("标签名为空");
        }
        customerTagService.tagCustomer(orgId, action.getResourceId(), tagName.toString().trim(), "AI");
    }

    /** 自动催款：确认执行时把 AI 催款话术作为站内信发给资源负责人（复用跟进闭环的同款站内信，话术作为正文注入） */
    private void executeDunning(AiEmployeeAction action, String orgId, String userId) {
        // 两段确认的最后一段：确认执行时服务端重新校验资源更新权限（按资源类型区分合同/回款计划）
        if (AiEmployeeAction.RESOURCE_TYPE_PAYMENT_PLAN.equals(action.getResourceType())) {
            resourcePermissionService.checkResourcePermission(PermissionConstants.CONTRACT_PAYMENT_PLAN_UPDATE,
                    action.getResourceId(), FormKeyConstants.CONTRACT_PAYMENT_PLAN, userId, orgId);
        } else {
            resourcePermissionService.checkResourcePermission(PermissionConstants.CONTRACT_UPDATE,
                    action.getResourceId(), FormKeyConstants.CONTRACT, userId, orgId);
        }

        Map<String, Object> payload = JSON.parseToMap(action.getPayload());
        Object content = payload == null ? null : payload.get("dunningContent");
        String dunningContent = content == null ? null : content.toString();

        // 站内信接收人 = 资源负责人（AI 建议本就是给负责人的；含管理员代确认时也归负责人）
        String owner = StringUtils.defaultIfBlank(action.getOwnerId(), userId);
        insertNotification(orgId, owner, action.getResourceName(), dunningContent);
    }

    /** 直接落一条系统站内信（复用跟进闭环的同款范式），正文为 AI 生成的催款话术 */
    private void insertNotification(String orgId, String receiverId, String resourceName, String dunningContent) {
        long now = System.currentTimeMillis();
        String content = StringUtils.defaultIfBlank(dunningContent,
                "您负责的「" + StringUtils.defaultString(resourceName) + "」已到期/即将到期，请及时跟进催款。");
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(receiverId);
        notification.setSubject("AI 催款提醒");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(receiverId);
        notification.setOperation("AI_EMPLOYEE_ACTION");
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.CONTRACT);
        notification.setResourceId("ai_employee_action");
        notification.setResourceName("AI 催款提醒");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(receiverId);
        notification.setUpdateUser(receiverId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
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

    /** 跟进方式：中文标签映射为存储编码（1 到访 / 2 电话 / 3 微信），默认电话 */
    private String toFollowMethodCode(String label) {
        if (StringUtils.isNotBlank(label)) {
            if (label.contains("微信") || label.contains("企微") || label.contains("企业微信")) {
                return "3";
            }
            if (label.contains("到访") || label.contains("拜访") || label.contains("上门") || label.contains("面谈")) {
                return "1";
            }
        }
        return "2";
    }
}
