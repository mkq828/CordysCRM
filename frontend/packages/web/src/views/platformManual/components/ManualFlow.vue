<template>
  <div class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
    <div v-if="title" class="mb-[14px] flex items-center gap-[8px]">
      <span
        class="flex h-[26px] w-[26px] items-center justify-center rounded-[7px]"
        :style="{ background: `${color}1f`, color }"
      >
        <CrmIcon :type="icon" :size="15" />
      </span>
      <span class="text-[15px] font-semibold text-[#1f2329]">{{ title }}</span>
    </div>
    <div ref="flowRef" class="manual-flow"></div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import mermaid from 'mermaid';

  const props = withDefaults(
    defineProps<{
      graph: string;
      color?: string;
      title?: string;
      icon?: string;
    }>(),
    { color: '#4282FF', title: '', icon: 'iconicon_aiming' }
  );

  const flowRef = ref<HTMLElement>();

  let idCounter = 0;

  onMounted(() => {
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      theme: 'base',
      themeVariables: {
        primaryColor: `${props.color}1a`,
        primaryBorderColor: props.color,
        primaryTextColor: '#1f2329',
        lineColor: '#9ca3af',
        fontSize: '13px',
        edgeLabelBackground: '#ffffff',
      },
    });

    if (!flowRef.value) return;
    idCounter += 1;
    const id = `manual-flow-${idCounter}`;
    mermaid
      .render(id, props.graph)
      .then(({ svg }) => {
        if (flowRef.value) flowRef.value.innerHTML = svg;
      })
      .catch((e) => {
        console.error('mermaid render failed', e);
      });
  });
</script>

<style lang="less" scoped>
  .manual-flow {
    display: flex;
    justify-content: center;
    overflow-x: auto;
    width: 100%;
    min-height: 60px;
  }
</style>
