package cn.cordys.crm.ai.callreview.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.callreview.domain.AiCallReviewConfig;
import cn.cordys.crm.ai.callreview.dto.request.CallReviewConfigSaveRequest;
import cn.cordys.crm.ai.callreview.dto.request.CallReviewPageRequest;
import cn.cordys.crm.ai.callreview.dto.request.CallReviewUploadRequest;
import cn.cordys.crm.ai.callreview.dto.response.CallReviewConfigResponse;
import cn.cordys.crm.ai.callreview.dto.response.CallReviewDetailResponse;
import cn.cordys.crm.ai.callreview.dto.response.CallReviewResponse;
import cn.cordys.crm.ai.callreview.service.CallReviewConfigService;
import cn.cordys.crm.ai.callreview.service.CallReviewService;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能通话复盘接口（登录即可用，G3 版本开关门控）。
 */
@RestController
@RequestMapping("/agent/call-review")
@Tag(name = "智能通话复盘")
public class CallReviewController {

    @Resource
    private CallReviewService callReviewService;
    @Resource
    private CallReviewConfigService callReviewConfigService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通智能通话复盘")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_CALL_REVIEW);
    }

    @PostMapping("/page")
    @Operation(summary = "通话复盘-列表查询")
    public Pager<List<CallReviewResponse>> page(@RequestBody CallReviewPageRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return callReviewService.page(orgId, request);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "通话复盘-详情")
    public CallReviewDetailResponse detail(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return callReviewService.detail(orgId, id);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传/粘贴录音发起复盘")
    public String upload(@RequestBody CallReviewUploadRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String id = callReviewService.create(orgId, SessionUtils.getUserId(), request);
        callReviewService.transcribeAndReview(id);
        return id;
    }

    @PostMapping("/retry/{id}")
    @Operation(summary = "重试通话复盘")
    public void retry(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        callReviewService.retry(orgId, SessionUtils.getUserId(), id);
        callReviewService.transcribeAndReview(id);
    }

    @GetMapping("/config")
    @Operation(summary = "获取回调配置")
    public CallReviewConfigResponse config(HttpServletRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return callReviewConfigService.toResponse(
                callReviewConfigService.getOrCreate(orgId, SessionUtils.getUserId()), baseUrl(request));
    }

    @PostMapping("/config")
    @Operation(summary = "保存回调配置")
    public CallReviewConfigResponse saveConfig(@RequestBody CallReviewConfigSaveRequest request,
            HttpServletRequest servletRequest) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        AiCallReviewConfig config = callReviewConfigService.save(orgId, SessionUtils.getUserId(),
                request.getFieldMapping(), request.getEnable());
        return callReviewConfigService.toResponse(config, baseUrl(servletRequest));
    }

    @PostMapping("/config/reset-key")
    @Operation(summary = "重置回调密钥")
    public CallReviewConfigResponse resetKey(HttpServletRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        AiCallReviewConfig config = callReviewConfigService.resetKey(orgId, SessionUtils.getUserId());
        return callReviewConfigService.toResponse(config, baseUrl(request));
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_CALL_REVIEW)) {
            throw new GenericException("当前版本未开通智能通话复盘，请升级至专业版");
        }
    }

    /** 拼装回调地址 base：优先代理透传的 X-Forwarded-*，否则用当前请求 host */
    private String baseUrl(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        String scheme = StringUtils.isBlank(proto) ? request.getScheme() : proto.split(",")[0].trim();
        String host = request.getHeader("X-Forwarded-Host");
        if (StringUtils.isBlank(host)) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
            if (!defaultPort) {
                host = host + ":" + port;
            }
        }
        return scheme + "://" + host;
    }
}
