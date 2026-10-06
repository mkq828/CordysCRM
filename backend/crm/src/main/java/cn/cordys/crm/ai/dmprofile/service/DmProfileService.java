package cn.cordys.crm.ai.dmprofile.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.EncryptUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dmprofile.client.DmProfileClient;
import cn.cordys.crm.ai.dmprofile.domain.AiDmAccount;
import cn.cordys.crm.ai.dmprofile.domain.AiDmProfile;
import cn.cordys.crm.ai.dmprofile.dto.DmAccountIdentity;
import cn.cordys.crm.ai.dmprofile.dto.DmCollectResult;
import cn.cordys.crm.ai.dmprofile.dto.DmRawConversation;
import cn.cordys.crm.ai.dmprofile.dto.DmRawMessage;
import cn.cordys.crm.ai.dmprofile.dto.request.DmAccountBindRequest;
import cn.cordys.crm.ai.dmprofile.dto.request.DmProfileImportRequest;
import cn.cordys.crm.ai.dmprofile.dto.response.DmAccountResponse;
import cn.cordys.crm.ai.dmprofile.dto.response.DmImportResponse;
import cn.cordys.crm.ai.dmprofile.dto.response.DmProfileResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.clue.dto.request.ClueAddRequest;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 私信获客画像（功能 8）：账号绑定编排 + 采集触发 + G1 模型画像 + G2 额度 + 导入线索池。
 * 采集由 Python 微服务完成并返回原始会话；画像走 CRM 自己的模型路由与额度计量。
 * 采集为异步执行：Python 侧 N 浏览器槽位池 + asyncio 任务队列（提交立即返回 task_id，轮询取结果），
 * Java 侧 @Async 提交并轮询，接口立即返回「采集中」，前端轮询账号状态与画像列表感知进度。
 */
@Service
@Slf4j
public class DmProfileService {

    /** 账号状态：登录态有效 */
    private static final String STATUS_LOGGED_IN = "LOGGED_IN";
    /** 账号状态：采集中（异步任务进行时） */
    private static final String STATUS_CRAWLING = "CRAWLING";
    /** 账号状态：登录态过期，需重新绑定 */
    private static final String STATUS_EXPIRED = "EXPIRED";
    /** 账号状态：采集失败 */
    private static final String STATUS_FAILED = "FAILED";
    /** 画像状态：只落原始消息，待画像 */
    private static final String PROFILE_RAW = "RAW";
    /** 画像状态：已画像 */
    private static final String PROFILE_ANALYZED = "ANALYZED";
    /** 画像状态：已导入线索池 */
    private static final String PROFILE_IMPORTED = "IMPORTED";

    /** 采集任务轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 3000L;

    /** 采集最长等待时间（毫秒）：Python 单任务约 240s 上限 + 排队冗余 */
    private static final long POLL_TIMEOUT_MS = 15 * 60 * 1000L;

    /** 手机号正则（平台不暴露，只能从私信正文提用户自留号码） */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /** 微信号正则（微信/vx/V信 等引导词后的号，尽力提取） */
    private static final Pattern WECHAT_PATTERN =
            Pattern.compile("(?:微信|vx|VX|V信|wx|WX|加V)[:：\\s]*([a-zA-Z][a-zA-Z0-9_-]{5,19})");

    /** 线索来源自定义字段 internalKey */
    private static final String CLUE_SOURCE_INTERNAL_KEY = "clueSource";

    /** 导入线索时的来源标记 */
    private static final String CLUE_SOURCE_DM_PROFILE = "私信获客";

