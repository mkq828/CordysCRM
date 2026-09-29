package cn.cordys.crm.ai.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.domain.AiModelPrice;
import cn.cordys.crm.ai.domain.AiQuotaOverride;
import cn.cordys.crm.ai.domain.AiQuotaUsage;
import cn.cordys.crm.ai.domain.AiUsageRecord;
import cn.cordys.crm.ai.dto.request.AdminAiCostRequest;
import cn.cordys.crm.ai.dto.request.AiModelPriceSaveRequest;
import cn.cordys.crm.ai.dto.request.AiQuotaConfigRequest;
import cn.cordys.crm.ai.dto.response.AdminAiCostResponse;
import cn.cordys.crm.ai.dto.response.AdminAiCostTenantRow;
import cn.cordys.crm.ai.dto.response.AiCostTrendPoint;
import cn.cordys.crm.ai.dto.response.AiFeatureUsagePoint;
import cn.cordys.crm.ai.dto.response.AiModelPriceResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaConfigResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiQuotaTrendPoint;
import cn.cordys.crm.ai.dto.response.AiTenantQuotaRow;
import cn.cordys.crm.ai.dto.response.TenantQuotaOverviewResponse;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.domain.SysFeature;
import cn.cordys.crm.system.domain.TenantEdition;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.cordys.crm.ai.constant.AiQuotaConstant.GROUP_BY_DAY;
import static cn.cordys.crm.ai.constant.AiQuotaConstant.GROUP_BY_MONTH;
import static cn.cordys.crm.ai.constant.AiQuotaConstant.STATUS_CIRCUIT_BROKEN;
import static cn.cordys.crm.ai.constant.AiQuotaConstant.STATUS_HARD_LIMITED;
import static cn.cordys.crm.ai.constant.AiQuotaConstant.STATUS_NORMAL;
import static cn.cordys.crm.ai.constant.AiQuotaConstant.STATUS_RATE_LIMITED;
import static cn.cordys.crm.ai.constant.AiQuotaConstant.STATUS_SOFT_LIMITED;

/**
 * AI 额度框架（G2）核心服务：计量记账 / 月配额 / 软硬超 / 限流熔断 / 双看板。
 * <p>
 * 联网后由模型层在「调用前 {@link #checkQuota}、调用后 {@link #record}」接入；
 * 现阶段通过 admin 的 mock-record 接口验证全链路。
 * </p>
 */
@Service
@Slf4j
public class AiQuotaService {

    private static final String KEY_RATE_MINUTE = "ai:rl:minute:";
    private static final String KEY_RATE_DAILY = "ai:rl:daily:";
    private static final String KEY_COST_DAILY = "ai:cost:daily:";
    private static final String KEY_CIRCUIT = "ai:cb:";

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_BUCKET_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MINUTE_BUCKET_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    @Resource
    private BaseMapper<AiUsageRecord> usageRecordMapper;
    @Resource
    private BaseMapper<AiQuotaUsage> quotaUsageMapper;
    @Resource
    private BaseMapper<AiQuotaOverride> overrideMapper;
    @Resource
    private BaseMapper<AiModelPrice> modelPriceMapper;
    @Resource
    private BaseMapper<Parameter> parameterMapper;
    @Resource
    private BaseMapper<SysFeature> featureMapper;
    @Resource
    private BaseMapper<Organization> organizationMapper;
    @Resource
    private EditionService editionService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ==================== 计量入口（联网后模型层调用） ====================

    /**
     * 调用前校验：不记账，只返回当前额度状态。模型层据此决定是否放行（HARD/RATE/CIRCUIT 应拦截）。
     */
    public AiQuotaRecordResult checkQuota(String organizationId) {
        AiQuotaRecordResult result = new AiQuotaRecordResult();
        result.setQuota(getMonthlyQuota(organizationId));
        result.setUsedCalls(getUsedCalls(organizationId));
        String status;
        if (isCircuitBroken(organizationId)) {
            status = STATUS_CIRCUIT_BROKEN;
        } else if (readRateExceeded(organizationId)) {
            status = STATUS_RATE_LIMITED;
        } else {
            status = resolveStatus(organizationId, BigDecimal.ZERO);
        }
        result.setStatus(status);
        return result;
    }

