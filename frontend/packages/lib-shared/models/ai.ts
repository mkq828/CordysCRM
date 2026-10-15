import type { CommonList, TableQueryParams } from './common';

export interface AgentChatStreamParams {
  message: string;
  conversationId?: string;
  /** 请求级幂等键（本轮唯一）。用于未产生 runId 前定位取消与保存兜底。 */
  requestId: string;
  mcpIds?: string[];
  attachmentIds?: string[];
  picIds?: string[];
}

export interface SmartFocusParams {
  focus: string;
}

export interface AgentChatStreamOptions {
  signal?: AbortSignal; // 浏览器侧中断连接
  onSession?: (sessionId: string, conversationId?: string) => void;
}

export interface AgentChatCancelParams {
  conversationId?: string;
  sessionId?: string;
  /** 请求级幂等键（本轮唯一）。runId 未产生时用于按 requestId 取消。 */
  requestId: string;
}

export interface AgentChatRunData {
  conversationId: string;
  runId: string;
  userMessageId?: string;
  assistantMessageId?: string;
}

export interface AgentChatConfirmData {
  dialogId: string;
  conversationId?: string;
  orgId?: string;
  sessionId?: string;
  userId?: string;
  confirmation?: boolean;
  items: AgentChatConfirmItem[];
  createdAt?: number;
}

export interface AgentChatConfirmRequest {
  outcome: 'ANSWERED' | 'CONFIRMED' | 'CANCELLED';
  answers: Record<string, string>;
}

export interface AgentChatConfirmItem {
  prompt: string;
  title: string;
  selectionType: 'SINGLE' | 'MULTIPLE';
  options?: AgentChatConfirmOption[];
  textInput?: boolean;
}

export interface AgentChatConfirmOption {
  label: string;
  description?: string;
  value: string;
}

export interface AgentChatProgressData {
  schemaVersion?: number;
  sequence: number;
  actionId: string;
  stage: string;
  status: string;
  title: string;
  description?: string;
  timestamp?: number;
  details?: {
    input?: string;
    output?: string;
    [key: string]: unknown;
  };
}

export interface AgentChatDoneData {
  runId?: string;
  output?: number;
  conversationId?: string;
  input?: number;
  assistantMessageId?: string;
  totalTokens?: number; // Tokens 消耗
  /** 流式结束后回传的结构化结果（会话军师分析 / 获客物料包 / 知识库出处等） */
  payload?: unknown;
}

export interface AgentChatStreamEvent {
  type: 'run' | 'progress' | 'chunk' | 'confirm' | 'error' | 'done';
  content?: string;
  conversationId?: string;
  sessionId?: string;
  run?: AgentChatRunData;
  progress?: AgentChatProgressData;
  confirm?: AgentChatConfirmData;
  data?: AgentChatDoneData;
  errorMessage?: string;
  raw?: unknown;
}

export type AgentConversationQueryRequest = TableQueryParams & {
  /** 会话分组码：chat / ai_advisor / ai_kb / ai_acquire */
  featureCode?: string;
};

export interface AgentConversationItem {
  id: string;
  createUser?: string;
  createUserName?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  organizationId?: string;
  userId?: string;
  title: string;
  featureCode?: string;
  localPending?: boolean;
}

export type AgentConversationPageResult = CommonList<AgentConversationItem>;

export type AgentConversationMessageStatus = 'done' | 'stopped' | 'error';

export interface AgentConversationMessage {
  id: string;
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  content: string;
  inputTokens?: number | null;
  outputTokens?: number | null;
  totalTokens?: number | null;
  organizationId?: string;
  role: 'USER' | 'ASSISTANT';
  conversationId: string;
  runId?: string;
  helpful?: boolean | null;
  status?: AgentConversationMessageStatus;
  /** 结构化结果 JSON 字符串（会话军师分析 / 获客物料包 / 知识库出处） */
  payload?: string | null;
}

