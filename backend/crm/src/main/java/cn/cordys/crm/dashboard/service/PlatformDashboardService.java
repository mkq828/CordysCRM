package cn.cordys.crm.dashboard.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.dashboard.dto.response.OrgOverviewRow;
import cn.cordys.crm.dashboard.dto.response.PlatformDashboardResponse;
import cn.cordys.crm.dashboard.mapper.ExtPlatformDashboardMapper;
import cn.cordys.crm.system.domain.Organization;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 平台大屏服务（仅 admin）。跨租户聚合 + Redis 缓存。
 */
@Service
public class PlatformDashboardService {

    private static final String CACHE_KEY = "platform_dashboard:overview";
    private static final long CACHE_TTL_SECONDS = 60;
    private static final long ACTIVE_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000;
    private static final long STREAK_WINDOW_MILLIS = 365L * 24 * 60 * 60 * 1000;

    @Resource
    private ExtPlatformDashboardMapper extPlatformDashboardMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public PlatformDashboardResponse overview(String userId) {
        if (!InternalUser.ADMIN.getValue().equals(userId)) {
            throw new GenericException(Translator.get("dashboard.admin_only"));
        }
        String cached = stringRedisTemplate.opsForValue().get(CACHE_KEY);
        if (StringUtils.isNotBlank(cached)) {
            return JSON.parseObject(cached, PlatformDashboardResponse.class);
        }
        PlatformDashboardResponse response = build();
        stringRedisTemplate.opsForValue().set(CACHE_KEY, JSON.toJSONString(response), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return response;
    }

    private PlatformDashboardResponse build() {
        // 排除内置默认组织(100001)：它是配置模板、非真实租户，不应出现在平台大屏
        List<Organization> organizations = extPlatformDashboardMapper.selectAllOrganizations().stream()
                .filter(org -> !OrganizationContext.DEFAULT_ORGANIZATION_ID.equals(org.getId()))
                .toList();
        Map<String, Long> userCounts = toLongMap(extPlatformDashboardMapper.countUsersGroupByOrg());
        Map<String, Long> customerCounts = toLongMap(extPlatformDashboardMapper.countCustomersGroupByOrg());
        Map<String, Long> clueCounts = toLongMap(extPlatformDashboardMapper.countCluesGroupByOrg());
        Map<String, Long> opportunityCounts = toLongMap(extPlatformDashboardMapper.countOpportunitiesGroupByOrg());
        Map<String, Long> orderCounts = toLongMap(extPlatformDashboardMapper.countOrdersGroupByOrg());
        Map<String, BigDecimal> contractAmounts = toBigDecimalMap(extPlatformDashboardMapper.sumContractAmountGroupByOrg());
        Map<String, BigDecimal> receivedAmounts = toBigDecimalMap(extPlatformDashboardMapper.sumReceivedAmountGroupByOrg());
        Map<String, Long> lastLoginTimes = toLongMap(extPlatformDashboardMapper.maxLoginTimeGroupByOrg());

        long now = System.currentTimeMillis();
        Map<String, int[]> loginDayStats = computeLoginDayStats(
                extPlatformDashboardMapper.selectLoginDaysGroupByOrg(now - STREAK_WINDOW_MILLIS));
        Map<String, String> lastLoginCities = toStringMap(extPlatformDashboardMapper.selectLatestLoginCityGroupByOrg());
        Map<String, String> mainLoginCities = toStringMap(
                extPlatformDashboardMapper.selectTopLoginCityGroupByOrg(now - ACTIVE_WINDOW_MILLIS));
        List<OrgOverviewRow> rows = new ArrayList<>();
        long enterprise = 0;
        long personal = 0;
        long activeTenant = 0;
        long totalAccount = 0;
        long totalCustomer = 0;
        long totalOpportunity = 0;
        long totalOrder = 0;
        BigDecimal totalContract = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;

        for (Organization org : organizations) {
            String orgId = org.getId();
            OrgOverviewRow row = new OrgOverviewRow();
            row.setOrganizationId(orgId);
            row.setOrganizationName(org.getName());
            row.setOrgType(org.getOrgType());
            row.setCreateTime(org.getCreateTime());
            row.setAccountCount(value(userCounts.get(orgId)));
            row.setCustomerCount(value(customerCounts.get(orgId)));
            row.setClueCount(value(clueCounts.get(orgId)));
            row.setOpportunityCount(value(opportunityCounts.get(orgId)));
            row.setOrderCount(value(orderCounts.get(orgId)));

            BigDecimal contractAmount = contractAmounts.getOrDefault(orgId, BigDecimal.ZERO);
            BigDecimal receivedAmount = receivedAmounts.getOrDefault(orgId, BigDecimal.ZERO);
            row.setContractAmount(contractAmount);
            row.setReceivedAmount(receivedAmount);
            row.setOutstandingAmount(contractAmount.subtract(receivedAmount));

            Long lastLogin = lastLoginTimes.get(orgId);
            row.setLastLoginTime(lastLogin);
            boolean active = lastLogin != null && (now - lastLogin) <= ACTIVE_WINDOW_MILLIS;
            row.setActive(active);

            int[] stats = loginDayStats.get(orgId);
            row.setUsageDays(stats == null ? 0 : stats[0]);
            row.setActiveDays30(stats == null ? 0 : stats[1]);
            row.setLastLoginCity(lastLoginCities.get(orgId));
            row.setMainLoginCity(mainLoginCities.get(orgId));

            if ("ENTERPRISE".equals(org.getOrgType())) {
                enterprise++;
            } else {
                personal++;
            }
            if (active) {
                activeTenant++;
            }
            totalAccount += row.getAccountCount();
            totalCustomer += row.getCustomerCount();
            totalOpportunity += row.getOpportunityCount();
            totalOrder += row.getOrderCount();
            totalContract = totalContract.add(contractAmount);
            totalReceived = totalReceived.add(receivedAmount);
            rows.add(row);
        }

        // 活跃租户排前，其次按创建时间
        rows.sort((a, b) -> {
            int activeCmp = Boolean.compare(b.isActive(), a.isActive());
            if (activeCmp != 0) {
                return activeCmp;
            }
            return Long.compare(a.getCreateTime() == null ? 0 : a.getCreateTime(),
                    b.getCreateTime() == null ? 0 : b.getCreateTime());
        });

        PlatformDashboardResponse response = new PlatformDashboardResponse();
        response.setTotalTenant(organizations.size());
        response.setEnterpriseTenant(enterprise);
        response.setPersonalTenant(personal);
        response.setActiveTenant(activeTenant);
        response.setTotalAccount(totalAccount);
        response.setTotalCustomer(totalCustomer);
        response.setTotalOpportunity(totalOpportunity);
        response.setTotalOrder(totalOrder);
        response.setTotalContractAmount(totalContract);
        response.setTotalReceivedAmount(totalReceived);
        response.setTotalOutstandingAmount(totalContract.subtract(totalReceived));
        response.setRows(rows);
        return response;
    }

    /**
     * 计算每个租户的「连续使用天数」与「近30天活跃天数」。
     * 连续天数：历史最长连续登录天数（任意起点往前连续的最长天数）；
     * 近30天活跃天数：最近30个自然日内有登录的天数。
     */
    private Map<String, int[]> computeLoginDayStats(List<Map<String, Object>> list) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate thirtyDaysAgo = today.minusDays(29);

        Map<String, Set<LocalDate>> daysByOrg = new HashMap<>();
        for (Map<String, Object> item : list) {
            String orgId = (String) item.get("organizationId");
            Object time = item.get("createTime");
            if (orgId == null || time == null) {
                continue;
            }
            LocalDate day = Instant.ofEpochMilli(((Number) time).longValue()).atZone(zone).toLocalDate();
            daysByOrg.computeIfAbsent(orgId, k -> new HashSet<>()).add(day);
        }

        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, Set<LocalDate>> entry : daysByOrg.entrySet()) {
            Set<LocalDate> days = entry.getValue();
            result.put(entry.getKey(), computeStreak(days, thirtyDaysAgo, today));
        }
        return result;
    }

    /**
     * usageDays = 历史最长连续登录天数（任意起点往前连续的最长天数）；
     * activeDays30 = 近30个自然日内有登录的天数。
     */
    private int[] computeStreak(Set<LocalDate> days, LocalDate thirtyDaysAgo, LocalDate today) {
        List<LocalDate> sorted = days.stream().sorted().toList();
        int maxStreak = 0;
        int current = 0;
        LocalDate prev = null;
        for (LocalDate day : sorted) {
            if (prev != null && day.equals(prev.plusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            maxStreak = Math.max(maxStreak, current);
            prev = day;
        }
        int activeDays30 = 0;
        for (LocalDate day : days) {
            if (!day.isBefore(thirtyDaysAgo) && !day.isAfter(today)) {
                activeDays30++;
            }
        }
        return new int[] { maxStreak, activeDays30 };
    }

    private Map<String, Long> toLongMap(List<Map<String, Object>> list) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> item : list) {
            String orgId = (String) item.get("organizationId");
            Object v = item.get("value");
            map.put(orgId, v == null ? 0L : ((Number) v).longValue());
        }
        return map;
    }

    private Map<String, BigDecimal> toBigDecimalMap(List<Map<String, Object>> list) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> item : list) {
            String orgId = (String) item.get("organizationId");
            Object v = item.get("value");
            map.put(orgId, v == null ? BigDecimal.ZERO : new BigDecimal(v.toString()));
        }
        return map;
    }

    private Map<String, String> toStringMap(List<Map<String, Object>> list) {
        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> item : list) {
            String orgId = (String) item.get("organizationId");
            Object v = item.get("value");
            map.put(orgId, v == null ? null : v.toString());
        }
        return map;
    }

    private long value(Long v) {
        return v == null ? 0L : v;
    }
}
