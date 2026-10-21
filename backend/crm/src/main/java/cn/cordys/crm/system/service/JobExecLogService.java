package cn.cordys.crm.system.service;

import cn.cordys.common.pager.Pager;
import cn.cordys.crm.system.domain.JobExecLog;
import cn.cordys.crm.system.dto.request.JobExecLogRequest;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务执行日志服务：两阶段落库 + admin 分页查询。
 */
@Service
public class JobExecLogService {

    /** 任务开始时落 RUNNING 记录。 */
    public void recordStart(JobExecLog log) {
        DataAccessLayer.with(JobExecLog.class).insert(log);
    }

    /** 任务结束时更新为 SUCCESS/FAILED，回填结束时间与耗时。 */
    public void recordEnd(String id, Long endTime, Long durationMs, String status, String errorMsg) {
        JobExecLog update = new JobExecLog();
        update.setId(id);
        update.setEndTime(endTime);
        update.setDurationMs(durationMs);
        update.setStatus(status);
        update.setErrorMsg(errorMsg);
        DataAccessLayer.with(JobExecLog.class).updateById(update);
    }

    /** 全量查询（报警 job 用，按开始时间倒序）。 */
    public List<JobExecLog> listAll() {
        return DataAccessLayer.with(JobExecLog.class)
                .selectListByLambda(new LambdaQueryWrapper<JobExecLog>()
                        .orderByDesc(JobExecLog::getStartTime));
    }

    /** 分页查询（内存分页，平台侧与 SecurityEventService 同款做法）。 */
    public Pager<List<JobExecLog>> list(JobExecLogRequest request) {
        List<JobExecLog> filtered = filter(request);
        int page = request.getCurrent() <= 0 ? 1 : request.getCurrent();
        int size = request.getPageSize() <= 0 ? 20 : request.getPageSize();
        int total = filtered.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return new Pager<>(filtered.subList(from, to), total, size, page);
    }

    private List<JobExecLog> filter(JobExecLogRequest request) {
        return listAll().stream()
                .filter(e -> StringUtils.isBlank(request.getJobName())
                        || (e.getJobName() != null && e.getJobName().contains(request.getJobName())))
                .filter(e -> StringUtils.isBlank(request.getStatus())
                        || request.getStatus().equals(e.getStatus()))
                .filter(e -> request.getStartTime() == null
                        || (e.getStartTime() != null && e.getStartTime() >= request.getStartTime()))
                .filter(e -> request.getEndTime() == null
                        || (e.getStartTime() != null && e.getStartTime() <= request.getEndTime()))
                .toList();
    }
}