export interface AgentConversationDetail {
  messages: AgentConversationMessage[];
  conversation: AgentConversationItem;
}

export interface AgentMcpConfigItem {
  id: string;
  name: string;
  description?: string;
}

export interface AgentActionSuggestionItem {
  id: string;
  organizationId?: string;
  createTime?: number;
  summary?: string;
  createUser?: string;
  content?: string;
  topic?: string;
  userId?: string;
  actions?: string;
  priority?: number;
}

export interface AgentActionApproveItem {
  id: string;
  summary?: string;
  topic?: string;
  createUser?: string;
  type?: string;
  userId?: string;
  organizationId?: string;
  createTime?: number;
  content?: string;
}

/** AI 销售会话军师分析请求：粘贴文本 + 可选截图附件 id */
export interface SalesAdvisorAnalyzeParams {
  message?: string;
  picIds?: string[];
  conversationId?: string;
  customerId?: string;
}

/** AI 销售会话军师结构化分析结果 */
export interface SalesAdvisorAnalyzeResult {
  intentScore?: string;
  signals?: string[];
  objections?: string[];
  emotion?: string;
  competitorMentions?: string[];
  churnRisk?: string;
  suggestedScripts?: string[];
  scriptRecommendations?: ScriptRecommend[];
  rawAnalysis?: string;
}

/** AI 销售会话军师查数请求：一句大白话（例如「张三最近跟进到哪了」） */
export interface SalesAdvisorQueryParams {
  question: string;
  conversationId?: string;
}

/** AI 销售会话军师查数响应：自然语言回答 + 可折叠数据来源 */
export interface SalesAdvisorQueryResult {
  answer: string;
  queried: boolean;
  columns: string[];
  rows: (string | number | boolean | null)[][];
  conversationId?: string;
}

/** 销售话术库-话术条目 */
export interface AiSalesScript {
  id?: string;
  category?: string;
  title?: string;
  content?: string;
  source?: string;
  createTime?: number;
  updateTime?: number;
  createUser?: string;
  updateUser?: string;
}

/** 销售话术库-分页查询参数 */
export interface AiSalesScriptPageParams {
  current?: number;
  pageSize?: number;
  keyword?: string;
  category?: string;
}

/** 销售话术库-新增/更新参数 */
export interface AiSalesScriptSaveParams {
  id?: string;
  category?: string;
  title: string;
  content: string;
  source?: string;
}

/** 销售话术库-检索参数 */
export interface AiSalesScriptRetrieveParams {
  scenario: string;
  category?: string;
  topK?: number;
}

/** 销售话术库-检索推荐结果 */
export interface ScriptRecommend {
  title?: string;
  content?: string;
  source?: string;
  originalContent?: string;
}

/** AI 获客内容生成-请求参数 */
export interface AiContentGenerateParams {
  industry: string;
  product: string;
  platform?: 'douyin' | 'xiaohongshu' | 'moments';
  topicCount?: number;
  conversationId?: string;
}

/** AI 获客内容生成-单条内容（一份可直接发布的物料包） */
export interface AiContentItem {
  topic?: string;
  title?: string;
  copy?: string;
  imageCopy?: string;
  coverCopy?: string;
  hashtags?: string[];
  bestTime?: string;
  script?: string;
}

/** AI 获客内容生成-结果 */
export interface AiContentGenerateResult {
  contents?: AiContentItem[];
  rawContent?: string;
}

/** AI 方案/PPT-方案大纲区块 */
export interface AiPptSection {
  key?: string;
  title?: string;
  content?: string;
}

/** AI 方案/PPT-生成方案大纲请求 */
export interface AiPptOutlineParams {
  customerName?: string;
  industry: string;
  product: string;
  background: string;
  budget?: string;
  competitor?: string;
  focus?: string;
  conversationId?: string;
}

