package cn.cordys.crm.ai.replica.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 爆款复刻 · 参考视频解析：ffprobe 取时长 + ffmpeg 抽封面帧、抽 16kHz 单声道 WAV 供 paraformer 转写。
 * 只「读原视频」，不生成任何画面；画面声音在后续合成阶段 100% 复用原片。
 */
@Slf4j
@Component
public class ReplicaVideoParser {

    /** 解析产物：时长（秒）、封面帧、16kHz 单声道音频 */
    public record ParseResult(double durationSeconds, File coverImage, File audioWav) {
    }

    /** 解析参考视频：抽封面帧（1s 处，短于 1s 取首帧）+ 抽 16kHz mono WAV + 取时长 */
    public ParseResult parse(File videoFile, File workDir) throws Exception {
        double duration = probeDuration(videoFile);
        File cover = new File(workDir, "cover.jpg");
        extractFrame(videoFile, cover, Math.min(1.0, Math.max(0, duration)));
        File audio = new File(workDir, "audio.wav");
        extractAudio(videoFile, audio);
        return new ParseResult(duration, cover, audio);
    }

    /** 抽一帧封面（-ss 定位 + 单帧 jpg，画质 q=2） */
    private void extractFrame(File video, File out, double atSeconds) throws Exception {
        List<String> command = List.of(
                "ffmpeg", "-y",
                "-ss", String.format("%.3f", atSeconds),
                "-i", video.getAbsolutePath(),
                "-frames:v", "1", "-q:v", "2",
                out.getAbsolutePath());
        runCommand(command, 1);
    }

    /** 抽音频：去掉视频流，转 16kHz 单声道 PCM WAV（paraformer 支持的输入） */
    private void extractAudio(File video, File out) throws Exception {
        List<String> command = List.of(
                "ffmpeg", "-y",
                "-i", video.getAbsolutePath(),
                "-vn", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le",
                out.getAbsolutePath());
        runCommand(command, 5);
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
