package cn.cordys.crm.dashboard.service;

import cn.cordys.common.service.BaseService;
import cn.cordys.crm.dashboard.dto.response.AlertItem;
import cn.cordys.crm.dashboard.dto.response.AlertResponse;
import cn.cordys.crm.dashboard.dto.response.StaleBigCustomerRow;
import cn.cordys.crm.dashboard.dto.response.StaleOwnerRow;
import cn.cordys.crm.dashboard.dto.response.TrendPoint;
import cn.cordys.crm.dashboard.mapper.ExtBusinessScreenMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营大屏「主动预警」规则引擎。纯查数 + 阈值判定，不调用 LLM、不改业务数据。
 * 三条规则：合同金额环比大跌 / 大客户久未联系 / 某人多日无跟进。
 */
@Service
@Slf4j
public class AlertService {

    /** 合同金额环比跌幅阈值（30%）。 */
    private static final BigDecimal CONTRACT_DROP_RATIO = new BigDecimal("0.30");

    /** 「大客户」累计合同额门槛（元，100 万）。 */
    private static final BigDecimal BIG_CUSTOMER_AMOUNT = new BigDecimal("1000000");

    /** 大客户久未联系天数阈值。 */
    private static final int STALE_BIG_CUSTOMER_DAYS = 30;

    /** 某人无跟进天数阈值。 */
    private static final int STALE_OWNER_DAYS = 7;

    private static final long DAY_MILLIS = 86_400_000L;

    @Resource
    private ExtBusinessScreenMapper extBusinessScreenMapper;
    @Resource
    private BusinessScreenService businessScreenService;
    @Resource
    private BaseService baseService;

    public AlertResponse alerts(String orgId) {
        List<AlertItem> items = new ArrayList<>();
        items.addAll(contractDropAlerts(orgId));
        items.addAll(staleBigCustomerAlerts(orgId));
        items.addAll(staleOwnerAlerts(orgId));

        AlertResponse response = new AlertResponse();
        response.setItems(items);
        return response;
    }

    /** 环比大跌：复用已补零的近 12 月合同金额趋势，比较最后两个月。 */
    private List<AlertItem> contractDropAlerts(String orgId) {
        List<AlertItem> items = new ArrayList<>();
        List<TrendPoint> trend = businessScreenService.overview(orgId, null).getContractTrend();
        if (trend == null || trend.size() < 2) {
            return items;
        }
        TrendPoint current = trend.get(trend.size() - 1);
        TrendPoint previous = trend.get(trend.size() - 2);
        BigDecimal previousAmount = zeroIfNull(previous.getAmount());
        BigDecimal currentAmount = zeroIfNull(current.getAmount());
        if (previousAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return items;
        }
        BigDecimal ratio = currentAmount.divide(previousAmount, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.ONE.subtract(CONTRACT_DROP_RATIO)) >= 0) {
            return items;
        }

        BigDecimal dropPercent = BigDecimal.ONE.subtract(ratio)
                .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);

        AlertItem item = new AlertItem();
        item.setType(AlertItem.TYPE_CONTRACT_DROP);
        item.setLevel("CRITICAL");
        item.setTitle("合同金额环比大跌");
        item.setDesc(previous.getMonth() + " 合同金额 " + wan(previousAmount) + " 万 → "
                + current.getMonth() + " " + wan(currentAmount) + " 万，环比下降 " + dropPercent + "%");
        item.setTargetKind("trend");
        item.setCurrentAmount(currentAmount);
        item.setPreviousAmount(previousAmount);
        items.add(item);
        return items;
    }

    private List<AlertItem> staleBigCustomerAlerts(String orgId) {
        List<AlertItem> items = new ArrayList<>();
        long staleBefore = System.currentTimeMillis() - STALE_BIG_CUSTOMER_DAYS * DAY_MILLIS;
        List<StaleBigCustomerRow> rows = extBusinessScreenMapper.selectStaleBigCustomer(
                orgId, BIG_CUSTOMER_AMOUNT, staleBefore);
        if (rows == null || rows.isEmpty()) {
            return items;
        }
        Map<String, String> nameMap = enrichNames(rows.stream().map(StaleBigCustomerRow::getOwner).toList());
        for (StaleBigCustomerRow row : rows) {
            AlertItem item = new AlertItem();
            item.setType(AlertItem.TYPE_STALE_BIG_CUSTOMER);
            item.setLevel("WARNING");
            item.setTitle("大客户久未联系");
            item.setDesc(row.getCustomerName() + "（累计合同 " + wan(row.getContractAmount()) + " 万）"
                    + " 已超 " + STALE_BIG_CUSTOMER_DAYS + " 天未跟进，负责人 "
                    + resolveName(nameMap, row.getOwner()));
            item.setTargetKind("sales");
            item.setTargetKey(row.getOwner());
            item.setEntityId(row.getCustomerId());
            item.setEntityName(row.getCustomerName());
            item.setLastFollowTime(row.getFollowTime());
            items.add(item);
        }
        return items;
    }

    private List<AlertItem> staleOwnerAlerts(String orgId) {
        List<AlertItem> items = new ArrayList<>();
        long staleBefore = System.currentTimeMillis() - STALE_OWNER_DAYS * DAY_MILLIS;
        List<StaleOwnerRow> rows = extBusinessScreenMapper.selectStaleOwner(orgId, staleBefore);
        if (rows == null || rows.isEmpty()) {
            return items;
        }
        Map<String, String> nameMap = enrichNames(rows.stream().map(StaleOwnerRow::getOwner).toList());
        long now = System.currentTimeMillis();
        for (StaleOwnerRow row : rows) {
            AlertItem item = new AlertItem();
            item.setType(AlertItem.TYPE_STALE_OWNER);
            item.setLevel("WARNING");
            item.setTitle("负责人多日无跟进");
            String ownerName = resolveName(nameMap, row.getOwner());
            boolean never = row.getLastFollowTime() == null || row.getLastFollowTime() <= 0;
            item.setDesc(never
                    ? ownerName + " 名下客户从未跟进"
                    : ownerName + " 已 " + ((now - row.getLastFollowTime()) / DAY_MILLIS) + " 天未跟进");
            item.setTargetKind("sales");
            item.setTargetKey(row.getOwner());
            item.setEntityName(ownerName);
            item.setLastFollowTime(never ? null : row.getLastFollowTime());
            items.add(item);
        }
        return items;
    }

    private Map<String, String> enrichNames(List<String> ownerIds) {
        List<String> distinct = ownerIds.stream().filter(StringUtils::isNotBlank).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        Map<String, String> raw = baseService.getUserNameMap(distinct);
        Map<String, String> result = new HashMap<>();
        for (String ownerId : distinct) {
            result.put(ownerId, baseService.getAndCheckOptionName(raw.get(ownerId)));
        }
        return result;
    }

    private String resolveName(Map<String, String> nameMap, String ownerId) {
        String name = nameMap.get(ownerId);
        return StringUtils.isBlank(name) ? "—" : name;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String wan(BigDecimal amount) {
        return zeroIfNull(amount).divide(BigDecimal.valueOf(10000), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