/** AI 方案/PPT-方案大纲结果 */
export interface AiPptOutlineResult {
  title?: string;
  sections?: AiPptSection[];
  rawContent?: string;
}

/** AI 方案/PPT-生成 PPT 逐页请求 */
export interface AiPptSlidesParams {
  title?: string;
  sections?: AiPptSection[];
  conversationId?: string;
}

/** AI 方案/PPT-单页 */
export interface AiPptSlide {
  title?: string;
  bullets?: string[];
  notes?: string;
}

/** AI 方案/PPT-逐页结果 */
export interface AiPptSlidesResult {
  title?: string;
  slides?: AiPptSlide[];
  rawContent?: string;
}

/** 企业知识库-文档 */
export interface AiKnowledgeDoc {
  id?: string;
  name?: string;
  fileType?: string;
  fileSize?: number;
  version?: string;
  effectiveDate?: string;
  revoked?: number;
  status?: string;
  chunkCount?: number;
  errorMsg?: string;
  createUser?: string;
  createTime?: number;
}

/** 企业知识库-文档分页查询参数 */
export interface AiKnowledgeDocPageParams {
  current?: number;
  pageSize?: number;
  keyword?: string;
}

/** 企业知识库-问答请求 */
export interface AiKnowledgeAskParams {
  question: string;
  topK?: number;
  conversationId?: string;
}

/** 企业知识库-问答出处引用 */
export interface AiKnowledgeCitation {
  docName?: string;
  content?: string;
}

/** 企业知识库-问答结果 */
export interface AiKnowledgeAnswerResult {
  answer?: string;
  citations?: AiKnowledgeCitation[];
}

/** 企业知识库-设置（出处片段长度、rerank 模型名） */
export interface AiKnowledgeConfig {
  snippetMax?: number;
  rerankModel?: string;
}

/** AI 员工-角色目录-部门导航项 */
export interface AiEmployeeDivision {
  division: string;
  count: number;
}

/** AI 员工-角色目录-列表项 */
export interface AiEmployeeItem {
  id: string;
  division: string;
  name: string;
  description?: string;
  color?: string;
  emoji?: string;
  vibe?: string;
}

/** AI 员工-角色目录-详情（含完整系统提示词） */
export interface AiEmployeeDetail extends AiEmployeeItem {
  systemPrompt?: string;
}

/** AI 员工-岗位 Agent */
export interface AiEmployeeAgent {
  id: string;
  name: string;
  employeeId: string;
  employeeName?: string;
  employeeEmoji?: string;
  employeeColor?: string;
  /** 岗位描述（添加时填写；为空时回退到 employeeDescription） */
  description?: string;
  /** 关联角色描述（角色目录里该岗位的描述，作为 description 为空时的回退） */
  employeeDescription?: string;
  customPrompt?: string;
  knowledgeEnabled?: number;
  sort?: number;
  createTime?: number;
  updateTime?: number;
}

/** AI 员工-岗位 Agent 新增/更新参数 */
export interface AiEmployeeAgentSaveParams {
  id?: string;
  name: string;
  employeeId: string;
  customPrompt?: string;
  description?: string;
  knowledgeEnabled?: boolean;
  sort?: number;
}

/** AI 员工-岗位 Agent 对话请求 */
export interface AiEmployeeChatParams {
  message: string;
  conversationId?: string;
  /** 此前同事发言（虚拟会议室按顺序注入，供当前岗位参考以避免重复） */
  context?: string;
  /** 会议阶段（speak=发言轮，discuss=讨论轮，空=普通对话） */
  stage?: 'speak' | 'discuss';
}

/** AI 员工-客户字段修改建议中的单项（old→new） */
export interface AgentUpdateChange {
  fieldId: string;
  fieldLabel: string;
  oldValue?: string;
  newValue: string;
}

/** AI 员工-客户字段修改建议（经 done.payload 下发，用户确认后才落库） */
export interface AgentUpdateProposal {
  type: string;
  customerId: string;
  customerName?: string;
  changes: AgentUpdateChange[];
}

