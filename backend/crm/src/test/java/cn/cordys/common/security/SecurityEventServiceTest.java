package cn.cordys.common.security;

import cn.cordys.crm.system.domain.SecurityEvent;
import cn.cordys.crm.system.service.SecurityEventService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全事件 CSV 导出单元测试，覆盖字段转义（引号/换行）与空值。
 */
class SecurityEventServiceTest {

    private final SecurityEventService service = new SecurityEventService();

    @Test
    void toCsvHasHeaderAndEscapesQuotesAndNewlines() {
        SecurityEvent e = new SecurityEvent();
        e.setEventType("sql_injection");
        e.setDetail("命中 SQL 注入特征");
        e.setRemoteAddr("1.2.3.4");
        e.setUri("/api/search?q=1\" or \"1\"=\"1\n--");
        e.setCreateTime(0L);

        String csv = service.toCsv(List.of(e));

        String[] lines = csv.split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("事件类型,事件描述"));
        // 引号翻倍、换行去掉
        assertTrue(lines[1].contains("\"/api/search?q=1\"\" or \"\"1\"\"=\"\"1 --\""));
    }

    @Test
    void toCsvEmptyFieldsBecomeEmptyQuoted() {
        SecurityEvent e = new SecurityEvent();
        e.setEventType("xss");

        String csv = service.toCsv(List.of(e));

        String[] lines = csv.split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[1].contains("\"xss\""));
        assertTrue(lines[1].contains("\"\""));
    }
}
