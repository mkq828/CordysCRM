package cn.cordys.crm.platform.service;

import cn.cordys.crm.platform.constants.PlatformContractStatus;
import cn.cordys.crm.platform.constants.PlatformInvoiceStatus;
import cn.cordys.crm.platform.constants.PlatformPaymentVerificationStatus;
import cn.cordys.crm.platform.domain.PlatformContract;
import cn.cordys.crm.platform.domain.PlatformInvoice;
import cn.cordys.crm.platform.domain.PlatformPaymentRecord;
import cn.cordys.crm.platform.dto.request.PlatformRevenueRequest;
import cn.cordys.crm.platform.dto.response.PlatformRevenueOverviewResponse;
import cn.cordys.crm.platform.dto.response.PlatformRevenuePoint;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 平台营收看板服务：合同金额(应收)/实收(现金流)/待收/开票金额 + 周/月/年时间序列（内存聚合）
 */
@Service
public class PlatformRevenueService {

    private static final String GROUP_BY_WEEK = "WEEK";
    private static final String GROUP_BY_YEAR = "YEAR";
    private static final String GROUP_BY_MONTH = "MONTH";

    @Resource
    private BaseMapper<PlatformContract> contractMapper;
    @Resource
    private BaseMapper<PlatformPaymentRecord> recordMapper;
    @Resource
    private BaseMapper<PlatformInvoice> invoiceMapper;
    @Resource
    private BaseMapper<Organization> organizationMapper;

    /**
     * 营收看板总览（KPI + 时间序列）
     */
    public PlatformRevenueOverviewResponse overview(PlatformRevenueRequest request) {
        List<PlatformContract> contracts = contractMapper.selectListByLambda(new LambdaQueryWrapper<PlatformContract>());
        List<PlatformPaymentRecord> records = recordMapper.selectListByLambda(new LambdaQueryWrapper<PlatformPaymentRecord>());
        List<PlatformInvoice> invoices = invoiceMapper.selectListByLambda(new LambdaQueryWrapper<PlatformInvoice>());

        // 排除演示租户：演示数据不进全局营收
        Set<String> demoOrgIds = DataAccessLayer.with(Organization.class)
                .selectListByLambda(new LambdaQueryWrapper<Organization>()).stream()
                .filter(o -> Boolean.TRUE.equals(o.getDemo()))
                .map(Organization::getId)
                .collect(Collectors.toSet());
        contracts = contracts.stream().filter(c -> !demoOrgIds.contains(c.getOrganizationId())).toList();
        records = records.stream().filter(r -> !demoOrgIds.contains(r.getOrganizationId())).toList();
        invoices = invoices.stream().filter(i -> !demoOrgIds.contains(i.getOrganizationId())).toList();

        String groupBy = StringUtils.isBlank(request.getGroupBy()) ? GROUP_BY_MONTH : request.getGroupBy();
        Long start = request.getStartTime();
        Long end = request.getEndTime();

        // 合同金额(应收)：已生效合同（已完成/已归档，排除草稿/待签署/作废），按创建时间
        List<PlatformContract> contractsInRange = contracts.stream()
                .filter(c -> PlatformContractStatus.COMPLETED.name().equals(c.getStatus())
                        || PlatformContractStatus.ARCHIVED.name().equals(c.getStatus()))
                .filter(c -> inRange(c.getCreateTime(), start, end))
                .toList();
        BigDecimal contractAmount = sum(contractsInRange, PlatformContract::getAmount);

        // 实收(现金流)：已核销回款，按核销时间
        List<PlatformPaymentRecord> doneRecords = records.stream()
                .filter(r -> PlatformPaymentVerificationStatus.DONE.name().equals(r.getVerificationStatus()))
                .filter(r -> inRange(r.getVerifyTime(), start, end))
                .toList();
        BigDecimal receivedAmount = sum(doneRecords, PlatformPaymentRecord::getAmount);

        // 待收：应收合同集合的合同金额 − 这些合同的已核销回款（口径一致，避免跨期/作废算成负数）
        Set<String> contractIds = contractsInRange.stream()
                .map(PlatformContract::getId)
                .collect(Collectors.toSet());
        BigDecimal receivedForContracts = sum(records.stream()
                .filter(r -> PlatformPaymentVerificationStatus.DONE.name().equals(r.getVerificationStatus()))
                .filter(r -> contractIds.contains(r.getContractId()))
                .toList(), PlatformPaymentRecord::getAmount);
        BigDecimal outstandingAmount = contractAmount.subtract(receivedForContracts);

        // 开票金额(发票)：已开票，按创建时间
        List<PlatformInvoice> invoicedList = invoices.stream()
                .filter(i -> PlatformInvoiceStatus.INVOICED.name().equals(i.getInvoiceStatus()))
                .filter(i -> inRange(i.getCreateTime(), start, end))
                .toList();
        BigDecimal invoiceAmount = sum(invoicedList, PlatformInvoice::getAmount);

        PlatformRevenueOverviewResponse response = new PlatformRevenueOverviewResponse();
        response.setContractAmount(contractAmount);
        response.setReceivedAmount(receivedAmount);
        response.setOutstandingAmount(outstandingAmount);
        response.setInvoiceAmount(invoiceAmount);
        response.setSeries(buildSeries(contractsInRange, doneRecords, invoicedList, groupBy));
        return response;
    }

    /**
     * 按周/月/年聚合时间序列（桶名字典序排序）
     */
    private List<PlatformRevenuePoint> buildSeries(List<PlatformContract> contracts,
                                                   List<PlatformPaymentRecord> records,
                                                   List<PlatformInvoice> invoices,
                                                   String groupBy) {
        Map<String, BigDecimal> contractMap = new LinkedHashMap<>();
        Map<String, BigDecimal> receivedMap = new LinkedHashMap<>();
        Map<String, BigDecimal> invoiceMap = new LinkedHashMap<>();
        contracts.forEach(c -> contractMap.merge(bucket(c.getCreateTime(), groupBy), nvl(c.getAmount()), BigDecimal::add));
        records.forEach(r -> receivedMap.merge(bucket(r.getVerifyTime(), groupBy), nvl(r.getAmount()), BigDecimal::add));
        invoices.forEach(i -> invoiceMap.merge(bucket(i.getCreateTime(), groupBy), nvl(i.getAmount()), BigDecimal::add));

        java.util.Set<String> buckets = new TreeSet<>();
        buckets.addAll(contractMap.keySet());
        buckets.addAll(receivedMap.keySet());
        buckets.addAll(invoiceMap.keySet());

        List<PlatformRevenuePoint> series = new ArrayList<>();
        for (String bucket : buckets) {
            PlatformRevenuePoint point = new PlatformRevenuePoint();
            point.setBucket(bucket);
            point.setContractAmount(contractMap.getOrDefault(bucket, BigDecimal.ZERO));
            point.setReceivedAmount(receivedMap.getOrDefault(bucket, BigDecimal.ZERO));
            point.setInvoiceAmount(invoiceMap.getOrDefault(bucket, BigDecimal.ZERO));
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

    private <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> extractor) {
        return list.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