    /** 画像 prompt：只输出 fenced JSON，字段不足填未知 */
    private static final String PROFILE_SYSTEM_PROMPT = """
            你是私域获客的客户画像分析师。基于抖音私信对话内容 + 客户主页简介，推断客户画像，帮助销售判断跟进价值与话术。
            写作铁律：
            1. 只依据对话内容与主页简介推断，信息不足的字段填「未知」，不要编造。
            2. 品类、喜好、话术要具体，能指导销售下一步动作。

            只输出一个 ```json 代码块（不要任何多余文字），字段如下：
            {
              "name": "客户自报的真实姓名，未提及填 未知",
              "gender": "男/女/未知",
              "age": "年龄段，如 25-30，未知填 未知",
              "city": "城市，未知填 未知",
              "accountCategory": "客户账号的品类/定位（从主页简介或认证推断，如美食探店、服装带货、亲子记录），无则填 未知",
              "category": "客户咨询的品类/关注点",
              "preference": "客户喜好/关注点",
              "summary": "私聊话术汇总：客户说了什么、意向如何",
              "confidence": "高/中/低"
            }
            """;

    @Resource
    private BaseMapper<AiDmAccount> aiDmAccountMapper;
    @Resource
    private BaseMapper<AiDmProfile> aiDmProfileMapper;
    @Resource
    private DmProfileClient dmProfileClient;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private ClueService clueService;
    @Resource
    private ModuleFormService moduleFormService;

    // ==================== 账号 ====================

