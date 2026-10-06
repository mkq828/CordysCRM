<template>
  <div class="agent-task-chat flex h-full flex-col">
    <!-- 顶部：岗位身份 + 返回会议室 -->
    <header class="chat-hud">
      <div class="hud-title">
        <span class="hud-avatar">
          <svg viewBox="0 0 40 40" fill="none" aria-hidden="true">
            <circle cx="20" cy="15" r="7" fill="currentColor" />
            <path d="M20 24c-6.4 0-11.6 4.8-11.6 10.8V37h23.2v-2.2C31.6 28.8 26.4 24 20 24z" fill="currentColor" />
          </svg>
        </span>
        <span class="hud-name">{{ agent.name }}</span>
      </div>
      <n-button size="tiny" quaternary @click="emit('back')">{{ t('aiEmployee.backToMeeting') }}</n-button>
    </header>

    <div class="chat-hint">{{ t('aiEmployee.taskHint') }}</div>

    <!-- 消息区 + 工具步骤 -->
    <div ref="listRef" class="chat-body min-h-0 flex-1 overflow-y-auto">
      <div v-if="messages.length === 0" class="chat-empty">
        <div class="chat-empty-icon">
          <svg viewBox="0 0 40 40" fill="none" aria-hidden="true">
            <circle cx="20" cy="15" r="7" fill="currentColor" />
            <path d="M20 24c-6.4 0-11.6 4.8-11.6 10.8V37h23.2v-2.2C31.6 28.8 26.4 24 20 24z" fill="currentColor" />
          </svg>
        </div>
        <p>{{ t('aiEmployee.taskEmpty') }}</p>
      </div>

      <template v-for="(msg, index) in messages" :key="index">
        <div class="msg" :class="msg.role === 'user' ? 'out' : 'in'">
          <span v-if="msg.role === 'assistant'" class="msg-avatar">
            <svg viewBox="0 0 40 40" fill="none" aria-hidden="true">
              <circle cx="20" cy="15" r="7" fill="currentColor" />
              <path d="M20 24c-6.4 0-11.6 4.8-11.6 10.8V37h23.2v-2.2C31.6 28.8 26.4 24 20 24z" fill="currentColor" />
            </svg>
          </span>
          <div class="msg-body">
            <!-- 工具执行步骤：与头像顶部对齐，显示在当前轮岗位输出内容的上方 -->
            <div v-if="msg.role === 'assistant' && index === messages.length - 1 && steps.length > 0" class="steps">
              <div v-for="step in steps" :key="`${step.sequence}-${step.actionId}`" class="step">
                <div class="step-head">
                  <span v-if="step.status === 'running'" class="step-spin" aria-hidden="true"></span>
                  <span v-else class="step-check" aria-hidden="true">✓</span>
                  <span class="step-title" :class="{ 'is-running': step.status === 'running' }">{{ step.title }}</span>
                </div>
              </div>
            </div>
            <div class="bubble" :class="bubbleClass(msg)">
              <ShimmerText v-if="msg.role === 'assistant' && msg.streaming && !msg.content" size="14px">
                {{ t('aiEmployee.thinking') }}
              </ShimmerText>
              <span v-else-if="msg.role === 'user'">{{ msg.content }}</span>
              <div
                v-else-if="msg.role === 'assistant' && isHtmlReport(msg.content) && msg.streaming"
                class="report-preview"
              >
                {{ reportPreviewText(msg.content) }}
              </div>
              <AiReportBlock v-else-if="msg.role === 'assistant' && isHtmlReport(msg.content)" :html="msg.content" />
              <div v-else class="md-body" v-html="markdownHtml(msg)"></div>
              <span v-if="msg.streaming && msg.content && !msg.report" class="cursor-blink">▍</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 修改确认卡：AI 只提议，用户点「确认修改」后才真正落库 -->
      <div v-if="pendingUpdate" class="confirm-card">
        <div class="confirm-title">{{ t('aiEmployee.updateConfirmTitle') }}</div>
        <div v-for="change in pendingUpdate.changes" :key="change.fieldId" class="confirm-row">
          <span class="confirm-label">{{ change.fieldLabel }}</span>
          <span class="confirm-old">{{ change.oldValue || '-' }}</span>
          <span class="confirm-arrow">→</span>
          <span class="confirm-new">{{ change.newValue }}</span>
        </div>
        <div class="confirm-actions">
          <n-button size="tiny" :disabled="applying" @click="cancelApply">
            {{ t('aiEmployee.updateCancel') }}
          </n-button>
          <n-button size="tiny" type="primary" :loading="applying" @click="confirmApply">
            {{ t('aiEmployee.updateConfirm') }}
          </n-button>
        </div>
      </div>
    </div>

    <!-- 底部输入框 -->
    <div class="composer">
      <n-input
        v-model:value="question"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        :placeholder="t('aiEmployee.taskPlaceholder')"
        :disabled="sending"
        @keydown.enter.exact.prevent="send"
      />
      <n-button type="primary" :loading="sending" :disabled="!canSend" @click="send">
        {{ t('aiEmployee.taskSend') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, reactive, ref, watch } from 'vue';
  import { NButton, NInput, useMessage } from 'naive-ui';

  import { renderMarkdown } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AgentChatProgressData,
    AgentTaskApplyRequest,
    AgentUpdateProposal,
    AiEmployeeAgent,
  } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiReportBlock from './AiReportBlock.vue';

  import { applyAiEmployeeTask, streamAiEmployeeTask } from '@/api/modules';
  import useTypewriter from '@/hooks/useTypewriter';

  interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    streaming?: boolean;
    /** 渲染后的 markdown HTML，历史消息缓存、流式消息每次实时渲染 */
    html?: string;
    /** 是否探测为「客户全景分析」HTML 报告：逐字揭示期间隐藏 Markdown 光标，报告走 AiReportBlock 渲染 */
    report?: boolean;
  }

  interface TaskStep {
    sequence: number;
    actionId: string;
    title: string;
    status: string;
  }

  const props = defineProps<{
    agent: AiEmployeeAgent;
  }>();

  const emit = defineEmits<{
    (e: 'back'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const question = ref('');
  const messages = ref<ChatMessage[]>([]);
  const steps = ref<TaskStep[]>([]);
  const sending = ref(false);
  const conversationId = ref<string | undefined>(undefined);
  const listRef = ref<HTMLElement>();
  const pendingUpdate = ref<AgentUpdateProposal | null>(null);
  const applying = ref(false);

  function scrollToBottom() {
    nextTick(() => {
      const el = listRef.value;
      if (!el) {
        return;
      }
      const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 64;
      if (nearBottom) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }

  // 助手回答按 Markdown 渲染；流式消息实时重渲染，历史消息缓存避免重复解析
  function markdownHtml(msg: ChatMessage): string {
    if (msg.streaming || msg.html === undefined) {
      msg.html = renderMarkdown(msg.content, { copyText: t('common.copy') });
    }
    return msg.html;
  }

  // 判定一段输出是否为「客户全景分析」HTML 报告：以 html/div/section 等标签开头且含约定 class
  function isHtmlReport(content: string): boolean {
    return /^\s*(<!DOCTYPE|<html|<div|<section|<header)/i.test(content) && content.includes('ai-report');
  }

  // 流式期间把未完成的 HTML 报告剥成纯文本预览：只显示已输出的文字，不渲染半成品卡片，避免逐字重建 DOM 闪烁。
  // 除完整标签外，还要处理「正在输入、尚未闭合的末尾标签」，否则流式期间会闪现 <div class=… 这类代码
  function reportPreviewText(html: string): string {
    return html
      .replace(/<[^>]*>/g, ' ')
      .replace(/<[^>]*$/g, ' ')
      .replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/\s+/g, ' ')
      .trim();
  }

  // 报告探测上限：累积这么多字符仍无法判定为报告，就按 Markdown 处理
  const REPORT_PROBE_MAX = 200;

  // 气泡样式：HTML 报告去掉内边距、加宽，让 iframe 报告占满
  function bubbleClass(msg: ChatMessage): string {
    if (msg.role === 'user') {
      return 'bubble--user';
    }
    if (msg.report || isHtmlReport(msg.content)) {
      return 'bubble--report';
    }
    return 'bubble--assistant';
  }

  // 当前正在揭示的 assistant 消息，逐字写入其 content
  const currentAssistant = ref<ChatMessage | null>(null);
  const typewriter = useTypewriter({ onTick: scrollToBottom });

  watch(typewriter.text, (value) => {
    if (currentAssistant.value) {
      currentAssistant.value.content = value;
    }
  });

  const canSend = computed(() => !sending.value && question.value.trim() !== '');

  function upsertStep(progress: AgentChatProgressData) {
    const existing = steps.value.find((s) => s.sequence === progress.sequence && s.actionId === progress.actionId);
    if (existing) {
      existing.status = progress.status;
      existing.title = progress.title;
      return;
    }
    steps.value.push({
      sequence: progress.sequence,
      actionId: progress.actionId,
      title: progress.title,
      status: progress.status,
    });
  }

  async function confirmApply() {
    const proposal = pendingUpdate.value;
    if (!proposal || applying.value) {
      return;
    }
    applying.value = true;
    const applyRequest: AgentTaskApplyRequest = {
      customerId: proposal.customerId,
      changes: proposal.changes.map((change) => ({ fieldId: change.fieldId, newValue: change.newValue })),
    };
    try {
      await applyAiEmployeeTask(props.agent.id, applyRequest);
      Message.success(t('aiEmployee.updateApplied'));
      pendingUpdate.value = null;
      const receipt = proposal.changes
        .map((change) => `${change.fieldLabel}：${change.oldValue || '-'} → ${change.newValue}`)
        .join('；');
      messages.value.push({ role: 'assistant', content: `${t('aiEmployee.updateApplied')}：${receipt}` });
      scrollToBottom();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      applying.value = false;
    }
  }

  function cancelApply() {
    pendingUpdate.value = null;
  }

  async function send() {
    const text = question.value.trim();
    if (!text || sending.value) {
      return;
    }
    question.value = '';
    messages.value.push({ role: 'user', content: text });
    const assistant = reactive<ChatMessage>({ role: 'assistant', content: '', streaming: true });
    messages.value.push(assistant);
    currentAssistant.value = assistant;
    steps.value = [];
    typewriter.reset();
    sending.value = true;
    scrollToBottom();

    try {
      const stream = streamAiEmployeeTask(props.agent.id, {
        message: text,
        conversationId: conversationId.value,
      });

      // 报告探测：确认是 HTML 报告后仍走 typewriter 逐字揭示（与豆包一致），
      // 探测期只缓冲不渲染，避免「<div class="ai-」被 Markdown 渲染成半成品再切到报告的闪烁
      let reportProbe = '';
      let reportDetected = false;
      const feedChunk = (chunk: string) => {
        if (!reportDetected) {
          reportProbe += chunk;
          const trimmed = reportProbe.trim();
          const looksLikeHtml = /^\s*(<!DOCTYPE|<html|<div|<section|<header)/i.test(trimmed);
          if (looksLikeHtml && trimmed.includes('ai-report')) {
            reportDetected = true;
            assistant.report = true;
            typewriter.feed(trimmed);
            reportProbe = '';
            return;
          }
          if (trimmed.length > REPORT_PROBE_MAX || (trimmed.length > 0 && !/^\s*</.test(trimmed))) {
            reportDetected = true;
            typewriter.feed(reportProbe);
            reportProbe = '';
          }
          return;
        }
        typewriter.feed(chunk);
      };

      // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，顺序消费流式事件
      for await (const event of stream) {
        if (event.type === 'run') {
          conversationId.value = event.run?.conversationId || conversationId.value;
        } else if (event.type === 'progress' && event.progress) {
          upsertStep(event.progress);
        } else if (event.type === 'chunk') {
          feedChunk(event.content || '');
        } else if (event.type === 'done' && event.data?.payload) {
          const payload = event.data.payload as AgentUpdateProposal;
          if (payload?.type === 'customer_update') {
            pendingUpdate.value = payload;
          }
        } else if (event.type === 'error') {
          throw new Error(event.errorMessage);
        }
      }
      await typewriter.finish();
    } catch (error) {
      typewriter.flush();
      if (assistant.report) {
        // 报告生成失败：清空半成品，回退为普通文本展示错误原因
        assistant.report = false;
        assistant.content = '';
      } else {
        assistant.content = typewriter.text.value;
      }
      if (!assistant.content) {
        assistant.content = (error as Error)?.message || t('common.operationFailed');
      } else {
        Message.error((error as Error)?.message || t('common.operationFailed'));
      }
    } finally {
      assistant.streaming = false;
      currentAssistant.value = null;
      sending.value = false;
      scrollToBottom();
    }
  }
</script>

<style lang="less" scoped>
  .agent-task-chat {
    background: #f7f8fa;
  }
  .chat-hud {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    border-bottom: 1px solid var(--divider-color);
    background: #ffffff;
  }
  .hud-title {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .hud-avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    color: #ffffff;
    background: radial-gradient(circle at 34% 26%, #fdba74, #f97316 48%, #ea580c);
    flex-shrink: 0;
    svg {
      width: 17px;
      height: 17px;
    }
  }
  .hud-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .chat-hint {
    padding: 8px 14px;
    font-size: 12px;
    color: #f97316;
    background: rgb(249 115 22 / 6%);
  }
  .chat-body {
    padding: 14px;
  }
  .chat-empty {
    display: flex;
    align-items: center;
    padding-top: 64px;
    font-size: 13px;
    text-align: center;
    color: var(--text-n3);
    flex-direction: column;
    gap: 10px;
  }
  .chat-empty-icon {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    color: #ffffff;
    background: radial-gradient(circle at 34% 26%, #fdba74, #f97316 48%, #ea580c);
    svg {
      width: 30px;
      height: 30px;
    }
  }
  .msg {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 12px;
    &.out {
      justify-content: flex-end;
    }
  }

  // 头像右侧的纵向容器：步骤（上）与气泡/报告（下）都从这里开始，与头像顶部对齐
  .msg-body {
    display: flex;
    flex-direction: column;
    flex: 1 1 0%;
    min-width: 0;
  }
  .msg.out .msg-body {
    align-items: flex-end;
  }
  .msg-avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    color: #ffffff;
    background: radial-gradient(circle at 34% 26%, #fdba74, #f97316 48%, #ea580c);
    flex-shrink: 0;
    svg {
      width: 18px;
      height: 18px;
    }
  }
  .bubble {
    padding: 9px 12px;
    max-width: 76%;
    font-size: 13px;
    border-radius: 10px;
    white-space: pre-wrap;
    line-height: 1.6;
    word-break: break-word;
  }
  .bubble--user {
    color: #ffffff;
    background: var(--primary-8);
    border-top-right-radius: 2px;
  }
  .bubble--assistant {
    border: 1px solid var(--divider-color);
    color: var(--text-n1);
    background: #ffffff;
    border-top-left-radius: 2px;
  }
  .bubble--report {
    overflow: hidden;
    padding: 0;
    width: 100%;
    min-width: 0;
    max-width: 100%;
    border: 0;
    border-radius: 12px;
    white-space: normal;
    background: transparent;
  }

  // 报告流式期间的纯文本预览（完成后切换为卡片渲染）
  .report-preview {
    padding: 12px 14px;
    font-size: 13px;
    border: 1px solid var(--divider-color);
    border-radius: 12px;
    white-space: pre-wrap;
    color: var(--text-n1);
    background: #ffffff;
    line-height: 1.7;
    word-break: break-word;
  }
  .cursor-blink {
    color: var(--primary-8);
    animation: cursor-blink 1s steps(1) infinite;
  }

  @keyframes cursor-blink {
    50% {
      opacity: 0;
    }
  }
  .steps {
    margin: 0 0 10px;
    padding: 10px 12px;
    border: 1px solid var(--divider-color);
    border-radius: 8px;
    background: #ffffff;
  }
  .step {
    padding: 4px 0;
    & + .step {
      border-top: 1px dashed var(--divider-color);
    }
  }
  .step-head {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .step-spin {
    width: 12px;
    height: 12px;
    border: 2px solid rgb(249 115 22 / 25%);
    border-top-color: #f97316;
    border-radius: 50%;
    animation: step-spin 0.8s linear infinite;
  }

  @keyframes step-spin {
    to {
      transform: rotate(360deg);
    }
  }
  .step-check {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    width: 14px;
    height: 14px;
    font-size: 11px;
    border-radius: 50%;
    color: #ffffff;
    background: #16a34a;
  }
  .step-title {
    font-size: 12px;
    color: var(--text-n2);
    &.is-running {
      font-weight: 500;
      color: #f97316;
    }
  }
  .composer {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    padding: 10px 14px;
    border-top: 1px solid var(--divider-color);
    background: #ffffff;
  }

  // Markdown 渲染区（与 AiMarkdownBlock.vue 保持一致，只保留 CRM 回答会用到的块）
  .md-body {
    white-space: normal;
    :deep(p),
    :deep(blockquote),
    :deep(hr),
    :deep(ol),
    :deep(pre),
    :deep(ul),
    :deep(.ai-code-block),
    :deep(.ai-table-wrapper) {
      margin: 0 0 12px;
    }
    :deep(h1),
    :deep(h2),
    :deep(h3),
    :deep(h4),
    :deep(h5),
    :deep(h6) {
      margin: 24px 0 10px;
      font-weight: 600;
      color: var(--text-n1);
      line-height: 1.35;
    }
    :deep(h1) {
      padding-bottom: 8px;
      font-size: 24px;
    }
    :deep(h2) {
      padding-bottom: 6px;
      font-size: 20px;
    }
    :deep(h3) {
      font-size: 18px;
    }
    :deep(h4) {
      font-size: 16px;
    }
    :deep(h5) {
      font-size: 14px;
    }
    :deep(h6) {
      font-size: 14px;
      color: var(--text-n3);
    }
    :deep(ul) {
      padding-left: 24px;
      list-style: disc;
    }
    :deep(ol) {
      padding-left: 24px;
      list-style: decimal;
    }
    :deep(ul ul),
    :deep(ol ul) {
      list-style: circle;
    }
    :deep(ol ol),
    :deep(ul ol) {
      list-style: lower-alpha;
    }
    :deep(li + li) {
      margin-top: 4px;
    }
    :deep(li > p) {
      margin: 0;
    }
    :deep(a) {
      text-decoration: none;
      color: var(--primary-8);
    }
    :deep(a:hover) {
      text-decoration: underline;
    }
    :deep(strong) {
      font-weight: 600;
    }
    :deep(del) {
      color: var(--text-n3);
    }
    :deep(code) {
      padding: 2px 4px;
      font-size: 12px;
      border-radius: 4px;
      background: var(--text-n8);
    }
    :deep(pre) {
      overflow: auto;
      border-radius: 6px;
      background: var(--text-n9);
    }
    :deep(pre code) {
      padding: 0;
      background: transparent;
    }
    :deep(.ai-code-block) {
      padding: 0;
      border-radius: 6px;
      background: var(--text-n9);
    }
    :deep(.ai-code-block__header) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 6px 10px;
      font-size: 12px;
      border-bottom: 1px solid var(--text-n8);
      color: var(--text-n4);
      gap: 8px;
    }
    :deep(.ai-code-block__lang) {
      overflow: hidden;
      min-width: 0;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    :deep(.ai-code-block__lang:empty::before) {
      content: 'text';
    }
    :deep(.ai-code-block__copy) {
      padding: 0;
      font-size: 12px;
      border: 0;
      color: var(--primary-8);
      background: transparent;
      flex: none;
      cursor: pointer;
    }
    :deep(.ai-code-block__body) {
      display: block;
      overflow: auto;
      padding: 10px 12px;
      background: transparent;
    }
    :deep(.katex-display) {
      overflow-x: auto;
      overflow-y: hidden;
      padding: 2px 0;
    }
    :deep(blockquote) {
      padding: 0 0 0 12px;
      border-left: 4px solid var(--text-n8);
      color: var(--text-n3);
    }
    :deep(hr) {
      height: 1px;
      border: 0;
      background: var(--text-n8);
    }
    :deep(.ai-table-wrapper) {
      overflow-x: auto;
      width: 100%;
      max-width: 100%;
    }
    :deep(table) {
      width: 100%;
      font-size: 14px;
      border-top: 1px solid var(--text-n9);
      border-bottom: 1px solid var(--text-n9);
      background: var(--text-n10);
      border-collapse: collapse;
    }
    :deep(.ai-table-wrapper table) {
      width: max-content;
      min-width: 100%;
    }
    :deep(th),
    :deep(td) {
      padding: 0 10px;
      height: 46px;
      border-top: 1px solid var(--text-n9);
      border-bottom: 1px solid var(--text-n9);
      text-align: left;
      vertical-align: middle;
    }
    :deep(th) {
      font-weight: 500;
      white-space: nowrap;
      color: var(--text-n4);
      background: var(--text-n10);
    }
    :deep(td) {
      max-width: 320px;
      overflow-wrap: anywhere;
      white-space: normal;
      color: var(--text-n1);
      background: var(--text-n10);
      word-break: break-word;
    }
  }

  // 修改确认卡
  .confirm-card {
    margin: 4px 0 12px 38px;
    padding: 12px;
    border: 1px solid #fdba74;
    border-radius: 8px;
    background: #fff7ed;
  }
  .confirm-title {
    margin-bottom: 10px;
    font-size: 13px;
    font-weight: 600;
    color: #c2410c;
  }
  .confirm-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
    font-size: 13px;
    & + .confirm-row {
      border-top: 1px dashed #fdba74;
    }
  }
  .confirm-label {
    flex-shrink: 0;
    min-width: 56px;
    color: var(--text-n2);
  }
  .confirm-old {
    text-decoration: line-through;
    color: var(--text-n3);
    word-break: break-all;
  }
  .confirm-arrow {
    flex-shrink: 0;
    color: #f97316;
  }
  .confirm-new {
    font-weight: 600;
    color: #c2410c;
    word-break: break-all;
  }
  .confirm-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 10px;
  }
</style>
