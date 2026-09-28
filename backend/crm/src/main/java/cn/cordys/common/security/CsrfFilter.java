package cn.cordys.common.security;

import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.security.SessionConstants;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;

import java.io.IOException;

import static cn.cordys.security.SessionUser.getRandomAlphabetic;

/**
 * 自定义过滤器，用于处理 CSRF 校验。
 * 该过滤器确保只有认证用户才能访问非公开资源，并对请求中的 CSRF token 和 Referer 进行校验。
 */
public class CsrfFilter extends AnonymousFilter {

    /**
     * 在处理请求之前进行 CSRF 校验。
     * 如果请求没有通过认证或请求头中包含有效的 CSRF token，则允许请求继续。
     * 否则，抛出相应的异常或返回无效的认证状态。
     *
     * @param request     Servlet 请求
     * @param response    Servlet 响应
     * @param mappedValue 过滤器链中的映射值
     *
     * @return true 如果请求继续处理，false 如果请求被中断
     */
    @Override
    protected boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpServletRequest = WebUtils.toHttp(request);

        // 如果用户未认证，返回认证无效状态
        if (!SecurityUtils.getSubject().isAuthenticated()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            // 单点登录：旧会话被新登录踢掉后，残留请求走到这里。从 CSRF token 解密出 userId，
            // 若该用户存在被踢标记，则直接返回 401 + code 100461，前端弹「该账号已在其他设备登录」。
            String kickedUserId = decryptCsrfUserId(httpServletRequest);
            if (kickedUserId != null && SessionUtils.isKicked(kickedUserId)) {
                writeKickedResponse(httpResponse);
                return false;
            }
            httpResponse.setHeader(SessionConstants.AUTHENTICATION_STATUS, SessionConstants.AUTHENTICATION_INVALID);
            return true;
        }

        // 错误页面无需 CSRF 校验
        if (httpServletRequest.getRequestURI().equals("/error")) {
            return true;
        }

        // API 请求无需 CSRF 校验
        if (ApiKeyHandler.isApiKeyCall(httpServletRequest)) {
            return true;
        }

        // WebSocket 请求无需 CSRF 校验
        String websocketKey = httpServletRequest.getHeader("Sec-WebSocket-Key");
        if (StringUtils.isNotBlank(websocketKey)) {
            return true;
        }

        // 允许 Swagger UI 访问，无需 CSRF 校验
        if (Strings.CS.startsWithAny(httpServletRequest.getRequestURI(), "/swagger-ui", "/v3/api-docs")) {
            return true;
        }

        // 获取请求头中的 CSRF token 和 X-Auth-Token
        String csrfToken = httpServletRequest.getHeader(SessionConstants.CSRF_TOKEN);
        String xAuthToken = httpServletRequest.getHeader(SessionConstants.HEADER_TOKEN);

        // 校验 CSRF token 和 X-Auth-Token
        validateToken(csrfToken, xAuthToken);

        // 校验 Referer
        validateReferer(httpServletRequest);

        return true;
    }

    /**
     * 校验请求中的 Referer 是否符合配置的域名要求。
     * 如果没有配置 Referer 域名，默认不进行校验。
     *
     * @param request HttpServletRequest 请求
     */
    private void validateReferer(HttpServletRequest request) {
        Environment env = CommonBeanFactory.getBean(Environment.class);
        assert env != null;
        String domains = env.getProperty("referer.urls");

        // 如果没有配置 referer.urls，则不校验
        if (StringUtils.isBlank(domains)) {
            return;
        }

        String[] allowedDomains = StringUtils.split(domains, ",");
        String referer = request.getHeader(HttpHeaders.REFERER);

        // 校验 Referer 是否在允许的域名列表中
        if (allowedDomains != null && !ArrayUtils.contains(allowedDomains, referer)) {
            throw new RuntimeException("CSRF error: invalid referer");
        }
    }

    /**
     * 校验请求中的 CSRF token 和 X-Auth-Token 是否有效。
     * 如果 token 无效，抛出异常。
     *
     * @param csrfToken  CSRF token
     * @param xAuthToken X-Auth-Token
     */
    private void validateToken(String csrfToken, String xAuthToken) {
        if (StringUtils.isBlank(csrfToken)) {
            throw new RuntimeException("CSRF token is empty");
        }

        // 解密 CSRF token
        csrfToken = CodingUtils.aesDecrypt(csrfToken, SessionUser.secret, CodingUtils.generateIv());

        String[] signatureArray = StringUtils.split(StringUtils.trimToNull(csrfToken), "|");
        if (signatureArray.length != 4) {
            throw new RuntimeException("Invalid CSRF token format");
        }

        // 校验用户 ID 和会话 ID 是否匹配
        if (!Strings.CS.equals(SessionUtils.getUserId(), signatureArray[0])) {
            throw new RuntimeException("CSRF token does not match the current user");
        }

        // 校验 sessionId 或 X-Auth-Token 是否匹配
        if (!Strings.CS.equals(SessionUtils.getSessionId(), signatureArray[2]) &&
                !Strings.CS.equals(xAuthToken, signatureArray[2])) {
            throw new RuntimeException("CSRF token does not match the current session");
        }
    }

    /**
     * 从 CSRF token 解密出 userId（用于单点登录被踢检测）。
     * 解密失败（匿名请求、非法 token）时静默返回 null，避免影响正常未认证流程。
     *
     * @param request HttpServletRequest
     *
     * @return userId，无法解析时返回 null
     */
    private String decryptCsrfUserId(HttpServletRequest request) {
        String csrfToken = request.getHeader(SessionConstants.CSRF_TOKEN);
        if (StringUtils.isBlank(csrfToken)) {
            return null;
        }
        try {
            String decrypted = CodingUtils.aesDecrypt(csrfToken, SessionUser.secret, CodingUtils.generateIv());
            String[] parts = StringUtils.split(StringUtils.trimToNull(decrypted), "|");
            if (parts != null && parts.length >= 1) {
                return parts[0];
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 单点登录被踢：写死 HTTP 401 + body code=100461（不走异常处理器，避免 code%1000 把状态码映射成 461）。
     *
     * @param response HttpServletResponse
     */
    private void writeKickedResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(SessionConstants.AUTHENTICATION_STATUS, SessionConstants.AUTHENTICATION_INVALID);
        String body = "{\"code\":" + CrmHttpResultCode.KICKED_OUT.getCode()
                + ",\"message\":\"" + Translator.get("account.kicked.other.device") + "\"}";
        try {
            response.getWriter().write(body);
        } catch (IOException e) {
            // 响应写入失败不抛异常，避免掩盖被踢语义
        }
    }
}
