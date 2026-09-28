package cn.cordys.crm.platform.service;

import cn.cordys.crm.platform.constants.PlatformContractStatus;
import cn.cordys.crm.platform.constants.PlatformPaymentVerificationStatus;
import cn.cordys.crm.platform.domain.PlatformCityManager;
import cn.cordys.crm.platform.domain.PlatformContract;
import cn.cordys.crm.platform.domain.PlatformPaymentRecord;
import cn.cordys.crm.platform.dto.request.CityManagerPerformanceRequest;
import cn.cordys.crm.platform.dto.response.CityManagerPerformanceOverviewResponse;
import cn.cordys.crm.platform.dto.response.CityManagerPerformancePoint;
import cn.cordys.crm.platform.dto.response.CityManagerPerformanceSummary;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 城市经理业绩看板：签约客户数 / 合同金额 / 回款金额，按经理汇总 + 周/月/年时间序列（内存聚合）。
 * 业绩口径以租户表 sign/follow_manager_id 为主，合同表 sign_manager_id 为签约快照。
 */
@Service
public class CityManagerPerformanceService {

    private static final String GROUP_BY_WEEK = "WEEK";
    private static final String GROUP_BY_YEAR = "YEAR";
    private static final String GROUP_BY_MONTH = "MONTH";

    @Resource
    private BaseMapper<PlatformCityManager> cityManagerMapper;
    @Resource
    private BaseMapper<Organization> organizationMapper;
    @Resource
    private BaseMapper<PlatformContract> contractMapper;
    @Resource
    private BaseMapper<PlatformPaymentRecord> recordMapper;

    /**
     * 业绩看板总览（按经理汇总 + 时间序列）
     */
    public CityManagerPerformanceOverviewResponse overview(CityManagerPerformanceRequest request) {
        List<PlatformCityManager> managers = cityManagerMapper.selectListByLambda(
                new LambdaQueryWrapper<PlatformCityManager>());
        List<Organization> orgs = DataAccessLayer.with(Organization.class).selectListByLambda(new LambdaQueryWrapper<Organization>());
        List<PlatformContract> contracts = contractMapper.selectListByLambda(new LambdaQueryWrapper<PlatformContract>());
        List<PlatformPaymentRecord> records = recordMapper.selectListByLambda(new LambdaQueryWrapper<PlatformPaymentRecord>());

        // 排除演示租户：演示数据不进业绩（演示通过进入 demo 租户工作台展示，不计入签约/合同/回款）
        Set<String> demoOrgIds = orgs.stream()
                .filter(o -> Boolean.TRUE.equals(o.getDemo()))
                .map(Organization::getId)
                .collect(Collectors.toSet());
        orgs = orgs.stream().filter(o -> !demoOrgIds.contains(o.getId())).toList();
        contracts = contracts.stream().filter(c -> !demoOrgIds.contains(c.getOrganizationId())).toList();
        records = records.stream().filter(r -> !demoOrgIds.contains(r.getOrganizationId())).toList();

        String groupBy = StringUtils.isBlank(request.getGroupBy()) ? GROUP_BY_MONTH : request.getGroupBy();
        Long start = request.getStartTime();
        Long end = request.getEndTime();
        String managerId = request.getManagerId();

        // 目标经理集合（admin 空=全部，city_manager=本人）
        List<PlatformCityManager> targetManagers = managers.stream()
                .filter(m -> StringUtils.isBlank(managerId) || managerId.equals(m.getId()))
                .toList();
        Set<String> targetManagerIds = targetManagers.stream()
                .map(PlatformCityManager::getId)
                .collect(Collectors.toSet());

        // 签约客户数：租户表 sign_manager_id 口径
        Map<String, Long> signedCountByManager = orgs.stream()
                .filter(o -> o.getSignManagerId() != null && targetManagerIds.contains(o.getSignManagerId()))
                .collect(Collectors.groupingBy(Organization::getSignManagerId, Collectors.counting()));

        // 合同金额：合同表 sign_manager_id + 已生效(已完成/已归档)
        List<PlatformContract> effectiveContracts = contracts.stream()
                .filter(c -> c.getSignManagerId() != null && targetManagerIds.contains(c.getSignManagerId()))
                .filter(c -> PlatformContractStatus.COMPLETED.name().equals(c.getStatus())
                        || PlatformContractStatus.ARCHIVED.name().equals(c.getStatus()))
                .filter(c -> inRange(c.getCreateTime(), start, end))
                .toList();
        Map<String, BigDecimal> contractAmountByManager = new HashMap<>();
        for (PlatformContract c : effectiveContracts) {
            if (c.getAmount() != null) {
                contractAmountByManager.merge(c.getSignManagerId(), c.getAmount(), BigDecimal::add);
            }
        }

        // 回款金额：跟进经理名下租户 -> 已核销回款
        Set<String> followOrgIds = orgs.stream()
                .filter(o -> o.getFollowManagerId() != null && targetManagerIds.contains(o.getFollowManagerId()))
                .map(Organization::getId)
                .collect(Collectors.toSet());
        Map<String, String> orgFollowManager = orgs.stream()
                .filter(o -> o.getFollowManagerId() != null)
                .collect(Collectors.toMap(Organization::getId, Organization::getFollowManagerId, (a, b) -> a));
        List<PlatformPaymentRecord> doneRecords = records.stream()
                .filter(r -> PlatformPaymentVerificationStatus.DONE.name().equals(r.getVerificationStatus()))
                .filter(r -> followOrgIds.contains(r.getOrganizationId()))
                .filter(r -> inRange(r.getVerifyTime(), start, end))
                .toList();
        Map<String, BigDecimal> paymentAmountByManager = new HashMap<>();
        for (PlatformPaymentRecord r : doneRecords) {
            String fm = orgFollowManager.get(r.getOrganizationId());
            if (fm != null) {
                paymentAmountByManager.merge(fm, nvl(r.getAmount()), BigDecimal::add);
            }
        }

        List<CityManagerPerformanceSummary> summary = targetManagers.stream().map(m -> {
            CityManagerPerformanceSummary s = new CityManagerPerformanceSummary();
            s.setManagerId(m.getId());
            s.setName(m.getName());
            s.setSignedCount(signedCountByManager.getOrDefault(m.getId(), 0L));
            s.setContractAmount(contractAmountByManager.getOrDefault(m.getId(), BigDecimal.ZERO));
            s.setPaymentAmount(paymentAmountByManager.getOrDefault(m.getId(), BigDecimal.ZERO));
            return s;
        }).toList();

        CityManagerPerformanceOverviewResponse response = new CityManagerPerformanceOverviewResponse();
        response.setSummary(summary);
        response.setSeries(buildSeries(effectiveContracts, doneRecords, groupBy));
        return response;
    }

