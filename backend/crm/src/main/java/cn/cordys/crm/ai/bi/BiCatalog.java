package cn.cordys.crm.ai.bi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 问数 BI（功能 12）语义层目录：可查询的指标 / 维度白名单。
 * <p>
 * 所有表名、列名、函数在此硬编码，LLM 只产出目录内的 key；SQL 编译层据此拼装参数化只读 SQL，
 * 无条件注入 {@code organization_id}，从结构上杜绝 SQL 注入与跨租户越权。
 * </p>
 */
public final class BiCatalog {

    /** 指标定义：fromSql/selectSql/whereSql 均为受控 SQL 片段；dimensions 为该指标支持的维度（含具体 SQL）。 */
    public record Metric(String key, String name, String fromSql, String selectSql, String whereSql,
                         String orgColumn, String timeColumn, Map<String, Dimension> dimensions) {
    }

    /** 维度定义：selectSql 为展示列表达式，groupSql 为 GROUP BY 表达式。 */
    public record Dimension(String name, String selectSql, String groupSql) {
    }

    public static final String DIM_TIME_DAY = "time_day";
    public static final String DIM_TIME_WEEK = "time_week";
    public static final String DIM_TIME_MONTH = "time_month";

    private static final String CONTRACT_FROM =
            "contract c LEFT JOIN customer cu ON cu.id = c.customer_id";
    private static final String PAYMENT_FROM =
            "contract_payment_record p LEFT JOIN contract c ON c.id = p.contract_id "
                    + "LEFT JOIN customer cu ON cu.id = c.customer_id";

    /** 指标目录（key = LLM 产出的 metric）。 */
    public static final Map<String, Metric> METRICS = Map.of(
            "contract_amount", new Metric("contract_amount", "合同金额", CONTRACT_FROM,
                    "SUM(c.amount)", null, "c.organization_id", "c.start_time",
                    contractDims("c.start_time")),
            "contract_count", new Metric("contract_count", "合同数", CONTRACT_FROM,
                    "COUNT(c.id)", null, "c.organization_id", "c.start_time",
                    contractDims("c.start_time")),
            "contract_avg_amount", new Metric("contract_avg_amount", "平均合同额", CONTRACT_FROM,
                    "AVG(c.amount)", null, "c.organization_id", "c.start_time",
                    contractDims("c.start_time")),
            "payment_amount", new Metric("payment_amount", "回款金额", PAYMENT_FROM,
                    "SUM(p.record_amount)", null, "p.organization_id", "p.record_end_time",
                    paymentDims("p.record_end_time")),
            "payment_verified_amount", new Metric("payment_verified_amount", "已核销回款金额", PAYMENT_FROM,
                    "SUM(p.record_amount)", "p.verification_status = 'DONE'", "p.organization_id", "p.record_end_time",
                    paymentVerifiedDims("p.record_end_time")),
            "customer_count", new Metric("customer_count", "客户数", "customer cu",
                    "COUNT(cu.id)", null, "cu.organization_id", "cu.create_time",
                    customerDims("cu.create_time"))
    );

    /** 首页/输入框下方的示例问题（LLM 之外的确定性引导）。 */
    public static final List<String> SAMPLE_QUESTIONS = List.of(
            "上月各客户回款金额 Top10",
            "本月新签合同额是多少",
            "近 6 个月每月回款趋势",
            "各负责人已核销回款金额",
            "今年新增客户数"
    );

    /** 可查范围提示（前端橙色小字，多行展示）。 */
    public static final String SCOPE_HINT =
            "数据范围：客户、合同、回款三个列表。\n"
            + "可查指标：合同金额、合同数、平均合同额、回款金额、已核销回款金额、客户数。\n"
            + "可分组：客户、负责人、日期/周/月、合同阶段、审批状态、核销状态。";

    private static Map<String, Dimension> contractDims(String timeCol) {
        Map<String, Dimension> m = new LinkedHashMap<>();
        m.put("customer", new Dimension("客户", "cu.name", "cu.id, cu.name"));
        m.put("owner", new Dimension("负责人", "c.owner", "c.owner"));
        m.put("contract_stage", new Dimension("合同阶段", "c.stage", "c.stage"));
        m.put("contract_approval", new Dimension("审批状态", "c.approval_status", "c.approval_status"));
        timeDims(m, timeCol);
        return m;
    }

    private static Map<String, Dimension> paymentDims(String timeCol) {
        Map<String, Dimension> m = new LinkedHashMap<>();
        m.put("customer", new Dimension("客户", "cu.name", "cu.id, cu.name"));
        m.put("owner", new Dimension("负责人", "c.owner", "c.owner"));
        m.put("payment_verify_status", new Dimension("回款核销状态", "p.verification_status", "p.verification_status"));
        timeDims(m, timeCol);
        return m;
    }

    private static Map<String, Dimension> paymentVerifiedDims(String timeCol) {
        Map<String, Dimension> m = new LinkedHashMap<>();
        m.put("customer", new Dimension("客户", "cu.name", "cu.id, cu.name"));
        m.put("owner", new Dimension("负责人", "c.owner", "c.owner"));
        timeDims(m, timeCol);
        return m;
    }

    private static Map<String, Dimension> customerDims(String timeCol) {
        Map<String, Dimension> m = new LinkedHashMap<>();
        m.put("owner", new Dimension("负责人", "cu.owner", "cu.owner"));
        timeDims(m, timeCol);
        return m;
    }

    private static void timeDims(Map<String, Dimension> m, String timeCol) {
        m.put(DIM_TIME_DAY, new Dimension("日期",
                dateFmt(timeCol, "%Y-%m-%d"), dateFmt(timeCol, "%Y-%m-%d")));
        m.put(DIM_TIME_WEEK, new Dimension("周",
                dateFmt(timeCol, "%Y-%u"), dateFmt(timeCol, "%Y-%u")));
        m.put(DIM_TIME_MONTH, new Dimension("月份",
                dateFmt(timeCol, "%Y-%m"), dateFmt(timeCol, "%Y-%m")));
    }

    private static String dateFmt(String timeCol, String fmt) {
        return "DATE_FORMAT(FROM_UNIXTIME(" + timeCol + "/1000), '" + fmt + "')";
    }

    private BiCatalog() {
    }
}
