package cn.cordys.crm.ai.knowledge.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块：按约 500 字符切块、前后重叠 100 字符，段边界（换行）就近断开，返回块文本列表。
 */
@Component
public class DocumentChunker {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String t = text.replace("\r\n", "\n").trim();
        int len = t.length();
        int start = 0;
        while (start < len) {
            int end = Math.min(start + CHUNK_SIZE, len);
            // 未到结尾时，在后 1/3 区间内就近找换行断开，避免句子被拦腰截断
            if (end < len) {
                int brk = t.lastIndexOf('\n', end);
                if (brk > start + CHUNK_SIZE * 2 / 3) {
                    end = brk;
                }
            }
            String piece = t.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= len) {
                break;
            }
            start = Math.max(end - OVERLAP, start + 1);
        }
        return chunks;
    }
}
