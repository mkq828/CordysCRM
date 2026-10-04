package cn.cordys.crm.ai.employee.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.employee.domain.AiEmployee;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 员工角色目录初始化器：应用启动时把 {@code classpath:ai-employees/<division>/*.md} 的 264 个角色
 * （agency-agents，MIT，保留署名）解析进 {@link AiEmployee} 表，幂等——表内已有数据即跳过。
 * <p>
 * 目录文件仅在首次导入时消费一次；后续升级目录（如上游新增角色）可通过清空表后重启触发重导。
 * </p>
 */
@Component
@Slf4j
public class AiEmployeeCatalogInitializer implements ApplicationRunner {

    private static final String CATALOG_PATTERN = "classpath*:ai-employees/*/*.md";

    private static final Pattern DIVISION_PATTERN = Pattern.compile("ai-employees/([^/]+)/[^/]+\\.md$");

    @Resource
    private BaseMapper<AiEmployee> employeeMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    public void run(ApplicationArguments args) {
        try {
            long existing = employeeMapper.countByExample(new AiEmployee());
            if (existing > 0) {
                log.info("AI 员工角色目录已存在 {} 条，跳过初始化", existing);
                return;
            }
            List<ParsedEmployee> parsed = loadCatalog();
            if (parsed.isEmpty()) {
                log.warn("未在 classpath:ai-employees 下解析到任何角色文件，跳过初始化");
                return;
            }
            importCatalog(parsed);
            log.info("AI 员工角色目录初始化完成，共导入 {} 条", parsed.size());
        } catch (Exception e) {
            // 目录初始化失败不应阻断整个应用启动：记录清晰日志，功能侧目录列表会返回空，便于后续修复后重导
            log.error("AI 员工角色目录初始化失败，目录功能可能为空", e);
        }
    }

    /** 加载并解析目录文件：仅匹配 ai-employees 下一级子目录内的 md（NOTICE.md 位于根，自然排除） */
    private List<ParsedEmployee> loadCatalog() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource[] resources = resolver.getResources(CATALOG_PATTERN);
        List<ParsedEmployee> result = new ArrayList<>();
        for (org.springframework.core.io.Resource resource : resources) {
            String division = resolveDivision(resource);
            if (StringUtils.isBlank(division)) {
                continue;
            }
            String content = readContent(resource);
            ParsedEmployee parsed = parse(content, division);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return result;
    }

    /** 从资源 URI 路径中截取 division 段（{@code .../ai-employees/<division>/<file>.md}） */
    private String resolveDivision(org.springframework.core.io.Resource resource) throws Exception {
        Matcher matcher = DIVISION_PATTERN.matcher(resource.getURI().getPath());
        return matcher.find() ? matcher.group(1) : null;
    }

    private String readContent(org.springframework.core.io.Resource resource) throws Exception {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 事务内批量落库，保证目录要么全导、要么不导（部分导入可由清表重启重导） */
    private void importCatalog(List<ParsedEmployee> parsed) {
        Map<String, List<ParsedEmployee>> byDivision = new LinkedHashMap<>();
        for (ParsedEmployee employee : parsed) {
            byDivision.computeIfAbsent(employee.division(), k -> new ArrayList<>()).add(employee);
        }
        new TransactionTemplate(transactionManager).execute(status -> {
            long now = System.currentTimeMillis();
            for (List<ParsedEmployee> list : byDivision.values()) {
                List<ParsedEmployee> sorted = new ArrayList<>(list);
                sorted.sort(Comparator.comparing(ParsedEmployee::name));
                for (int i = 0; i < sorted.size(); i++) {
                    ParsedEmployee p = sorted.get(i);
                    AiEmployee employee = new AiEmployee();
                    employee.setId(IDGenerator.nextStr());
                    employee.setDivision(p.division());
                    employee.setName(p.name());
                    employee.setDescription(p.description());
                    employee.setColor(p.color());
                    employee.setEmoji(p.emoji());
                    employee.setVibe(p.vibe());
                    employee.setSystemPrompt(p.systemPrompt());
                    employee.setSort((long) i);
                    employee.setCreateTime(now);
                    employee.setUpdateTime(now);
                    employeeMapper.insert(employee);
                }
            }
            return null;
        });
    }

    /** 解析 agency-agents 的 Markdown + YAML frontmatter：首行 --- 至第二个 --- 为元数据，其后为系统提示词 */
    private ParsedEmployee parse(String content, String division) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String[] lines = content.split("\\r?\\n", -1);
        int i = 0;
        if (i >= lines.length || !"---".equals(lines[i].trim())) {
            return null;
        }
        i++;
        Map<String, String> meta = new LinkedHashMap<>();
        String currentKey = null;
        for (; i < lines.length; i++) {
            String line = lines[i];
            if ("---".equals(line.trim())) {
                i++;
                break;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                // 无冒号视为上一字段的续行，追加到上一值
                if (currentKey != null) {
                    meta.merge(currentKey, line.trim(), (a, b) -> a + " " + b);
                }
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = stripQuotes(line.substring(colon + 1).trim());
            meta.put(key, value);
            currentKey = key;
        }
        StringBuilder body = new StringBuilder();
        for (; i < lines.length; i++) {
            body.append(lines[i]).append('\n');
        }
        String name = meta.get("name");
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String systemPrompt = body.toString().trim();
        if (systemPrompt.startsWith("# ")) {
            // 去掉首行「# xxx Agent」标题，避免与角色名重复
            int firstNewline = systemPrompt.indexOf('\n');
            if (firstNewline >= 0) {
                systemPrompt = systemPrompt.substring(firstNewline + 1).trim();
            }
        }
        return new ParsedEmployee(division, name, meta.get("description"), meta.get("color"),
                meta.get("emoji"), meta.get("vibe"), systemPrompt);
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.length() >= 2) {
            char first = v.charAt(0);
            char last = v.charAt(v.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return v.substring(1, v.length() - 1);
            }
        }
        return v;
    }

    private record ParsedEmployee(String division, String name, String description, String color,
                                  String emoji, String vibe, String systemPrompt) {
    }
}
