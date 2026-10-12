package cn.cordys.crm.ai.drama.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 漫剧成片合成（Phase 6）：逐镜「静图 + 口播配音」压成统一规格的竖屏片段，再按镜序 concat 成 mp4。
 * ffmpeg 为服务器部署依赖（与短视频 {@code VideoSynthesizer} 一致），本类通过 ProcessBuilder 调用；
 * 每镜画面 loop 到本镜时长，配音不足补静音、超长截断，保证 concat 后音画对齐。Seedance 图生视频（Phase 7）会替换静图为视频片段。
 */
@Slf4j
@Component
public class DramaVideoComposer {

    /** 成片画幅：竖屏 9:16，与分镜文生图 1080x1920 对齐 */
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final int FPS = 30;

    /** 一镜素材：画面图 / 镜头视频（二选一，视频优先）+ 口播音频（可空则静音）+ 计划时长 */
    public record Clip(File image, File video, File audio, double durationSec) {}

    /**
     * 逐镜合成并拼接，返回成片 mp4（临时文件，由调用方上传后清理）。
     */
    public File compose(List<Clip> clips) throws Exception {
        List<Clip> usable = clips == null ? List.of()
                : clips.stream().filter(c -> hasVisual(c)).toList();
        if (usable.isEmpty()) {
            throw new RuntimeException("本集还没有可合成的分镜画面");
        }

        File workDir = Files.createTempDirectory("drama-compose-").toFile();
        List<File> segments = new ArrayList<>();
        try {
            for (int i = 0; i < usable.size(); i++) {
                Clip clip = usable.get(i);
                File segment = new File(workDir, String.format("seg-%03d.mp4", i));
                buildSegment(clip, segment);
                segments.add(segment);
            }
            if (segments.size() == 1) {
                File single = segments.get(0);
                File out = new File(workDir, "compose.mp4");
                // 单镜直接复用，无需 concat
                if (!single.renameTo(out)) {
                    Files.copy(single.toPath(), out.toPath());
                }
                return out;
            }
            File out = new File(workDir, "compose.mp4");
            concat(segments, out);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("成片合成失败：" + (e.getMessage() == null ? "ffmpeg 执行异常" : e.getMessage()), e);
        } finally {
            // 清理中间片段（成片文件由调用方消费后连同工作目录一起删除）
            for (File segment : segments) {
                try {
                    Files.deleteIfExists(segment.toPath());
                } catch (Exception ignored) {
                    // 清理失败不影响主流程
                }
            }
        }
    }

    /** 素材有画面：静图或镜头视频任一存在即可 */
    private boolean hasVisual(Clip clip) {
        return (clip.image() != null && clip.image().exists())
                || (clip.video() != null && clip.video().exists());
    }

    /** 单镜片段：静图 loop / 视频 loop 到本镜时长，配音补静音/截断到等长 */
    private void buildSegment(Clip clip, File output) throws Exception {
        double duration = resolveDuration(clip);
        boolean useVideo = clip.video() != null && clip.video().exists();
        File visual = useVideo ? clip.video() : clip.image();
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        if (useVideo) {
            // 镜头视频 loop 补齐到本镜时长（Seedance 单镜 5s，长镜重复播放）
            command.addAll(List.of("-stream_loop", "-1", "-i", visual.getAbsolutePath()));
        } else {
            command.addAll(List.of("-loop", "1", "-i", visual.getAbsolutePath()));
        }
        if (clip.audio() != null && clip.audio().exists()) {
            command.add("-i");
            command.add(clip.audio().getAbsolutePath());
        } else {
            command.addAll(List.of("-f", "lavfi", "-i", "anullsrc=r=44100:cl=stereo"));
        }
        command.addAll(List.of(
                "-t", String.format("%.3f", duration),
                "-vf", String.format(
                        "scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d,format=yuv420p,fps=%d",
                        WIDTH, HEIGHT, WIDTH, HEIGHT, FPS),
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p",
                "-af", "apad", "-c:a", "aac", "-ar", "44100", "-ac", "2",
                "-movflags", "+faststart",
                output.getAbsolutePath()));
        runCommand(command, 3);
    }

    /** 本镜时长：取计划时长与配音时长的较大者，留 0.2s 尾缓冲防截音 */
    private double resolveDuration(Clip clip) throws Exception {
        double planned = clip.durationSec() >= 4 ? clip.durationSec() : 5;
        if (clip.audio() == null || !clip.audio().exists()) {
            return planned + 0.2;
        }
        double audio = probeDuration(clip.audio());
        return Math.max(planned, audio + 0.2) + 0.2;
    }

    /** 拼接已统一规格的片段（stream copy），输出成片 */
    private void concat(List<File> segments, File output) throws Exception {
        StringBuilder list = new StringBuilder();
        for (File segment : segments) {
            list.append("file '").append(segment.getAbsolutePath()).append("'\n");
        }
        File concatList = File.createTempFile("drama-concat-", ".txt");
        try {
            Files.writeString(concatList.toPath(), list.toString(), StandardCharsets.UTF_8);
            List<String> command = new ArrayList<>(List.of(
                    "ffmpeg", "-y",
                    "-f", "concat", "-safe", "0", "-i", concatList.getAbsolutePath(),
                    "-c", "copy", "-movflags", "+faststart",
                    output.getAbsolutePath()));
            runCommand(command, 3);
        } finally {
            Files.deleteIfExists(concatList.toPath());
        }
    }

    /** 用 ffprobe 取媒体时长（秒） */
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

    /** 执行外部命令（ffmpeg/ffprobe），丢弃输出防阻塞，超时强杀，非零退出码抛异常 */
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
