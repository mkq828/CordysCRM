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
