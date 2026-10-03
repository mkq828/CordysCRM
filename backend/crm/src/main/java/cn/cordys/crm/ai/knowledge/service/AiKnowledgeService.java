package cn.cordys.crm.ai.knowledge.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.knowledge.domain.AiKnowledgeChunk;
import cn.cordys.crm.ai.knowledge.domain.AiKnowledgeDoc;
import cn.cordys.crm.ai.knowledge.dto.request.AiKnowledgeAskRequest;
import cn.cordys.crm.ai.knowledge.dto.request.AiKnowledgeDocPageRequest;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeAnswerResponse;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeCitation;
import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeDocResponse;
import cn.cordys.crm.ai.knowledge.mapper.ExtAiKnowledgeChunkMapper;
import cn.cordys.crm.ai.knowledge.mapper.ExtAiKnowledgeDocMapper;
import cn.cordys.crm.ai.knowledge.parser.DocumentChunker;
import cn.cordys.crm.ai.knowledge.parser.DocumentTextExtractor;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 企业知识库服务（功能 4）：文档上传解析（PDF/Word/Markdown/TXT 抽文本 + 分块存库）+ 检索问答。
 * 问答复用额度→模型→provider→计费四步链路，先关键词粗筛候选块，再让模型基于块回答，出处由服务端按块编号回填，
 * 避免模型编造（与话术库 retrieve 同一范式）。检索底座为自建轻量（无向量库/embedding），后续可平滑升级真向量化。
 */
@Service
@Slf4j
public class AiKnowledgeService {

