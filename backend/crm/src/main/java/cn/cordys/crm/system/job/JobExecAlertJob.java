package cn.cordys.crm.system.job;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.JobExecLog;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.service.JobExecLogService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 定时任务执行异常报警：每天 8:30 检查当天 FAILED 或长时间未结束(RUNNING 超过 30 分钟)的任务，
 * 有异常则给平台超级管理员发站内信。
 */
@Component
@Slf4j
public class JobExecAlertJob {

    private static final long RUNNING_TIMEOUT_MS = 30 * 60 * 1000L;

    @Resource
    private JobExecLogService jobExecLogService;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @QuartzScheduled(cron = "0 30 8 * * ?")
    public void onEvent() {
        try {
            long now = System.currentTimeMillis();
            long todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            List<JobExecLog> abnormal = jobExecLogService.listAll().stream()
                    .filter(l -> l.getStartTime() != null && l.getStartTime() >= todayStart)
                    .filter(l -> "FAILED".equals(l.getStatus())
                            || ("RUNNING".equals(l.getStatus()) && now - l.getStartTime() > RUNNING_TIMEOUT_MS))
                    .toList();
            if (abnormal.isEmpty()) {
                return;
            }
            StringBuilder sb = new StringBuilder("今日定时任务执行异常，请前往「系统设置-定时任务执行记录」查看：");
            for (JobExecLog item : abnormal) {
                sb.append("\n· ").append(item.getJobName())
                        .append("（").append("FAILED".equals(item.getStatus()) ? "失败" : "未完成").append("）");
            }
            sendAdminNotice(sb.toString());
        } catch (Exception e) {
            log.error("定时任务执行异常报警失败", e);
        }
    }

    private void sendAdminNotice(String content) {
        long now = System.currentTimeMillis();
        String admin = InternalUser.ADMIN.getValue();
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(admin);
        notification.setSubject("定时任务执行异常提醒");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(admin);
        notification.setOperation("JOB_EXEC_ALERT");
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceId("job_exec_log");
        notification.setResourceName("定时任务执行监控");
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(admin);
        notification.setUpdateUser(admin);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }
}
