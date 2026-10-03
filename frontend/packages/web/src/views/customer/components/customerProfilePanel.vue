<template>
  <div class="customer-profile-panel flex h-full flex-col gap-[16px] overflow-y-auto p-[16px]">
    <template v-if="loading">
      <div class="flex flex-1 items-center justify-center">
        <n-spin />
      </div>
    </template>

    <!-- 未开通专业版+：升级空态 -->
    <template v-else-if="!profile?.available">
      <div class="flex flex-1 flex-col items-center justify-center gap-[16px]">
        <n-empty :description="t('customer.profileUpgradeTip')" />
        <n-button type="primary" size="small" @click="goAdvisor">
          {{ t('customer.profileUpgradeAction') }}
        </n-button>
      </div>
    </template>

    <template v-else>
      <!-- 客户基础 -->
      <CrmCard :title="t('customer.profile')" hide-footer auto-height>
        <div class="flex flex-wrap items-center gap-x-[24px] gap-y-[8px] text-[13px] text-[var(--text-n2)]">
          <span>{{ t('customer.profileCustomerName') }}：{{ profile.customerName || '—' }}</span>
          <span>{{ t('customer.profileOwnerName') }}：{{ profile.ownerName || '—' }}</span>
          <span>{{ t('customer.lastFollowUpDate') }}：{{ formatTime(profile.followTime) }}</span>
          <span>{{ t('customer.lastFollowUps') }}：{{ profile.followerName || '—' }}</span>
        </div>
      </CrmCard>

      <!-- AI 洞察（会话军师沉淀，只读回读） -->
      <CrmCard :title="t('customer.profileAiInsight')" hide-footer auto-height>
        <AdvisorResult v-if="hasAiInsight" :result="aiInsight" />
        <div v-else class="flex flex-col items-center gap-[12px] py-[16px]">
          <span class="text-[13px] text-[var(--text-n3)]">{{ t('customer.profileNoAiInsight') }}</span>
          <n-button type="primary" size="small" @click="goAdvisor">
            {{ t('customer.profileGoAdvisor') }}
          </n-button>
        </div>
      </CrmCard>

      <!-- 360° 数据总览 -->
      <div class="grid grid-cols-3 gap-[12px]">
        <div v-for="metric in metrics" :key="metric.label" class="profile-metric">
          <div class="text-[12px] text-[var(--text-n3)]">{{ metric.label }}</div>
          <div class="mt-[4px] text-[20px] font-semibold leading-none text-[var(--text-n1)]">{{ metric.value }}</div>
          <div v-if="metric.sub" class="mt-[6px] truncate text-[12px] text-[var(--text-n4)]" :title="metric.sub">
            {{ metric.sub }}
          </div>
        </div>
      </div>

      <!-- 商机阶段分布 -->
      <CrmCard v-if="stageList.length" :title="t('customer.profileStage')" hide-footer auto-height>
        <div class="flex flex-wrap gap-[8px]">
          <n-tag v-for="stage in stageList" :key="stage.stage" size="small" :bordered="false">
            {{ stage.name }} · {{ stage.count }}
          </n-tag>
        </div>
      </CrmCard>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import { NButton, NEmpty, NSpin, NTag } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SalesAdvisorAnalyzeResult } from '@lib/shared/models/ai';
  import type { CustomerProfileResponse } from '@lib/shared/models/customer';
  import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import AdvisorResult from '@/views/workbench/smart/components/AdvisorResult.vue';

  import { getCustomerProfile, getOpportunityStageConfig } from '@/api/modules';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const router = useRouter();

  const loading = ref(false);
  const profile = ref<CustomerProfileResponse>();
  const stageMap = ref<Record<string, string>>({});

  function buildStageMap(config?: OpportunityStageConfig): Record<string, string> {
    const map: Record<string, string> = {};
    (config?.stageConfigList || []).forEach((stage) => {
      map[stage.id] = stage.name;
    });
    return map;
  }

  function formatAmount(value?: number | string): string {
    if (value === undefined || value === null || value === '') {
      return '0';
    }
    const num = Number(value);
    if (!Number.isFinite(num)) {
      return String(value);
    }
    return num.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

  function formatPercent(value?: number | string): string {
    if (value === undefined || value === null || value === '') {
      return '0%';
    }
    const num = Number(value);
    if (!Number.isFinite(num)) {
      return String(value);
    }
    return `${num}%`;
  }

  function formatTime(ts?: number): string {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '—';
  }

  async function load() {
    if (!props.sourceId) {
      return;
    }
    loading.value = true;
    try {
      const [profileRes, stageConfig] = await Promise.all([
        getCustomerProfile(props.sourceId),
        getOpportunityStageConfig().catch(() => undefined),
      ]);
      profile.value = profileRes;
      stageMap.value = buildStageMap(stageConfig);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  const hasAiInsight = computed(() => !!profile.value?.aiInsight);
  const aiInsight = computed<SalesAdvisorAnalyzeResult>(() => {
    return (profile.value?.aiInsight as SalesAdvisorAnalyzeResult) || {};
  });

  interface Metric {
    label: string;
    value: string;
    sub?: string;
  }

  const metrics = computed<Metric[]>(() => {
    const p = profile.value;
    if (!p) {
      return [];
    }
    return [
      {
        label: t('customer.profileFollow'),
        value: `${p.follow?.count ?? 0} ${t('customer.profileFollowUnit')}`,
        sub: p.follow?.latestContent,
      },
      {
        label: t('customer.profileOpportunity'),
        value: `${p.opportunity?.count ?? 0} ${t('customer.profileCountUnit')}`,
        sub: formatAmount(p.opportunity?.amount),
      },
      {
        label: t('customer.profileOrder'),
        value: `${p.order?.count ?? 0} ${t('customer.profileCountUnit')}`,
        sub: formatAmount(p.order?.amount),
      },
      {
        label: t('customer.profileContract'),
        value: `${p.contract?.count ?? 0} ${t('customer.profileCountUnit')}`,
        sub: formatAmount(p.contract?.amount),
      },
      { label: t('customer.profilePaidAmount'), value: formatAmount(p.paidAmount) },
      { label: t('customer.profilePaymentRate'), value: formatPercent(p.paymentRate) },
    ];
  });

  const stageList = computed(() =>
    (profile.value?.opportunityStages || []).map((stage) => ({
      stage: stage.stage || '',
      name: stageMap.value[stage.stage || ''] || stage.stage || '—',
      count: stage.count ?? 0,
    }))
  );

  function goAdvisor() {
    router.push({ name: WorkbenchRouteEnum.WORKBENCH_SMART });
  }

  watch(
    () => props.sourceId,
    () => load(),
    { immediate: true }
  );
</script>

<style scoped>
  .profile-metric {
    padding: 12px;
    border: 1px solid rgb(148 163 184 / 10%);
    border-radius: 8px;
    background: rgb(148 163 184 / 5%);
  }
</style>
