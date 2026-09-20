package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.LinkScenarioKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.domain.*;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.ControlRuleProp;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.form.base.LinkField;
import cn.cordys.crm.system.dto.form.base.LinkScenario;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 表单/字段的一次性初始化与历史数据迁移服务。
 *
 * <p>从 {@link ModuleFormService} 中拆分出的「数据初始化 / 数据迁移」方法集合，
 * 全部通过 {@link cn.cordys.common.service.DataInitService#initOneTime()} 以幂等键
 * 执行一次，避免与表单配置的日常读写逻辑耦合。</p>
 *
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ModuleFormMigrationService {

    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    private static final String CONTROL_RULES_KEY = "showControlRules";
    private static final String SHOW_FIELD_KEY = "showFields";
    private static final String SUB_FIELDS = "subFields";
    private static final String UPGRADE_EXT_FIELD = "ext_ver";

    @Value("classpath:form/form.json")
    private org.springframework.core.io.Resource formResource;
    @Value("classpath:form/field.json")
    private org.springframework.core.io.Resource fieldResource;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleFormBlob> moduleFormBlobMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;
    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private ModuleFieldService moduleFieldService;

    /**
     * 表单初始化
     */
    public void initForm() {
        initFormAndFields(FormKey.allKeys());
    }

    /**
     * 初始化组织表单配置。
     * <p>默认组织走内置 JSON 初始化；其它组织复制默认组织 100001 当前的表单/字段/属性，
     * 保证新租户开通后即拥有完整可用的表单（后续可在表单设计器里自行修改）。</p>
     *
     * @param organizationId 目标组织 ID
     */
    public void initForm(String organizationId) {
        if (DEFAULT_ORGANIZATION_ID.equals(organizationId)) {
            initForm();
            return;
        }
        copyFormsFromDefault(organizationId);
    }

    /**
     * 复制默认组织 100001 的表单配置到目标组织。
     * <p>重映射表单 id、字段 id，以及字段/表单属性 JSON 内的跨字段 id 引用
     * （resourceFieldId / subTableFieldId / showControlRules.fieldIds / 联动字段等）。</p>
     */
    private void copyFormsFromDefault(String organizationId) {
        ModuleForm example = new ModuleForm();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        List<ModuleForm> sourceForms = moduleFormMapper.select(example);
        if (CollectionUtils.isEmpty(sourceForms)) {
            log.warn("默认组织无表单配置，跳过表单初始化: {}", organizationId);
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, String> idMap = new HashMap<>();

        // 1. 复制表单
        List<ModuleForm> newForms = new ArrayList<>(sourceForms.size());
        for (ModuleForm source : sourceForms) {
            String newId = IDGenerator.nextStr();
            idMap.put(source.getId(), newId);
            ModuleForm form = new ModuleForm();
            form.setId(newId);
            form.setFormKey(source.getFormKey());
            form.setOrganizationId(organizationId);
            form.setCreateUser(InternalUser.ADMIN.getValue());
            form.setCreateTime(now);
            form.setUpdateUser(InternalUser.ADMIN.getValue());
            form.setUpdateTime(now);
            newForms.add(form);
        }

        // 2. 复制字段（默认组织全部字段，按表单归属）
        List<String> sourceFormIds = sourceForms.stream().map(ModuleForm::getId).toList();
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getFormId, sourceFormIds);
        List<ModuleField> sourceFields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        List<ModuleField> newFields = new ArrayList<>(sourceFields.size());
        for (ModuleField source : sourceFields) {
            String newId = IDGenerator.nextStr();
            idMap.put(source.getId(), newId);
            ModuleField field = new ModuleField();
            field.setId(newId);
            field.setFormId(idMap.get(source.getFormId()));
            field.setName(source.getName());
            field.setInternalKey(source.getInternalKey());
            field.setType(source.getType());
            field.setMobile(source.getMobile());
            field.setPos(source.getPos());
            field.setCreateUser(InternalUser.ADMIN.getValue());
            field.setCreateTime(now);
            field.setUpdateUser(InternalUser.ADMIN.getValue());
            field.setUpdateTime(now);
            newFields.add(field);
        }

        // 3. 复制表单属性与字段属性（重映射 id 引用）
        List<ModuleFormBlob> newFormBlobs = new ArrayList<>(sourceForms.size());
        for (ModuleForm source : sourceForms) {
            ModuleFormBlob blob = moduleFormBlobMapper.selectByPrimaryKey(source.getId());
            if (blob != null) {
                ModuleFormBlob newBlob = new ModuleFormBlob();
                newBlob.setId(idMap.get(source.getId()));
                newBlob.setProp(remapIds(blob.getProp(), idMap));
                newFormBlobs.add(newBlob);
            }
        }
        List<ModuleFieldBlob> newFieldBlobs = new ArrayList<>(sourceFields.size());
        for (ModuleField source : sourceFields) {
            ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(source.getId());
            if (blob != null) {
                ModuleFieldBlob newBlob = new ModuleFieldBlob();
                newBlob.setId(idMap.get(source.getId()));
                newBlob.setProp(remapIds(blob.getProp(), idMap));
                newFieldBlobs.add(newBlob);
            }
        }

        moduleFormMapper.batchInsert(newForms);
        moduleFormBlobMapper.batchInsert(newFormBlobs);
        moduleFieldMapper.batchInsert(newFields);
        moduleFieldBlobMapper.batchInsert(newFieldBlobs);
    }

    /**
     * 回填存量租户缺失的表单配置（在注册开通表单播种逻辑之前已创建的组织）。
     */
    public void backfillTenantForms() {
        List<Organization> organizations = DataAccessLayer.with(Organization.class).selectAll(null);
        for (Organization organization : organizations) {
            if (DEFAULT_ORGANIZATION_ID.equals(organization.getId())) {
                continue;
            }
            LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
            formWrapper.eq(ModuleForm::getOrganizationId, organization.getId());
            if (CollectionUtils.isNotEmpty(moduleFormMapper.selectListByLambda(formWrapper))) {
                continue;
            }
            initForm(organization.getId());
        }
    }

    /**
     * 回填跟进记录表单的联系人联动与默认日期配置（存量租户）。
     */
    public void backfillRecordFollowConfig() {
        backfillFollowFormConfig(FormKey.FOLLOW_RECORD.getKey(), "recordCustomer", "recordContact", "recordTime");
    }

    /**
     * 回填跟进计划表单的联系人联动配置（存量租户）。
     */
    public void backfillPlanFollowConfig() {
        backfillFollowFormConfig(FormKey.FOLLOW_PLAN.getKey(), "planCustomer", "planContact", null);
    }

    /**
     * 回填商机表单联系人联动（按当前客户过滤，避免混入其他客户联系人）。
     */
    public void backfillOpportunityContactConfig() {
        backfillFollowFormConfig(FormKey.OPPORTUNITY.getKey(), "opportunityCustomer", "opportunityContact", null);
    }

    /**
     * 回填客户表单字段配置（存量租户）：
     * <p>1. 客户行业、客户来源 SELECT 改为 INPUT；</p>
     * <p>2. 客户等级下拉选项改为 A~E 级；</p>
     * <p>3. 删除客户类型、线上来源详情两个字段。</p>
     */
    public void backfillCustomerFieldConfig() {
        LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
        formWrapper.eq(ModuleForm::getFormKey, FormKey.CUSTOMER.getKey());
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            return;
        }
        List<String> formIds = forms.stream().map(ModuleForm::getId).toList();
        List<String> internalKeys = List.of("customerIndustry", "customerLevel", "customerSource",
                "customerType", "customerOnlineSource");
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getFormId, formIds);
        fieldWrapper.in(ModuleField::getInternalKey, internalKeys);
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);

        Map<String, Map<String, ModuleField>> formFieldMap = new HashMap<>();
        for (ModuleField field : fields) {
            formFieldMap.computeIfAbsent(field.getFormId(), k -> new HashMap<>(5))
                    .put(field.getInternalKey(), field);
        }

        for (ModuleForm form : forms) {
            Map<String, ModuleField> fieldMap = formFieldMap.get(form.getId());
            if (fieldMap == null) {
                continue;
            }
            convertToInput(fieldMap.get("customerIndustry"));
            convertToInput(fieldMap.get("customerSource"));
            updateLevelOptions(fieldMap.get("customerLevel"));
            deleteCustomerFields(fieldMap);
        }
    }

    /**
     * 回填线索转客户 / 线索转商机的表单联动字段映射（存量租户）。
     * <p>线索转客户、转商机时仅复制「表单联动规则」里配置过的字段，此前只给
     * 线索转联系人配置了姓名/电话，客户、商机的联动为空，导致来源等字段丢失。</p>
     */
    public void backfillClueLinkRules() {
        backfillLinkScenario(FormKey.CUSTOMER.getKey(), LinkScenarioKey.CLUE_TO_CUSTOMER.name(),
                Map.of("clueName", "customerName", "clueSource", "customerSource",
                        "clueArea", "customerArea", "clueOwner", "customerOwner"));
        backfillLinkScenario(FormKey.OPPORTUNITY.getKey(), LinkScenarioKey.CLUE_TO_OPPORTUNITY.name(),
                Map.of("clueProduct", "opportunityProduct", "clueSource", "opportunitySource",
                        "clueArea", "opportunityArea", "clueOwner", "opportunityOwner"));
    }

    /**
     * 回填「来源」类字段全局统一为输入框（线索来源、商机来源），并删除线索的「线上来源详情」。
     * <p>客户来源已在前一步处理，此处补齐线索、商机，保证同一概念交互方式一致，
     * 同时把已存在的下拉选项码回填成中文文本。</p>
     */
    public void backfillSourceFieldConfig() {
        backfillSourceInput(FormKey.CLUE.getKey(), "clueSource", "clue_field");
        backfillSourceInput(FormKey.OPPORTUNITY.getKey(), "opportunitySource", "opportunity_field");
        deleteClueOnlineSourceFields();
    }

    /**
     * 回填合同回款记录表单字段：删除写死的「收款银行」「收款银行账号」两个下拉，
     * 新增「收款账户」数据源字段（引用 bank_account）与「付款凭证」附件字段（存量租户）。
     * <p>数据源类型 BANK_ACCOUNT 的字段值由回款记录自行保存账户 ID，展示时由
     * 后端数据源解析器映射为账户名称，附件则走既有的上传/预览链路。</p>
     */
    public void backfillContractPaymentRecordFields() {
        LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
        formWrapper.eq(ModuleForm::getFormKey, FormKey.CONTRACT_PAYMENT_RECORD.getKey());
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            return;
        }
        List<String> formIds = forms.stream().map(ModuleForm::getId).toList();

        // 1. 删除旧的「收款银行」「收款银行账号」字段（行 + blob）
        LambdaQueryWrapper<ModuleField> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(ModuleField::getFormId, formIds);
        deleteWrapper.in(ModuleField::getInternalKey, List.of("contractPaymentRecordBank", "contractPaymentRecordBankNo"));
        List<ModuleField> oldFields = moduleFieldMapper.selectListByLambda(deleteWrapper);
        if (CollectionUtils.isNotEmpty(oldFields)) {
            List<String> ids = oldFields.stream().map(ModuleField::getId).toList();
            extModuleFieldMapper.deleteByIds(ids);
            extModuleFieldMapper.deletePropByIds(ids);
        }

        // 2. 幂等保护：收集各表单已存在的 internalKey
        LambdaQueryWrapper<ModuleField> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.in(ModuleField::getFormId, formIds);
        List<ModuleField> existFields = moduleFieldMapper.selectListByLambda(existWrapper);
        Map<String, Set<String>> existingKeysByForm = new HashMap<>();
        for (ModuleField field : existFields) {
            existingKeysByForm.computeIfAbsent(field.getFormId(), k -> new HashSet<>()).add(field.getInternalKey());
        }

        // 3. 追加「收款账户」「付款凭证」两个字段
        List<ModuleField> newFields = new ArrayList<>();
        List<ModuleFieldBlob> newFieldBlobs = new ArrayList<>();
        for (ModuleForm form : forms) {
            Set<String> existingKeys = existingKeysByForm.getOrDefault(form.getId(), Set.of());
            Long maxPos = extModuleFieldMapper.getMaxFieldPosByFormId(form.getId());
            AtomicLong pos = new AtomicLong(maxPos == null ? 0L : maxPos);
            for (Map<String, Object> fieldDef : List.of(bankAccountFieldDef(), paymentVoucherFieldDef())) {
                String internalKey = fieldDef.get("internalKey").toString();
                if (existingKeys.contains(internalKey)) {
                    continue;
                }
                ModuleField field = supplyFieldInfo(fieldDef, form.getId(), pos.incrementAndGet(), new HashMap<>(2));
                fieldDef.put("id", field.getId());
                newFields.add(field);
                ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
                fieldBlob.setId(field.getId());
                fieldBlob.setProp(JSON.toJSONString(fieldDef));
                newFieldBlobs.add(fieldBlob);
            }
        }
        if (CollectionUtils.isNotEmpty(newFields)) {
            moduleFieldMapper.batchInsert(newFields);
        }
        if (CollectionUtils.isNotEmpty(newFieldBlobs)) {
            moduleFieldBlobMapper.batchInsert(newFieldBlobs);
        }
    }

    /**
     * 「收款账户」数据源字段定义（与 field.json 中的新字段保持一致）。
     */
    private Map<String, Object> bankAccountFieldDef() {
        Map<String, Object> fieldDef = new HashMap<>(12);
        fieldDef.put("name", "收款账户");
        fieldDef.put("internalKey", "contractPaymentRecordBankAccount");
        fieldDef.put("type", "DATA_SOURCE");
        fieldDef.put("dataSourceType", "BANK_ACCOUNT");
        fieldDef.put("showLabel", true);
        fieldDef.put("readable", true);
        fieldDef.put("editable", true);
        fieldDef.put("fieldWidth", 1);
        fieldDef.put("rules", List.of(Map.of("key", "required")));
        fieldDef.put("mobile", true);
        return fieldDef;
    }

    /**
     * 「付款凭证」附件字段定义（与 field.json 中的新字段保持一致）。
     */
    private Map<String, Object> paymentVoucherFieldDef() {
        Map<String, Object> fieldDef = new HashMap<>(14);
        fieldDef.put("name", "付款凭证");
        fieldDef.put("internalKey", "contractPaymentRecordVoucher");
        fieldDef.put("type", "ATTACHMENT");
        fieldDef.put("showLabel", true);
        fieldDef.put("description", "");
        fieldDef.put("defaultValue", new ArrayList<>());
        fieldDef.put("readable", true);
        fieldDef.put("editable", true);
        fieldDef.put("mobile", true);
        fieldDef.put("fieldWidth", 1);
        fieldDef.put("rules", new ArrayList<>());
        fieldDef.put("onlyOne", false);
        fieldDef.put("accept", "jpg,jpeg,png,pdf");
        fieldDef.put("limitSize", "20MB");
        return fieldDef;
    }

    /**
     * 回填跟进类表单字段配置：
     * <p>1. 联系人下拉按当前客户过滤，避免混入其他客户联系人；
     * 2. 该客户仅有一个联系人时自动选中；
     * 3. 跟进时间默认当前日期（timeKey 为空则跳过）。</p>
     */
    private void backfillFollowFormConfig(String formKey, String customerKey, String contactKey, String timeKey) {
        LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
        formWrapper.eq(ModuleForm::getFormKey, formKey);
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            return;
        }
        List<String> formIds = forms.stream().map(ModuleForm::getId).toList();
        List<String> internalKeys = new ArrayList<>(3);
        internalKeys.add(customerKey);
        internalKeys.add(contactKey);
        if (StringUtils.isNotBlank(timeKey)) {
            internalKeys.add(timeKey);
        }
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getFormId, formIds);
        fieldWrapper.in(ModuleField::getInternalKey, internalKeys);
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);

        Map<String, Map<String, String>> formFieldMap = new HashMap<>();
        for (ModuleField field : fields) {
            formFieldMap.computeIfAbsent(field.getFormId(), k -> new HashMap<>(3))
                    .put(field.getInternalKey(), field.getId());
        }

        for (ModuleForm form : forms) {
            Map<String, String> fieldMap = formFieldMap.get(form.getId());
            if (fieldMap == null) {
                continue;
            }
            updateContactField(fieldMap, customerKey, contactKey);
            if (StringUtils.isNotBlank(timeKey)) {
                updateTimeField(fieldMap, timeKey);
            }
        }
    }

    /**
     * 联系人字段联动：按当前客户过滤联系人，且仅一个联系人时自动选中。
     */
    @SuppressWarnings("unchecked")
    private void updateContactField(Map<String, String> fieldMap, String customerKey, String contactKey) {
        String customerFieldId = fieldMap.get(customerKey);
        String contactFieldId = fieldMap.get(contactKey);
        if (StringUtils.isBlank(contactFieldId) || StringUtils.isBlank(customerFieldId)) {
            return;
        }
        ModuleFieldBlob contactBlob = moduleFieldBlobMapper.selectByPrimaryKey(contactFieldId);
        if (contactBlob == null || StringUtils.isBlank(contactBlob.getProp())) {
            return;
        }
        Map<String, Object> propMap = JSON.parseMap(contactBlob.getProp());
        boolean changed = false;
        if (propMap.get("combineSearch") == null) {
            Map<String, Object> condition = new HashMap<>(8);
            condition.put("leftFieldId", "customerId");
            condition.put("leftFieldType", "DATA_SOURCE");
            condition.put("operator", "IN");
            condition.put("matchType", "MATCH_FIELD");
            condition.put("rightFieldId", customerFieldId);
            condition.put("rightFieldCustom", false);
            condition.put("rightFieldCustomValue", "");
            condition.put("rightFieldType", "DATA_SOURCE");
            Map<String, Object> combineSearch = new HashMap<>(2);
            combineSearch.put("searchMode", "OR");
            combineSearch.put("conditions", List.of(condition));
            propMap.put("combineSearch", combineSearch);
            changed = true;
        }
        if (!Boolean.TRUE.equals(propMap.get("autoSelectSingleOption"))) {
            propMap.put("autoSelectSingleOption", true);
            changed = true;
        }
        if (changed) {
            contactBlob.setProp(JSON.toJSONString(propMap));
            moduleFieldBlobMapper.updateById(contactBlob);
        }
    }

    /**
     * 跟进时间字段默认当前日期。
     */
    @SuppressWarnings("unchecked")
    private void updateTimeField(Map<String, String> fieldMap, String timeKey) {
        String timeFieldId = fieldMap.get(timeKey);
        if (StringUtils.isBlank(timeFieldId)) {
            return;
        }
        ModuleFieldBlob timeBlob = moduleFieldBlobMapper.selectByPrimaryKey(timeFieldId);
        if (timeBlob == null || StringUtils.isBlank(timeBlob.getProp())) {
            return;
        }
        Map<String, Object> propMap = JSON.parseMap(timeBlob.getProp());
        if (!"current".equals(propMap.get("dateDefaultType"))) {
            propMap.put("dateDefaultType", "current");
            timeBlob.setProp(JSON.toJSONString(propMap));
            moduleFieldBlobMapper.updateById(timeBlob);
        }
    }

    /**
     * 将字段转换为输入框：行 type 与 blob type 同时改，并清理下拉专属配置。
     */
    @SuppressWarnings("unchecked")
    private void convertToInput(ModuleField field) {
        if (field == null) {
            return;
        }
        if (!"INPUT".equals(field.getType())) {
            field.setType("INPUT");
            moduleFieldMapper.updateById(field);
        }
        ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(field.getId());
        if (blob == null || StringUtils.isBlank(blob.getProp())) {
            return;
        }
        Map<String, Object> propMap = JSON.parseMap(blob.getProp());
        boolean changed = false;
        if (!"INPUT".equals(propMap.get("type"))) {
            propMap.put("type", "INPUT");
            changed = true;
        }
        String[] removeKeys = {"options", "customOptions", "optionSource", "defaultValue",
                "refId", "refFormKey", "linkProp", "showControlRules"};
        for (String key : removeKeys) {
            if (propMap.containsKey(key)) {
                propMap.remove(key);
                changed = true;
            }
        }
        if (changed) {
            blob.setProp(JSON.toJSONString(propMap));
            moduleFieldBlobMapper.updateById(blob);
        }
    }

    /**
     * 客户等级下拉选项改为 A~E 级（值 1~5 保留数据语义）。
     */
    @SuppressWarnings("unchecked")
    private void updateLevelOptions(ModuleField field) {
        if (field == null) {
            return;
        }
        ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(field.getId());
        if (blob == null || StringUtils.isBlank(blob.getProp())) {
            return;
        }
        Map<String, Object> propMap = JSON.parseMap(blob.getProp());
        String[] labels = {"A级", "B级", "C级", "D级", "E级"};
        List<Map<String, Object>> options = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> option = new HashMap<>(3);
            option.put("label", labels[i]);
            option.put("value", String.valueOf(i + 1));
            option.put("disabled", null);
            options.add(option);
        }
        propMap.put("options", options);
        if (!"1".equals(propMap.get("defaultValue"))) {
            propMap.put("defaultValue", "1");
        }
        blob.setProp(JSON.toJSONString(propMap));
        moduleFieldBlobMapper.updateById(blob);
    }

    /**
     * 删除客户类型、线上来源详情两个字段（行 + blob）。
     */
    private void deleteCustomerFields(Map<String, ModuleField> fieldMap) {
        List<String> ids = new ArrayList<>(2);
        for (String key : List.of("customerType", "customerOnlineSource")) {
            ModuleField field = fieldMap.get(key);
            if (field != null) {
                ids.add(field.getId());
            }
        }
        if (!ids.isEmpty()) {
            extModuleFieldMapper.deleteByIds(ids);
            extModuleFieldMapper.deletePropByIds(ids);
        }
    }

    /**
     * 为指定目标表单的「线索来源」联动场景回填字段映射（按租户解析字段 id）。
     *
     * @param targetFormKey 目标表单 key（customer / opportunity）
     * @param scenarioKey   联动场景 key
     * @param mapping       来源线索字段 internalKey -> 目标字段 internalKey
     */
    @SuppressWarnings("unchecked")
    private void backfillLinkScenario(String targetFormKey, String scenarioKey, Map<String, String> mapping) {
        LambdaQueryWrapper<ModuleForm> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(ModuleForm::getFormKey, targetFormKey);
        List<ModuleForm> targetForms = moduleFormMapper.selectListByLambda(targetWrapper);
        if (CollectionUtils.isEmpty(targetForms)) {
            return;
        }
        List<String> orgIds = targetForms.stream().map(ModuleForm::getOrganizationId).distinct().toList();

        LambdaQueryWrapper<ModuleForm> clueWrapper = new LambdaQueryWrapper<>();
        clueWrapper.eq(ModuleForm::getFormKey, FormKey.CLUE.getKey());
        clueWrapper.in(ModuleForm::getOrganizationId, orgIds);
        List<ModuleForm> clueForms = moduleFormMapper.selectListByLambda(clueWrapper);
        Map<String, String> clueFormIdByOrg = clueForms.stream()
                .collect(Collectors.toMap(ModuleForm::getOrganizationId, ModuleForm::getId, (a, b) -> a));

        List<String> internalKeys = new ArrayList<>(mapping.size() * 2);
        internalKeys.addAll(mapping.keySet());
        internalKeys.addAll(mapping.values());
        List<String> formIds = new ArrayList<>(targetForms.size() + clueForms.size());
        targetForms.forEach(form -> formIds.add(form.getId()));
        clueForms.forEach(form -> formIds.add(form.getId()));
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getFormId, formIds);
        fieldWrapper.in(ModuleField::getInternalKey, internalKeys);
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        Map<String, Map<String, String>> fieldIdMap = new HashMap<>();
        for (ModuleField field : fields) {
            fieldIdMap.computeIfAbsent(field.getFormId(), k -> new HashMap<>(8))
                    .put(field.getInternalKey(), field.getId());
        }

        for (ModuleForm targetForm : targetForms) {
            Map<String, String> targetFieldIds = fieldIdMap.get(targetForm.getId());
            Map<String, String> clueFieldIds = fieldIdMap.get(clueFormIdByOrg.get(targetForm.getOrganizationId()));
            if (targetFieldIds == null || clueFieldIds == null) {
                continue;
            }
            List<LinkField> linkFields = new ArrayList<>(mapping.size());
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String sourceId = clueFieldIds.get(entry.getKey());
                String targetId = targetFieldIds.get(entry.getValue());
                if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(targetId)) {
                    continue;
                }
                LinkField linkField = new LinkField();
                linkField.setCurrent(targetId);
                linkField.setLink(sourceId);
                linkField.setEnable(true);
                linkFields.add(linkField);
            }
            if (linkFields.isEmpty()) {
                continue;
            }
            ModuleFormBlob formBlob = moduleFormBlobMapper.selectByPrimaryKey(targetForm.getId());
            if (formBlob == null || StringUtils.isBlank(formBlob.getProp())) {
                continue;
            }
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            Map<String, Object> linkProp = (Map<String, Object>) propMap.get("linkProp");
            if (linkProp == null) {
                linkProp = new HashMap<>(2);
            }
            // 仅覆盖「线索」来源的场景，保留其他来源（如商机表单的 customer 场景）
            linkProp.put(FormKey.CLUE.getKey(), List.of(
                    LinkScenario.builder().key(scenarioKey).linkFields(linkFields).build()));
            propMap.put("linkProp", linkProp);
            formBlob.setProp(JSON.toJSONString(propMap));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    /**
     * 将指定「来源」字段改为输入框，并先把已有下拉选项码回填为中文文本。
     */
    @SuppressWarnings("unchecked")
    private void backfillSourceInput(String formKey, String sourceKey, String valueTable) {
        LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
        formWrapper.eq(ModuleForm::getFormKey, formKey);
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            return;
        }
        List<String> formIds = forms.stream().map(ModuleForm::getId).toList();
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getFormId, formIds);
        fieldWrapper.eq(ModuleField::getInternalKey, sourceKey);
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        for (ModuleField field : fields) {
            // 改输入框前，先按旧选项把已存在的选项码回填成文本
            ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(field.getId());
            if (blob != null && StringUtils.isNotBlank(blob.getProp())) {
                Map<String, Object> propMap = JSON.parseMap(blob.getProp());
                Object optionsObj = propMap.get("options");
                if (optionsObj instanceof List<?> options && !options.isEmpty()) {
                    for (Object opt : options) {
                        if (!(opt instanceof Map<?, ?> option)) {
                            continue;
                        }
                        Object value = option.get("value");
                        Object label = option.get("label");
                        if (value == null || label == null) {
                            continue;
                        }
                        extModuleFieldMapper.updateFieldValueCodeToText(valueTable, field.getId(), value.toString(), label.toString());
                    }
                }
            }
            convertToInput(field);
        }
    }

    /**
     * 删除线索表单的「线上来源详情」字段（其显隐由「线索来源=线上」触发，改为输入框后不再需要）。
     */
    private void deleteClueOnlineSourceFields() {
        LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
        formWrapper.eq(ModuleForm::getFormKey, FormKey.CLUE.getKey());
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            return;
        }
        List<String> formIds = forms.stream().map(ModuleForm::getId).toList();
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getFormId, formIds);
        fieldWrapper.eq(ModuleField::getInternalKey, "clueOnlineSource");
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        List<String> ids = fields.stream().map(ModuleField::getId).toList();
        if (!ids.isEmpty()) {
            extModuleFieldMapper.deleteByIds(ids);
            extModuleFieldMapper.deletePropByIds(ids);
        }
    }

    /**
     * 重映射属性 JSON 内的 id 引用（雪花 id 全局唯一，文本替换安全）。
     */
    private String remapIds(String prop, Map<String, String> idMap) {
        if (StringUtils.isEmpty(prop) || idMap.isEmpty()) {
            return prop;
        }
        String result = prop;
        for (Map.Entry<String, String> entry : idMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 初始化升级表单
     */
    public void initUpgradeForm() {
        List<String> allKeys = FormKey.allKeys();
        LambdaQueryWrapper<ModuleForm> moduleFormWrapper = new LambdaQueryWrapper<>();
        moduleFormWrapper.in(ModuleForm::getFormKey, allKeys);
        List<ModuleForm> oldForms = moduleFormMapper.selectListByLambda(moduleFormWrapper);
        allKeys.removeAll(oldForms.stream().map(ModuleForm::getFormKey).toList());
        if (CollectionUtils.isEmpty(allKeys)) {
            // 初始化完成, 无升级表单.
            return;
        }
        initFormAndFields(allKeys);
    }

    /**
     * 初始化线索转联系人表单联动规则
     */
    @SuppressWarnings("unchecked")
    public void initContactFormLinkRules() {
        // 加载初始化的字段信息
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getInternalKey, List.of("contactName", "contactPhone", "clueContactName", "clueContactPhone"));
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        if (CollectionUtils.isEmpty(fields) || fields.size() < 4) {
            log.error("未找到对应的内置字段，无法初始化联动规则");
            return;
        }
        Map<String, String> fieldMap = fields.stream().collect(Collectors.toMap(ModuleField::getInternalKey, ModuleField::getId));
        // 构建联动规则
        LinkField contactNameLink = new LinkField();
        contactNameLink.setCurrent(fieldMap.get("contactName"));
        contactNameLink.setLink(fieldMap.get("clueContactName"));
        contactNameLink.setEnable(true);
        LinkField contactPhoneLink = new LinkField();
        contactPhoneLink.setCurrent(fieldMap.get("contactPhone"));
        contactPhoneLink.setLink(fieldMap.get("clueContactPhone"));
        contactPhoneLink.setEnable(true);
        // 更新表单属性
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.CONTACT.getKey());
        ModuleForm contactForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob formBlob = moduleFormBlobMapper.selectByPrimaryKey(contactForm.getId());
        Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
        List<LinkScenario> contactLinkProp = List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_CONTACT.name())
                .linkFields(List.of(contactNameLink, contactPhoneLink)).build());
        propMap.put("linkProp", Map.of(FormKey.CLUE.getKey(), contactLinkProp));
        formBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(formBlob);
    }

    /**
     * 初始化订单(合同)联动规则
     */
    @SuppressWarnings("unchecked")
    public void initContractToOrderLinkScenario() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.ORDER.getKey());
        ModuleForm orderForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob formBlob = moduleFormBlobMapper.selectByPrimaryKey(orderForm.getId());
        Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
        propMap.put("linkProp", Map.of(FormKey.CONTRACT.getKey(), List.of(
                LinkScenario.builder().key(LinkScenarioKey.CONTRACT_TO_ORDER.name()).linkFields(new ArrayList<>()))));
        formBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(formBlob);
    }

    @SuppressWarnings("unchecked")
    public void initExtFieldsByVer(String version) {
        try {
            List<ModuleField> fields = new ArrayList<>();
            List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
            Map<String, List<Map<String, Object>>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
            fieldMap.forEach((formKey, formFields) -> {
                boolean existExtVerField = formFields.stream().anyMatch(f -> f.containsKey(UPGRADE_EXT_FIELD) && Strings.CS.equals(version, f.get(UPGRADE_EXT_FIELD).toString()));
                if (!existExtVerField) {
                    return;
                }
                ModuleForm example = new ModuleForm();
                example.setFormKey(formKey);
                ModuleForm form = moduleFormMapper.selectOne(example);
                if (form == null) {
                    log.error("未找到表单 {}, 无法初始化扩展字段", formKey);
                    return;
                }
                Long maxPos = extModuleFieldMapper.getMaxFieldPosByFormId(form.getId());
                List<Map<String, Object>> extFields = formFields.stream().filter(f -> f.containsKey(UPGRADE_EXT_FIELD) && Strings.CS.equals(version, f.get(UPGRADE_EXT_FIELD).toString())).toList();
                AtomicLong pos = new AtomicLong(maxPos + 1);
                extFields.forEach(initField -> {
                    ModuleField field = supplyFieldInfo(initField, form.getId(), pos.getAndIncrement(), new HashMap<>(2));
                    initField.put("id", field.getId());
                    fields.add(field);
                    ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
                    fieldBlob.setId(field.getId());
                    fieldBlob.setProp(JSON.toJSONString(initField));
                    fieldBlobs.add(fieldBlob);
                });
            });
            if (CollectionUtils.isNotEmpty(fields)) {
                moduleFieldMapper.batchInsert(fields);
            }
            if (CollectionUtils.isNotEmpty(fieldBlobs)) {
                moduleFieldBlobMapper.batchInsert(fieldBlobs);
            }
        } catch (Exception e) {
            log.error("表单扩展字段初始化失败", e);
            throw new GenericException("表单扩展字段初始化失败", e);
        }
    }

    /**
     * 表单及字段初始化 (升级)
     *
     * @param initKeys 初始化Key集合
     */
    private void initFormAndFields(List<String> initKeys) {
        Map<String, String> formKeyMap = new HashMap<>(FormKey.values().length);
        List<ModuleForm> forms = new ArrayList<>();
        List<ModuleFormBlob> formBlobs = new ArrayList<>();
        initKeys.forEach(formKey -> {
            ModuleForm form = new ModuleForm();
            form.setId(IDGenerator.nextStr());
            form.setFormKey(formKey);
            form.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            form.setCreateUser(InternalUser.ADMIN.getValue());
            form.setCreateTime(System.currentTimeMillis());
            form.setUpdateUser(InternalUser.ADMIN.getValue());
            form.setUpdateTime(System.currentTimeMillis());
            forms.add(form);
            formKeyMap.put(formKey, form.getId());
            ModuleFormBlob formBlob = new ModuleFormBlob();
            formBlob.setId(form.getId());
            try {
                FormProp formProp = JSON.parseObject(formResource.getInputStream(), FormProp.class);
                formBlob.setProp(JSON.toJSONString(formProp));
            } catch (IOException e) {
                throw new GenericException("表单属性初始化失败", e);
            }
            formBlobs.add(formBlob);
        });
        moduleFormMapper.batchInsert(forms);
        moduleFormBlobMapper.batchInsert(formBlobs);
        // init form fields
        initFormFields(formKeyMap);
    }

    /**
     * 字段初始化 (静态json文件)
     *
     * @param formKeyMap 表单Key映射
     */
    @SuppressWarnings("unchecked")
    public void initFormFields(Map<String, String> formKeyMap) {
        List<ModuleField> fields = new ArrayList<>();
        List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
        try {
            Map<String, List<Map<String, Object>>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
            formKeyMap.keySet().forEach(key -> {
                String formId = formKeyMap.get(key);
                List<Map<String, Object>> initFields = fieldMap.get(key);
                AtomicLong pos = new AtomicLong(1L);
                // 显隐规则Key-ID映射
                Map<String, String> controlKeyPreMap = new HashMap<>(2);
                initFields.forEach(initField -> {
                    if (initField.containsKey(UPGRADE_EXT_FIELD)) {
                        return;
                    }
                    ModuleField field = supplyFieldInfo(initField, formId, pos.getAndIncrement(), controlKeyPreMap);
                    initField.put("id", field.getId());
                    fields.add(field);
                    if (initField.containsKey(CONTROL_RULES_KEY)) {
                        List<ControlRuleProp> controlRules = JSON.parseArray(JSON.toJSONString(initField.get(CONTROL_RULES_KEY)), ControlRuleProp.class);
                        controlRules.forEach(controlRule -> {
                            List<String> showFieldIds = new ArrayList<>();
                            controlRule.getFieldIds().forEach(fieldKey -> {
                                if (!controlKeyPreMap.containsKey(fieldKey)) {
                                    controlKeyPreMap.put(fieldKey, IDGenerator.nextStr());
                                }
                                showFieldIds.add(controlKeyPreMap.get(fieldKey));
                            });
                            controlRule.setFieldIds(showFieldIds);
                        });
                        initField.put(CONTROL_RULES_KEY, controlRules);
                    }
                    handleShowFieldsInit(initField, fields);
                    if (initField.containsKey(SUB_FIELDS)) {
                        List<BaseField> subFields = JSON.parseArray(JSON.toJSONString(initField.get(SUB_FIELDS)), BaseField.class);
                        subFields.forEach(subField -> subField.setId(IDGenerator.nextStr()));
                        initField.put(SUB_FIELDS, subFields);
                    }
                    ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
                    fieldBlob.setId(field.getId());
                    fieldBlob.setProp(JSON.toJSONString(initField));
                    fieldBlobs.add(fieldBlob);
                });
            });
            moduleFieldMapper.batchInsert(fields);
            moduleFieldBlobMapper.batchInsert(fieldBlobs);
        } catch (Exception e) {
            log.error("表单字段初始化失败", e);
            throw new GenericException("表单字段初始化失败", e);
        }
    }

    /**
     * 处理显示字段初始化
     *
     * @param initField  初始化字段
     * @param initFields 如果 initForm 初始化，数据库没有数据，需要从 initFields 中获取
     */
    @SuppressWarnings("unchecked")
    private void handleShowFieldsInit(Map<String, Object> initField, List<ModuleField> initFields) {
        if (initField.containsKey(SHOW_FIELD_KEY)) {
            List<String> showFieldKeys = (List<String>) initField.get(SHOW_FIELD_KEY);
            List<ModuleField> showFields = moduleFieldService.selectFieldsByInternalKeys(showFieldKeys);

            if (CollectionUtils.isEmpty(showFields)) {
                // initForm 初始化，数据库没有数据，需要从 initFields 中获取
                showFields = initFields.stream()
                        .filter(f -> showFieldKeys.contains(f.getInternalKey()))
                        .collect(Collectors.toList());
            }

            if (CollectionUtils.isNotEmpty(showFieldKeys)) {
                Set<String> internalKeys = showFields.stream()
                        .map(ModuleField::getInternalKey)
                        .collect(Collectors.toSet());

                // 添加表单字段
                List<String> showFieldResult = new ArrayList<>(showFields.stream().map(ModuleField::getId).toList());
                // 添加表单中没有的系统字段
                List<String> systemFieldKeys = showFieldKeys.stream()
                        .filter(fieldKey -> !internalKeys.contains(fieldKey))
                        .toList();
                showFieldResult.addAll(systemFieldKeys);
                initField.put(SHOW_FIELD_KEY, showFieldResult);
            }
        }
    }


    /**
     * 组装字段基础信息
     *
     * @param fieldMap         字段集合
     * @param formId           表单ID
     * @param pos              字段位置
     * @param controlKeyPreMap 显隐规则Key-ID映射
     * @return 字段
     */
    private ModuleField supplyFieldInfo(Map<String, Object> fieldMap, String formId, Long pos, Map<String, String> controlKeyPreMap) {
        ModuleField field = new ModuleField();
        field.setInternalKey(fieldMap.get("internalKey").toString());
        field.setId(controlKeyPreMap.containsKey(field.getInternalKey()) ? controlKeyPreMap.get(field.getInternalKey()) : IDGenerator.nextStr());
        field.setFormId(formId);
        field.setType(fieldMap.get("type").toString());
        field.setName(fieldMap.get("name").toString());
        field.setMobile((Boolean) fieldMap.getOrDefault("mobile", false));
        field.setPos(pos);
        field.setCreateTime(System.currentTimeMillis());
        field.setCreateUser(InternalUser.ADMIN.getValue());
        field.setUpdateTime(System.currentTimeMillis());
        field.setUpdateUser(InternalUser.ADMIN.getValue());
        return field;
    }

    /**
     * 表单联动处理(旧数据)
     */
    @SuppressWarnings("unchecked")
    public void modifyFormLinkProp() {
        List<ModuleForm> moduleForms = moduleFormMapper.selectAll(null);
        List<String> formIds = moduleForms.stream().map(ModuleForm::getId).toList();
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formIds);
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            Object linkProp = propMap.get("linkProp");
            if (linkProp == null) {
                continue;
            }
            Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
            if (linkPropMap.containsKey("formKey") && linkPropMap.containsKey("linkFields")) {
                Map<String, List<LinkField>> dataMap = new HashMap<>(2);
                String formKey = linkPropMap.get("formKey").toString();
                List<LinkField> linkFields = (List<LinkField>) linkPropMap.get("linkFields");
                dataMap.put(formKey, linkFields);
                propMap.put("linkProp", dataMap);
                formBlob.setProp(JSON.toJSONString(propMap));
                moduleFormBlobMapper.updateById(formBlob);
            }
        }
    }

    /**
     * 处理表单联动的旧数据&&支持多场景 (客户&商机&记录)
     */
    @SuppressWarnings("unchecked")
    public void processOldLinkData() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ModuleForm::getFormKey, List.of(FormKey.CUSTOMER.getKey(), FormKey.OPPORTUNITY.getKey()));
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(wrapper);
        Map<String, String> formKeyMap = forms.stream().collect(Collectors.toMap(ModuleForm::getId, ModuleForm::getFormKey));
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formKeyMap.keySet().stream().toList());
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            Object linkProp = propMap.get("linkProp");
            Map<String, List<LinkScenario>> dataMap = new HashMap<>(2);
            String formKey = formKeyMap.get(formBlob.getId());
            if (linkProp == null) {
                if (Strings.CS.equals(formKey, FormKey.CUSTOMER.getKey())) {
                    dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_CUSTOMER.name()).linkFields(new ArrayList<>()).build()));
                } else if (Strings.CS.equals(formKey, FormKey.OPPORTUNITY.getKey())) {
                    dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_OPPORTUNITY.name()).linkFields(new ArrayList<>()).build()));
                    dataMap.put(FormKey.CUSTOMER.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CUSTOMER_TO_OPPORTUNITY.name()).linkFields(new ArrayList<>()).build()));
                }
            } else {
                Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
                for (Map.Entry<String, Object> entry : linkPropMap.entrySet()) {
                    if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null || !(entry.getValue() instanceof List)) {
                        continue;
                    }
                    List<Map<String, Object>> fields = (List<Map<String, Object>>) entry.getValue();
                    List<LinkField> fieldList = fields.stream().map(field -> {
                        field.put("enable", true);
                        LinkField linkField = new LinkField();
                        try {
                            org.apache.commons.beanutils.BeanUtils.populate(linkField, field);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            log.error("Populate old link field error", e);
                        }
                        return linkField;
                    }).toList();
                    String scenarioKey = (Strings.CS.equals(formKey, FormKey.CUSTOMER.getKey()) && Strings.CS.equals(entry.getKey(), FormKey.CLUE.getKey()) ?
                            LinkScenarioKey.CLUE_TO_CUSTOMER.name() :
                            (Strings.CS.equals(formKey, FormKey.OPPORTUNITY.getKey()) && Strings.CS.equals(entry.getKey(), FormKey.CLUE.getKey()) ?
                                    LinkScenarioKey.CLUE_TO_OPPORTUNITY.name() : LinkScenarioKey.CUSTOMER_TO_OPPORTUNITY.name()));
                    LinkScenario linkScenario = LinkScenario.builder().key(scenarioKey).linkFields(fieldList).build();
                    dataMap.put(entry.getKey(), List.of(linkScenario));
                }
            }
            propMap.put("linkProp", dataMap);
            formBlob.setProp(JSON.toJSONString(propMap));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    /**
     * 初始化跟进记录表单联动场景
     */
    @SuppressWarnings("unchecked")
    public void initFormScenarioProp() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.FOLLOW_RECORD.getKey());
        ModuleForm recordForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob recordFormBlob = moduleFormBlobMapper.selectByPrimaryKey(recordForm.getId());
        Map<String, Object> propMap = JSON.parseMap(recordFormBlob.getProp());
        Object linkProp = propMap.get("linkProp");
        Map<String, List<LinkScenario>> dataMap = new HashMap<>(4);
        dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        dataMap.put(FormKey.CUSTOMER.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CUSTOMER_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        dataMap.put(FormKey.OPPORTUNITY.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.OPPORTUNITY_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        if (linkProp != null) {
            Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
            List<Map<String, Object>> fields = (List<Map<String, Object>>) linkPropMap.get(FormKey.FOLLOW_PLAN.getKey());
            List<LinkField> fieldList = fields.stream().map(field -> {
                LinkField linkField = JSON.parseObject(JSON.toJSONString(field), LinkField.class);
                linkField.setEnable(true);
                return linkField;
            }).toList();
            dataMap.put(FormKey.FOLLOW_PLAN.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.PLAN_TO_RECORD.name()).linkFields(fieldList).build()));
        } else {
            dataMap.put(FormKey.FOLLOW_PLAN.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.PLAN_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        }
        propMap.put("linkProp", dataMap);
        recordFormBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(recordFormBlob);
    }

    /**
     * 初始化发票表单联动场景
     */
    @SuppressWarnings("unchecked")
    public void initInvoiceFormScenarioProp() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.INVOICE.getKey());
        ModuleForm invoiceForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob invoiceFormBlob = moduleFormBlobMapper.selectByPrimaryKey(invoiceForm.getId());
        Map<String, Object> propMap = JSON.parseMap(invoiceFormBlob.getProp());
        List<LinkScenario> contractLinkProp = List.of(LinkScenario.builder().key(LinkScenarioKey.CONTRACT_TO_INVOICE.name()).linkFields(List.of()).build());
        propMap.put("linkProp", Map.of(FormKey.CONTRACT.getKey(), contractLinkProp));
        invoiceFormBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(invoiceFormBlob);
    }

    /**
     * 初始化订单表单联动场景
     */
    @SuppressWarnings("unchecked")
    public void initOrderFormScenarioProp() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.ORDER.getKey());
        ModuleForm orderForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob orderFormBlob = moduleFormBlobMapper.selectByPrimaryKey(orderForm.getId());
        Map<String, Object> propMap = JSON.parseMap(orderFormBlob.getProp());
        List<LinkScenario> contractLinkProp = List.of(LinkScenario.builder().key(LinkScenarioKey.CONTRACT_TO_ORDER.name()).linkFields(List.of()).build());
        propMap.put("linkProp", Map.of(FormKey.CONTRACT.getKey(), contractLinkProp));
        orderFormBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(orderFormBlob);
    }

    /**
     * 表单属性处理(视图)
     */
    @SuppressWarnings("unchecked")
    public void modifyFormProp() {
        List<ModuleForm> moduleForms = moduleFormMapper.selectAll(null);
        List<String> formIds = moduleForms.stream().map(ModuleForm::getId).toList();
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formIds);
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            if (propMap.containsKey("viewSize")) {
                continue;
            }
            propMap.put("viewSize", "large");
            formBlob.setProp(JSON.toJSONString(propMap));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    @SuppressWarnings("unchecked")
    public void modifyFieldMobile() {
        List<ModuleField> moduleFields = moduleFieldMapper.selectAll(null);
        List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
        List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectByIds(fieldIds);
        for (ModuleFieldBlob fieldBlob : moduleFieldBlobs) {
            Map<String, Object> propMap = JSON.parseMap(fieldBlob.getProp());
            propMap.put("mobile", true);
            fieldBlob.setProp(JSON.toJSONString(propMap));
            moduleFieldBlobMapper.updateById(fieldBlob);
        }
        extModuleFieldMapper.batchUpdateMobile(fieldIds, true);
    }
}