/** AI 员工-确认执行客户字段修改的请求体（只回传要改的字段，旧值由服务端重读） */
export interface AgentTaskApplyRequest {
  customerId: string;
  changes: { fieldId: string; newValue: string }[];
}

/** AI 员工-虚拟会议室-发言流水 */
export interface AiMeetingSpeech {
  speaker: string;
  content: string;
}

/** AI 员工-虚拟会议室-生成会议纪要参数 */
export interface AiMeetingMinutesGenerateParams {
  title?: string;
  timeText?: string;
  host?: string;
  attendees?: string[];
  transcript: AiMeetingSpeech[];
}

/** AI 员工-虚拟会议室-任务项 */
export interface AiMeetingMinutesTask {
  task: string;
  deadline?: string;
  deliverable?: string;
}

/** AI 员工-虚拟会议室-会议纪要 */
export interface AiMeetingMinutes {
  id: string;
  title: string;
  timeText?: string;
  place?: string;
  host?: string;
  recorder?: string;
  attendees?: string[];
  content?: string[];
  next?: string[];
  tasks?: AiMeetingMinutesTask[];
  createTime?: number;
}

/** 竞品/行业洞察-请求参数 */
export interface CompetitorInsightParams {
  competitor?: string;
  industry?: string;
  focus?: string;
  conversationId?: string;
}

/** 竞品/行业洞察-结构化结果 */
export interface CompetitorInsightResult {
  overview?: string;
  dynamics?: string[];
  trends?: string[];
  risks?: string[];
  suggestions?: string[];
  rawContent?: string;
}

/** 私信获客画像-已绑定平台账号 */
export interface DmAccount {
  id: string;
  platform: string;
  accountName?: string;
  uniqueId?: string;
  status: 'LOGGED_IN' | 'CRAWLING' | 'EXPIRED' | 'FAILED' | string;
  lastCrawlTime?: number;
  lastError?: string;
}

/** 私信获客画像-画像结果（画像字段在 profileJson 内，由前端解析渲染） */
export interface DmProfile {
  id: string;
  nickname?: string;
  uniqueId?: string;
  followerCount?: string;
  followingCount?: string;
  likedCount?: string;
  /** 主页图标判色的性别（男/女，可能为空，为空时回退 profileJson.gender） */
  gender?: string;
  /** 主页简介（签名/认证文本，用于 AI 提炼账号品类） */
  bio?: string;
  isGroup?: boolean;
  phone?: string;
  wechat?: string;
  status: 'RAW' | 'ANALYZED' | 'IMPORTED' | 'FAILED' | string;
  profileJson?: string;
  clueId?: string;
  createTime?: number;
}

/** 私信获客画像-导入线索池结果 */
export interface DmProfileImportResult {
  successCount: number;
  /** 因无联系方式被过滤、未导入的条数 */
  filteredCount?: number;
  failedNames?: string[];
}

/** 私信获客画像-profileJson 解析后的结构化字段 */
export interface DmProfileDetail {
  name?: string;
  gender?: string;
  age?: string;
  city?: string;
  accountCategory?: string;
  category?: string;
  preference?: string;
  summary?: string;
  confidence?: string;
}

/** 私信画像快照：线索转客户时拍平落库到客户，客户详情「私信画像」tab 展示 */
export interface DmProfileSnapshot {
  nickname?: string;
  name?: string;
  gender?: string;
  age?: string;
  city?: string;
  accountCategory?: string;
  category?: string;
  preference?: string;
  summary?: string;
  confidence?: string;
  phone?: string;
  wechat?: string;
  bio?: string;
  uniqueId?: string;
  followerCount?: string;
  followingCount?: string;
  likedCount?: string;
  isGroup?: boolean;
}

