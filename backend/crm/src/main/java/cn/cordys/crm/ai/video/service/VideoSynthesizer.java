package cn.cordys.crm.ai.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 视频合成（功能 7 MVP）：竖屏纯色背景 + 语音 → mp4。
 * ffmpeg 为服务器部署依赖，本类通过 ProcessBuilder 调用，不自研视频引擎。
 */
@Slf4j
@Component
public class VideoSynthesizer {

    /** 竖屏 9:16（抖音/视频号口播规格） */
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final int FPS = 30;
    /** 深色背景，后续可换成品牌色或背景图 */
    private static final String BACKGROUND_COLOR = "0x1E1B2E";

    /**
     * 合成竖屏口播视频：纯色背景 + 语音，输出 mp4。
     *
     * @param audioFile  语音文件（mp3）
     * @param outputFile 输出 mp4
     */
    public void synthesize(File audioFile, File outputFile) throws Exception {
        double duration = probeDuration(audioFile);

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add(String.format("color=c=%s:s=%dx%d:r=%d:d=%.3f",
                BACKGROUND_COLOR, WIDTH, HEIGHT, FPS, duration));
        command.add("-i");
        command.add(audioFile.getAbsolutePath());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("1:a:0");
        command.add("-c:v");
        command.add("libx264");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-c:a");
        command.add("aac");
        command.add("-shortest");
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
        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new RuntimeException("视频合成失败(未生成文件)");
        }
    }

    /** 用 ffprobe 取音频时长（秒），用于设定背景视频时长 */
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
