package cn.cordys.crm.ai.dmprofile.client;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.dmprofile.dto.DmAccountIdentity;
import cn.cordys.crm.ai.dmprofile.dto.DmCollectResult;
import cn.cordys.crm.ai.dmprofile.dto.DmRawConversation;
import cn.cordys.crm.integration.common.utils.HttpClientUtils;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 私信采集适配器客户端：调用独立 Python 微服务（16.test + scrapling，Playwright 驱动抖音网页版）。
 * 只负责「采集」——注入 Cookie / 读私信返回原始会话；画像由 CRM 侧 G1 模型层做。
 * 服务地址与内部鉴权 token 走 sys_parameter 部署时配置。
 */
@Component
@Slf4j
public class DmProfileClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 私信采集服务 baseUrl 参数 key */
    private static final String PARAM_BASE_URL = "ai.dm.baseUrl";

    /** 私信采集服务内部鉴权 token 参数 key（Python 校验 X-INTERNAL-TOKEN） */
    private static final String PARAM_INTERNAL_TOKEN = "ai.dm.internalToken";

    @Resource
    private BaseMapper<Parameter> parameterMapper;

    /**
     * 注入登录 Cookie 并校验登录态。返回登录账号「自己」的昵称与抖音号（采集服务读不到时为 null）。
     */
    public DmAccountIdentity injectCookie(String cookieText) {
        Map<String, Object> body = Map.of("cookies", cookieText);
        JsonNode resp = post("/api/login/cookie", body);
        boolean ok = resp.path("ok").asBoolean(false);
        boolean loggedIn = resp.path("logged_in").asBoolean(false);
        if (!ok || !loggedIn) {
            throw new GenericException(resp.path("message").asText("Cookie 校验未通过，请重新登录后复制"));
        }
        return new DmAccountIdentity(
                resp.path("account_name").asText(null),
                resp.path("unique_id").asText(null));
    }

    /**
     * 提交一次「仅采集」任务（异步）：Cookie 随请求体传给 Python 浏览器池，立即返回 task_id。
     * 真正的采集在 Python 侧后台 worker 执行，结果用 {@link #pollCollect(String)} 轮询。
     * 提交不重试——若服务端已入队、响应中途丢失，重试会重复入队产生冗余采集。
     */
    public String submitCollect(String cookieText) {
        JsonNode resp = post("/api/crawl/collect", Map.of("cookies", cookieText), 1);
        String taskId = resp.path("task_id").asText(null);
        if (StringUtils.isBlank(taskId)) {
            String message = resp.path("message").asText("采集服务未返回任务编号");
            throw new GenericException(message);
        }
        return taskId;
    }

    /**
     * 轮询采集任务结果。返回 {@link DmCollectResult}，由调用方按 {@code status} 判断是否继续轮询。
     */
    public DmCollectResult pollCollect(String taskId) {
        JsonNode resp = get("/api/crawl/result/" + taskId);
        DmCollectResult result = new DmCollectResult();
        result.setStatus(resp.path("status").asText("unknown"));
        result.setOk(resp.path("ok").asBoolean(false));
        result.setLoggedIn(resp.path("logged_in").asBoolean(true));
        result.setMessage(resp.path("message").asText(null));
        JsonNode convs = resp.path("convs");
        if (convs.isMissingNode() || convs.isNull()) {
            result.setConvs(List.of());
        } else {
            try {
                result.setConvs(OBJECT_MAPPER.convertValue(convs, new TypeReference<List<DmRawConversation>>() {
                }));
            } catch (Exception e) {
                log.error("解析私信采集结果失败", e);
                throw new GenericException("私信采集返回数据异常，请稍后重试");
            }
        }
        return result;
    }

    /** 发起带内部鉴权 token 的 POST，返回解析后的 JSON（默认走 HttpClientUtils 重试） */
    private JsonNode post(String path, Map<String, Object> body) {
        return post(path, body, 3);
    }

    /** 发起带内部鉴权 token 的 POST，可指定重试次数 */
    private JsonNode post(String path, Map<String, Object> body, int maxRetries) {
        String url = buildUrl(path);
        Map<String, String> headers = buildHeaders();
        String payload;
        try {
            payload = OBJECT_MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new GenericException("构造私信采集请求失败");
        }
        try {
            String respBody = HttpClientUtils.sendPostRequest(url, payload, headers, maxRetries);
            return OBJECT_MAPPER.readTree(respBody);
        } catch (Exception e) {
            log.error("调用私信采集服务失败, path={}", path, e);
            throw new GenericException("私信采集服务不可用，请检查配置或稍后重试");
        }
    }

    /** 发起带内部鉴权 token 的 GET，返回解析后的 JSON */
    private JsonNode get(String path) {
        String url = buildUrl(path);
        Map<String, String> headers = buildHeaders();
        try {
            String respBody = HttpClientUtils.sendGetRequest(url, headers);
            return OBJECT_MAPPER.readTree(respBody);
        } catch (Exception e) {
            log.error("调用私信采集服务失败, path={}", path, e);
            throw new GenericException("私信采集服务不可用，请检查配置或稍后重试");
        }
    }

    private String buildUrl(String path) {
        String baseUrl = requireParam(PARAM_BASE_URL, "请先在系统参数配置私信采集服务地址 ai.dm.baseUrl");
        return baseUrl.replaceAll("/+$", "") + path;
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String token = param(PARAM_INTERNAL_TOKEN);
        if (StringUtils.isNotBlank(token)) {
            headers.put("X-INTERNAL-TOKEN", token);
        }
        return headers;
    }

    private String requireParam(String key, String message) {
        String value = param(key);
        if (StringUtils.isBlank(value)) {
            throw new GenericException(message);
        }
        return value.trim();
    }

    private String param(String key) {
        try {
            Parameter p = parameterMapper.selectByPrimaryKey(key);
            return p == null ? null : p.getParamValue();
        } catch (Exception e) {
            log.warn("读取系统参数失败, key={}", key, e);
            return null;
        }
    }
}