    public List<DmAccountResponse> listAccounts(String orgId) {
        LambdaQueryWrapper<AiDmAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiDmAccount::getOrganizationId, orgId).orderByDesc(AiDmAccount::getCreateTime);
        List<DmAccountResponse> result = new ArrayList<>();
        for (AiDmAccount account : aiDmAccountMapper.selectListByLambda(wrapper)) {
            result.add(toAccountResponse(account));
        }
        return result;
    }

    public DmAccountResponse bindAccount(String orgId, String userId, DmAccountBindRequest request) {
        String cookieText = StringUtils.trimToEmpty(request.getCookieText());
        if (!cookieText.contains("sessionid_ss") || !cookieText.contains("uid_tt")) {
            throw new GenericException("Cookie 缺少关键项（需含 sessionid_ss 和 uid_tt），请重新登录后复制");
        }
        // 先喂给采集服务注入并校验登录态，通过后再加密落库；顺带带回「自己」的昵称/抖音号回填
        DmAccountIdentity identity = dmProfileClient.injectCookie(cookieText);

        long now = System.currentTimeMillis();
        AiDmAccount account = findByOrgPlatform(orgId, request.getPlatform());
        boolean insert = account == null;
        if (insert) {
            account = new AiDmAccount();
            account.setId(IDGenerator.nextStr());
            account.setOrganizationId(orgId);
            account.setPlatform(request.getPlatform());
            account.setCreateTime(now);
            account.setCreateUser(userId);
        }
        account.setCookieCiphertext(EncryptUtils.aesEncrypt(cookieText));
        account.setStatus(STATUS_LOGGED_IN);
        account.setLastError(null);
        if (StringUtils.isNotBlank(identity.accountName())) {
            account.setAccountName(identity.accountName());
        }
        if (StringUtils.isNotBlank(identity.uniqueId())) {
            account.setUniqueId(identity.uniqueId());
        }
        account.setUpdateTime(now);
        account.setUpdateUser(userId);
        if (insert) {
            aiDmAccountMapper.insert(account);
        } else {
            aiDmAccountMapper.updateById(account);
        }
        return toAccountResponse(account);
    }

    public void unbind(String orgId, String accountId) {
        // 解绑即删除：一个平台一个账号，解除登录态后账号不再保留，重新绑定时重建
        requireAccount(orgId, accountId);
        aiDmAccountMapper.deleteByPrimaryKey(accountId);
    }

    // ==================== 采集（异步） ====================

    /**
     * 触发采集（同步入口）：校验 + 标记「采集中」并返回账号，真正的采集由 {@link #crawlAsync} 异步执行。
     * 由 Controller 跨 Bean 调用 {@link #crawlAsync}，保证 @Async 代理生效。
     */
    public DmAccountResponse startCrawl(String orgId, String accountId, String userId) {
        AiDmAccount account = requireAccount(orgId, accountId);
        if (StringUtils.isBlank(account.getCookieCiphertext())) {
            throw new GenericException("该账号未绑定或登录态已清空，请重新绑定");
        }
        if (STATUS_CRAWLING.equals(account.getStatus())) {
            throw new GenericException("该账号正在采集中，请稍候");
        }
        account.setStatus(STATUS_CRAWLING);
        account.setLastError(null);
        account.setUpdateTime(System.currentTimeMillis());
        account.setUpdateUser(userId);
        aiDmAccountMapper.updateById(account);
        return toAccountResponse(account);
    }

    /**
     * 异步采集 + 画像。入参只传 ID 与上下文（异步线程无法继承请求态），
     * 结束时把账号状态归位为 LOGGED_IN / EXPIRED / FAILED。
     */
    @Async
    public void crawlAsync(String accountId, String orgId, String userId) {
        AiDmAccount account = aiDmAccountMapper.selectByPrimaryKey(accountId);
        if (account == null || !orgId.equals(account.getOrganizationId())) {
            return;
        }
        long now = System.currentTimeMillis();
        try {
            String cookieText = EncryptUtils.aesDecrypt(account.getCookieCiphertext());
            // 提交采集任务（Cookie 随任务入队，Python 槽位内原子「注入 + 采集」），轮询到结束
            String taskId = dmProfileClient.submitCollect(cookieText);
            DmCollectResult result = awaitCollect(taskId);
            if (!result.isOk()) {
                // loggedIn=false 表示 Cookie 已失效需重绑；否则为采集失败（风控/超时等）
                String error = StringUtils.isBlank(result.getMessage()) ? "采集失败，请稍后重试" : result.getMessage();
                failAccount(account, result.isLoggedIn() ? STATUS_FAILED : STATUS_EXPIRED, error, userId);
                return;
            }
            for (DmRawConversation conv : result.getConvs()) {
                if (conv == null || StringUtils.isBlank(conv.getName())) {
                    continue;
                }
                try {
                    persistAndAnalyze(orgId, accountId, account.getPlatform(), conv, userId);
                } catch (Exception e) {
                    log.warn("画像分析失败, accountId={}, name={}", accountId, conv.getName(), e);
                    persistRaw(orgId, accountId, account.getPlatform(), conv, userId, e.getMessage());
                }
            }
            account.setLastCrawlTime(now);
            account.setStatus(STATUS_LOGGED_IN);
            account.setLastError(null);
        } catch (Exception e) {
            log.error("私信采集失败, accountId={}, orgId={}", accountId, orgId, e);
            failAccount(account, STATUS_FAILED, message(e, "采集失败，请稍后重试"), userId);
            return;
        }
        account.setUpdateTime(System.currentTimeMillis());
        account.setUpdateUser(userId);
        aiDmAccountMapper.updateById(account);
    }

    /** 轮询采集任务直到结束（done/failed），超时返回失败结果 */
    private DmCollectResult awaitCollect(String taskId) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            DmCollectResult result = dmProfileClient.pollCollect(taskId);
            if (result.isFinished()) {
                return result;
            }
            sleepUnchecked(POLL_INTERVAL_MS);
        }
        DmCollectResult timeout = new DmCollectResult();
        timeout.setStatus("failed");
        timeout.setOk(false);
        timeout.setLoggedIn(true);
        timeout.setMessage("采集超时，请稍后重试");
        return timeout;
    }

    private void sleepUnchecked(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenericException("采集等待被中断");
        }
    }

    private void failAccount(AiDmAccount account, String status, String message, String userId) {
        account.setStatus(status);
        account.setLastError(truncate(message, 500));
        account.setUpdateTime(System.currentTimeMillis());
        account.setUpdateUser(userId);
        aiDmAccountMapper.updateById(account);
    }

    private String message(Exception e, String fallback) {
        String msg = e.getMessage();
        return StringUtils.isBlank(msg) ? fallback : msg;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    // ==================== 画像结果 ====================

    public List<DmProfileResponse> listProfiles(String orgId, String status) {
        LambdaQueryWrapper<AiDmProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiDmProfile::getOrganizationId, orgId);
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(AiDmProfile::getStatus, status);
        }
        wrapper.orderByDesc(AiDmProfile::getCreateTime);
        List<DmProfileResponse> result = new ArrayList<>();
        for (AiDmProfile profile : aiDmProfileMapper.selectListByLambda(wrapper)) {
            result.add(toProfileResponse(profile));
        }
        return result;
    }

    public DmImportResponse importProfiles(String orgId, String userId, DmProfileImportRequest request) {
        String clueSourceFieldId = resolveClueSourceFieldId(orgId);
        List<String> failed = new ArrayList<>();
        int success = 0;
        int filtered = 0;
        for (String profileId : request.getProfileIds()) {
            AiDmProfile profile = findProfile(orgId, profileId);
            if (profile == null) {
                failed.add(profileId);
                continue;
            }
            // 无手机号无法联系，导入无意义，直接过滤
            if (StringUtils.isBlank(profile.getPhone())) {
                filtered++;
                continue;
            }
            try {
                ClueAddRequest clueRequest = new ClueAddRequest();
                clueRequest.setName(profile.getNickname());
                clueRequest.setPhone(profile.getPhone());
                if (clueSourceFieldId != null) {
                    clueRequest.setModuleFields(List.of(new BaseModuleFieldValue(clueSourceFieldId, CLUE_SOURCE_DM_PROFILE)));
                }
                var clue = clueService.add(clueRequest, userId, orgId);
                profile.setClueId(clue == null ? null : clue.getId());
                profile.setStatus(PROFILE_IMPORTED);
                profile.setUpdateTime(System.currentTimeMillis());
                profile.setUpdateUser(userId);
                aiDmProfileMapper.updateById(profile);
                success++;
            } catch (Exception e) {
                log.warn("画像导入线索失败, profileId={}", profileId, e);
                failed.add(profile.getNickname());
            }
        }
        DmImportResponse response = new DmImportResponse();
        response.setSuccessCount(success);
        response.setFilteredCount(filtered);
        response.setFailedNames(failed);
        return response;
    }

    // ==================== 采集落库 + 画像 ====================

    private void persistAndAnalyze(String orgId, String accountId, String platform,
            DmRawConversation conv, String userId) {
        String messagesJson = messagesJson(conv.getMessages());
        String phone = extractPhone(conv.getMessages());
        String wechat = extractWechat(conv.getMessages());

        AiDmProfile profile = findByNickname(orgId, conv.getName());
        boolean insert = profile == null;
        long now = System.currentTimeMillis();
        if (insert) {
            profile = new AiDmProfile();
            profile.setId(IDGenerator.nextStr());
            profile.setOrganizationId(orgId);
            profile.setAccountId(accountId);
            profile.setPlatform(platform);
            profile.setCreateTime(now);
            profile.setCreateUser(userId);
        }
        profile.setSecUid(conv.getSecUid());
        profile.setNickname(conv.getName());
        profile.setUniqueId(conv.getAccountId());
        profile.setFollowerCount(conv.getFollowerCount());
        profile.setFollowingCount(conv.getFollowingCount());
        profile.setLikedCount(conv.getLikedCount());
        profile.setGender(conv.getGender());
        profile.setBio(conv.getBio());
        profile.setIsGroup(conv.isGroup());
        profile.setPhone(phone);
        profile.setWechat(wechat);
        profile.setMessagesJson(messagesJson);

        // 画像走 G1 模型层（挂 G2 额度）；失败时先落 RAW 再抛出
        try {
            profile.setProfileJson(analyzeProfile(orgId, userId, conv.getName(), conv.getBio(), conv.isGroup(), conv.getMessages()));
            profile.setStatus(PROFILE_ANALYZED);
        } catch (Exception e) {
            profile.setStatus(PROFILE_RAW);
            profile.setUpdateTime(now);
            profile.setUpdateUser(userId);
            if (insert) {
                aiDmProfileMapper.insert(profile);
            } else {
                aiDmProfileMapper.updateById(profile);
            }
            throw e;
        }
        profile.setUpdateTime(System.currentTimeMillis());
        profile.setUpdateUser(userId);
        if (insert) {
            aiDmProfileMapper.insert(profile);
        } else {
            aiDmProfileMapper.updateById(profile);
        }
    }

    /** 画像失败兜底：只落原始消息，状态 FAILED，不丢数据 */
    private void persistRaw(String orgId, String accountId, String platform,
            DmRawConversation conv, String userId, String error) {
        AiDmProfile profile = findByNickname(orgId, conv.getName());
        long now = System.currentTimeMillis();
        boolean insert = profile == null;
        if (insert) {
            profile = new AiDmProfile();
            profile.setId(IDGenerator.nextStr());
            profile.setOrganizationId(orgId);
            profile.setAccountId(accountId);
            profile.setPlatform(platform);
            profile.setCreateTime(now);
            profile.setCreateUser(userId);
        }
        profile.setSecUid(conv.getSecUid());
        profile.setNickname(conv.getName());
        profile.setUniqueId(conv.getAccountId());
        profile.setFollowerCount(conv.getFollowerCount());
        profile.setFollowingCount(conv.getFollowingCount());
        profile.setLikedCount(conv.getLikedCount());
        profile.setGender(conv.getGender());
        profile.setBio(conv.getBio());
        profile.setIsGroup(conv.isGroup());
        profile.setPhone(extractPhone(conv.getMessages()));
        profile.setWechat(extractWechat(conv.getMessages()));
        profile.setMessagesJson(messagesJson(conv.getMessages()));
        profile.setStatus(STATUS_FAILED);
        profile.setProfileJson("{\"error\":\"" + (error == null ? "画像失败" : error) + "\"}");
        profile.setUpdateTime(now);
        profile.setUpdateUser(userId);
        if (insert) {
            aiDmProfileMapper.insert(profile);
        } else {
            aiDmProfileMapper.updateById(profile);
        }
    }

    /** 调用任务模型做画像，返回结构化画像 JSON 字符串（含 confidence） */
    private String analyzeProfile(String orgId, String userId, String nickname, String bio, boolean isGroup,
            List<DmRawMessage> messages) {
        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId, userId);
        if (isBlocked(quota.getStatus())) {
            throw new GenericException(blockedMessage(quota.getStatus()));
        }
        List<LlmMessage> llmMessages = buildProfileMessages(nickname, bio, isGroup, messages);

        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(llmRequest, model);
                llmRequest.setMessages(llmMessages);

                StringBuilder sb = new StringBuilder();
                LlmUsage usage = provider.chatStream(llmRequest, sb::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_DM_PROFILE, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return extractJson(sb.toString());
            } catch (Exception e) {
                lastError = e;
                log.warn("私信画像模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildProfileMessages(String nickname, String bio, boolean isGroup,
            List<DmRawMessage> messages) {
        List<LlmMessage> result = new ArrayList<>();
        result.add(new LlmMessage("system", PROFILE_SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        if (isGroup) {
            // 群聊无个人主页，只汇总群内意向与可提取的联系方式
            sb.append("群聊名称：").append(StringUtils.trimToEmpty(nickname)).append('\n');
        } else {
            sb.append("客户昵称：").append(StringUtils.trimToEmpty(nickname)).append('\n');
            if (StringUtils.isNotBlank(bio)) {
                sb.append("客户主页简介：").append(bio.trim()).append('\n');
            }
        }
        sb.append("私信对话：\n");
        if (messages != null) {
            for (DmRawMessage m : messages) {
                if (m == null || StringUtils.isBlank(m.getContent())) {
                    continue;
                }
                sb.append("me".equals(m.getSender()) ? "我：" : (isGroup ? "群成员：" : "客户："))
                        .append(m.getContent().trim()).append('\n');
            }
        }
        sb.append(isGroup ? "\n请输出该群聊的画像 JSON（个人字段无对应时填 未知，summary 汇总群内意向）。"
                : "\n请输出该客户的画像 JSON。");
        result.add(new LlmMessage("user", sb.toString()));
        return result;
    }

    // ==================== 工具方法 ====================

    private AiDmAccount findByOrgPlatform(String orgId, String platform) {
        LambdaQueryWrapper<AiDmAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiDmAccount::getOrganizationId, orgId).eq(AiDmAccount::getPlatform, platform);
        List<AiDmAccount> list = aiDmAccountMapper.selectListByLambda(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    private AiDmAccount requireAccount(String orgId, String accountId) {
        AiDmAccount account = aiDmAccountMapper.selectByPrimaryKey(accountId);
        if (account == null || !orgId.equals(account.getOrganizationId())) {
            throw new GenericException("账号不存在或无权访问");
        }
        return account;
    }

    private AiDmProfile findByNickname(String orgId, String nickname) {
        LambdaQueryWrapper<AiDmProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiDmProfile::getOrganizationId, orgId).eq(AiDmProfile::getNickname, nickname);
        List<AiDmProfile> list = aiDmProfileMapper.selectListByLambda(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    private AiDmProfile findProfile(String orgId, String profileId) {
        AiDmProfile profile = aiDmProfileMapper.selectByPrimaryKey(profileId);
        if (profile == null || !orgId.equals(profile.getOrganizationId())) {
            return null;
        }
        return profile;
    }

    private String messagesJson(List<DmRawMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "[]";
        }
        return JSON.toJSONString(messages);
    }

    private String extractPhone(List<DmRawMessage> messages) {
        if (messages == null) {
            return null;
        }
        for (DmRawMessage m : messages) {
            if (m == null || StringUtils.isBlank(m.getContent())) {
                continue;
            }
            Matcher matcher = PHONE_PATTERN.matcher(m.getContent());
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private String extractWechat(List<DmRawMessage> messages) {
        if (messages == null) {
            return null;
        }
        for (DmRawMessage m : messages) {
            if (m == null || StringUtils.isBlank(m.getContent())) {
                continue;
            }
            Matcher matcher = WECHAT_PATTERN.matcher(m.getContent());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

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

    /** 剥 ```json ... ``` 包裹，取首个 { 到末个 } 的 JSON 片段 */
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
            return t;
        }
        return t.substring(begin, end + 1);
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

    private DmAccountResponse toAccountResponse(AiDmAccount account) {
        DmAccountResponse response = new DmAccountResponse();
        response.setId(account.getId());
        response.setPlatform(account.getPlatform());
        response.setAccountName(account.getAccountName());
        response.setUniqueId(account.getUniqueId());
        response.setStatus(account.getStatus());
        response.setLastCrawlTime(account.getLastCrawlTime());
        response.setLastError(account.getLastError());
        return response;
    }

    private DmProfileResponse toProfileResponse(AiDmProfile profile) {
        DmProfileResponse response = new DmProfileResponse();
        response.setId(profile.getId());
        response.setNickname(profile.getNickname());
        response.setUniqueId(profile.getUniqueId());
        response.setFollowerCount(profile.getFollowerCount());
        response.setFollowingCount(profile.getFollowingCount());
        response.setLikedCount(profile.getLikedCount());
        response.setGender(profile.getGender());
        response.setBio(profile.getBio());
        response.setIsGroup(profile.getIsGroup());
        response.setPhone(profile.getPhone());
        response.setWechat(profile.getWechat());
        response.setStatus(profile.getStatus());
        response.setProfileJson(profile.getProfileJson());
        response.setClueId(profile.getClueId());
        response.setCreateTime(profile.getCreateTime());
        return response;
    }
}
