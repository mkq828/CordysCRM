package cn.cordys.crm.ai.advisor;

import java.time.LocalDate;

/**
 * 军师查数语义层白名单（第一版：客户 + 跟进两个数据域）。
 * <p>
 * 与问数 BI 同理：所有表名、列名在此硬编码，LLM 只产出目录内的 action 与参数；
 * 编译层据此拼装参数化只读 SQL，并无条件注入 organization_id 与「本人+下级」负责人范围，
 * 从结构上杜绝 SQL 注入与越权。
 * </p>
 */
public final class AdvisorDataCatalog {

    // ==================== 查询动作 ====================
    public static final String ACTION_LIST_CUSTOMERS = "listCustomers";
    public static final String ACTION_CUSTOMER_FOLLOWS = "customerFollows";
    public static final String ACTION_RECENT_FOLLOWS = "recentFollows";
    public static final String ACTION_NONE = "none";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private AdvisorDataCatalog() {
    }

    /** LLM 产出的查询意图（action 必须命中目录，参数白名单化）。 */
    public record QueryIntent(String action, String name, Boolean sharedPool, Integer staleDays,
                              Integer withinDays, Integer limit) {
    }

    public static String intentPrompt() {
        return "你是企业 CRM 的「销售数据助手」，把销售的大白话问题转成一段查询指令 JSON，交给系统查数据库。\n"
                + "今天日期：" + LocalDate.now() + "\n\n"
                + "你只能查「销售本人及下属」负责的客户与跟进数据，支持三种查询：\n\n"
                + "1. " + ACTION_LIST_CUSTOMERS + "（查客户列表）\n"
                + "   - name：客户名称关键词，模糊匹配，可选\n"
                + "   - sharedPool：是否在公海，true/false，可选\n"
                + "   - staleDays：超过 N 天未跟进（含从未跟进）的客户，数字，可选\n"
                + "2. " + ACTION_CUSTOMER_FOLLOWS + "（查某个客户的跟进记录）\n"
                + "   - name：客户名称关键词，必填\n"
                + "   - withinDays：最近 N 天内的跟进，数字，可选\n"
                + "3. " + ACTION_RECENT_FOLLOWS + "（查本人及下属最近的跟进记录）\n"
                + "   - withinDays：最近 N 天内的跟进，数字，可选\n\n"
                + "输出只能是 JSON（不要 markdown、不要解释），结构如下：\n"
                + "{\"action\":\"" + ACTION_LIST_CUSTOMERS + "\",\"name\":null,\"sharedPool\":null,"
                + "\"staleDays\":7,\"withinDays\":null,\"limit\":10}\n\n"
                + "规则：\n"
                + "- 「张三最近跟进到哪了」→ " + ACTION_CUSTOMER_FOLLOWS + "，name=张三\n"
                + "- 「我名下有哪些客户 7 天没跟进了」→ " + ACTION_LIST_CUSTOMERS + "，staleDays=7\n"
                + "- 「最近我跟进了哪些客户」→ " + ACTION_RECENT_FOLLOWS + "\n"
                + "- 「公海里有哪些客户」→ " + ACTION_LIST_CUSTOMERS + "，sharedPool=true\n"
                + "- 「最近 30 天跟进了哪些客户」→ " + ACTION_RECENT_FOLLOWS + "，withinDays=30\n"
                + "- 不是查数据的问题（如让你分析一段对话、闲聊、问业务规则）→ action=" + ACTION_NONE + "\n"
                + "- name 只填客户名关键词，不要带「客户/公司」等多余字；limit 默认 " + DEFAULT_LIMIT + "，最大 " + MAX_LIMIT + "。";
    }

    public static String answerPrompt() {
        return "你是企业 CRM 的销售军师。下面是一段查到的客户/跟进数据（JSON 数组，每行是一组结果，字段名即列名），"
                + "请你结合销售的问题，用自然语言把数据讲清楚。\n\n"
                + "要求：\n"
                + "- 直接回答结论，不要「根据数据显示」「好的」这类废话\n"
                + "- 时间用「9月28日 14:30」这样的可读格式，负责人用姓名\n"
                + "- 数据为空时，明确说「你名下没有相关数据」\n"
                + "- 客户列表按跟进紧急程度或时间排序讲重点，最多展开前 10 条\n"
                + "- 回答简洁，销售能一眼看懂，不要堆砌全部原始字段";
    }

    public static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
