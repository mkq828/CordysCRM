package cn.cordys.crm.customer.tag.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.customer.tag.domain.CustomerTag;
import cn.cordys.crm.customer.tag.domain.CustomerTagRel;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户标签字典与打标。AI 自动打标签与人工打标签共用 {@link #tagCustomer}：标签名租户内唯一，
 * 不存在则按名自动创建；关联去重（UNIQUE 约束兜底），来源区分 manual/AI。
 */
@Service
@Slf4j
public class CustomerTagService {

    /** 「流失预警」内置标签名：自动打标签扫描的固定阈值标签，客户列表筛选也用它做「分群」 */
    public static final String CHURN_TAG_NAME = "流失预警";

    /** 常见状态型标签的语义色：按标签名（即客户状态）区分颜色，未命中的走哈希取色兜底 */
    private static final Map<String, String> TAG_COLORS = Map.of(
            "流失预警", "#e05a00",
            "待跟进", "#f97316",
            "新客户", "#16a34a",
            "高价值", "#d97706",
            "待建联", "#0891b2",
            "重点跟进", "#1677ff"
    );

    /** 未命中语义色的标签，按名称哈希从该色板稳定取色，保证同名同色、不同名尽量异色 */
    private static final String[] FALLBACK_COLORS = {
            "#1677ff", "#16a34a", "#d97706", "#7c3aed", "#0891b2", "#db2777", "#4f46e5", "#0d9488"
    };

    @Resource
    private BaseMapper<CustomerTag> tagMapper;

    @Resource
    private BaseMapper<CustomerTagRel> relMapper;

    /** 租户标签字典（列表筛选、自动打标签候选） */
    public List<CustomerTag> list(String orgId) {
        LambdaQueryWrapper<CustomerTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerTag::getOrganizationId, orgId).orderByAsc(CustomerTag::getCreateTime);
        return tagMapper.selectListByLambda(wrapper);
    }

    /** 取租户内指定名称的标签，不存在则创建（AI 打标签时标签可能来自模型建议，需懒创建） */
    @Transactional(rollbackFor = Exception.class)
    public CustomerTag ensureTag(String orgId, String name) {
        CustomerTag existing = findByName(orgId, name);
        if (existing != null) {
            return existing;
        }
        long now = System.currentTimeMillis();
        CustomerTag tag = new CustomerTag();
        tag.setId(IDGenerator.nextStr());
        tag.setOrganizationId(orgId);
        tag.setName(name);
        tag.setColor(colorOf(name));
        tag.setBuiltIn(false);
        tag.setCreateTime(now);
        tag.setUpdateTime(now);
        tagMapper.insert(tag);
        return tag;
    }

    /** 给客户打标签：确保标签存在后建立关联（幂等，重复打同一标签不产生重复关联） */
    @Transactional(rollbackFor = Exception.class)
    public void tagCustomer(String orgId, String customerId, String tagName, String source) {
        if (StringUtils.isBlank(tagName)) {
            return;
        }
        CustomerTag tag = ensureTag(orgId, tagName.trim());
        associate(orgId, customerId, tag.getId(), source);
    }

    /** 客户已打标签列表（客户详情抽屉展示） */
    public List<CustomerTag> listCustomerTags(String orgId, String customerId) {
        LambdaQueryWrapper<CustomerTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(CustomerTagRel::getCustomerId, customerId);
        List<CustomerTagRel> rels = relMapper.selectListByLambda(relWrapper);
        if (rels.isEmpty()) {
            return List.of();
        }
        List<String> tagIds = rels.stream().map(CustomerTagRel::getTagId).toList();
        return tagMapper.selectByColumn("id", tagIds.toArray(new String[0]));
    }

    /** 批量查询客户的已打标签（客户列表页展示标签列，避免逐行 N+1 请求） */
    public Map<String, List<CustomerTag>> listCustomerTagsByCustomerIds(String orgId, List<String> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<CustomerTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.in(CustomerTagRel::getCustomerId, customerIds);
        List<CustomerTagRel> rels = relMapper.selectListByLambda(relWrapper);
        if (rels.isEmpty()) {
            return Map.of();
        }
        List<String> tagIds = rels.stream().map(CustomerTagRel::getTagId).distinct().toList();
        Map<String, CustomerTag> tagById = tagMapper.selectByColumn("id", tagIds.toArray(new String[0]))
                .stream().collect(Collectors.toMap(CustomerTag::getId, tag -> tag));
        Map<String, List<CustomerTag>> result = new HashMap<>();
        for (CustomerTagRel rel : rels) {
            CustomerTag tag = tagById.get(rel.getTagId());
            if (tag != null) {
                result.computeIfAbsent(rel.getCustomerId(), k -> new ArrayList<>()).add(tag);
            }
        }
        return result;
    }

    /** 按标签名取语义色，未命中则名称哈希取色（同名同色、不同名尽量异色） */
    private String colorOf(String name) {
        String color = TAG_COLORS.get(name);
        if (color != null) {
            return color;
        }
        int hash = 0;
        for (int i = 0; i < name.length(); i++) {
            hash = hash * 31 + name.charAt(i);
        }
        return FALLBACK_COLORS[Math.abs(hash) % FALLBACK_COLORS.length];
    }

    private CustomerTag findByName(String orgId, String name) {
        LambdaQueryWrapper<CustomerTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerTag::getOrganizationId, orgId).eq(CustomerTag::getName, name);
        List<CustomerTag> list = tagMapper.selectListByLambda(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    private void associate(String orgId, String customerId, String tagId, String source) {
        LambdaQueryWrapper<CustomerTagRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerTagRel::getCustomerId, customerId).eq(CustomerTagRel::getTagId, tagId);
        if (!relMapper.selectListByLambda(wrapper).isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        CustomerTagRel rel = new CustomerTagRel();
        rel.setId(IDGenerator.nextStr());
        rel.setOrganizationId(orgId);
        rel.setCustomerId(customerId);
        rel.setTagId(tagId);
        rel.setSource(source);
        rel.setCreateTime(now);
        rel.setCreateUser(null);
        relMapper.insert(rel);
    }
}
