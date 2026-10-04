<template>
  <div v-if="parsed" class="follow-content">
    <div v-if="parsed.prefix" class="follow-content__plain">{{ parsed.prefix }}</div>
    <FlowBorder :radius="10">
      <div class="follow-content__scripts">
        <div class="follow-content__scripts-label">{{ parsed.label }}</div>
        <div v-for="(script, index) in parsed.scripts" :key="index" class="follow-content__script">
          <span class="follow-content__script-dot"></span>
          <span>{{ script }}</span>
        </div>
      </div>
    </FlowBorder>
    <div v-if="parsed.suffix" class="follow-content__plain">{{ parsed.suffix }}</div>
  </div>
  <div v-else class="follow-content__plain">{{ content }}</div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import FlowBorder from '@/components/pure/effects/FlowBorder.vue';

  const props = defineProps<{
    content?: string;
  }>();

  interface ParsedContent {
    prefix: string;
    label: string;
    scripts: string[];
    suffix: string;
  }

  const SCRIPT_LABEL = '候选话术';

  /** 把跟进内容里「候选话术」段落拆出来单独用流动渐变边框包起来，其余内容原样平铺 */
  const parsed = computed<ParsedContent | null>(() => {
    const content = props.content || '';
    const lines = content.split('\n');
    const idx = lines.findIndex((line) => line.includes(SCRIPT_LABEL));
    if (idx === -1) {
      return null;
    }

    const labelLine = lines[idx];
    const prefix = lines.slice(0, idx).join('\n');
    const label = labelLine.replace(/[:：]\s*$/, '').trim() || SCRIPT_LABEL;

    const scripts: string[] = [];
    const inline = labelLine
      .replace(SCRIPT_LABEL, '')
      .replace(/^[:：]\s*/, '')
      .trim();
    if (inline) {
      scripts.push(inline);
    }
    let j = idx + 1;
    while (j < lines.length && /^\s*[-•·]\s*/.test(lines[j])) {
      scripts.push(lines[j].replace(/^\s*[-•·]\s*/, '').trim());
      j += 1;
    }

    if (!scripts.length) {
      return null;
    }
    return { prefix, label, scripts, suffix: lines.slice(j).join('\n') };
  });
</script>

<style scoped lang="less">
  .follow-content__plain {
    white-space: pre-line;
    word-break: break-word;
  }
  .follow-content__scripts {
    padding: 10px 12px;
  }
  .follow-content__scripts-label {
    margin-bottom: 6px;
    font-weight: 600;
    color: #f97316;
  }
  .follow-content__script {
    display: flex;
    gap: 6px;
    align-items: flex-start;
    color: var(--text-n1);
    line-height: 1.6;
  }
  .follow-content__script + .follow-content__script {
    margin-top: 4px;
  }
  .follow-content__script-dot {
    margin-top: 8px;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #f97316;
    flex-shrink: 0;
  }
</style>
