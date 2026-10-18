package cn.cordys.crm.ai.sentiment.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.search.DeepSeekSearchClient;
import cn.cordys.crm.ai.sentiment.domain.SentimentResult;
import cn.cordys.crm.ai.sentiment.domain.SentimentSubscription;
import cn.cordys.crm.ai.sentiment.dto.request.SentimentSubscriptionCreateRequest;
import cn.cordys.crm.ai.service.AiQuotaService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 舆情监控订阅：维护监控对象（品牌/客户/供应商名 + 关键词），定时 + 手动联网搜索负面舆情/风险信号，
 * 按 URL 去重落库并站内信推给订阅人。门控与额度复用 {@code lead_crawl}；搜索按次折算 token 记账。
 */
@Service
@Slf4j
public class SentimentSubscriptionService {

    @Resource
    private BaseMapper<SentimentSubscription> subscriptionMapper;
    @Resource
    private BaseMapper<SentimentResult> resultMapper;
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

    public List<SentimentSubscription> list(String orgId) {
        return subscriptionMapper.selectListByLambda(new LambdaQueryWrapper<SentimentSubscription>()
                .eq(SentimentSubscription::getOrganizationId, orgId)
                .orderByDesc(SentimentSubscription::getCreateTime));
    }

    public SentimentSubscription create(String orgId, String userId, SentimentSubscriptionCreateRequest request) {
        checkAvailable(orgId);
        String target = StringUtils.trimToNull(request.getTarget());
        if (target == null) {
            throw new GenericException("请填写监控对象");
        }
        String keyword = StringUtils.trimToNull(request.getKeyword());
        long now = System.currentTimeMillis();
        SentimentSubscription sub = new SentimentSubscription();
        sub.setId(IDGenerator.nextStr());
        sub.setOrganizationId(orgId);
        sub.setUserId(userId);
        sub.setTarget(target);
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
        resultMapper.selectListByLambda(new LambdaQueryWrapper<SentimentResult>()
                .eq(SentimentResult::getSubscriptionId, id)).forEach(r -> resultMapper.deleteByPrimaryKey(r.getId()));
    }

    public void toggle(String orgId, String id, boolean enabled) {
        SentimentSubscription sub = require(orgId, id);
        sub.setEnabled(enabled);
        sub.setUpdateTime(System.currentTimeMillis());
        subscriptionMapper.updateById(sub);
    }

    public List<SentimentResult> results(String orgId, String id) {
        require(orgId, id);
        return resultMapper.selectListByLambda(new LambdaQueryWrapper<SentimentResult>()
                .eq(SentimentResult::getSubscriptionId, id)
                .orderByDesc(SentimentResult::getCreateTime));
    }

    /** 手动「立即搜索」某条订阅，返回本次新发现的舆情/风险信号。 */
    public List<SentimentResult> searchNow(String orgId, String id) {
        return searchSubscription(orgId, require(orgId, id));
    }

    /** 定时任务入口：对所有启用的订阅逐个联网搜索，单条失败不影响其余。 */
    public void searchAllSubscriptions() {
        List<SentimentSubscription> all = subscriptionMapper.selectAll(null);
        for (SentimentSubscription sub : all) {
            if (!Boolean.TRUE.equals(sub.getEnabled())) {
                continue;
            }
            try {
                searchSubscription(sub.getOrganizationId(), sub);
            } catch (Exception e) {
                log.error("舆情监控订阅 {} 定时搜索失败", sub.getId(), e);
            }
        }
    }

    private List<SentimentResult> searchSubscription(String orgId, SentimentSubscription sub) {
        checkAvailable(orgId);
        String query = buildQuery(sub);
        if (StringUtils.isBlank(query)) {
            return List.of();
        }

        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }

        List<AdvisorWebSource> sources = deepSeekSearchClient.search(query, 15);
        recordSearch(orgId, sub.getUserId());

        Set<String> existingUrls = new HashSet<>();
        resultMapper.selectListByLambda(new LambdaQueryWrapper<SentimentResult>()
                .eq(SentimentResult::getSubscriptionId, sub.getId()))
                .forEach(r -> existingUrls.add(r.getUrl()));

        List<String> keywords = splitKeywords(sub.getKeyword());
        List<SentimentResult> newResults = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (AdvisorWebSource source : sources) {
            String url = StringUtils.left(source.getUrl(), 512);
            if (StringUtils.isBlank(url) || existingUrls.contains(url)) {
                continue;
            }
            if (!keywords.isEmpty() && !matchesAny(keywords, source)) {
                continue;
            }
            SentimentResult result = new SentimentResult();
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
    private void push(SentimentSubscription sub, int count) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("name", subLabel(sub));
        resource.put("count", count);
        commonNoticeSendService.sendNotice(NotificationConstants.Module.SYSTEM,
                NotificationConstants.Event.SENTIMENT_MONITOR_NEW, resource,
                sub.getUserId(), sub.getOrganizationId(), List.of(sub.getUserId()), false);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_LEAD_CRAWL)) {
            throw new GenericException("当前版本未开通「舆情监控」功能");
        }
    }

    private SentimentSubscription require(String orgId, String id) {
        SentimentSubscription sub = subscriptionMapper.selectByPrimaryKey(id);
        if (sub == null || !orgId.equals(sub.getOrganizationId())) {
            throw new GenericException("订阅不存在");
        }
        return sub;
    }

    private String buildQuery(SentimentSubscription sub) {
        List<String> parts = new ArrayList<>();
        // 关键词前置，作为搜索主意图，避免被固定后缀稀释
        if (StringUtils.isNotBlank(sub.getKeyword())) {
            parts.add(sub.getKeyword().trim());
        }
        if (StringUtils.isNotBlank(sub.getTarget())) {
            parts.add(sub.getTarget().trim());
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(" ", parts) + " 负面 舆情 涉诉 风险";
    }

    /** 关键词按空格/逗号/顿号切分，任一命中标题/摘要/正文即视为相关。 */
    private List<String> splitKeywords(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        return Arrays.stream(keyword.split("[\\s,，、]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private boolean matchesAny(List<String> keywords, AdvisorWebSource source) {
        String haystack = StringUtils.defaultString(source.getTitle()) + " "
                + StringUtils.defaultString(source.getSnippet()) + " "
                + StringUtils.defaultString(source.getContent());
        return keywords.stream().anyMatch(haystack::contains);
    }

    private String subLabel(SentimentSubscription sub) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(sub.getTarget())) {
            parts.add("对象=" + sub.getTarget().trim());
        }
        if (StringUtils.isNotBlank(sub.getKeyword())) {
            parts.add("关键词=" + sub.getKeyword().trim());
        }
        return parts.isEmpty() ? "舆情监控" : String.join(" ", parts);
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
