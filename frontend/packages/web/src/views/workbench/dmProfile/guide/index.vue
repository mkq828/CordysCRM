<template>
  <div class="dm-profile-guide-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard hide-footer no-content-padding content-height="100%" class="min-h-0 flex-1 overflow-hidden">
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('dmProfileGuide.title') }}</span>
          <div class="ml-auto">
            <n-button quaternary size="small" @click="goBack">{{ t('dmProfileGuide.back') }}</n-button>
          </div>
        </div>
      </template>

      <div class="h-full overflow-auto p-[20px]">
        <section class="mb-[24px] rounded-[8px] bg-[#fff7f9] p-[16px]">
          <h2 class="mb-[8px] text-[15px] font-semibold">{{ t('dmProfileGuide.whyTitle') }}</h2>
          <p class="text-[13px] leading-[1.7] text-[var(--text-n4)]">{{ t('dmProfileGuide.whyDesc') }}</p>
        </section>

        <section
          class="mb-[24px] flex items-center gap-[16px] rounded-[8px] border border-[#f0f0f0] bg-[#fff] p-[16px]"
        >
          <div class="min-w-0 flex-1">
            <h2 class="mb-[4px] text-[15px] font-semibold">{{ t('dmProfileGuide.downloadTitle') }}</h2>
            <p class="text-[13px] text-[var(--text-n4)]">{{ t('dmProfileGuide.downloadDesc') }}</p>
          </div>
          <n-button type="primary" size="large" @click="downloadPlugin">{{ t('dmProfileGuide.download') }}</n-button>
        </section>

        <section v-for="(sec, i) in sections" :key="i" class="mb-[24px]">
          <h2 class="mb-[4px] text-[15px] font-semibold">{{ sec.title }}</h2>
          <p v-if="sec.desc" class="mb-[10px] text-[12px] text-[var(--text-n4)]">{{ sec.desc }}</p>
          <ol class="flex flex-col gap-[10px]">
            <li v-for="(s, j) in sec.steps" :key="j" class="flex items-start gap-[10px]">
              <span
                class="flex h-[20px] w-[20px] shrink-0 items-center justify-center rounded-full bg-[var(--primary-8)] text-[12px] font-semibold leading-none text-white"
                >{{ j + 1 }}</span
              >
              <span class="pt-[2px] text-[13px] leading-[1.6]">{{ s }}</span>
            </li>
          </ol>
        </section>

        <section class="rounded-[8px] bg-[#fff7ed] p-[16px]">
          <h2 class="mb-[8px] text-[15px] font-semibold text-orange-500">{{ t('dmProfileGuide.tipTitle') }}</h2>
          <p class="text-[13px] leading-[1.7] text-orange-500">{{ t('dmProfileGuide.tip') }}</p>
        </section>
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { useRouter } from 'vue-router';
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const sections = computed(() => [
    {
      title: t('dmProfileGuide.installTitle'),
      desc: t('dmProfileGuide.installDesc'),
      steps: ['install.step1', 'install.step2', 'install.step3', 'install.step4'].map((k) => t(`dmProfileGuide.${k}`)),
    },
    {
      title: t('dmProfileGuide.useTitle'),
      desc: '',
      steps: ['use.step1', 'use.step2', 'use.step3', 'use.step4'].map((k) => t(`dmProfileGuide.${k}`)),
    },
    {
      title: t('dmProfileGuide.bindTitle'),
      desc: '',
      steps: ['bind.step1', 'bind.step2', 'bind.step3'].map((k) => t(`dmProfileGuide.${k}`)),
    },
  ]);

  function goBack() {
    // 新窗口打开时（window.open）直接关闭；常规导航则跳回账号绑定页
    if (window.opener) {
      window.close();
      return;
    }
    router.push({ name: WorkbenchRouteEnum.AI_DM_PROFILE });
  }

  function downloadPlugin() {
    const a = document.createElement('a');
    a.href = '/downloads/cookie-editor.zip';
    a.download = 'cookie-editor.zip';
    a.click();
  }
</script>

<style lang="less" scoped>
  .dm-profile-guide-page {
    background: #f5f7fa;
  }
</style>
