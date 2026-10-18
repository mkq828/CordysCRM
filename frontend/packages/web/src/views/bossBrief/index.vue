<template>
  <div class="boss-brief-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard hide-footer content-height="100%" class="min-h-0 flex-1 overflow-hidden">
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiBossBrief') }}</span>
        </div>
      </template>

      <n-empty v-if="available === false" class="boss-brief-locked" :description="t('bossBrief.locked')" />

      <div v-else class="flex h-full flex-col gap-[16px] overflow-y-auto p-[24px]">
        <div class="text-[13px] text-orange-500">{{ t('bossBrief.desc') }}</div>

        <div class="grid grid-cols-1 gap-[16px] md:grid-cols-3">
          <div class="flex items-center gap-[12px]">
            <span class="w-[56px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('bossBrief.topic') }}</span>
            <n-input v-model:value="topic" :placeholder="t('bossBrief.topicPlaceholder')" />
          </div>
          <div class="flex items-center">
            <AiActionButton :loading="creating" :disabled="!canAdd" @click="addTopic">
              {{ creating ? t('bossBrief.adding') : t('bossBrief.add') }}
            </AiActionButton>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-[16px] lg:grid-cols-2">
          <div class="flex flex-col gap-[12px]">
            <div class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('bossBrief.list') }}</div>
            <n-empty v-if="!topics.length" :description="t('bossBrief.empty')" class="mt-[12px]" />
            <div
              v-for="sub in topics"
              :key="sub.id"
              class="flex items-center justify-between gap-[12px] rounded-[8px] border p-[12px]"
              :class="selectedId === sub.id ? 'border-[var(--primary-8)]' : 'border-[var(--divider-color)]'"
            >
              <div class="flex min-w-0 flex-1 cursor-pointer flex-col gap-[4px]" @click="select(sub.id)">
                <span class="truncate text-[13px] text-[var(--text-n1)]">{{ sub.topic }}</span>
                <span class="text-[12px] text-[var(--text-n3)]">{{ formatTime(sub.createTime) }}</span>
              </div>
              <div class="flex shrink-0 items-center gap-[8px]">
                <n-switch :value="!!sub.enabled" size="small" @update:value="(v) => toggle(sub.id, v)" />
                <n-button size="tiny" type="primary" :loading="searchingId === sub.id" @click="searchNow(sub.id)">
                  {{ t('bossBrief.searchNow') }}
                </n-button>
                <n-popconfirm @positive-click="remove(sub.id)">
                  <template #trigger>
                    <n-button size="tiny" quaternary type="error">{{ t('bossBrief.delete') }}</n-button>
                  </template>
                  {{ t('bossBrief.delete') }}？
                </n-popconfirm>
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-[12px]">
            <div class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('bossBrief.results') }}</div>
            <n-empty v-if="!selectedId" :description="t('bossBrief.selectFirst')" class="mt-[12px]" />
            <n-spin v-else-if="loadingResults" class="self-center" />
            <n-empty v-else-if="!results.length" :description="t('bossBrief.noResult')" class="mt-[12px]" />
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
                    {{ showContent[item.id] ? t('bossBrief.hideContent') : t('bossBrief.viewContent') }}
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
  import { computed, onMounted, ref } from 'vue';
  import { NButton, NEmpty, NInput, NPopconfirm, NSpin, NSwitch } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { BossBriefResult, BossBriefTopic } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import {
    createBossBrief,
    deleteBossBrief,
    getBossBriefAvailable,
    getBossBriefList,
    getBossBriefResults,
    searchBossBriefNow,
    toggleBossBrief,
  } from '@/api/modules';

  const { t } = useI18n();

  const available = ref<boolean | null>(null);
  const topic = ref('');
  const creating = ref(false);
  const topics = ref<BossBriefTopic[]>([]);
  const selectedId = ref('');
  const results = ref<BossBriefResult[]>([]);
  const loadingResults = ref(false);
  const searchingId = ref('');
  const showContent = ref<Record<string, boolean>>({});

  const canAdd = computed(() => !creating.value && topic.value.trim() !== '');

  async function loadTopics() {
    try {
      topics.value = await getBossBriefList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      topics.value = [];
    }
  }

  async function addTopic() {
    if (!canAdd.value) {
      return;
    }
    creating.value = true;
    try {
      await createBossBrief({ topic: topic.value.trim() });
      topic.value = '';
      await loadTopics();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      creating.value = false;
    }
  }

  async function remove(id: string) {
    try {
      await deleteBossBrief(id);
      if (selectedId.value === id) {
        selectedId.value = '';
        results.value = [];
      }
      await loadTopics();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function toggle(id: string, enabled: boolean) {
    try {
      await toggleBossBrief(id, enabled);
      const sub = topics.value.find((item) => item.id === id);
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
      results.value = await getBossBriefResults(id);
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
      await searchBossBriefNow(id);
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

  function formatTime(time?: number) {
    if (!time) {
      return '';
    }
    return new Date(time).toLocaleString();
  }

  onMounted(async () => {
    try {
      available.value = await getBossBriefAvailable();
    } catch {
      available.value = false; // 拉取失败交给后端门控，前端按未开通展示升级提示
    }
    if (available.value !== false) {
      await loadTopics();
    }
  });
</script>

<style lang="less" scoped>
  .boss-brief-locked {
    padding-top: 60px;
  }
</style>
