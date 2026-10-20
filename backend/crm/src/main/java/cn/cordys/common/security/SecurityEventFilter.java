package cn.cordys.common.security;

import cn.cordys.common.context.TraceIdWebFilter;
import cn.cordys.common.utils.IpRegionService;
import cn.cordys.common.utils.IpUtils;
import cn.cordys.crm.system.domain.SecurityEvent;
import cn.cordys.crm.system.service.SecurityEventService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全事件留证过滤器。识别疑似恶意请求（注入/穿越/XSS/扫描器/高频错误），
 * 命中只落库留证、不拦截，避免误杀正常业务。证据用于报案取证。
 */
@Slf4j
public class SecurityEventFilter extends OncePerRequestFilter {

    private static final int URI_MAX = 2048;
    private static final int FIELD_MAX = 512;
    private static final int ERROR_FLOOD_THRESHOLD = 30;
    private static final long ERROR_FLOOD_WINDOW_MS = 60_000;
    private static final int ERROR_FLOOD_MAP_MAX = 10_000;

    private final SecurityEventService securityEventService;
    private final IpRegionService ipRegionService;

    // shortcut: 内存滑动窗口统计高频错误，进程重启即清零、多实例不共享，规模化时换 Redis 计数
    private final ConcurrentHashMap<String, long[]> errorWindows = new ConcurrentHashMap<>();

    public SecurityEventFilter(SecurityEventService securityEventService, IpRegionService ipRegionService) {
        this.securityEventService = securityEventService;
        this.ipRegionService = ipRegionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        StatusCaptureResponseWrapper wrapped = new StatusCaptureResponseWrapper(response);
        try {
            chain.doFilter(request, wrapped);
        } finally {
            detectAndRecord(request, wrapped.getStatus());
        }
    }

    private void detectAndRecord(HttpServletRequest request, int status) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");

        SecurityEventDetector.Rule rule = SecurityEventDetector.detect(path, query, userAgent);
        String remoteAddr = request.getRemoteAddr();
        boolean flood = isErrorFlood(remoteAddr, status);

        if (rule == null && !flood) {
            return;
        }

        SecurityEvent event = new SecurityEvent();
        event.setEventType(rule != null ? rule.eventType : "error_flood");
        event.setDetail(rule != null ? rule.detail : "同 IP 短时间高频错误状态码，疑似扫描或爆破");
        event.setRemoteAddr(truncate(remoteAddr, 64));
        String clientIp = IpUtils.getClientIpAddress(request);
        event.setClientIp(truncate(clientIp, 64));
        event.setForwardChain(truncate(request.getHeader("X-Forwarded-For"), FIELD_MAX));
        event.setIpRegion(resolveRegion(remoteAddr, clientIp));
        event.setMethod(request.getMethod());
        event.setUri(truncate(StringUtils.isBlank(query) ? path : path + "?" + query, URI_MAX));
        event.setUserAgent(truncate(userAgent, FIELD_MAX));
        event.setReferer(truncate(request.getHeader("Referer"), FIELD_MAX));
        event.setHttpStatus(status);
        event.setTraceId(StringUtils.firstNonBlank(MDC.get(TraceIdWebFilter.MDC_KEY),
                request.getHeader(TraceIdWebFilter.TRACE_ID_HEADER)));
        event.setCreateTime(System.currentTimeMillis());
        securityEventService.record(event);
    }

    /**
     * 同一真实来源 IP 在时间窗内错误状态码达到阈值则记一次，随后重置窗口避免刷屏。
     * 依赖 remoteAddr 为真实客户端 IP；反向代理部署需配 real_ip，否则会以代理 IP 聚合。
     */
    private boolean isErrorFlood(String remoteAddr, int status) {
        if (status < 400 || StringUtils.isBlank(remoteAddr)) {
            return false;
        }
        if (errorWindows.size() > ERROR_FLOOD_MAP_MAX) {
            errorWindows.clear();
        }
        long now = System.currentTimeMillis();
        long[] window = errorWindows.compute(remoteAddr, (k, v) -> {
            if (v == null || now - v[0] > ERROR_FLOOD_WINDOW_MS) {
                return new long[]{now, 1};
            }
            v[1]++;
            return v;
        });
        if (window[1] < ERROR_FLOOD_THRESHOLD) {
            return false;
        }
        errorWindows.put(remoteAddr, new long[]{now, 0});
        return true;
    }

    private String resolveRegion(String remoteAddr, String clientIp) {
        String region = ipRegionService.searchCity(remoteAddr);
        return region != null ? region : ipRegionService.searchCity(clientIp);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static final class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {

        private int status = HttpServletResponse.SC_OK;

        StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.status = HttpServletResponse.SC_MOVED_TEMPORARILY;
            super.sendRedirect(location);
        }

        @Override
        public int getStatus() {
            return this.status;
        }
    }
}
