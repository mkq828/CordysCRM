package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.TenantPlanStatus;
import cn.cordys.crm.system.domain.SysEdition;
import cn.cordys.crm.system.domain.SysEditionFeature;
import cn.cordys.crm.system.domain.SysFeature;
import cn.cordys.crm.system.domain.TenantEdition;
import cn.cordys.crm.system.dto.request.EditionSaveRequest;
import cn.cordys.crm.system.dto.request.FeatureSaveRequest;
import cn.cordys.crm.system.dto.response.EditionResponse;
import cn.cordys.crm.system.dto.response.FeatureResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 版本与套餐配置服务
 * <p>
 * 维护版本（sys_edition）、功能（sys_feature）、版本-功能归属（sys_edition_feature），
 * 并在租户开通/续费时写入版本快照（tenant_edition）。
 * </p>
 */
@Service
public class EditionService {

    @Resource
    private BaseMapper<SysEdition> editionMapper;

    @Resource
    private BaseMapper<SysFeature> featureMapper;

    @Resource
    private BaseMapper<SysEditionFeature> editionFeatureMapper;

    @Resource
    private BaseMapper<TenantEdition> tenantEditionMapper;

    // ==================== 版本 ====================

    /**
     * 版本列表（全量，按 sort 升序）
     */
    public List<EditionResponse> listEditions() {
        LambdaQueryWrapper<SysEdition> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysEdition::getSort);
        return editionMapper.selectListByLambda(wrapper).stream()
                .map(e -> BeanUtils.copyBean(new EditionResponse(), e))
                .toList();
    }

    /**
     * 启用中的版本（开通/续费下拉用）
     */
    public List<EditionResponse> listEnabledEditions() {
        LambdaQueryWrapper<SysEdition> wrapper = new LambdaQueryWrapper<SysEdition>()
                .eq(SysEdition::getStatus, 1);
        wrapper.orderByAsc(SysEdition::getSort);
        return editionMapper.selectListByLambda(wrapper).stream()
                .map(e -> BeanUtils.copyBean(new EditionResponse(), e))
                .toList();
    }

    /**
     * 按编码查询版本
     */
    public SysEdition getEditionByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        List<SysEdition> list = editionMapper.selectListByLambda(new LambdaQueryWrapper<SysEdition>()
                .eq(SysEdition::getCode, code));
        return list.isEmpty() ? null : list.getFirst();
    }

    /**
     * 新增版本（含功能归属）
     */
    @OperationLog(module = LogModule.SYSTEM_EDITION, type = LogType.ADD)
    public void addEdition(EditionSaveRequest request, String operatorId) {
        long now = System.currentTimeMillis();
        ensureEditionCodeUnique(request.getCode(), null);
        SysEdition edition = new SysEdition();
        edition.setId(IDGenerator.nextStr());
        edition.setCreateTime(now);
        edition.setCreateUser(operatorId);
        edition.setUpdateTime(now);
        edition.setUpdateUser(operatorId);
        applyEdition(edition, request);
        editionMapper.insert(edition);
        saveMapping(edition.getId(), request.getFeatureIds(), operatorId);

        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(edition)
                .resourceId(edition.getId())
                .resourceName(edition.getName())
                .build());
    }

    /**
     * 更新版本（含功能归属）
     */
    @OperationLog(module = LogModule.SYSTEM_EDITION, type = LogType.UPDATE)
    public void updateEdition(EditionSaveRequest request, String operatorId) {
        long now = System.currentTimeMillis();
        SysEdition edition = editionMapper.selectByPrimaryKey(request.getId());
        if (edition == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        SysEdition original = BeanUtils.copyBean(new SysEdition(), edition);
        ensureEditionCodeUnique(request.getCode(), edition.getId());
        applyEdition(edition, request);
        edition.setUpdateTime(now);
        edition.setUpdateUser(operatorId);
        editionMapper.updateById(edition);
        saveMapping(edition.getId(), request.getFeatureIds(), operatorId);

        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(original)
                .modifiedValue(edition)
                .resourceId(edition.getId())
                .resourceName(edition.getName())
                .build());
    }

    /**
     * 删除版本（连带删除功能归属）
     */
    @OperationLog(module = LogModule.SYSTEM_EDITION, type = LogType.DELETE, resourceId = "{#id}")
    public void deleteEdition(String id) {
        SysEdition edition = editionMapper.selectByPrimaryKey(id);
        if (edition == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        editionFeatureMapper.deleteByLambda(new LambdaQueryWrapper<SysEditionFeature>()
                .eq(SysEditionFeature::getEditionId, id));
        editionMapper.deleteByPrimaryKey(id);
        OperationLogContext.setResourceName(edition.getName());
    }

    private void applyEdition(SysEdition edition, EditionSaveRequest request) {
        edition.setCode(request.getCode());
        edition.setName(request.getName());
        edition.setYearPrice(request.getYearPrice());
        edition.setFirstYearPrice(request.getFirstYearPrice());
        edition.setSoftLimit(request.getSoftLimit());
        edition.setValidityDays(request.getValidityDays() == null ? 365 : request.getValidityDays());
        edition.setAiMonthlyQuota(request.getAiMonthlyQuota() == null ? 0 : request.getAiMonthlyQuota());
        edition.setSort(request.getSort() == null ? 0 : request.getSort());
        edition.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private void ensureEditionCodeUnique(String code, String excludeId) {
        List<SysEdition> exist = editionMapper.selectListByLambda(new LambdaQueryWrapper<SysEdition>()
                .eq(SysEdition::getCode, code));
        boolean duplicated = exist.stream().anyMatch(e -> !e.getId().equals(excludeId));
        if (duplicated) {
            throw new GenericException(Translator.get("edition.code_exists"));
        }
    }

    // ==================== 功能 ====================

    /**
     * 功能列表（全量）
     */
    public List<FeatureResponse> listFeatures() {
        LambdaQueryWrapper<SysFeature> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysFeature::getCategory);
        wrapper.orderByAsc(SysFeature::getFeatureCode);
        return featureMapper.selectListByLambda(wrapper).stream()
                .map(f -> BeanUtils.copyBean(new FeatureResponse(), f))
                .toList();
    }

    /**
     * 新增功能
     */
    @OperationLog(module = LogModule.SYSTEM_EDITION, type = LogType.ADD)
    public void addFeature(FeatureSaveRequest request, String operatorId) {
        long now = System.currentTimeMillis();
        ensureFeatureCodeUnique(request.getFeatureCode(), null);
        SysFeature feature = new SysFeature();
        feature.setId(IDGenerator.nextStr());
        feature.setCreateTime(now);
        feature.setCreateUser(operatorId);
        feature.setUpdateTime(now);
        feature.setUpdateUser(operatorId);
        applyFeature(feature, request);
        featureMapper.insert(feature);

        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(feature)
                .resourceId(feature.getId())
                .resourceName(feature.getName())
                .build());
    }

    /**
     * 更新功能
     */
    @OperationLog(module = LogModule.SYSTEM_EDITION, type = LogType.UPDATE)
    public void updateFeature(FeatureSaveRequest request, String operatorId) {
        long now = System.currentTimeMillis();
        SysFeature feature = featureMapper.selectByPrimaryKey(request.getId());
        if (feature == null) {
            throw new GenericException(Translator.get("feature.not_found"));
        }
        SysFeature original = BeanUtils.copyBean(new SysFeature(), feature);
        ensureFeatureCodeUnique(request.getFeatureCode(), feature.getId());
        applyFeature(feature, request);
        feature.setUpdateTime(now);
        feature.setUpdateUser(operatorId);
        featureMapper.updateById(feature);

        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(original)
                .modifiedValue(feature)
                .resourceId(feature.getId())
                .resourceName(feature.getName())
                .build());
    }

    /**
     * 删除功能（连带删除归属关系）
     */
    @OperationLog(module = LogModule.SYSTEM_EDITION, type = LogType.DELETE, resourceId = "{#id}")
    public void deleteFeature(String id) {
        SysFeature feature = featureMapper.selectByPrimaryKey(id);
        if (feature == null) {
            throw new GenericException(Translator.get("feature.not_found"));
        }
        editionFeatureMapper.deleteByLambda(new LambdaQueryWrapper<SysEditionFeature>()
                .eq(SysEditionFeature::getFeatureId, id));
        featureMapper.deleteByPrimaryKey(id);
        OperationLogContext.setResourceName(feature.getName());
    }

    private void applyFeature(SysFeature feature, FeatureSaveRequest request) {
        feature.setFeatureCode(request.getFeatureCode());
        feature.setName(request.getName());
        feature.setCategory(request.getCategory());
        feature.setEnable(request.getEnable() == null || request.getEnable());
    }

    private void ensureFeatureCodeUnique(String code, String excludeId) {
        List<SysFeature> exist = featureMapper.selectListByLambda(new LambdaQueryWrapper<SysFeature>()
                .eq(SysFeature::getFeatureCode, code));
        boolean duplicated = exist.stream().anyMatch(f -> !f.getId().equals(excludeId));
        if (duplicated) {
            throw new GenericException(Translator.get("feature.code_exists"));
        }
    }

    // ==================== 归属 ====================

    /**
     * 版本归属的功能ID集合
     */
    public List<String> listFeatureIdsByEditionId(String editionId) {
        return editionFeatureMapper.selectListByLambda(new LambdaQueryWrapper<SysEditionFeature>()
                        .eq(SysEditionFeature::getEditionId, editionId))
                .stream()
                .map(SysEditionFeature::getFeatureId)
                .toList();
    }

    /**
     * 版本归属的功能编码集合（快照用）
     */
    public List<String> listFeatureCodesByEditionCode(String editionCode) {
        SysEdition edition = getEditionByCode(editionCode);
        if (edition == null) {
            return List.of();
        }
        List<String> featureIds = listFeatureIdsByEditionId(edition.getId());
        if (featureIds.isEmpty()) {
            return List.of();
        }
        return featureMapper.selectListByLambda(new LambdaQueryWrapper<SysFeature>()
                        .in(SysFeature::getId, featureIds))
                .stream()
                .map(SysFeature::getFeatureCode)
                .toList();
    }

    /**
     * 重建版本-功能归属
     */
    public void saveMapping(String editionId, List<String> featureIds, String operatorId) {
        editionFeatureMapper.deleteByLambda(new LambdaQueryWrapper<SysEditionFeature>()
                .eq(SysEditionFeature::getEditionId, editionId));
        if (featureIds == null || featureIds.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String featureId : featureIds) {
            SysEditionFeature rel = new SysEditionFeature();
            rel.setId(IDGenerator.nextStr());
            rel.setEditionId(editionId);
            rel.setFeatureId(featureId);
            rel.setCreateTime(now);
            rel.setCreateUser(operatorId);
            rel.setUpdateTime(now);
            rel.setUpdateUser(operatorId);
            editionFeatureMapper.insert(rel);
        }
    }

    // ==================== 快照 ====================

    /**
     * 写入/更新租户版本快照（开通/续费时调用）
     */
    public void writeSnapshot(String organizationId, String editionCode, Long expireTime, BigDecimal price, String operatorId) {
        SysEdition edition = getEditionByCode(editionCode);
        if (edition == null) {
            throw new GenericException(Translator.get("edition.not_found"));
        }
        List<String> featureCodes = listFeatureCodesByEditionCode(editionCode);
        long now = System.currentTimeMillis();

        TenantEdition snapshot = getByOrganizationId(organizationId);
        boolean isNew = snapshot == null;
        if (isNew) {
            snapshot = new TenantEdition();
            snapshot.setId(IDGenerator.nextStr());
            snapshot.setOrganizationId(organizationId);
            snapshot.setCreateTime(now);
            snapshot.setCreateUser(operatorId);
        }
        snapshot.setEditionCode(editionCode);
        snapshot.setEditionName(edition.getName());
        snapshot.setPrice(price != null ? price : edition.getYearPrice());
        snapshot.setFeaturesJson(JSON.toJSONString(featureCodes));
        snapshot.setAiMonthlyQuota(edition.getAiMonthlyQuota());
        snapshot.setStartTime(now);
        snapshot.setExpireTime(expireTime);
        snapshot.setStatus(TenantPlanStatus.ACTIVE.getValue());
        snapshot.setUpdateTime(now);
        snapshot.setUpdateUser(operatorId);

        if (isNew) {
            tenantEditionMapper.insert(snapshot);
        } else {
            tenantEditionMapper.updateById(snapshot);
        }
    }

    /**
     * 按组织查询版本快照
     */
    public TenantEdition getByOrganizationId(String organizationId) {
        List<TenantEdition> list = tenantEditionMapper.selectListByLambda(new LambdaQueryWrapper<TenantEdition>()
                .eq(TenantEdition::getOrganizationId, organizationId));
        return list.isEmpty() ? null : list.getFirst();
    }

    /**
     * 判断租户是否拥有某 AI 功能（查看型功能只挂这一层 G3 权限开关，不挂 G2 额度）。
     * 试用租户（无生效版本快照）视为放开；已开通租户按「版本→功能」实时映射判断，避免功能新增后老快照过期。
     */
    public boolean hasFeature(String organizationId, String featureCode) {
        TenantEdition edition = getByOrganizationId(organizationId);
        if (edition == null) {
            return true;
        }
        return listFeatureCodesByEditionCode(edition.getEditionCode()).contains(featureCode);
    }
}