    /**
     * 按周/月/年聚合时间序列（签约/合同按合同创建时间，回款按核销时间）
     */
    private List<CityManagerPerformancePoint> buildSeries(List<PlatformContract> effectiveContracts,
                                                          List<PlatformPaymentRecord> doneRecords,
                                                          String groupBy) {
        Map<String, Set<String>> signedOrgBuckets = new LinkedHashMap<>();
        Map<String, BigDecimal> contractBucket = new LinkedHashMap<>();
        for (PlatformContract c : effectiveContracts) {
            String b = bucket(c.getCreateTime(), groupBy);
            signedOrgBuckets.computeIfAbsent(b, k -> new HashSet<>()).add(c.getOrganizationId());
            contractBucket.merge(b, nvl(c.getAmount()), BigDecimal::add);
        }
        Map<String, BigDecimal> paymentBucket = new LinkedHashMap<>();
        for (PlatformPaymentRecord r : doneRecords) {
            paymentBucket.merge(bucket(r.getVerifyTime(), groupBy), nvl(r.getAmount()), BigDecimal::add);
        }

        java.util.Set<String> buckets = new TreeSet<>();
        buckets.addAll(signedOrgBuckets.keySet());
        buckets.addAll(contractBucket.keySet());
        buckets.addAll(paymentBucket.keySet());

        List<CityManagerPerformancePoint> series = new ArrayList<>();
        for (String b : buckets) {
            CityManagerPerformancePoint point = new CityManagerPerformancePoint();
            point.setBucket(b);
            point.setSignedCount((long) signedOrgBuckets.getOrDefault(b, Set.of()).size());
            point.setContractAmount(contractBucket.getOrDefault(b, BigDecimal.ZERO));
            point.setPaymentAmount(paymentBucket.getOrDefault(b, BigDecimal.ZERO));
            series.add(point);
        }
        return series;
    }

    private String bucket(Long time, String groupBy) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        if (GROUP_BY_WEEK.equals(groupBy)) {
            WeekFields wf = WeekFields.ISO;
            return String.format("%d-W%02d", dt.get(wf.weekBasedYear()), dt.get(wf.weekOfWeekBasedYear()));
        }
        if (GROUP_BY_YEAR.equals(groupBy)) {
            return String.valueOf(dt.getYear());
        }
        return String.format("%04d-%02d", dt.getYear(), dt.getMonthValue());
    }

    private boolean inRange(Long time, Long start, Long end) {
        if (time == null) {
            return false;
        }
        if (start != null && time < start) {
            return false;
        }
        return end == null || time <= end;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
