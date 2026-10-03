package cn.cordys.crm.ai.knowledge.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
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
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

/**
 * 企业知识库服务（功能 4）：文档上传解析（PDF/Word/Markdown/TXT 抽文本 + 分块存库）+ 检索问答。
 * 问答复用额度→模型→provider→计费四步链路，先关键词粗筛候选块，再让模型基于块回答，出处由服务端按块编号回填，
 * 避免模型编造（与话术库 retrieve 同一范式）。检索底座为自建轻量（无向量库/embedding），后续可平滑升级真向量化。
 */
@Service
@Slf4j
public class AiKnowledgeService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    /** 提问分词时过滤的常见停用词 */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "吗", "呢", "啊", "吧", "么", "什么", "怎么", "如何", "为什么", "哪些", "哪个",
            "我们", "你们", "他们", "请问", "一下", "关于", "以及", "这个", "那个", "一个", "可以", "能否");

    private static final String SYSTEM_PROMPT = """
            你是企业知识库问答助手。请仅依据用户提供的参考资料回答问题，条理清晰、简洁准确。\
            若资料中未提及相关内容，明确回答「资料中未找到相关信息」，严禁编造。输出严格 JSON（不要 markdown 代码块、不要任何多余文字），结构如下：
            {
              "answer": "回答内容",
              "citations": [1, 3]
            }
            citations 为引用的资料编号数组（可空），只输出上述 JSON 本身。
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
        if (StringUtils.isBlank(request.getQuestion())) {
            throw new GenericException("请输入问题");
        }
        int topK = request.getTopK() == null || request.getTopK() <= 0 ? DEFAULT_TOP_K : request.getTopK();

        List<AiKnowledgeChunk> chunks = retrieveChunks(request.getQuestion().trim(), orgId);
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
                llmRequest.setMessages(buildMessages(request.getQuestion().trim(), chunks));

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    content.append(chunk);
                });
                aiQuotaService.record(orgId, AiQuotaConstant.AI_KB, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return parseAnswer(content.toString(), chunks, docNames, topK);
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
    private List<AiKnowledgeChunk> retrieveChunks(String question, String orgId) {
        List<String> keywords = tokenizeKeywords(question);
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
        sb.append("\n请仅依据上述资料回答，并在 citations 中给出引用的资料编号。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 解析模型返回 JSON，用块编号反查出文档名与原文片段，保证出处权威不被模型编造 */
    private AiKnowledgeAnswerResponse parseAnswer(String text, List<AiKnowledgeChunk> chunks,
                                                   Map<String, String> docNames, int topK) {
        String json = extractJson(text);
        if (json == null) {
            throw new GenericException("AI 未能生成回答，请重试");
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            String answer = root.path("answer").asText(null);
            if (StringUtils.isBlank(answer)) {
                throw new GenericException("AI 未能生成回答，请重试");
            }
            AiKnowledgeAnswerResponse resp = new AiKnowledgeAnswerResponse();
            resp.setAnswer(answer.trim());
            List<AiKnowledgeCitation> citations = new ArrayList<>();
            JsonNode citNodes = root.get("citations");
            if (citNodes != null && citNodes.isArray()) {
                for (JsonNode cit : citNodes) {
                    int index = cit.asInt(-1);
                    if (index <= 0 || index > chunks.size()) {
                        continue;
                    }
                    AiKnowledgeChunk chunk = chunks.get(index - 1);
                    AiKnowledgeCitation c = new AiKnowledgeCitation();
                    c.setDocName(docNames.getOrDefault(chunk.getDocId(), ""));
                    c.setContent(chunk.getContent());
                    citations.add(c);
                    if (citations.size() >= topK) {
                        break;
                    }
                }
            }
            resp.setCitations(citations);
            return resp;
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.warn("知识库回答结果 JSON 解析失败", e);
            throw new GenericException("AI 未能生成回答，请重试");
        }
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
