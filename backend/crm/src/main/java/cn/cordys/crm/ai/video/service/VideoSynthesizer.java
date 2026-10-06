package cn.cordys.crm.ai.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 视频合成（功能 7 MVP）：竖屏背景 + 封面标题卡 + 口播字幕 + 结尾卡 → mp4。
 * ffmpeg 为服务器部署依赖，本类通过 ProcessBuilder 调用；字幕走 ASS（libass）渲染，中文由字体配置提供。
 */
@Slf4j
@Component
public class VideoSynthesizer {

    /** 竖屏 9:16（抖音/视频号口播规格） */
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final int FPS = 30;
    /** 深色背景（后续可换品牌色或背景图/素材库） */
    private static final String BACKGROUND_COLOR = "0x1E1B2E";

    /** 片头标题卡与片尾卡时长（秒），期间静音 */
    private static final double TITLE_DURATION = 2.0;
    private static final double CLOSING_DURATION = 2.0;
    /** 片尾引导文案 */
    private static final String CLOSING_TEXT = "关注主页 · 了解更多";

    /** 字幕单行最大字数（1080 宽下约 14 字一行不换行） */
    private static final int SUBTITLE_MAX_CHARS_PER_LINE = 14;
    /** 标题卡单行最大字数 */
    private static final int TITLE_MAX_CHARS_PER_LINE = 10;

    /** 字幕字体族，缺省 macOS PingFang；Linux 部署需覆盖为已安装 CJK 字体（如 Noto Sans CJK SC） */
    @Value("${cordys.video.subtitle-font:PingFang SC}")
    private String subtitleFont;

    /**
     * 合成竖屏口播视频：纯色背景 + 标题卡 + 分段字幕 + 结尾卡，输出 mp4。
     *
     * @param audioFile  语音文件（wav/mp3）
     * @param outputFile 输出 mp4
     * @param title      视频标题（封面大字）
     * @param script     口播脚本正文（拆分为字幕段）
     */
    public void synthesize(File audioFile, File outputFile, String title, String script) throws Exception {
        double audioDuration = probeDuration(audioFile);
        double subtitleEnd = TITLE_DURATION + audioDuration;
        double totalDuration = subtitleEnd + CLOSING_DURATION;

        File assFile = File.createTempFile("cordys-video-subtitle-", ".ass");
        try {
            Files.writeString(assFile.toPath(), buildAss(title, script, audioDuration), StandardCharsets.UTF_8);
            runFfmpeg(audioFile, outputFile, assFile, totalDuration);
        } finally {
            assFile.delete();
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new RuntimeException("视频合成失败(未生成文件)");
        }
    }

