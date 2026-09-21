package cn.cordys.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端工具类
 */
public class ServletUtils {
    /**
     * @param request 请求
     *
     * @return ua
     */
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "";
    }

    /**
     * 获得请求
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return getUserAgent(request);
    }

    public static String getRequestHost(HttpServletRequest request) {
        String port = ":" + request.getServerPort();
        if (request.getServerPort() == 80 || request.getServerPort() == 443) {
            port = "";
        }
        return request.getScheme() + "://" + request.getServerName() + port;
    }

    public static String getUrl() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }

        return getRequestHost(request);
    }

    /**
     * 获取客户端 IP，优先取 X-Forwarded-For 首段，否则取 remoteAddr。
     *
     * @param request 请求
     *
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
