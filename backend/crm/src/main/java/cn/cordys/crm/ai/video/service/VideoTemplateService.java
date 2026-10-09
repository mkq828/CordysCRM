package cn.cordys.crm.ai.video.service;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.video.domain.AiVideoTemplate;
import cn.cordys.crm.ai.video.domain.VideoTemplateConfig;
import cn.cordys.crm.ai.video.dto.response.VideoTemplateOption;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频模板库目录：读取启用的预置模板，并把 config_json 解析为 {@link VideoTemplateConfig} 供合成/生成消费。
 * 模板暂为系统预置（V1.41.0 种子），后续「爆款复刻」可动态插入新模板。
 */
@Slf4j
@Service
public class VideoTemplateService {

    /** 默认模板标识（请求未带模板/模板未启用时回退），对应种子模板 default-talk */
    public static final String DEFAULT_TEMPLATE_KEY = "default-talk";

    @Resource
    private BaseMapper<AiVideoTemplate> aiVideoTemplateMapper;

    /** 前端模板选择卡片列表（仅启用，按 sort 升序） */
    public List<VideoTemplateOption> listOptions() {
        LambdaQueryWrapper<AiVideoTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiVideoTemplate::getEnabled, 1);
        wrapper.orderByAsc(AiVideoTemplate::getSort);
        return aiVideoTemplateMapper.selectListByLambda(wrapper).stream()
                .map(this::toOption)
                .toList();
    }

    /** 解析模板配置；模板标识为空/未启用/缺失时回退默认配置，保证无模板也能成片 */
    public VideoTemplateConfig resolveConfig(String templateKey) {
        AiVideoTemplate template = findEnabled(templateKey);
        if (template == null) {
            return VideoTemplateConfig.defaults();
        }
        return parseConfig(template.getConfigJson());
    }

    /** 模板展示名（回填任务 style 字段，供列表/详情展示）；无模板回退默认名 */
    public String resolveName(String templateKey) {
        AiVideoTemplate template = findEnabled(templateKey);
        return template == null ? "口播" : template.getName();
    }

    /** 规范化模板标识：为空/未启用时回退默认模板，保证任务始终引用一个可用模板 */
    public String resolveKey(String templateKey) {
        return findEnabled(templateKey) == null ? DEFAULT_TEMPLATE_KEY : templateKey.trim();
    }

    private AiVideoTemplate findEnabled(String templateKey) {
        if (StringUtils.isBlank(templateKey)) {
            return null;
        }
        List<AiVideoTemplate> list = aiVideoTemplateMapper.selectListByLambda(new LambdaQueryWrapper<AiVideoTemplate>()
                .eq(AiVideoTemplate::getTemplateKey, templateKey)
                .eq(AiVideoTemplate::getEnabled, 1));
        return list.isEmpty() ? null : list.get(0);
    }

    private VideoTemplateOption toOption(AiVideoTemplate t) {
        VideoTemplateOption option = new VideoTemplateOption();
        option.setTemplateKey(t.getTemplateKey());
        option.setName(t.getName());
        option.setDescription(t.getDescription());
        option.setColor(t.getColor());
        option.setSoftColor(t.getSoftColor());
        // 封面预览用：把 config_json 的底色/结尾卡文案带上，前端据此渲染「视频海报」式封面
        VideoTemplateConfig cfg = parseConfig(t.getConfigJson());
        option.setBackground(toCssHex(cfg.background()));
        option.setClosingText(cfg.closingText());
        return option;
    }

    /** 把 config 里的 "0xRRGGBB" 转成 CSS "#RRGGBB"；空值回退默认深底 */
    private String toCssHex(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "#1E1B2E";
        }
        String s = raw.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        return s.startsWith("#") ? s : "#" + s;
    }

    /** 解析 config_json，字段缺失时回退默认值，避免历史/不完整配置导致合成异常 */
    private VideoTemplateConfig parseConfig(String json) {
        VideoTemplateConfig d = VideoTemplateConfig.defaults();
        if (StringUtils.isBlank(json)) {
            return d;
        }
        try {
            JsonNode n = JSON.MAPPER.readTree(json);
            return new VideoTemplateConfig(
                    text(n, "background", d.background()),
                    intVal(n, "titleFontSize", d.titleFontSize()),
                    text(n, "titleColor", d.titleColor()),
                    intVal(n, "subtitleFontSize", d.subtitleFontSize()),
                    text(n, "subtitleColor", d.subtitleColor()),
                    text(n, "subtitleOutline", d.subtitleOutline()),
                    text(n, "subtitleShadow", d.subtitleShadow()),
                    boolVal(n, "subtitleCenter", d.subtitleCenter()),
                    text(n, "closingText", d.closingText()),
                    text(n, "transition", d.transition()),
                    blankToNull(text(n, "bgmFile", null)),
                    doubleVal(n, "bgmVolume", d.bgmVolume()),
                    text(n, "scriptStyle", d.scriptStyle()),
                    text(n, "imageStyle", d.imageStyle()));
        } catch (Exception e) {
            log.warn("视频模板 config_json 解析失败，回退默认配置", e);
            return d;
        }
    }

    private String text(JsonNode n, String field, String fallback) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() || StringUtils.isBlank(v.asText()) ? fallback : v.asText().trim();
    }

    private int intVal(JsonNode n, String field, int fallback) {
        JsonNode v = n.get(field);
        return v == null || !v.canConvertToInt() ? fallback : v.asInt();
    }

    private boolean boolVal(JsonNode n, String field, boolean fallback) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? fallback : v.asBoolean();
    }

    private double doubleVal(JsonNode n, String field, double fallback) {
        JsonNode v = n.get(field);
        return v == null || !v.isNumber() ? fallback : v.asDouble();
    }

    private String blankToNull(String s) {
        return StringUtils.isBlank(s) ? null : s;
    }
}
