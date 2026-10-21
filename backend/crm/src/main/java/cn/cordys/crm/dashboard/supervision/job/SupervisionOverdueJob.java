package cn.cordys.crm.dashboard.supervision.job;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.dashboard.supervision.domain.SupervisionTask;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 督办逾期检查：每天 8 点把已过截止时间的待办督办翻为 OVERDUE，并站内信提醒下达人（老板）。
 * 无登录态，直接按任务自身携带的 organization_id / create_user 发通知。
 */
@Component
@Slf4j
public class SupervisionOverdueJob {

    private static final String OPERATION_OVERDUE = "SUPERVISION_OVERDUE";

    @Resource
    private BaseMapper<SupervisionTask> supervisionTaskMapper;
    @Resource
    private BaseMapper<Notification> notificationMapper;

    @QuartzScheduled(cron = "0 0 8 * * ?")
    public void onEvent() {
        try {
            long now = System.currentTimeMillis();
            List<SupervisionTask> overdueTasks = supervisionTaskMapper.selectListByLambda(
                    new LambdaQueryWrapper<SupervisionTask>()
                            .eq(SupervisionTask::getStatus, SupervisionTask.STATUS_PENDING)
                            .ltT(SupervisionTask::getDeadline, now));
            for (SupervisionTask task : overdueTasks) {
                task.setStatus(SupervisionTask.STATUS_OVERDUE);
                task.setUpdateTime(now);
                supervisionTaskMapper.updateById(task);
                notifyBoss(task, now);
            }
            if (!overdueTasks.isEmpty()) {
                log.info("督办逾期检查完成，共标记 {} 条逾期", overdueTasks.size());
            }
        } catch (Exception e) {
            log.error("督办逾期检查执行失败", e);
        }
    }

    /** 站内信提醒下达人（老板）。 */
    private void notifyBoss(SupervisionTask task, long now) {
        if (StringUtils.isBlank(task.getCreateUser())) {
            return;
        }
        String content = "督办任务已逾期：" + task.getContent();
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(task.getCreateUser());
        notification.setSubject("督办任务已逾期");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(task.getCreateUser());
        notification.setOperation(OPERATION_OVERDUE);
        notification.setOrganizationId(task.getOrganizationId());
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId(task.getId());
        notification.setResourceName("督办任务已逾期");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(task.getCreateUser());
        notification.setUpdateUser(task.getCreateUser());
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }
}
