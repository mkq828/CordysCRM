<template>
  <div
    v-for="section in sections"
    :key="section.key"
    class="rounded-[12px] bg-white p-[16px] shadow-[0_1px_2px_rgba(16,24,40,0.04)]"
  >
    <div class="mb-[14px] flex items-center gap-[8px]">
      <span
        class="flex h-[26px] w-[26px] items-center justify-center rounded-[7px]"
        :style="{ background: `${accent}1f`, color: accent }"
      >
        <CrmIcon :type="section.icon" :size="15" />
      </span>
      <span class="text-[15px] font-semibold text-[#1f2329]">{{ t(`${prefix}.section.${section.key}.title`) }}</span>
    </div>
    <div class="flex flex-col gap-[10px]">
      <div
        v-for="(item, idx) in section.items"
        :key="item.key"
        class="relative overflow-hidden rounded-[10px] border bg-[#fafbfc] p-[14px]"
        :style="{ borderColor: '#eef0f3' }"
      >
        <span class="absolute left-0 top-0 h-full w-[3px]" :style="{ background: accent }"></span>
        <div class="flex items-start gap-[10px] pl-[6px]">
          <span
            class="mt-[1px] flex h-[20px] w-[20px] shrink-0 items-center justify-center rounded-[6px] text-[12px] font-semibold"
            :style="{ background: `${accent}1f`, color: accent }"
          >
            {{ idx + 1 }}
          </span>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-[8px]">
              <span class="text-[14px] font-medium text-[#1f2329]">{{ t(`${prefix}.item.${item.key}.name`) }}</span>
              <span
                v-if="item.tag"
                class="rounded-[4px] px-[6px] py-[1px] font-mono text-[11px]"
                :style="{ background: `${accent}14`, color: accent }"
              >
                {{ item.tag }}
              </span>
            </div>
            <div class="mt-[4px] text-[13px] leading-[20px] text-[#6b7280]">
              {{ t(`${prefix}.item.${item.key}.desc`) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  interface SectionItem {
    key: string;
    tag?: string;
  }

  interface Section {
    key: string;
    icon: string;
    items: SectionItem[];
  }

  withDefaults(
    defineProps<{
      prefix: string;
      sections: Section[];
      accent?: string;
    }>(),
    { accent: '#4282FF' }
  );

  const { t } = useI18n();
</script>
