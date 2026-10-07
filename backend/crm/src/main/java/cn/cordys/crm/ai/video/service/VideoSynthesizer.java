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
 * FFmpeg 视频合成（功能 7）：竖屏背景 + 封面标题卡 + 分镜配图（Ken Burns 推拉）+ 口播字幕 + 结尾卡 → mp4。
 * ffmpeg 为服务器部署依赖，本类通过 ProcessBuilder 调用；字幕走 ASS（libass）渲染，中文由字体配置提供。
 * 有配图的分镜用 zoompan 做缓慢推拉，无配图的分镜回退纯色背景（配图模型未配置或生成失败时也能成片）。
 */
@Slf4j
@Component
public class VideoSynthesizer {

    /** 竖屏 9:16（抖音/视频号口播规格） */
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final int FPS = 30;
    /** 深色背景（无配图分镜与片头片尾的底色） */
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

    /** 一帧画面：口播文本 + 可选配图（无图则纯色背景） */
    public static class Scene {
        private final String text;
        private final File image;

        public Scene(String text, File image) {
            this.text = text == null ? "" : text;
            this.image = image;
        }

        public String text() {
            return text;
        }

        public File image() {
            return image;
        }
    }

    /** 合成时的一帧画面（按字数占比分摊后的时长） */
    private record SceneTiming(String text, File image, double duration) {}

    /**
     * 合成竖屏口播视频：标题卡 + 分镜（配图 Ken Burns 或纯色）+ 字幕 + 结尾卡，输出 mp4。
     */
    public void synthesize(File audioFile, File outputFile, String title, List<Scene> scenes) throws Exception {
        double audioDuration = probeDuration(audioFile);
        List<SceneTiming> timings = computeTimings(scenes, audioDuration);

        File assFile = File.createTempFile("cordys-video-subtitle-", ".ass");
        try {
            Files.writeString(assFile.toPath(), buildAss(title, timings, audioDuration), StandardCharsets.UTF_8);
            runFfmpeg(audioFile, outputFile, assFile, timings);
        } finally {
            assFile.delete();
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new RuntimeException("视频合成失败(未生成文件)");
        }
    }

    /** 每个分镜时长：按字数占比分摊整段音频 */
    private List<SceneTiming> computeTimings(List<Scene> scenes, double audioDuration) {
        if (scenes == null || scenes.isEmpty()) {
            return List.of();
        }
        List<Scene> body = scenes.stream().filter(s -> !s.text().isBlank()).toList();
        int totalChars = body.stream().mapToInt(s -> s.text().length()).sum();
        List<SceneTiming> result = new ArrayList<>();
        for (Scene s : body) {
            double d = totalChars <= 0 ? audioDuration / body.size()
                    : audioDuration * s.text().length() / totalChars;
            result.add(new SceneTiming(s.text(), s.image(), Math.max(d, 0.05)));
        }
        return result;
    }

    /** 执行 ffmpeg：片头纯色 + 每个分镜（配图 zoompan 或纯色）+ 片尾纯色 concat，叠加 ASS 字幕与延迟音频 */
    private void runFfmpeg(File audioFile, File outputFile, File assFile, List<SceneTiming> timings) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");

        List<String> chains = new ArrayList<>();
        List<String> concatParts = new ArrayList<>();
        int idx = 0;

        // 片头标题卡：纯色 2s
        command.addAll(List.of("-f", "lavfi", "-i", colorSource(TITLE_DURATION)));
        chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        concatParts.add("[t" + idx + "]");
        idx++;

        // 分镜：有配图走 zoompan 推拉，无配图回退纯色
        for (SceneTiming t : timings) {
            if (t.image() != null && t.image().exists()) {
                command.add("-i");
                command.add(t.image().getAbsolutePath());
                int frames = Math.max(1, (int) Math.round(t.duration() * FPS));
                chains.add("[" + idx + ":v]scale=" + WIDTH + ":" + HEIGHT + ":force_original_aspect_ratio=increase,"
                        + "crop=" + WIDTH + ":" + HEIGHT + ","
                        + "zoompan=z='min(zoom+0.0006,1.15)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':"
                        + "d=" + frames + ":s=" + WIDTH + "x" + HEIGHT + ":fps=" + FPS + ","
                        + "format=yuv420p,setpts=PTS-STARTPTS[v" + idx + "]");
                concatParts.add("[v" + idx + "]");
            } else {
                command.addAll(List.of("-f", "lavfi", "-i", colorSource(t.duration())));
                chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[v" + idx + "]");
                concatParts.add("[v" + idx + "]");
            }
            idx++;
        }

        // 片尾卡：纯色 2s
        command.addAll(List.of("-f", "lavfi", "-i", colorSource(CLOSING_DURATION)));
        chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        concatParts.add("[t" + idx + "]");
        idx++;

        // 音频
        command.add("-i");
        command.add(audioFile.getAbsolutePath());
        int audioIdx = idx;

        String filterComplex = String.join(";", chains)
                + ";" + String.join("", concatParts) + "concat=n=" + concatParts.size() + ":v=1:a=0[bg]"
                + ";[bg]ass=filename=" + assFile.getAbsolutePath() + "[v]"
                + ";[" + audioIdx + ":a]adelay=" + (long) (TITLE_DURATION * 1000) + "|"
                + (long) (TITLE_DURATION * 1000) + "[a]";

        command.add("-filter_complex");
        command.add(filterComplex);
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

    /** 纯色背景输入源（lavfi color） */
    private String colorSource(double duration) {
        return String.format("color=c=%s:s=%dx%d:r=%d:d=%.3f", BACKGROUND_COLOR, WIDTH, HEIGHT, FPS, duration);
    }

    /** 生成 ASS 字幕：标题卡(0~2s) → 分镜字幕(2s 起按字数占比分摊) → 结尾卡(末 2s) */
    private String buildAss(String title, List<SceneTiming> timings, double audioDuration) {
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

        // 分镜字幕：按字数占比在 [2, 2+D) 内分摊
        double cursor = TITLE_DURATION;
        double spanEnd = TITLE_DURATION + audioDuration;
        for (SceneTiming t : timings) {
            double end = Math.min(cursor + t.duration(), spanEnd);
            if (end <= cursor) {
                continue;
            }
            sb.append(dialogue(cursor, end, "Sub", wrap(sanitize(t.text()), SUBTITLE_MAX_CHARS_PER_LINE)));
            cursor = end;
        }

        // 结尾卡
        sb.append(dialogue(spanEnd, spanEnd + CLOSING_DURATION,
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
