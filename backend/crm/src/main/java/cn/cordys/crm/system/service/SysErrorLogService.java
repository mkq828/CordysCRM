package cn.cordys.crm.system.service;

import cn.cordys.common.log.ErrorLogAppender;
import cn.cordys.common.log.ErrorLogAppender.ErrorRecord;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.SysErrorLog;
import cn.cordys.crm.system.dto.request.SysErrorLogRequest;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 系统错误日志服务：后台线程消费 ErrorLogAppender 队列批量落库 + admin 分页查询。
 */
@Service
@Slf4j
public class SysErrorLogService {

    private static final int BATCH_SIZE = 200;
    private static final int MESSAGE_MAX = 4000;

    @Resource
    private BaseMapper<SysErrorLog> sysErrorLogMapper;

    private volatile boolean running = true;
    private Thread pumpThread;

    @PostConstruct
    public void startPump() {
        pumpThread = new Thread(this::pump, "error-log-pump");
        pumpThread.setDaemon(true);
        pumpThread.start();
    }

    @PreDestroy
    public void stopPump() {
        running = false;
        if (pumpThread != null) {
            pumpThread.interrupt();
        }
    }

    private void pump() {
        ErrorLogAppender.PUMP_THREAD.set(true);
        BlockingQueue<ErrorRecord> queue = ErrorLogAppender.QUEUE;
        List<ErrorRecord> batch = new ArrayList<>(BATCH_SIZE);
        while (running) {
            try {
                ErrorRecord first = queue.poll(1, TimeUnit.SECONDS);
                if (first == null) {
                    continue;
                }
                batch.clear();
                batch.add(first);
                queue.drainTo(batch, BATCH_SIZE - 1);
                for (ErrorRecord record : batch) {
                    try {
                        insert(record);
                    } catch (Exception e) {
                        // warn 不进 ERROR 队列，防落库失败自激
                        log.warn("系统错误日志落库失败", e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 停机前尽力排空一次
        batch.clear();
        queue.drainTo(batch);
        for (ErrorRecord record : batch) {
            try {
                insert(record);
            } catch (Exception ignored) {
                // 停机排空失败无需处理
            }
        }
    }

    private void insert(ErrorRecord record) {
        SysErrorLog error = new SysErrorLog();
        error.setId(IDGenerator.nextStr());
        error.setTraceId(truncate(record.traceId(), 64));
        error.setLoggerName(truncate(record.loggerName(), 255));
        error.setMessage(truncate(record.message(), MESSAGE_MAX));
        error.setStackTrace(record.stackTrace());
        error.setCreateTime(record.timestamp());
        sysErrorLogMapper.insert(error);
    }

    /** 分页查询（内存分页，平台侧与 SecurityEventService 同款做法）。 */
    public Pager<List<SysErrorLog>> list(SysErrorLogRequest request) {
        List<SysErrorLog> filtered = filter(request);
        int page = request.getCurrent() <= 0 ? 1 : request.getCurrent();
        int size = request.getPageSize() <= 0 ? 20 : request.getPageSize();
        int total = filtered.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return new Pager<>(filtered.subList(from, to), total, size, page);
    }

    private List<SysErrorLog> filter(SysErrorLogRequest request) {
        return DataAccessLayer.with(SysErrorLog.class)
                .selectListByLambda(new LambdaQueryWrapper<SysErrorLog>()
                        .orderByDesc(SysErrorLog::getCreateTime)).stream()
                .filter(e -> StringUtils.isBlank(request.getLoggerName())
                        || (e.getLoggerName() != null && e.getLoggerName().contains(request.getLoggerName())))
                .filter(e -> StringUtils.isBlank(request.getTraceId())
                        || request.getTraceId().equals(e.getTraceId()))
                .filter(e -> StringUtils.isBlank(request.getKeyword())
                        || (e.getMessage() != null && e.getMessage().contains(request.getKeyword())))
                .filter(e -> request.getStartTime() == null
                        || (e.getCreateTime() != null && e.getCreateTime() >= request.getStartTime()))
                .filter(e -> request.getEndTime() == null
                        || (e.getCreateTime() != null && e.getCreateTime() <= request.getEndTime()))
                .toList();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
