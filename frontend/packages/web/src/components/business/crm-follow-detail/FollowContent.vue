<template>
  <div v-if="sections" class="follow-content">
    <template v-for="(section, index) in sections" :key="index">
      <!-- 候选话术保留流动渐变边框 -->
      <FlowBorder v-if="section.label === '候选话术'" :radius="10">
        <div class="follow-content__scripts">
          <div class="follow-content__scripts-label">{{ section.label }}</div>
          <div v-for="(script, scriptIndex) in section.items" :key="scriptIndex" class="follow-content__script">
            <span class="follow-content__script-dot"></span>
            <span>{{ script }}</span>
          </div>
        </div>
      </FlowBorder>
      <!-- 其余结构化字段：加粗彩色标签 + 圆点列表 -->
      <div v-else class="follow-content__section">
        <div class="follow-content__row">
          <span class="follow-content__label" :style="{ color: section.color }">{{ section.label }}</span>
          <span v-if="section.items.length === 1" class="follow-content__value">{{ section.items[0] }}</span>
        </div>
        <div v-if="section.items.length > 1" class="follow-content__items">
          <div v-for="(item, itemIndex) in section.items" :key="itemIndex" class="follow-content__item">
            <span class="follow-content__item-dot" :style="{ background: section.color }"></span>
            <span>{{ item }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
  <div v-else class="follow-content__plain">{{ content }}</div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import FlowBorder from '@/components/pure/effects/FlowBorder.vue';

  const props = defineProps<{
    content?: string;
  }>();

  interface Section {
    label: string;
    color: string;
    items: string[];
  }

  // 会话军师/通话复盘结构化分析结果里的标签，颜色沿用智慧工作台分析卡片的语义色
  const LABELS: Record<string, { color: string; multi?: boolean }> = {
    意向评分: { color: '#f97316' },
    成交信号: { color: '#10b981', multi: true },
    异议点: { color: '#ef4444', multi: true },
    情绪满意度: { color: '#3b82f6' },
    竞品提及: { color: '#8b5cf6', multi: true },
    流失风险: { color: '#f59e0b' },
    候选话术: { color: '#6366f1' },
  };

  /** 把「标签：内容」结构化跟进内容解析为分节；普通自由文本则返回 null 走纯文本兜底 */
  const sections = computed<Section[] | null>(() => {
    const content = (props.content || '').trim();
    if (!content) {
      return null;
    }

    const lines = content
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);
    // 首行不是已知结构化标签，视为普通自由文本
    const firstMatch = lines[0]?.match(/^([^:：]+)[:：]\s*(.*)$/);
    if (!firstMatch || !LABELS[firstMatch[1].trim()]) {
      return null;
    }

    const result: Section[] = [];
    let current: Section | null = null;

    lines.forEach((line) => {
      // 上一节（如候选话术）下的 - 子项
      if (current && /^[-•·]\s*/.test(line)) {
        current.items.push(line.replace(/^[-•·]\s*/, '').trim());
        return;
      }

      const match = line.match(/^([^:：]+)[:：]\s*(.*)$/);
      const conf = match ? LABELS[match[1].trim()] : undefined;
      if (match && conf) {
        current = { label: match[1].trim(), color: conf.color, items: [] };
        const value = match[2].trim();
        if (value) {
          current.items = conf.multi
            ? value
                .split('；')
                .map((s) => s.trim())
                .filter(Boolean)
            : [value];
        }
        result.push(current);
        return;
      }

      // 已进入结构化渲染的额外行并入当前节
      if (current) {
        current.items.push(line);
      }
    });

    const filtered = result.filter((section) => section.items.length > 0);
    return filtered.length ? filtered : null;
  });
</script>

<style scoped lang="less">
  .follow-content {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .follow-content__section {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .follow-content__row {
    display: flex;
    gap: 8px;
    align-items: flex-start;
    line-height: 1.6;
  }
  .follow-content__label {
    font-size: 13px;
    font-weight: 600;
    flex-shrink: 0;
  }
  .follow-content__value {
    color: var(--text-n1);
    word-break: break-word;
  }
  .follow-content__items {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .follow-content__item {
    display: flex;
    gap: 6px;
    align-items: flex-start;
    color: var(--text-n1);
    line-height: 1.6;
  }
  .follow-content__item-dot {
    margin-top: 8px;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    flex-shrink: 0;
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
  .follow-content__plain {
    white-space: pre-line;
    word-break: break-word;
  }
</style>
