<template>
  <div class="flex flex-col gap-[16px]">
    <ManualHeader :title="title" :subtitle="intro" :icon="icon" :color="color" />

    <!-- 截图 -->
    <div v-if="screenshot" class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div v-if="screenshotCaption" class="mb-[12px] flex items-center gap-[8px]">
        <span
          class="flex h-[26px] w-[26px] items-center justify-center rounded-[7px]"
          :style="{ background: `${color}1f`, color }"
        >
          <CrmIcon type="iconicon_image" :size="15" />
        </span>
        <span class="text-[15px] font-semibold text-[#1f2329]">{{ screenshotCaption }}</span>
      </div>
      <div class="overflow-hidden rounded-[8px] border border-[#eef0f3]">
        <img :src="screenshot" :alt="title" class="block w-full" />
      </div>
    </div>

    <!-- 它是什么 -->
    <section class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <SectionTitle :icon="'iconicon_aiming'" :color="color" :text="t('platformManual.shell.what')" />
      <p class="text-[14px] leading-[22px] text-[#4b5563]">{{ what }}</p>
    </section>

    <!-- 在哪点 -->
    <section class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <SectionTitle :icon="'iconicon_root_list'" :color="color" :text="t('platformManual.shell.where')" />
      <div class="flex items-center gap-[8px] text-[14px] leading-[22px] text-[#4b5563]">
        <span class="text-[#9ca3af]">「{{ title }}」</span>
        <span class="text-[#cbd5e1]">·</span>
        <span>{{ where }}</span>
      </div>
    </section>

    <!-- 一步步操作 -->
    <section class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <SectionTitle :icon="'iconicon_file'" :color="color" :text="t('platformManual.shell.steps')" />
      <ol class="flex flex-col gap-[10px]">
        <li
          v-for="(step, idx) in steps"
          :key="idx"
          class="relative overflow-hidden rounded-[10px] border bg-[#fafbfc] p-[14px]"
          style="border-color: #eef0f3"
        >
          <span class="absolute left-0 top-0 h-full w-[3px]" :style="{ background: color }"></span>
          <div class="flex items-start gap-[10px] pl-[6px]">
            <span
              class="mt-[1px] flex h-[20px] w-[20px] shrink-0 items-center justify-center rounded-[6px] text-[12px] font-semibold"
              :style="{ background: `${color}1f`, color }"
            >
              {{ idx + 1 }}
            </span>
            <div class="text-[13px] leading-[20px] text-[#4b5563]">{{ step }}</div>
          </div>
        </li>
      </ol>
    </section>

    <!-- 举个例子 -->
    <section class="relative overflow-hidden rounded-[12px] border bg-[#fffbeb] p-[16px]" style="border-color: #fde68a">
      <span class="absolute left-0 top-0 h-full w-[3px]" style="background: #f59e0b"></span>
      <SectionTitle :icon="'iconicon_star'" :color="'#f59e0b'" :text="t('platformManual.shell.example')" />
      <p class="pl-[34px] text-[13px] leading-[20px] text-[#6b7280]">{{ example }}</p>
    </section>

    <!-- 提示 -->
    <section
      v-if="tip"
      class="relative overflow-hidden rounded-[12px] border bg-[#eff6ff] p-[16px]"
      style="border-color: #bfdbfe"
    >
      <span class="absolute left-0 top-0 h-full w-[3px]" style="background: #3b82f6"></span>
      <SectionTitle :icon="'iconicon_tips'" :color="'#3b82f6'" :text="t('platformManual.shell.tip')" />
      <p class="pl-[34px] text-[13px] leading-[20px] text-[#4b5563]">{{ tip }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ManualHeader from '@/views/platformManual/components/ManualHeader.vue';
  import SectionTitle from '@/views/platformManual/components/SectionTitle.vue';

  const { t } = useI18n();

  withDefaults(
    defineProps<{
      title: string;
      intro: string;
      icon: string;
      color: string;
      what: string;
      where: string;
      steps: string[];
      example: string;
      screenshot?: string;
      screenshotCaption?: string;
      tip?: string;
    }>(),
    { screenshot: '', screenshotCaption: '', tip: '' }
  );
</script>
