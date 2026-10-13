package cn.cordys.crm.ai.replica.service;

import cn.cordys.crm.ai.callreview.asr.AsrSentence;
import cn.cordys.crm.ai.replica.dto.ReplicaVariantPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 爆款复刻 · 变体合成（换皮）：原视频画面 scale/crop 到目标比例 + 原音轨直通 + ASS 字幕（钩子大字卡 + 标题 + 逐字卡拉OK字幕）。
 * 画面声音 100% 复用原片，AI 只动文字层；卡拉OK用 ASS 原生 {@code \k} 标签（Primary=已唱高亮色，Secondary=未唱色），不引无头浏览器。
 */
@Slf4j
@Component
public class ReplicaVideoComposer {

    /** 字幕单行最大字数（1080 宽下约 14 字一行不换行） */
    private static final int SUBTITLE_MAX_CHARS_PER_LINE = 14;

    /** 预设字幕样式：key → (Primary 卡拉OK高亮色, Secondary 未唱底色)。颜色为 ASS BGR 格式。 */
    private record Style(String primary, String secondary) {
    }

    private static final Map<String, Style> STYLES = Map.of(
            "classic", new Style("&H00FFFFFF", "&H00FFFFFF"),
            "karaoke_yellow", new Style("&H0000D7FF", "&H00FFFFFF"),
            "karaoke_green", new Style("&H0000FF00", "&H00FFFFFF"),
            "karaoke_pink", new Style("&H0000B4FF", "&H00FFFFFF"));

    @Value("${cordys.video.subtitle-font:PingFang SC}")
    private String subtitleFont;

    /** 合成单个变体：原视频 → 目标比例 + 字幕，输出 mp4 */
    public void compose(File source, List<AsrSentence> sentences, ReplicaVariantPlan plan, File output) throws Exception {
        int[] res = resolution(plan.ratio());
        int width = res[0];
        int height = res[1];
        double total = totalDuration(sentences);
        File ass = File.createTempFile("cordys-replica-sub-", ".ass");
        Files.writeString(ass.toPath(), buildAss(sentences, plan, width, height, total), StandardCharsets.UTF_8);
        String scale = "scale=" + width + ":" + height + ":force_original_aspect_ratio=increase,crop=" + width + ":" + height;
        try {
            List<String> command = List.of(
                    "ffmpeg", "-y",
                    "-i", source.getAbsolutePath(),
                    "-vf", scale + ",ass=" + ass.getAbsolutePath(),
                    "-map", "0:v", "-map", "0:a?",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-movflags", "+faststart",
                    output.getAbsolutePath());
            runCommand(command, 10);
        } finally {
            ass.delete();
        }
    }

    private int[] resolution(String ratio) {
        if ("16:9".equals(ratio)) {
            return new int[]{1920, 1080};
        }
        if ("1:1".equals(ratio)) {
            return new int[]{1080, 1080};
        }
        return new int[]{1080, 1920};
    }

    /** 全片时长：至少 3s 保证钩子大字卡可显示，其余按最后一句字幕结束时间 */
    private double totalDuration(List<AsrSentence> sentences) {
        double max = 3.0;
        for (AsrSentence s : sentences) {
            max = Math.max(max, s.endMs() / 1000.0);
        }
        return max;
    }

    private String buildAss(List<AsrSentence> sentences, ReplicaVariantPlan plan, int width, int height, double total) {
        Style style = STYLES.getOrDefault(plan.subtitleStyle(), STYLES.get("classic"));
        StringBuilder sb = new StringBuilder();
        sb.append("[Script Info]\nScriptType: v4.00+\nPlayResX: ").append(width)
                .append("\nPlayResY: ").append(height).append("\n\n");
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, "
                + "Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, "
                + "Alignment, MarginL, MarginR, MarginV, Encoding\n");
        // Sub：卡拉OK，Primary=高亮(已唱)色，Secondary=未唱底色，底部居中
        sb.append("Style: Sub,").append(font()).append(",52,")
                .append(style.primary()).append(',').append(style.secondary())
                .append(",&H00000000,&H00000000,1,0,0,0,100,100,0,0,1,3,1,2,60,60,160,1\n");
        // Hook：开头大字钩子，屏幕居中加粗
        sb.append("Style: Hook,").append(font())
                .append(",88,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00000000,1,0,0,0,100,100,0,0,1,4,2,5,40,40,0,1\n");
        // Title：主题标签，顶部居中，全片常驻
        sb.append("Style: Title,").append(font())
                .append(",36,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,8,40,40,20,1\n");
        sb.append("\n[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");