/** 短视频生成-提交参数（功能 7） */
export interface AiVideoGenerateParams {
  title: string;
  /** 原始输入（一句话主题或完整文案），脚本生成与分镜台大纲展示用 */
  sourceText?: string;
  /** 额外要求/卖点/补充素材（也兼容「做成视频」带入的现成脚本） */
  extraText?: string;
  /** 风格提示词（可选覆盖，空则随模板） */
  stylePrompt?: string;
  /** 角色提示词（可选，空则 AI 按模板与主题决定） */
  characterPrompt?: string;
  /** 成片方式：video / image_text（暂都走 Ken Burns） */
  mode?: string;
  /** 成片类型：talk / product / scenario / comic（默认 talk 口播科普） */
  videoType?: string;
  /** 所选图片模型ID（空则取推荐） */
  imageModelId?: string;
  /** 所选视频模型ID（预留） */
  videoModelId?: string;
  voice?: string;
  templateKey?: string;
  /** 成片比例：9:16 / 16:9 / 1:1，默认 9:16 */
  ratio?: string;
}

/** 短视频生成-任务（列表与详情复用，生成进度由前端轮询感知） */
export interface AiVideoTask {
  id: string;
  title: string;
  /** 原始输入（一句话主题或完整文案），分镜台大纲展示用 */
  sourceText?: string;
  voice?: string;
  style?: string;
  templateKey?: string;
  /** 成片方式 video / image_text */
  mode?: string;
  /** 成片类型 talk / product / scenario / comic */
  videoType?: string;
  /** 所选图片模型ID */
  imageModelId?: string;
  /** 所选视频模型ID（预留） */
  videoModelId?: string;
  /** 风格提示词 */
  stylePrompt?: string;
  /** 角色提示词 */
  characterPrompt?: string;
  /** 额外要求 */
  extraPrompt?: string;
  status:
    | 'PENDING'
    | 'SCRIPTING'
    | 'SCRIPT_READY'
    | 'IMAGING'
    | 'TTS'
    | 'ASSETS_READY'
    | 'SYNTH'
    | 'DONE'
    | 'FAILED'
    | string;
  progress?: number;
  attachmentId?: string;
  /** 封面附件ID（更换/用首镜后回填，空则回退模板封面） */
  coverAttachmentId?: string;
  /** 配套文案（抖音/小红书/朋友圈 JSON，生成失败为 null） */
  copyJson?: string;
  /** 分镜 JSON 数组 [{text,image,imageAttachmentId}]，供分镜台展示 */
  storyboardJson?: string;
  errorMsg?: string;
  createTime?: number;
}

/** 短视频生成-单个分镜（口播 + 画面描述 + 配图附件ID + 视频提示词 + 镜头视频附件ID） */
export interface AiVideoStoryboardSegment {
  text?: string;
  image?: string;
  imageAttachmentId?: string;
  videoPrompt?: string;
  videoAttachmentId?: string;
}

/** 短视频生成-编辑单个分镜请求（旁白/画面描述/视频提示词任一为空则保持不变） */
export interface AiVideoShotUpdateParams {
  text?: string;
  image?: string;
  videoPrompt?: string;
}

/** 短视频生成-编辑内置提示词请求（风格/角色/额外，null 不变，空串清空） */
export interface AiVideoPromptUpdateParams {
  stylePrompt?: string;
  characterPrompt?: string;
  extraPrompt?: string;
}

/** 短视频生成-配音音色选项（跟随已配置的配音模型） */
export interface AiVideoVoice {
  value: string;
  label: string;
  /** 音色性别：female/male，卡片「女声/男声」标识用 */
  gender?: string;
}

/** 短视频生成-视频模板库选项（模板选择卡片） */
export interface AiVideoTemplate {
  templateKey: string;
  name: string;
  description?: string;
  color?: string;
  softColor?: string;
  /** 片头/片尾底色，封面预览用（#RRGGBB） */
  background?: string;
  /** 片尾引导文案，封面预览用 */
  closingText?: string;
}

