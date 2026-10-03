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

export type AgentConversationQueryRequest = TableQueryParams;

export interface AgentConversationItem {
  id: string;
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  organizationId?: string;
  userId?: string;
  title: string;
  localPending?: boolean;
}

export type AgentConversationPageResult = CommonList<AgentConversationItem>;

export type AgentConversationMessageStatus = 'done' | 'stopped';

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
