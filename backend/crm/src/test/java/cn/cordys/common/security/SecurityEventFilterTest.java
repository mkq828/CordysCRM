package cn.cordys.common.security;

import cn.cordys.common.utils.IpRegionService;
import cn.cordys.crm.system.domain.SecurityEvent;
import cn.cordys.crm.system.service.SecurityEventService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 安全事件留证过滤器单元测试，验证恶意请求落库、正常请求不误报、高频错误触发。
 */
class SecurityEventFilterTest {

    private final SecurityEventService service = mock(SecurityEventService.class);
    private final SecurityEventFilter filter = new SecurityEventFilter(service, new IpRegionService());

    @Test
    void maliciousRequestProducesEvent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customer/list");
        request.setQueryString("id=1 or 1=1");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passthroughChain());

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(service, times(1)).record(captor.capture());
        assertEquals("sql_injection", captor.getValue().getEventType());
        assertEquals("1.2.3.4", captor.getValue().getRemoteAddr());
        assertEquals(200, captor.getValue().getHttpStatus());
    }

    @Test
    void normalRequestProducesNothing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customer/list");
        request.setQueryString("page=1&name=张三");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passthroughChain());

        verify(service, never()).record(any(SecurityEvent.class));
    }

    @Test
    void errorFloodTriggersAfterThreshold() throws Exception {
        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/login");
            request.setRemoteAddr("5.6.7.8");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, errorChain());
        }

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(service, times(1)).record(captor.capture());
        assertEquals("error_flood", captor.getValue().getEventType());
    }

    private static FilterChain passthroughChain() {
        return (request, response) -> {
        };
    }

    private static FilterChain errorChain() {
        return (request, response) -> ((HttpServletResponse) response).setStatus(500);
    }
}
