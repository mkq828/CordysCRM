<template>
  <div class="lead-mining-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard hide-footer content-height="100%" class="min-h-0 flex-1 overflow-hidden">
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiLeadMining') }}</span>
        </div>
      </template>

      <div class="flex h-full flex-col gap-[16px] overflow-y-auto p-[24px]">
        <div class="text-[13px] text-orange-500">{{ t('leadMining.desc') }}</div>

        <div class="grid grid-cols-1 gap-[16px] md:grid-cols-3">
          <div class="flex items-center gap-[12px]">
            <span class="w-[44px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('leadMining.industry') }}</span>
            <n-input v-model:value="industry" :placeholder="t('leadMining.industryPlaceholder')" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[44px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('leadMining.region') }}</span>
            <n-input v-model:value="region" :placeholder="t('leadMining.regionPlaceholder')" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[44px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ t('leadMining.keyword') }}</span>
            <n-input v-model:value="keyword" :placeholder="t('leadMining.keywordPlaceholder')" />
          </div>
        </div>

        <div class="flex items-center gap-[12px]">
          <AiActionButton :loading="searching" :disabled="!canSearch" @click="search">
            {{ searching ? t('leadMining.searching') : t('leadMining.search') }}
          </AiActionButton>
        </div>

        <div class="flex flex-1 flex-col gap-[12px]">
          <n-spin v-if="searching" class="self-center" />

          <n-empty
            v-else-if="!sources.length"
            :description="searched ? t('leadMining.noResult') : t('leadMining.empty')"
            class="mt-[20px]"
          />

          <div v-else class="flex flex-col gap-[16px]">
            <div
              v-for="(source, index) in sources"
              :key="index"
              class="flex flex-col gap-[6px] rounded-[8px] border border-[var(--divider-color)] p-[12px]"
            >
              <div class="flex items-start justify-between gap-[12px]">
                <a
                  :href="source.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="min-w-0 flex-1 text-[13px] leading-[1.6] text-[var(--primary-8)] hover:underline"
                >
                  {{ index + 1 }}. {{ source.title || source.url }}
                </a>
                <div class="flex shrink-0 items-center gap-[8px]">
                  <n-tag v-if="imported[index]" size="small" type="success" :bordered="false">
                    {{ t('leadMining.imported') }}
                  </n-tag>
                  <n-button
                    v-else
                    size="tiny"
                    type="primary"
                    :loading="importingIndex === String(index)"
                    @click="importSource(index)"
                  >
                    {{ t('leadMining.import') }}
                  </n-button>
                </div>
              </div>

              <span v-if="source.snippet" class="text-[12px] leading-[1.5] text-[var(--text-n3)]">
                {{ source.snippet }}
              </span>

              <template v-if="source.content">
                <n-button size="tiny" quaternary color="#f97316" class="w-fit" @click="toggleContent(index)">
                  {{ showContent[index] ? t('leadMining.hideContent') : t('leadMining.viewContent') }}
                </n-button>
                <div
                  v-if="showContent[index]"
                  class="whitespace-pre-wrap rounded-[6px] bg-[var(--fill-2)] p-[8px] text-[12px] leading-[1.6] text-[var(--text-n2)]"
                >
                  {{ source.content }}
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NButton, NEmpty, NInput, NSpin, NTag } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AdvisorWebSource } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import { importLeadMining, searchLeadMining } from '@/api/modules';

  const { t } = useI18n();

  const industry = ref('');
  const region = ref('');
  const keyword = ref('');
  const searching = ref(false);
  const searched = ref(false);
  const sources = ref<AdvisorWebSource[]>([]);
  const showContent = ref<Record<string, boolean>>({});
  const importingIndex = ref('');
  const imported = ref<Record<string, string>>({});

  const canSearch = computed(
    () =>
      !searching.value && (industry.value.trim() !== '' || region.value.trim() !== '' || keyword.value.trim() !== '')
  );

  async function search() {
    if (!canSearch.value) {
      return;
    }
    searching.value = true;
    imported.value = {};
    showContent.value = {};
    try {
      sources.value = await searchLeadMining({
        industry: industry.value.trim(),
        region: region.value.trim(),
        keyword: keyword.value.trim(),
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      sources.value = [];
    } finally {
      searched.value = true;
      searching.value = false;
    }
  }

  async function importSource(index: number) {
    const source = sources.value[index];
    if (!source || importingIndex.value) {
      return;
    }
    importingIndex.value = String(index);
    try {
      await importLeadMining({
        name: source.title,
        url: source.url,
        snippet: source.snippet,
      });
      imported.value[String(index)] = source.title || '';
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      importingIndex.value = '';
    }
  }

  function toggleContent(index: number) {
    showContent.value[String(index)] = !showContent.value[String(index)];
  }
</script>

<style lang="less" scoped></style>
