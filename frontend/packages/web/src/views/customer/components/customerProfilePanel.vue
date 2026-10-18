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
        <n-button type="primary" size="small" @click="goUpgrade">
          {{ t('customer.profileUpgradeAction') }}
        </n-button>
      </div>
    </template>

    <template v-else>
      <!-- 客户基础 -->
      <CrmCard :title="t('customer.profileBasicInfo')" hide-footer auto-height>
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

      <!-- 公开动态（联网检索新闻/舆情） -->
      <CrmCard :title="t('customer.profilePublicDynamic')" hide-footer auto-height>
        <div class="flex flex-col gap-[12px]">
          <n-button
            type="primary"
            size="small"
            class="w-fit"
            :loading="publicDynamicLoading"
            @click="fetchPublicDynamics"
          >
            {{ t('customer.profilePublicDynamicAction') }}
          </n-button>

          <template v-if="hasPublicDynamic">
            <div v-if="!hasAnySource" class="text-[13px] text-[var(--text-n3)]">
              {{ t('customer.profilePublicDynamicEmpty') }}
            </div>
            <div v-for="(group, gIndex) in publicDynamicGroups" :key="group.label" class="flex flex-col gap-[6px]">
              <template v-if="group.sources.length">
                <span class="text-[13px] font-semibold text-[var(--text-n1)]">
                  {{ group.label }}（{{ group.sources.length }} 条）
                </span>
                <div
                  v-for="(source, index) in group.sources"
                  :key="`${gIndex}-${index}`"
                  class="flex flex-col gap-[2px]"
                >
                  <a
                    :href="source.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-[13px] leading-[1.6] text-[var(--primary-8)] hover:underline"
                  >
                    {{ index + 1 }}. {{ source.title || source.url }}
                  </a>
                  <span v-if="source.snippet" class="text-[12px] leading-[1.5] text-[var(--text-n3)]">
                    {{ source.snippet }}
                  </span>
                  <template v-if="source.content">
                    <n-button
                      size="tiny"
                      quaternary
                      color="#f97316"
                      class="w-fit"
                      @click="togglePublicContent(gIndex, index)"
                    >
                      {{
                        showPublicContent[`${gIndex}-${index}`]
                          ? t('customer.profilePublicDynamicHideContent')
                          : t('customer.profilePublicDynamicViewContent')
                      }}
                    </n-button>
                    <div
                      v-if="showPublicContent[`${gIndex}-${index}`]"
                      class="whitespace-pre-wrap rounded-[6px] bg-[var(--fill-2)] p-[8px] text-[12px] leading-[1.6] text-[var(--text-n2)]"
                    >
                      {{ source.content }}
                    </div>
                  </template>
                </div>
              </template>
            </div>
          </template>
          <div v-else class="text-[12px] text-[var(--text-n3)]">{{ t('customer.profilePublicDynamicHint') }}</div>
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

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { emitOpenPersonalInfo } from '@lib/shared/method/personal-center';
  import type { AdvisorWebSource, SalesAdvisorAnalyzeResult } from '@lib/shared/models/ai';
  import type { CustomerProfileResponse, CustomerPublicDynamicResponse } from '@lib/shared/models/customer';
  import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import AdvisorResult from '@/views/workbench/smart/components/AdvisorResult.vue';

  import { fetchCustomerPublicDynamics, getCustomerProfile, getOpportunityStageConfig } from '@/api/modules';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const router = useRouter();

  const loading = ref(false);
  const profile = ref<CustomerProfileResponse>();
  const stageMap = ref<Record<string, string>>({});
  const publicDynamicLoading = ref(false);
  const publicDynamic = ref<CustomerPublicDynamicResponse>();
  const showPublicContent = ref<Record<string, boolean>>({});

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
    // 切换客户时清空上一客户的公开动态，避免串客
    publicDynamic.value = undefined;
    showPublicContent.value = {};
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

  interface PublicDynamicGroup {
    label: string;
    sources: AdvisorWebSource[];
  }

  const publicDynamicGroups = computed<PublicDynamicGroup[]>(() => [
    { label: t('customer.profilePublicDynamicNews'), sources: publicDynamic.value?.newsSources || [] },
    { label: t('customer.profilePublicDynamicSentiment'), sources: publicDynamic.value?.sentimentSources || [] },
  ]);
  const hasPublicDynamic = computed(() => publicDynamic.value !== undefined);
  const hasAnySource = computed(
    () => (publicDynamic.value?.newsSources?.length || 0) + (publicDynamic.value?.sentimentSources?.length || 0) > 0
  );

  function togglePublicContent(gIndex: number, index: number) {
    const key = `${gIndex}-${index}`;
    showPublicContent.value[key] = !showPublicContent.value[key];
  }

  async function fetchPublicDynamics() {
    if (!props.sourceId || publicDynamicLoading.value) {
      return;
    }
    publicDynamicLoading.value = true;
    try {
      publicDynamic.value = await fetchCustomerPublicDynamics(props.sourceId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      publicDynamicLoading.value = false;
    }
  }

  function goAdvisor() {
    router.push({ name: WorkbenchRouteEnum.AI_SMART });
  }

  // 了解升级：跳转到个人信息（个人中心）的「版本与授权」查看并自助申请续费/升级
  function goUpgrade() {
    emitOpenPersonalInfo(PersonalEnum.INFO);
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
