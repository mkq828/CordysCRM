<template>
  <div
    class="action-card shrink-0 rounded-[8px] border border-[var(--divider-color)] p-[12px] transition-colors hover:border-[var(--primary-8)]"
  >
    <div class="flex flex-wrap items-center gap-[8px]">
      <n-tag size="small" :bordered="false" type="info">{{ typeLabel }}</n-tag>
      <span class="truncate text-[13px] font-semibold text-[var(--text-n1)]">{{ title }}</span>
      <span class="text-[12px] text-[var(--text-n3)]"
        >{{ t('aiEmployee.actionsOwner') }}：{{ action.ownerName || '-' }}</span
      >
    </div>

    <!-- 跟进建议 -->
    <template v-if="action.actionType === 'FOLLOW_UP_CREATE'">
      <div class="mt-[8px] break-words text-[12px] leading-[1.6] text-[var(--text-n2)]">{{ action.content }}</div>
      <div class="mt-[6px] flex items-center gap-[8px] text-[12px] text-[var(--text-n3)]">
        <span>{{ t('aiEmployee.actionsMethod') }}：{{ action.followMethod || '-' }}</span>
      </div>
      <div v-if="action.suggestion" class="mt-[6px] break-words text-[12px] leading-[1.5] text-orange-500">
        {{ t('aiEmployee.actionsSuggestion') }}：{{ action.suggestion }}
      </div>
    </template>

    <!-- 催款建议 -->
    <template v-else-if="action.actionType === 'DUNNING'">
      <div class="mt-[8px] break-words text-[12px] leading-[1.6] text-[var(--text-n2)]">{{
        payload.dunningContent
      }}</div>
      <div v-if="payload.suggestion" class="mt-[6px] break-words text-[12px] leading-[1.5] text-orange-500">
        {{ t('aiEmployee.actionsSuggestion') }}：{{ payload.suggestion }}
      </div>
    </template>

    <!-- 打标签建议 -->
    <template v-else-if="action.actionType === 'TAG'">
      <div class="mt-[8px] flex flex-wrap items-center gap-[8px]">
        <n-tag size="small" :bordered="false" type="warning">{{ payload.tagName }}</n-tag>
        <span v-if="suggestedTags.length" class="text-[12px] text-[var(--text-n3)]">
          {{ t('aiEmployee.actionSuggestedTags') }}：{{ suggestedTags.join('、') }}
        </span>
      </div>
      <div v-if="payload.reason" class="mt-[6px] break-words text-[12px] leading-[1.5] text-orange-500">
        {{ t('aiEmployee.actionReason') }}：{{ payload.reason }}
      </div>
    </template>

    <!-- 线索流转建议 -->
    <template v-else-if="action.actionType === 'CLUE_FLOW'">
      <div class="mt-[8px] flex flex-wrap items-center gap-[8px]">
        <span class="text-[12px] text-[var(--text-n3)]">{{ t('aiEmployee.actionTargetStage') }}：</span>
        <n-tag size="small" :bordered="false" type="success">{{ stageText }}</n-tag>
      </div>
      <div v-if="payload.reason" class="mt-[6px] break-words text-[12px] leading-[1.5] text-orange-500">
        {{ t('aiEmployee.actionReason') }}：{{ payload.reason }}
      </div>
    </template>

    <!-- 补全数据建议 -->
    <template v-else-if="action.actionType === 'FILL_FIELD'">
      <div class="mt-[8px] flex flex-col gap-[6px]">
        <div
          v-for="(field, index) in fillFields"
          :key="field.fieldId || index"
          class="rounded-[6px] bg-[var(--bg-n2)] p-[8px]"
        >
          <div class="flex items-center gap-[8px]">
            <span class="text-[12px] font-medium text-[var(--text-n1)]">{{ field.fieldName || field.fieldId }}</span>
            <span class="break-words text-[12px] text-[var(--text-n2)]">{{ field.suggestedValue }}</span>
          </div>
          <div v-if="field.reason" class="mt-[4px] break-words text-[12px] leading-[1.4] text-orange-500">
            {{ t('aiEmployee.actionReason') }}：{{ field.reason }}
          </div>
        </div>
      </div>
    </template>

    <div class="mt-[10px] flex justify-end gap-[8px]">
      <n-button size="tiny" type="primary" :loading="processing" @click="emit('approve', action)">
        {{ t('aiEmployee.actionsApprove') }}
      </n-button>
      <n-button size="tiny" :disabled="processing" @click="emit('reject', action)">
        {{ t('aiEmployee.actionsReject') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { NButton, NTag } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiEmployeeAction } from '@lib/shared/models/ai';

  const props = defineProps<{
    action: AiEmployeeAction;
    processing?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'approve', action: AiEmployeeAction): void;
    (e: 'reject', action: AiEmployeeAction): void;
  }>();

  const { t } = useI18n();

  const TYPE_LABEL_KEYS: Record<string, string> = {
    FOLLOW_UP_CREATE: 'aiEmployee.actionFollowUp',
    DUNNING: 'aiEmployee.actionDunning',
    TAG: 'aiEmployee.actionTag',
    CLUE_FLOW: 'aiEmployee.actionClueFlow',
    FILL_FIELD: 'aiEmployee.actionFillField',
  };

  const STAGE_LABEL_KEYS: Record<string, string> = {
    SUCCESS: 'aiEmployee.stageSuccess',
    FAIL: 'aiEmployee.stageFail',
    FOLLOWING: 'aiEmployee.stageFollowing',
  };

  const payload = computed<Record<string, any>>(() => {
    if (!props.action.payload) {
      return {};
    }
    try {
      const parsed = JSON.parse(props.action.payload);
      return parsed && typeof parsed === 'object' ? parsed : {};
    } catch {
      return {};
    }
  });

  const typeLabel = computed(() => {
    const key = TYPE_LABEL_KEYS[props.action.actionType];
    return key ? t(key) : props.action.actionType;
  });

  const title = computed(() => {
    if (props.action.actionType === 'DUNNING' || props.action.actionType === 'CLUE_FLOW') {
      return props.action.resourceName || props.action.customerName || '-';
    }
    return props.action.customerName || props.action.resourceName || '-';
  });

  const stageText = computed(() => {
    const stage = payload.value.targetStage;
    if (!stage) {
      return '-';
    }
    const key = STAGE_LABEL_KEYS[stage];
    return key ? t(key) : stage;
  });

  const suggestedTags = computed<string[]>(() => {
    const tags = payload.value.suggestedTags;
    if (!Array.isArray(tags)) {
      return [];
    }
    return tags.filter((item) => typeof item === 'string');
  });

  const fillFields = computed<Array<Record<string, any>>>(() => {
    const { fields } = payload.value;
    if (!Array.isArray(fields)) {
      return [];
    }
    return fields.filter((item) => item && typeof item === 'object');
  });
</script>
