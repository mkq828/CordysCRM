<template>
  <div class="meeting-room flex h-full flex-col">
    <!-- 顶部 HUD 状态条 -->
    <header class="room-hud">
      <div class="hud-title">
        <span class="hud-dot"></span>
        <span class="hud-name">{{ t('aiEmployee.meetingRoom') }}</span>
        <span class="hud-live">{{ t('aiEmployee.liveBadge') }}</span>
      </div>
      <div class="hud-meta">
        <span class="hud-chip">{{ t('aiEmployee.attendeeCount', { count: attendees.length }) }}</span>
      </div>
    </header>

    <!-- 会议区域：左席位 | 会议桌（发言大屏） | 右席位 -->
    <div class="meeting min-h-0 flex-1" :style="{ '--seat-scale': seatScale }">
      <!-- 左席位（1,3,5...） -->
      <aside class="side side--left">
        <div v-for="a in leftSeats" :key="a.agentId" class="person">
          <span class="seat-label">{{ a.name }}</span>
          <span class="seat-avatar">
            <svg viewBox="0 0 40 40" fill="none" aria-hidden="true">
              <circle cx="20" cy="15" r="7" fill="currentColor" />
              <path d="M20 24c-6.4 0-11.6 4.8-11.6 10.8V37h23.2v-2.2C31.6 28.8 26.4 24 20 24z" fill="currentColor" />
            </svg>
          </span>
        </div>
      </aside>

      <!-- 中间会议桌 -->
      <div ref="listRef" class="table">
        <i class="corner corner--tl"></i>
        <i class="corner corner--tr"></i>
        <i class="corner corner--bl"></i>
        <i class="corner corner--br"></i>
        <div v-if="transcript.length === 0" class="table-empty">
          <div class="table-empty-icon">🫧</div>
          <p>{{ t('aiEmployee.inviteHint') }}</p>
        </div>
        <template v-for="(msg, index) in transcript" :key="index">
          <div v-if="msg.time" class="time">{{ msg.time }}</div>
          <div class="msg" :class="msg.role === 'chairman' ? 'out' : 'in'">
            <div class="who">{{ msg.speaker }}</div>
            <div class="bubble">
              <ShimmerText v-if="msg.streaming && !msg.content" size="14px">
                {{ t('aiEmployee.thinking') }}
              </ShimmerText>
              <span v-else>{{ msg.content }}</span>
              <span v-if="msg.streaming && msg.content" class="cursor-blink">▍</span>
            </div>
          </div>
        </template>
      </div>

      <!-- 右席位（2,4,6...） -->
      <aside class="side side--right">
        <div v-for="a in rightSeats" :key="a.agentId" class="person">
          <span class="seat-label">{{ a.name }}</span>
          <span class="seat-avatar">
            <svg viewBox="0 0 40 40" fill="none" aria-hidden="true">
              <circle cx="20" cy="15" r="7" fill="currentColor" />
              <path d="M20 24c-6.4 0-11.6 4.8-11.6 10.8V37h23.2v-2.2C31.6 28.8 26.4 24 20 24z" fill="currentColor" />
            </svg>
          </span>
        </div>
      </aside>
    </div>

    <!-- 底部：董事长输入框 + 会议结束 -->
    <div class="composer">
      <div class="composer-head">
        <span class="composer-title">{{ t('aiEmployee.chairman') }}<em>·</em>{{ chairmanName }}</span>
        <button
          class="end-btn"
          type="button"
          :disabled="sending || generating || transcript.length === 0"
          @click="endMeeting"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <path d="M16 17l5-5-5-5" />
            <path d="M21 12H9" />
          </svg>
          <span>{{ t('aiEmployee.endMeeting') }}</span>
        </button>
      </div>

      <div class="input-wrap">
        <textarea
          v-model="question"
          :placeholder="t('aiEmployee.meetingPlaceholder')"
          :disabled="sending || generating"
          @keydown.enter.exact.prevent="send"
        ></textarea>
        <button class="send" type="button" aria-label="发送" :disabled="!canSend" @click="send">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 19V5" />
            <path d="M5.5 11.5L12 5l6.5 6.5" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, reactive, ref, watch } from 'vue';
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentChatStreamEvent, AiMeetingMinutes } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';

  import { generateAiMeetingMinutes, streamAiEmployeeChat } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  interface Attendee {
    agentId: string;
    name: string;
    emoji?: string;
    color?: string;
  }

  interface Speech {
    role: 'chairman' | 'agent';
    speaker: string;
    emoji?: string;
    content: string;
    streaming?: boolean;
    time?: string;
  }

  const props = defineProps<{
    attendees: Attendee[];
  }>();

  const emit = defineEmits<{
    (e: 'minutesGenerated', minutes: AiMeetingMinutes): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const userStore = useUserStore();

  const chairmanName = computed(() => userStore.userInfo.name || '董事长');

  const question = ref('');
  const transcript = ref<Speech[]>([]);
  const sending = ref(false);
  const generating = ref(false);
  const meetingStartTime = ref(0);
  const listRef = ref<HTMLElement>();

  // 打字机节奏：逐字揭示的每拍字符数与间隔（毫秒），模拟豆包式逐字输出
  const TYPEWRITER_CHARS_PER_TICK = 3;
  const TYPEWRITER_TICK_MS = 30;

  // 席位分列：左 1,3,5… / 右 2,4,6…（发言顺序仍是原始顺序）
  const leftSeats = computed(() => props.attendees.filter((_, i) => i % 2 === 0));
  const rightSeats = computed(() => props.attendees.filter((_, i) => i % 2 === 1));

  // 席位自适应缩放：单侧人数越多缩得越小，保证一屏展示
  const seatScale = computed(() => {
    const perSide = Math.max(leftSeats.value.length, rightSeats.value.length);
    if (perSide <= 3) return 1;
    if (perSide <= 4) return 0.88;
    if (perSide <= 6) return 0.74;
    if (perSide <= 9) return 0.6;
    return 0.5;
  });

  const canSend = computed(
    () => !sending.value && !generating.value && question.value.trim() !== '' && props.attendees.length > 0
  );

  const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];

  function pad(n: number): string {
    return String(n).padStart(2, '0');
  }

  function nowStamp(): string {
    const d = new Date();
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
      d.getMinutes()
    )}`;
  }

  function scrollToBottom() {
    nextTick(() => {
      const el = listRef.value;
      if (!el) {
        return;
      }
      // 仅当用户本就贴近底部时才跟随滚动；用户上翻查看历史发言时不打扰，避免被拽回底部
      const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 64;
      if (nearBottom) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }

  function buildTimeText(): string {
    const start = meetingStartTime.value ? new Date(meetingStartTime.value) : new Date();
    const end = new Date();
    const duration = Math.max(1, Math.round((end.getTime() - start.getTime()) / 60000));
    return `${start.getFullYear()}年${start.getMonth() + 1}月${start.getDate()}日 ${WEEKDAYS[start.getDay()]} ${pad(
      start.getHours()
    )}:${pad(start.getMinutes())} — ${pad(end.getHours())}:${pad(end.getMinutes())}（时长 ${duration} 分钟）`;
  }

  // 逐字揭示：网络流式内容先进缓冲，再按固定节奏逐字写入气泡，模拟豆包式打字机效果
  async function revealSpeech(entry: Speech, stream: AsyncIterable<AgentChatStreamEvent>) {
    let pending = '';
    let streamFinished = false;
    let finishReveal: (() => void) | undefined;
    const revealDone = new Promise<void>((resolve) => {
      finishReveal = resolve;
    });

    const revealTimer = window.setInterval(() => {
      if (pending.length > 0) {
        entry.content += pending.slice(0, TYPEWRITER_CHARS_PER_TICK);
        pending = pending.slice(TYPEWRITER_CHARS_PER_TICK);
        scrollToBottom();
      } else if (streamFinished && finishReveal) {
        clearInterval(revealTimer);
        finishReveal();
        finishReveal = undefined;
      }
    }, TYPEWRITER_TICK_MS);

    let streamError: Error | undefined;
    try {
      // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，顺序消费流式事件
      for await (const event of stream) {
        if (event.type === 'chunk') {
          pending += event.content || '';
        } else if (event.type === 'error') {
          streamError = new Error(event.errorMessage || t('common.operationFailed'));
          break;
        }
      }
    } catch (error) {
      streamError = error as Error;
    }

    streamFinished = true;

    if (streamError) {
      clearInterval(revealTimer);
      // 出错时把已缓冲内容一次性补全，避免丢字
      if (pending) {
        entry.content += pending;
        pending = '';
      }
      entry.streaming = false;
      throw streamError;
    }

    // 等缓冲区逐字揭示完毕后，再标记发言结束、轮到下一位
    await revealDone;
    entry.streaming = false;
    scrollToBottom();
  }

  async function send() {
    if (!canSend.value) {
      return;
    }
    const text = question.value.trim();
    question.value = '';
    if (meetingStartTime.value === 0) {
      meetingStartTime.value = Date.now();
    }
    transcript.value.push({ role: 'chairman', speaker: chairmanName.value, content: text, time: nowStamp() });
    sending.value = true;
    scrollToBottom();

    try {
      // eslint-disable-next-line no-restricted-syntax -- 顺序编排参会岗位依次发言
      for (const attendee of props.attendees) {
        // 用 reactive 包裹，逐字揭示时 entry.content 的每次变化都能触发视图更新
        const entry = reactive<Speech>({
          role: 'agent',
          speaker: attendee.name,
          emoji: attendee.emoji,
          content: '',
          streaming: true,
        });
        transcript.value.push(entry);
        scrollToBottom();

        const prior = transcript.value
          .filter((s) => s.role === 'agent' && s.content)
          .map((s) => `${s.speaker}：${s.content}`)
          .join('\n');

        const stream = streamAiEmployeeChat(attendee.agentId, {
          message: text,
          context: prior || undefined,
        });

        // eslint-disable-next-line no-await-in-loop -- 顺序编排，逐位等待发言揭示完成
        await revealSpeech(entry, stream);
      }
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      sending.value = false;
      scrollToBottom();
    }
  }

  function endMeeting() {
    if (sending.value || transcript.value.length === 0) {
      return;
    }
    openModal({
      type: 'warning',
      title: t('aiEmployee.endMeetingConfirmTitle'),
      content: t('aiEmployee.endMeetingConfirmContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        generating.value = true;
        try {
          const minutes = await generateAiMeetingMinutes({
            timeText: buildTimeText(),
            host: chairmanName.value,
            attendees: [chairmanName.value, ...props.attendees.map((a) => a.name)],
            transcript: transcript.value.map((s) => ({ speaker: s.speaker, content: s.content })),
          });
          transcript.value = [];
          question.value = '';
          meetingStartTime.value = 0;
          emit('minutesGenerated', minutes);
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        } finally {
          generating.value = false;
        }
      },
    });
  }

  // 外部清空参会人（如纪要生成后重置）时，同步清空本地发言流
  watch(
    () => props.attendees.length,
    (len) => {
      if (len === 0 && !sending.value) {
        transcript.value = [];
        question.value = '';
        meetingStartTime.value = 0;
      }
    }
  );
</script>

<style lang="less" scoped>
  .meeting-room {
    position: relative;
    overflow: hidden;
    padding: 14px;
    background: radial-gradient(1100px 560px at 50% -12%, #14335a 0%, transparent 62%),
      radial-gradient(860px 480px at 100% 100%, #0d2f3f 0%, transparent 55%),
      linear-gradient(180deg, #060a14 0%, #081020 100%);
  }
  .meeting-room::before {
    position: absolute;
    inset: 0;
    background-image: linear-gradient(rgb(34 211 238 / 5%) 1px, transparent 1px),
      linear-gradient(90deg, rgb(34 211 238 / 5%) 1px, transparent 1px);
    background-size: 42px 42px;
    content: '';
    pointer-events: none;
  }
  .meeting-room::after {
    position: absolute;
    inset: 0;
    background: radial-gradient(640px 320px at 50% 118%, rgb(34 211 238 / 12%), transparent 60%);
    content: '';
    pointer-events: none;
  }
  .room-hud {
    position: relative;
    z-index: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding: 8px 14px;
    border: 1px solid rgb(34 211 238 / 24%);
    border-radius: 10px;
    background: linear-gradient(90deg, rgb(34 211 238 / 12%), rgb(34 211 238 / 2%) 55%, transparent);
    box-shadow: inset 0 0 20px rgb(34 211 238 / 8%);
  }
  .hud-title {
    display: flex;
    align-items: center;
    gap: 9px;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 2px;
    color: #bff4ff;
  }
  .hud-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #22d3ee;
    box-shadow: 0 0 10px #22d3ee;
    animation: hudPulse 1.6s ease-in-out infinite;
  }
  @keyframes hudPulse {
    0%,
    100% {
      opacity: 1;
      box-shadow: 0 0 6px #22d3ee;
    }
    50% {
      opacity: 0.4;
      box-shadow: 0 0 14px #22d3ee;
    }
  }
  .hud-live {
    padding: 2px 8px;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 2px;
    border-radius: 4px;
    color: #03151a;
    background: linear-gradient(135deg, #67e8f9, #22d3ee);
    box-shadow: 0 0 12px rgb(34 211 238 / 60%);
  }
  .hud-meta {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .hud-chip {
    padding: 3px 11px;
    font-size: 11px;
    letter-spacing: 1px;
    border: 1px solid rgb(34 211 238 / 30%);
    border-radius: 999px;
    color: #7dd3fc;
    background: rgb(8 20 34 / 60%);
  }
  .meeting {
    position: relative;
    z-index: 1;
    display: grid;
    grid-template-columns: 140px minmax(0, 1fr) 140px;
    grid-template-rows: minmax(0, 1fr);
    gap: 12px;
    align-items: stretch;
  }
  .side {
    display: flex;
    justify-content: space-evenly;
    overflow-y: auto;
    padding: 6px 0;
    min-height: 0;
    flex-direction: column;
    gap: calc(10px * var(--seat-scale, 1));
  }
  .person {
    display: flex;
    align-items: center;
    flex-direction: column;
    gap: calc(8px * var(--seat-scale, 1));
    width: 100%;
    transition: transform 0.28s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
  .person:hover {
    transform: translateY(-2px);
  }
  .seat-label {
    padding: calc(6px * var(--seat-scale, 1)) calc(10px * var(--seat-scale, 1));
    font-size: calc(12.5px * var(--seat-scale, 1));
    border: 1px solid rgb(34 211 238 / 34%);
    border-radius: 8px;
    text-align: center;
    color: #a5f3fc;
    background: rgb(8 22 38 / 72%);
    box-shadow: 0 0 12px rgb(34 211 238 / 10%), inset 0 0 8px rgb(34 211 238 / 6%);
    transition: color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
    line-height: 1.4;
    overflow-wrap: break-word;
    letter-spacing: 0.4px;
  }
  .person:hover .seat-label {
    border-color: #22d3ee;
    color: #e0fbff;
    box-shadow: 0 0 16px rgb(34 211 238 / 32%), inset 0 0 10px rgb(34 211 238 / 12%);
  }
  .seat-avatar {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    width: calc(46px * var(--seat-scale, 1));
    height: calc(46px * var(--seat-scale, 1));
    border-radius: 50%;
    color: #ffffff;
    background: radial-gradient(circle at 34% 26%, #67e8f9, #22d3ee 48%, #0891b2);
    box-shadow: 0 0 18px rgb(34 211 238 / 55%), inset 0 1.5px 2px rgb(255 255 255 / 55%);
    transition: transform 0.28s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.28s ease;
    flex: 0 0 auto;
  }
  .seat-avatar::after {
    position: absolute;
    inset: -4px;
    border: 1px solid rgb(34 211 238 / 45%);
    border-radius: 50%;
    content: '';
    animation: ringPulse 2.2s ease-out infinite;
    pointer-events: none;
  }
  @keyframes ringPulse {
    0% {
      transform: scale(0.9);
      opacity: 0.8;
    }
    70%,
    100% {
      transform: scale(1.2);
      opacity: 0;
    }
  }
  .seat-avatar svg {
    width: calc(26px * var(--seat-scale, 1));
    height: calc(26px * var(--seat-scale, 1));
  }
  .person:hover .seat-avatar {
    transform: scale(1.08);
    box-shadow: 0 0 26px rgb(34 211 238 / 82%), inset 0 1.5px 2px rgb(255 255 255 / 60%);
  }
  .table {
    position: relative;
    // 覆盖 Tailwind 的 .table 工具类（display: table），否则 overflow 不生效、长内容会撑爆
    display: block;
    overflow-x: hidden;
    overflow-y: auto;
    padding: 26px 28px;
    min-height: 0;
    border: 1px solid rgb(34 211 238 / 30%);
    border-radius: 16px;
    background: linear-gradient(180deg, rgb(10 18 34 / 88%), rgb(7 13 26 / 84%));
    box-shadow: 0 0 30px rgb(34 211 238 / 14%), inset 0 0 30px rgb(34 211 238 / 6%);
    scrollbar-width: thin;
    scrollbar-color: rgb(34 211 238 / 40%) transparent;
    scroll-behavior: smooth;
  }
  .table::before {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 3;
    width: 100%;
    height: 2px;
    background: linear-gradient(90deg, transparent, rgb(34 211 238 / 70%), transparent);
    content: '';
    animation: scanLine 6s linear infinite;
    pointer-events: none;
  }
  @keyframes scanLine {
    0% {
      top: 0;
      opacity: 0;
    }
    8% {
      opacity: 1;
    }
    92% {
      opacity: 1;
    }
    100% {
      top: 100%;
      opacity: 0;
    }
  }
  .corner {
    position: absolute;
    z-index: 4;
    width: 18px;
    height: 18px;
    pointer-events: none;
  }
  .corner--tl {
    top: -1px;
    left: -1px;
    border-top: 2px solid #22d3ee;
    border-left: 2px solid #22d3ee;
    border-top-left-radius: 16px;
  }
  .corner--tr {
    top: -1px;
    right: -1px;
    border-top: 2px solid #22d3ee;
    border-right: 2px solid #22d3ee;
    border-top-right-radius: 16px;
  }
  .corner--bl {
    bottom: -1px;
    left: -1px;
    border-bottom: 2px solid #22d3ee;
    border-left: 2px solid #22d3ee;
    border-bottom-left-radius: 16px;
  }
  .corner--br {
    right: -1px;
    bottom: -1px;
    border-right: 2px solid #22d3ee;
    border-bottom: 2px solid #22d3ee;
    border-bottom-right-radius: 16px;
  }
  .table::-webkit-scrollbar {
    width: 6px;
  }
  .table::-webkit-scrollbar-track {
    background: transparent;
  }
  .table::-webkit-scrollbar-thumb {
    border-radius: 99px;
    background: rgb(34 211 238 / 35%);
  }
  .table::-webkit-scrollbar-thumb:hover {
    background: rgb(34 211 238 / 60%);
  }
  .table-empty {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    font-size: 13px;
    color: rgb(148 163 184 / 70%);
    flex-direction: column;
    gap: 14px;
    letter-spacing: 0.6px;
  }
  .table-empty-icon {
    font-size: 40px;
    opacity: 0.7;
  }
  .time {
    margin: 22px 0 12px;
    font-size: 12px;
    text-align: center;
    color: rgb(148 163 184 / 62%);
    letter-spacing: 0.4px;
    font-variant-numeric: tabular-nums;
  }
  .msg {
    display: flex;
    flex-direction: column;
    animation: msgIn 0.5s cubic-bezier(0.2, 0.8, 0.2, 1) both;
  }
  .msg + .msg {
    margin-top: 18px;
  }
  @keyframes msgIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: none;
    }
  }
  .msg.out {
    align-items: flex-end;
  }
  .msg.in {
    align-items: flex-start;
  }
  .msg .who {
    margin-bottom: 10px;
    padding: 0 4px;
    font-size: 12.5px;
    color: #7dd3fc;
    letter-spacing: 0.4px;
  }
  .bubble {
    padding: 13px 18px;
    max-width: 82%;
    font-size: 13.5px;
    white-space: pre-wrap;
    line-height: 1.72;
    letter-spacing: 0.2px;
    word-break: break-word;
  }
  .msg.out .bubble {
    border-radius: 14px 14px 4px;
    color: #ffffff;
    background: linear-gradient(135deg, #22d3ee 0%, #0891b2 62%, #0e7490 100%);
    box-shadow: 0 12px 24px -14px rgb(34 211 238 / 70%), inset 0 1px 0 rgb(255 255 255 / 35%);
  }
  .msg.in .bubble {
    border: 1px solid rgb(34 211 238 / 24%);
    border-radius: 14px 14px 14px 4px;
    color: #dbeafe;
    background: linear-gradient(160deg, rgb(22 32 52 / 94%), rgb(13 20 36 / 94%));
    box-shadow: 0 12px 24px -16px rgb(0 0 0 / 60%), inset 0 1px 0 rgb(255 255 255 / 8%);
  }
  .cursor-blink {
    animation: blink 1s step-end infinite;
    color: #7dd3fc;
    opacity: 0.8;
  }
  @keyframes blink {
    50% {
      opacity: 0;
    }
  }
  .composer {
    position: relative;
    z-index: 1;
    flex-shrink: 0;
    padding-top: 12px;
  }
  .composer-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    gap: 12px;
  }
  .composer-title {
    overflow: hidden;
    font-size: 15px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #bff4ff;
    letter-spacing: 1px;
  }
  .composer-title em {
    margin: 0 8px;
    color: rgb(148 163 184 / 60%);
    font-style: normal;
  }
  .end-btn {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    padding: 0 16px;
    height: 38px;
    font-size: 13px;
    font-family: inherit;
    border: none;
    border-radius: 12px;
    white-space: nowrap;
    color: #ffffff;
    background: linear-gradient(145deg, #f87171, #ef4444 55%, #dc2626);
    box-shadow: 0 10px 22px -12px rgb(239 68 68 / 90%), inset 0 1px 0 rgb(255 255 255 / 30%);
    transition: transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.22s ease, opacity 0.2s ease;
    gap: 7px;
    letter-spacing: 1px;
    cursor: pointer;
    flex-shrink: 0;
  }
  .end-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 16px 28px -12px rgb(239 68 68 / 100%), inset 0 1px 0 rgb(255 255 255 / 40%);
  }
  .end-btn:active:not(:disabled) {
    transform: translateY(0) scale(0.97);
  }
  .end-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    box-shadow: none;
  }
  .end-btn svg {
    width: 16px;
    height: 16px;
  }
  .input-wrap {
    position: relative;
    padding: 14px 64px 14px 18px;
    border: 1px solid rgb(34 211 238 / 28%);
    border-radius: 16px;
    background: rgb(8 16 30 / 72%);
    box-shadow: 0 12px 26px -20px rgb(0 0 0 / 60%), inset 0 1px 0 rgb(255 255 255 / 6%);
    transition: border-color 0.25s ease, box-shadow 0.25s ease;
  }
  .input-wrap:focus-within {
    border-color: #22d3ee;
    box-shadow: 0 0 24px -8px rgb(34 211 238 / 60%), inset 0 1px 0 rgb(255 255 255 / 8%);
  }
  .input-wrap textarea {
    width: 100%;
    min-height: 46px;
    max-height: 96px;
    font-size: 14px;
    font-family: inherit;
    border: none;
    color: #dbeafe;
    background: transparent;
    outline: none;
    resize: none;
    line-height: 1.7;
  }
  .input-wrap textarea::placeholder {
    color: rgb(148 163 184 / 60%);
    letter-spacing: 0.5px;
  }
  .input-wrap textarea:disabled {
    opacity: 0.55;
  }
  .send {
    position: absolute;
    right: 12px;
    bottom: 12px;
    display: grid;
    width: 42px;
    height: 42px;
    border: none;
    border-radius: 50%;
    color: #03151a;
    background: linear-gradient(145deg, #67e8f9, #22d3ee 55%, #0891b2);
    box-shadow: 0 10px 22px -10px rgb(34 211 238 / 80%), inset 0 1px 0 rgb(255 255 255 / 45%);
    transition: transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.22s ease, opacity 0.2s ease;
    place-items: center;
    cursor: pointer;
  }
  .send:hover:not(:disabled) {
    transform: translateY(-3px) scale(1.06);
    box-shadow: 0 16px 28px -12px rgb(34 211 238 / 100%), inset 0 1px 0 rgb(255 255 255 / 55%);
  }
  .send:active:not(:disabled) {
    transform: translateY(0) scale(0.96);
  }
  .send:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
  .send svg {
    width: 19px;
    height: 19px;
  }
</style>
