package cn.cordys.crm.ai.knowledge.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AdvisorWebSource;
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
import cn.cordys.crm.ai.llm.LlmEmbedRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmRerankRequest;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.search.DeepSeekSearchClient;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /** 提问分词的关键词上限（中文按 2-gram 切分后数量变多，放宽以提高召回） */
    private static final int MAX_KEYWORDS = 20;

    /** 最终送入模型的候选块上限 */
    private static final int CHUNK_CANDIDATE_LIMIT = 20;

    private static final int DEFAULT_TOP_K = 4;

    /** 出处片段最大长度默认值（字符），实际值可在系统参数 sys_parameter 的 ai.kb.snippet_max 配置 */
    private static final int DEFAULT_SNIPPET_MAX = 200;

    private static final String SNIPPET_MAX_PARAM_KEY = "ai.kb.snippet_max";

    /** embedding 模型名（sys_parameter ai.kb.embedding_model 可配，默认 text-embedding-v3） */
    private static final String EMBEDDING_MODEL_PARAM_KEY = "ai.kb.embedding_model";
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-v3";

    /** rerank 模型名（sys_parameter ai.kb.rerank_model 可配，默认 qwen3-rerank） */
    private static final String RERANK_MODEL_PARAM_KEY = "ai.kb.rerank_model";
    private static final String DEFAULT_RERANK_MODEL = "qwen3-rerank";

    /** rerank 精排的候选块上限：只对融合排序前 N 个做精排，控制调用成本 */
    private static final int RERANK_CANDIDATE_LIMIT = 10;

    /** 向量检索最小余弦相似度阈值，低于该值视为不相关，避免低相关片段被当作出处引用 */
    private static final double MIN_SIMILARITY = 0.5;

    /** 懒加载回填每批上限：避免老数据首查时批量向量化拖慢响应 */
    private static final int BACKFILL_BATCH_LIMIT = 5;

    /** 提问分词时过滤的常见停用词 */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "吗", "呢", "啊", "吧", "么", "什么", "怎么", "如何", "为什么", "哪些", "哪个",
            "我们", "你们", "他们", "请问", "一下", "关于", "以及", "这个", "那个", "一个", "可以", "能否");

    private static final String SYSTEM_PROMPT = """
            你是企业知识库问答助手。请依据用户提供的参考资料回答问题，条理清晰、简洁准确。
            参考资料分为「知识库资料」和「联网时效信息」两类：知识库资料优先；当知识库资料对时效性问题（最新政策、近期数据、当前动态等）覆盖不足时，可引用联网时效信息补充，并在回答中标注「据联网公开信息」。
            紧扣问题焦点作答：只回答用户实际问的点（问价格只答价格、问流程只答流程、问时间只答时间），参考资料中与问题焦点无关的内容不要展开、不要罗列。
            若两类资料均未提及相关内容，明确回答「资料中未找到相关信息」，严禁编造。
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
    @Resource
    private DeepSeekSearchClient deepSeekSearchClient;

    // ==================== 文档维护 ====================

    public Pager<List<AiKnowledgeDocResponse>> page(AiKnowledgeDocPageRequest request, String orgId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<AiKnowledgeDocResponse> list = extDocMapper.selectPage(orgId, request.getKeyword());
        return PageUtils.setPageInfo(page, list);
    }

    /** 上传并同步解析：抽文本 + 分块 + 落库。解析失败直接抛异常，不落脏数据。 */
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocResponse upload(MultipartFile file, String orgId, String userId,
            String version, LocalDate effectiveDate) {
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
        doc.setVersion(StringUtils.isBlank(version) ? null : version.trim());
        doc.setEffectiveDate(effectiveDate);
        doc.setRevoked(0);
        doc.setStatus(STATUS_READY);
        doc.setChunkCount(chunkTexts.size());
        doc.setCreateUser(userId);
        doc.setUpdateUser(userId);
        doc.setCreateTime(now);
        doc.setUpdateTime(now);
        docMapper.insert(doc);
        // 同名新版本上传后自动废止旧版（排除自身），旧版不再参与检索
        extDocMapper.revokeSameName(orgId, name, docId);

        AgentModel embedAgent = agentModelService.resolveEmbeddingModel(orgId);
        String embedModel = embeddingModelName();

        for (int i = 0; i < chunkTexts.size(); i++) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setId(IDGenerator.nextStr());
            chunk.setOrganizationId(orgId);
            chunk.setDocId(docId);
            chunk.setSeq(i);
            chunk.setContent(chunkTexts.get(i));
            chunk.setVector(embedVector(chunkTexts.get(i), embedModel, embedAgent));
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

        List<AiKnowledgeChunk> chunks = retrieveChunks(orgId, question, keywords);
        if (chunks.isEmpty() && !hasAnyChunk(orgId)) {
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

        // 联网补充时效信息：勾选后按问题联网检索公开信息注入（未配 key / 失败静默降级，不阻断问答）
        List<AdvisorWebSource> webSources = List.of();
        if (Boolean.TRUE.equals(request.getWebSearch())) {
            String query = question.length() > 128 ? question.substring(0, 128) : question;
            webSources = deepSeekSearchClient.search(query);
            if (!webSources.isEmpty()) {
                aiQuotaService.record(orgId, AiQuotaConstant.AI_KB, AiQuotaConstant.SEARCH_MODEL_CODE,
                        getSearchTokensPerCall(), 0L, userId);
            }
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
                llmRequest.setMessages(buildMessages(question, chunks, webSources));

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

    /**
     * 供其他 AI 功能（如 AI 员工）复用：按提问关键词检索企业知识库，返回拼接好的参考资料文本。
     * 只做关键词召回与相关性排序，不做模型生成，便于编排层把知识作为上下文注入；无相关命中时返回空串。
     */
    public String retrieveContext(String orgId, String question) {
        if (StringUtils.isBlank(question)) {
            return "";
        }
        List<String> keywords = tokenizeKeywords(question.trim());
        List<AiKnowledgeChunk> chunks = retrieveChunks(orgId, question.trim(), keywords);
        if (chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            sb.append(i + 1).append(". ").append(chunks.get(i).getContent()).append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * 供自动任务等编排场景复用：按提问关键词检索企业知识库，返回带出处（文档名 + 截取片段）的引用列表。
     * 与 {@link #retrieveContext} 同召回口径，但保留 docName 便于 prompt 注入时标注来源；无命中返回空列表。
     */
    public List<AiKnowledgeCitation> retrieveCitations(String orgId, String question) {
        if (StringUtils.isBlank(question)) {
            return List.of();
        }
        List<String> keywords = tokenizeKeywords(question.trim());
        List<AiKnowledgeChunk> chunks = retrieveChunks(orgId, question.trim(), keywords);
        if (chunks.isEmpty()) {
            return List.of();
        }
        Map<String, String> docNames = buildDocNameMap(chunks);
        List<AiKnowledgeCitation> citations = new ArrayList<>();
        for (AiKnowledgeChunk chunk : chunks) {
            AiKnowledgeCitation citation = new AiKnowledgeCitation();
            citation.setDocName(docNames.getOrDefault(chunk.getDocId(), ""));
            citation.setContent(snippetOf(chunk.getContent(), keywords));
            citations.add(citation);
        }
        return citations;
    }

    // ==================== 内部方法 ====================

    /**
     * 检索召回：向量检索（语义，覆盖同义不同词）与关键词检索（字面，覆盖无向量的老数据）结果合并去重，
     * 按综合分排序（关键词命中率×2 + 向量相似度），兼顾「字面强命中」与「语义强相关」两类信号。
     * 无相关命中返回空，由调用方区分「知识库为空」与「检索无命中」。
     */
    private List<AiKnowledgeChunk> retrieveChunks(String orgId, String question, List<String> keywords) {
        // 1) 向量检索（语义召回，带相似度）
        VectorSearchResult vectorResult = retrieveByVector(orgId, question, CHUNK_CANDIDATE_LIMIT);
        List<ScoredChunk> vectorHits = vectorResult.hits();
        // 2) 关键词检索（字面召回，能覆盖无向量的老数据）
        List<AiKnowledgeChunk> keywordHits = keywords.isEmpty()
                ? List.of()
                : extChunkMapper.selectByKeywords(orgId, keywords, KEYWORD_CANDIDATE_LIMIT);

        // 合并去重，并保留向量命中块的相似度供融合排序
        Map<String, AiKnowledgeChunk> merged = new LinkedHashMap<>();
        Map<String, Double> simOf = new HashMap<>();
        for (ScoredChunk sc : vectorHits) {
            merged.put(sc.chunk().getId(), sc.chunk());
            simOf.put(sc.chunk().getId(), sc.similarity());
        }
        // 向量检索正常执行时，有向量块的语义相关性已由相似度阈值裁决（<0.5 不召回）；
        // 关键词命中只兜底「无向量」的老块，避免「政策」这类泛词把语义不相关的向量块拉回来。
        for (AiKnowledgeChunk chunk : keywordHits) {
            if (vectorResult.executed() && chunk.getVector() != null) {
                continue;
            }
            merged.putIfAbsent(chunk.getId(), chunk);
        }
        if (merged.isEmpty()) {
            return List.of();
        }

        List<AiKnowledgeChunk> candidates = new ArrayList<>(merged.values());
        candidates.sort((a, b) -> Double.compare(
                AiKnowledgeFusionScorer.fusionScore(b.getContent(), keywords, simOf.getOrDefault(b.getId(), 0.0)),
                AiKnowledgeFusionScorer.fusionScore(a.getContent(), keywords, simOf.getOrDefault(a.getId(), 0.0))));
        List<AiKnowledgeChunk> result = new ArrayList<>(candidates.subList(0, Math.min(CHUNK_CANDIDATE_LIMIT, candidates.size())));
        // 3) rerank 精排（可选，模型未配置或失败时自动降级回融合排序）
        result = rerank(result, question, orgId);
        // 4) 懒加载回填：命中块缺向量时现场补（老文档逐渐向量化，零停机）
        backfillVectors(orgId, result);
        return result;
    }

    /** 判断租户知识库是否存在任意分块，用于区分「知识库为空」与「检索无相关命中」 */
    private boolean hasAnyChunk(String orgId) {
        return !extChunkMapper.selectRecent(orgId, 1).isEmpty();
    }

    /** 向量命中块 + 相似度（融合排序用） */
    private record ScoredChunk(AiKnowledgeChunk chunk, double similarity) {}

    /** 向量检索结果：hits 为相似度达阈值的命中块；executed 表示「embedding 可用 + 有向量数据 + 已成功计算相似度」 */
    private record VectorSearchResult(List<ScoredChunk> hits, boolean executed) {
        static final VectorSearchResult NOT_EXECUTED = new VectorSearchResult(List.of(), false);
    }

    /** 向量检索：提问向量化后与租户已向量化分块做内存余弦相似度取 top-K；embedding 不可用或失败返回未执行 */
    private VectorSearchResult retrieveByVector(String orgId, String question, int topK) {
        AgentModel embedAgent = agentModelService.resolveEmbeddingModel(orgId);
        if (embedAgent == null) {
            return VectorSearchResult.NOT_EXECUTED;
        }
        float[] query;
        try {
            query = embedToVector(question, embeddingModelName(), embedAgent);
        } catch (Exception e) {
            log.warn("知识库提问向量化失败，降级关键词检索: {}", e.getMessage());
            return VectorSearchResult.NOT_EXECUTED;
        }
        List<AiKnowledgeChunk> candidates = extChunkMapper.selectVectorizedByOrg(orgId);
        if (candidates.isEmpty()) {
            return VectorSearchResult.NOT_EXECUTED;
        }
        List<ScoredChunk> scored = new ArrayList<>();
        for (AiKnowledgeChunk chunk : candidates) {
            float[] vec = parseVector(chunk.getVector());
            if (vec == null || vec.length != query.length) {
                continue;
            }
            scored.add(new ScoredChunk(chunk, cosineSimilarity(query, vec)));
        }
        if (scored.isEmpty()) {
            return VectorSearchResult.NOT_EXECUTED;
        }
        scored.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
        // 已按相似度降序：首个低于阈值即视为无相关命中，后续更低无需再看
        List<ScoredChunk> hits = new ArrayList<>();
        for (ScoredChunk sc : scored) {
            if (sc.similarity() < MIN_SIMILARITY) {
                break;
            }
            hits.add(sc);
            if (hits.size() >= topK) {
                break;
            }
        }
        return new VectorSearchResult(hits, true);
    }

    /**
     * rerank 精排：复用阿里云百炼凭证（与 embedding 同一端点），把融合排序后的前 RERANK_CANDIDATE_LIMIT 个
     * 候选交给 qwen3-rerank 按与提问的相关性重排并替换原顺序。模型未配置 / 调用失败 / 结果异常时返回原列表（降级）。
     */
    private List<AiKnowledgeChunk> rerank(List<AiKnowledgeChunk> candidates, String question, String orgId) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        AgentModel rerankAgent = agentModelService.resolveEmbeddingModel(orgId);
        if (rerankAgent == null) {
            return candidates;
        }
        String model = rerankModelName();
        if (StringUtils.isBlank(model)) {
            return candidates;
        }
        int n = Math.min(RERANK_CANDIDATE_LIMIT, candidates.size());
        List<AiKnowledgeChunk> head = new ArrayList<>(candidates.subList(0, n));
        try {
            LlmProvider provider = llmProviderFactory.get(rerankAgent.getProvider());
            LlmRerankRequest req = new LlmRerankRequest();
            req.setModel(model);
            req.setBaseUrl(rerankBaseUrl(rerankAgent.getApiUrl()));
            req.setApiKey(rerankAgent.getApiKey());
            req.setQuery(question);
            req.setDocuments(head.stream().map(AiKnowledgeChunk::getContent).toList());
            req.setTopN(n);
            List<Integer> order = provider.rerank(req);
            if (order == null || order.size() != n) {
                return candidates;
            }
            List<AiKnowledgeChunk> reranked = new ArrayList<>(n);
            for (int idx : order) {
                if (idx < 0 || idx >= n) {
                    return candidates;
                }
                reranked.add(head.get(idx));
            }
            reranked.addAll(candidates.subList(n, candidates.size()));
            return reranked;
        } catch (Exception e) {
            log.warn("知识库 rerank 精排失败，降级融合排序: {}", e.getMessage());
            return candidates;
        }
    }

    /** 阿里云 apiUrl 是 OpenAI 兼容的 /compatible-mode/v1，rerank 走 Cohere 兼容 /compatible-api/v1，替换路径段 */
    private String rerankBaseUrl(String apiUrl) {
        if (StringUtils.isNotBlank(apiUrl) && apiUrl.contains("compatible-mode")) {
            return apiUrl.replace("compatible-mode", "compatible-api");
        }
        return apiUrl;
    }

    /** 懒加载回填：对缺向量的候选块现场向量化落库（老文档逐渐向量化，零停机），失败静默 */
    private void backfillVectors(String orgId, List<AiKnowledgeChunk> chunks) {
        AgentModel embedAgent = agentModelService.resolveEmbeddingModel(orgId);
        if (embedAgent == null) {
            return;
        }
        String embedModel = embeddingModelName();
        int filled = 0;
        for (AiKnowledgeChunk chunk : chunks) {
            if (StringUtils.isNotBlank(chunk.getVector())) {
                continue;
            }
            String vector = embedVector(chunk.getContent(), embedModel, embedAgent);
            if (vector != null) {
                extChunkMapper.updateVector(chunk.getId(), vector);
                if (++filled >= BACKFILL_BATCH_LIMIT) {
                    break;
                }
            }
        }
    }

    /** 调用 provider 向量化（抛异常由调用方处理） */
    private float[] embedToVector(String text, String embedModel, AgentModel embedAgent) throws Exception {
        LlmProvider provider = llmProviderFactory.get(embedAgent.getProvider());
        LlmEmbedRequest req = new LlmEmbedRequest();
        req.setModel(embedModel);
        req.setBaseUrl(embedAgent.getApiUrl());
        req.setApiKey(embedAgent.getApiKey());
        req.setText(text);
        return provider.embed(req);
    }

    /** 向量化并序列化为 JSON 字符串；模型未配置或调用失败返回 null（该块降级关键词检索） */
    private String embedVector(String text, String embedModel, AgentModel embedAgent) {
        if (embedAgent == null || StringUtils.isBlank(embedModel)) {
            return null;
        }
        try {
            return JSON.MAPPER.writeValueAsString(embedToVector(text, embedModel, embedAgent));
        } catch (Exception e) {
            log.warn("知识库分块向量化失败，该块降级关键词检索: {}", e.getMessage());
            return null;
        }
    }

    /** 读取 embedding 模型名（sys_parameter ai.kb.embedding_model），未配置或非法时回退默认 */
    private String embeddingModelName() {
        try {
            Parameter param = parameterMapper.selectByPrimaryKey(EMBEDDING_MODEL_PARAM_KEY);
            if (param != null && StringUtils.isNotBlank(param.getParamValue())) {
                return param.getParamValue().trim();
            }
        } catch (Exception e) {
            log.warn("解析 embedding 模型名参数失败，使用默认 {}", DEFAULT_EMBEDDING_MODEL, e);
        }
        return DEFAULT_EMBEDDING_MODEL;
    }

    /** 读取 rerank 模型名（sys_parameter ai.kb.rerank_model），未配置或非法时回退默认 */
    private String rerankModelName() {
        try {
            Parameter param = parameterMapper.selectByPrimaryKey(RERANK_MODEL_PARAM_KEY);
            if (param != null && StringUtils.isNotBlank(param.getParamValue())) {
                return param.getParamValue().trim();
            }
        } catch (Exception e) {
            log.warn("解析 rerank 模型名参数失败，使用默认 {}", DEFAULT_RERANK_MODEL, e);
        }
        return DEFAULT_RERANK_MODEL;
    }

    /** 解析分块向量 JSON 为浮点数组；非法或为空返回 null */
    private float[] parseVector(String vectorJson) {
        if (StringUtils.isBlank(vectorJson)) {
            return null;
        }
        try {
            return JSON.MAPPER.readValue(vectorJson, float[].class);
        } catch (Exception e) {
            log.warn("解析分块向量失败，跳过该块: {}", e.getMessage());
            return null;
        }
    }

    /** 余弦相似度（调用方已保证维度一致） */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 按标点/空白拆分提问，再对纯中文片段做 2-gram 切分（避免整句中文长关键词如「代理记账多少钱」永不命中内容里的
     * 「代理记账」等子串），过滤停用词与单字，最多取 {@link #MAX_KEYWORDS} 个关键词。
     */
    private List<String> tokenizeKeywords(String question) {
        String[] parts = question.split("[\\s，。！？、；：,.!?;:'\"（）()【】\\[\\]<>《》/\\\\|]+");
        List<String> keywords = new ArrayList<>();
        for (String part : parts) {
            String kw = part.trim();
            if (kw.isEmpty()) {
                continue;
            }
            if (containsAscii(kw)) {
                // 含 ASCII（如 CRM、Q4、HR制度）保留整段，统一小写
                addKeyword(keywords, kw.toLowerCase());
            } else if (kw.length() == 2) {
                addKeyword(keywords, kw);
            } else {
                // 纯中文长度 >=3：生成重叠 2-gram，如「代理记账多少钱」→ 代理/理记/记账/账多/多少/少钱
                for (int i = 0; i + 1 < kw.length() && keywords.size() < MAX_KEYWORDS; i++) {
                    addKeyword(keywords, kw.substring(i, i + 2));
                }
            }
            if (keywords.size() >= MAX_KEYWORDS) {
                break;
            }
        }
        return keywords;
    }

    /** 是否含 ASCII 字符（用于区分纯中文片段与带英文/数字的片段） */
    private boolean containsAscii(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < 128) {
                return true;
            }
        }
        return false;
    }

    /** 加入一个关键词（去停用词、去重） */
    private void addKeyword(List<String> keywords, String kw) {
        if (kw.length() < 2 || STOP_WORDS.contains(kw) || keywords.contains(kw)) {
            return;
        }
        keywords.add(kw);
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

    private List<LlmMessage> buildMessages(String question, List<AiKnowledgeChunk> chunks,
            List<AdvisorWebSource> webSources) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("问题：").append(question).append("\n\n参考资料：\n");
        if (chunks.isEmpty()) {
            sb.append("（无知识库相关资料）\n");
        } else {
            for (int i = 0; i < chunks.size(); i++) {
                sb.append(i + 1).append(". ").append(chunks.get(i).getContent()).append('\n');
            }
        }
        if (webSources != null && !webSources.isEmpty()) {
            sb.append("\n联网时效信息（供知识库资料覆盖不足时补充，引用请标注「据联网公开信息」）：\n");
            int index = 1;
            for (AdvisorWebSource source : webSources) {
                sb.append(index++).append(". 《")
                        .append(StringUtils.defaultIfBlank(source.getTitle(), "无标题")).append("》");
                if (StringUtils.isNotBlank(source.getSnippet())) {
                    sb.append("——").append(source.getSnippet());
                }
                if (StringUtils.isNotBlank(source.getContent())) {
                    sb.append("\n   正文：").append(source.getContent());
                }
                if (StringUtils.isNotBlank(source.getUrl())) {
                    sb.append("（来源：").append(source.getUrl()).append("）");
                }
                sb.append('\n');
            }
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
                if (StringUtils.isNotBlank(sentence) && AiKnowledgeFusionScorer.countHits(sentence, keywords) > 0) {
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

    /** 1 次联网搜索折算 token 数（可配置，非法回退默认值），与军师/订阅场景同口径 */
    private long getSearchTokensPerCall() {
        try {
            Parameter param = parameterMapper.selectByPrimaryKey(AiQuotaConstant.PARAM_SEARCH_TOKENS_PER_CALL);
            if (param != null && StringUtils.isNotBlank(param.getParamValue())) {
                return Long.parseLong(param.getParamValue().trim());
            }
        } catch (Exception e) {
            log.warn("解析搜索折算 token 参数失败，使用默认值", e);
        }
        return AiQuotaConstant.DEFAULT_SEARCH_TOKENS_PER_CALL;
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

    /** 读取 rerank 模型名（sys_parameter ai.kb.rerank_model），供前端设置回显 */
    public String getRerankModel() {
        return rerankModelName();
    }

    /** 更新 rerank 模型名，写入系统参数 sys_parameter（留空回退默认 qwen3-rerank） */
    public void updateRerankModel(String model) {
        String value = StringUtils.isBlank(model) ? DEFAULT_RERANK_MODEL : model.trim();
        parameterMapper.deleteByPrimaryKey(RERANK_MODEL_PARAM_KEY);
        Parameter param = new Parameter();
        param.setParamKey(RERANK_MODEL_PARAM_KEY);
        param.setParamValue(value);
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
        resp.setVersion(doc.getVersion());
        resp.setEffectiveDate(doc.getEffectiveDate());
        resp.setRevoked(doc.getRevoked());
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
