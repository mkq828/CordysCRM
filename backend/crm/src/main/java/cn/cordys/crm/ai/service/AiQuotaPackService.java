package cn.cordys.crm.ai.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.domain.AiQuotaPack;
import cn.cordys.crm.ai.domain.AiQuotaPackBalance;
import cn.cordys.crm.ai.domain.AiQuotaPackOrder;
import cn.cordys.crm.ai.dto.response.AiQuotaPackBalanceResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaPackOrderResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaPackResponse;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.SysFeature;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 额度包（G2 扩展）：额度包商品 + 购买/充值单 + 已购余额。
 * <p>
 * 扣减规则：月配额（套餐内通用池）先用，用完后从该功能的已购包扣（specific 功能包优先，其次通用包）。
 * 第一版不走在线支付，租户申请 → 客服/admin 确认到账。
 * </p>
 */
@Service
@Slf4j
public class AiQuotaPackService {

    @Resource
    private BaseMapper<AiQuotaPack> packMapper;
    @Resource
    private BaseMapper<AiQuotaPackOrder> orderMapper;
    @Resource
    private BaseMapper<AiQuotaPackBalance> balanceMapper;
    @Resource
    private BaseMapper<Organization> organizationMapper;
    @Resource
    private BaseMapper<SysFeature> featureMapper;

    // ==================== 额度包商品（admin 维护） ====================