/** 短视频生成-主题/文案 AI 扩写请求（AI 生成主题 / AI 扩写文案 / AI 生成风格提示词 / 生成内置提示词） */
export interface AiVideoExpandParams {
  topic: string;
  mode?: 'theme' | 'script' | 'style' | 'prompts';
}

/** 短视频生成-主题/文案 AI 扩写结果（title 用于项目名称，content 用于输入框，prompts 模式返回三条提示词） */
export interface AiVideoExpandResult {
  title?: string;
  content?: string;
  stylePrompt?: string;
  characterPrompt?: string;
  extraPrompt?: string;
}

/** 短视频生成-音色试听结果（audio 为 base64 WAV） */
export interface AiVideoVoicePreviewResult {
  audio?: string;
}

/** 短视频生成-风格页「模型」区块的单个模型选项 */
export interface AiVideoModelOption {
  id: string;
  label: string;
  description?: string;
  recommended?: boolean;
}

/** 短视频生成-风格页图片/视频模型列表（图片模型跟随已启用的豆包 Seedream，视频模型当前为空） */
export interface AiVideoModelsResult {
  imageModels: AiVideoModelOption[];
  videoModels: AiVideoModelOption[];
}

/** 爆款复刻-提交参数（换皮变体，功能 7 扩展） */
export interface AiReplicaSubmitParams {
  /** 参考视频附件ID（上传临时附件返回的 ID） */
  referenceAttachmentId: string;
  /** 任务名（默认取参考视频文件名） */
  title?: string;
  /** 变体数量（默认 3，范围 1~5） */
  variantCount?: number;
}

/** 爆款复刻-变体（换皮变体：画面/声音复用原视频，AI 只动钩子/标题/字幕样式/比例） */
export interface AiReplicaVariant {
  id: string;
  /** 变体序号 */
  variantNo?: number;
  /** 钩子文案（开头大字） */
  hookText?: string;
  /** 标题/主题标签文案 */
  titleText?: string;
  /** 字幕样式标识（预设样式 key：classic / karaoke_yellow / karaoke_green / karaoke_pink） */
  subtitleStyle?: string;
  /** 输出比例：9:16 / 16:9 / 1:1 */
  ratio?: string;
  status: 'PENDING' | 'COMPOSING' | 'DONE' | 'FAILED' | string;
  /** 输出变体视频附件ID */
  attachmentId?: string;
  errorMsg?: string;
}

/** 爆款复刻-任务（含变体列表，解析/生成/合成进度由前端轮询感知） */
export interface AiReplicaTask {
  id: string;
  title?: string;
  referenceAttachmentId?: string;
  /** 封面帧附件ID */
  coverAttachmentId?: string;
  /** 视频时长（毫秒） */
  durationMs?: number;
  parseStatus?: 'PENDING' | 'PARSING' | 'READY' | 'FAILED' | string;
  status: 'PENDING' | 'PARSING' | 'GENERATING' | 'READY' | 'FAILED' | string;
  errorMsg?: string;
  variants?: AiReplicaVariant[];
  createTime?: number;
}

/** AI 漫剧-项目（列表与详情共用，详情多带 script） */
export interface DramaProject {
  id: string;
  title: string;
  description?: string | null;
  workflow?: string | null;
  coverAttachmentId?: string | null;
  paramsJson?: string | null;
  episodeCount?: number;
  assetCount?: number;
  hasScript?: boolean;
  script?: DramaScript | null;
  createTime?: number | null;
  updateTime?: number | null;
}

/** AI 漫剧-剧本（summaryJson / episodeContentJson 为 JSON 字符串，使用时自行解析） */
export interface DramaScript {
  id: string;
  projectId: string;
  name?: string | null;
  sourceText?: string | null;
  summaryJson?: string | null;
  episodeContentJson?: string | null;
  summaryStatus?: string | null;
  paramsJson?: string | null;
}

