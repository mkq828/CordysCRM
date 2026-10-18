package cn.cordys.crm.ai.tender.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.search.DeepSeekSearchClient;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.ai.tender.domain.TenderResult;
import cn.cordys.crm.ai.tender.domain.TenderSubscription;
import cn.cordys.crm.ai.tender.dto.request.TenderSubscriptionCreateRequest;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 标讯订阅：维护订阅条件（行业/地域/关键词），定时 + 手动联网搜索招标信息，按 URL 去重落库并站内信推给订阅人。
 * 门控与额度复用 {@code lead_crawl}（同属联网找客户/商机类）；搜索按次折算 token 记账。
 */
@Service
@Slf4j
public class TenderSubscriptionService {

    @Resource
    private BaseMapper<TenderSubscription> subscriptionMapper;
    @Resource
    private BaseMapper<TenderResult> resultMapper;
    @Resource
    private DeepSeekSearchClient deepSeekSearchClient;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private EditionService editionService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;

    public List<TenderSubscription> list(String orgId) {
        return subscriptionMapper.selectListByLambda(new LambdaQueryWrapper<TenderSubscription>()
                .eq(TenderSubscription::getOrganizationId, orgId)
                .orderByDesc(TenderSubscription::getCreateTime));
    }

    public TenderSubscription create(String orgId, String userId, TenderSubscriptionCreateRequest request) {
        checkAvailable(orgId);
        String industry = StringUtils.trimToNull(request.getIndustry());
        String region = StringUtils.trimToNull(request.getRegion());
        String keyword = StringUtils.trimToNull(request.getKeyword());
        if (industry == null && region == null && keyword == null) {
            throw new GenericException("请至少填写行业、地域或关键词");
        }
        long now = System.currentTimeMillis();
        TenderSubscription sub = new TenderSubscription();
        sub.setId(IDGenerator.nextStr());
        sub.setOrganizationId(orgId);
        sub.setUserId(userId);
        sub.setIndustry(industry);
        sub.setRegion(region);
        sub.setKeyword(keyword);
        sub.setEnabled(true);
        sub.setCreateUser(userId);
        sub.setUpdateUser(userId);
        sub.setCreateTime(now);
        sub.setUpdateTime(now);
        subscriptionMapper.insert(sub);
        return sub;
    }

    public void delete(String orgId, String id) {
        require(orgId, id);
        subscriptionMapper.deleteByPrimaryKey(id);
        resultMapper.selectListByLambda(new LambdaQueryWrapper<TenderResult>()
                .eq(TenderResult::getSubscriptionId, id)).forEach(r -> resultMapper.deleteByPrimaryKey(r.getId()));
    }

    public void toggle(String orgId, String id, boolean enabled) {
        TenderSubscription sub = require(orgId, id);
        sub.setEnabled(enabled);
        sub.setUpdateTime(System.currentTimeMillis());
        subscriptionMapper.updateById(sub);
    }

    public List<TenderResult> results(String orgId, String id) {
        require(orgId, id);
        return resultMapper.selectListByLambda(new LambdaQueryWrapper<TenderResult>()
                .eq(TenderResult::getSubscriptionId, id)
                .orderByDesc(TenderResult::getCreateTime));
    }

    /** 手动「立即搜索」某条订阅，返回本次新发现的招标结果。 */
    public List<TenderResult> searchNow(String orgId, String id) {
        return searchSubscription(orgId, require(orgId, id));
    }

    /** 定时任务入口：对所有启用的订阅逐个联网搜索，单条失败不影响其余。 */
    public void searchAllSubscriptions() {
        List<TenderSubscription> all = subscriptionMapper.selectAll(null);
        for (TenderSubscription sub : all) {
            if (!Boolean.TRUE.equals(sub.getEnabled())) {
                continue;
            }
            try {
                searchSubscription(sub.getOrganizationId(), sub);
            } catch (Exception e) {
                log.error("标讯订阅 {} 定时搜索失败", sub.getId(), e);
            }
        }
    }

