package cn.cordys.crm.ai.competitor.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.competitor.domain.CompetitorResult;
import cn.cordys.crm.ai.competitor.domain.CompetitorSubscription;
import cn.cordys.crm.ai.competitor.dto.request.CompetitorSubscriptionCreateRequest;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.search.DeepSeekSearchClient;
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
 * 竞品动态订阅：维护关注竞品（竞品名/关键词），定时 + 手动联网搜索竞品最新动态，按 URL 去重落库并站内信推给订阅人。
 * 门控与额度复用 {@code lead_crawl}（同属联网找客户/商机类）；搜索按次折算 token 记账。
 */
@Service
@Slf4j
public class CompetitorSubscriptionService {

    @Resource
    private BaseMapper<CompetitorSubscription> subscriptionMapper;
    @Resource
    private BaseMapper<CompetitorResult> resultMapper;
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

    public List<CompetitorSubscription> list(String orgId) {
        return subscriptionMapper.selectListByLambda(new LambdaQueryWrapper<CompetitorSubscription>()
                .eq(CompetitorSubscription::getOrganizationId, orgId)
                .orderByDesc(CompetitorSubscription::getCreateTime));
    }

    public CompetitorSubscription create(String orgId, String userId, CompetitorSubscriptionCreateRequest request) {
        checkAvailable(orgId);
        String competitor = StringUtils.trimToNull(request.getCompetitor());
        if (competitor == null) {
            throw new GenericException("请填写竞品名称");
        }
        String keyword = StringUtils.trimToNull(request.getKeyword());
        long now = System.currentTimeMillis();
        CompetitorSubscription sub = new CompetitorSubscription();
        sub.setId(IDGenerator.nextStr());
        sub.setOrganizationId(orgId);
        sub.setUserId(userId);
        sub.setCompetitor(competitor);
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
        resultMapper.selectListByLambda(new LambdaQueryWrapper<CompetitorResult>()
                .eq(CompetitorResult::getSubscriptionId, id)).forEach(r -> resultMapper.deleteByPrimaryKey(r.getId()));
    }

    public void toggle(String orgId, String id, boolean enabled) {
        CompetitorSubscription sub = require(orgId, id);
        sub.setEnabled(enabled);
        sub.setUpdateTime(System.currentTimeMillis());
        subscriptionMapper.updateById(sub);
    }

    public List<CompetitorResult> results(String orgId, String id) {
        require(orgId, id);
        return resultMapper.selectListByLambda(new LambdaQueryWrapper<CompetitorResult>()
                .eq(CompetitorResult::getSubscriptionId, id)
                .orderByDesc(CompetitorResult::getCreateTime));
    }

    /** 手动「立即搜索」某条订阅，返回本次新发现的竞品动态。 */
    public List<CompetitorResult> searchNow(String orgId, String id) {
        return searchSubscription(orgId, require(orgId, id));
    }

    /** 定时任务入口：对所有启用的订阅逐个联网搜索，单条失败不影响其余。 */
    public void searchAllSubscriptions() {
        List<CompetitorSubscription> all = subscriptionMapper.selectAll(null);
        for (CompetitorSubscription sub : all) {
            if (!Boolean.TRUE.equals(sub.getEnabled())) {
                continue;
            }
            try {
                searchSubscription(sub.getOrganizationId(), sub);
            } catch (Exception e) {
                log.error("竞品动态订阅 {} 定时搜索失败", sub.getId(), e);
            }
        }
    }

    private List<CompetitorResult> searchSubscription(String orgId, CompetitorSubscription sub) {
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
        resultMapper.selectListByLambda(new LambdaQueryWrapper<CompetitorResult>()
                .eq(CompetitorResult::getSubscriptionId, sub.getId()))
                .forEach(r -> existingUrls.add(r.getUrl()));

        List<String> keywords = splitKeywords(sub.getKeyword());
        List<CompetitorResult> newResults = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (AdvisorWebSource source : sources) {
            String url = StringUtils.left(source.getUrl(), 512);
            if (StringUtils.isBlank(url) || existingUrls.contains(url)) {
                continue;
            }
            if (!keywords.isEmpty() && !matchesAny(keywords, source)) {
                continue;
            }
            CompetitorResult result = new CompetitorResult();
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
    private void push(CompetitorSubscription sub, int count) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("name", subLabel(sub));
        resource.put("count", count);
        commonNoticeSendService.sendNotice(NotificationConstants.Module.SYSTEM,
                NotificationConstants.Event.COMPETITOR_MONITOR_NEW, resource,
                sub.getUserId(), sub.getOrganizationId(), List.of(sub.getUserId()), false);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_LEAD_CRAWL)) {
            throw new GenericException("当前版本未开通「竞品动态」功能");
        }
    }

    private CompetitorSubscription require(String orgId, String id) {
        CompetitorSubscription sub = subscriptionMapper.selectByPrimaryKey(id);
        if (sub == null || !orgId.equals(sub.getOrganizationId())) {
            throw new GenericException("订阅不存在");
        }
        return sub;
    }

    private String buildQuery(CompetitorSubscription sub) {
        List<String> parts = new ArrayList<>();
        // 关键词前置，作为搜索主意图，避免被固定后缀稀释
        if (StringUtils.isNotBlank(sub.getKeyword())) {
            parts.add(sub.getKeyword().trim());
        }
        if (StringUtils.isNotBlank(sub.getCompetitor())) {
            parts.add(sub.getCompetitor().trim());
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(" ", parts) + " 最新动态 新闻 融资";
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

    private String subLabel(CompetitorSubscription sub) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(sub.getCompetitor())) {
            parts.add("竞品=" + sub.getCompetitor().trim());
        }
        if (StringUtils.isNotBlank(sub.getKeyword())) {
            parts.add("关键词=" + sub.getKeyword().trim());
        }
        return parts.isEmpty() ? "竞品动态" : String.join(" ", parts);
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
