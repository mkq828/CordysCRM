package cn.cordys.crm.ai.voiceclone.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.video.tts.TtsConfig;
import cn.cordys.crm.ai.video.tts.TtsProvider;
import cn.cordys.crm.ai.video.tts.TtsProviderFactory;
import cn.cordys.crm.ai.voiceclone.domain.AiCloneVoice;
import cn.cordys.crm.ai.voiceclone.dto.request.CloneVoiceSubmitRequest;
import cn.cordys.crm.ai.voiceclone.dto.response.CloneVoicePreviewResponse;
import cn.cordys.crm.ai.voiceclone.dto.response.CloneVoiceResponse;
import cn.cordys.crm.ai.voiceclone.provider.VoiceCloneProvider;
import cn.cordys.crm.ai.voiceclone.provider.VoiceCloneProviderFactory;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 声音克隆（功能 11）：上传样本音频 → 阿里云百炼 Qwen-TTS 声音复刻（同步返回 voice_id）→ 成功后作为配音音色复用。
 * 异步执行照 {@code CallReviewService.transcribeAndReview} 的 {@code @Async} 样板，
 * 由 Controller 跨 Bean 触发保证 @Async 生效；复刻接口同步完成，无需训练轮询。
 */
@Slf4j
@Service
public class CloneVoiceService {

    private static final String PROVIDER_NAME = "阿里云";
    private static final String STATUS_TRAINING = "TRAINING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private static final String PREVIEW_TEXT = "大家好，这是克隆音色的试听效果，用于短视频配音讲解。";

    @Resource
    private BaseMapper<AiCloneVoice> voiceMapper;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private VoiceCloneProviderFactory voiceCloneProviderFactory;
    @Resource
    private TtsProviderFactory ttsProviderFactory;
    @Resource
    private AttachmentService attachmentService;

    /** 提交复刻：校验后落库 TRAINING 并立即返回，真正复刻由 Controller 跨 Bean 触发 {@link #trainAsync} */
    public CloneVoiceResponse submit(CloneVoiceSubmitRequest request, String orgId, String userId) {
        String name = StringUtils.trimToNull(request.getName());
        if (name == null) {
            throw new GenericException("请填写音色名称");
        }
        if (StringUtils.isBlank(request.getSampleAttachmentId())) {
            throw new GenericException("请上传样本音频");
        }
        long now = System.currentTimeMillis();
        AiCloneVoice voice = new AiCloneVoice();
        voice.setId(IDGenerator.nextStr());
        voice.setOrganizationId(orgId);
        voice.setName(name);
        voice.setProvider(PROVIDER_NAME);
        // voice_id 由厂商返回，提交时留空，复刻成功后回填
        voice.setStatus(STATUS_TRAINING);
        voice.setSampleAttachmentId(request.getSampleAttachmentId());
        voice.setCreateUser(userId);
        voice.setUpdateUser(userId);
        voice.setCreateTime(now);
        voice.setUpdateTime(now);
        voiceMapper.insert(voice);
        return toResponse(voice);
    }

    /** 异步复刻主流程：读样本 → 同步克隆 → 回填 voice_id 与状态 */
    @Async
    public void trainAsync(String id) {
        AiCloneVoice voice = voiceMapper.selectByPrimaryKey(id);
        if (voice == null) {
            return;
        }
        String orgId = voice.getOrganizationId();
        try {
            String apiKey = agentModelService.resolveProviderApiKey(orgId, PROVIDER_NAME, "声音克隆");
            String cloneModel = agentModelService.resolveVoiceCloneModel(orgId);
            VoiceCloneProvider provider = voiceCloneProviderFactory.get(PROVIDER_NAME);
            SampleAudio sample = readSample(voice.getSampleAttachmentId());
            String voiceId = provider.clone(sample.bytes(), sample.format(), generatePreferredName(), cloneModel, apiKey);
            voice.setSpeakerId(voiceId);
            voice.setStatus(STATUS_SUCCESS);
            voice.setErrorMsg(null);
            voice.setUpdateTime(System.currentTimeMillis());
            voiceMapper.updateById(voice);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "声音克隆失败，请稍后重试" : e.getMessage();
            log.error("声音克隆失败, id={}, orgId={}", id, orgId, e);
            voice.setStatus(STATUS_FAILED);
            voice.setErrorMsg(message.length() > 500 ? message.substring(0, 500) : message);
            voice.setUpdateTime(System.currentTimeMillis());
            voiceMapper.updateById(voice);
        }
    }

