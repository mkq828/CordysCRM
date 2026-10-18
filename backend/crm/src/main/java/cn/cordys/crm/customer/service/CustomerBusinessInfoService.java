package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ThirdConfigTypeConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.search.DeepSeekSearchClient;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.dto.response.BusinessInfoFetchResponse;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.QccThirdConfigRequest;
import cn.cordys.crm.integration.qcc.client.QccClient;
import cn.cordys.crm.integration.qcc.dto.InfoData;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.dto.field.DateTimeField;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.crm.system.service.IntegrationConfigService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 客户「工商信息」获取：企查查核验为主，联网检索 + 模型提取兜底，结果只补不删地写入客户自定义字段。
 */
@Service
@Slf4j
public class CustomerBusinessInfoService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 要求模型严格返回 JSON，字段名即客户工商信息自定义字段 internalKey，便于直接落库 */
    private static final String SYSTEM_PROMPT = """
            你是企业工商信息提取助手。根据下面联网检索到的企业公开信息，提取该企业的工商登记信息，用 ```json 代码块输出严格 JSON（不要任何多余文字），字段如下：
            {
              "customerCreditCode": "统一社会信用代码，查不到填空串",
              "customerLegalPerson": "法定代表人，查不到填空串",
              "customerBusinessStatus": "经营状态，从 在营/存续、注销、吊销、迁出、停业、清算 中选一个，查不到填空串",
              "customerRegisteredCapital": "注册资本，含单位如 '500万人民币'，查不到填空串",
              "customerTaxNo": "纳税人识别号，查不到填空串",
              "customerRegAddress": "注册地址，查不到填空串",
              "customerContactPhone": "联系电话，查不到填空串",
              "customerEstablishDate": "成立日期，格式 yyyy-MM-dd，查不到填空串",
              "customerBusinessScope": "经营业务范围，原文或摘要，查不到填空串",
              "customerCompanyType": "企业类型，从 有限责任公司、股份有限公司、合伙企业、个人独资企业、个体工商户、外商投资企业、国有企业、集体企业、其他 中选一个，查不到填空串",
              "customerPaidInCapital": "实缴资本，含单位，查不到填空串",
              "customerBusinessTerm": "营业期限，如 '长期' 或起止日期，查不到填空串",
              "customerRegAuthority": "登记机关，查不到填空串"
            }
            只输出 JSON 代码块，不要其他内容。
            """;

    /** 客户工商信息自定义字段 internalKey → 展示名，同时作为可落库字段白名单 */
    private static final Map<String, String> FIELD_NAMES = new LinkedHashMap<>();

    static {
        FIELD_NAMES.put("customerBusinessStatus", "企业经营状态");
        FIELD_NAMES.put("customerCreditCode", "统一社会信用代码");
        FIELD_NAMES.put("customerEstablishDate", "成立日期");
        FIELD_NAMES.put("customerLegalPerson", "法定代表人");
        FIELD_NAMES.put("customerContactPhone", "联系电话");
        FIELD_NAMES.put("customerRegisteredCapital", "注册资本");
        FIELD_NAMES.put("customerPaidInCapital", "实缴资本");
        FIELD_NAMES.put("customerTaxNo", "纳税人识别号");
        FIELD_NAMES.put("customerCompanyType", "企业类型");
        FIELD_NAMES.put("customerBusinessTerm", "营业期限");
        FIELD_NAMES.put("customerRegAuthority", "登记机关");
        FIELD_NAMES.put("customerRegAddress", "注册地址");
        FIELD_NAMES.put("customerBusinessScope", "经营业务范围");
    }

    private static final List<String> BUSINESS_STATUS_OPTIONS =
            List.of("在营/存续", "注销", "吊销", "迁出", "停业", "清算");
    private static final List<String> COMPANY_TYPE_OPTIONS =
            List.of("有限责任公司", "股份有限公司", "合伙企业", "个人独资企业", "个体工商户",
                    "外商投资企业", "国有企业", "集体企业", "其他");

    @Resource
    private QccClient qccClient;
    @Resource
    private IntegrationConfigService integrationConfigService;
    @Resource
    private DeepSeekSearchClient deepSeekSearchClient;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private EditionService editionService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private CustomerFieldService customerFieldService;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Parameter> parameterMapper;

    /**
     * 获取客户工商信息并写入自定义字段，返回已填充字段与数据来源。
     */
    public BusinessInfoFetchResponse fetchAndFill(String customerId, String userId, String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_CUSTOMER_BUSINESS)) {
            throw new GenericException("当前版本未开通「获取工商信息」功能");
        }
        Customer customer = customerMapper.selectByPrimaryKey(customerId);
        if (customer == null || !StringUtils.equals(customer.getOrganizationId(), orgId)) {
            throw new GenericException("客户不存在");
        }
        String name = customer.getName();
        if (StringUtils.isBlank(name)) {
            throw new GenericException("请先填写客户名称");
        }

        Map<String, String> values = new LinkedHashMap<>();
        boolean qccHit = false;
        InfoData qcc = queryQcc(name, orgId);
        if (qcc != null) {
            qccHit = true;
            fillFromQcc(qcc, values);
        }
        int qccFilled = values.size();
        // 联网兜底：补齐企查查拿不到或未覆盖的字段（模型按白名单只回填仍空缺项）
        fillFromWebSearch(name, orgId, userId, values);
        boolean webFilled = values.size() > qccFilled;

        BusinessInfoFetchResponse response = new BusinessInfoFetchResponse();
        response.setFilled(writeFields(customer, orgId, userId, values));
        response.setSource(qccHit && webFilled ? "mixed" : qccHit ? "qcc" : "web");
        return response;
    }

    /** 企查查核验：未配置、关闭或查无此企业时静默返回 null，交给联网兜底 */
    private InfoData queryQcc(String name, String orgId) {
        try {
            ThirdConfigBaseDTO<?> thirdConfig = integrationConfigService.getThirdConfigForPublic(
                    ThirdConfigTypeConstants.QCC.name(), orgId);
            if (!Boolean.TRUE.equals(thirdConfig.getVerify())) {
                return null;
            }
            QccThirdConfigRequest qccConfig = JSON.MAPPER.convertValue(thirdConfig.getConfig(), QccThirdConfigRequest.class);
            if (qccConfig == null || !Boolean.TRUE.equals(qccConfig.getQccEnable())) {
                return null;
            }
            return qccClient.verify(qccConfig, name.trim());
        } catch (GenericException e) {
            log.warn("企查查查询不可用，降级联网兜底, name={}, msg={}", name, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("企查查查询异常，降级联网兜底, name={}", name, e);
            return null;
        }
    }

    private void fillFromQcc(InfoData data, Map<String, String> values) {
        putIfNotBlank(values, "customerCreditCode", data.getCreditCode());
        putIfNotBlank(values, "customerLegalPerson", data.getLegalPerson());
        putIfNotBlank(values, "customerBusinessStatus", normalizeBusinessStatus(data.getStatus()));
        putIfNotBlank(values, "customerRegisteredCapital", data.getRegisterCapi());
        putIfNotBlank(values, "customerTaxNo", data.getTaxNo());
        putIfNotBlank(values, "customerRegAddress", data.getAddress());
        if (data.getContactInfo() != null) {
            putIfNotBlank(values, "customerContactPhone", data.getContactInfo().getTel());
        }
    }

    /** 联网检索 + 模型提取，回填仍空缺的字段；无模型/额度受限/检索失败时静默跳过 */
    private void fillFromWebSearch(String name, String orgId, String userId, Map<String, String> values) {
        if (values.size() == FIELD_NAMES.size()) {
            return;
        }
        List<AdvisorWebSource> sources = deepSeekSearchClient.search(name.trim() + " 工商信息 统一社会信用代码 法定代表人 成立日期 注册资本");
        if (sources.isEmpty()) {
            return;
        }
        String context = buildSearchContext(sources);
        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            return;
        }
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            log.warn("获取工商信息联网兜底跳过（额度受限）, orgId={}, status={}", orgId, quota.getStatus());
            return;
        }
        aiQuotaService.record(orgId, AiQuotaConstant.AI_CUSTOMER_BUSINESS, AiQuotaConstant.SEARCH_MODEL_CODE,
                getSearchTokensPerCall(), 0L, userId);

        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(llmRequest, model);
                List<LlmMessage> messages = List.of(
                        new LlmMessage("system", SYSTEM_PROMPT),
                        new LlmMessage("user", "企业名称：" + name + "\n联网检索到的公开信息：\n" + context));
                llmRequest.setMessages(messages);

                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_CUSTOMER_BUSINESS, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                mergeExtracted(sb.toString(), values);
                return;
            } catch (Exception e) {
                log.warn("获取工商信息模型调用失败，尝试下一个候选, model={}", model.getModelName(), e);
            }
        }
    }

    /** 解析模型 JSON 输出，仅回填白名单内且仍空缺的字段 */
    private void mergeExtracted(String text, Map<String, String> values) {
        String json = extractJson(text);
        if (json == null) {
            return;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            for (String internalKey : FIELD_NAMES.keySet()) {
                JsonNode node = root.get(internalKey);
                if (node == null || node.isNull()) {
                    continue;
                }
                String raw = node.asText().trim();
                if (StringUtils.isBlank(raw)) {
                    continue;
                }
                String normalized = normalizeSelect(internalKey, raw);
                if (normalized == null) {
                    continue;
                }
                putIfNotBlank(values, internalKey, normalized);
            }
        } catch (Exception e) {
            log.warn("获取工商信息模型结果解析失败", e);
        }
    }

    /** 将填充结果写入客户自定义字段（只补不删），返回成功填充的字段展示名 */
    private List<String> writeFields(Customer customer, String orgId, String userId, Map<String, String> values) {
        List<BaseField> allFields = moduleFormService.getAllFields(FormKey.CUSTOMER.getKey(), orgId);
        Map<String, BaseField> fieldByKey = allFields.stream()
                .filter(field -> StringUtils.isNotBlank(field.getInternalKey()))
                .collect(Collectors.toMap(BaseField::getInternalKey, Function.identity(), (a, b) -> a));

        List<BaseModuleFieldValue> moduleFields = new ArrayList<>();
        List<String> filledNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (StringUtils.isBlank(entry.getValue())) {
                continue;
            }
            BaseField field = fieldByKey.get(entry.getKey());
            if (field == null) {
                continue;
            }
            Object converted = convertFieldValue(field, entry.getValue());
            if (converted == null) {
                continue;
            }
            moduleFields.add(new BaseModuleFieldValue(field.getId(), converted));
            filledNames.add(FIELD_NAMES.get(entry.getKey()));
        }
        if (moduleFields.isEmpty()) {
            return List.of();
        }
        List<BaseModuleFieldValue> originFields = customerFieldService.getModuleFieldValuesByResourceId(customer.getId());
        customerFieldService.updateModuleFieldByAgent(customer, originFields, moduleFields, orgId, userId);
        return filledNames;
    }

    /** 业务可读值转字段入库格式：SELECT 标签→选项值，DATE yyyy-MM-dd→毫秒时间戳，其余原样；无法转换返回 null 跳过 */
    private Object convertFieldValue(BaseField field, String value) {
        if (field instanceof SelectField selectField) {
            return selectField.getOptions().stream()
                    .filter(option -> StringUtils.equals(option.getLabel(), value)
                            || StringUtils.equals(String.valueOf(option.getValue()), value))
                    .map(option -> String.valueOf(option.getValue()))
                    .findFirst()
                    .orElse(null);
        }
        if (field instanceof DateTimeField) {
            try {
                return LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                return null;
            }
        }
        return value;
    }

    private String buildSearchContext(List<AdvisorWebSource> sources) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (AdvisorWebSource source : sources) {
            sb.append(index++).append(". ").append(StringUtils.defaultIfBlank(source.getTitle(), "无标题"));
            if (StringUtils.isNotBlank(source.getSnippet())) {
                sb.append("——").append(source.getSnippet());
            }
            if (StringUtils.isNotBlank(source.getContent())) {
                sb.append("\n正文：").append(source.getContent());
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private void putIfNotBlank(Map<String, String> values, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            values.put(key, value.trim());
        }
    }

    /** 下拉字段值归一化到配置选项，无法匹配时返回 null（不落库） */
    private String normalizeSelect(String internalKey, String raw) {
        List<String> options;
        if ("customerBusinessStatus".equals(internalKey)) {
            options = BUSINESS_STATUS_OPTIONS;
        } else if ("customerCompanyType".equals(internalKey)) {
            options = COMPANY_TYPE_OPTIONS;
        } else {
            return raw;
        }
        String v = raw.trim();
        for (String option : options) {
            if (option.equals(v) || v.contains(option)) {
                return option;
            }
        }
        return null;
    }

    /** 企查查经营状态（在业/存续/开业等）归一化到客户字段选项 */
    private String normalizeBusinessStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return null;
        }
        String s = status.trim();
        if (s.contains("注销")) {
            return "注销";
        }
        if (s.contains("吊销")) {
            return "吊销";
        }
        if (s.contains("迁出")) {
            return "迁出";
        }
        if (s.contains("停业")) {
            return "停业";
        }
        if (s.contains("清算")) {
            return "清算";
        }
        if (s.contains("在业") || s.contains("存续") || s.contains("在营") || s.contains("开业") || s.contains("正常")) {
            return "在营/存续";
        }
        return null;
    }

    /** 剥 ```json ... ``` 包裹，取首个 { 到末个 } 的 JSON 片段；无合法片段返回 null */
    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('\n');
            if (start < 0) {
                return null;
            }
            t = t.substring(start + 1);
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
            t = t.trim();
        }
        int begin = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (begin < 0 || end < 0 || end <= begin) {
            return null;
        }
        return t.substring(begin, end + 1);
    }

    private boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }

    /** 1 次联网搜索折算 token 数（可配置，默认 1000），配置非法时回退默认值 */
    private long getSearchTokensPerCall() {
        Parameter parameter = parameterMapper.selectByPrimaryKey(AiQuotaConstant.PARAM_SEARCH_TOKENS_PER_CALL);
        if (parameter == null || StringUtils.isBlank(parameter.getParamValue())) {
            return AiQuotaConstant.DEFAULT_SEARCH_TOKENS_PER_CALL;
        }
        try {
            return Long.parseLong(parameter.getParamValue().trim());
        } catch (NumberFormatException e) {
            return AiQuotaConstant.DEFAULT_SEARCH_TOKENS_PER_CALL;
        }
    }
}
