<template>
  <!-- field 变体：带缩略图的字段触发器（工作台工具栏） -->
  <div v-if="variant === 'field'" class="drama-style-picker-field">
    <span v-if="fieldLabel" class="drama-style-picker-field-label">{{ fieldLabel }}</span>
    <button
      type="button"
      class="drama-style-picker-trigger"
      :class="{ 'is-active': active }"
      :disabled="disabled"
      :aria-expanded="open"
      @click="open = true"
    >
      <span v-if="value" class="drama-style-picker-thumb" aria-hidden>
        <DramaImageStylePreviewImg :style-id="value" alt="" />
      </span>
      <span v-else class="drama-style-picker-thumb is-empty" aria-hidden />
      <span class="drama-style-picker-text">{{ triggerLabel }}</span>
      <svg
        viewBox="0 0 24 24"
        width="14"
        height="14"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        :class="{ 'is-open': open }"
      >
        <path d="m6 9 6 6 6-6" />
      </svg>
    </button>
  </div>

  <!-- toolbar 变体：胶囊触发器（新建面板） -->
  <button
    v-else
    type="button"
    class="drama-agent-opt-trigger"
    :class="{ 'is-active': active }"
    :disabled="disabled"
    :aria-expanded="open"
    @click="open = true"
  >
    <svg
      viewBox="0 0 24 24"
      width="15"
      height="15"
      fill="none"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
      <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
    </svg>
    <span class="drama-agent-opt-label">{{ triggerLabel }}</span>
    <svg
      viewBox="0 0 24 24"
      width="13"
      height="13"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      :class="{ 'is-open': open }"
    >
      <path d="m6 9 6 6 6-6" />
    </svg>
  </button>

  <!-- 画面风格弹窗：无风格 + 缩略图卡片网格 -->
  <n-modal v-model:show="open" preset="card" :title="modalTitle" class="drama-style-modal !w-[640px]">
    <p class="drama-style-modal-hint">{{ t('drama.styleModalHint') }}</p>
    <div class="drama-style-modal-grid">
      <button type="button" class="drama-style-modal-none" :class="{ 'is-selected': !value }" @click="select(null)">
        <svg
          v-if="!value"
          class="drama-style-modal-check"
          viewBox="0 0 24 24"
          width="12"
          height="12"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
        {{ t('drama.styleNone') }}
      </button>
      <button
        v-for="opt in IMAGE_STYLE_OPTIONS"
        :key="opt.id"
        type="button"
        class="drama-style-modal-card"
        :class="{ 'is-selected': value === opt.id }"
        @click="select(opt.id)"
      >
        <DramaImageStylePreviewImg :style-id="opt.id" :alt="opt.label" />
        <span>{{ opt.label }}</span>
        <svg
          v-if="value === opt.id"
          class="drama-style-modal-check on-media"
          viewBox="0 0 24 24"
          width="12"
          height="12"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
      </button>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NModal } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import DramaImageStylePreviewImg from './DramaImageStylePreviewImg.vue';

  import { getImageStyleLabel, IMAGE_STYLE_OPTIONS } from './templates';

  const props = withDefaults(
    defineProps<{
      value?: string | null;
      variant?: 'toolbar' | 'field';
      disabled?: boolean;
      fieldLabel?: string;
      title?: string;
      emptyLabel?: string;
    }>(),
    { value: null, variant: 'toolbar', disabled: false, fieldLabel: '', title: '', emptyLabel: '' }
  );

  const emit = defineEmits<{
    (e: 'update:value', v: string | null): void;
  }>();

  const { t } = useI18n();
  const open = ref(false);

  const styleLabel = computed(() => getImageStyleLabel(props.value));
  const triggerLabel = computed(() => {
    if (styleLabel.value) return styleLabel.value;
    if (props.emptyLabel) return props.emptyLabel;
    return props.variant === 'field' ? t('drama.styleChoose') : t('drama.style');
  });
  const modalTitle = computed(() => props.title || t('drama.style'));
  const active = computed(() => Boolean(props.value) || open.value);

  function select(id: string | null) {
    emit('update:value', id);
    open.value = false;
  }
</script>
