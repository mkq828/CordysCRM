<template>
  <CrmCard hide-footer class="script-retrieve h-full overflow-y-auto">
    <template #title>
      <div class="flex items-center gap-[8px]">
        <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
        <span class="text-[14px] font-semibold">{{ t('script.tabRetrieve') }}</span>
      </div>
    </template>

    <div class="flex flex-col gap-[16px] px-[24px] pb-[24px]">
      <div class="text-[13px] text-orange-500">{{ t('script.retrieveDesc') }}</div>

      <div class="flex items-center gap-[12px]">
        <span class="shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('script.category') }}</span>
        <n-select
          v-model:value="category"
          :options="categoryOptions"
          clearable
          :placeholder="t('script.allCategory')"
          class="!w-[220px]"
        />
      </div>

      <n-input
        v-model:value="scenario"
        type="textarea"
        :placeholder="t('script.scenarioPlaceholder')"
        :autosize="{ minRows: 5, maxRows: 12 }"
      />

      <div>
        <AiActionButton :loading="retrieving" :disabled="!canRetrieve" @click="handleRetrieve">
          {{ retrieving ? t('script.retrieving') : t('script.retrieve') }}
        </AiActionButton>
      </div>

      <div v-if="recommends.length" class="flex flex-col gap-[12px]">
        <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('script.recommendResult') }}</span>
        <div v-for="(item, index) in recommends" :key="index" class="script-recommend-card">
          <div class="flex items-start gap-[10px]">
            <span
              class="mt-[2px] flex h-[20px] w-[20px] shrink-0 items-center justify-center rounded-[6px] text-[12px] font-bold text-white"
              style="background: linear-gradient(135deg, #6366f1, #8b5cf6)"
            >
              {{ index + 1 }}
            </span>
            <div class="flex min-w-0 flex-1 flex-col gap-[10px]">
              <div class="flex items-center gap-[8px]">
                <span class="truncate text-[14px] font-semibold text-[var(--text-n1)]">{{ item.title || '-' }}</span>
                <n-tag v-if="item.source" size="small" round :bordered="false">{{ item.source }}</n-tag>
              </div>
              <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
                {{ item.content }}
              </div>
              <div v-if="item.originalContent" class="script-original">
                <span class="script-original-label">{{ t('script.original') }}</span>
                <span class="whitespace-pre-wrap">{{ item.originalContent }}</span>
              </div>
              <div class="flex justify-end">
                <n-button size="tiny" quaternary @click="copy(item.content)">{{ t('script.copy') }}</n-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <n-empty v-else-if="hasSearched && !retrieving" :description="t('script.empty')" />
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NEmpty, NInput, NSelect, NTag, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ScriptRecommend } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import { getAiSalesScriptCategories, retrieveAiSalesScript } from '@/api/modules';

  import { buildScriptCategoryOptions } from '../constants';

  const { t } = useI18n();
  const Message = useMessage();

  const scenario = ref('');
  const category = ref<string | null>(null);
  const retrieving = ref(false);
  const hasSearched = ref(false);
  const recommends = ref<ScriptRecommend[]>([]);

  const categoryOptions = ref<Array<{ label: string; value: string }>>([]);
  async function loadCategories() {
    try {
      const res = await getAiSalesScriptCategories();
      categoryOptions.value = buildScriptCategoryOptions(res || []);
    } catch (error) {
      categoryOptions.value = buildScriptCategoryOptions();
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const canRetrieve = computed(() => !retrieving.value && scenario.value.trim() !== '');

  async function handleRetrieve() {
    if (!canRetrieve.value) {
      return;
    }
    retrieving.value = true;
    hasSearched.value = true;
    try {
      recommends.value = await retrieveAiSalesScript({
        scenario: scenario.value.trim(),
        category: category.value || undefined,
      });
    } catch (error) {
      recommends.value = [];
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      retrieving.value = false;
    }
  }

  async function copy(text?: string) {
    if (!text) {
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('script.copied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }

  onMounted(() => {
    loadCategories();
  });
</script>

<style lang="less" scoped>
  .script-recommend-card {
    padding: 14px 16px;
    border: 1px solid rgb(99 102 241 / 16%);
    border-radius: 8px;
    background: rgb(99 102 241 / 5%);
  }
  .script-original {
    display: flex;
    padding: 8px 10px;
    font-size: 12px;
    border-radius: 6px;
    color: var(--text-n3);
    background: rgb(148 163 184 / 8%);
    gap: 8px;
    line-height: 1.5;
    .script-original-label {
      flex-shrink: 0;
      font-weight: 500;
    }
  }
</style>