    private List<TenderResult> searchSubscription(String orgId, TenderSubscription sub) {
        checkAvailable(orgId);
        String query = buildQuery(sub);
        if (StringUtils.isBlank(query)) {
            return List.of();
        }

        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }

        List<AdvisorWebSource> sources = deepSeekSearchClient.search(query);
        recordSearch(orgId, sub.getUserId());

        Set<String> existingUrls = new HashSet<>();
        resultMapper.selectListByLambda(new LambdaQueryWrapper<TenderResult>()
                .eq(TenderResult::getSubscriptionId, sub.getId()))
                .forEach(r -> existingUrls.add(r.getUrl()));

        List<TenderResult> newResults = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (AdvisorWebSource source : sources) {
            String url = StringUtils.left(source.getUrl(), 512);
            if (StringUtils.isBlank(url) || existingUrls.contains(url)) {
                continue;
            }
            TenderResult result = new TenderResult();
            result.setId(IDGenerator.nextStr());
            result.setOrganizationId(orgId);
            result.setSubscriptionId(sub.getId());
            result.setTitle(StringUtils.defaultIfBlank(source.getTitle(), url));
            result.setUrl(url);
            result.setSnippet(source.getSnippet());
            result.setContent(source.getContent());
            result.setCreateUser(sub.getUserId());
            result.setCreateTime(now);
            resultMapper.insert(result);
            existingUrls.add(url);
            newResults.add(result);
        }

        if (!newResults.isEmpty()) {
            push(sub, newResults.size());
        }
        return newResults;
    }

    /** 每条订阅一批新结果只发一条站内信，告知订阅人数量。 */
    private void push(TenderSubscription sub, int count) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("name", subLabel(sub));
        resource.put("count", count);
        commonNoticeSendService.sendNotice(NotificationConstants.Module.SYSTEM,
                NotificationConstants.Event.TENDER_SUBSCRIBE_NEW, resource,
                sub.getUserId(), sub.getOrganizationId(), List.of(sub.getUserId()), false);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_LEAD_CRAWL)) {
            throw new GenericException("当前版本未开通「标讯订阅」功能");
        }
    }

    private TenderSubscription require(String orgId, String id) {
        TenderSubscription sub = subscriptionMapper.selectByPrimaryKey(id);
        if (sub == null || !orgId.equals(sub.getOrganizationId())) {
            throw new GenericException("订阅不存在");
        }
        return sub;
    }

    private String buildQuery(TenderSubscription sub) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(sub.getIndustry())) {
            parts.add(sub.getIndustry().trim());
        }
        if (StringUtils.isNotBlank(sub.getRegion())) {
            parts.add(sub.getRegion().trim());
        }
        if (StringUtils.isNotBlank(sub.getKeyword())) {
            parts.add(sub.getKeyword().trim());
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(" ", parts) + " 招标 采购 公告";
    }

    private String subLabel(TenderSubscription sub) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(sub.getIndustry())) {
            parts.add("行业=" + sub.getIndustry().trim());
        }
        if (StringUtils.isNotBlank(sub.getRegion())) {
            parts.add("地域=" + sub.getRegion().trim());
        }
        if (StringUtils.isNotBlank(sub.getKeyword())) {
            parts.add("关键词=" + sub.getKeyword().trim());
        }
        return parts.isEmpty() ? "标讯订阅" : String.join(" ", parts);
    }

    /** 1 次联网搜索折算 token 记账，与潜客挖掘同一按次口径。 */
    private void recordSearch(String orgId, String userId) {
        aiQuotaService.record(orgId, AiQuotaConstant.AI_LEAD_CRAWL, AiQuotaConstant.SEARCH_MODEL_CODE,
                getSearchTokensPerCall(), 0L, userId);
    }

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

    private boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }

    private String blockedMessage(String status) {
        if (AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)) {
            return "调用过于频繁，请稍后再试";
        }
        if (AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status)) {
            return "AI 服务暂不可用，请联系管理员";
        }
        return "AI 额度已用完，请加购或升级";
    }
}
