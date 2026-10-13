package cn.cordys.crm.dashboard.service;

import cn.cordys.common.service.BaseService;
import cn.cordys.crm.dashboard.dto.response.BusinessScreenKpi;
import cn.cordys.crm.dashboard.dto.response.BusinessScreenOverviewResponse;
import cn.cordys.crm.dashboard.dto.response.CustomerBrief;
import cn.cordys.crm.dashboard.dto.response.DistributionItem;
import cn.cordys.crm.dashboard.dto.response.Funnel;
import cn.cordys.crm.dashboard.dto.response.RankItem;
import cn.cordys.crm.dashboard.dto.response.RegionDetailResponse;
import cn.cordys.crm.dashboard.dto.response.RegionItem;
import cn.cordys.crm.dashboard.dto.response.RegionKpi;
import cn.cordys.crm.dashboard.dto.response.SalesKpi;
import cn.cordys.crm.dashboard.dto.response.SalesOverviewResponse;
import cn.cordys.crm.dashboard.dto.response.StageContractItem;
import cn.cordys.crm.dashboard.dto.response.StageDetailResponse;
import cn.cordys.crm.dashboard.dto.response.TrendPoint;
import cn.cordys.crm.dashboard.mapper.ExtBusinessScreenMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营大屏服务（租户视角）。老板全局视角：仅按 organization_id 隔离，不按负责人过滤。
 */
@Service
@Slf4j
public class BusinessScreenService {

    /** 趋势图回溯月数（近12月）。 */
    private static final int TREND_MONTHS = 12;

    @Resource
    private ExtBusinessScreenMapper extBusinessScreenMapper;
    @Resource
    private BaseService baseService;