    /**
     * 调用后记账：限流/熔断/软硬超判定 + 落明细 + 累月计数 + 单日成本熔断。
     * 硬超（&gt;软超阈值）时拒绝记账、不再累加。
     */
    public AiQuotaRecordResult record(String organizationId, String featureCode, String modelCode,
                                      long inputTokens, long outputTokens) {
        AiQuotaRecordResult result = new AiQuotaRecordResult();
        result.setQuota(getMonthlyQuota(organizationId));

        if (isCircuitBroken(organizationId)) {
            result.setStatus(STATUS_CIRCUIT_BROKEN);
            return result;
        }
        if (consumeRateLimit(organizationId)) {
            result.setStatus(STATUS_RATE_LIMITED);
            return result;
        }

        BigDecimal costCalls = calcCostCalls(inputTokens, outputTokens);
        result.setCostCalls(costCalls);
        String status = resolveStatus(organizationId, costCalls);
        if (STATUS_HARD_LIMITED.equals(status)) {
            result.setStatus(status);
            return result;
        }

        persistUsage(organizationId, featureCode, modelCode, inputTokens, outputTokens, costCalls, status);
        checkCostCircuitBreak(organizationId, modelCode, inputTokens, outputTokens);

        result.setStatus(status);
        result.setUsedCalls(getUsedCalls(organizationId));
        return result;
    }

    // ==================== 租户端看板 ====================

    /**
     * 租户本月额度总览（次数视角，不含金额）
     */
    public TenantQuotaOverviewResponse getTenantOverview(String organizationId) {
        TenantQuotaOverviewResponse response = new TenantQuotaOverviewResponse();
        response.setPeriod(currentPeriod());
        int quota = getMonthlyQuota(organizationId);
        BigDecimal used = getUsedCalls(organizationId);
        BigDecimal remaining = BigDecimal.valueOf(quota).subtract(used);
        if (remaining.signum() < 0) {
            remaining = BigDecimal.ZERO;
        }
        BigDecimal percent = quota > 0
                ? used.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(quota), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        response.setQuota(quota);
        response.setUsedCalls(used);
        response.setRemainingCalls(remaining);
        response.setUsedPercent(percent);
        response.setSoftLimitPercent(getSoftLimitPercent());
        response.setTodayCalls(getTodayCalls(organizationId));
        response.setStatus(resolveStatus(organizationId, BigDecimal.ZERO));
        return response;
    }

