package cn.cordys.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 安全事件特征识别器单元测试，覆盖各攻击特征命中与正常请求不误报。
 */
class SecurityEventDetectorTest {

    @Test
    void detectSqlInjection() {
        assertEquals(SecurityEventDetector.Rule.SQL_INJECTION,
                SecurityEventDetector.detect("/api/customer/list", "id=1 or 1=1", "Mozilla/5.0"));
        assertEquals(SecurityEventDetector.Rule.SQL_INJECTION,
                SecurityEventDetector.detect("/api/search", "q=1' or '1'='1", "Mozilla/5.0"));
    }

    @Test
    void detectXss() {
        assertEquals(SecurityEventDetector.Rule.XSS,
                SecurityEventDetector.detect("/api/note", "content=<script>alert(1)</script>", "Mozilla/5.0"));
    }

    @Test
    void detectPathTraversal() {
        assertEquals(SecurityEventDetector.Rule.PATH_TRAVERSAL,
                SecurityEventDetector.detect("/download/../../etc/passwd", null, "Mozilla/5.0"));
        assertEquals(SecurityEventDetector.Rule.PATH_TRAVERSAL,
                SecurityEventDetector.detect("/download", "file=/etc/passwd", "Mozilla/5.0"));
    }

    @Test
    void detectScannerUserAgent() {
        assertEquals(SecurityEventDetector.Rule.SCANNER_UA,
                SecurityEventDetector.detect("/api/customer/list", "page=1", "sqlmap/1.7.2"));
    }

    @Test
    void normalRequestNotFlagged() {
        assertNull(SecurityEventDetector.detect("/api/customer/list", "page=1&name=张三", "Mozilla/5.0"));
    }

    @Test
    void detectUrlEncodedSqlInjection() {
        // 空格编码成 %20，原始串无空格不命中，解码后命中
        assertEquals(SecurityEventDetector.Rule.SQL_INJECTION,
                SecurityEventDetector.detect("/api/customer/list", "id=1%20or%202=3", "Mozilla/5.0"));
        // 引号编码成 %27
        assertEquals(SecurityEventDetector.Rule.SQL_INJECTION,
                SecurityEventDetector.detect("/api/customer/list", "q=1%27%20or%20%271%27=%271", "Mozilla/5.0"));
    }

    @Test
    void detectUrlEncodedXss() {
        assertEquals(SecurityEventDetector.Rule.XSS,
                SecurityEventDetector.detect("/api/note", "content=%3Cscript%3Ealert(1)%3C/script%3E", "Mozilla/5.0"));
    }

    @Test
    void detectDoubleEncodedTraversal() {
        // %252e 解码一次后变成 %2e，命中路径穿越
        assertEquals(SecurityEventDetector.Rule.PATH_TRAVERSAL,
                SecurityEventDetector.detect("/download", "file=%252e%252e%252fetc%252fpasswd", "Mozilla/5.0"));
    }
}
