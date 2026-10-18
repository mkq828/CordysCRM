<template>
  <div class="competitor-monitor-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard hide-footer content-height="100%" class="min-h-0 flex-1 overflow-hidden">
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiCompetitorMonitor') }}</span>
        </div>
      </template>

      <div class="flex h-full flex-col gap-[16px] overflow-y-auto p-[24px]">
        <div class="text-[13px] text-orange-500">{{ t('competitorMonitor.desc') }}</div>

        <div class="grid grid-cols-1 gap-[16px] md:grid-cols-3">
          <div class="flex items-center gap-[12px]">
            <span class="w-[44px] shrink-0 text-[13px] text-[var(--text-n2)]">{{
              t('competitorMonitor.competitor')
            }}</span>
            <n-input v-model:value="competitor" :placeholder="t('competitorMonitor.competitorPlaceholder')" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[44px] shrink-0 text-[13px] text-[var(--text-n2)]">{{
              t('competitorMonitor.keyword')
            }}</span>
            <n-input v-model:value="keyword" :placeholder="t('competitorMonitor.keywordPlaceholder')" />
          </div>
          <div class="flex items-center">
            <AiActionButton :loading="creating" :disabled="!canAdd" @click="addSubscription">
              {{ creating ? t('competitorMonitor.adding') : t('competitorMonitor.add') }}
            </AiActionButton>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-[16px] lg:grid-cols-2">
          <div class="flex flex-col gap-[12px]">
            <div class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('competitorMonitor.list') }}</div>
            <n-empty v-if="!subscriptions.length" :description="t('competitorMonitor.empty')" class="mt-[12px]" />
            <div
              v-for="sub in subscriptions"
              :key="sub.id"
              class="flex items-center justify-between gap-[12px] rounded-[8px] border p-[12px]"
              :class="selectedId === sub.id ? 'border-[var(--primary-8)]' : 'border-[var(--divider-color)]'"
            >
              <div class="flex min-w-0 flex-1 cursor-pointer flex-col gap-[4px]" @click="select(sub.id)">
                <span class="truncate text-[13px] text-[var(--text-n1)]">{{ subLabel(sub) }}</span>
                <span class="text-[12px] text-[var(--text-n3)]">{{ formatTime(sub.createTime) }}</span>
              </div>
              <div class="flex shrink-0 items-center gap-[8px]">
                <n-switch :value="!!sub.enabled" size="small" @update:value="(v) => toggle(sub.id, v)" />
                <n-button size="tiny" type="primary" :loading="searchingId === sub.id" @click="searchNow(sub.id)">
                  {{ t('competitorMonitor.searchNow') }}
                </n-button>
                <n-popconfirm @positive-click="remove(sub.id)">
                  <template #trigger>
                    <n-button size="tiny" quaternary type="error">{{ t('competitorMonitor.delete') }}</n-button>
                  </template>
                  {{ t('competitorMonitor.delete') }}？
                </n-popconfirm>
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-[12px]">
            <div class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('competitorMonitor.results') }}</div>
            <n-empty v-if="!selectedId" :description="t('competitorMonitor.selectFirst')" class="mt-[12px]" />
            <n-spin v-else-if="loadingResults" class="self-center" />
            <n-empty v-else-if="!results.length" :description="t('competitorMonitor.noResult')" class="mt-[12px]" />
            <div v-else class="flex flex-col gap-[12px]">
              <div
                v-for="(item, index) in results"
                :key="item.id"
                class="flex flex-col gap-[6px] rounded-[8px] border border-[var(--divider-color)] p-[12px]"
              >
                <a
                  :href="item.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="min-w-0 flex-1 text-[13px] leading-[1.6] text-[var(--primary-8)] hover:underline"
                >
                  {{ index + 1 }}. {{ item.title || item.url }}
                </a>
                <span v-if="item.snippet" class="text-[12px] leading-[1.5] text-[var(--text-n3)]">
                  {{ item.snippet }}
                </span>
                <template v-if="item.content">
                  <n-button size="tiny" quaternary color="#f97316" class="w-fit" @click="toggleContent(item.id)">
                    {{ showContent[item.id] ? t('competitorMonitor.hideContent') : t('competitorMonitor.viewContent') }}
                  </n-button>
                  <div
                    v-if="showContent[item.id]"
                    class="whitespace-pre-wrap rounded-[6px] bg-[var(--fill-2)] p-[8px] text-[12px] leading-[1.6] text-[var(--text-n2)]"
                  >
                    {{ item.content }}
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NButton, NEmpty, NInput, NPopconfirm, NSpin, NSwitch } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CompetitorMonitorResult, CompetitorMonitorSubscription } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import {
    createCompetitorMonitor,
    deleteCompetitorMonitor,
    getCompetitorMonitorList,
    getCompetitorMonitorResults,
    searchCompetitorMonitorNow,
    toggleCompetitorMonitor,
  } from '@/api/modules';

  const { t } = useI18n();

  const competitor = ref('');
  const keyword = ref('');
  const creating = ref(false);
  const subscriptions = ref<CompetitorMonitorSubscription[]>([]);
  const selectedId = ref('');
  const results = ref<CompetitorMonitorResult[]>([]);
  const loadingResults = ref(false);
  const searchingId = ref('');
  const showContent = ref<Record<string, boolean>>({});

  const canAdd = computed(() => !creating.value && competitor.value.trim() !== '');

  async function loadSubscriptions() {
    try {
      subscriptions.value = await getCompetitorMonitorList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      subscriptions.value = [];
    }
  }

  async function addSubscription() {
    if (!canAdd.value) {
      return;
    }
    creating.value = true;
    try {
      await createCompetitorMonitor({
        competitor: competitor.value.trim(),
        keyword: keyword.value.trim(),
      });
      competitor.value = '';
      keyword.value = '';
      await loadSubscriptions();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      creating.value = false;
    }
  }

  async function remove(id: string) {
    try {
      await deleteCompetitorMonitor(id);
      if (selectedId.value === id) {
        selectedId.value = '';
        results.value = [];
      }
      await loadSubscriptions();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function toggle(id: string, enabled: boolean) {
    try {
      await toggleCompetitorMonitor(id, enabled);
      const sub = subscriptions.value.find((item) => item.id === id);
      if (sub) {
        sub.enabled = enabled;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function loadResults(id: string) {
    loadingResults.value = true;
    showContent.value = {};
    try {
      results.value = await getCompetitorMonitorResults(id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      results.value = [];
    } finally {
      loadingResults.value = false;
    }
  }

  async function select(id: string) {
    selectedId.value = id;
    await loadResults(id);
  }

  async function searchNow(id: string) {
    if (searchingId.value) {
      return;
    }
    searchingId.value = id;
    try {
      await searchCompetitorMonitorNow(id);
      if (selectedId.value !== id) {
        selectedId.value = id;
      }
      await loadResults(id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      searchingId.value = '';
    }
  }

  function toggleContent(id: string) {
    showContent.value[id] = !showContent.value[id];
  }

  function subLabel(sub: CompetitorMonitorSubscription) {
    const parts = [sub.competitor, sub.keyword].filter(Boolean);
    return parts.length ? parts.join(' · ') : t('competitorMonitor.list');
  }

  function formatTime(time?: number) {
    if (!time) {
      return '';
    }
    return new Date(time).toLocaleString();
  }

  loadSubscriptions();
</script>

<style lang="less" scoped></style>
