<template>
  <div class="meeting-room flex h-full flex-col">
    <!-- 会议区域：左席位 | 会议桌（发言大屏） | 右席位 -->
    <div class="meeting min-h-0 flex-1">
      <!-- 左席位（1,3,5...） -->
      <aside class="side side--left">
        <div v-for="a in leftSeats" :key="a.agentId" class="person">
          <span class="seat-label" :title="a.name">{{ a.name }}</span>
          <span class="seat-avatar">{{ a.emoji || '👤' }}</span>
        </div>
      </aside>

      <!-- 中间会议桌 -->
      <div ref="listRef" class="table">
        <div v-if="transcript.length === 0" class="table-empty">
          <div class="table-empty-icon">🫧</div>
          <p>{{ t('aiEmployee.inviteHint') }}</p>
        </div>
        <template v-for="(msg, index) in transcript" :key="index">
          <div v-if="msg.time" class="time">{{ msg.time }}</div>
          <div class="msg" :class="msg.role === 'chairman' ? 'out' : 'in'">
            <div class="who">{{ msg.speaker }}</div>
            <div class="bubble">
              <span v-if="msg.streaming && !msg.content" class="typing"><i></i><i></i><i></i></span>
              <span v-else>{{ msg.content }}</span>
              <span v-if="msg.streaming && msg.content" class="cursor-blink">▍</span>
            </div>
          </div>
        </template>
      </div>

      <!-- 右席位（2,4,6...） -->
      <aside class="side side--right">
        <div v-for="a in rightSeats" :key="a.agentId" class="person">
          <span class="seat-avatar">{{ a.emoji || '👤' }}</span>
          <span class="seat-label" :title="a.name">{{ a.name }}</span>
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
  import { computed, nextTick, ref, watch } from 'vue';
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiMeetingMinutes } from '@lib/shared/models/ai';

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

  // 席位分列：左 1,3,5… / 右 2,4,6…（发言顺序仍是原始顺序）
  const leftSeats = computed(() => props.attendees.filter((_, i) => i % 2 === 0));
  const rightSeats = computed(() => props.attendees.filter((_, i) => i % 2 === 1));

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
      if (el) {
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
        const entry: Speech = {
          role: 'agent',
          speaker: attendee.name,
          emoji: attendee.emoji,
          content: '',
          streaming: true,
        };
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

        // eslint-disable-next-line no-restricted-syntax, no-await-in-loop -- 原生 async generator，顺序消费流式事件
        for await (const event of stream) {
          if (event.type === 'chunk') {
            entry.content += event.content || '';
            scrollToBottom();
          } else if (event.type === 'error') {
            entry.content = event.errorMessage || t('common.operationFailed');
            throw new Error(event.errorMessage || t('common.operationFailed'));
          }
        }
        entry.streaming = false;
        scrollToBottom();
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
    padding: 14px;
    background: #f0f5f2;
    background-image: radial-gradient(700px 420px at 50% -8%, #e7f5ed 0%, transparent 62%),
      radial-gradient(620px 420px at 0% 100%, #eef4fb 0%, transparent 55%);
  }
  .meeting {
    display: grid;
    grid-template-columns: 112px minmax(0, 1fr) 112px;
    gap: 12px;
    align-items: stretch;
  }
  .side {
    display: flex;
    justify-content: space-around;
    overflow-y: auto;
    padding: 12px 0;
    min-height: 0;
    flex-direction: column;
    gap: 12px;
  }
  .person {
    display: flex;
    align-items: center;
    gap: 8px;
    transition: transform 0.28s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
  .side--left .person {
    flex-direction: row;
    justify-content: flex-end;
  }
  .side--right .person {
    flex-direction: row;
    justify-content: flex-start;
  }
  .side--left .person:hover {
    transform: translateX(3px);
  }
  .side--right .person:hover {
    transform: translateX(-3px);
  }
  .seat-label {
    overflow: hidden;
    padding: 7px 9px;
    max-width: 74px;
    font-size: 12.5px;
    border: 1px solid #e8eeeb;
    border-radius: 8px;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #4b5752;
    background: #fbfdfc;
    box-shadow: 0 1px 2px rgb(30 60 40 / 3%);
    transition: color 0.25s ease, border-color 0.25s ease;
    letter-spacing: 0.4px;
  }
  .person:hover .seat-label {
    border-color: #c9ddf5;
    color: #2563eb;
  }
  .seat-avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 42px;
    height: 42px;
    font-size: 19px;
    border-radius: 50%;
    background: radial-gradient(circle at 34% 26%, #60a5fa, #3b82f6 55%, #2563eb);
    box-shadow: 0 7px 16px -7px rgb(59 130 246 / 90%), inset 0 1.5px 2px rgb(255 255 255 / 50%);
    transition: transform 0.28s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.28s ease;
    flex: 0 0 42px;
  }
  .person:hover .seat-avatar {
    transform: scale(1.08);
    box-shadow: 0 12px 22px -8px rgb(59 130 246 / 100%), inset 0 1.5px 2px rgb(255 255 255 / 60%);
  }
  .table {
    position: relative;
    overflow-x: hidden;
    overflow-y: auto;
    padding: 24px 26px;
    border: 1.5px solid #e8efeb;
    border-radius: 28px;
    background: linear-gradient(180deg, #ffffff 0%, #fbfefc 100%);
    box-shadow: 0 22px 46px -30px rgb(18 74 48 / 28%), 0 6px 16px -12px rgb(18 74 48 / 8%),
      inset 0 1px 0 rgb(255 255 255 / 90%);
    scrollbar-width: thin;
    scrollbar-color: #d6e4dc transparent;
    scroll-behavior: smooth;
  }
  .table::-webkit-scrollbar {
    width: 7px;
  }
  .table::-webkit-scrollbar-track {
    background: transparent;
  }
  .table::-webkit-scrollbar-thumb {
    border-radius: 99px;
    background: #d6e4dc;
  }
  .table::-webkit-scrollbar-thumb:hover {
    background: #b8ccc0;
  }
  .table-empty {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    font-size: 13px;
    color: #9ca8a3;
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
    color: #a0ada7;
    letter-spacing: 0.4px;
    font-variant-numeric: tabular-nums;
  }
  .msg {
    display: flex;
    flex-direction: column;
    animation: msgIn 0.5s cubic-bezier(0.2, 0.8, 0.2, 1) both;
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
    margin-bottom: 7px;
    padding: 0 4px;
    font-size: 12.5px;
    color: #3d4843;
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
    background: linear-gradient(135deg, #4ade80 0%, #22c55e 58%, #16a34a 100%);
    box-shadow: 0 12px 24px -14px rgb(34 197 94 / 90%), inset 0 1px 0 rgb(255 255 255 / 35%);
  }
  .msg.in .bubble {
    border-radius: 14px 14px 14px 4px;
    color: #ffffff;
    background: linear-gradient(160deg, #2f3835, #262d2b);
    box-shadow: 0 12px 24px -16px rgb(0 0 0 / 60%), inset 0 1px 0 rgb(255 255 255 / 8%);
  }
  .cursor-blink {
    animation: blink 1s step-end infinite;
    color: #ffffff;
    opacity: 0.8;
  }
  @keyframes blink {
    50% {
      opacity: 0;
    }
  }
  .typing {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 0;
  }
  .typing i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #ffffff;
    opacity: 0.45;
    animation: typing 1.2s infinite;
  }
  .typing i:nth-child(2) {
    animation-delay: 0.2s;
  }
  .typing i:nth-child(3) {
    animation-delay: 0.4s;
  }
  @keyframes typing {
    0%,
    60%,
    100% {
      opacity: 0.3;
      transform: translateY(0);
    }
    30% {
      opacity: 1;
      transform: translateY(-3px);
    }
  }
  .composer {
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
    color: #2b3531;
    letter-spacing: 1px;
  }
  .composer-title em {
    margin: 0 8px;
    color: #b8c2bd;
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
    border: 1.5px solid #e8efeb;
    border-radius: 16px;
    background: #ffffff;
    box-shadow: 0 12px 26px -20px rgb(18 74 48 / 40%), inset 0 1px 0 #ffffff;
    transition: border-color 0.25s ease, box-shadow 0.25s ease;
  }
  .input-wrap:focus-within {
    border-color: #b3e3c7;
    box-shadow: 0 16px 32px -20px rgb(34 197 94 / 55%), inset 0 1px 0 #ffffff;
  }
  .input-wrap textarea {
    width: 100%;
    min-height: 46px;
    max-height: 96px;
    font-size: 14px;
    font-family: inherit;
    border: none;
    color: #2b3330;
    background: transparent;
    outline: none;
    resize: none;
    line-height: 1.7;
  }
  .input-wrap textarea::placeholder {
    color: #b8c2bd;
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
    color: #ffffff;
    background: linear-gradient(145deg, #4ade80, #22c55e 55%, #16a34a);
    box-shadow: 0 10px 22px -10px rgb(34 197 94 / 100%), inset 0 1px 0 rgb(255 255 255 / 45%);
    transition: transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.22s ease, opacity 0.2s ease;
    place-items: center;
    cursor: pointer;
  }
  .send:hover:not(:disabled) {
    transform: translateY(-3px) scale(1.06);
    box-shadow: 0 16px 28px -12px rgb(34 197 94 / 100%), inset 0 1px 0 rgb(255 255 255 / 55%);
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
