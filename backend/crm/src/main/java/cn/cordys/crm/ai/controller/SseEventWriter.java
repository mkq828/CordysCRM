package cn.cordys.crm.ai.controller;

import cn.cordys.common.util.JSON;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * SSE 事件帧写出工具。data 为 String 时按纯文本逐行输出（兼容多行 markdown），否则 JSON 序列化。
 */
public final class SseEventWriter {

    private SseEventWriter() {
    }

    public static void write(PrintWriter writer, String event, String id, Object data) throws IOException {
        writer.write("event: " + event + "\n");
        if (id != null) {
            writer.write("id: " + id + "\n");
        }
        String content = data instanceof String s ? s : JSON.toJSONString(data);
        for (String line : content.replace("\r\n", "\n").split("\n", -1)) {
            writer.write("data: " + line + "\n");
        }
        writer.write("\n");
        writer.flush();
    }
}
