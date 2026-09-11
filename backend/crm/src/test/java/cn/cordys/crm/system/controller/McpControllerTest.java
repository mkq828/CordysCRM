package cn.cordys.crm.system.controller;

import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.dto.field.base.SimpleField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

/**
 * MCP 三方接口控制器测试
 *
 * <p>覆盖 {@link McpController} 的表单配置查询接口，此前该控制器没有任何测试覆盖。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class McpControllerTest extends BaseTest {

    @Test
    void testGetMcpFormConfig() throws Exception {
        // 请求成功，返回 quotation 表单的 MCP 字段列表
        MvcResult mvcResult = this.requestGetWithOkAndReturn("/mcp/form/config/{formKey}", "quotation");
        List<SimpleField> fields = getResultDataArray(mvcResult, SimpleField.class);

        // 字段列表应可正常解析（不为 null）
        Assertions.assertNotNull(fields);
    }
}
