package cn.cordys.crm.ai.callreview.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.callreview.CallReviewConstants;
import cn.cordys.crm.ai.callreview.asr.AsrProvider;
import cn.cordys.crm.ai.callreview.asr.AsrProviderFactory;
import cn.cordys.crm.ai.callreview.domain.AiCallRecord;
import cn.cordys.crm.ai.callreview.domain.AiCallReviewConfig;
import cn.cordys.crm.ai.callreview.dto.request.CallReviewPageRequest;
import cn.cordys.crm.ai.callreview.dto.request.CallReviewUploadRequest;
import cn.cordys.crm.ai.callreview.dto.response.CallReviewDetailResponse;
import cn.cordys.crm.ai.callreview.dto.response.CallReviewResponse;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.request.SalesAdvisorAnalyzeRequest;
import cn.cordys.crm.ai.dto.response.SalesAdvisorAnalyzeResponse;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.SalesAdvisorService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能通话复盘（功能 9 · 第一版）：录音回流（外呼回调 / 手动上传）→ 语音转写（阿里云 paraformer-v2）
 * → 复用会话军师分析链路出复盘 → 关联客户时沉淀客户画像。
 * <p>
 * 转写不挂 G2 token 额度（阿里云按音频时长外部计费）；AI 复盘这步走 {@code SalesAdvisorService.analyze}
 * 的额度→模型→provider→计费链路，按 {@code ai_call_review} 记账。
 * </p>
 */
@Service
@Slf4j
public class CallReviewService {

    private static final String ASR_PROVIDER = "阿里云";
    private static final int ASR_MAX_POLL = 300;
    private static final long ASR_POLL_INTERVAL_MS = 2000L;

    @Resource
    private BaseMapper<AiCallRecord> recordMapper;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private AsrProviderFactory asrProviderFactory;
    @Resource
    private AttachmentService attachmentService;
    @Resource
    private SalesAdvisorService salesAdvisorService;
    @Resource
    private CustomerService customerService;

    // ==================== 查询 ====================

    public Pager<List<CallReviewResponse>> page(String orgId, CallReviewPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<AiCallRecord> wrapper = new LambdaQueryWrapper<AiCallRecord>()
                .eq(AiCallRecord::getOrganizationId, orgId)
                .orderByDesc(AiCallRecord::getCreateTime);
        if (StringUtils.isNotBlank(request.getStatus())) {
            wrapper.eq(AiCallRecord::getStatus, request.getStatus());
        }
        List<CallReviewResponse> list = recordMapper.selectListByLambda(wrapper).stream()
                .map(this::toResponse)
                .toList();
        return PageUtils.setPageInfo(page, list);
    }

    public CallReviewDetailResponse detail(String orgId, String id) {
        AiCallRecord record = require(orgId, id);
        CallReviewDetailResponse detail = new CallReviewDetailResponse();
        BeanUtils.copyProperties(record, detail);
        detail.setCustomerName(resolveCustomerName(record.getCustomerId()));
        detail.setReview(parseReview(record.getReviewJson()));
        return detail;
    }

    // ==================== 写入 ====================

    /** 手动上传/粘贴录音发起复盘：落库后异步转写 + 复盘 */
    public String create(String orgId, String userId, CallReviewUploadRequest request) {
        if (StringUtils.isBlank(request.getRecordUrl()) && StringUtils.isBlank(request.getRecordAttachmentId())) {
            throw new GenericException("请上传录音文件或填写录音地址");
        }
        long now = System.currentTimeMillis();
        AiCallRecord record = new AiCallRecord();
        record.setId(IDGenerator.nextStr());
        record.setOrganizationId(orgId);
        record.setCaller(StringUtils.trimToNull(request.getCaller()));
        record.setCallee(StringUtils.trimToNull(request.getCallee()));
        record.setCustomerPhone(StringUtils.trimToNull(request.getCustomerPhone()));
        record.setCustomerId(StringUtils.trimToNull(request.getCustomerId()));
        record.setCallTime(request.getCallTime() == null ? now : request.getCallTime());
        record.setDuration(request.getDuration());
        record.setRecordUrl(StringUtils.trimToNull(request.getRecordUrl()));
        record.setRecordAttachmentId(StringUtils.trimToNull(request.getRecordAttachmentId()));
        record.setStatus(CallReviewConstants.STATUS_PENDING_TRANSCRIBE);
        record.setCreateUser(userId);
        record.setUpdateUser(userId);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        recordMapper.insert(record);
        return record.getId();
    }

