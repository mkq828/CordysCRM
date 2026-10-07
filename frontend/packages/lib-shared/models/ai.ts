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

/** 企业知识库-出处片段长度设置 */
export interface AiKnowledgeConfig {
  snippetMax: number;
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

/** 线索爬取-企业信息（搜索列表项/详情复用，未拉详情时仅 name 有值） */
export interface LeadCrawlEnterprise {
  name: string;
  fromCache?: boolean;
  taxNo?: string;
  creditCode?: string;
  legalPerson?: string;
  status?: string;
  registerCapi?: string;
  personScope?: string;
  scale?: string;
  industry?: string;
  areaProvince?: string;
  areaCity?: string;
  registerPhone?: string;
  address?: string;
  bankName?: string;
  bankAccount?: string;
  regNo?: string;
}

/** 线索爬取-导入线索池结果 */
export interface LeadCrawlImportResult {
  successCount: number;
  failedNames?: string[];
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
  extraText?: string;
  voice?: string;
  style?: string;
}

/** 短视频生成-任务（列表与详情复用，生成进度由前端轮询感知） */
export interface AiVideoTask {
  id: string;
  title: string;
  voice?: string;
  style?: string;
  status: 'PENDING' | 'SCRIPTING' | 'TTS' | 'SYNTH' | 'DONE' | 'FAILED' | string;
  progress?: number;
  attachmentId?: string;
  /** 配套文案（抖音/小红书/朋友圈 JSON，生成失败为 null） */
  copyJson?: string;
  errorMsg?: string;
  createTime?: number;
}

/** 短视频生成-配音音色选项（跟随已配置的配音模型） */
export interface AiVideoVoice {
  value: string;
  label: string;
}
