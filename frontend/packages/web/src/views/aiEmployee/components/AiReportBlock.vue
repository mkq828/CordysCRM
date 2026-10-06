<script setup lang="ts">
  import { computed, onMounted } from 'vue';

  import AI_REPORT_CSS from './reportTheme';
  import DOMPurify from 'dompurify';

  /**
   * AI 员工「客户全景分析」HTML 报告渲染块（仅在报告完成后挂载）。
   * 流式期间由 AgentTaskChat 用纯文本预览逐字展示（reportPreviewText），避免半成品 HTML 反复重建 DOM 闪烁；
   * 这里等内容完整后经 DOMPurify 清洗，v-html 渲染成卡片式报告，主题 CSS 一次性注入 head（.ai-report 作用域）。
   */
  const props = defineProps<{ html: string }>();

  const STYLE_ID = 'ai-report-theme';

  function ensureStyle() {
    if (document.getElementById(STYLE_ID)) {
      return;
    }
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = AI_REPORT_CSS;
    document.head.appendChild(style);
  }

  onMounted(ensureStyle);

  const sanitized = computed(() => DOMPurify.sanitize(props.html));
</script>

<template>
  <div class="ai-report" v-html="sanitized" />
</template>