    public List<CloneVoiceResponse> list(String orgId) {
        return voiceMapper.selectListByLambda(new LambdaQueryWrapper<AiCloneVoice>()
                        .eq(AiCloneVoice::getOrganizationId, orgId)
                        .orderByDesc(AiCloneVoice::getCreateTime)).stream()
                .map(this::toResponse)
                .toList();
    }

    public CloneVoiceResponse get(String orgId, String id) {
        return toResponse(requireVoice(orgId, id));
    }

    public void delete(String orgId, String id) {
        AiCloneVoice voice = requireVoice(orgId, id);
        if (StringUtils.isNotBlank(voice.getSampleAttachmentId())) {
            attachmentService.delete(voice.getSampleAttachmentId());
        }
        voiceMapper.deleteByPrimaryKey(id);
    }

    /** 试听：仅复刻成功可试听，用该音色（voice_id）走阿里云 TTS 合成样例，返回 base64 WAV */
    public CloneVoicePreviewResponse preview(String orgId, String id, String text) {
        AiCloneVoice voice = requireVoice(orgId, id);
        if (!STATUS_SUCCESS.equals(voice.getStatus())) {
            throw new GenericException("音色尚未复刻完成，无法试听");
        }
        String previewText = StringUtils.isNotBlank(text) ? text : PREVIEW_TEXT;
        String cloneModel = agentModelService.resolveVoiceCloneModel(orgId);
        for (AgentModel model : agentModelService.resolveAliyunTtsModels(orgId)) {
            try {
                TtsConfig config = new TtsConfig();
                config.setModel(model.getModelName());
                config.setApiKey(model.getApiKey());
                config.setModelParams(model.getModelParams());
                config.setVoiceCloneModel(cloneModel);
                TtsProvider tts = ttsProviderFactory.get(model.getProvider());
                byte[] audio = tts.synthesize(previewText, voice.getSpeakerId(), config);
                CloneVoicePreviewResponse response = new CloneVoicePreviewResponse();
                response.setAudio(Base64.getEncoder().encodeToString(audio));
                return response;
            } catch (Exception e) {
                log.warn("克隆音色试听失败，降级尝试下一个配音模型, provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        throw new GenericException("试听生成失败，请确认已配置阿里云配音模型");
    }

    /** 读临时附件样本字节，并按 Content-Type 解析音频格式（供厂商接口提交） */
    private SampleAudio readSample(String attachmentId) throws Exception {
        ResponseEntity<org.springframework.core.io.Resource> res = attachmentService.getResource(attachmentId);
        if (res == null || res.getBody() == null) {
            throw new GenericException("样本音频不存在，请重新上传");
        }
        String format = resolveFormat(res.getHeaders().getContentType());
        try (InputStream in = res.getBody().getInputStream()) {
            return new SampleAudio(in.readAllBytes(), format);
        }
    }

    private String resolveFormat(MediaType mediaType) {
        if (mediaType == null) {
            return "wav";
        }
        String sub = mediaType.getSubtype().toLowerCase();
        if ("mpeg".equals(sub) || "mp3".equals(sub)) {
            return "mp3";
        }
        if ("mp4".equals(sub)) {
            return "m4a";
        }
        if ("ogg".equals(sub)) {
            return "ogg";
        }
        if ("aac".equals(sub)) {
            return "aac";
        }
        return "wav";
    }

    /** 厂商音色命名关键词：仅字母数字、最多 10 字符（v + 8 位 hex = 9 字符） */
    private String generatePreferredName() {
        return "v" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private AiCloneVoice requireVoice(String orgId, String id) {
        AiCloneVoice voice = voiceMapper.selectByPrimaryKey(id);
        if (voice == null || !orgId.equals(voice.getOrganizationId())) {
            throw new GenericException("音色不存在");
        }
        return voice;
    }

    private CloneVoiceResponse toResponse(AiCloneVoice voice) {
        CloneVoiceResponse response = new CloneVoiceResponse();
        response.setId(voice.getId());
        response.setName(voice.getName());
        response.setProvider(voice.getProvider());
        response.setSpeakerId(voice.getSpeakerId());
        response.setStatus(voice.getStatus());
        response.setSampleAttachmentId(voice.getSampleAttachmentId());
        response.setErrorMsg(voice.getErrorMsg());
        response.setCreateTime(voice.getCreateTime());
        return response;
    }

    private record SampleAudio(byte[] bytes, String format) {
    }
}
