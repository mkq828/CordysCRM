import { useI18n } from '../../hooks/useI18n';
import { getToken } from '../../method/auth';
import type {
  AgentActionApproveItem,
  AgentActionSuggestionItem,
  AgentChatCancelParams,
  AgentChatConfirmData,
  AgentChatConfirmRequest,
  AgentChatDoneData,
  AgentChatProgressData,
  AgentChatRunData,
  AgentChatStreamEvent,
  AgentChatStreamOptions,
  AgentChatStreamParams,
  AgentConversationQueryRequest,
  AgentMcpConfigItem,
  AgentTaskApplyRequest,
  AiCloneVoice,
  AiCloneVoicePreviewResult,
  AiCloneVoiceSubmitParams,
  AiContentGenerateParams,
  AiContentGenerateResult,
  AiEmployeeAgent,
  AiEmployeeAgentSaveParams,
  AiEmployeeChatParams,
  AiEmployeeDetail,
  AiEmployeeDivision,
  AiEmployeeItem,
  AiKnowledgeAnswerResult,
  AiKnowledgeAskParams,
  AiKnowledgeConfig,
  AiKnowledgeDoc,
  AiKnowledgeDocPageParams,
  AiMeetingMinutes,
  AiMeetingMinutesGenerateParams,
  AiPptOutlineParams,
  AiPptSlidesParams,
  AiReplicaSubmitParams,
  AiReplicaTask,
  AiReplicaVariant,
  AiSalesScript,
  AiSalesScriptPageParams,
  AiSalesScriptRetrieveParams,
  AiSalesScriptSaveParams,
  AiToolImageParams,
  AiToolTask,
  AiToolTextParams,
  AiToolTranscodeParams,
  AiVideoExpandParams,
  AiVideoExpandResult,
  AiVideoGenerateParams,
  AiVideoModelsResult,
  AiVideoPromptUpdateParams,
  AiVideoShotUpdateParams,
  AiVideoTask,
  AiVideoTemplate,
  AiVideoVoice,
  AiVideoVoicePreviewResult,
  AskBiCatalogResult,
  AskBiQueryParams,
  AskBiQueryResult,
  CompetitorInsightParams,
  CompetitorInsightResult,
  DmAccount,
  DmProfile,
  DmProfileImportResult,
  DramaAddEpisodeParams,
  DramaAddEpisodeResult,
  DramaAsset,
  DramaEpisode,
  DramaEpisodeScriptParams,
  DramaEpisodeScriptResult,
  DramaFragmentsSaveParams,
  DramaGenerateParams,
  DramaGenerateStatusResult,
  DramaNarrationParams,
  DramaPlanFragmentsResult,
  DramaProject,
  DramaProjectCreateParams,
  DramaScript,
  DramaScriptSummaryParams,
  DramaScriptSummaryResult,
  DramaScriptUpdateParams,
  DramaSeedAssetsParams,
  DramaSeedAssetsResult,
  DramaVoicePreviewParams,
  DramaVoicePreviewResult,
  SalesAdvisorAnalyzeParams,
  SalesAdvisorAnalyzeResult,
  SalesAdvisorQueryParams,
  SalesAdvisorQueryResult,
  ScriptRecommend,
  SmartFocusParams,
} from '../../models/ai';
import type { CommonList, ImportUploadParams, TableQueryParams } from '../../models/common';
import type { ValidateInfo } from '../../models/system/org';
import type { CordysAxios } from '../http/Axios';
import {
  AgentActionApproveConfirmUrl,
  AgentActionApproveIgnoreUrl,
  AgentActionApprovePageUrl,
  AgentActionSuggestionIgnoreUrl,
  AgentActionSuggestionPageUrl,
  AgentActionSuggestionSubmitUrl,
  AgentChatCancelUrl,
  AgentChatConfirmUrl,
  AgentChatFileUploadUrl,
  AgentChatStreamUrl,
  AgentChatUrl,
  AgentConversationDeleteUrl,
  AgentConversationDetailUrl,
  AgentConversationPageUrl,
  AgentConversationRenameUrl,
  AgentMcpConfigDeleteUrl,
  AgentMcpConfigImportUrl,
  AgentMcpConfigListUrl,
  AiBiAvailableUrl,
  AiBiCatalogUrl,
  AiBiQueryUrl,
  AiContentGenerateStreamUrl,
  AiContentGenerateUrl,
  AiDramaAddEpisodeUrl,
  AiDramaAssetsUrl,
  AiDramaAvailableUrl,
  AiDramaEpisodeScriptUrl,
  AiDramaEpisodesUrl,
  AiDramaFragmentVoicePreviewUrl,
  AiDramaModelsUrl,
  AiDramaProjectsUrl,
  AiDramaScriptSummaryUrl,
  AiDramaScriptUrl,
  AiDramaSeedAssetsUrl,
  AiDramaVoicesUrl,
  AiEmployeeAgentAddUrl,
  AiEmployeeAgentDeleteUrl,
  AiEmployeeAgentsUrl,
  AiEmployeeAgentUpdateUrl,
  AiEmployeeCatalogUrl,
  AiEmployeeChatStreamUrl,
  AiEmployeeDetailUrl,
  AiEmployeeDivisionsUrl,
  AiEmployeeTaskApplyUrl,
  AiEmployeeTaskStreamUrl,
  AiKnowledgeAskStreamUrl,
  AiKnowledgeAskUrl,
  AiKnowledgeConfigUrl,
  AiKnowledgeDocDeleteUrl,
  AiKnowledgeDocPageUrl,
  AiKnowledgeDocUploadUrl,
  AiMeetingMinutesUrl,
  AiPptOutlineStreamUrl,
  AiPptSlidesStreamUrl,
  AiReplicaAvailableUrl,
  AiReplicaTasksUrl,
  AiReplicaVariantsUrl,
  AiSalesScriptAddUrl,
  AiSalesScriptCategoriesUrl,
  AiSalesScriptDeleteUrl,
  AiSalesScriptImportPreCheckUrl,
  AiSalesScriptImportTemplateUrl,
  AiSalesScriptImportUrl,
  AiSalesScriptPageUrl,
  AiSalesScriptRetrieveUrl,
  AiSalesScriptUpdateUrl,
  AiToolAvailableUrl,
  AiToolImageUrl,
  AiToolTasksPageUrl,
  AiToolTasksUrl,
  AiToolTextStreamUrl,
  AiToolTranscodeUrl,
  AiVideoAvailableUrl,
  AiVideoExpandUrl,
  AiVideoModelsUrl,
  AiVideoTasksUrl,
  AiVideoTemplatesUrl,
  AiVideoVoicePreviewUrl,
  AiVideoVoicesUrl,
  AiVoiceCloneAvailableUrl,
  AiVoiceCloneTasksUrl,
  CompetitorInsightStreamUrl,
  DmProfileAccountsUrl,
  DmProfileAvailableUrl,
  DmProfileImportUrl,
  DmProfileProfilesUrl,
  SalesAdvisorAnalyzeStreamUrl,
  SalesAdvisorAnalyzeUrl,
  SalesAdvisorQueryUrl,
  SmartAiSummaryRegenerateUrl,
  SmartAiSummaryUrl,
  SmartDataOverviewRegenerateUrl,
  SmartDataOverviewUrl,
} from '../requrls/ai';

