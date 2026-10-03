package cn.cordys.crm.ai.callreview.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.handler.NoResultHolder;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.crm.ai.callreview.domain.AiCallReviewConfig;
import cn.cordys.crm.ai.callreview.service.CallReviewConfigService;
import cn.cordys.crm.ai.callreview.service.CallReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * 外呼供应商通用回调接入（匿名，Shiro 白名单放行）。
 * 鉴权：HMAC-SHA256(secretKey, body + timestamp)，请求头 X-Call-Timestamp / X-Call-Signature，时间戳 ±5 分钟有效。
 * 回调 body 为供应商原始 JSON，按租户字段映射抽取 CRM 标准字段后落库并触发异步转写 + 复盘。
 */
@RestController
@RequestMapping("/open/call-review/callback")
@Tag(name = "智能通话复盘回调")
public class CallReviewCallbackController {

    private static final String HEADER_TIMESTAMP = "X-Call-Timestamp";
    private static final String HEADER_SIGNATURE = "X-Call-Signature";
    private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000L;

    @Resource
    private CallReviewConfigService callReviewConfigService;
    @Resource
    private CallReviewService callReviewService;

    @NoResultHolder
    @PostMapping("/{appKey}")
    @Operation(summary = "外呼供应商通话记录回调（HMAC 签名鉴权）")
    public Map<String, Object> callback(@PathVariable String appKey, HttpServletRequest request) throws Exception {
        AiCallReviewConfig config = callReviewConfigService.findByAppKey(appKey);
        if (config == null || !Boolean.TRUE.equals(config.getEnable())) {
            throw new GenericException(CrmHttpResultCode.UNAUTHORIZED, "回调 appKey 无效或已停用");
        }

        String body = readBody(request);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String signature = request.getHeader(HEADER_SIGNATURE);
        if (StringUtils.isBlank(timestamp) || StringUtils.isBlank(signature)) {
            throw new GenericException(CrmHttpResultCode.UNAUTHORIZED, "缺少回调签名头");
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new GenericException(CrmHttpResultCode.UNAUTHORIZED, "回调时间戳格式错误");
        }
        if (Math.abs(System.currentTimeMillis() - ts) > TIMESTAMP_TOLERANCE_MS) {
            throw new GenericException(CrmHttpResultCode.UNAUTHORIZED, "回调时间戳已过期");
        }
        if (!constantTimeEquals(hmacSha256(config.getSecretKey(), body + timestamp), signature)) {
            throw new GenericException(CrmHttpResultCode.UNAUTHORIZED, "回调签名校验失败");
        }

        String recordId = callReviewService.createByCallback(config, body);
        callReviewService.transcribeAndReview(recordId);
        return Map.of("code", 0, "message", "ok");
    }

    private String readBody(HttpServletRequest request) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
