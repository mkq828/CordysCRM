package cn.cordys.crm.ai.video.service;

import cn.cordys.crm.ai.video.domain.VideoTemplateConfig;
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

    /** 帧率 */
    private static final int FPS = 30;

    /** 片头标题卡与片尾卡时长（秒），期间静音 */
    private static final double TITLE_DURATION = 2.0;
    private static final double CLOSING_DURATION = 2.0;
    /** 片头/片尾淡入淡出时长（秒），transition=fade 时生效 */
    private static final double FADE_DURATION = 0.4;

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

    /** 一镜视频 + 口播文本（Seedance 图生视频产物拼片，无视频时回退纯色背景） */
    public record ClipScene(String text, File video) {}

    /** 合成时的一镜视频（时长已按字数占比对齐旁白） */
    private record ClipTiming(String text, File video, double duration) {}

    /**
     * 合成竖屏口播视频：标题卡（封面图或纯色）+ 分镜（配图 Ken Burns 或纯色）+ 字幕 + 结尾卡，输出 mp4。
     * cover 非空时用封面图做标题卡背景；closingAudio 非空时片尾卡按片尾口播时长铺开并配音。
     */
    public void synthesize(File audioFile, File outputFile, String title, List<Scene> scenes,
                           VideoTemplateConfig cfg, int width, int height, File cover, File closingAudio) throws Exception {
        double audioDuration = probeDuration(audioFile);
        double closingDuration = closingDuration(closingAudio);
        List<SceneTiming> timings = computeTimings(scenes, audioDuration);

        File assFile = File.createTempFile("cordys-video-subtitle-", ".ass");
        try {
            Files.writeString(assFile.toPath(), buildAss(title, timings, audioDuration, closingDuration, cfg, width, height),
                    StandardCharsets.UTF_8);
            runFfmpeg(audioFile, closingAudio, outputFile, assFile, timings, audioDuration, closingDuration, cfg, cover,
                    width, height);
        } finally {
            assFile.delete();
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new RuntimeException("视频合成失败(未生成文件)");
        }
    }

    /**
     * 用 Seedance 图生视频产物拼成成片：每个分镜视频先归一化到旁白时长（不足末帧冻结、超长截断），
     * 再与片头/片尾卡 concat，叠加 ASS 字幕与配音。字幕与时长分摊复用静图同一条字数占比逻辑。
     * cover / closingAudio 语义与 {@link #synthesize} 一致。
     */
    public void synthesizeFromClips(File audioFile, File outputFile, String title, List<ClipScene> clips,
                                    VideoTemplateConfig cfg, int width, int height, File cover, File closingAudio)
            throws Exception {
        List<ClipScene> body = clips == null ? List.of()
                : clips.stream().filter(c -> c.text() != null && !c.text().isBlank()).toList();
        if (body.isEmpty()) {
            throw new RuntimeException("无有效分镜视频，无法合成");
        }
        double audioDuration = probeDuration(audioFile);
        double closingDuration = closingDuration(closingAudio);
        int totalChars = body.stream().mapToInt(c -> c.text().length()).sum();
        List<ClipTiming> timings = new ArrayList<>();
        for (ClipScene c : body) {
            double d = totalChars <= 0 ? audioDuration / body.size()
                    : audioDuration * c.text().length() / totalChars;
            timings.add(new ClipTiming(c.text(), c.video(), Math.max(d, 0.05)));
        }

        // 逐镜把 Seedance 视频归一化到旁白时长（trim / 末帧冻结），再串片
        List<File> normalized = new ArrayList<>();
        try {
            for (ClipTiming t : timings) {
                File n = File.createTempFile("cordys-clip-", ".mp4");
                normalizeClip(t.video(), n, t.duration(), width, height);
                normalized.add(n);
            }
            List<SceneTiming> subTimings = new ArrayList<>();
            for (ClipTiming t : timings) {
                subTimings.add(new SceneTiming(t.text(), null, t.duration()));
            }
            File assFile = File.createTempFile("cordys-video-subtitle-", ".ass");
            try {
                Files.writeString(assFile.toPath(),
                        buildAss(title, subTimings, audioDuration, closingDuration, cfg, width, height),
                        StandardCharsets.UTF_8);
                runClipFfmpeg(audioFile, closingAudio, outputFile, assFile, normalized, audioDuration, closingDuration,
                        cfg, cover, width, height);
            } finally {
                assFile.delete();
            }
        } finally {
            for (File n : normalized) {
                n.delete();
            }
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new RuntimeException("视频合成失败(未生成文件)");
        }
    }

    /** 片尾卡时长：有片尾口播时取口播时长（至少 2s），否则回退固定 2s */
    private double closingDuration(File closingAudio) {
        if (closingAudio == null || !closingAudio.exists()) {
            return CLOSING_DURATION;
        }
        try {
            return Math.max(CLOSING_DURATION, probeDuration(closingAudio) + 0.2);
        } catch (Exception e) {
            log.warn("片尾口播时长探测失败，回退固定 {} 秒", CLOSING_DURATION);
            return CLOSING_DURATION;
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

    /** 执行 ffmpeg：片头（封面图或纯色）+ 每个分镜（配图 zoompan 或纯色）+ 片尾纯色 concat，叠加 ASS 字幕，
     *  延迟语音、片尾口播并可选混 BGM。 */
    private void runFfmpeg(File audioFile, File closingAudio, File outputFile, File assFile, List<SceneTiming> timings,
                           double audioDuration, double closingDuration, VideoTemplateConfig cfg, File cover,
                           int width, int height) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");

        List<String> chains = new ArrayList<>();
        List<String> concatParts = new ArrayList<>();
        int idx = 0;

        // 片头标题卡：有封面用封面图（缓慢推近），否则纯色 2s
        if (cover != null && cover.exists()) {
            command.add("-i");
            command.add(cover.getAbsolutePath());
            int frames = Math.max(1, (int) Math.round(TITLE_DURATION * FPS));
            chains.add("[" + idx + ":v]scale=" + (width * 2) + ":" + (height * 2) + ":force_original_aspect_ratio=increase,"
                    + "crop=" + (width * 2) + ":" + (height * 2) + ","
                    + kenBurns(0, frames, width, height)
                    + ",format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        } else {
            command.addAll(List.of("-f", "lavfi", "-i", colorSource(TITLE_DURATION, cfg.background(), width, height)));
            chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        }
        concatParts.add("[t" + idx + "]");
        idx++;

        // 分镜：有配图走 Ken Burns（三种运动交替 + 2x 放大抑制抖动），无配图回退纯色
        int sceneNo = 0;
        for (SceneTiming t : timings) {
            if (t.image() != null && t.image().exists()) {
                command.add("-i");
                command.add(t.image().getAbsolutePath());
                int frames = Math.max(1, (int) Math.round(t.duration() * FPS));
                chains.add("[" + idx + ":v]scale=" + (width * 2) + ":" + (height * 2) + ":force_original_aspect_ratio=increase,"
                        + "crop=" + (width * 2) + ":" + (height * 2) + ","
                        + kenBurns(sceneNo, frames, width, height)
                        + ",format=yuv420p,setpts=PTS-STARTPTS[v" + idx + "]");
                concatParts.add("[v" + idx + "]");
                sceneNo++;
            } else {
                command.addAll(List.of("-f", "lavfi", "-i", colorSource(t.duration(), cfg.background(), width, height)));
                chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[v" + idx + "]");
                concatParts.add("[v" + idx + "]");
            }
            idx++;
        }

        // 片尾卡：片尾口播时长（至少 2s），纯色
        command.addAll(List.of("-f", "lavfi", "-i", colorSource(closingDuration, cfg.background(), width, height)));
        chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        concatParts.add("[t" + idx + "]");
        idx++;

        // BGM（可选，预留 Phase 2 资产库提供无版权音频）；文件缺失时静默跳过
        File bgm = resolveBgm(cfg);
        if (bgm != null) {
            command.add("-i");
            command.add(bgm.getAbsolutePath());
        }
        int bgmIdx = bgm == null ? -1 : idx;
        if (bgm != null) {
            idx++;
        }

        // 音频（配音）
        command.add("-i");
        command.add(audioFile.getAbsolutePath());
        int audioIdx = idx;
        idx++;

        // 片尾口播（可选）
        int closingIdx = -1;
        if (closingAudio != null && closingAudio.exists()) {
            command.add("-i");
            command.add(closingAudio.getAbsolutePath());
            closingIdx = idx;
            idx++;
        }

        double total = TITLE_DURATION + audioDuration + closingDuration;
        long voiceDelayMs = (long) (TITLE_DURATION * 1000);
        long closingDelayMs = (long) ((TITLE_DURATION + audioDuration) * 1000);

        StringBuilder filter = new StringBuilder();
        filter.append(String.join(";", chains))
                .append(';').append(String.join("", concatParts)).append("concat=n=").append(concatParts.size())
                .append(":v=1:a=0[bg]")
                .append(";[bg]ass=filename=").append(assFile.getAbsolutePath()).append("[assv]")
                .append(videoTail(cfg, total))
                .append(audioTail(audioIdx, closingIdx, bgmIdx, voiceDelayMs, closingDelayMs, cfg));

        command.add("-filter_complex");
        command.add(filter.toString());
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

        runCommand(command, 5);
    }

    /** 执行 Seedance 拼片合成：片头（封面图或纯色）+ 归一化后的分镜视频 + 片尾纯色 concat，叠加 ASS 字幕与配音。 */
    private void runClipFfmpeg(File audioFile, File closingAudio, File outputFile, File assFile, List<File> clips,
                               double audioDuration, double closingDuration, VideoTemplateConfig cfg, File cover,
                               int width, int height) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");

        List<String> chains = new ArrayList<>();
        List<String> concatParts = new ArrayList<>();
        int idx = 0;

        // 片头标题卡：有封面用封面图（缓慢推近），否则纯色 2s
        if (cover != null && cover.exists()) {
            command.add("-i");
            command.add(cover.getAbsolutePath());
            int frames = Math.max(1, (int) Math.round(TITLE_DURATION * FPS));
            chains.add("[" + idx + ":v]scale=" + (width * 2) + ":" + (height * 2) + ":force_original_aspect_ratio=increase,"
                    + "crop=" + (width * 2) + ":" + (height * 2) + ","
                    + kenBurns(0, frames, width, height)
                    + ",format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        } else {
            command.addAll(List.of("-f", "lavfi", "-i", colorSource(TITLE_DURATION, cfg.background(), width, height)));
            chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        }
        concatParts.add("[t" + idx + "]");
        idx++;

        // 分镜视频：已归一化到各自旁白时长
        for (File clip : clips) {
            command.add("-i");
            command.add(clip.getAbsolutePath());
            chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[v" + idx + "]");
            concatParts.add("[v" + idx + "]");
            idx++;
        }

        // 片尾卡：片尾口播时长（至少 2s），纯色
        command.addAll(List.of("-f", "lavfi", "-i", colorSource(closingDuration, cfg.background(), width, height)));
        chains.add("[" + idx + ":v]format=yuv420p,setpts=PTS-STARTPTS[t" + idx + "]");
        concatParts.add("[t" + idx + "]");
        idx++;

        // BGM（可选，与静图一致）
        File bgm = resolveBgm(cfg);
        if (bgm != null) {
            command.add("-i");
            command.add(bgm.getAbsolutePath());
        }
        int bgmIdx = bgm == null ? -1 : idx;
        if (bgm != null) {
            idx++;
        }

        // 配音
        command.add("-i");
        command.add(audioFile.getAbsolutePath());
        int audioIdx = idx;
        idx++;

        // 片尾口播（可选）
        int closingIdx = -1;
        if (closingAudio != null && closingAudio.exists()) {
            command.add("-i");
            command.add(closingAudio.getAbsolutePath());
            closingIdx = idx;
            idx++;
        }

        double total = TITLE_DURATION + audioDuration + closingDuration;
        long voiceDelayMs = (long) (TITLE_DURATION * 1000);
        long closingDelayMs = (long) ((TITLE_DURATION + audioDuration) * 1000);

        StringBuilder filter = new StringBuilder();
        filter.append(String.join(";", chains))
                .append(';').append(String.join("", concatParts)).append("concat=n=").append(concatParts.size())
                .append(":v=1:a=0[bg]")
                .append(";[bg]ass=filename=").append(assFile.getAbsolutePath()).append("[assv]")
                .append(videoTail(cfg, total))
                .append(audioTail(audioIdx, closingIdx, bgmIdx, voiceDelayMs, closingDelayMs, cfg));

        command.add("-filter_complex");
        command.add(filter.toString());
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

        runCommand(command, 5);
    }

    /**
     * 把一段视频归一化到指定时长：缩放裁切到画布 + 固定帧率；源比目标短则末帧冻结补齐，长则截断。
     * 这样 concat 出来的总长严格对齐旁白，字幕不会漂移。
     */
    private void normalizeClip(File src, File dst, double duration, int width, int height) throws Exception {
        double nativeDuration = probeDuration(src);
        StringBuilder vf = new StringBuilder();
        vf.append("scale=").append(width).append(':').append(height)
                .append(":force_original_aspect_ratio=increase,crop=").append(width).append(':').append(height)
                .append(",fps=").append(FPS).append(",format=yuv420p");
        if (nativeDuration < duration) {
            vf.append(",tpad=stop_mode=clone:stop_duration=").append(String.format("%.3f", duration - nativeDuration));
        }
        List<String> command = new ArrayList<>(List.of(
                "ffmpeg", "-y", "-i", src.getAbsolutePath(),
                "-vf", vf.toString(),
                "-t", String.format("%.3f", duration),
                "-an", "-c:v", "libx264", "-pix_fmt", "yuv420p",
                dst.getAbsolutePath()));
        runCommand(command, 3);
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

    /** 视频链尾部：字幕后的可选片头/片尾淡入淡出，输出 [v] */
    private String videoTail(VideoTemplateConfig cfg, double total) {
        if (cfg.isFade() && total > FADE_DURATION * 2 + 0.2) {
            return ";[assv]fade=t=in:st=0:d=" + FADE_DURATION + ",fade=t=out:st=" + (total - FADE_DURATION)
                    + ":d=" + FADE_DURATION + "[v]";
        }
        return ";[assv]null[v]";
    }

    /** 音频链尾部：配音延迟片头时长，片尾口播延迟到片尾卡起点，再可选混入 BGM，输出 [a] */
    private String audioTail(int audioIdx, int closingIdx, int bgmIdx, long voiceDelayMs, long closingDelayMs,
                             VideoTemplateConfig cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append(";[").append(audioIdx).append(":a]adelay=").append(voiceDelayMs).append('|').append(voiceDelayMs)
                .append("[voice]");
        if (closingIdx >= 0) {
            sb.append(";[").append(closingIdx).append(":a]adelay=").append(closingDelayMs).append('|').append(closingDelayMs)
                    .append("[closing]")
                    .append(";[voice][closing]amix=inputs=2:duration=longest:dropout_transition=0:normalize=0[voicemix]");
        } else {
            sb.append(";[voice]anull[voicemix]");
        }
        if (bgmIdx >= 0) {
            sb.append(";[").append(bgmIdx).append(":a]volume=").append(cfg.bgmVolume()).append("[bgm]")
                    .append(";[voicemix][bgm]amix=inputs=2:duration=first:dropout_transition=0:normalize=0[a]");
        } else {
            sb.append(";[voicemix]anull[a]");
        }
        return sb.toString();
    }

    /** 解析模板配置的 BGM 文件：路径非空且文件存在才参与混音，否则返回 null */
    private File resolveBgm(VideoTemplateConfig cfg) {
        if (!cfg.hasBgm()) {
            return null;
        }
        File file = new File(cfg.bgmFile());
        return file.isFile() ? file : null;
    }

    /** 纯色背景输入源（lavfi color） */
    private String colorSource(double duration, String color, int width, int height) {
        return String.format("color=c=%s:s=%dx%d:r=%d:d=%.3f", color, width, height, FPS, duration);
    }

    /**
     * 分镜 Ken Burns 运动表达式：0=中心推近，1=左→右横移，2=右→左横移。
     * 少于 2 帧退化为中心推近，避免平移表达式的 (frames-1) 除零。
     */
    private String kenBurns(int sceneNo, int frames, int width, int height) {
        int motion = frames < 2 ? 0 : sceneNo % 3;
        int divisor = Math.max(1, frames - 1);
        return switch (motion) {
            case 1 -> "zoompan=z='1.15':x='(iw-iw/zoom)*on/" + divisor + "':y='ih/2-(ih/zoom/2)':"
                    + "d=" + frames + ":s=" + width + "x" + height + ":fps=" + FPS;
            case 2 -> "zoompan=z='1.15':x='(iw-iw/zoom)*(1-on/" + divisor + ")':y='ih/2-(ih/zoom/2)':"
                    + "d=" + frames + ":s=" + width + "x" + height + ":fps=" + FPS;
            default -> "zoompan=z='min(zoom+0.0018,1.20)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':"
                    + "d=" + frames + ":s=" + width + "x" + height + ":fps=" + FPS;
        };
    }

    /** 生成 ASS 字幕：标题卡(0~2s) → 分镜字幕(2s 起按字数占比分摊) → 结尾卡(末 2s)。样式由模板配置驱动。 */
    private String buildAss(String title, List<SceneTiming> timings, double audioDuration, double closingDuration,
                            VideoTemplateConfig cfg, int width, int height) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Script Info]\nScriptType: v4.00+\nPlayResX: ").append(width)
                .append("\nPlayResY: ").append(height).append("\n\n");
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, "
                + "Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, "
                + "Alignment, MarginL, MarginR, MarginV, Encoding\n");
        // 字幕：Alignment 2=底部居中，5=屏幕居中（subtitleCenter=true 时用大字号居中风格）
        int subAlign = cfg.subtitleCenter() ? 5 : 2;
        sb.append("Style: Sub,").append(font()).append(',').append(cfg.subtitleFontSize()).append(',')
                .append(cfg.subtitleColor()).append(',').append(cfg.subtitleColor()).append(',')
                .append(cfg.subtitleOutline()).append(',').append(cfg.subtitleShadow())
                .append(",0,0,0,0,100,100,0,0,1,3,1,").append(subAlign).append(",60,60,160,1\n");
        // 标题卡：恒居中加粗
        sb.append("Style: Title,").append(font()).append(',').append(cfg.titleFontSize()).append(',')
                .append(cfg.titleColor()).append(',').append(cfg.titleColor())
                .append(",&H00000000,&H00000000,1,0,0,0,100,100,0,0,1,4,2,5,40,40,0,1\n");
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
        sb.append(dialogue(spanEnd, spanEnd + closingDuration,
                "Title", wrap(sanitize(cfg.closingText()), TITLE_MAX_CHARS_PER_LINE)));
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

    /** 用 ffprobe 取媒体时长（秒），用于设定背景视频时长、字幕分摊与分镜视频归一化 */
    private double probeDuration(File mediaFile) throws Exception {
        List<String> command = List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                mediaFile.getAbsolutePath());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        process.waitFor(30, TimeUnit.SECONDS);
        try {
            return Double.parseDouble(out);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无法获取媒体时长: " + out);
        }
    }
}
