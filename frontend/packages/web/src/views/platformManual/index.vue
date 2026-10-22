<template>
  <div class="flex h-full bg-[#f5f7fa]">
    <!-- 左侧章节导航（docs 风格） -->
    <aside class="flex w-[248px] shrink-0 flex-col border-r border-[#eef0f3] bg-white">
      <div class="flex items-center gap-[10px] px-[20px] pb-[14px] pt-[20px]">
        <span
          class="flex h-[32px] w-[32px] shrink-0 items-center justify-center rounded-[9px]"
          style="color: #4282ff; background: #4282ff1f"
        >
          <CrmIcon type="iconicon_books" :size="17" />
        </span>
        <div class="min-w-0">
          <div class="truncate text-[15px] font-semibold leading-[22px] text-[#1f2329]">
            {{ t('menu.platformManual') }}
          </div>
          <div class="truncate text-[11px] leading-[16px] text-[#9ca3af]">{{ t('platformManual.shell.subtitle') }}</div>
        </div>
      </div>

      <nav class="flex flex-1 flex-col gap-[2px] overflow-y-auto px-[10px] pb-[16px]">
        <template v-for="row in navRows" :key="row.key">
          <!-- 分组小标题（规则说明 / 使用说明） -->
          <div
            v-if="row.type === 'section'"
            class="px-[12px] pb-[6px] pt-[16px] text-[11px] font-semibold text-[#9ca3af]"
          >
            {{ row.label }}
          </div>

          <!-- 使用说明下的模块名 -->
          <div
            v-else-if="row.type === 'group'"
            class="px-[12px] pb-[4px] pt-[10px] text-[11px] font-medium text-[#c0c6d0]"
          >
            {{ row.label }}
          </div>

          <!-- 可点击条目 -->
          <button
            v-else
            type="button"
            class="group flex w-full items-center gap-[10px] rounded-[9px] px-[12px] py-[9px] text-left transition-colors"
            :class="row.indent ? 'pl-[20px]' : ''"
            :style="activeKey === row.key ? { background: `${row.color}14` } : undefined"
            @click="activeKey = row.key"
          >
            <span
              class="flex h-[24px] w-[24px] shrink-0 items-center justify-center rounded-[7px] transition-colors"
              :style="
                activeKey === row.key
                  ? { background: `${row.color}1f`, color: row.color }
                  : { background: '#f5f7fa', color: '#9ca3af' }
              "
            >
              <CrmIcon :type="row.icon" :size="14" />
            </span>
            <span
              class="min-w-0 flex-1 truncate text-[13px] font-medium"
              :style="{ color: activeKey === row.key ? '#1f2329' : '#6b7280' }"
            >
              {{ row.label }}
            </span>
            <span
              class="h-[18px] w-[3px] shrink-0 rounded-full transition-opacity"
              :style="{ background: row.color, opacity: activeKey === row.key ? 1 : 0 }"
            ></span>
          </button>
        </template>
      </nav>
    </aside>

    <!-- 右侧内容区 -->
    <main class="min-w-0 flex-1 overflow-y-auto p-[16px]">
      <component :is="activeRule.view" v-if="activeRule" />
      <ManualGuidePage v-else-if="activeGuide" :meta="activeGuide" />
      <OnboardingFlow v-else />
    </main>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiFeatures from '@/views/platformManual/ai-features/index.vue';
  import ManualGuidePage from '@/views/platformManual/components/ManualGuidePage.vue';
  import ConfigItems from '@/views/platformManual/config-items/index.vue';
  import NoticeRules from '@/views/platformManual/notice-rules/index.vue';
  import OnboardingFlow from '@/views/platformManual/onboarding-flow/index.vue';
  import RolePermission from '@/views/platformManual/role-permission/index.vue';
  import VerifyRules from '@/views/platformManual/verify-rules/index.vue';

  import type { GuideMeta } from '@/views/platformManual/usage/guideMeta';
  import { guideMetas } from '@/views/platformManual/usage/guideMeta';
  import type { Component } from 'vue';

  const { t } = useI18n();

  interface RuleItem {
    key: string;
    label: string;
    icon: string;
    color: string;
    view: Component;
  }

  const ruleItems: RuleItem[] = [
    {
      key: 'onboarding-flow',
      label: 'menu.platformManualOnboardingFlow',
      icon: 'iconicon_books',
      color: '#4282FF',
      view: OnboardingFlow,
    },
    {
      key: 'notice-rules',
      label: 'menu.platformManualNoticeRules',
      icon: 'iconicon_notification',
      color: '#4282FF',
      view: NoticeRules,
    },
    {
      key: 'verify-rules',
      label: 'menu.platformManualVerifyRules',
      icon: 'iconicon_check_circle',
      color: '#f59e0b',
      view: VerifyRules,
    },
    {
      key: 'role-permission',
      label: 'menu.platformManualRolePermission',
      icon: 'iconicon_usergroup',
      color: '#8b5cf6',
      view: RolePermission,
    },
    {
      key: 'ai-features',
      label: 'menu.platformManualAiFeatures',
      icon: 'iconicon_aiming',
      color: '#10b981',
      view: AiFeatures,
    },
    {
      key: 'config-items',
      label: 'menu.platformManualConfigItems',
      icon: 'iconicon_setting',
      color: '#06b6d4',
      view: ConfigItems,
    },
  ];

  const guideByKey = Object.fromEntries(guideMetas.map((meta) => [meta.key, meta])) as Record<string, GuideMeta>;

  // 使用说明的 4 个模块分组（title 复用现有菜单名，文案与左侧菜单保持一致）。
  const usageGroups: { key: string; title: string; keys: string[] }[] = [
    {
      key: 'tenant-lifecycle',
      title: 'menu.platformFinance',
      keys: ['registerAudit', 'paidUser', 'planApplication', 'contract', 'paymentRecord', 'invoice', 'revenue'],
    },
    { key: 'city-partner', title: 'menu.cityManager', keys: ['cityAccount', 'cityDashboard'] },
    {
      key: 'platform-ops',
      title: 'menu.platformAdmin',
      keys: ['edition', 'log', 'bugReport', 'securityEvent', 'jobExecLog', 'errorLog'],
    },
    { key: 'ai-quota', title: 'menu.platformAiQuota', keys: ['aiQuota'] },
  ];

  type NavRow =
    | { type: 'section'; key: string; label: string }
    | { type: 'group'; key: string; label: string }
    | {
        type: 'item';
        key: string;
        label: string;
        icon: string;
        color: string;
        indent?: boolean;
        view?: Component;
        meta?: GuideMeta;
      };

  const navRows = computed<NavRow[]>(() => {
    const rows: NavRow[] = [];
    rows.push({ type: 'section', key: 'sec-rules', label: t('platformManual.shell.rulesTitle') });
    ruleItems.forEach((item) => {
      rows.push({
        type: 'item',
        key: item.key,
        label: t(item.label),
        icon: item.icon,
        color: item.color,
        view: item.view,
      });
    });

    rows.push({ type: 'section', key: 'sec-usage', label: t('platformManual.shell.usageTitle') });
    usageGroups.forEach((group) => {
      rows.push({ type: 'group', key: `grp-${group.key}`, label: t(group.title) });
      group.keys.forEach((key) => {
        const meta = guideByKey[key];
        rows.push({
          type: 'item',
          key: meta.key,
          label: t(meta.label),
          icon: meta.icon,
          color: meta.color,
          indent: true,
          meta,
        });
      });
    });
    return rows;
  });

  const activeKey = ref('onboarding-flow');

  const activeRule = computed(() => ruleItems.find((item) => item.key === activeKey.value));
  const activeGuide = computed(() => guideByKey[activeKey.value]);
</script>
