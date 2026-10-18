package cn.cordys.crm.ai.bossbrief.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.bossbrief.domain.BossBriefResult;
import cn.cordys.crm.ai.bossbrief.domain.BossBriefTopic;
import cn.cordys.crm.ai.bossbrief.dto.request.BossBriefTopicCreateRequest;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 老板晨报：维护关注主题（行业/市场/政策等），定时 + 手动联网搜索当日要闻，
 * 按 URL 去重落库并站内信推给订阅人。门控与额度复用 {@code lead_crawl}；搜索按次折算 token 记账。
 */
@Service
@Slf4j
public class BossBriefService {

    @Resource
    private BaseMapper<BossBriefTopic> topicMapper;
    @Resource
    private BaseMapper<BossBriefResult> resultMapper;
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

    public List<BossBriefTopic> list(String orgId) {
        return topicMapper.selectListByLambda(new LambdaQueryWrapper<BossBriefTopic>()
                .eq(BossBriefTopic::getOrganizationId, orgId)
                .orderByDesc(BossBriefTopic::getCreateTime));
    }

    public BossBriefTopic create(String orgId, String userId, BossBriefTopicCreateRequest request) {
        checkAvailable(orgId);
        String topic = StringUtils.trimToNull(request.getTopic());
        if (topic == null) {
            throw new GenericException("请填写关注主题");
        }
        long now = System.currentTimeMillis();
        BossBriefTopic sub = new BossBriefTopic();
        sub.setId(IDGenerator.nextStr());
        sub.setOrganizationId(orgId);
        sub.setUserId(userId);
        sub.setTopic(topic);
        sub.setEnabled(true);
        sub.setCreateUser(userId);
        sub.setUpdateUser(userId);
        sub.setCreateTime(now);
        sub.setUpdateTime(now);
        topicMapper.insert(sub);
        return sub;
    }

    public void delete(String orgId, String id) {
        require(orgId, id);
        topicMapper.deleteByPrimaryKey(id);
        resultMapper.selectListByLambda(new LambdaQueryWrapper<BossBriefResult>()
                .eq(BossBriefResult::getTopicId, id)).forEach(r -> resultMapper.deleteByPrimaryKey(r.getId()));
    }

    public void toggle(String orgId, String id, boolean enabled) {
        BossBriefTopic sub = require(orgId, id);
        sub.setEnabled(enabled);
        sub.setUpdateTime(System.currentTimeMillis());
        topicMapper.updateById(sub);
    }

    public List<BossBriefResult> results(String orgId, String id) {
        require(orgId, id);
        return resultMapper.selectListByLambda(new LambdaQueryWrapper<BossBriefResult>()
                .eq(BossBriefResult::getTopicId, id)
                .orderByDesc(BossBriefResult::getCreateTime));
    }

    /** 手动「立即生成」某条主题的当日要闻，返回本次新发现的结果。 */
    public List<BossBriefResult> searchNow(String orgId, String id) {
        return searchTopic(orgId, require(orgId, id));
    }

    /** 定时任务入口：对所有启用的主题逐个联网搜索，单条失败不影响其余。 */
    public void searchAllTopics() {
        List<BossBriefTopic> all = topicMapper.selectAll(null);
        for (BossBriefTopic sub : all) {
            if (!Boolean.TRUE.equals(sub.getEnabled())) {
                continue;
            }
            try {
                searchTopic(sub.getOrganizationId(), sub);
            } catch (Exception e) {
                log.error("老板晨报主题 {} 定时搜索失败", sub.getId(), e);
            }
        }
    }

    private List<BossBriefResult> searchTopic(String orgId, BossBriefTopic sub) {
        checkAvailable(orgId);
        String query = StringUtils.trimToNull(sub.getTopic());
        if (query == null) {
            return List.of();
        }
        query = query + " 最新动态 行业要闻";

        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }

        List<AdvisorWebSource> sources = deepSeekSearchClient.search(query, 15);
        recordSearch(orgId, sub.getUserId());

        Set<String> existingUrls = new HashSet<>();
        resultMapper.selectListByLambda(new LambdaQueryWrapper<BossBriefResult>()
                .eq(BossBriefResult::getTopicId, sub.getId()))
                .forEach(r -> existingUrls.add(r.getUrl()));

        List<BossBriefResult> newResults = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (AdvisorWebSource source : sources) {
            String url = StringUtils.left(source.getUrl(), 512);
            if (StringUtils.isBlank(url) || existingUrls.contains(url)) {
                continue;
            }
            BossBriefResult result = new BossBriefResult();
            result.setId(IDGenerator.nextStr());
            result.setOrganizationId(orgId);
            result.setTopicId(sub.getId());
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

    /** 每条主题一批新结果只发一条站内信，告知订阅人数量。 */
    private void push(BossBriefTopic sub, int count) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("name", sub.getTopic());
        resource.put("count", count);
        commonNoticeSendService.sendNotice(NotificationConstants.Module.SYSTEM,
                NotificationConstants.Event.BOSS_BRIEF_NEW, resource,
                sub.getUserId(), sub.getOrganizationId(), List.of(sub.getUserId()), false);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_LEAD_CRAWL)) {
            throw new GenericException("当前版本未开通「老板晨报」功能");
        }
    }

    private BossBriefTopic require(String orgId, String id) {
        BossBriefTopic sub = topicMapper.selectByPrimaryKey(id);
        if (sub == null || !orgId.equals(sub.getOrganizationId())) {
            throw new GenericException("主题不存在");
        }
        return sub;
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
