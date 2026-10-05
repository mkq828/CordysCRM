package cn.cordys.crm.ai.leadcrawl.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ThirdConfigTypeConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.leadcrawl.dto.request.LeadCrawlImportRequest;
import cn.cordys.crm.ai.leadcrawl.dto.response.LeadCrawlEnterpriseResponse;
import cn.cordys.crm.ai.leadcrawl.dto.response.LeadCrawlImportResponse;
import cn.cordys.crm.clue.dto.request.ClueAddRequest;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.QccThirdConfigRequest;
import cn.cordys.crm.integration.qcc.client.QccClient;
import cn.cordys.crm.integration.qcc.domain.QccEnterpriseInfo;
import cn.cordys.crm.integration.qcc.dto.InfoData;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.IntegrationConfigService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 线索爬取（功能 10 第一块）：企查查搜索企业 → 拉取工商详情 → 沉淀平台缓存 → 导入线索池。
 * 本模块不调 LLM、不计量额度，只挂 G3 版本门控（企业版）。
 */
@Service
@Slf4j
public class LeadCrawlService {

    /** 企业信息缓存过期天数系统参数 key */
    private static final String PARAM_CACHE_TTL_DAYS = "ai.qcc.cacheTtlDays";

    /** 缓存默认过期天数 */
    private static final int DEFAULT_CACHE_TTL_DAYS = 90;

    /** 线索来源自定义字段 internalKey */
    private static final String CLUE_SOURCE_INTERNAL_KEY = "clueSource";

    /** 导入线索时的来源标记 */
    private static final String CLUE_SOURCE_LEAD_CRAWL = "线索爬取";

    @Resource
    private BaseMapper<QccEnterpriseInfo> qccEnterpriseInfoMapper;
    @Resource
    private QccClient qccClient;
    @Resource
    private IntegrationConfigService integrationConfigService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ClueService clueService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;

    /**
     * 按关键字搜索企业名单。命中平台缓存的企业直接回带详情（fromCache=true），未命中的只回名称。
     */
    public List<LeadCrawlEnterpriseResponse> search(String orgId, String keyword) {
        QccThirdConfigRequest config = requireConfig(orgId);
        List<String> names = qccClient.fuzzySearch(config, keyword);
        Map<String, QccEnterpriseInfo> cached = findByNames(names);
        List<LeadCrawlEnterpriseResponse> result = new ArrayList<>();
        for (String name : names) {
            QccEnterpriseInfo entity = cached.get(name);
            result.add(toResponse(name, entity, entity != null));
        }
        return result;
    }

    /**
     * 拉取企业详情：先查缓存（未过期直接复用），未命中再调企查查详情核验并写缓存。
     */
    public List<LeadCrawlEnterpriseResponse> fetchDetail(String orgId, List<String> names) {
        List<LeadCrawlEnterpriseResponse> result = new ArrayList<>();
        for (String name : names) {
            DetailHit hit = resolveDetail(orgId, name);
            if (hit != null) {
                result.add(toResponse(name, hit.entity(), hit.fromCache()));
            }
        }
        return result;
    }

    /**
     * 把选中企业导入线索池。逐条导入，单条失败（如联系人电话重复）不影响其余，返回成败统计。
     */
    public LeadCrawlImportResponse importToPool(String orgId, String userId, LeadCrawlImportRequest request) {
        String clueSourceFieldId = resolveClueSourceFieldId(orgId);
        List<String> failed = new ArrayList<>();
        int success = 0;
        for (String name : request.getNames()) {
            try {
                DetailHit hit = resolveDetail(orgId, name);
                if (hit == null) {
                    failed.add(name);
                    continue;
                }
                ClueAddRequest clueRequest = new ClueAddRequest();
                clueRequest.setName(name);
                clueRequest.setPhone(hit.entity().getRegisterPhone());
                if (clueSourceFieldId != null) {
                    clueRequest.setModuleFields(List.of(new BaseModuleFieldValue(clueSourceFieldId, CLUE_SOURCE_LEAD_CRAWL)));
                }
                clueService.add(clueRequest, userId, orgId);
                success++;
            } catch (Exception e) {
                log.warn("导入线索失败, name={}", name, e);
                failed.add(name);
            }
        }
        LeadCrawlImportResponse response = new LeadCrawlImportResponse();
        response.setSuccessCount(success);
        response.setFailedNames(failed);
        return response;
    }

    // ==================== 缓存与企查查调用 ====================

    /** 读取租户企查查配置，未开启或未填 key 时抛可读提示 */
    private QccThirdConfigRequest requireConfig(String orgId) {
        ThirdConfigBaseDTO<?> dto = integrationConfigService.getApplicationConfig(orgId, SessionUtils.getUserId(),
                ThirdConfigTypeConstants.QCC.name());
        QccThirdConfigRequest config = dto == null || dto.getConfig() == null
                ? null : JSON.MAPPER.convertValue(dto.getConfig(), QccThirdConfigRequest.class);
        if (config == null || !Boolean.TRUE.equals(config.getQccEnable())
                || StringUtils.isBlank(config.getQccAddress())
                || StringUtils.isBlank(config.getQccAccessKey())
                || StringUtils.isBlank(config.getQccSecretKey())) {
            throw new GenericException("未配置企查查 API Key，请先在「第三方设置」中开启并填写企查查密钥");
        }
        return config;
    }