        String hook = sanitize(plan.hookText());
        if (!hook.isEmpty()) {
            sb.append(dialogue(0, Math.min(3.0, total), "Hook", "{\\fad(300,300)}" + wrap(hook, 8)));
        }
        String title = sanitize(plan.titleText());
        if (!title.isEmpty()) {
            sb.append(dialogue(0, total, "Title", wrap(title, 12)));
        }
        for (AsrSentence s : sentences) {
            String karaoke = karaokeLine(s);
            if (!karaoke.isEmpty()) {
                sb.append(dialogue(s.beginMs() / 1000.0, s.endMs() / 1000.0, "Sub", karaoke));
            }
        }
        return sb.toString();
    }

    private String dialogue(double start, double end, String style, String text) {
        return "Dialogue: 0," + assTime(start) + "," + assTime(end) + "," + style + ",,0,0,0,," + text + "\n";
    }

    /** 秒 → ASS 时间戳 H:MM:SS.cc */
    private String assTime(double seconds) {
        int total = (int) Math.round(seconds * 100);
        int cs = total % 100;
        int sec = total / 100;
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        return String.format("%d:%02d:%02d.%02d", h, m, s, cs);
    }

    /** 一句字幕：按句内字数占比分配字级 {@code \k} 时间，每 14 字折行，逐字卡拉OK高亮 */
    private String karaokeLine(AsrSentence s) {
        String text = sanitize(s.text()).replace(" ", "").replace("　", "");
        if (text.isEmpty()) {
            return "";
        }
        long totalCs = Math.max(1, (s.endMs() - s.beginMs()) / 10);
        int[] cps = text.codePoints().toArray();
        long perChar = Math.max(1, totalCs / cps.length);
        long spent = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.length; i++) {
            if (i > 0 && i % SUBTITLE_MAX_CHARS_PER_LINE == 0) {
                sb.append("\\N");
            }
            long cs = (i == cps.length - 1) ? Math.max(1, totalCs - spent) : perChar;
            spent += cs;
            sb.append("{\\k").append(cs).append('}');
            sb.append(new String(Character.toChars(cps[i])));
        }
        return sb.toString();
    }

    /** 超长行按最大字数折行，用 ASS 换行符 \N 连接 */
    private String wrap(String text, int maxCharsPerLine) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String t = text.replace("\\", "").replace("{", "（").replace("}", "）").replace("\n", "").trim();
        if (t.codePointCount(0, t.length()) <= maxCharsPerLine) {
            return t;
        }
        StringBuilder sb = new StringBuilder();
        int[] cps = t.codePoints().toArray();
        for (int i = 0; i < cps.length; i++) {
            if (i > 0 && i % maxCharsPerLine == 0) {
                sb.append("\\N");
            }
            sb.append(new String(Character.toChars(cps[i])));
        }
        return sb.toString();
    }

    /** 过滤 emoji 等无法渲染的字符，仅保留中文、中文标点、全角与 ASCII 可打印字符 */
    private String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        s.codePoints().forEach(cp -> {
            if (isSupported(cp)) {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }

    private boolean isSupported(int cp) {
        if (cp >= 0x4E00 && cp <= 0x9FFF) {
            return true;
        }
        if (cp >= 0x3000 && cp <= 0x303F) {
            return true;
        }
        if (cp >= 0xFF00 && cp <= 0xFFEF) {
            return true;
        }
        if (cp >= 0x20 && cp <= 0x7E) {
            return true;
        }
        return cp == '\n' || cp == '\r' || cp == '\t';
    }

    private String font() {
        return subtitleFont == null || subtitleFont.isBlank() ? "PingFang SC" : subtitleFont.trim();
    }

    /** 执行外部命令（ffmpeg），丢弃输出防阻塞，超时强杀，非零退出码抛异常 */
    private void runCommand(List<String> command, int timeoutMinutes) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        byte[] buffer = new byte[4096];
        try (var in = process.getInputStream()) {
            while (in.read(buffer) != -1) {
                // 丢弃日志输出，避免缓冲区写满阻塞
            }
        }
        boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException(command.get(0) + " 执行超时");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException(command.get(0) + " 执行失败(退出码 " + process.exitValue() + ")");
        }
    }
}
