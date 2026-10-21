package cn.cordys.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.MDC;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 全局 ERROR 日志采集：把 ERROR 级日志塞进有界内存队列，由 SysErrorLogService 后台线程批量落库 sys_error_log。
 * Appender 在 Spring 容器就绪前即被 logback 实例化，故只入队、不碰数据库，避免初始化鸡生蛋。
 * 级别过滤由 logback-spring.xml 的 LevelFilter 保证，这里只做防自激判断。
 */
public class ErrorLogAppender extends AppenderBase<ILoggingEvent> {

    /** 全局队列，跨 Spring 生命周期共享；满则丢弃最新一条，避免撑爆内存。 */
    public static final BlockingQueue<ErrorRecord> QUEUE = new ArrayBlockingQueue<>(2000);

    /** 标记消费线程，其自身打出的 ERROR 不再入队，防落库失败引发自激循环。 */
    public static final ThreadLocal<Boolean> PUMP_THREAD = new ThreadLocal<>();

    @Override
    protected void append(ILoggingEvent event) {
        if (Boolean.TRUE.equals(PUMP_THREAD.get())) {
            return;
        }
        String stack = null;
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            stack = ThrowableProxyUtil.asString(throwable);
        }
        QUEUE.offer(new ErrorRecord(event.getTimeStamp(), event.getLoggerName(),
                event.getFormattedMessage(), stack, MDC.get("traceId")));
    }

    /** 一条错误记录的最小载体。 */
    public record ErrorRecord(long timestamp, String loggerName, String message, String stackTrace, String traceId) {
    }
}