    /** 解析单家企业详情：缓存未过期直接复用，否则调企查查并写回缓存 */
    private DetailHit resolveDetail(String orgId, String name) {
        QccEnterpriseInfo cached = findByNames(List.of(name)).get(name);
        if (cached != null && !expired(cached)) {
            return new DetailHit(cached, true);
        }
        QccThirdConfigRequest config = requireConfig(orgId);
        InfoData data = qccClient.verify(config, name);
        if (data == null) {
            return null;
        }
        return new DetailHit(upsert(name, data), false);
    }

    private QccEnterpriseInfo upsert(String name, InfoData data) {
        long now = System.currentTimeMillis();
        QccEnterpriseInfo existing = findByNames(List.of(name)).get(name);
        QccEnterpriseInfo entity = existing == null ? new QccEnterpriseInfo() : existing;
        if (existing == null) {
            entity.setId(IDGenerator.nextStr());
            entity.setName(name);
            entity.setSource("qcc");
            entity.setCreateTime(now);
        }
        entity.setTaxNo(data.getTaxNo());
        entity.setCreditCode(data.getCreditCode());
        entity.setLegalPerson(data.getLegalPerson());
        entity.setStatus(data.getStatus());
        entity.setRegisterCapi(data.getRegisterCapi());
        entity.setPersonScope(data.getPersonScope());
        entity.setScale(data.getScale());
        entity.setIndustry(data.getIndustry() == null ? null : data.getIndustry().getSubIndustry());
        entity.setAreaProvince(data.getArea() == null ? null : data.getArea().getProvince());
        entity.setAreaCity(data.getArea() == null ? null : data.getArea().getCity());
        entity.setRegisterPhone(data.getContactInfo() == null ? null : data.getContactInfo().getTel());
        entity.setAddress(data.getAddress());
        entity.setBankName(data.getBankInfo() == null ? null : data.getBankInfo().getBank());
        entity.setBankAccount(data.getBankInfo() == null ? null : data.getBankInfo().getBankAccount());
        entity.setRegNo(data.getNo());
        entity.setUpdateTime(now);
        if (existing == null) {
            qccEnterpriseInfoMapper.insert(entity);
        } else {
            qccEnterpriseInfoMapper.updateById(entity);
        }
        return entity;
    }

    private Map<String, QccEnterpriseInfo> findByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<QccEnterpriseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(QccEnterpriseInfo::getName, names);
        return qccEnterpriseInfoMapper.selectListByLambda(wrapper).stream()
                .collect(Collectors.toMap(QccEnterpriseInfo::getName, Function.identity(), (a, b) -> a));
    }

    private boolean expired(QccEnterpriseInfo entity) {
        if (entity.getUpdateTime() == null) {
            return true;
        }
        return System.currentTimeMillis() - entity.getUpdateTime() > cacheTtlMs();
    }

    private long cacheTtlMs() {
        int days = DEFAULT_CACHE_TTL_DAYS;
        Parameter parameter = parameterMapper.selectByPrimaryKey(PARAM_CACHE_TTL_DAYS);
        if (parameter != null && StringUtils.isNotBlank(parameter.getParamValue())) {
            try {
                days = Integer.parseInt(parameter.getParamValue().trim());
            } catch (NumberFormatException ignored) {
                // 参数非法时回退默认值
            }
        }
        return days * 24L * 3600 * 1000;
    }

    // ==================== 线索字段与响应组装 ====================

    /** 解析「线索来源」自定义字段的 fieldId，失败返回 null（导入时跳过该字段） */
    private String resolveClueSourceFieldId(String orgId) {
        try {
            return moduleFormService.getAllFields(FormKey.CLUE.getKey(), orgId).stream()
                    .filter(field -> CLUE_SOURCE_INTERNAL_KEY.equals(field.getInternalKey()))
                    .map(BaseField::getId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("解析线索来源字段失败", e);
            return null;
        }
    }

    private LeadCrawlEnterpriseResponse toResponse(String name, QccEnterpriseInfo entity, boolean fromCache) {
        LeadCrawlEnterpriseResponse response = new LeadCrawlEnterpriseResponse();
        response.setName(name);
        response.setFromCache(fromCache);
        if (entity != null) {
            response.setTaxNo(entity.getTaxNo());
            response.setCreditCode(entity.getCreditCode());
            response.setLegalPerson(entity.getLegalPerson());
            response.setStatus(entity.getStatus());
            response.setRegisterCapi(entity.getRegisterCapi());
            response.setPersonScope(entity.getPersonScope());
            response.setScale(entity.getScale());
            response.setIndustry(entity.getIndustry());
            response.setAreaProvince(entity.getAreaProvince());
            response.setAreaCity(entity.getAreaCity());
            response.setRegisterPhone(entity.getRegisterPhone());
            response.setAddress(entity.getAddress());
            response.setBankName(entity.getBankName());
            response.setBankAccount(entity.getBankAccount());
            response.setRegNo(entity.getRegNo());
        }
        return response;
    }

    private record DetailHit(QccEnterpriseInfo entity, boolean fromCache) {
    }
}