interface SseBlock {
  event: string;
  data: string;
  id?: string;
}

interface AgentErrorEventData {
  error?: string;
  message?: string;
}

function getApiUrl(path: string): string {
  const baseUrl = String(import.meta.env.VITE_API_BASE_URL ?? '')
    .trim()
    .replace(/^['"]|['"]$/g, '')
    .replace(/^\/|\/$/g, '');

  return `${window.location.origin}${baseUrl ? `/${baseUrl}` : ''}${path}`;
}

// 请求头处理
function getAgentHeaders(): Record<string, string> {
  const currentLocale = localStorage.getItem('CRM-locale') || 'zh-CN';
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (token) {
    const { sessionId, csrfToken } = token;

    return {
      ...headers,
      'X-AUTH-TOKEN': sessionId ?? '',
      'CSRF-TOKEN': csrfToken ?? '',
      'Accept-Language': currentLocale,
    };
  }

  return headers;
}

// 拼接
function parseSseBlock(block: string): SseBlock {
  let event = 'message';
  let id: string | undefined;
  const data: string[] = [];

  block.split('\n').forEach((line) => {
    if (line.startsWith('id:')) {
      id = line.slice(3).trim();
      return;
    }

    if (line.startsWith('event:')) {
      event = line.slice(6).trim();
      return;
    }

    if (line.startsWith('data:')) {
      data.push(line.slice(5).replace(/^ /, ''));
    }
  });

  return {
    event,
    data: data.join('\n'),
    id,
  };
}

function getErrorMessage(data: string): string {
  const { t } = useI18n();

  try {
    const errorData = JSON.parse(data) as AgentErrorEventData;

    return errorData.error || errorData.message || data || t('common.operationFailed');
  } catch {
    return data || t('common.operationFailed');
  }
}

function toStreamEvent(block: SseBlock, options: AgentChatStreamOptions): AgentChatStreamEvent | undefined {
  if (block.event === 'run') {
    const run = JSON.parse(block.data) as AgentChatRunData;

    options.onSession?.(run.runId, run.conversationId);

    return {
      type: 'run',
      run,
      raw: block,
    };
  }

  if (block.event === 'progress') {
    const progress = JSON.parse(block.data) as AgentChatProgressData;

    return {
      type: 'progress',
      conversationId: block.id,
      progress,
      raw: block,
    };
  }

  if (block.event === 'chunk') {
    return {
      type: 'chunk',
      content: block.data,
      conversationId: block.id,
      raw: block,
    };
  }

  if (block.event === 'confirm') {
    const confirm = JSON.parse(block.data) as AgentChatConfirmData;

    return {
      type: 'confirm',
      conversationId: block.id,
      sessionId: confirm.sessionId,
      confirm,
      raw: block,
    };
  }

  if (block.event === 'error') {
    return {
      type: 'error',
      errorMessage: getErrorMessage(block.data),
      conversationId: block.id,
      raw: block,
    };
  }

  if (block.event === 'done') {
    return {
      type: 'done',
      data: JSON.parse(block.data) as AgentChatDoneData,
      raw: block,
    };
  }

  return undefined;
}

async function* readAgentStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  options: AgentChatStreamOptions,
  onErrorEvent: () => void
): AsyncIterable<AgentChatStreamEvent> {
  const decoder = new TextDecoder();
  let buffer = '';
  let hasErrorEvent = false;

  function readBufferedEvents(): AgentChatStreamEvent[] {
    const events: AgentChatStreamEvent[] = [];
    let boundary = buffer.indexOf('\n\n');

    while (boundary >= 0) {
      const rawBlock = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);

      if (rawBlock.trim()) {
        const event = toStreamEvent(parseSseBlock(rawBlock), options);

        if (event) {
          events.push(event);
        }
      }

      boundary = buffer.indexOf('\n\n');
    }

    return events;
  }

  while (true) {
    // eslint-disable-next-line no-await-in-loop
    const result = await reader.read();
    buffer = (buffer + decoder.decode(result.value || new Uint8Array(), { stream: !result.done })).replace(
      /\r\n/g,
      '\n'
    );

    const events = readBufferedEvents();

    for (let index = 0; index < events.length; index += 1) {
      const event = events[index];

      yield event;

      if (event.type === 'error') {
        hasErrorEvent = true;
        onErrorEvent();
        break;
      }
    }

    if (hasErrorEvent || result.done) {
      break;
    }
  }

  if (!hasErrorEvent && buffer.trim()) {
    const event = toStreamEvent(parseSseBlock(buffer), options);

    if (event) {
      yield event;

      if (event.type === 'error') {
        onErrorEvent();
      }
    }
  }
}

