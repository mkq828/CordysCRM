package cn.cordys.crm.ai.tool.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 转码工具（工具中心-视频转格式）。ffmpeg 为服务器部署依赖，通过 ProcessBuilder 调用。
 * 支持 mp4 / mov / gif / webm；gif 走缩放降帧避免体积过大，其余按容器规格重编码保证兼容性。
 */
@Slf4j
@Component
public class FfmpegUtil {

    /** 支持的输出格式（小写，与前端下拉一致） */
    public static final List<String> FORMATS = List.of("mp4", "mov", "gif", "webm");

    public void transcode(File input, File output, String format) throws Exception {
        String ext = normalize(format);
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(input.getAbsolutePath());
        switch (ext) {
            case "gif" -> command.addAll(List.of("-vf", "fps=10,scale=480:-1:flags=lanczos", "-loop", "0"));
            case "webm" -> command.addAll(List.of("-c:v", "libvpx", "-b:v", "1M", "-c:a", "libopus"));
            default -> command.addAll(List.of("-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac",
                    "-movflags", "+faststart"));
        }
        command.add(output.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        byte[] buffer = new byte[4096];
        try (var in = process.getInputStream()) {
            while (in.read(buffer) != -1) {
                // 丢弃 ffmpeg 日志，避免缓冲区写满阻塞
            }
        }
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("视频转格式超时");
        }
        if (process.exitValue() != 0 || !output.exists() || output.length() == 0) {
            throw new RuntimeException("视频转格式失败(ffmpeg 退出码 " + process.exitValue() + ")");
        }
    }

    private String normalize(String format) {
        String ext = format == null ? "mp4" : format.trim().toLowerCase();
        return FORMATS.contains(ext) ? ext : "mp4";
    }
}
