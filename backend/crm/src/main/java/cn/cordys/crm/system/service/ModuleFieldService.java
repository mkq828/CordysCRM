package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.service.BusinessTitleService;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.DatasourceRefDTO;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.DateTimeField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.request.DatasourceRefQueryRequest;
import cn.cordys.crm.system.dto.request.FieldRepeatCheckRequest;
import cn.cordys.crm.system.dto.response.FieldRepeatCheckResponse;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Service
@Slf4j
public class ModuleFieldService {

    /**
     * 表单表格映射
     */
    private static final Map<String, String> FORM_TABLE = new HashMap<>(8);

	private static final Map<String, Class<?>> SOURCE_REF_CLASS = new HashMap<>();

    static {
        FORM_TABLE.put(FormKey.CLUE.getKey(), "clue");
        FORM_TABLE.put(FormKey.CUSTOMER.getKey(), "customer");
        FORM_TABLE.put(FormKey.CONTACT.getKey(), "customer_contact");
        FORM_TABLE.put(FormKey.OPPORTUNITY.getKey(), "opportunity");
        FORM_TABLE.put(FormKey.QUOTATION.getKey(), "opportunity_quotation");
        FORM_TABLE.put(FormKey.PRODUCT.getKey(), "product");
        FORM_TABLE.put(FormKey.PRICE.getKey(), "product_price");
        FORM_TABLE.put(FormKey.FOLLOW_RECORD.getKey(), "follow_up_record");
        FORM_TABLE.put(FormKey.FOLLOW_PLAN.getKey(), "follow_up_plan");
        FORM_TABLE.put(FormKey.CONTRACT.getKey(), "contract");
        FORM_TABLE.put(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), "contract_payment_plan");
        FORM_TABLE.put(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), "contract_payment_record");
        FORM_TABLE.put(FormKey.INVOICE.getKey(), "contract_invoice");
        FORM_TABLE.put(FormKey.ORDER.getKey(), "sales_order");

		SOURCE_REF_CLASS.put(FieldSourceType.PRODUCT.name(), ProductService.class);
		SOURCE_REF_CLASS.put(FieldSourceType.PRICE.name(), ProductPriceService.class);
		SOURCE_REF_CLASS.put(FieldSourceType.BUSINESS_TITLE.name(), BusinessTitleService.class);
    }

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;
	@Resource
	private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private CommonMapper commonMapper;

    /**
     * 获取不带用户的信息的部门树
     *
     * @return 部门树
     */
    public List<BaseTreeNode> getDeptTree(String orgId) {
        return departmentService.getTree(orgId);
    }

    /**
     * 修改日期时间类型的字段部分属性
     */
    @Transactional(rollbackFor = Exception.class)
    public void modifyDateProp() {
        LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleField::getType, FieldType.DATE_TIME.name());
        List<ModuleField> moduleFields = moduleFieldMapper.selectListByLambda(queryWrapper);
        List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
        if (CollectionUtils.isNotEmpty(fieldIds)) {
            LambdaQueryWrapper<ModuleFieldBlob> blobWrapper = new LambdaQueryWrapper<>();
            blobWrapper.in(ModuleFieldBlob::getId, fieldIds);
            List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectListByLambda(blobWrapper);
            for (ModuleFieldBlob blob : moduleFieldBlobs) {
                DateTimeField dateTimeField = JSON.parseObject(blob.getProp(), DateTimeField.class);
                if (StringUtils.isEmpty(dateTimeField.getDateDefaultType())) {
                    dateTimeField.setDateDefaultType("custom");
                }
                blob.setProp(JSON.toJSONString(dateTimeField));
                moduleFieldBlobMapper.updateById(blob);
            }
        }
    }

    /**
     * 校验字段值是否唯一
     *
     * @param request 请求参数
     * @return 是否唯一
     */
    public FieldRepeatCheckResponse checkRepeat(FieldRepeatCheckRequest request, String currentOrg) {
        ModuleField field = moduleFieldMapper.selectByPrimaryKey(request.getId());
        if (field == null) {
            throw new GenericException(Translator.get("module.field.not_exist"));
        }
        String tableName = FORM_TABLE.get(request.getFormKey());
        if (StringUtils.isBlank(tableName)) {
            tableName = "custom_form_data";
        }
        String value = request.getValue();
        if (Strings.CI.equals(field.getType(), FieldType.PHONE.toString())) {
            value = StringUtils.deleteWhitespace(value);
        }

        BusinessModuleField businessField = BusinessModuleField.ofKey(field.getInternalKey());
        String repeatName;
        if (businessField != null) {
            // 业务字段
            repeatName = commonMapper.checkInternalRepeatName(tableName, businessField.getBusinessKey(), value, currentOrg);
        } else {
            repeatName = commonMapper.checkFieldRepeatName(tableName, tableName + "_field", request.getId(), value, currentOrg);
        }
        boolean repeat = false;
        if (StringUtils.isNotBlank(repeatName)) {
            repeat = Strings.CS.equals(request.getValue(), repeatName);
        }
        return FieldRepeatCheckResponse.builder().name(repeatName).repeat(repeat).build();
    }

	@SuppressWarnings("unchecked")
	public List<DatasourceRefDTO> getSourceRefDetail(DatasourceRefQueryRequest request) {
		List<String> sourceIds = request.getSourceIds().stream().distinct().toList();
		Class<?> sourceClass = SOURCE_REF_CLASS.get(request.getDataSourceType());
		if (sourceClass == null) {
			return new ArrayList<>();
		}

		Object service = CommonBeanFactory.getBean(sourceClass);
		List<DatasourceRefDTO> result = new ArrayList<>();
		try {
			Method executeMethod = sourceClass.getMethod("get", String.class);
			for (String id : sourceIds) {
				Object res = executeMethod.invoke(service, id);
				if (res == null) {
					continue;
				}
				DatasourceRefDTO dto = new DatasourceRefDTO();
				if (Strings.CI.equals(request.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
					BeanUtils.copyBean(dto, res);
				} else {
					dto.setId(id);
					dto.setName(getField(res, "name", String.class));
					dto.setModuleFields((List<BaseModuleFieldValue>) getField(res, "moduleFields", List.class));
					dto.setProducts((List<Map<String, Object>>) getField(res, "products", List.class));
					dto.setOptionMap((Map<String, List<OptionDTO>>) getField(res, "optionMap", Map.class));
					dto.setStatus(getField(res, "status", String.class));

					if (Strings.CI.equals(request.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
						// 产品需要获取系统价格字段
						dto.setPrice(getField(res, "price", BigDecimal.class));
					}
				}
				result.add(dto);
			}
		} catch (Exception e) {
			log.error("获取数据源引用详细失败, {}", e.getMessage());
			return new ArrayList<>();
		}

		return result;
	}

    @Transactional(rollbackFor = Exception.class)
    public void modifyInvoiceShowFields() {
        LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleField::getInternalKey, BusinessModuleField.INVOICE_CONTRACT_ID.getKey());
        ModuleField example = new ModuleField();
        example.setInternalKey(BusinessModuleField.INVOICE_CONTRACT_ID.getKey());
        ModuleField contractField = moduleFieldMapper.selectOne(example);
        if (contractField != null) {
            ModuleFieldBlob contractFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(contractField.getId());
            String prop = contractFieldBlob.getProp();
            DatasourceField datasourceField = JSON.parseObject(prop, DatasourceField.class);
            List<String> showFields = datasourceField.getShowFields();
            if (CollectionUtils.isNotEmpty(showFields)) {
                for (int i = 0; i < showFields.size(); i++) {
                    // 合同总金额字段初始化之后，显示字段替换成字段ID
                    if (Strings.CI.equalsAny(showFields.get(i),
                            "contractProductSumAmount",
                            BusinessModuleField.CONTRACT_TOTAL_AMOUNT.getKey())) {
                        ModuleField contractNameField = selectFieldByInternalKey(BusinessModuleField.CONTRACT_TOTAL_AMOUNT.getKey());
                        if (contractNameField != null) {
                            showFields.set(i, contractNameField.getId());
                            contractFieldBlob.setProp(JSON.toJSONString(datasourceField));
                            moduleFieldBlobMapper.updateById(contractFieldBlob);
                            break;
                        }
                    }
                }
            }
        }
    }

    public List<ModuleField> selectFieldsByInternalKeys(List<String> internalKeys) {
        return moduleFieldMapper.selectListByLambda(new LambdaQueryWrapper<ModuleField>()
                .in(ModuleField::getInternalKey, internalKeys));
    }

    public ModuleField selectFieldByInternalKey(String internalKey) {
        ModuleField field = new ModuleField();
        field.setInternalKey(internalKey);
        return moduleFieldMapper.selectOne(field);
    }

	@SuppressWarnings("unchecked")
	private <T> T getField(Object obj, String fieldName, Class<T> type) {
		Class<?> clazz = obj.getClass();

		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);

				Object value = field.get(obj);
				if (type.isInstance(value)) {
					return (T) value;
				}

				return null;
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			} catch (IllegalAccessException e) {
				return null;
			}
		}

		return null;
	}

    /**
     * 订单模块字段初始化
     */
    @Transactional(rollbackFor = Exception.class)
    public void initOrderFields() {
        initOrderContractFilter();
        initOrderProducts();
    }

    /**
     * 订单产品字段增加金额计算公式，订单增加订单金额计算公式
     */
	@SuppressWarnings("unchecked")
    private void initOrderProducts() {
        LambdaQueryWrapper<ModuleField> productsWrapper = new LambdaQueryWrapper<>();
        productsWrapper.eq(ModuleField::getInternalKey, "orderProducts");
        List<ModuleField> productsFields = moduleFieldMapper.selectListByLambda(productsWrapper);
        for (ModuleField productsField : productsFields) {
            ModuleFieldBlob productsBlob = moduleFieldBlobMapper.selectByPrimaryKey(productsField.getId());
            if (productsBlob == null) {
                continue;
            }
            Map<String, Object> productsFieldMap = JSON.parseMap(productsBlob.getProp());
            List<Map<String, Object>> subFields = (List<Map<String, Object>>) productsFieldMap.get("subFields");
            if (CollectionUtils.isEmpty(subFields)) {
                continue;
            }
            String priceId = null;
            String numberId = null;
            String amountId = null;
            Map<String, Object> amountSubField = null;
            for (Map<String, Object> subField : subFields) {
                String key = (String) subField.get("internalKey");
                if (Strings.CI.equals(key, "orderProductPrice")) {
                    priceId = (String) subField.get("id");
                } else if (Strings.CI.equals(key, "orderProductNumber")) {
                    numberId = (String) subField.get("id");
                } else if (Strings.CI.equals(key, "orderProductAmount")) {
                    amountId = (String) subField.get("id");
                    amountSubField = subField;
                }
            }
            // 金额 = 产品单价 × 数量
            if (amountSubField != null && priceId != null && numberId != null) {
                amountSubField.put("formula", getOrderProductAmountFormula(priceId, numberId));
                // 金额按数值渲染（否则浮点尾差直接以文本展示），保留两位小数与千分位
                amountSubField.put("formulaResultFormat", "number");
                amountSubField.put("decimalPlaces", true);
                amountSubField.put("precision", 2);
                productsBlob.setProp(JSON.toJSONString(productsFieldMap));
                moduleFieldBlobMapper.updateById(productsBlob);
            }
            // 订单金额 = SUM(产品明细.金额)
            if (amountId == null) {
                continue;
            }
            ModuleField orderAmount = selectFieldByFormIdAndInternalKey(productsField.getFormId(), "orderAmount");
            if (orderAmount == null) {
                continue;
            }
            ModuleFieldBlob orderAmountBlob = moduleFieldBlobMapper.selectByPrimaryKey(orderAmount.getId());
            if (orderAmountBlob == null) {
                continue;
            }
            Map<String, Object> orderAmountField = JSON.parseMap(orderAmountBlob.getProp());
            orderAmountField.put("formula", getOrderAmountFormula(productsField.getId() + "." + amountId));
            orderAmountField.put("formulaResultFormat", "number");
            orderAmountField.put("decimalPlaces", true);
            orderAmountField.put("precision", 2);
            orderAmountBlob.setProp(JSON.toJSONString(orderAmountField));
            moduleFieldBlobMapper.updateById(orderAmountBlob);
        }
    }

    /**
     * 合同模块字段初始化：金额 = 产品单价 × 数量；累计金额 = SUM(合同报价信息.金额)。
     * <p>订单模块由 {@link #initOrderProducts()} 生成公式，合同模块此前漏配，导致
     * 新建合同时金额/累计金额无法自动计算。此处遍历所有租户的合同表单补齐公式。</p>
     */
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public void initContractFields() {
        LambdaQueryWrapper<ModuleField> productsWrapper = new LambdaQueryWrapper<>();
        productsWrapper.eq(ModuleField::getInternalKey, "contractProducts");
        List<ModuleField> productsFields = moduleFieldMapper.selectListByLambda(productsWrapper);
        for (ModuleField productsField : productsFields) {
            ModuleFieldBlob productsBlob = moduleFieldBlobMapper.selectByPrimaryKey(productsField.getId());
            if (productsBlob == null) {
                continue;
            }
            Map<String, Object> productsFieldMap = JSON.parseMap(productsBlob.getProp());
            List<Map<String, Object>> subFields = (List<Map<String, Object>>) productsFieldMap.get("subFields");
            if (CollectionUtils.isEmpty(subFields)) {
                continue;
            }
            String amountId = null;
            String numberId = null;
            String sumAmountId = null;
            Map<String, Object> sumAmountSubField = null;
            for (Map<String, Object> subField : subFields) {
                String key = (String) subField.get("internalKey");
                if (Strings.CI.equals(key, "contractProductAmount")) {
                    amountId = (String) subField.get("id");
                } else if (Strings.CI.equals(key, "contractProductNumber")) {
                    numberId = (String) subField.get("id");
                } else if (Strings.CI.equals(key, "contractProductSumAmount")) {
                    sumAmountId = (String) subField.get("id");
                    sumAmountSubField = subField;
                }
            }
            // 金额 = 产品单价 × 数量
            if (sumAmountSubField != null && amountId != null && numberId != null
                    && StringUtils.isEmpty((String) sumAmountSubField.get("formula"))) {
                sumAmountSubField.put("formula", getOrderProductAmountFormula(amountId, numberId));
                sumAmountSubField.put("formulaResultFormat", "number");
                sumAmountSubField.put("decimalPlaces", true);
                sumAmountSubField.put("precision", 2);
                productsBlob.setProp(JSON.toJSONString(productsFieldMap));
                moduleFieldBlobMapper.updateById(productsBlob);
            }
            // 累计金额 = SUM(合同报价信息.金额)
            if (sumAmountId == null) {
                continue;
            }
            ModuleField totalField = selectFieldByFormIdAndInternalKey(productsField.getFormId(), "contractTotalAmount");
            if (totalField == null) {
                continue;
            }
            ModuleFieldBlob totalBlob = moduleFieldBlobMapper.selectByPrimaryKey(totalField.getId());
            if (totalBlob == null) {
                continue;
            }
            Map<String, Object> totalFieldMap = JSON.parseMap(totalBlob.getProp());
            if (StringUtils.isEmpty((String) totalFieldMap.get("formula"))) {
                totalFieldMap.put("formula", getContractTotalAmountFormula(productsField.getId() + "." + sumAmountId));
                totalFieldMap.put("formulaResultFormat", "number");
                totalFieldMap.put("decimalPlaces", true);
                totalFieldMap.put("precision", 2);
                totalBlob.setProp(JSON.toJSONString(totalFieldMap));
                moduleFieldBlobMapper.updateById(totalBlob);
            }
        }
    }

    /**
     * 报价模块字段初始化：金额 = 产品定价 × 折扣 ÷ 10 × (1 + 税点)；累计金额 = SUM(报价产品.金额)。
     * <p>报价产品明细的金额/累计金额为 FORMULA 字段，但此前未生成公式，导致新建报价时
     * 金额无法自动计算。折扣按整数填写（默认 10 原价、9 即 9 折）、税点默认 0（无税），遍历所有租户补齐。</p>
     */
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public void initQuotationFields() {
        LambdaQueryWrapper<ModuleField> productsWrapper = new LambdaQueryWrapper<>();
        productsWrapper.eq(ModuleField::getInternalKey, "quotationProducts");
        List<ModuleField> productsFields = moduleFieldMapper.selectListByLambda(productsWrapper);
        for (ModuleField productsField : productsFields) {
            ModuleFieldBlob productsBlob = moduleFieldBlobMapper.selectByPrimaryKey(productsField.getId());
            if (productsBlob == null) {
                continue;
            }
            Map<String, Object> productsFieldMap = JSON.parseMap(productsBlob.getProp());
            List<Map<String, Object>> subFields = (List<Map<String, Object>>) productsFieldMap.get("subFields");
            if (CollectionUtils.isEmpty(subFields)) {
                continue;
            }
            String priceId = null;
            String discountId = null;
            String taxId = null;
            String amountId = null;
            Map<String, Object> amountSubField = null;
            Map<String, Object> discountSubField = null;
            Map<String, Object> taxSubField = null;
            for (Map<String, Object> subField : subFields) {
                String key = (String) subField.get("internalKey");
                if (Strings.CI.equals(key, "quotationProductAmount")) {
                    priceId = (String) subField.get("id");
                } else if (Strings.CI.equals(key, "quotationDiscount")) {
                    discountId = (String) subField.get("id");
                    discountSubField = subField;
                } else if (Strings.CI.equals(key, "quotationTax")) {
                    taxId = (String) subField.get("id");
                    taxSubField = subField;
                } else if (Strings.CI.equals(key, "quotationAmount")) {
                    amountId = (String) subField.get("id");
                    amountSubField = subField;
                }
            }
            // 折扣默认 10（原价，按整数填写：10=原价、9=9折）、税点默认 0（无税），避免留空导致金额算成 0
            if (discountSubField != null) {
                discountSubField.put("defaultValue", "10");
            }
            if (taxSubField != null) {
                taxSubField.put("defaultValue", "0");
            }
            // 金额 = 产品定价 × 折扣 ÷ 10 × (1 + 税点)，无条件覆盖以迁移旧公式（折扣语义由直接乘数改为 ÷10）
            if (amountSubField != null && priceId != null && discountId != null && taxId != null) {
                amountSubField.put("formula", getQuotationProductAmountFormula(priceId, discountId, taxId));
                // 金额按数值渲染（否则浮点尾差如 22373.999999999996 直接以文本展示），保留两位小数与千分位
                amountSubField.put("formulaResultFormat", "number");
                amountSubField.put("decimalPlaces", true);
                amountSubField.put("precision", 2);
            }
            productsBlob.setProp(JSON.toJSONString(productsFieldMap));
            moduleFieldBlobMapper.updateById(productsBlob);
            // 累计金额 = SUM(报价产品.金额)
            if (amountId == null) {
                continue;
            }
            ModuleField totalField = selectFieldByFormIdAndInternalKey(productsField.getFormId(), "quotationTotalAmount");
            if (totalField == null) {
                continue;
            }
            ModuleFieldBlob totalBlob = moduleFieldBlobMapper.selectByPrimaryKey(totalField.getId());
            if (totalBlob == null) {
                continue;
            }
            Map<String, Object> totalFieldMap = JSON.parseMap(totalBlob.getProp());
            if (StringUtils.isEmpty((String) totalFieldMap.get("formula"))) {
                totalFieldMap.put("formula", getQuotationTotalAmountFormula(productsField.getId() + "." + amountId));
                totalFieldMap.put("formulaResultFormat", "number");
                totalFieldMap.put("decimalPlaces", true);
                totalFieldMap.put("precision", 2);
                totalBlob.setProp(JSON.toJSONString(totalFieldMap));
                moduleFieldBlobMapper.updateById(totalBlob);
            }
        }
    }

    /**
     * 按表单与字段内部标识查询字段。
     */
    private ModuleField selectFieldByFormIdAndInternalKey(String formId, String internalKey) {
        ModuleField field = new ModuleField();
        field.setFormId(formId);
        field.setInternalKey(internalKey);
        return moduleFieldMapper.selectOne(field);
    }

    private String getOrderAmountFormula(String orderAmountFormulaId) {
        return String.format("""
                {
                    "source": "SUM(${%s})",
                    "display": "SUM(产品明细.金额)",
                    "fields": [
                        {
                            "fieldId": "%s",
                            "fieldType": "FORMULA",
                            "numberType": "number"
                        }
                    ],
                    "ir": {
                        "type": "function",
                        "name": "SUM",
                        "args": [
                            {
                                "type": "field",
                                "fieldId": "%s",
                                "name": "产品明细.金额",
                                "fieldType": "FORMULA",
                                "numberType": "number",
                                "startTokenIndex": 2,
                                "endTokenIndex": 2
                            }
                        ]
                    }
                }""", orderAmountFormulaId, orderAmountFormulaId, orderAmountFormulaId);
    }

    private String getOrderProductAmountFormula(String orderProductPriceId, String orderProductNumberId) {
        return String.format("""
                {
                     "source": "${%s} * ${%s}",
                     "display": "产品单价 * 数量",
                     "fields": [
                         {
                             "fieldId": "%s",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number"
                         },
                         {
                             "fieldId": "%s",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number"
                         }
                     ],
                     "ir": {
                         "type": "binary",
                         "operator": "*",
                         "left": {
                             "type": "field",
                             "fieldId": "%s",
                             "name": "产品单价",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number",
                             "startTokenIndex": 0,
                             "endTokenIndex": 0
                         },
                         "right": {
                             "type": "field",
                             "fieldId": "%s",
                             "name": "数量",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number",
                             "startTokenIndex": 2,
                             "endTokenIndex": 2
                         }
                     }
                 }""", orderProductPriceId, orderProductNumberId, orderProductPriceId, orderProductNumberId, orderProductPriceId, orderProductNumberId);
    }

    /**
     * 生成合同累计金额公式：SUM(合同报价信息.金额)。格式与订单累计金额一致。
     */
    private String getContractTotalAmountFormula(String contractTotalAmountFormulaId) {
        return String.format("""
                {
                    "source": "SUM(${%s})",
                    "display": "SUM(合同报价信息.金额)",
                    "fields": [
                        {
                            "fieldId": "%s",
                            "fieldType": "FORMULA",
                            "numberType": "number"
                        }
                    ],
                    "ir": {
                        "type": "function",
                        "name": "SUM",
                        "args": [
                            {
                                "type": "field",
                                "fieldId": "%s",
                                "name": "合同报价信息.金额",
                                "fieldType": "FORMULA",
                                "numberType": "number",
                                "startTokenIndex": 2,
                                "endTokenIndex": 2
                            }
                        ]
                    }
                }""", contractTotalAmountFormulaId, contractTotalAmountFormulaId, contractTotalAmountFormulaId);
    }

    /**
     * 生成报价明细金额公式：产品定价 × 折扣 ÷ 10 × (1 + 税点)。
     * 折扣按整数填写（10=原价、9=9折），税点为 percent（引擎自动 ÷100）。
     */
    private String getQuotationProductAmountFormula(String priceId, String discountId, String taxId) {
        return String.format("""
                {
                     "source": "${%s} * ${%s} / 10 * (1 + ${%s})",
                     "display": "产品定价 * 折扣 / 10 * (1 + 税点)",
                     "fields": [
                         {
                             "fieldId": "%s",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number"
                         },
                         {
                             "fieldId": "%s",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number"
                         },
                         {
                             "fieldId": "%s",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "percent"
                         }
                     ],
                     "ir": {
                         "type": "binary",
                         "operator": "*",
                         "left": {
                             "type": "binary",
                             "operator": "/",
                             "left": {
                                 "type": "binary",
                                 "operator": "*",
                                 "left": {
                                     "type": "field",
                                     "fieldId": "%s",
                                     "name": "产品定价",
                                     "fieldType": "INPUT_NUMBER",
                                     "numberType": "number",
                                     "startTokenIndex": 0,
                                     "endTokenIndex": 0
                                 },
                                 "right": {
                                     "type": "field",
                                     "fieldId": "%s",
                                     "name": "折扣",
                                     "fieldType": "INPUT_NUMBER",
                                     "numberType": "number",
                                     "startTokenIndex": 2,
                                     "endTokenIndex": 2
                                 }
                             },
                             "right": {
                                 "type": "literal",
                                 "value": 10,
                                 "valueType": "number"
                             }
                         },
                         "right": {
                             "type": "binary",
                             "operator": "+",
                             "left": {
                                 "type": "literal",
                                 "value": 1,
                                 "valueType": "number"
                             },
                             "right": {
                                 "type": "field",
                                 "fieldId": "%s",
                                 "name": "税点",
                                 "fieldType": "INPUT_NUMBER",
                                 "numberType": "percent",
                                 "startTokenIndex": 8,
                                 "endTokenIndex": 8
                             }
                         }
                     }
                 }""", priceId, discountId, taxId, priceId, discountId, taxId, priceId, discountId, taxId);
    }

    /**
     * 生成报价累计金额公式：SUM(报价产品.金额)。格式与合同累计金额一致。
     */
    private String getQuotationTotalAmountFormula(String quotationAmountFormulaId) {
        return String.format("""
                {
                    "source": "SUM(${%s})",
                    "display": "SUM(报价产品.金额)",
                    "fields": [
                        {
                            "fieldId": "%s",
                            "fieldType": "FORMULA",
                            "numberType": "number"
                        }
                    ],
                    "ir": {
                        "type": "function",
                        "name": "SUM",
                        "args": [
                            {
                                "type": "field",
                                "fieldId": "%s",
                                "name": "报价产品.金额",
                                "fieldType": "FORMULA",
                                "numberType": "number",
                                "startTokenIndex": 2,
                                "endTokenIndex": 2
                            }
                        ]
                    }
                }""", quotationAmountFormulaId, quotationAmountFormulaId, quotationAmountFormulaId);
    }

    /**
     * 订单合同字段增加订单客户过滤条件
     */
    private void initOrderContractFilter() {
        LambdaQueryWrapper<ModuleField> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(ModuleField::getInternalKey, BusinessModuleField.ORDER_CONTRACT.getKey());
        List<ModuleField> orderContracts = moduleFieldMapper.selectListByLambda(contractWrapper);
        for (ModuleField orderContract : orderContracts) {
            ModuleField orderCustomer = selectFieldByFormIdAndInternalKey(orderContract.getFormId(),
                    BusinessModuleField.ORDER_CUSTOMER.getKey());
            if (orderCustomer == null) {
                continue;
            }
            ModuleFieldBlob orderContractFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(orderContract.getId());
            if (orderContractFieldBlob == null) {
                continue;
            }
            DatasourceField datasourceField = JSON.parseObject(orderContractFieldBlob.getProp(), DatasourceField.class);
            String customerConditionJson = String.format("""
                    {
                      "searchMode": "OR",
                      "conditions": [
                        {
                          "leftFieldId": "customerId",
                          "leftFieldType": "DATA_SOURCE",
                          "operator": "IN",
                          "rightFieldId": "%s",
                          "rightFieldCustom": false,
                          "rightFieldCustomValue": "",
                          "rightFieldType": "INPUT"
                        }
                      ]
                    }""", orderCustomer.getId());
            datasourceField.setCombineSearch(JSON.parseToMap(customerConditionJson));
            orderContractFieldBlob.setProp(JSON.toJSONString(datasourceField));
            moduleFieldBlobMapper.updateById(orderContractFieldBlob);
        }
    }

	/**
	 * 获取指定数据源的子表字段集合 (目前只有价格表类型的子表字段支持引用)
	 *
	 * @param sourceType 数据源类型
	 * @return 子表字段集合
	 */
	@Cacheable(value = "sub_fields_cache", key = "#sourceType + ':' + #orgId", unless = "#result == null")
	public List<BaseField> getSubFieldsBySourceType(String sourceType, String orgId) {
		List<ModuleFieldBlob> subFields = new ArrayList<>();
		if (Strings.CS.equals(sourceType, FieldSourceType.PRICE.name())) {
			subFields = extModuleFieldMapper.getFormSubFields(FormKey.PRICE.getKey(), orgId);
		}
		if (CollectionUtils.isEmpty(subFields)) {
			return new ArrayList<>();
		}
		List<BaseField> subFieldList = new ArrayList<>(subFields.size());
		for (ModuleFieldBlob fieldBlob : subFields) {
			SubField subField = JSON.parseObject(fieldBlob.getProp(), SubField.class);
			subFieldList.addAll(subField.getSubFields());
		}
		return subFieldList;
	}
}