    /**
     * 租户用量趋势（按日/月分桶）
     */
    public List<AiQuotaTrendPoint> getTenantTrend(String organizationId, String groupBy) {
        String gb = StringUtils.isBlank(groupBy) ? GROUP_BY_DAY : groupBy;
        List<AiUsageRecord> records = usageRecordMapper.selectListByLambda(new LambdaQueryWrapper<AiUsageRecord>()
                .eq(AiUsageRecord::getOrganizationId, organizationId));
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (AiUsageRecord r : records) {
            map.merge(bucket(r.getCreateTime(), gb), nvl(r.getCostCalls()), BigDecimal::add);
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    AiQuotaTrendPoint p = new AiQuotaTrendPoint();
                    p.setBucket(e.getKey());
                    p.setUsedCalls(e.getValue());
                    return p;
                })
                .toList();
    }

    /**
     * 租户分功能用量（按 feature_code 聚合）
     */
    public List<AiFeatureUsagePoint> getFeatureUsage(String organizationId) {
        List<AiUsageRecord> records = usageRecordMapper.selectListByLambda(new LambdaQueryWrapper<AiUsageRecord>()
                .eq(AiUsageRecord::getOrganizationId, organizationId));
        Map<String, BigDecimal> usedMap = new HashMap<>();
        for (AiUsageRecord r : records) {
            usedMap.merge(r.getFeatureCode(), nvl(r.getCostCalls()), BigDecimal::add);
        }
        Map<String, String> nameMap = buildFeatureNameMap();
        return usedMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    AiFeatureUsagePoint p = new AiFeatureUsagePoint();
                    p.setFeatureCode(e.getKey());
                    p.setFeatureName(nameMap.getOrDefault(e.getKey(), e.getKey()));
                    p.setUsedCalls(e.getValue());
                    return p;
                })
                .toList();
    }

    // ==================== 平台端看板 ====================

    /**
     * 平台成本看板：token × 模型单价折算成本（估算），按租户 + 按桶聚合。
     */
    public AdminAiCostResponse getAdminCostOverview(AdminAiCostRequest request) {
        String gb = StringUtils.isBlank(request.getGroupBy()) ? GROUP_BY_MONTH : request.getGroupBy();
        long[] range = resolveRange(request.getStartTime(), request.getEndTime());
        List<AiUsageRecord> records = usageRecordMapper.selectListByLambda(new LambdaQueryWrapper<AiUsageRecord>()
                .between(AiUsageRecord::getCreateTime, range[0], range[1]));
        Map<String, AiModelPrice> priceMap = buildModelPriceMap();

        AdminAiCostResponse response = new AdminAiCostResponse();
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalTokens = 0;
        BigDecimal totalCalls = BigDecimal.ZERO;
        Map<String, AdminAiCostTenantRow> tenantMap = new HashMap<>();
        Map<String, BigDecimal> seriesMap = new LinkedHashMap<>();

        for (AiUsageRecord r : records) {
            BigDecimal cost = calcCost(priceMap.get(r.getModelCode()), nvl(r.getInputTokens()), nvl(r.getOutputTokens()));
            totalCost = totalCost.add(cost);
            totalTokens += nvl(r.getInputTokens()) + nvl(r.getOutputTokens());
            totalCalls = totalCalls.add(nvl(r.getCostCalls()));

            AdminAiCostTenantRow row = tenantMap.computeIfAbsent(r.getOrganizationId(), k -> {
                AdminAiCostTenantRow nr = new AdminAiCostTenantRow();
                nr.setOrganizationId(k);
                nr.setCostCalls(BigDecimal.ZERO);
                nr.setInputTokens(0L);
                nr.setOutputTokens(0L);
                nr.setCost(BigDecimal.ZERO);
                return nr;
            });
            row.setCostCalls(row.getCostCalls().add(nvl(r.getCostCalls())));
            row.setInputTokens(row.getInputTokens() + nvl(r.getInputTokens()));
            row.setOutputTokens(row.getOutputTokens() + nvl(r.getOutputTokens()));
            row.setCost(row.getCost().add(cost));

            seriesMap.merge(bucket(r.getCreateTime(), gb), cost, BigDecimal::add);
        }

        for (AdminAiCostTenantRow row : tenantMap.values()) {
            Organization org = organizationMapper.selectByPrimaryKey(row.getOrganizationId());
            row.setOrganizationName(org != null ? org.getName() : row.getOrganizationId());
        }
        List<AdminAiCostTenantRow> tenantRows = tenantMap.values().stream()
                .sorted(Comparator.comparing(AdminAiCostTenantRow::getCost).reversed())
                .toList();
        List<AiCostTrendPoint> series = seriesMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    AiCostTrendPoint p = new AiCostTrendPoint();
                    p.setBucket(e.getKey());
                    p.setCost(e.getValue());
                    return p;
                })
                .toList();

        response.setTotalCost(totalCost);
        response.setTotalTokens(totalTokens);
        response.setTotalCalls(totalCalls);
        response.setTenantRows(tenantRows);
        response.setSeries(series);
        return response;
    }

    // ==================== 模型单价（admin 维护） ====================

    public List<AiModelPriceResponse> listModelPrices() {
        return modelPriceMapper.selectAll(null).stream()
                .map(p -> BeanUtils.copyBean(new AiModelPriceResponse(), p))
                .toList();
    }

    public void saveModelPrice(AiModelPriceSaveRequest request, String operatorId) {
        long now = System.currentTimeMillis();
        if (StringUtils.isBlank(request.getId())) {
            ensureModelCodeUnique(request.getModelCode(), null);
            AiModelPrice price = new AiModelPrice();
            price.setId(IDGenerator.nextStr());
            applyModelPrice(price, request);
            price.setCreateTime(now);
            price.setCreateUser(operatorId);
            price.setUpdateTime(now);
            price.setUpdateUser(operatorId);
            modelPriceMapper.insert(price);
        } else {
            AiModelPrice price = modelPriceMapper.selectByPrimaryKey(request.getId());
            if (price == null) {
                throw new GenericException(Translator.get("ai.model_price.not_found"));
            }
            ensureModelCodeUnique(request.getModelCode(), price.getId());
            applyModelPrice(price, request);
            price.setUpdateTime(now);
            price.setUpdateUser(operatorId);
            modelPriceMapper.updateById(price);
        }
    }

    // ==================== 全局配置（admin 维护） ====================

    public AiQuotaConfigResponse getConfig() {
        AiQuotaConfigResponse response = new AiQuotaConfigResponse();
        response.setTokensPerCall(getTokensPerCall());
        response.setSoftLimitPercent(getSoftLimitPercent());
        response.setTrialQuota(getTrialQuota());
        response.setMinuteCallLimit(getMinuteCallLimit());
        response.setDailyCallLimit(getDailyCallLimit());
        response.setDailyCostThreshold(getDailyCostThreshold());
        return response;
    }

    public void updateConfig(AiQuotaConfigRequest request) {
        if (request.getTokensPerCall() != null) {
            setParam(AiQuotaConstant.PARAM_TOKENS_PER_CALL, String.valueOf(request.getTokensPerCall()));
        }
        if (request.getSoftLimitPercent() != null) {
            setParam(AiQuotaConstant.PARAM_SOFT_LIMIT_PERCENT, String.valueOf(request.getSoftLimitPercent()));
        }
        if (request.getTrialQuota() != null) {
            setParam(AiQuotaConstant.PARAM_TRIAL_QUOTA, String.valueOf(request.getTrialQuota()));
        }
        if (request.getMinuteCallLimit() != null) {
            setParam(AiQuotaConstant.PARAM_MINUTE_CALL_LIMIT, String.valueOf(request.getMinuteCallLimit()));
        }
        if (request.getDailyCallLimit() != null) {
            setParam(AiQuotaConstant.PARAM_DAILY_CALL_LIMIT, String.valueOf(request.getDailyCallLimit()));
        }
        if (request.getDailyCostThreshold() != null) {
            setParam(AiQuotaConstant.PARAM_DAILY_COST_THRESHOLD, String.valueOf(request.getDailyCostThreshold()));
        }
    }

    // ==================== 配额/配置读取 ====================

    /**
     * 租户本月配额：优先手动覆盖，其次 tenant_edition 快照，最后回退 ai.quota.trialQuota。
     * 覆盖值 0 表示停用 AI，同样生效。
     */
    public int getMonthlyQuota(String organizationId) {
        AiQuotaOverride override = getOverride(organizationId);
        if (override != null) {
            return override.getQuota();
        }
        TenantEdition edition = editionService.getByOrganizationId(organizationId);
        if (edition != null && edition.getAiMonthlyQuota() != null && edition.getAiMonthlyQuota() > 0) {
            return edition.getAiMonthlyQuota();
        }
        return getTrialQuota();
    }

    // ==================== 租户额度覆盖（admin 按租户调额） ====================

    /**
     * 列出所有租户（排除平台组织）的当前 AI 额度情况，供 admin 按租户调额。
     */
    public List<AiTenantQuotaRow> listTenantQuotas(String keyword) {
        Map<String, Integer> overrideMap = new HashMap<>();
        for (AiQuotaOverride o : overrideMapper.selectAll(null)) {
            overrideMap.put(o.getOrganizationId(), o.getQuota());
        }
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        return DataAccessLayer.with(Organization.class)
                .selectListByLambda(new LambdaQueryWrapper<Organization>()
                        .orderByDesc(Organization::getCreateTime)).stream()
                .filter(o -> !OrganizationContext.DEFAULT_ORGANIZATION_ID.equals(o.getId()))
                .filter(o -> kw == null
                        || (o.getName() != null && o.getName().contains(kw))
                        || (o.getId() != null && o.getId().contains(kw)))
                .map(o -> {
                    AiTenantQuotaRow row = new AiTenantQuotaRow();
                    row.setOrganizationId(o.getId());
                    row.setOrganizationName(o.getName());
                    row.setOrgType(o.getOrgType());
                    Integer override = overrideMap.get(o.getId());
                    row.setOverrideQuota(override);
                    TenantEdition edition = editionService.getByOrganizationId(o.getId());
                    Integer snapshot = null;
                    if (edition != null) {
                        snapshot = edition.getAiMonthlyQuota();
                        row.setEditionCode(edition.getEditionCode());
                        row.setEditionName(edition.getEditionName());
                        row.setExpireTime(edition.getExpireTime());
                    }
                    row.setSnapshotQuota(snapshot);
                    int effective;
                    if (override != null) {
                        effective = override;
                    } else if (snapshot != null && snapshot > 0) {
                        effective = snapshot;
                    } else {
                        effective = getTrialQuota();
                    }
                    row.setEffectiveQuota(effective);
                    BigDecimal used = getUsedCalls(o.getId());
                    row.setUsedCalls(used);
                    BigDecimal remaining = BigDecimal.valueOf(effective).subtract(used);
                    row.setRemainingCalls(remaining.signum() < 0 ? BigDecimal.ZERO : remaining);
                    BigDecimal quota = BigDecimal.valueOf(effective);
                    BigDecimal softLimit = quota.multiply(BigDecimal.valueOf(getSoftLimitPercent()))
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    row.setStatus(used.compareTo(softLimit) > 0 ? STATUS_HARD_LIMITED
                            : (used.compareTo(quota) > 0 ? STATUS_SOFT_LIMITED : STATUS_NORMAL));
                    return row;
                })
                .toList();
    }

    /**
     * 保存覆盖（有则更新，无则新增），quota=0 表示停用 AI。
     */
    public void saveOverride(String organizationId, int quota, String operatorId) {
        long now = System.currentTimeMillis();
        AiQuotaOverride override = getOverride(organizationId);
        if (override == null) {
            override = new AiQuotaOverride();
            override.setId(IDGenerator.nextStr());
            override.setOrganizationId(organizationId);
            override.setQuota(quota);
            override.setCreateTime(now);
            override.setCreateUser(operatorId);
            override.setUpdateTime(now);
            override.setUpdateUser(operatorId);
            overrideMapper.insert(override);
        } else {
            override.setQuota(quota);
            override.setUpdateTime(now);
            override.setUpdateUser(operatorId);
            overrideMapper.updateById(override);
        }
    }

    /**
     * 删除覆盖，恢复默认（套餐快照/试用配额）。
     */
    public void resetOverride(String organizationId) {
        AiQuotaOverride override = getOverride(organizationId);
        if (override != null) {
            overrideMapper.deleteByPrimaryKey(override.getId());
        }
    }

    private AiQuotaOverride getOverride(String organizationId) {
        List<AiQuotaOverride> list = overrideMapper.selectListByLambda(new LambdaQueryWrapper<AiQuotaOverride>()
                .eq(AiQuotaOverride::getOrganizationId, organizationId));
        return list.isEmpty() ? null : list.getFirst();
    }

    public long getTokensPerCall() {
        return getLongParam(AiQuotaConstant.PARAM_TOKENS_PER_CALL, AiQuotaConstant.DEFAULT_TOKENS_PER_CALL);
    }

    public int getSoftLimitPercent() {
        return getIntParam(AiQuotaConstant.PARAM_SOFT_LIMIT_PERCENT, AiQuotaConstant.DEFAULT_SOFT_LIMIT_PERCENT);
    }

    public int getTrialQuota() {
        return getIntParam(AiQuotaConstant.PARAM_TRIAL_QUOTA, AiQuotaConstant.DEFAULT_TRIAL_QUOTA);
    }

    public int getMinuteCallLimit() {
        return getIntParam(AiQuotaConstant.PARAM_MINUTE_CALL_LIMIT, AiQuotaConstant.DEFAULT_MINUTE_CALL_LIMIT);
    }

    public int getDailyCallLimit() {
        return getIntParam(AiQuotaConstant.PARAM_DAILY_CALL_LIMIT, AiQuotaConstant.DEFAULT_DAILY_CALL_LIMIT);
    }

    public double getDailyCostThreshold() {
        return getDoubleParam(AiQuotaConstant.PARAM_DAILY_COST_THRESHOLD, AiQuotaConstant.DEFAULT_DAILY_COST_THRESHOLD);
    }

    // ==================== 私有：额度判定 ====================

    /**
     * 根据「已用 + 本次增量」判定额度状态。
     * 软超（100%–softLimitPercent%）：SOFT_LIMITED；硬超（&gt;softLimitPercent%）：HARD_LIMITED。
     */
    private String resolveStatus(String organizationId, BigDecimal additionalCalls) {
        BigDecimal used = getUsedCalls(organizationId);
        BigDecimal projected = used.add(additionalCalls);
        BigDecimal quota = BigDecimal.valueOf(getMonthlyQuota(organizationId));
        BigDecimal softLimit = quota.multiply(BigDecimal.valueOf(getSoftLimitPercent()))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (projected.compareTo(softLimit) > 0) {
            return STATUS_HARD_LIMITED;
        }
        if (projected.compareTo(quota) > 0) {
            return STATUS_SOFT_LIMITED;
        }
        return STATUS_NORMAL;
    }

    private BigDecimal calcCostCalls(long inputTokens, long outputTokens) {
        long total = inputTokens + outputTokens;
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(getTokensPerCall()), 4, RoundingMode.HALF_UP);
    }

    private void persistUsage(String organizationId, String featureCode, String modelCode,
                              long inputTokens, long outputTokens, BigDecimal costCalls, String status) {
        long now = System.currentTimeMillis();
        AiUsageRecord record = new AiUsageRecord();
        record.setId(IDGenerator.nextStr());
        record.setOrganizationId(organizationId);
        record.setFeatureCode(featureCode);
        record.setModelCode(modelCode);
        record.setInputTokens(inputTokens);
        record.setOutputTokens(outputTokens);
        record.setTotalTokens(inputTokens + outputTokens);
        record.setCostCalls(costCalls);
        record.setStatus(status);
        record.setCreateTime(now);
        usageRecordMapper.insert(record);
        upsertMonthlyUsage(organizationId, costCalls, now);
    }

    private void upsertMonthlyUsage(String organizationId, BigDecimal costCalls, long now) {
        String period = currentPeriod();
        List<AiQuotaUsage> list = quotaUsageMapper.selectListByLambda(new LambdaQueryWrapper<AiQuotaUsage>()
                .eq(AiQuotaUsage::getOrganizationId, organizationId)
                .eq(AiQuotaUsage::getPeriod, period));
        if (list.isEmpty()) {
            AiQuotaUsage usage = new AiQuotaUsage();
            usage.setId(IDGenerator.nextStr());
            usage.setOrganizationId(organizationId);
            usage.setPeriod(period);
            usage.setUsedCalls(costCalls);
            usage.setCreateTime(now);
            usage.setUpdateTime(now);
            quotaUsageMapper.insert(usage);
        } else {
            AiQuotaUsage usage = list.getFirst();
            usage.setUsedCalls(usage.getUsedCalls().add(costCalls));
            usage.setUpdateTime(now);
            quotaUsageMapper.updateById(usage);
        }
    }

    public BigDecimal getUsedCalls(String organizationId) {
        List<AiQuotaUsage> list = quotaUsageMapper.selectListByLambda(new LambdaQueryWrapper<AiQuotaUsage>()
                .eq(AiQuotaUsage::getOrganizationId, organizationId)
                .eq(AiQuotaUsage::getPeriod, currentPeriod()));
        return list.isEmpty() ? BigDecimal.ZERO : list.getFirst().getUsedCalls();
    }

    private BigDecimal getTodayCalls(String organizationId) {
        long start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = System.currentTimeMillis();
        List<AiUsageRecord> records = usageRecordMapper.selectListByLambda(new LambdaQueryWrapper<AiUsageRecord>()
                .eq(AiUsageRecord::getOrganizationId, organizationId)
                .between(AiUsageRecord::getCreateTime, start, end));
        return records.stream().map(AiUsageRecord::getCostCalls).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== 私有：限流熔断 ====================

    private boolean isCircuitBroken(String organizationId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_CIRCUIT + organizationId));
    }

    private boolean readRateExceeded(String organizationId) {
        long minute = readCounter(KEY_RATE_MINUTE + organizationId + ":" + MINUTE_BUCKET_FMT.format(LocalDateTime.now()));
        long daily = readCounter(KEY_RATE_DAILY + organizationId + ":" + DAY_BUCKET_FMT.format(LocalDate.now()));
        return minute >= getMinuteCallLimit() || daily >= getDailyCallLimit();
    }

    private boolean consumeRateLimit(String organizationId) {
        long minute = incrementCounter(KEY_RATE_MINUTE + organizationId + ":" + MINUTE_BUCKET_FMT.format(LocalDateTime.now()), 60);
        long daily = incrementCounter(KEY_RATE_DAILY + organizationId + ":" + DAY_BUCKET_FMT.format(LocalDate.now()), 24 * 60 * 60);
        return minute > getMinuteCallLimit() || daily > getDailyCallLimit();
    }

    private long readCounter(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long incrementCounter(String key, long ttlSeconds) {
        stringRedisTemplate.opsForValue().setIfAbsent(key, "0", Duration.ofSeconds(ttlSeconds));
        Long value = stringRedisTemplate.opsForValue().increment(key);
        return value == null ? 0 : value;
    }

    private void checkCostCircuitBreak(String organizationId, String modelCode, long inputTokens, long outputTokens) {
        double threshold = getDailyCostThreshold();
        if (threshold <= 0) {
            return;
        }
        BigDecimal cost = calcCost(getModelPrice(modelCode), inputTokens, outputTokens);
        if (cost.signum() <= 0) {
            return;
        }
        String dayKey = KEY_COST_DAILY + organizationId + ":" + DAY_BUCKET_FMT.format(LocalDate.now());
        stringRedisTemplate.opsForValue().setIfAbsent(dayKey, "0", Duration.ofSeconds(secondsUntilNextMidnight()));
        Double total = stringRedisTemplate.opsForValue().increment(dayKey, cost.doubleValue());
        if (total != null && total >= threshold) {
            boolean firstBreak = !isCircuitBroken(organizationId);
            stringRedisTemplate.opsForValue().set(KEY_CIRCUIT + organizationId, "1",
                    Duration.ofSeconds(secondsUntilNextMidnight()));
            if (firstBreak) {
                notifyCircuitBreak(organizationId);
            }
        }
    }

    private void notifyCircuitBreak(String organizationId) {
        try {
            Organization organization = organizationMapper.selectByPrimaryKey(organizationId);
            String name = organization != null ? organization.getName() : organizationId;
            commonNoticeSendService.sendNotice(
                    NotificationConstants.Module.SYSTEM,
                    NotificationConstants.Event.AI_QUOTA_BREAK,
                    Map.of("name", name),
                    InternalUser.ADMIN.getValue(),
                    OrganizationContext.DEFAULT_ORGANIZATION_ID,
                    List.of(InternalUser.ADMIN.getValue()),
                    false);
        } catch (Exception e) {
            log.warn("AI 额度熔断告警发送失败: {}", e.getMessage());
        }
    }

    // ==================== 私有：成本折算 ====================

    private AiModelPrice getModelPrice(String modelCode) {
        List<AiModelPrice> list = modelPriceMapper.selectListByLambda(new LambdaQueryWrapper<AiModelPrice>()
                .eq(AiModelPrice::getModelCode, modelCode));
        return list.isEmpty() ? null : list.getFirst();
    }

    private Map<String, AiModelPrice> buildModelPriceMap() {
        Map<String, AiModelPrice> map = new HashMap<>();
        for (AiModelPrice p : modelPriceMapper.selectAll(null)) {
            map.put(p.getModelCode(), p);
        }
        return map;
    }

    private BigDecimal calcCost(AiModelPrice price, long inputTokens, long outputTokens) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(nvl(price.getInputPrice()));
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(nvl(price.getOutputPrice()));
        return inputCost.add(outputCost);
    }

    private void applyModelPrice(AiModelPrice price, AiModelPriceSaveRequest request) {
        price.setModelCode(request.getModelCode());
        price.setModelName(request.getModelName());
        price.setInputPrice(request.getInputPrice());
        price.setOutputPrice(request.getOutputPrice());
        price.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private void ensureModelCodeUnique(String modelCode, String excludeId) {
        if (StringUtils.isBlank(modelCode)) {
            return;
        }
        List<AiModelPrice> exist = modelPriceMapper.selectListByLambda(new LambdaQueryWrapper<AiModelPrice>()
                .eq(AiModelPrice::getModelCode, modelCode));
        boolean duplicated = exist.stream().anyMatch(p -> !p.getId().equals(excludeId));
        if (duplicated) {
            throw new GenericException(Translator.get("ai.model_price.code_exists"));
        }
    }

    private Map<String, String> buildFeatureNameMap() {
        Map<String, String> map = new HashMap<>();
        for (SysFeature f : featureMapper.selectAll(null)) {
            map.put(f.getFeatureCode(), f.getName());
        }
        return map;
    }

    // ==================== 私有：时间/参数 ====================

    private String currentPeriod() {
        return PERIOD_FMT.format(LocalDate.now());
    }

    private long[] resolveRange(Long start, Long end) {
        if (start != null && end != null) {
            return new long[]{start, end};
        }
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        LocalDate last = first.plusMonths(1).minusDays(1);
        long s = first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long e = last.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
        return new long[]{s, e};
    }

    private String bucket(Long time, String groupBy) {
        if (time == null) {
            return "-";
        }
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        if (GROUP_BY_DAY.equals(groupBy)) {
            return String.format("%04d-%02d-%02d", dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
        }
        return String.format("%04d-%02d", dt.getYear(), dt.getMonthValue());
    }

    private long secondsUntilNextMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, nextMidnight).getSeconds() + 1;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long nvl(Long value) {
        return value == null ? 0 : value;
    }

    // ==================== 私有：sys_parameter ====================

    private String getParam(String key) {
        Parameter parameter = parameterMapper.selectByPrimaryKey(key);
        return parameter == null ? null : parameter.getParamValue();
    }

    private void setParam(String key, String value) {
        parameterMapper.deleteByPrimaryKey(key);
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        parameter.setType("text");
        parameterMapper.insert(parameter);
    }

    private long getLongParam(String key, long defaultValue) {
        String value = getParam(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getIntParam(String key, int defaultValue) {
        String value = getParam(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double getDoubleParam(String key, double defaultValue) {
        String value = getParam(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
