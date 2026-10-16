package cn.cordys.crm.dashboard.supervision.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.dashboard.mapper.ExtBusinessScreenMapper;
import cn.cordys.crm.dashboard.supervision.domain.SupervisionTask;
import cn.cordys.crm.dashboard.supervision.dto.request.SupervisionCreateRequest;
import cn.cordys.crm.dashboard.supervision.dto.response.AssigneeOption;
import cn.cordys.crm.dashboard.supervision.dto.response.SupervisionBoardResponse;
import cn.cordys.crm.dashboard.supervision.dto.response.SupervisionItem;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 经营大屏「督办闭环」：下达督办（落表 + 站内信通知被督办人）、督办看板列表、老板标记完成。
 * 第一版由老板手动标记完成，被督办人侧完成确认后续再做。
 */
@Service
@Slf4j
public class SupervisionService {

    private static final String OPERATION_CREATE = "SUPERVISION_CREATE";

    @Resource
    private BaseMapper<SupervisionTask> supervisionTaskMapper;
    @Resource
    private BaseMapper<Notification> notificationMapper;
    @Resource
    private ExtBusinessScreenMapper extBusinessScreenMapper;
    @Resource
    private BaseService baseService;

    public SupervisionItem create(String orgId, String operatorId, SupervisionCreateRequest request) {
        if (StringUtils.isBlank(request.getContent())) {
            throw new GenericException("请填写督办内容");
        }
        if (StringUtils.isBlank(request.getAssignee())) {
            throw new GenericException("请选择被督办人");
        }

        long now = System.currentTimeMillis();
        String assigneeName = resolveName(request.getAssignee());

        SupervisionTask task = new SupervisionTask();
        task.setId(IDGenerator.nextStr());
        task.setOrganizationId(orgId);
        task.setAssignee(request.getAssignee());
        task.setAssigneeName(assigneeName);
        task.setContent(request.getContent().trim());
        task.setDeadline(request.getDeadline());
        task.setStatus(SupervisionTask.STATUS_PENDING);
        task.setCreateUser(operatorId);
        task.setUpdateUser(operatorId);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        supervisionTaskMapper.insert(task);

        notifyAssignee(orgId, operatorId, request.getAssignee(), request.getContent().trim(), task.getId());
        return toItem(task);
    }

    public SupervisionBoardResponse board(String orgId) {
        List<SupervisionTask> tasks = supervisionTaskMapper.selectListByLambda(
                new LambdaQueryWrapper<SupervisionTask>()
                        .eq(SupervisionTask::getOrganizationId, orgId)
                        .orderByDesc(SupervisionTask::getCreateTime));

        SupervisionBoardResponse response = new SupervisionBoardResponse();
        long now = System.currentTimeMillis();
        for (SupervisionTask task : tasks) {
            response.getItems().add(toItem(task, now));
        }
        response.setAssignees(assignees(orgId));
        return response;
    }

    public void markDone(String orgId, String operatorId, String id) {
        SupervisionTask task = supervisionTaskMapper.selectByPrimaryKey(id);
        if (task == null || !orgId.equals(task.getOrganizationId())) {
            throw new GenericException("督办任务不存在");
        }
        if (SupervisionTask.STATUS_DONE.equals(task.getStatus())) {
            return;
        }
        task.setStatus(SupervisionTask.STATUS_DONE);
        task.setUpdateUser(operatorId);
        task.setUpdateTime(System.currentTimeMillis());
        supervisionTaskMapper.updateById(task);
    }

    private List<AssigneeOption> assignees(String orgId) {
        List<String> ownerIds = extBusinessScreenMapper.selectOwnerIds(orgId);
        if (ownerIds == null || ownerIds.isEmpty()) {
            return List.of();
        }
        Map<String, String> nameMap = baseService.getUserNameMap(ownerIds);
        return ownerIds.stream()
                .map(owner -> {
                    AssigneeOption option = new AssigneeOption();
                    option.setOwner(owner);
                    option.setOwnerName(baseService.getAndCheckOptionName(nameMap.get(owner)));
                    return option;
                })
                .sorted(Comparator.comparing(AssigneeOption::getOwnerName))
                .toList();
    }

    private String resolveName(String userId) {
        Map<String, String> nameMap = baseService.getUserNameMap(List.of(userId));
        String name = baseService.getAndCheckOptionName(nameMap.get(userId));
        return StringUtils.isBlank(name) ? userId : name;
    }

    /** 站内信通知被督办人。第一版直插 sys_notification，避免新增消息事件种子。 */
    private void notifyAssignee(String orgId, String operatorId, String assignee, String content, String taskId) {
        long now = System.currentTimeMillis();
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(assignee);
        notification.setSubject("收到一条督办任务");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(operatorId);
        notification.setOperation(OPERATION_CREATE);
        notification.setOrganizationId(orgId);
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId(taskId);
        notification.setResourceName("督办任务");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(operatorId);
        notification.setUpdateUser(operatorId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }

    private SupervisionItem toItem(SupervisionTask task) {
        return toItem(task, System.currentTimeMillis());
    }

    /** 转条目并在内存里把已过截止时间的待办标记为逾期展示（落库由定时任务负责）。 */
    private SupervisionItem toItem(SupervisionTask task, long now) {
        SupervisionItem item = new SupervisionItem();
        item.setId(task.getId());
        item.setAssignee(task.getAssignee());
        item.setAssigneeName(task.getAssigneeName());
        item.setContent(task.getContent());
        item.setDeadline(task.getDeadline());
        item.setStatus(task.getStatus());
        item.setCreateUser(task.getCreateUser());
        item.setCreateTime(task.getCreateTime());
        if (SupervisionTask.STATUS_PENDING.equals(item.getStatus())
                && item.getDeadline() != null && item.getDeadline() < now) {
            item.setStatus(SupervisionTask.STATUS_OVERDUE);
        }
        return item;
    }
}
