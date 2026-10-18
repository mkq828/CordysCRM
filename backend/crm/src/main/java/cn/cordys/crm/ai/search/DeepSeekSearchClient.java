package cn.cordys.crm.ai.search;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 网页搜索客户端（{@code /v1/tools/search}）。平台级独立能力，后续标讯/竞品情报等场景可复用。
 * <p>
 * key 读自 sys_parameter {@code ai.deepseek.searchApiKey}，为空或调用失败时返回空列表，不阻断调用方主流程。
 * </p>
 */
@Component
@Slf4j
public class DeepSeekSearchClient {

    private static final String PARAM_API_KEY = "ai.deepseek.searchApiKey";

    private static final String SEARCH_URL = "https://api.deepseek.com/v1/tools/search";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /** 单条正文截断上限（字符）：比 summary（约 600-800 字）更深，又控制注入 LLM 的 token 开销 */
    private static final int CONTENT_MAX_CHARS = 1500;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Resource
    private BaseMapper<Parameter> parameterMapper;

    /**
     * 按关键词搜索网页，返回最多 5 条结构化结果（标题/链接/摘要/正文）。未配 key 或调用失败时返回空列表。
     */
    public List<AdvisorWebSource> search(String query) {
        String apiKey = getApiKey();
        if (StringUtils.isBlank(apiKey)) {
            log.warn("未配置 {}，跳过联网搜索", PARAM_API_KEY);
            return List.of();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("count", 5);
        body.put("need_content", true);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEARCH_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("DeepSeek 搜索失败，status={}, body={}", response.statusCode(), response.body());
                return List.of();
            }
            return parseResults(response.body());
        } catch (Exception e) {
            log.warn("DeepSeek 搜索异常: {}", e.getMessage());
            return List.of();
        }
    }

    private List<AdvisorWebSource> parseResults(String body) {
        List<AdvisorWebSource> sources = new ArrayList<>();
        try {
            JsonNode root = JSON.MAPPER.readTree(body);
            JsonNode results = root.path("results");
            if (!results.isArray()) {
                return sources;
            }
            for (JsonNode item : results) {
                AdvisorWebSource source = new AdvisorWebSource();
                source.setTitle(cleanHtml(textOf(item, "title")));
                source.setUrl(textOf(item, "url"));
                source.setSnippet(cleanHtml(firstNonBlank(textOf(item, "snippet"), textOf(item, "summary"))));
                source.setContent(truncate(cleanHtml(textOf(item, "content")), CONTENT_MAX_CHARS));
                if (StringUtils.isNotBlank(source.getUrl())) {
                    sources.add(source);
                }
            }
        } catch (Exception e) {
            log.warn("解析 DeepSeek 搜索结果失败: {}", e.getMessage());
        }
        return sources;
    }

    private String textOf(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String firstNonBlank(String a, String b) {
        return StringUtils.isNotBlank(a) ? a : b;
    }

    /** 正文截断到上限字符，超长取前 N 字，避免整页全文注入撑爆上下文 */
    private String truncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    /** 剥离标题/摘要里的 HTML 标签与实体，兼容一层或多层实体编码（如 &lt;table&gt;），避免原始标记直接展示。 */
    private String cleanHtml(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw;
        // 解码后可能又露出新的标签，循环几轮直到不再变化
        for (int i = 0; i < 3; i++) {
            String prev = text;
            text = StringEscapeUtils.unescapeHtml4(text);
            // 表格/块级标签边界补空格，避免 td 之间文字粘连；再剥离其余行内标签
            text = text.replaceAll("(?i)</?(table|tr|td|th|p|div|li|ul|ol|br|h[1-6])[^>]*>", " ");
            text = text.replaceAll("<[^>]*>", "");
            text = text.replaceAll("\\s+", " ").trim();
            if (text.equals(prev)) {
                break;
            }
        }
        return text;
    }

    private String getApiKey() {
        Parameter parameter = parameterMapper.selectByPrimaryKey(PARAM_API_KEY);
        String value = parameter == null ? null : parameter.getParamValue();
        return value == null ? "" : value.trim();
    }
}
