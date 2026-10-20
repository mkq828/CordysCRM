package cn.cordys.common.security;

import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 安全事件特征识别器。纯静态规则，命中即返回对应规则，不拦截请求。
 * <p>
 * 只做「留证」不做「封禁」：规则保守，避免把正常业务请求误判成攻击。
 */
public final class SecurityEventDetector {

    public enum Rule {
        SQL_INJECTION("sql_injection", "命中 SQL 注入特征"),
        XSS("xss", "命中跨站脚本(XSS)特征"),
        PATH_TRAVERSAL("path_traversal", "命中路径穿越/文件读取特征"),
        SCANNER_UA("scanner_ua", "命中已知扫描器 User-Agent");

        public final String eventType;
        public final String detail;

        Rule(String eventType, String detail) {
            this.eventType = eventType;
            this.detail = detail;
        }
    }

    private static final Pattern SQL_INJECTION = Pattern.compile(
            "(?i)(union\\s+select|select\\s+.+?\\s+from|\\bor\\s+\\d+\\s*=\\s*\\d+\\b|"
                    + "\\band\\s+\\d+\\s*=\\s*\\d+\\b|sleep\\s*\\(|benchmark\\s*\\(|information_schema|"
                    + "waitfor\\s+delay|group_concat\\s*\\(|/\\*!|'\\s*=\\s*'|\\b1\\s*=\\s*1\\b)");

    private static final Pattern XSS = Pattern.compile(
            "(?i)(<script|javascript:\\s*|onerror\\s*=|onload\\s*=|alert\\s*\\(|<iframe|"
                    + "src\\s*=\\s*['\"]?\\s*javascript:|document\\.cookie)");

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
            "(?i)(\\.\\.(?:/|\\\\|%2f|%5c)|/etc/(?:passwd|shadow)|%2e%2e%2f|"
                    + "windows/win\\.ini|\\.bash_history|/proc/self/environ|WEB-INF|web\\.xml)");

    private static final Pattern SCANNER_UA = Pattern.compile(
            "(?i)(sqlmap|nikto|nessus|nuclei|gobuster|dirbuster|\\bdirb\\b|wpscan|acunetix|"
                    + "openvas|masscan|zgrab|nmap|burpsuite|fimap|skipfish|havij|netsparker|appscan)");

    private SecurityEventDetector() {
    }

    /**
     * 检测单个请求是否命中攻击特征。返回命中的规则，未命中返回 null。
     */
    public static Rule detect(String path, String query, String userAgent) {
        Rule rule = matchUri(path, query);
        if (rule != null) {
            return rule;
        }
        if (StringUtils.isNotBlank(userAgent) && SCANNER_UA.matcher(userAgent).find()) {
            return Rule.SCANNER_UA;
        }
        return null;
    }

    /**
     * 先匹配原始未解码 URI，再 URL 解码后匹配一次，覆盖把空格/引号/尖括号/斜杠编码成 %xx 或 + 的载荷。
     */
    private static Rule matchUri(String path, String query) {
        String raw = StringUtils.defaultString(path) + " " + StringUtils.defaultString(query);
        Rule rule = matchTarget(raw);
        if (rule != null) {
            return rule;
        }
        return matchTarget(decode(path) + " " + decode(query));
    }

    private static Rule matchTarget(String target) {
        if (PATH_TRAVERSAL.matcher(target).find()) {
            return Rule.PATH_TRAVERSAL;
        }
        if (SQL_INJECTION.matcher(target).find()) {
            return Rule.SQL_INJECTION;
        }
        if (XSS.matcher(target).find()) {
            return Rule.XSS;
        }
        return null;
    }

    /**
     * URL 解码。非法百分号序列（如单独出现的 %）解码会抛异常，此时保留原文。
     */
    private static String decode(String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
