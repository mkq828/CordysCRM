package cn.cordys.crm.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 经营大屏时间范围切换（今日 / 本周 / 本月 / 本季 / 本年 / 累计）。
 *
 * <p>语义：切换后，大屏核心指标与趋势统一按该时间窗口聚合（计数=窗口内新增/发生数，金额=窗口内合计）。
 * 「累计」等价于当前行为（KPI 全部历史 + 近 12 月趋势）。</p>
 */
public enum TimeRange {

    TODAY("today"),
    WEEK("week"),
    MONTH("month"),
    QUARTER("quarter"),
    YEAR("year"),
    ALL("all");

    private final String code;

    TimeRange(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 解析前端传入的 range 编码，未知值回退为「累计」。 */
    public static TimeRange from(String code) {
        if (code == null || code.isBlank()) {
            return ALL;
        }
        for (TimeRange range : values()) {
            if (range.code.equalsIgnoreCase(code)) {
                return range;
            }
        }
        return ALL;
    }

    /** 趋势是否按「日」粒度（今日 / 本周 / 本月）；其余按月。 */
    public boolean isDaily() {
        return this == TODAY || this == WEEK || this == MONTH;
    }

    /** 趋势聚合粒度：DAY / MONTH（与 Mapper XML 的 {@code granularity} 参数对应）。 */
    public String granularity() {
        return isDaily() ? "DAY" : "MONTH";
    }

    /** 统计窗口下界（毫秒）；「累计」返回 0，表示不加时间下界。 */
    public long startMillis(ZoneId zone) {
        LocalDate start = switch (this) {
            case TODAY -> LocalDate.now(zone);
            case WEEK -> LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> LocalDate.now(zone).withDayOfMonth(1);
            case QUARTER -> quarterStart(LocalDate.now(zone));
            case YEAR -> LocalDate.now(zone).withDayOfYear(1);
            case ALL -> null;
        };
        return start == null ? 0L : start.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /** 趋势查询的数据下界（毫秒）。「累计」回溯近 12 月，其余同统计窗口下界。 */
    public long trendSinceMillis(ZoneId zone) {
        if (this == ALL) {
            return YearMonth.now(zone).minusMonths(11).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
        }
        return startMillis(zone);
    }

    /** 趋势轴标签（与 Mapper XML 的 DATE_FORMAT 输出一致），用于补零。 */
    public List<String> trendLabels(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        List<String> labels = new ArrayList<>();
        if (isDaily()) {
            LocalDate start = switch (this) {
                case TODAY -> today;
                case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                default -> today.withDayOfMonth(1);
            };
            for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
                labels.add(day.toString());
            }
            return labels;
        }
        YearMonth current = YearMonth.now(zone);
        YearMonth start = switch (this) {
            case QUARTER -> YearMonth.of(current.getYear(), ((current.getMonthValue() - 1) / 3) * 3 + 1);
            case YEAR -> YearMonth.of(current.getYear(), 1);
            default -> current.minusMonths(11);
        };
        int months = this == QUARTER ? 3 : 12;
        for (int i = 0; i < months; i++) {
            labels.add(start.plusMonths(i).toString());
        }
        return labels;
    }

    private LocalDate quarterStart(LocalDate date) {
        int month = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return date.withMonth(month).withDayOfMonth(1);
    }
}
