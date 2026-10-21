package cn.cordys.common.schedule;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.JobExecLog;
import cn.cordys.crm.system.service.JobExecLogService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.EverythingMatcher;
import org.springframework.stereotype.Component;

/**
 * 全局 Quartz JobListener：把每个任务的起止/耗时/成败落到 sys_job_exec_log。
 * 一处注册覆盖所有任务，业务代码零改动。两阶段：jobToBeExecuted 落 RUNNING，jobWasExecuted 更新终态。
 */
@Component
@Slf4j
public class JobExecLogListener implements JobListener {

    public static final String NAME = "jobExecLogListener";
    private static final String CTX_LOG_ID = "jobExecLogId";
    private static final String CTX_START_TIME = "jobExecStartTime";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Resource
    private Scheduler scheduler;

    @Resource
    private JobExecLogService jobExecLogService;

    @PostConstruct
    public void register() throws SchedulerException {
        scheduler.getListenerManager().addJobListener(this, EverythingMatcher.allJobs());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            JobKey key = context.getJobDetail().getKey();
            long startTime = System.currentTimeMillis();
            JobExecLog log = new JobExecLog();
            log.setId(IDGenerator.nextStr());
            log.setJobName(resolveJobName(context));
            log.setJobGroup(key.getGroup());
            log.setStatus(STATUS_RUNNING);
            log.setStartTime(startTime);
            log.setCreateTime(startTime);
            jobExecLogService.recordStart(log);
            context.put(CTX_LOG_ID, log.getId());
            context.put(CTX_START_TIME, startTime);
        } catch (Exception e) {
            log.error("记录任务执行开始失败，job={}", context.getJobDetail().getKey(), e);
        }
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            Object idObj = context.get(CTX_LOG_ID);
            if (idObj == null) {
                return;
            }
            long endTime = System.currentTimeMillis();
            Object startObj = context.get(CTX_START_TIME);
            long startTime = startObj instanceof Long ? (Long) startObj : endTime;
            String status = jobException == null ? STATUS_SUCCESS : STATUS_FAILED;
            String errorMsg = jobException == null ? null : jobException.getMessage();
            jobExecLogService.recordEnd(String.valueOf(idObj), endTime, endTime - startTime, status, errorMsg);
        } catch (Exception e) {
            log.error("记录任务执行结束失败", e);
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // 任务被否决不执行，无需记录
    }

    /** 任务名：Quartz JobKey name 是生成的 UUID，真正的可读标识在 JobDataMap 的 targetObject(bean 名)+targetMethod(方法名) 里。 */
    private String resolveJobName(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String targetObject = dataMap.getString("targetObject");
        String targetMethod = dataMap.getString("targetMethod");
        if (targetObject != null && targetMethod != null) {
            return targetObject + "#" + targetMethod;
        }
        if (targetObject != null) {
            return targetObject;
        }
        return context.getJobDetail().getKey().getName();
    }
}