/** AI 漫剧-分集（含分镜列表） */
export interface DramaEpisode {
  id: string;
  projectId: string;
  name?: string | null;
  sortOrder?: number;
  status?: string | null;
  videoAttachmentId?: string | null;
  narrationStatus?: string | null;
  videoStatus?: string | null;
  videoError?: string | null;
  composeStatus?: string | null;
  composeError?: string | null;
  fragments: DramaFragment[];
}

/** AI 漫剧-分镜片段 */
export interface DramaFragment {
  id: string;
  episodeId: string;
  sortOrder?: number;
  content?: string | null;
  narration?: string | null;
  narrationAttachmentId?: string | null;
  voice?: string | null;
  durationSec?: number | null;
  coverAttachmentId?: string | null;
  videoAttachmentId?: string | null;
  assetIdsJson?: string | null;
  status?: string | null;
}

/** AI 漫剧-资产（角色/场景/道具/音色） */
export interface DramaAsset {
  id: string;
  projectId: string;
  type: string;
  assetType?: string | null;
  name?: string | null;
  voice?: string | null;
  coverAttachmentId?: string | null;
  paramsJson?: string | null;
  status?: string | null;
}

/** AI 漫剧-新建项目参数 */
export interface DramaProjectCreateParams {
  title?: string;
  source?: string;
  episodeCount?: number;
  imageStyleId?: string;
  workflow?: string;
}

/** AI 漫剧-单集正文（episode_content 数组项，后端存 JSON 字符串） */
export interface DramaEpisodeBody {
  episodeNumber: number;
  title?: string;
  creative?: string;
  summary?: string;
  body?: string;
  origin?: string;
}

/** AI 漫剧-更新剧本参数（创意/摘要/分集/风格/剧本名，对齐后端 DramaScriptUpdateRequest） */
export interface DramaScriptUpdateParams {
  source?: string;
  summaryJson?: string | null;
  episodeContentJson?: string | null;
  imageStyleId?: string;
  name?: string;
}

/** AI 漫剧-生成剧情摘要参数 */
export interface DramaScriptSummaryParams {
  projectId: string;
  creative?: string;
  episodeCount?: number;
  imageStyleId?: string;
}

/** AI 漫剧-生成剧情摘要结果（queued 是否真正入队异步） */
export interface DramaScriptSummaryResult {
  queued: boolean;
  script: DramaScript;
}

/** AI 漫剧-生成分集剧本参数 */
export interface DramaEpisodeScriptParams {
  projectId: string;
  batchSize?: number;
  force?: boolean;
  episodeNumber?: number;
}

/** AI 漫剧-生成分集剧本结果 */
export interface DramaEpisodeScriptResult {
  queued: boolean;
  totalGenerated: number;
  totalTarget: number;
  done: boolean;
  script: DramaScript;
}

/** AI 漫剧-追加一集参数 */
export interface DramaAddEpisodeParams {
  projectId: string;
  title?: string;
}

/** AI 漫剧-追加一集结果 */
export interface DramaAddEpisodeResult {
  episodeNumber: number;
  script: DramaScript;
}

/** AI 漫剧-从剧本抽取资产参数 */
export interface DramaSeedAssetsParams {
  projectId: string;
  refreshPrompts?: boolean;
}

/** AI 漫剧-从剧本抽取资产结果（异步时前端轮询项目 paramsJson.assets_seed_status） */
export interface DramaSeedAssetsResult {
  status: string;
  queued: boolean;
  createdCount?: number | null;
  message?: string | null;
  assets: DramaAsset[];
}

/** AI 漫剧-保存分镜列表参数（按 id 增量更新，未提交分镜删除） */
export interface DramaFragmentsSaveParams {
  fragments: Array<{
    id?: string | null;
    sortOrder?: number;
    content?: string | null;
    narration?: string | null;
    voice?: string | null;
    durationSec?: number | null;
    assetIdsJson?: string | null;
  }>;
}

