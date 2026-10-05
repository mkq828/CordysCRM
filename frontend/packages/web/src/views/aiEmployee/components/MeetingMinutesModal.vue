<template>
  <n-modal
    :show="visible"
    preset="card"
    :mask-closable="false"
    :bordered="false"
    class="meeting-minutes-modal !w-[680px]"
    @update:show="(v: boolean) => emit('update:visible', v)"
  >
    <template #header>
      <div class="card-head">
        <span class="card-title">{{ minutes?.title || t('aiEmployee.minutesTab') }}</span>
        <span class="archive-badge">{{ t('aiEmployee.archived') }}</span>
      </div>
    </template>

    <div v-if="minutes" class="card-body">
      <!-- 元信息 -->
      <div class="meta">
        <div v-if="minutes.timeText" class="meta-row">
          <span class="meta-label">{{ t('aiEmployee.minutesTime') }}</span>
          <span class="meta-value">{{ minutes.timeText }}</span>
        </div>
        <div v-if="minutes.place" class="meta-row">
          <span class="meta-label">{{ t('aiEmployee.minutesPlace') }}</span>
          <span class="meta-value">{{ minutes.place }}</span>
        </div>
        <div v-if="minutes.host" class="meta-row">
          <span class="meta-label">{{ t('aiEmployee.minutesHost') }}</span>
          <span class="meta-value">{{ minutes.host }}</span>
        </div>
        <div v-if="minutes.recorder" class="meta-row">
          <span class="meta-label">{{ t('aiEmployee.minutesRecorder') }}</span>
          <span class="meta-value">{{ minutes.recorder }}</span>
        </div>
        <div v-if="minutes.attendees?.length" class="meta-row meta-row--attendees">
          <span class="meta-label">{{ t('aiEmployee.minutesAttendees') }}</span>
          <span class="attendees">
            <span v-for="name in minutes.attendees" :key="name" class="attendee-chip">{{ name }}</span>
          </span>
        </div>
      </div>

      <!-- 发言纪要 -->
      <div v-if="minutes.content?.length" class="section">
        <div class="sec-title">{{ t('aiEmployee.minutesContent') }}</div>
        <ul class="sec-list">
          <li v-for="(item, index) in minutes.content" :key="index">{{ item }}</li>
        </ul>
      </div>

      <!-- 后续行动 -->
      <div v-if="minutes.next?.length" class="section">
        <div class="sec-title">{{ t('aiEmployee.minutesNext') }}</div>
        <ul class="sec-list">
          <li v-for="(item, index) in minutes.next" :key="index">{{ item }}</li>
        </ul>
      </div>

      <!-- 任务清单 -->
      <div v-if="minutes.tasks?.length" class="section">
        <div class="sec-title">{{ t('aiEmployee.minutesTasks') }}</div>
        <div class="tbl-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ t('aiEmployee.taskCol') }}</th>
                <th>{{ t('aiEmployee.deadlineCol') }}</th>
                <th>{{ t('aiEmployee.deliverableCol') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(task, index) in minutes.tasks" :key="index">
                <td class="td-task">{{ task.task }}</td>
                <td class="td-date">{{ task.deadline || '—' }}</td>
                <td class="td-deliverable">{{ task.deliverable || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="card-footer">
        <button class="confirm-btn" type="button" @click="emit('update:visible', false)">
          {{ t('common.confirm') }}
        </button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
  import { NModal } from 'naive-ui';

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
  .meeting-minutes-modal {
    :deep(.n-card) {
      overflow: hidden;
      max-width: 92vw;
      border-radius: 24px;
      box-shadow: 0 30px 70px -30px rgb(17 63 40 / 45%);
    }
    :deep(.n-card-header) {
      padding: 22px 26px 14px;
    }
    :deep(.n-card__content) {
      padding: 0 26px;
    }
    :deep(.n-card__action) {
      padding: 16px 26px 22px;
    }
  }
  .card-head {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .card-title {
    font-size: 17px;
    font-weight: 700;
    color: #22352c;
    letter-spacing: 1px;
    line-height: 1.4;
  }
  .archive-badge {
    padding: 3px 11px;
    font-size: 12px;
    font-weight: 600;
    border: 1px solid #b9e7cb;
    border-radius: 999px;
    color: #16a34a;
    background: #e8f8ef;
    flex-shrink: 0;
    letter-spacing: 1px;
  }
  .card-body {
    overflow-y: auto;
    padding-right: 4px;
    max-height: 66vh;
    scrollbar-width: thin;
    scrollbar-color: #d6e4dc transparent;
    &::-webkit-scrollbar {
      width: 7px;
    }
    &::-webkit-scrollbar-thumb {
      border-radius: 99px;
      background: #d6e4dc;
    }
  }
  .meta {
    margin-bottom: 20px;
    padding: 16px 18px;
    border: 1px solid #e0f0e7;
    border-radius: 16px;
    background: linear-gradient(135deg, #f2faf5 0%, #e9f6ee 100%);
  }
  .meta-row {
    display: flex;
    gap: 12px;
    font-size: 13px;
    line-height: 1.8;
  }
  .meta-row--attendees {
    align-items: flex-start;
  }
  .meta-label {
    flex-shrink: 0;
    width: 56px;
    color: #8aa197;
    letter-spacing: 0.5px;
  }
  .meta-value {
    color: #2f3d37;
    word-break: break-all;
  }
  .attendees {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .attendee-chip {
    padding: 2px 11px;
    font-size: 12px;
    border: 1px solid #cfdff5;
    border-radius: 999px;
    color: #2563eb;
    background: #eaf2fd;
  }
  .section {
    margin-bottom: 20px;
  }
  .sec-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    font-size: 15px;
    font-weight: 700;
    color: #22352c;
    letter-spacing: 1px;
  }
  .sec-title::before {
    content: '';
    width: 4px;
    height: 16px;
    border-radius: 3px;
    background: linear-gradient(180deg, #4ade80, #16a34a);
  }
  .sec-list {
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .sec-list li {
    position: relative;
    margin-bottom: 9px;
    padding-left: 18px;
    font-size: 13px;
    color: #3a4a43;
    line-height: 1.75;
    letter-spacing: 0.2px;
  }
  .sec-list li::before {
    position: absolute;
    top: 10px;
    left: 2px;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #22c55e;
    box-shadow: 0 0 0 3px #e8f8ef;
    content: '';
  }
  .tbl-wrap {
    overflow: hidden;
    border: 1px solid #e4efe9;
    border-radius: 14px;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
  }
  thead th {
    padding: 11px 16px;
    font-weight: 600;
    text-align: left;
    color: #ffffff;
    background: linear-gradient(135deg, #4ade80 0%, #22c55e 55%, #16a34a 100%);
    letter-spacing: 1px;
  }
  thead th:first-child {
    width: 42%;
  }
  thead th:nth-child(2) {
    width: 26%;
  }
  thead th:nth-child(3) {
    width: 32%;
  }
  tbody tr {
    border-top: 1px solid #eef4f0;
  }
  tbody tr:nth-child(even) {
    background: #fafdfb;
  }
  tbody td {
    padding: 11px 16px;
    vertical-align: top;
    line-height: 1.65;
    color: #3a4a43;
  }
  .td-task {
    color: #2f3d37;
  }
  .td-date {
    color: #16a34a;
    font-variant-numeric: tabular-nums;
  }
  .td-deliverable {
    color: #2563eb;
  }
  .card-footer {
    display: flex;
    justify-content: flex-end;
  }
  .confirm-btn {
    padding: 0 26px;
    height: 40px;
    font-size: 14px;
    font-family: inherit;
    border: none;
    border-radius: 12px;
    color: #ffffff;
    background: linear-gradient(145deg, #4ade80, #22c55e 55%, #16a34a);
    box-shadow: 0 10px 22px -10px rgb(34 197 94 / 90%), inset 0 1px 0 rgb(255 255 255 / 40%);
    transition: transform 0.2s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.2s ease;
    letter-spacing: 2px;
    cursor: pointer;
  }
  .confirm-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 28px -10px rgb(34 197 94 / 100%), inset 0 1px 0 rgb(255 255 255 / 50%);
  }
  .confirm-btn:active {
    transform: translateY(0) scale(0.97);
  }
</style>