export default function useAiApi(CDR: CordysAxios) {
  // 统一 SSE 流式客户端：会话军师 / 获客内容 / 知识库问答 / 哆咪AI 共用同一套事件协议（run/chunk/done/error）
  async function* streamSse(
    url: string,
    params: unknown,
    options: AgentChatStreamOptions = {}
  ): AsyncIterable<AgentChatStreamEvent> {
    let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;

    try {
      const response = await fetch(getApiUrl(url), {
        method: 'POST',
        headers: getAgentHeaders(),
        credentials: 'include',
        body: JSON.stringify(params),
        signal: options.signal,
      });

      if (!response.ok) {
        const { t } = useI18n();

        throw new Error((await response.text()) || `${t('common.operationFailed')}：${response.status}`);
      }

      if (!response.body) {
        throw new Error(useI18n().t('common.operationFailed'));
      }

      reader = response.body.getReader();

      let hasErrorEvent = false;

      yield* readAgentStream(reader, options, () => {
        hasErrorEvent = true;
      });

      if (!hasErrorEvent) {
        yield {
          type: 'done',
        };
      }
    } catch (error) {
      if (!options.signal?.aborted) {
        throw error;
      }
    } finally {
      reader?.releaseLock();
    }
  }

  function streamAgentChat(params: AgentChatStreamParams, options: AgentChatStreamOptions = {}) {
    return streamSse(AgentChatStreamUrl, params, options);
  }

  function streamSalesAdvisor(data: SalesAdvisorAnalyzeParams, options: AgentChatStreamOptions = {}) {
    return streamSse(SalesAdvisorAnalyzeStreamUrl, data, options);
  }

  function streamGenerateAiContent(data: AiContentGenerateParams, options: AgentChatStreamOptions = {}) {
    return streamSse(AiContentGenerateStreamUrl, data, options);
  }

  function streamGenerateAiPptOutline(data: AiPptOutlineParams, options: AgentChatStreamOptions = {}) {
    return streamSse(AiPptOutlineStreamUrl, data, options);
  }

  function streamGenerateAiPptSlides(data: AiPptSlidesParams, options: AgentChatStreamOptions = {}) {
    return streamSse(AiPptSlidesStreamUrl, data, options);
  }

  function streamAskKnowledge(data: AiKnowledgeAskParams, options: AgentChatStreamOptions = {}) {
    return streamSse(AiKnowledgeAskStreamUrl, data, options);
  }

  async function cancelAgentChat(data: AgentChatCancelParams) {
    await CDR.post({ url: AgentChatCancelUrl, data });
  }

  async function confirmAgentChat(dialogId: string, request: AgentChatConfirmRequest) {
    await CDR.post({
      url: `${AgentChatConfirmUrl}/${dialogId}`,
      data: request,
    });
  }

  function likeAgentChat(runId: string) {
    return CDR.post({ url: `${AgentChatUrl}/${runId}/like` });
  }

  function dislikeAgentChat(runId: string, data?: { reason: string }) {
    return CDR.post({ url: `${AgentChatUrl}/${runId}/dislike`, data });
  }

  function cancelAgentChatFeedback(runId: string) {
    return CDR.post({ url: `${AgentChatUrl}/${runId}/feedback/cancel` });
  }

  function getAgentConversationPage(data: AgentConversationQueryRequest) {
    return CDR.post({ url: AgentConversationPageUrl, data });
  }

  function getAgentConversationDetail(conversationId: string) {
    return CDR.get({ url: `${AgentConversationDetailUrl}/${conversationId}` });
  }

  function deleteAgentConversation(conversationId: string) {
    return CDR.get({ url: `${AgentConversationDeleteUrl}/${conversationId}` });
  }

  function renameAgentConversation(conversationId: string, data: { title: string }) {
    return CDR.put({
      url: `${AgentConversationRenameUrl}/${conversationId}`,
      data,
    });
  }

  function uploadAgentChatFile(files: File[]) {
    return CDR.uploadFile<{ data: string[] }>({ url: AgentChatFileUploadUrl }, { fileList: files }, 'files', true);
  }

  function getAgentMcpConfigList() {
    return CDR.get<AgentMcpConfigItem[]>({ url: AgentMcpConfigListUrl });
  }

  function importAgentMcpConfig(file: File) {
    return CDR.uploadFile({ url: AgentMcpConfigImportUrl }, { fileList: [file] }, 'file');
  }

  function deleteAgentMcpConfig(id: string) {
    return CDR.get({ url: `${AgentMcpConfigDeleteUrl}/${id}` });
  }

  function getSmartDataOverview() {
    return CDR.post<string>({ url: SmartDataOverviewUrl });
  }

  function regenerateSmartDataOverview() {
    return CDR.post<string>({ url: SmartDataOverviewRegenerateUrl });
  }

  function getSmartAiSummary(data: SmartFocusParams) {
    return CDR.post<string>({ url: SmartAiSummaryUrl, data });
  }

  function regenerateSmartAiSummary(data: SmartFocusParams) {
    return CDR.post<string>({ url: SmartAiSummaryRegenerateUrl, data });
  }

  function getAgentActionSuggestionPage(data: TableQueryParams) {
    return CDR.post<CommonList<AgentActionSuggestionItem>>({ url: AgentActionSuggestionPageUrl, data });
  }

  function ignoreAgentActionSuggestion(id: string) {
    return CDR.post({ url: `${AgentActionSuggestionIgnoreUrl}/${id}` });
  }

  function submitAgentActionSuggestion(id: string, label: string) {
    return CDR.post({ url: `${AgentActionSuggestionSubmitUrl}/${id}`, params: { label } });
  }

  function getAgentActionApprovePage(data: TableQueryParams) {
    return CDR.post<CommonList<AgentActionApproveItem>>({ url: AgentActionApprovePageUrl, data });
  }

  function ignoreAgentActionApprove(id: string) {
    return CDR.post({ url: `${AgentActionApproveIgnoreUrl}/${id}` });
  }

  function confirmAgentActionApprove(id: string) {
    return CDR.post({ url: `${AgentActionApproveConfirmUrl}/${id}` });
  }

  function analyzeSalesAdvisor(data: SalesAdvisorAnalyzeParams) {
    return CDR.post<SalesAdvisorAnalyzeResult>({ url: SalesAdvisorAnalyzeUrl, data });
  }

  function querySalesAdvisor(data: SalesAdvisorQueryParams) {
    return CDR.post<SalesAdvisorQueryResult>({ url: SalesAdvisorQueryUrl, data });
  }

  function getAiSalesScriptPage(data: AiSalesScriptPageParams) {
    return CDR.post<CommonList<AiSalesScript>>({ url: AiSalesScriptPageUrl, data });
  }

  function getAiSalesScriptCategories() {
    return CDR.get<string[]>({ url: AiSalesScriptCategoriesUrl });
  }

  function addAiSalesScript(data: AiSalesScriptSaveParams) {
    return CDR.post({ url: AiSalesScriptAddUrl, data });
  }

  function updateAiSalesScript(data: AiSalesScriptSaveParams) {
    return CDR.post({ url: AiSalesScriptUpdateUrl, data });
  }

  function deleteAiSalesScript(id: string) {
    return CDR.get({ url: `${AiSalesScriptDeleteUrl}/${id}` });
  }

  function retrieveAiSalesScript(data: AiSalesScriptRetrieveParams) {
    return CDR.post<ScriptRecommend[]>({ url: AiSalesScriptRetrieveUrl, data });
  }

  function generateAiContent(data: AiContentGenerateParams) {
    return CDR.post<AiContentGenerateResult>({ url: AiContentGenerateUrl, data });
  }

  function getAiKnowledgeDocPage(data: AiKnowledgeDocPageParams) {
    return CDR.post<CommonList<AiKnowledgeDoc>>({ url: AiKnowledgeDocPageUrl, data });
  }

  function uploadKnowledgeDoc(file: File) {
    return CDR.uploadFile({ url: AiKnowledgeDocUploadUrl }, { fileList: [file] }, 'file');
  }

  function deleteAiKnowledgeDoc(id: string) {
    return CDR.get({ url: `${AiKnowledgeDocDeleteUrl}/${id}` });
  }

  function askKnowledge(data: AiKnowledgeAskParams) {
    return CDR.post<AiKnowledgeAnswerResult>({ url: AiKnowledgeAskUrl, data });
  }

  function getAiKnowledgeConfig() {
    return CDR.get<AiKnowledgeConfig>({ url: AiKnowledgeConfigUrl });
  }

  function saveAiKnowledgeConfig(data: AiKnowledgeConfig) {
    return CDR.post({ url: AiKnowledgeConfigUrl, data });
  }

  function getAiEmployeeDivisions() {
    return CDR.get<AiEmployeeDivision[]>({ url: AiEmployeeDivisionsUrl });
  }

  function getAiEmployeeCatalog(division: string, keyword?: string) {
    return CDR.get<AiEmployeeItem[]>({ url: AiEmployeeCatalogUrl, params: { division, keyword } });
  }

  function getAiEmployeeDetail(id: string) {
    return CDR.get<AiEmployeeDetail>({ url: `${AiEmployeeDetailUrl}/${id}` });
  }

  function getAiEmployeeAgents() {
    return CDR.get<AiEmployeeAgent[]>({ url: AiEmployeeAgentsUrl });
  }

  function addAiEmployeeAgent(data: AiEmployeeAgentSaveParams) {
    return CDR.post({ url: AiEmployeeAgentAddUrl, data });
  }

  function updateAiEmployeeAgent(data: AiEmployeeAgentSaveParams) {
    return CDR.post({ url: AiEmployeeAgentUpdateUrl, data });
  }

  function deleteAiEmployeeAgent(id: string) {
    return CDR.get({ url: `${AiEmployeeAgentDeleteUrl}/${id}` });
  }

  function streamAiEmployeeChat(agentId: string, data: AiEmployeeChatParams, options: AgentChatStreamOptions = {}) {
    return streamSse(`${AiEmployeeChatStreamUrl}/${agentId}/chat/stream`, data, options);
  }

  function streamAiEmployeeTask(agentId: string, data: AiEmployeeChatParams, options: AgentChatStreamOptions = {}) {
    return streamSse(`${AiEmployeeTaskStreamUrl}/${agentId}/task/stream`, data, options);
  }

  function applyAiEmployeeTask(agentId: string, data: AgentTaskApplyRequest) {
    return CDR.post({ url: `${AiEmployeeTaskApplyUrl}/${agentId}/task/apply`, data });
  }

  function generateAiMeetingMinutes(data: AiMeetingMinutesGenerateParams) {
    return CDR.post<AiMeetingMinutes>({ url: AiMeetingMinutesUrl, data });
  }

  function getAiMeetingMinutes() {
    return CDR.get<AiMeetingMinutes[]>({ url: AiMeetingMinutesUrl });
  }

  function getAiMeetingMinutesDetail(id: string) {
    return CDR.get<AiMeetingMinutes>({ url: `${AiMeetingMinutesUrl}/${id}` });
  }

  function streamCompetitorInsight(data: CompetitorInsightParams, options: AgentChatStreamOptions = {}) {
    return streamSse(CompetitorInsightStreamUrl, data, options);
  }

  function getDmProfileAvailable() {
    return CDR.get<boolean>({ url: DmProfileAvailableUrl });
  }

  function getDmProfileAccounts() {
    return CDR.get<DmAccount[]>({ url: DmProfileAccountsUrl });
  }

  function bindDmProfileAccount(data: { platform: string; cookieText: string }) {
    return CDR.post<DmAccount>({ url: DmProfileAccountsUrl, data });
  }

  function unbindDmProfileAccount(id: string) {
    return CDR.delete({ url: `${DmProfileAccountsUrl}/${id}` });
  }

  function crawlDmProfile(id: string) {
    return CDR.post<DmAccount>({ url: `${DmProfileAccountsUrl}/${id}/crawl` });
  }

  function getDmProfileProfiles(status?: string) {
    return CDR.get<DmProfile[]>({ url: DmProfileProfilesUrl, params: status ? { status } : undefined });
  }

  function importDmProfile(data: { profileIds: string[] }) {
    return CDR.post<DmProfileImportResult>({ url: DmProfileImportUrl, data });
  }

  function getAiVideoAvailable() {
    return CDR.get<boolean>({ url: AiVideoAvailableUrl });
  }

  function createAiVideoTask(data: AiVideoGenerateParams) {
    return CDR.post<AiVideoTask>({ url: AiVideoTasksUrl, data });
  }

  function getAiVideoTasks() {
    return CDR.get<AiVideoTask[]>({ url: AiVideoTasksUrl });
  }

  function getAiVideoTask(id: string) {
    return CDR.get<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}` });
  }

  function deleteAiVideoTask(id: string) {
    return CDR.delete({ url: `${AiVideoTasksUrl}/${id}` });
  }

  function setAiVideoCover(id: string, params: ImportUploadParams) {
    return CDR.uploadFile<{ data: AiVideoTask }>({ url: `${AiVideoTasksUrl}/${id}/cover` }, params, 'file');
  }

  function setAiVideoFirstShotCover(id: string) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/cover/first-shot` });
  }

  function confirmAiVideoAssets(id: string) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/assets` });
  }

  function composeAiVideo(id: string) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/compose` });
  }

  function regenerateAiVideoCopy(id: string) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/copy` });
  }

  function updateAiVideoShot(id: string, index: number, data: AiVideoShotUpdateParams) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/shots/${index}`, data });
  }

  function updateAiVideoPrompts(id: string, data: AiVideoPromptUpdateParams) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/prompts`, data });
  }

  function regenAiVideoShotImage(id: string, index: number, data: AiVideoShotUpdateParams) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/shots/${index}/regen-image`, data });
  }

  function previewAiVideoShotVoice(id: string, index: number) {
    return CDR.post<AiVideoVoicePreviewResult>({ url: `${AiVideoTasksUrl}/${id}/shots/${index}/preview-voice` });
  }

  function generateAiVideoShots(id: string) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/videos` });
  }

  function regenAiVideoShotVideo(id: string, index: number, data: AiVideoShotUpdateParams) {
    return CDR.post<AiVideoTask>({ url: `${AiVideoTasksUrl}/${id}/shots/${index}/regen-video`, data });
  }

  function uploadAiVideoShotImage(id: string, index: number, params: ImportUploadParams) {
    return CDR.uploadFile<{ data: AiVideoTask }>(
      { url: `${AiVideoTasksUrl}/${id}/shots/${index}/image` },
      params,
      'file'
    );
  }

  function getAiVideoVoices() {
    return CDR.get<AiVideoVoice[]>({ url: AiVideoVoicesUrl });
  }

  function previewAiVideoVoice(voice: string) {
    return CDR.post<AiVideoVoicePreviewResult>({ url: AiVideoVoicePreviewUrl, data: { voice } });
  }

  function getAiVideoTemplates() {
    return CDR.get<AiVideoTemplate[]>({ url: AiVideoTemplatesUrl });
  }

  function expandAiVideoContent(data: AiVideoExpandParams) {
    return CDR.post<AiVideoExpandResult>({ url: AiVideoExpandUrl, data });
  }

  function getAiVideoModels() {
    return CDR.get<AiVideoModelsResult>({ url: AiVideoModelsUrl });
  }

  function getAiDramaAvailable() {
    return CDR.get<boolean>({ url: AiDramaAvailableUrl });
  }

  function getAiDramaProjects() {
    return CDR.get<DramaProject[]>({ url: AiDramaProjectsUrl });
  }

  function createAiDramaProject(data: DramaProjectCreateParams) {
    return CDR.post<DramaProject>({ url: AiDramaProjectsUrl, data });
  }

  function getAiDramaProject(id: string) {
    return CDR.get<DramaProject>({ url: `${AiDramaProjectsUrl}/${id}` });
  }

  function updateAiDramaProject(id: string, data: Partial<DramaProject>) {
    return CDR.put<DramaProject>({ url: `${AiDramaProjectsUrl}/${id}`, data });
  }

  function deleteAiDramaProject(id: string) {
    return CDR.delete({ url: `${AiDramaProjectsUrl}/${id}` });
  }

  function getAiDramaScript(projectId: string) {
    return CDR.get<DramaScript>({ url: `${AiDramaScriptUrl}/${projectId}/script` });
  }

  function updateAiDramaScript(projectId: string, data: DramaScriptUpdateParams) {
    return CDR.put<DramaScript>({ url: `${AiDramaScriptUrl}/${projectId}/script`, data });
  }

  function getAiDramaEpisodes(projectId: string) {
    return CDR.get<DramaEpisode[]>({ url: `${AiDramaScriptUrl}/${projectId}/episodes` });
  }

  function getAiDramaEpisode(id: string) {
    return CDR.get<DramaEpisode>({ url: `${AiDramaEpisodesUrl}/${id}` });
  }

  function updateAiDramaEpisode(id: string, data: { name?: string; paramsJson?: string }) {
    return CDR.put<DramaEpisode>({ url: `${AiDramaEpisodesUrl}/${id}`, data });
  }

  function renameAiDramaEpisode(projectId: string, episodeNumber: number, name: string) {
    return CDR.put<null>({ url: `${AiDramaScriptUrl}/${projectId}/episodes/${episodeNumber}`, data: { name } });
  }

  function deleteAiDramaEpisode(projectId: string, episodeNumber: number) {
    return CDR.delete<null>({ url: `${AiDramaScriptUrl}/${projectId}/episodes/${episodeNumber}` });
  }

  function getAiDramaAssets(projectId: string) {
    return CDR.get<DramaAsset[]>({ url: `${AiDramaScriptUrl}/${projectId}/assets` });
  }

  function getAllAiDramaAssets() {
    return CDR.get<DramaAsset[]>({ url: AiDramaAssetsUrl });
  }

  function createAiDramaAsset(data: Partial<DramaAsset> & { projectId: string }) {
    return CDR.post<DramaAsset>({ url: AiDramaAssetsUrl, data });
  }

  function updateAiDramaAsset(id: string, data: Partial<DramaAsset>) {
    return CDR.put<DramaAsset>({ url: `${AiDramaAssetsUrl}/${id}`, data });
  }

  function deleteAiDramaAsset(id: string) {
    return CDR.delete({ url: `${AiDramaAssetsUrl}/${id}` });
  }

  function getAiDramaVoices() {
    return CDR.get<AiVideoVoice[]>({ url: AiDramaVoicesUrl });
  }

  function getAiDramaModels() {
    return CDR.get<AiVideoModelsResult>({ url: AiDramaModelsUrl });
  }

  function generateAiDramaScriptSummary(data: DramaScriptSummaryParams) {
    return CDR.post<DramaScriptSummaryResult>({ url: AiDramaScriptSummaryUrl, data });
  }

  function generateAiDramaEpisodeScript(data: DramaEpisodeScriptParams) {
    return CDR.post<DramaEpisodeScriptResult>({ url: AiDramaEpisodeScriptUrl, data });
  }

  function addAiDramaEpisode(data: DramaAddEpisodeParams) {
    return CDR.post<DramaAddEpisodeResult>({ url: AiDramaAddEpisodeUrl, data });
  }

  function seedAiDramaAssets(data: DramaSeedAssetsParams) {
    return CDR.post<DramaSeedAssetsResult>({ url: AiDramaSeedAssetsUrl, data });
  }

  function generateAiDramaAssetImage(id: string) {
    return CDR.post<DramaAsset>({ url: `${AiDramaAssetsUrl}/${id}/generate` });
  }

  function uploadAiDramaAssetImage(id: string, params: ImportUploadParams) {
    return CDR.uploadFile<{ data: DramaAsset }>({ url: `${AiDramaAssetsUrl}/${id}/upload` }, params, 'file');
  }

  function planAiDramaFragments(id: string) {
    return CDR.post<DramaPlanFragmentsResult>({ url: `${AiDramaEpisodesUrl}/${id}/plan-fragments` });
  }

  function saveAiDramaFragments(id: string, data: DramaFragmentsSaveParams) {
    return CDR.put<DramaEpisode>({ url: `${AiDramaEpisodesUrl}/${id}/fragments`, data });
  }

  function generateAiDramaFragments(id: string, data: DramaGenerateParams) {
    return CDR.post<DramaGenerateStatusResult>({ url: `${AiDramaEpisodesUrl}/${id}/generate`, data });
  }

  function getAiDramaGenerateStatus(id: string) {
    return CDR.get<DramaGenerateStatusResult>({ url: `${AiDramaEpisodesUrl}/${id}/generate-status` });
  }

  function generateAiDramaNarration(id: string, data?: DramaNarrationParams) {
    return CDR.post<DramaEpisode>({ url: `${AiDramaEpisodesUrl}/${id}/voice`, data });
  }

  function previewAiDramaFragmentVoice(fragmentId: string, voice?: string, text?: string) {
    const data: DramaVoicePreviewParams = {};
    if (voice) data.voice = voice;
    if (text) data.text = text;
    return CDR.post<DramaVoicePreviewResult>({
      url: `${AiDramaFragmentVoicePreviewUrl}/${fragmentId}/voice-preview`,
      data,
    });
  }

  function composeAiDramaEpisode(id: string) {
    return CDR.post<DramaEpisode>({ url: `${AiDramaEpisodesUrl}/${id}/compose` });
  }

  function generateAiDramaFragmentVideos(id: string, data?: DramaGenerateParams) {
    return CDR.post<DramaEpisode>({ url: `${AiDramaEpisodesUrl}/${id}/generate-video`, data });
  }

  function getAiBiAvailable() {
    return CDR.get<boolean>({ url: AiBiAvailableUrl });
  }

  function getAiBiCatalog() {
    return CDR.get<AskBiCatalogResult>({ url: AiBiCatalogUrl });
  }

  function askBiQuery(data: AskBiQueryParams) {
    return CDR.post<AskBiQueryResult>({ url: AiBiQueryUrl, data });
  }

  function getAiToolAvailable() {
    return CDR.get<boolean>({ url: AiToolAvailableUrl });
  }

  function streamAiToolText(data: AiToolTextParams, options: AgentChatStreamOptions = {}) {
    return streamSse(AiToolTextStreamUrl, data, options);
  }

  function createAiToolImage(data: AiToolImageParams) {
    return CDR.post<AiToolTask>({ url: AiToolImageUrl, data });
  }

  function createAiToolTranscode(data: AiToolTranscodeParams) {
    return CDR.post<AiToolTask>({ url: AiToolTranscodeUrl, data });
  }

  function getAiToolTasks() {
    return CDR.get<AiToolTask[]>({ url: AiToolTasksUrl });
  }

  function getAiToolTasksPage(data: TableQueryParams) {
    return CDR.post<CommonList<AiToolTask>>({ url: AiToolTasksPageUrl, data });
  }

  function deleteAiToolTask(id: string) {
    return CDR.delete({ url: `${AiToolTasksUrl}/${id}` });
  }

  function getAiVoiceCloneAvailable() {
    return CDR.get<boolean>({ url: AiVoiceCloneAvailableUrl });
  }

  function submitAiVoiceClone(data: AiCloneVoiceSubmitParams) {
    return CDR.post<AiCloneVoice>({ url: AiVoiceCloneTasksUrl, data });
  }

  function getAiVoiceCloneTasks() {
    return CDR.get<AiCloneVoice[]>({ url: AiVoiceCloneTasksUrl });
  }

  function getAiVoiceCloneTask(id: string) {
    return CDR.get<AiCloneVoice>({ url: `${AiVoiceCloneTasksUrl}/${id}` });
  }

  function deleteAiVoiceCloneTask(id: string) {
    return CDR.delete({ url: `${AiVoiceCloneTasksUrl}/${id}` });
  }

  function previewAiVoiceClone(id: string, data?: { text?: string }) {
    return CDR.post<AiCloneVoicePreviewResult>({ url: `${AiVoiceCloneTasksUrl}/${id}/preview`, data });
  }

  function downloadAiSalesScriptTemplate() {
    return CDR.get(
      { url: AiSalesScriptImportTemplateUrl, responseType: 'blob' },
      { isTransformResponse: false, isReturnNativeResponse: true }
    );
  }

  function preCheckImportAiSalesScript(params: ImportUploadParams) {
    return CDR.uploadFile<{ data: ValidateInfo }>({ url: AiSalesScriptImportPreCheckUrl }, params, 'file');
  }

  function importAiSalesScript(params: ImportUploadParams) {
    return CDR.uploadFile({ url: AiSalesScriptImportUrl }, params, 'file');
  }

  function getAiReplicaAvailable() {
    return CDR.get<boolean>({ url: AiReplicaAvailableUrl });
  }

  function createAiReplicaTask(data: AiReplicaSubmitParams) {
    return CDR.post<AiReplicaTask>({ url: AiReplicaTasksUrl, data });
  }

  function getAiReplicaTasks() {
    return CDR.get<AiReplicaTask[]>({ url: AiReplicaTasksUrl });
  }

  function getAiReplicaTask(id: string) {
    return CDR.get<AiReplicaTask>({ url: `${AiReplicaTasksUrl}/${id}` });
  }

  function regenerateAiReplicaTask(id: string) {
    return CDR.post<null>({ url: `${AiReplicaTasksUrl}/${id}/regenerate` });
  }

  function deleteAiReplicaTask(id: string) {
    return CDR.delete({ url: `${AiReplicaTasksUrl}/${id}` });
  }

  function deleteAiReplicaVariant(id: string) {
    return CDR.delete({ url: `${AiReplicaVariantsUrl}/${id}` });
  }

  return {
    streamAgentChat,
    streamSalesAdvisor,
    streamGenerateAiContent,
    streamGenerateAiPptOutline,
    streamGenerateAiPptSlides,
    streamAskKnowledge,
    cancelAgentChat,
    confirmAgentChat,
    likeAgentChat,
    dislikeAgentChat,
    cancelAgentChatFeedback,
    getAgentConversationPage,
    getAgentConversationDetail,
    deleteAgentConversation,
    renameAgentConversation,
    uploadAgentChatFile,
    getAgentMcpConfigList,
    importAgentMcpConfig,
    deleteAgentMcpConfig,
    getSmartDataOverview,
    regenerateSmartDataOverview,
    getSmartAiSummary,
    regenerateSmartAiSummary,
    getAgentActionSuggestionPage,
    ignoreAgentActionSuggestion,
    submitAgentActionSuggestion,
    getAgentActionApprovePage,
    ignoreAgentActionApprove,
    confirmAgentActionApprove,
    analyzeSalesAdvisor,
    querySalesAdvisor,
    getAiSalesScriptPage,
    getAiSalesScriptCategories,
    addAiSalesScript,
    updateAiSalesScript,
    deleteAiSalesScript,
    retrieveAiSalesScript,
    generateAiContent,
    getAiKnowledgeDocPage,
    uploadKnowledgeDoc,
    deleteAiKnowledgeDoc,
    askKnowledge,
    getAiKnowledgeConfig,
    saveAiKnowledgeConfig,
    getAiEmployeeDivisions,
    getAiEmployeeCatalog,
    getAiEmployeeDetail,
    getAiEmployeeAgents,
    addAiEmployeeAgent,
    updateAiEmployeeAgent,
    deleteAiEmployeeAgent,
    streamAiEmployeeChat,
    streamAiEmployeeTask,
    applyAiEmployeeTask,
    generateAiMeetingMinutes,
    getAiMeetingMinutes,
    getAiMeetingMinutesDetail,
    streamCompetitorInsight,
    getDmProfileAvailable,
    getDmProfileAccounts,
    bindDmProfileAccount,
    unbindDmProfileAccount,
    crawlDmProfile,
    getDmProfileProfiles,
    importDmProfile,
    getAiVideoAvailable,
    createAiVideoTask,
    getAiVideoTasks,
    getAiVideoTask,
    getAiVideoVoices,
    previewAiVideoVoice,
    getAiVideoTemplates,
    expandAiVideoContent,
    getAiVideoModels,
    getAiDramaAvailable,
    getAiDramaProjects,
    createAiDramaProject,
    getAiDramaProject,
    updateAiDramaProject,
    deleteAiDramaProject,
    getAiDramaScript,
    updateAiDramaScript,
    getAiDramaEpisodes,
    getAiDramaEpisode,
    updateAiDramaEpisode,
    renameAiDramaEpisode,
    deleteAiDramaEpisode,
    getAiDramaAssets,
    getAllAiDramaAssets,
    createAiDramaAsset,
    updateAiDramaAsset,
    deleteAiDramaAsset,
    getAiDramaVoices,
    getAiDramaModels,
    generateAiDramaScriptSummary,
    generateAiDramaEpisodeScript,
    addAiDramaEpisode,
    seedAiDramaAssets,
    generateAiDramaAssetImage,
    uploadAiDramaAssetImage,
    planAiDramaFragments,
    saveAiDramaFragments,
    generateAiDramaFragments,
    getAiDramaGenerateStatus,
    generateAiDramaNarration,
    previewAiDramaFragmentVoice,
    composeAiDramaEpisode,
    generateAiDramaFragmentVideos,
    getAiBiAvailable,
    getAiBiCatalog,
    askBiQuery,
    deleteAiVideoTask,
    setAiVideoCover,
    setAiVideoFirstShotCover,
    confirmAiVideoAssets,
    composeAiVideo,
    regenerateAiVideoCopy,
    updateAiVideoShot,
    updateAiVideoPrompts,
    regenAiVideoShotImage,
    previewAiVideoShotVoice,
    generateAiVideoShots,
    regenAiVideoShotVideo,
    uploadAiVideoShotImage,
    getAiToolAvailable,
    streamAiToolText,
    createAiToolImage,
    createAiToolTranscode,
    getAiToolTasks,
    getAiToolTasksPage,
    deleteAiToolTask,
    getAiVoiceCloneAvailable,
    submitAiVoiceClone,
    getAiVoiceCloneTasks,
    getAiVoiceCloneTask,
    deleteAiVoiceCloneTask,
    previewAiVoiceClone,
    downloadAiSalesScriptTemplate,
    preCheckImportAiSalesScript,
    importAiSalesScript,
    getAiReplicaAvailable,
    createAiReplicaTask,
    getAiReplicaTasks,
    getAiReplicaTask,
    regenerateAiReplicaTask,
    deleteAiReplicaTask,
    deleteAiReplicaVariant,
  };
}