    /** 重试：把失败/卡住的记录重置为待转写，重新触发异步链路 */
    public void retry(String orgId, String userId, String id) {
        AiCallRecord record = require(orgId, id);
        record.setStatus(CallReviewConstants.STATUS_PENDING_TRANSCRIBE);
        record.setErrorMsg(null);
        record.setUpdateUser(userId);
        record.setUpdateTime(System.currentTimeMillis());
        recordMapper.updateById(record);
    }

    /** 一键转跟进成功后记录跟进记录ID，详情据此禁用重复转跟进按钮 */
    public void markFollowed(String orgId, String userId, String id, String followRecordId) {
        if (StringUtils.isBlank(followRecordId)) {
            throw new GenericException("跟进记录ID不能为空");
        }
        AiCallRecord record = require(orgId, id);
        record.setFollowRecordId(followRecordId);
        record.setUpdateUser(userId);
        record.setUpdateTime(System.currentTimeMillis());
        recordMapper.updateById(record);
    }

    /** 外呼回调接入：按字段映射从供应商 body 抽取标准字段落库（录音地址缺失则拒绝） */
    public String createByCallback(AiCallReviewConfig config, String body) {
        Map<String, Object> payload = JSON.parseToMap(body);
        Map<String, String> mapping = parseFieldMapping(config.getFieldMapping());

        AiCallRecord record = new AiCallRecord();
        record.setId(IDGenerator.nextStr());
        record.setOrganizationId(config.getOrganizationId());
        record.setSupplier("外呼系统");
        record.setCaller(strField(payload, mapping, CallReviewConstants.FIELD_CALLER));
        record.setCallee(strField(payload, mapping, CallReviewConstants.FIELD_CALLEE));
        record.setCustomerPhone(strField(payload, mapping, CallReviewConstants.FIELD_CUSTOMER_PHONE));
        record.setCallTime(longField(payload, mapping, CallReviewConstants.FIELD_CALL_TIME));
        record.setDuration(intField(payload, mapping, CallReviewConstants.FIELD_DURATION));
        record.setRecordUrl(strField(payload, mapping, CallReviewConstants.FIELD_RECORD_URL));
        if (StringUtils.isBlank(record.getRecordUrl())) {
            throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED, "回调缺少录音地址字段，请检查字段映射");
        }
        long now = System.currentTimeMillis();
        record.setStatus(CallReviewConstants.STATUS_PENDING_TRANSCRIBE);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        recordMapper.insert(record);
        return record.getId();
    }

    private Map<String, String> parseFieldMapping(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> mapping = JSON.parseObject(json, new TypeReference<Map<String, String>>() { });
            return mapping == null ? new LinkedHashMap<>() : mapping;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String strField(Map<String, Object> payload, Map<String, String> mapping, String standardField) {
        String supplierField = mapping.get(standardField);
        if (StringUtils.isBlank(supplierField)) {
            return null;
        }
        Object value = payload.get(supplierField);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long longField(Map<String, Object> payload, Map<String, String> mapping, String standardField) {
        String value = strField(payload, mapping, standardField);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intField(Map<String, Object> payload, Map<String, String> mapping, String standardField) {
        String value = strField(payload, mapping, standardField);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 异步转写 + 复盘 ====================

    /**
     * 转写 + 复盘主流程（异步，由 Controller 跨 Bean 触发保证 @Async 生效）。
     * 状态机：PENDING_TRANSCRIBE → TRANSCRIBING → ANALYZING → DONE / FAILED。
     */
    @Async
    public void transcribeAndReview(String recordId) {
        AiCallRecord record = recordMapper.selectByPrimaryKey(recordId);
        if (record == null) {
            return;
        }
        String orgId = record.getOrganizationId();
        try {
            mark(record, CallReviewConstants.STATUS_TRANSCRIBING, null, null);

            String apiKey = agentModelService.resolveProviderApiKey(orgId, ASR_PROVIDER);
            AsrProvider provider = asrProviderFactory.get(ASR_PROVIDER);
            String fileUrl = resolveFileUrl(record, provider, apiKey);

            String taskId = provider.submit(fileUrl, apiKey);
            mark(record, CallReviewConstants.STATUS_TRANSCRIBING, taskId, null);

            String transcript = pollTranscript(provider, taskId, apiKey);
            if (StringUtils.isBlank(transcript)) {
                throw new GenericException("语音转写结果为空");
            }
            mark(record, CallReviewConstants.STATUS_ANALYZING, taskId, transcript);

            SalesAdvisorAnalyzeRequest analyzeRequest = new SalesAdvisorAnalyzeRequest();
            analyzeRequest.setMessage(transcript);
            analyzeRequest.setCustomerId(record.getCustomerId());
            SalesAdvisorAnalyzeResponse review = salesAdvisorService.analyze(orgId, analyzeRequest,
                    AiQuotaConstant.AI_CALL_REVIEW);

            record.setReviewJson(JSON.toJSONString(review));
            record.setStatus(CallReviewConstants.STATUS_DONE);
            record.setErrorMsg(null);
            record.setUpdateTime(System.currentTimeMillis());
            recordMapper.updateById(record);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "通话复盘失败，请稍后重试" : e.getMessage();
            log.error("通话复盘失败, recordId={}, orgId={}", recordId, orgId, e);
            record.setStatus(CallReviewConstants.STATUS_FAILED);
            record.setErrorMsg(message.length() > 500 ? message.substring(0, 500) : message);
            record.setUpdateTime(System.currentTimeMillis());
            recordMapper.updateById(record);
        }
    }

    /** 轮询转写任务直至完成或超时 */
    private String pollTranscript(AsrProvider provider, String taskId, String apiKey) throws Exception {
        for (int i = 0; i < ASR_MAX_POLL; i++) {
            String text = provider.poll(taskId, apiKey);
            if (text != null) {
                return text;
            }
            Thread.sleep(ASR_POLL_INTERVAL_MS);
        }
        throw new GenericException("语音转写超时，请稍后重试");
    }

    /**
     * 确定转写文件地址：优先录音公网 URL；本地附件由 provider 决定小文件走 data: base64 URL、
     * 大文件走平台临时上传（oss://），支持到 200MB 级别的长录音。
     */
    private String resolveFileUrl(AiCallRecord record, AsrProvider provider, String apiKey) throws Exception {
        if (StringUtils.isNotBlank(record.getRecordUrl())) {
            return record.getRecordUrl();
        }
        ResponseEntity<org.springframework.core.io.Resource> res = attachmentService.getResource(record.getRecordAttachmentId());
        if (res == null || res.getBody() == null) {
            throw new GenericException("录音附件不存在，请重新上传");
        }
        byte[] bytes;
        try (InputStream in = res.getBody().getInputStream()) {
            bytes = in.readAllBytes();
        }
        MediaType mediaType = res.getHeaders().getContentType();
        String mime = mediaType == null ? "audio/mpeg" : mediaType.getType() + "/" + mediaType.getSubtype();
        String fileName = resolveAttachmentFileName(res, mime);
        return provider.resolveLocalFileUrl(bytes, fileName, mime, apiKey);
    }

    /** 从附件响应头解析原始文件名（保留扩展名供 OSS 上传识别音频格式），解析不到时按 MIME 兜底 */
    private String resolveAttachmentFileName(ResponseEntity<?> res, String mime) {
        String disposition = res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String name = null;
        if (StringUtils.isNotBlank(disposition)) {
            int idx = disposition.indexOf("filename*=UTF-8''");
            if (idx >= 0) {
                name = URLDecoder.decode(disposition.substring(idx + "filename*=UTF-8''".length()).split(";")[0].trim(),
                        StandardCharsets.UTF_8);
            } else {
                int fn = disposition.indexOf("filename=");
                if (fn >= 0) {
                    name = disposition.substring(fn + "filename=".length()).split(";")[0].trim().replace("\"", "");
                }
            }
        }
        if (StringUtils.isBlank(name)) {
            name = "recording." + (mime.contains("mpeg") ? "mp3" : mime.substring(mime.indexOf('/') + 1));
        }
        return name;
    }

    private void mark(AiCallRecord record, String status, String taskId, String transcript) {
        record.setStatus(status);
        if (taskId != null) {
            record.setAsrTaskId(taskId);
        }
        if (transcript != null) {
            record.setTranscript(transcript);
        }
        record.setUpdateTime(System.currentTimeMillis());
        recordMapper.updateById(record);
    }

    // ==================== 通用 ====================

    private AiCallRecord require(String orgId, String id) {
        AiCallRecord record = recordMapper.selectByPrimaryKey(id);
        if (record == null || !orgId.equals(record.getOrganizationId())) {
            throw new GenericException("通话记录不存在");
        }
        return record;
    }

    private CallReviewResponse toResponse(AiCallRecord record) {
        CallReviewResponse response = new CallReviewResponse();
        BeanUtils.copyProperties(record, response);
        response.setCustomerName(resolveCustomerName(record.getCustomerId()));
        return response;
    }

    private String resolveCustomerName(String customerId) {
        if (StringUtils.isBlank(customerId)) {
            return null;
        }
        try {
            return customerService.getCustomerName(customerId);
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseReview(String reviewJson) {
        if (StringUtils.isBlank(reviewJson)) {
            return null;
        }
        try {
            return JSON.parseObject(reviewJson);
        } catch (Exception e) {
            log.warn("解析通话复盘结果失败", e);
            return null;
        }
    }
}
