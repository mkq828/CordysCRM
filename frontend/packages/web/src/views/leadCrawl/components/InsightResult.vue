<template>
  <div v-if="hasContent" class="flex flex-col gap-[12px]">
    <div class="flex items-center justify-between">
      <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('leadCrawl.result') }}</span>
      <n-button v-if="hasStructured" size="tiny" quaternary @click="copyAll">
        {{ t('leadCrawl.copyAll') }}
      </n-button>
    </div>

    <div v-if="insight?.rawContent && !hasStructured" class="insight-raw">
      <span class="insight-raw-label">{{ t('leadCrawl.rawFallback') }}</span>
      <span class="whitespace-pre-wrap">{{ insight.rawContent }}</span>
    </div>

    <template v-if="hasStructured">
      <div v-if="insight?.overview" class="insight-section">
        <div class="insight-section-label">{{ t('leadCrawl.overview') }}</div>
        <div class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
          {{ insight.overview }}
        </div>
      </div>

      <div v-for="section in listSections" :key="section.label" class="insight-section">
        <div class="insight-section-label">{{ section.label }}</div>
        <ul class="flex flex-col gap-[6px]">
          <li v-for="(item, index) in section.items" :key="index" class="flex items-start gap-[8px]">
            <span class="insight-dot"></span>
            <span class="text-[13px] leading-[1.6] text-[var(--text-n2)]">{{ item }}</span>
          </li>
        </ul>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { NButton, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CompetitorInsightResult } from '@lib/shared/models/ai';

  const props = defineProps<{
    insight?: CompetitorInsightResult;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const hasStructured = computed(() =>
    Boolean(
      props.insight?.overview ||
        (props.insight?.dynamics?.length ?? 0) ||
        (props.insight?.trends?.length ?? 0) ||
        (props.insight?.risks?.length ?? 0) ||
        (props.insight?.suggestions?.length ?? 0)
    )
  );

  const hasContent = computed(() => hasStructured.value || Boolean(props.insight?.rawContent));

  const listSections = computed(() => {
    const sections: { label: string; items: string[] }[] = [];
    if (props.insight?.dynamics?.length) {
      sections.push({ label: t('leadCrawl.dynamics'), items: props.insight.dynamics });
    }
    if (props.insight?.trends?.length) {
      sections.push({ label: t('leadCrawl.trends'), items: props.insight.trends });
    }
    if (props.insight?.risks?.length) {
      sections.push({ label: t('leadCrawl.risks'), items: props.insight.risks });
    }
    if (props.insight?.suggestions?.length) {
      sections.push({ label: t('leadCrawl.suggestions'), items: props.insight.suggestions });
    }
    return sections;
  });

  function buildAllText(): string {
    const lines: string[] = [];
    if (props.insight?.overview) {
      lines.push(`【${t('leadCrawl.overview')}】${props.insight.overview}`);
    }
    listSections.value.forEach((section) => {
      lines.push(`【${section.label}】`);
      section.items.forEach((item, index) => lines.push(`${index + 1}. ${item}`));
    });
    if (props.insight?.rawContent && !hasStructured.value) {
      lines.push(props.insight.rawContent);
    }
    return lines.join('\n');
  }

  async function copyAll(): Promise<void> {
    try {
      await navigator.clipboard.writeText(buildAllText());
      Message.success(t('leadCrawl.copied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }
</script>

<style lang="less" scoped>
  .insight-section {
    padding: 10px 12px;
    border: 1px solid rgb(249 115 22 / 16%);
    border-radius: 8px;
    background: rgb(249 115 22 / 5%);
  }
  .insight-section-label {
    margin-bottom: 8px;
    font-size: 12px;
    font-weight: 500;
    color: var(--primary-8);
  }
  .insight-dot {
    margin-top: 8px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: linear-gradient(135deg, #f97316, #fb923c);
    flex-shrink: 0;
  }
  .insight-raw {
    display: flex;
    padding: 8px 10px;
    font-size: 12px;
    border-radius: 6px;
    color: var(--text-n3);
    background: rgb(148 163 184 / 8%);
    gap: 8px;
    line-height: 1.5;
    .insight-raw-label {
      flex-shrink: 0;
      font-weight: 500;
    }
  }
</style>