    /** 允许上传的文件类型（扩展名小写） */
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "md", "txt");

    /** 上传大小上限（20MB） */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    /** 单文档分块数上限，防止超大文档导致海量分块拖慢写入 */
    private static final int MAX_CHUNKS = 2000;

    private static final String STATUS_READY = "READY";

    /** 关键词召回时的候选块上限（命中数排序在服务层完成） */
    private static final int KEYWORD_CANDIDATE_LIMIT = 200;

    /** 最终送入模型的候选块上限 */
    private static final int CHUNK_CANDIDATE_LIMIT = 20;

    private static final int DEFAULT_TOP_K = 4;

    /** 出处片段最大长度默认值（字符），实际值可在系统参数 sys_parameter 的 ai.kb.snippet_max 配置 */
    private static final int DEFAULT_SNIPPET_MAX = 200;

    private static final String SNIPPET_MAX_PARAM_KEY = "ai.kb.snippet_max";

    /** 提问分词时过滤的常见停用词 */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "吗", "呢", "啊", "吧", "么", "什么", "怎么", "如何", "为什么", "哪些", "哪个",
            "我们", "你们", "他们", "请问", "一下", "关于", "以及", "这个", "那个", "一个", "可以", "能否");

    private static final String SYSTEM_PROMPT = """
            你是企业知识库问答助手。请仅依据用户提供的参考资料回答问题，条理清晰、简洁准确。
            若资料中未提及相关内容，明确回答「资料中未找到相关信息」，严禁编造。
            直接输出回答正文（Markdown 格式，可用要点列表），不要输出 JSON、不要任何前后缀、不要提及「资料编号」。
            """;

    @Resource
    private BaseMapper<AiKnowledgeDoc> docMapper;
    @Resource
    private BaseMapper<AiKnowledgeChunk> chunkMapper;
    @Resource
    private ExtAiKnowledgeDocMapper extDocMapper;
    @Resource
    private ExtAiKnowledgeChunkMapper extChunkMapper;
    @Resource
    private DocumentTextExtractor documentTextExtractor;
    @Resource
    private DocumentChunker documentChunker;
    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;
    @Resource
    private BaseMapper<Parameter> parameterMapper;

    // ==================== 文档维护 ====================

    public Pager<List<AiKnowledgeDocResponse>> page(AiKnowledgeDocPageRequest request, String orgId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<AiKnowledgeDocResponse> list = extDocMapper.selectPage(orgId, request.getKeyword());
        return PageUtils.setPageInfo(page, list);
    }

    /** 上传并同步解析：抽文本 + 分块 + 落库。解析失败直接抛异常，不落脏数据。 */
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocResponse upload(MultipartFile file, String orgId, String userId) {
        if (file == null || file.isEmpty()) {
            throw new GenericException("请上传文档");
        }
        String name = file.getOriginalFilename();
        String fileType = resolveFileType(name);
        if (!ALLOWED_TYPES.contains(fileType)) {
            throw new GenericException("仅支持 pdf/docx/md/txt 格式文档");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new GenericException("文档大小不能超过 20MB");
        }

        String text = documentTextExtractor.extract(file, fileType);
        List<String> chunkTexts = documentChunker.chunk(text);
        if (chunkTexts.size() > MAX_CHUNKS) {
            throw new GenericException("文档内容过大，请拆分后分批上传");
        }

        long now = System.currentTimeMillis();
        String docId = IDGenerator.nextStr();
        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setId(docId);
        doc.setOrganizationId(orgId);
        doc.setName(name);
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setStatus(STATUS_READY);
        doc.setChunkCount(chunkTexts.size());
        doc.setCreateUser(userId);
        doc.setUpdateUser(userId);
        doc.setCreateTime(now);
        doc.setUpdateTime(now);
        docMapper.insert(doc);

        for (int i = 0; i < chunkTexts.size(); i++) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setId(IDGenerator.nextStr());
            chunk.setOrganizationId(orgId);
            chunk.setDocId(docId);
            chunk.setSeq(i);
            chunk.setContent(chunkTexts.get(i));
            chunk.setCreateUser(userId);
            chunk.setUpdateUser(userId);
            chunk.setCreateTime(now);
            chunk.setUpdateTime(now);
            chunkMapper.insert(chunk);
        }
        return toResponse(doc);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, String orgId) {
        checkDoc(id, orgId);
        docMapper.deleteByPrimaryKey(id);
        extChunkMapper.deleteByDocId(orgId, id);
    }

    // ==================== 检索问答 ====================

    public AiKnowledgeAnswerResponse ask(AiKnowledgeAskRequest request, String orgId) {
        return doAsk(request, orgId, chunk -> { }).result();
    }

    /** 流式问答：回答正文直接流式输出（Markdown），出处由服务端按检索相关性回填相关片段。 */
    public AiStreamResult<AiKnowledgeAnswerResponse> askStream(AiKnowledgeAskRequest request, String orgId,
            Consumer<String> onChunk) {
        return doAsk(request, orgId, onChunk);
    }

    private AiStreamResult<AiKnowledgeAnswerResponse> doAsk(AiKnowledgeAskRequest request, String orgId,
            Consumer<String> onChunk) {
        if (StringUtils.isBlank(request.getQuestion())) {
            throw new GenericException("请输入问题");
        }
        int topK = request.getTopK() == null || request.getTopK() <= 0 ? DEFAULT_TOP_K : request.getTopK();
        String question = request.getQuestion().trim();
        List<String> keywords = tokenizeKeywords(question);

        List<AiKnowledgeChunk> chunks = retrieveChunks(orgId, keywords);
        if (chunks.isEmpty()) {
            throw new GenericException("知识库为空，请先上传文档");
        }
        Map<String, String> docNames = buildDocNameMap(chunks);

        List<AgentModel> models = agentModelService.resolveChatModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        StringBuilder content = new StringBuilder();
        AtomicBoolean emitted = new AtomicBoolean(false);
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
                llmRequest.setMessages(buildMessages(question, chunks));

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    content.append(chunk);
                    onChunk.accept(chunk);
                });
                aiQuotaService.record(orgId, AiQuotaConstant.AI_KB, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return new AiStreamResult<>(buildAnswer(content.toString(), chunks, keywords, docNames, topK), usage);
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("知识库问答模型调用中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("知识库问答模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("知识库问答全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    // ==================== 内部方法 ====================

    /** 关键词粗筛：先按命中块召回并按命中数排序；无命中时兜底取最近上传文档的最新块 */
    private List<AiKnowledgeChunk> retrieveChunks(String orgId, List<String> keywords) {
        List<AiKnowledgeChunk> matched = keywords.isEmpty()
                ? List.of()
                : extChunkMapper.selectByKeywords(orgId, keywords, KEYWORD_CANDIDATE_LIMIT);
        if (matched.isEmpty()) {
            return extChunkMapper.selectRecent(orgId, CHUNK_CANDIDATE_LIMIT);
        }
        matched.sort((a, b) -> Integer.compare(countHits(b.getContent(), keywords), countHits(a.getContent(), keywords)));
        return new ArrayList<>(matched.subList(0, Math.min(CHUNK_CANDIDATE_LIMIT, matched.size())));
    }

    /** 按标点/空白拆分提问，过滤停用词与单字，最多取 5 个关键词 */
    private List<String> tokenizeKeywords(String question) {
        String[] parts = question.split("[\\s，。！？、；：,.!?;:'\"（）()【】\\[\\]<>《》/\\\\|]+");
        List<String> keywords = new ArrayList<>();
        for (String part : parts) {
            String kw = part.trim();
            if (kw.length() < 2 || STOP_WORDS.contains(kw)) {
                continue;
            }
            keywords.add(kw);
            if (keywords.size() >= 5) {
                break;
            }
        }
        return keywords;
    }

    private int countHits(String content, List<String> keywords) {
        if (content == null) {
            return 0;
        }
        int hits = 0;
        for (String kw : keywords) {
            if (content.contains(kw)) {
                hits++;
            }
        }
        return hits;
    }

    private Map<String, String> buildDocNameMap(List<AiKnowledgeChunk> chunks) {
        Set<String> docIds = chunks.stream().map(c -> c.getDocId()).collect(Collectors.toSet());
        if (docIds.isEmpty()) {
            return Map.of();
        }
        List<AiKnowledgeDoc> docs = docMapper.selectByColumn("id", docIds.toArray(new String[0]));
        return docs.stream().collect(Collectors.toMap(d -> d.getId(),
                d -> d.getName() == null ? "" : d.getName(), (a, b) -> a));
    }

    private List<LlmMessage> buildMessages(String question, List<AiKnowledgeChunk> chunks) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("问题：").append(question).append("\n\n参考资料：\n");
        for (int i = 0; i < chunks.size(); i++) {
            sb.append(i + 1).append(". ").append(chunks.get(i).getContent()).append('\n');
        }
        sb.append("\n请仅依据上述资料回答。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 组装回答：正文取模型 Markdown 输出，出处按检索相关性取前 topK 块并截取相关片段（不整段暴露） */
    private AiKnowledgeAnswerResponse buildAnswer(String answer, List<AiKnowledgeChunk> chunks,
                                                  List<String> keywords, Map<String, String> docNames, int topK) {
        if (StringUtils.isBlank(answer)) {
            throw new GenericException("AI 未能生成回答，请重试");
        }
        AiKnowledgeAnswerResponse resp = new AiKnowledgeAnswerResponse();
        resp.setAnswer(answer.trim());
        List<AiKnowledgeCitation> citations = new ArrayList<>();
        int limit = Math.min(topK, chunks.size());
        for (int i = 0; i < limit; i++) {
            AiKnowledgeChunk chunk = chunks.get(i);
            AiKnowledgeCitation c = new AiKnowledgeCitation();
            c.setDocName(docNames.getOrDefault(chunk.getDocId(), ""));
            c.setContent(snippetOf(chunk.getContent(), keywords));
            citations.add(c);
        }
        resp.setCitations(citations);
        return resp;
    }

    /** 从块内抽取与问题关键词相关的句子作为出处片段；无关键词命中时退回开头一段（均截断到配置长度） */
    private String snippetOf(String content, List<String> keywords) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            return "";
        }
        int max = snippetMax();
        if (!keywords.isEmpty()) {
            String[] sentences = text.split("(?<=[。！？!?；;])");
            StringBuilder sb = new StringBuilder();
            for (String sentence : sentences) {
                if (StringUtils.isNotBlank(sentence) && countHits(sentence, keywords) > 0) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(sentence.trim());
                    if (sb.length() >= max) {
                        break;
                    }
                }
            }
            if (sb.length() > 0) {
                return trimTo(sb.toString(), max);
            }
        }
        return trimTo(text, max);
    }

    /** 读取出处片段长度（sys_parameter 的 ai.kb.snippet_max），未配置或非法时回退默认 200 */
    private int snippetMax() {
        try {
            Parameter param = parameterMapper.selectByPrimaryKey(SNIPPET_MAX_PARAM_KEY);
            if (param != null && StringUtils.isNotBlank(param.getParamValue())) {
                int value = Integer.parseInt(param.getParamValue().trim());
                return value > 0 ? value : DEFAULT_SNIPPET_MAX;
            }
        } catch (Exception e) {
            log.warn("解析知识库出处片段长度参数失败，使用默认值 {}", DEFAULT_SNIPPET_MAX, e);
        }
        return DEFAULT_SNIPPET_MAX;
    }

    private String trimTo(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    // ==================== 系统设置 ====================

    /** 读取出处片段长度（字），供前端设置回显 */
    public int getSnippetMax() {
        return snippetMax();
    }

    /** 更新出处片段长度（字），写入系统参数 sys_parameter（沿用 AiQuotaService.setParam 的删除后插入范式） */
    public void updateSnippetMax(int max) {
        int value = max > 0 ? max : DEFAULT_SNIPPET_MAX;
        parameterMapper.deleteByPrimaryKey(SNIPPET_MAX_PARAM_KEY);
        Parameter param = new Parameter();
        param.setParamKey(SNIPPET_MAX_PARAM_KEY);
        param.setParamValue(String.valueOf(value));
        param.setType("text");
        parameterMapper.insert(param);
    }

    private AiKnowledgeDoc checkDoc(String id, String orgId) {
        AiKnowledgeDoc doc = docMapper.selectByPrimaryKey(id);
        if (doc == null || !orgId.equals(doc.getOrganizationId())) {
            throw new GenericException("文档不存在");
        }
        return doc;
    }

    private AiKnowledgeDocResponse toResponse(AiKnowledgeDoc doc) {
        AiKnowledgeDocResponse resp = new AiKnowledgeDocResponse();
        resp.setId(doc.getId());
        resp.setName(doc.getName());
        resp.setFileType(doc.getFileType());
        resp.setFileSize(doc.getFileSize());
        resp.setStatus(doc.getStatus());
        resp.setChunkCount(doc.getChunkCount());
        resp.setErrorMsg(doc.getErrorMsg());
        resp.setCreateUser(doc.getCreateUser());
        resp.setCreateTime(doc.getCreateTime());
        return resp;
    }

    private String resolveFileType(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
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