    public BusinessScreenOverviewResponse overview(String orgId) {
        ZoneId zone = ZoneId.systemDefault();
        YearMonth current = YearMonth.now(zone);
        long monthStart = current.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long trendSince = current.minusMonths(TREND_MONTHS - 1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();

        BusinessScreenKpi kpi = extBusinessScreenMapper.selectKpi(orgId, monthStart);
        if (kpi == null) {
            kpi = new BusinessScreenKpi();
        }
        kpi.setTotalContractAmount(zeroIfNull(kpi.getTotalContractAmount()));
        kpi.setVerifiedAmount(zeroIfNull(kpi.getVerifiedAmount()));
        kpi.setPendingAmount(kpi.getTotalContractAmount().subtract(kpi.getVerifiedAmount()));

        List<String> months = months(current);
        List<TrendPoint> contractTrend = fillTrend(months, extBusinessScreenMapper.selectContractTrend(orgId, trendSince));
        List<TrendPoint> paymentTrend = fillTrend(months, extBusinessScreenMapper.selectPaymentTrend(orgId, trendSince));

        List<RankItem> salesRank = extBusinessScreenMapper.selectSalesRank(orgId);
        if (salesRank == null) {
            salesRank = new ArrayList<>();
        }
        enrichOwnerNames(salesRank);

        List<DistributionItem> contractStageDist = extBusinessScreenMapper.selectContractStageDist(orgId);
        if (contractStageDist == null) {
            contractStageDist = new ArrayList<>();
        }

        List<RegionItem> customerRegionDist = extBusinessScreenMapper.selectCustomerRegionDist(orgId);
        if (customerRegionDist == null) {
            customerRegionDist = new ArrayList<>();
        }

        Funnel funnel = new Funnel();
        funnel.setClue(kpi.getClueCount());
        funnel.setCustomer(kpi.getCustomerCount());
        funnel.setOpportunity(kpi.getOpportunityCount());
        funnel.setOrder(kpi.getOrderCount());
        funnel.setContract(kpi.getContractCount());

        BusinessScreenOverviewResponse response = new BusinessScreenOverviewResponse();
        response.setKpi(kpi);
        response.setContractTrend(contractTrend);
        response.setPaymentTrend(paymentTrend);
        response.setSalesRank(salesRank);
        response.setContractStageDist(contractStageDist);
        response.setCustomerRegionDist(customerRegionDist);
        response.setFunnel(funnel);
        return response;
    }

    public SalesOverviewResponse salesDetail(String orgId, String ownerId) {
        ZoneId zone = ZoneId.systemDefault();
        YearMonth current = YearMonth.now(zone);
        long monthStart = current.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long trendSince = current.minusMonths(TREND_MONTHS - 1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();

        SalesKpi kpi = extBusinessScreenMapper.selectSalesKpi(orgId, ownerId, monthStart);
        if (kpi == null) {
            kpi = new SalesKpi();
        }
        kpi.setContractAmount(zeroIfNull(kpi.getContractAmount()));
        kpi.setVerifiedAmount(zeroIfNull(kpi.getVerifiedAmount()));
        kpi.setPendingAmount(kpi.getContractAmount().subtract(kpi.getVerifiedAmount()));
        kpi.setMonthNewContractAmount(zeroIfNull(kpi.getMonthNewContractAmount()));

        List<String> months = months(current);
        List<TrendPoint> contractTrend = fillTrend(months, extBusinessScreenMapper.selectSalesContractTrend(orgId, ownerId, trendSince));
        List<TrendPoint> paymentTrend = fillTrend(months, extBusinessScreenMapper.selectSalesPaymentTrend(orgId, ownerId, trendSince));

        List<DistributionItem> stageDist = extBusinessScreenMapper.selectSalesStageDist(orgId, ownerId);
        if (stageDist == null) {
            stageDist = new ArrayList<>();
        }

        List<CustomerBrief> customerList = extBusinessScreenMapper.selectSalesCustomerList(orgId, ownerId);
        if (customerList == null) {
            customerList = new ArrayList<>();
        }

        SalesOverviewResponse response = new SalesOverviewResponse();
        response.setKpi(kpi);
        response.setContractTrend(contractTrend);
        response.setPaymentTrend(paymentTrend);
        response.setStageDist(stageDist);
        response.setCustomerList(customerList);
        return response;
    }

    public RegionDetailResponse regionDetail(String orgId, String adcode) {
        RegionKpi kpi = extBusinessScreenMapper.selectRegionKpi(orgId, adcode);
        if (kpi == null) {
            kpi = new RegionKpi();
        }
        kpi.setContractAmount(zeroIfNull(kpi.getContractAmount()));
        kpi.setVerifiedAmount(zeroIfNull(kpi.getVerifiedAmount()));
        kpi.setPendingAmount(kpi.getContractAmount().subtract(kpi.getVerifiedAmount()));

        List<CustomerBrief> customerList = extBusinessScreenMapper.selectRegionCustomerList(orgId, adcode);
        if (customerList == null) {
            customerList = new ArrayList<>();
        }

        List<RankItem> salesDist = extBusinessScreenMapper.selectRegionSalesDist(orgId, adcode);
        if (salesDist == null) {
            salesDist = new ArrayList<>();
        }
        enrichOwnerNames(salesDist);

        RegionDetailResponse response = new RegionDetailResponse();
        response.setKpi(kpi);
        response.setCustomerList(customerList);
        response.setSalesDist(salesDist);
        return response;
    }

    public StageDetailResponse stageDetail(String orgId, String stage) {
        String stageName = extBusinessScreenMapper.selectStageName(orgId, stage);
        if (StringUtils.isBlank(stageName)) {
            stageName = stage;
        }

        List<StageContractItem> list = extBusinessScreenMapper.selectStageContractList(orgId, stage);
        if (list == null) {
            list = new ArrayList<>();
        }
        enrichStageOwnerNames(list);

        long count = list.size();
        BigDecimal totalAmount = list.stream()
                .map(StageContractItem::getAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StageDetailResponse response = new StageDetailResponse();
        response.setStageName(stageName);
        response.setCount(count);
        response.setTotalAmount(totalAmount);
        response.setList(list);
        return response;
    }

    private List<String> months(YearMonth current) {
        List<String> list = new ArrayList<>(TREND_MONTHS);
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            list.add(current.minusMonths(i).toString());
        }
        return list;
    }

    private List<TrendPoint> fillTrend(List<String> months, List<TrendPoint> rows) {
        Map<String, BigDecimal> byMonth = new HashMap<>();
        if (rows != null) {
            for (TrendPoint row : rows) {
                byMonth.put(row.getMonth(), zeroIfNull(row.getAmount()));
            }
        }
        List<TrendPoint> result = new ArrayList<>(months.size());
        for (String month : months) {
            TrendPoint point = new TrendPoint();
            point.setMonth(month);
            point.setAmount(byMonth.getOrDefault(month, BigDecimal.ZERO));
            result.add(point);
        }
        return result;
    }

    private void enrichOwnerNames(List<RankItem> rank) {
        List<String> ownerIds = rank.stream()
                .map(RankItem::getOwner)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (ownerIds.isEmpty()) {
            return;
        }
        Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);
        for (RankItem item : rank) {
            item.setOwnerName(baseService.getAndCheckOptionName(userNameMap.get(item.getOwner())));
        }
    }

    private void enrichStageOwnerNames(List<StageContractItem> list) {
        List<String> ownerIds = list.stream()
                .map(StageContractItem::getOwner)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (ownerIds.isEmpty()) {
            return;
        }
        Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);
        for (StageContractItem item : list) {
            item.setOwnerName(baseService.getAndCheckOptionName(userNameMap.get(item.getOwner())));
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
