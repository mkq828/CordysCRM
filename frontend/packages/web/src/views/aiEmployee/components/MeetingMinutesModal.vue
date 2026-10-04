<template>
  <n-modal
    :show="visible"
    preset="card"
    :mask-closable="false"
    :title="minutes?.title || t('aiEmployee.minutesTab')"
    class="meeting-minutes-modal !w-[680px]"
    @update:show="(v: boolean) => emit('update:visible', v)"
  >
    <div v-if="minutes" class="minutes-body max-h-[70vh] overflow-y-auto">
      <!-- 元信息 -->
      <div class="minutes-meta mb-[16px] rounded-[8px] bg-[var(--fill-1)] p-[14px] text-[13px] leading-[2]">
        <div v-if="minutes.timeText" class="flex">
          <span class="meta-label">{{ t('aiEmployee.minutesTime') }}</span>
          <span class="text-[var(--text-n2)]">{{ minutes.timeText }}</span>
        </div>
        <div v-if="minutes.place" class="flex">
          <span class="meta-label">{{ t('aiEmployee.minutesPlace') }}</span>
          <span class="text-[var(--text-n2)]">{{ minutes.place }}</span>
        </div>
        <div v-if="minutes.host" class="flex">
          <span class="meta-label">{{ t('aiEmployee.minutesHost') }}</span>
          <span class="text-[var(--text-n2)]">{{ minutes.host }}</span>
        </div>
        <div v-if="minutes.recorder" class="flex">
          <span class="meta-label">{{ t('aiEmployee.minutesRecorder') }}</span>
          <span class="text-[var(--text-n2)]">{{ minutes.recorder }}</span>
        </div>
        <div v-if="minutes.attendees?.length" class="flex">
          <span class="meta-label">{{ t('aiEmployee.minutesAttendees') }}</span>
          <span class="flex flex-wrap gap-[6px] text-[var(--text-n2)]">
            <span v-for="name in minutes.attendees" :key="name" class="attendee-chip">{{ name }}</span>
          </span>
        </div>
      </div>

      <!-- 发言纪要 -->
      <div v-if="minutes.content?.length" class="minutes-section">
        <div class="minutes-section-title">{{ t('aiEmployee.minutesContent') }}</div>
        <ol class="minutes-list">
          <li v-for="(item, index) in minutes.content" :key="index" class="minutes-list-item">{{ item }}</li>
        </ol>
      </div>

      <!-- 后续行动 -->
      <div v-if="minutes.next?.length" class="minutes-section">
        <div class="minutes-section-title">{{ t('aiEmployee.minutesNext') }}</div>
        <ol class="minutes-list">
          <li v-for="(item, index) in minutes.next" :key="index" class="minutes-list-item">{{ item }}</li>
        </ol>
      </div>

      <!-- 任务清单 -->
      <div v-if="minutes.tasks?.length" class="minutes-section">
        <div class="minutes-section-title">{{ t('aiEmployee.minutesTasks') }}</div>
        <div class="task-table">
          <div class="task-row task-row--head">
            <span>{{ t('aiEmployee.taskCol') }}</span>
            <span>{{ t('aiEmployee.deadlineCol') }}</span>
            <span>{{ t('aiEmployee.deliverableCol') }}</span>
          </div>
          <div v-for="(task, index) in minutes.tasks" :key="index" class="task-row">
            <span class="text-[var(--text-n1)]">{{ task.task }}</span>
            <span class="text-[var(--text-n2)]">{{ task.deadline || '—' }}</span>
            <span class="text-[var(--text-n2)]">{{ task.deliverable || '—' }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end">
        <n-button type="primary" @click="emit('update:visible', false)">{{ t('common.confirm') }}</n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
  import { NButton, NModal } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiMeetingMinutes } from '@lib/shared/models/ai';

  defineProps<{
    visible: boolean;
    minutes: AiMeetingMinutes | null;
  }>();

  const emit = defineEmits<{
    (e: 'update:visible', value: boolean): void;
  }>();

  const { t } = useI18n();
</script>

<style lang="less" scoped>
  .meta-label {
    width: 56px;
    flex-shrink: 0;
    color: var(--text-n3);
  }
  .attendee-chip {
    padding: 1px 8px;
    font-size: 12px;
    border-radius: 4px;
    background: var(--fill-2);
  }
  .minutes-section {
    margin-bottom: 18px;
  }
  .minutes-section-title {
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-n1);
    letter-spacing: 1px;
  }
  .minutes-list {
    margin: 0;
    padding-left: 20px;
    color: var(--text-n2);
  }
  .minutes-list-item {
    margin-bottom: 6px;
    line-height: 1.7;
    font-size: 13px;
  }
  .task-table {
    overflow: hidden;
    border: 1px solid var(--divider-color);
    border-radius: 8px;
  }
  .task-row {
    display: grid;
    grid-template-columns: 1.6fr 1fr 1.2fr;
    gap: 8px;
    padding: 8px 12px;
    font-size: 13px;
    border-bottom: 1px solid var(--divider-color);
    &:last-child {
      border-bottom: none;
    }
    &--head {
      font-weight: 500;
      color: var(--text-n3);
      background: var(--fill-1);
    }
  }
</style>
