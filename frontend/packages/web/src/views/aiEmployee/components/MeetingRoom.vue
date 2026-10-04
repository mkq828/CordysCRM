<template>
  <div class="meeting-room flex h-full flex-col">
    <!-- 顶栏：董事长 + 结束会议 -->
    <div class="flex items-center gap-[10px] border-b border-[var(--divider-color)] px-[20px] py-[10px]">
      <div class="flex items-center gap-[8px]">
        <span class="text-[16px] font-semibold text-[var(--text-n1)]">{{ t('aiEmployee.meetingTitle') }}</span>
      </div>
      <div class="flex min-w-0 flex-1 items-center justify-center gap-[8px]">
        <div class="chairman-avatar">{{ chairmanInitial }}</div>
        <div class="min-w-0">
          <div class="truncate text-[13px] font-medium text-[var(--text-n1)]">{{ chairmanName }}</div>
          <div class="text-[11px] text-[var(--text-n3)]">{{ t('aiEmployee.chairman') }}</div>
        </div>
      </div>
      <n-button
        size="small"
        type="error"
        ghost
        :disabled="sending || generating || transcript.length === 0"
        @click="endMeeting"
      >
        {{ t('aiEmployee.endMeeting') }}
      </n-button>
    </div>

    <!-- 会议桌：参会岗位两列排布（左 1/3/5、右 2/4/6） -->
    <div class="meeting-table border-b border-[var(--divider-color)] px-[20px] py-[12px]">
      <template v-if="attendees.length > 0">
        <div class="grid grid-cols-2 gap-[10px]">
          <div
            v-for="a in attendees"
            :key="a.agentId"
            class="attendee-card flex items-center gap-[10px] rounded-[8px] border border-[var(--divider-color)] px-[12px] py-[8px]"
          >
            <div class="attendee-avatar">{{ a.emoji || '👤' }}</div>
            <div class="min-w-0 flex-1 truncate text-[13px] font-medium text-[var(--text-n1)]">{{ a.name }}</div>
          </div>
        </div>
      </template>
      <div v-else class="py-[8px] text-center text-[13px] text-[var(--text-n3)]">{{ t('aiEmployee.inviteHint') }}</div>
    </div>

    <!-- 发言流 -->
    <div ref="listRef" class="transcript-list min-h-0 flex-1 overflow-y-auto px-[20px] py-[16px]">
      <div v-if="transcript.length === 0" class="mt-[40px] text-center text-[13px] text-[var(--text-n3)]">
        {{ t('aiEmployee.meetingEmpty') }}
      </div>
      <div
        v-for="(msg, index) in transcript"
        :key="index"
        class="mb-[12px] flex"
        :class="msg.role === 'chairman' ? 'justify-end' : 'justify-start'"
      >
        <div class="max-w-[82%]">
          <div v-if="msg.role === 'agent'" class="mb-[4px] flex items-center gap-[6px]">
            <span class="attendee-avatar attendee-avatar--sm">{{ msg.emoji || '👤' }}</span>
            <span class="text-[12px] font-medium text-[var(--text-n2)]">{{ msg.speaker }}</span>
          </div>
          <div
            class="whitespace-pre-wrap rounded-[10px] px-[14px] py-[10px] text-[13px] leading-[1.6]"
            :class="msg.role === 'chairman' ? 'bubble--chairman' : 'bubble--agent'"
          >
            <ShimmerText v-if="msg.streaming && !msg.content" size="13px">{{ t('aiEmployee.sending') }}</ShimmerText>
            <span v-else>{{ msg.content }}</span>
            <span v-if="msg.streaming && msg.content" class="cursor-blink">▍</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="flex items-end gap-[12px] border-t border-[var(--divider-color)] px-[20px] py-[12px]">
      <n-input
        v-model:value="question"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        :placeholder="t('aiEmployee.meetingPlaceholder')"
        :disabled="sending || generating"
        @keydown.enter.exact.prevent="send"
      />
      <AiActionButton :loading="sending" :disabled="!canSend" @click="send">{{ t('aiEmployee.send') }}</AiActionButton>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, ref, watch } from 'vue';
  import { NButton, NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiMeetingMinutes } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

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
  const chairmanInitial = computed(() => chairmanName.value.charAt(0));

  const question = ref('');
  const transcript = ref<Speech[]>([]);
  const sending = ref(false);
  const generating = ref(false);
  const meetingStartTime = ref(0);
  const listRef = ref<HTMLElement>();

  const canSend = computed(
    () => !sending.value && !generating.value && question.value.trim() !== '' && props.attendees.length > 0
  );

  const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];

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
    const pad = (n: number) => String(n).padStart(2, '0');
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
    transcript.value.push({ role: 'chairman', speaker: chairmanName.value, content: text });
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
    background: #fbfcfd;
  }
  .chairman-avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 34px;
    height: 34px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 50%;
    color: #ffffff;
    background: var(--primary-8);
    flex-shrink: 0;
  }
  .attendee-avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 36px;
    height: 36px;
    font-size: 18px;
    border-radius: 50%;
    background: var(--fill-2);
    flex-shrink: 0;
    &--sm {
      width: 22px;
      height: 22px;
      font-size: 12px;
    }
  }
  .attendee-card {
    background: var(--fill-1);
  }
  .bubble--chairman {
    color: #ffffff;
    background: var(--primary-8);
  }
  .bubble--agent {
    color: var(--text-n1);
    background: var(--fill-1);
  }
  .cursor-blink {
    animation: blink 1s step-end infinite;
    color: var(--text-n3);
  }
  @keyframes blink {
    50% {
      opacity: 0;
    }
  }
</style>