    public List<AiQuotaPack> listPacks(boolean enabledOnly) {
        LambdaQueryWrapper<AiQuotaPack> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AiQuotaPack::getSort);
        wrapper.orderByAsc(AiQuotaPack::getCreateTime);
        if (enabledOnly) {
            wrapper.eq(AiQuotaPack::getStatus, 1);
        }
        return packMapper.selectListByLambda(wrapper);
    }

    public List<AiQuotaPack> listEnabledPacks() {
        return listPacks(true);
    }

    public void savePack(AiQuotaPack pack, String operatorId) {
        long now = System.currentTimeMillis();
        if (StringUtils.isBlank(pack.getId())) {
            pack.setId(IDGenerator.nextStr());
            pack.setStatus(pack.getStatus() == null ? 1 : pack.getStatus());
            pack.setSort(pack.getSort() == null ? 0 : pack.getSort());
            pack.setCreateTime(now);
            pack.setCreateUser(operatorId);
            pack.setUpdateTime(now);
            pack.setUpdateUser(operatorId);
            packMapper.insert(pack);
        } else {
            AiQuotaPack existing = packMapper.selectByPrimaryKey(pack.getId());
            if (existing == null) {
                throw new GenericException("额度包不存在");
            }
            existing.setFeatureCode(pack.getFeatureCode());
            existing.setName(pack.getName());
            existing.setCalls(pack.getCalls());
            existing.setPriceFen(pack.getPriceFen());
            existing.setStatus(pack.getStatus());
            existing.setSort(pack.getSort());
            existing.setRemark(pack.getRemark());
            existing.setUpdateTime(now);
            existing.setUpdateUser(operatorId);
            packMapper.updateById(existing);
        }
    }

    public void deletePack(String id) {
        packMapper.deleteByPrimaryKey(id);
    }

    // ==================== 已购余额（租户视角 + 扣减集成） ====================

    public List<AiQuotaPackBalance> listBalances(String organizationId) {
        return balanceMapper.selectListByLambda(new LambdaQueryWrapper<AiQuotaPackBalance>()
                .eq(AiQuotaPackBalance::getOrganizationId, organizationId));
    }

    /**
     * 租户某功能的已购包剩余：featureCode 为空 = 全功能合计；具体功能 = 功能包 + 通用包。
     * 供 AiQuotaService 判定有效额度用。
     */
    public BigDecimal getPackBalance(String organizationId, String featureCode) {
        List<AiQuotaPackBalance> balances = listBalances(organizationId);
        if (StringUtils.isBlank(featureCode)) {
            return balances.stream()
                    .map(AiQuotaPackBalance::getRemainingCalls)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal specific = balances.stream()
                .filter(b -> featureCode.equals(b.getFeatureCode()))
                .map(AiQuotaPackBalance::getRemainingCalls)
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(BigDecimal.ZERO);
        BigDecimal general = balances.stream()
                .filter(b -> AiQuotaConstant.PACK_FEATURE_ALL.equals(b.getFeatureCode()))
                .map(AiQuotaPackBalance::getRemainingCalls)
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(BigDecimal.ZERO);
        return specific.add(general);
    }

    /**
     * 扣减已购包剩余：specific 功能包优先，不足再扣通用包。仅当 amount &gt; 0 时生效。
     */
    public void consumePack(String organizationId, String featureCode, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        if (StringUtils.isBlank(featureCode)) {
            decrementBalance(organizationId, AiQuotaConstant.PACK_FEATURE_ALL, amount);
            return;
        }
        BigDecimal remaining = decrementBalance(organizationId, featureCode, amount);
        if (remaining.signum() > 0 && remaining.compareTo(amount) < 0) {
            // 功能包不足，剩余从通用包扣
            decrementBalance(organizationId, AiQuotaConstant.PACK_FEATURE_ALL, amount.subtract(remaining));
        }
    }

    /** 扣减某条余额，返回扣减后剩余（未扣到负数，不足时清零）。 */
    private BigDecimal decrementBalance(String organizationId, String featureCode, BigDecimal amount) {
        AiQuotaPackBalance balance = findBalance(organizationId, featureCode);
        if (balance == null || balance.getRemainingCalls() == null
                || balance.getRemainingCalls().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal newRemaining = balance.getRemainingCalls().subtract(amount).max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
        balance.setRemainingCalls(newRemaining);
        balance.setUpdateTime(System.currentTimeMillis());
        balanceMapper.updateById(balance);
        return newRemaining;
    }

    // ==================== 购买/充值单 ====================

    /** 租户申请购买额度包（下单即 PENDING，不直接到账）。 */
    public void applyOrder(String organizationId, String packId, String userId) {
        AiQuotaPack pack = packMapper.selectByPrimaryKey(packId);
        if (pack == null || !Integer.valueOf(1).equals(pack.getStatus())) {
            throw new GenericException("额度包不存在或已下架");
        }
        long now = System.currentTimeMillis();
        AiQuotaPackOrder order = new AiQuotaPackOrder();
        order.setId(IDGenerator.nextStr());
        order.setOrganizationId(organizationId);
        order.setPackId(pack.getId());
        order.setFeatureCode(pack.getFeatureCode());
        order.setPackName(pack.getName());
        order.setCalls(pack.getCalls());
        order.setAmountFen(pack.getPriceFen());
        order.setStatus(AiQuotaConstant.PACK_ORDER_PENDING);
        order.setApplyUser(userId);
        order.setApplyTime(now);
        order.setCreateTime(now);
        order.setCreateUser(userId);
        order.setUpdateTime(now);
        order.setUpdateUser(userId);
        orderMapper.insert(order);
    }

    public List<AiQuotaPackOrder> listOrders(String keyword, String status) {
        LambdaQueryWrapper<AiQuotaPackOrder> wrapper = new LambdaQueryWrapper<AiQuotaPackOrder>()
                .orderByDesc(AiQuotaPackOrder::getCreateTime);
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(AiQuotaPackOrder::getStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.or(w -> w.like(AiQuotaPackOrder::getPackName, kw)
                    .like(AiQuotaPackOrder::getOrganizationId, kw));
        }
        return orderMapper.selectListByLambda(wrapper);
    }

    /** admin 确认到账：订单置 CONFIRMED，余额 +calls。 */
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(String orderId, String operatorId) {
        AiQuotaPackOrder order = requireOrder(orderId);
        if (!AiQuotaConstant.PACK_ORDER_PENDING.equals(order.getStatus())) {
            throw new GenericException("订单已处理，请刷新后重试");
        }
        long now = System.currentTimeMillis();
        order.setStatus(AiQuotaConstant.PACK_ORDER_CONFIRMED);
        order.setConfirmUser(operatorId);
        order.setConfirmTime(now);
        order.setUpdateTime(now);
        order.setUpdateUser(operatorId);
        orderMapper.updateById(order);
        addBalance(order.getOrganizationId(), order.getFeatureCode(), BigDecimal.valueOf(order.getCalls()), operatorId);
    }

    /** admin 取消：订单置 CANCELLED，不动余额。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderId, String operatorId, String reason) {
        AiQuotaPackOrder order = requireOrder(orderId);
        if (!AiQuotaConstant.PACK_ORDER_PENDING.equals(order.getStatus())) {
            throw new GenericException("订单已处理，请刷新后重试");
        }
        order.setStatus(AiQuotaConstant.PACK_ORDER_CANCELLED);
        order.setConfirmUser(operatorId);
        order.setConfirmTime(System.currentTimeMillis());
        order.setRemark(reason);
        order.setUpdateTime(System.currentTimeMillis());
        order.setUpdateUser(operatorId);
        orderMapper.updateById(order);
    }

    private AiQuotaPackOrder requireOrder(String orderId) {
        AiQuotaPackOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new GenericException("订单不存在");
        }
        return order;
    }

    private void addBalance(String organizationId, String featureCode, BigDecimal calls, String operatorId) {
        String key = StringUtils.isBlank(featureCode) ? AiQuotaConstant.PACK_FEATURE_ALL : featureCode;
        AiQuotaPackBalance balance = findBalance(organizationId, key);
        long now = System.currentTimeMillis();
        if (balance == null) {
            balance = new AiQuotaPackBalance();
            balance.setId(IDGenerator.nextStr());
            balance.setOrganizationId(organizationId);
            balance.setFeatureCode(key);
            balance.setRemainingCalls(calls);
            balance.setCreateTime(now);
            balance.setCreateUser(operatorId);
            balance.setUpdateTime(now);
            balance.setUpdateUser(operatorId);
            balanceMapper.insert(balance);
        } else {
            balance.setRemainingCalls(balance.getRemainingCalls().add(calls));
            balance.setUpdateTime(now);
            balance.setUpdateUser(operatorId);
            balanceMapper.updateById(balance);
        }
    }

    private AiQuotaPackBalance findBalance(String organizationId, String featureCode) {
        List<AiQuotaPackBalance> list = balanceMapper.selectListByLambda(new LambdaQueryWrapper<AiQuotaPackBalance>()
                .eq(AiQuotaPackBalance::getOrganizationId, organizationId)
                .eq(AiQuotaPackBalance::getFeatureCode, featureCode));
        return list.isEmpty() ? null : list.getFirst();
    }

    // ==================== 租户订单 + 响应组装 ====================

    /** 租户本人/本组织的购买单列表。 */
    public List<AiQuotaPackOrder> listOrgOrders(String organizationId, String status) {
        LambdaQueryWrapper<AiQuotaPackOrder> wrapper = new LambdaQueryWrapper<AiQuotaPackOrder>()
                .eq(AiQuotaPackOrder::getOrganizationId, organizationId)
                .orderByDesc(AiQuotaPackOrder::getCreateTime);
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(AiQuotaPackOrder::getStatus, status);
        }
        return orderMapper.selectListByLambda(wrapper);
    }

    public List<AiQuotaPackResponse> listPackResponses(boolean enabledOnly) {
        return listPacks(enabledOnly).stream().map(this::toPackResponse).toList();
    }

    public List<AiQuotaPackBalanceResponse> listBalanceResponses(String organizationId) {
        Map<String, String> nameMap = buildFeatureNameMap();
        return listBalances(organizationId).stream().map(b -> {
            AiQuotaPackBalanceResponse r = new AiQuotaPackBalanceResponse();
            r.setFeatureCode(b.getFeatureCode());
            r.setFeatureName(featureName(nameMap, b.getFeatureCode()));
            r.setRemainingCalls(b.getRemainingCalls());
            return r;
        }).toList();
    }

    public List<AiQuotaPackOrderResponse> listOrderResponses(String keyword, String status) {
        Map<String, String> nameMap = buildFeatureNameMap();
        return listOrders(keyword, status).stream().map(o -> toOrderResponse(o, nameMap)).toList();
    }

    public List<AiQuotaPackOrderResponse> listOrgOrderResponses(String organizationId, String status) {
        Map<String, String> nameMap = buildFeatureNameMap();
        return listOrgOrders(organizationId, status).stream().map(o -> toOrderResponse(o, nameMap)).toList();
    }

    private AiQuotaPackOrderResponse toOrderResponse(AiQuotaPackOrder o, Map<String, String> nameMap) {
        AiQuotaPackOrderResponse r = new AiQuotaPackOrderResponse();
        r.setId(o.getId());
        r.setOrganizationId(o.getOrganizationId());
        r.setOrganizationName(resolveOrgName(o.getOrganizationId()));
        r.setPackName(o.getPackName());
        r.setFeatureCode(o.getFeatureCode());
        r.setFeatureName(featureName(nameMap, o.getFeatureCode()));
        r.setCalls(o.getCalls());
        r.setAmountFen(o.getAmountFen());
        r.setStatus(o.getStatus());
        r.setApplyTime(o.getApplyTime());
        r.setConfirmTime(o.getConfirmTime());
        r.setRemark(o.getRemark());
        r.setCreateTime(o.getCreateTime());
        return r;
    }

    private AiQuotaPackResponse toPackResponse(AiQuotaPack p) {
        AiQuotaPackResponse r = new AiQuotaPackResponse();
        r.setId(p.getId());
        r.setFeatureCode(p.getFeatureCode());
        r.setName(p.getName());
        r.setCalls(p.getCalls());
        r.setPriceFen(p.getPriceFen());
        r.setStatus(p.getStatus());
        r.setSort(p.getSort());
        r.setRemark(p.getRemark());
        r.setCreateTime(p.getCreateTime());
        return r;
    }

    private String resolveOrgName(String organizationId) {
        Organization org = organizationMapper.selectByPrimaryKey(organizationId);
        return org != null ? org.getName() : organizationId;
    }

    private Map<String, String> buildFeatureNameMap() {
        Map<String, String> map = new HashMap<>();
        for (SysFeature f : featureMapper.selectAll(null)) {
            map.put(f.getFeatureCode(), f.getName());
        }
        return map;
    }

    private String featureName(Map<String, String> nameMap, String featureCode) {
        if (AiQuotaConstant.PACK_FEATURE_ALL.equals(featureCode)) {
            return "全功能通用包";
        }
        return nameMap.getOrDefault(featureCode, featureCode);
    }
}