    /** 执行 ffmpeg：背景（总时长）+ 延迟 2s 的音频 + ASS 字幕，编码 h264/aac */
    private void runFfmpeg(File audioFile, File outputFile, File assFile, double totalDuration) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add(String.format("color=c=%s:s=%dx%d:r=%d:d=%.3f",
                BACKGROUND_COLOR, WIDTH, HEIGHT, FPS, totalDuration));
        command.add("-i");
        command.add(audioFile.getAbsolutePath());
        command.add("-filter_complex");
        command.add("[1:a]adelay=" + (long) (TITLE_DURATION * 1000) + "|"
                + (long) (TITLE_DURATION * 1000) + "[a];[0:v]ass=filename=" + assFile.getAbsolutePath() + "[v]");
        command.add("-map");
        command.add("[v]");
        command.add("-map");
        command.add("[a]");
        command.add("-c:v");
        command.add("libx264");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-c:a");
        command.add("aac");
        command.add(outputFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        byte[] buffer = new byte[4096];
        try (var in = process.getInputStream()) {
            while (in.read(buffer) != -1) {
                // 丢弃 ffmpeg 日志输出，避免缓冲区写满阻塞
            }
        }
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("视频合成超时");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("视频合成失败(ffmpeg 退出码 " + process.exitValue() + ")");
        }
    }

    /**
     * 生成 ASS 字幕：标题卡(0~2s) → 口播字幕(2s 起按字数占比分摊) → 结尾卡(末 2s)。
     */
    private String buildAss(String title, String script, double audioDuration) {
        List<String> segments = splitSegments(script);
        StringBuilder sb = new StringBuilder();
        sb.append("[Script Info]\nScriptType: v4.00+\nPlayResX: ").append(WIDTH)
                .append("\nPlayResY: ").append(HEIGHT).append("\n\n");
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, "
                + "Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, "
                + "Alignment, MarginL, MarginR, MarginV, Encoding\n");
        // 白字 + 半透明黑底 + 描边；1=底部居中，5=屏幕居中
        sb.append("Style: Sub,").append(font()).append(",64,&H00FFFFFF,&H000000FF,&H00000000,&HA0000000,"
                + "0,0,0,0,100,100,0,0,1,3,1,2,60,60,160,1\n");
        sb.append("Style: Title,").append(font()).append(",96,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,"
                + "1,0,0,0,100,100,0,0,1,4,2,5,40,40,0,1\n");
        sb.append("\n[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");

        // 标题卡
        sb.append(dialogue(0, TITLE_DURATION, "Title", wrap(sanitize(title), TITLE_MAX_CHARS_PER_LINE)));

        // 口播字幕：按字数占比在 [2, 2+D) 内分摊
        double subtitleStart = TITLE_DURATION;
        double subtitleSpan = audioDuration;
        int totalChars = segments.stream().mapToInt(String::length).sum();
        double cursor = subtitleStart;
        for (String seg : segments) {
            double segDuration = subtitleSpan * seg.length() / Math.max(totalChars, 1);
            double end = Math.min(cursor + segDuration, subtitleStart + subtitleSpan);
            if (end <= cursor) {
                continue;
            }
            sb.append(dialogue(cursor, end, "Sub", wrap(seg, SUBTITLE_MAX_CHARS_PER_LINE)));
            cursor = end;
        }

        // 结尾卡
        double closingStart = TITLE_DURATION + audioDuration;
        sb.append(dialogue(closingStart, closingStart + CLOSING_DURATION,
                "Title", wrap(sanitize(CLOSING_TEXT), TITLE_MAX_CHARS_PER_LINE)));
        return sb.toString();
    }

    /** 构造一条 Dialogue 行 */
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

    private String font() {
        return subtitleFont == null || subtitleFont.isBlank() ? "PingFang SC" : subtitleFont.trim();
    }

    /** 按句末标点切分脚本，过滤空白；短句合并避免字幕闪动 */
    private List<String> splitSegments(String script) {
        String text = sanitize(script);
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] raw = text.split("(?<=[。！？!?；;])");
        List<String> merged = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        for (String s : raw) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (pending.length() + trimmed.length() <= SUBTITLE_MAX_CHARS_PER_LINE * 2 && pending.length() < SUBTITLE_MAX_CHARS_PER_LINE) {
                pending.append(trimmed);
            } else {
                if (!pending.isEmpty()) {
                    merged.add(pending.toString());
                }
                pending = new StringBuilder(trimmed);
            }
        }
        if (!pending.isEmpty()) {
            merged.add(pending.toString());
        }
        return merged;
    }

    /** 超长行手动按最大字数折行，用 ASS 换行符 \N 连接 */
    private String wrap(String text, int maxCharsPerLine) {
        if (text == null) {
            return "";
        }
        String t = text.replace("\\", "").replace("{", "（").replace("}", "）").replace("\n", "").trim();
        if (t.length() <= maxCharsPerLine) {
            return t;
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < t.length(); i += maxCharsPerLine) {
            lines.add(t.substring(i, Math.min(i + maxCharsPerLine, t.length())));
        }
        return String.join("\\N", lines);
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
            return true; // CJK 汉字
        }
        if (cp >= 0x3000 && cp <= 0x303F) {
            return true; // CJK 标点
        }
        if (cp >= 0xFF00 && cp <= 0xFFEF) {
            return true; // 全角字符/标点
        }
        if (cp >= 0x20 && cp <= 0x7E) {
            return true; // ASCII 可打印
        }
        return cp == '\n' || cp == '\r' || cp == '\t';
    }

    /** 用 ffprobe 取音频时长（秒），用于设定背景视频时长与字幕分摊 */
    private double probeDuration(File audioFile) throws Exception {
        List<String> command = List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                audioFile.getAbsolutePath());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        process.waitFor(30, TimeUnit.SECONDS);
        try {
            return Double.parseDouble(out);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无法获取语音时长: " + out);
        }
    }
}
