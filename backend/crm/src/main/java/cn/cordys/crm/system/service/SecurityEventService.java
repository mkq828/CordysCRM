package cn.cordys.crm.system.service;

import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.SecurityEvent;
import cn.cordys.crm.system.dto.request.SecurityEventRequest;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 安全事件留证服务：异步落库 + admin 分页查询/导出。
 */
@Service
public class SecurityEventService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private BaseMapper<SecurityEvent> securityEventMapper;

    @Async
    public void record(SecurityEvent event) {
        event.setId(IDGenerator.nextStr());
        if (event.getCreateTime() == null) {
            event.setCreateTime(System.currentTimeMillis());
        }
        securityEventMapper.insert(event);
    }

    /**
     * 分页查询（内存分页，平台侧服务与 PlatformContractService 同款做法）。
     */
    public Pager<List<SecurityEvent>> list(SecurityEventRequest request) {
        List<SecurityEvent> filtered = filter(request);
        int page = request.getCurrent() <= 0 ? 1 : request.getCurrent();
        int size = request.getPageSize() <= 0 ? 20 : request.getPageSize();
        int total = filtered.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return new Pager<>(filtered.subList(from, to), total, size, page);
    }

    /**
     * 导出用全量查询（不过滤分页）。
     */
    public List<SecurityEvent> listForExport(SecurityEventRequest request) {
        return filter(request);
    }

    private List<SecurityEvent> filter(SecurityEventRequest request) {
        List<SecurityEvent> all = DataAccessLayer.with(SecurityEvent.class)
                .selectListByLambda(new LambdaQueryWrapper<SecurityEvent>()
                        .orderByDesc(SecurityEvent::getCreateTime));
        return all.stream()
                .filter(e -> StringUtils.isBlank(request.getEventType())
                        || request.getEventType().equals(e.getEventType()))
                .filter(e -> StringUtils.isBlank(request.getIp())
                        || (e.getRemoteAddr() != null && e.getRemoteAddr().contains(request.getIp()))
                        || (e.getClientIp() != null && e.getClientIp().contains(request.getIp())))
                .filter(e -> request.getStartTime() == null
                        || (e.getCreateTime() != null && e.getCreateTime() >= request.getStartTime()))
                .filter(e -> request.getEndTime() == null
                        || (e.getCreateTime() != null && e.getCreateTime() <= request.getEndTime()))
                .toList();
    }

    /**
     * 生成 CSV（字段转义 + 时间转本地可读格式），供报案取证。
     */
    public String toCsv(List<SecurityEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("事件类型,事件描述,直连IP,客户端IP,转发链,IP归属地,HTTP方法,请求URI,User-Agent,Referer,状态码,traceId,租户ID,时间\n");
        for (SecurityEvent e : events) {
            sb.append(csvLine(e.getEventType(), e.getDetail(), e.getRemoteAddr(), e.getClientIp(),
                    e.getForwardChain(), e.getIpRegion(), e.getMethod(), e.getUri(), e.getUserAgent(),
                    e.getReferer(), e.getHttpStatus(), e.getTraceId(), e.getOrganizationId(),
                    e.getCreateTime() == null ? "" : formatTime(e.getCreateTime())));
        }
        return sb.toString();
    }

    private String csvLine(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (Object v : values) {
            String s = v == null ? "" : String.valueOf(v).replace("\r", " ").replace("\n", " ");
            sb.append('"').append(s.replace("\"", "\"\"")).append('"').append(',');
        }
        sb.setLength(sb.length() - 1);
        sb.append('\n');
        return sb.toString();
    }

    private String formatTime(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(TIME_FORMAT);
    }
}