/** AI 漫剧-生成分镜画面参数（空则整集） */
export interface DramaGenerateParams {
  fragmentIds?: string[];
  modelId?: string;
  mode?: string;
}

/** AI 漫剧-分镜规划结果（异步时前端轮询分集 status） */
export interface DramaPlanFragmentsResult {
  queued: boolean;
  episode: DramaEpisode;
}

/** AI 漫剧-生成口播配音参数（空 fragmentIds 则整集含台词分镜） */
export interface DramaNarrationParams {
  fragmentIds?: string[];
  voice?: string | null;
}

/** AI 漫剧-分镜口播试听参数（text 传编辑框当前未保存台词，保证所听即所得） */
export interface DramaVoicePreviewParams {
  voice?: string | null;
  text?: string | null;
}

/** AI 漫剧-分镜口播试听结果（base64 WAV） */
export interface DramaVoicePreviewResult {
  audio: string;
  voice?: string | null;
}

/** AI 漫剧-分集生成进度（轮询用） */
export interface DramaGenerateStatusResult {
  episodeId: string;
  done: number;
  failed: number;
  running: number;
  total: number;
  fragments: Array<{
    fragmentId: string;
    status: string;
    coverAttachmentId?: string | null;
    videoAttachmentId?: string | null;
  }>;
}

/** AI 工具中心-文案类工具流式生成参数 */
export interface AiToolTextParams {
  type: 'REWRITE' | 'TITLE' | 'SCRIPT' | string;
  content: string;
  option?: string;
}

/** AI 工具中心-文生图提交参数 */
export interface AiToolImageParams {
  prompt: string;
  size?: string;
}

/** AI 工具中心-视频转格式提交参数 */
export interface AiToolTranscodeParams {
  sourceAttachmentId: string;
  sourceName?: string;
  format?: string;
}

/** AI 工具中心-异步任务（文生图/视频转格式，进度由前端轮询感知） */
export interface AiToolTask {
  id: string;
  type: 'IMAGE' | 'TRANSCODE' | string;
  name?: string;
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED' | string;
  progress?: number;
  attachmentId?: string;
  errorMsg?: string;
  createTime?: number;
}

/** AI 问数 BI（功能 12）- 大白话查数请求 */
export interface AskBiQueryParams {
  question: string;
}

/** AI 问数 BI（功能 12）- 查询结果（标题 + 图表类型 + 列名 + 数据行） */
export interface AskBiQueryResult {
  title?: string;
  chartType?: 'number' | 'bar' | 'line' | 'pie' | 'table' | string;
  columns?: string[];
  rows?: (string | number)[][];
}

/** AI 问数 BI（功能 12）- 目录项（指标 key + 中文名） */
export interface AskBiCatalogItem {
  key: string;
  name: string;
}

/** AI 问数 BI（功能 12）- 目录（可查指标 + 示例问题 + 可查范围提示） */
export interface AskBiCatalogResult {
  metrics?: AskBiCatalogItem[];
  sampleQuestions?: string[];
  scopeHint?: string;
}

/** 声音克隆（功能 11）- 音色任务（提交/列表/详情复用，进度由前端轮询感知） */
export interface AiCloneVoice {
  id: string;
  name: string;
  provider?: string;
  /** 火山引擎 custom_speaker_id，成功后可作为配音音色 */
  speakerId?: string;
  status: 'TRAINING' | 'SUCCESS' | 'FAILED' | string;
  sampleAttachmentId?: string;
  errorMsg?: string;
  createTime?: number;
}

/** 声音克隆（功能 11）- 提交复刻请求 */
export interface AiCloneVoiceSubmitParams {
  name: string;
  sampleAttachmentId: string;
}

/** 声音克隆（功能 11）- 试听结果（audio 为 base64 WAV） */
export interface AiCloneVoicePreviewResult {
  audio?: string;
}
