<template>
  <div class="dm-profile-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard
      hide-footer
      no-content-padding
      content-height="100%"
      class="dm-profile-card min-h-0 flex-1 overflow-hidden"
    >
      <template #title>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.workbench.dmProfile') }}</span>
        </div>
      </template>

      <n-tabs v-model:value="activeTab" type="line" class="dm-profile-tabs h-full">
        <!-- 账号绑定 -->
        <n-tab-pane name="account" :tab="t('dmProfile.accountTab')" class="h-full">
          <div class="flex h-full flex-col gap-[12px] overflow-hidden p-[16px]">
            <div class="text-[13px] text-orange-500">{{ t('dmProfile.desc') }}</div>

            <div class="flex items-start gap-[12px]">
              <n-select
                v-model:value="bindForm.platform"
                :options="platformOptions"
                :placeholder="t('dmProfile.platformPlaceholder')"
                class="!w-[160px]"
              />
              <n-input
                v-model:value="bindForm.cookieText"
                type="textarea"
                :placeholder="t('dmProfile.cookiePlaceholder')"
                class="min-w-0 flex-1"
                :autosize="{ minRows: 3, maxRows: 5 }"
              />
            </div>
            <div class="flex items-center gap-[12px]">
              <div class="text-[12px] text-[var(--text-n4)]">{{ t('dmProfile.cookieTip') }}</div>
              <n-button quaternary size="tiny" type="primary" @click="goGuide">{{ t('dmProfile.viewGuide') }}</n-button>
              <div class="ml-auto">
                <AiActionButton :loading="binding" :disabled="!canBind" @click="handleBind">
                  {{ binding ? t('dmProfile.binding') : t('dmProfile.bind') }}
                </AiActionButton>
              </div>
            </div>

            <div class="min-h-0 flex-1 overflow-auto">
              <n-empty
                v-if="!accounts.length && !loadingAccounts"
                :description="t('dmProfile.noAccount')"
                class="mt-[40px]"
              />
              <n-data-table
                v-else
                :columns="accountColumns"
                :data="accounts"
                :row-key="(row: DmAccount) => row.id"
                :loading="loadingAccounts"
                :bordered="false"
                size="small"
              />
            </div>
          </div>
        </n-tab-pane>

        <!-- 私信画像 -->
        <n-tab-pane name="profile" :tab="t('dmProfile.profileTab')" class="h-full">
          <div class="flex h-full flex-col overflow-hidden p-[16px]">
            <CrmTable
              v-model:checked-row-keys="checkedProfileIds"
              v-bind="propsRes"
              class="dm-profile-table min-h-0 flex-1"
              :columns="profileColumns"
              no-pagination
              @page-change="propsEvent.pageChange"
              @page-size-change="propsEvent.pageSizeChange"
              @sorter-change="propsEvent.sorterChange"
              @filter-change="propsEvent.filterChange"
              @refresh="loadProfiles"
            >
              <template #tableTop>
                <n-select
                  v-model:value="statusFilter"
                  clearable
                  :options="statusFilterOptions"
                  :placeholder="t('dmProfile.statusFilterPlaceholder')"
                  class="!w-[180px]"
                  @update:value="loadProfiles"
                />
              </template>
              <template #actionRight>
                <n-button
                  type="primary"
                  :disabled="!checkedProfileIds.length"
                  :loading="importing"
                  @click="handleImport"
                >
                  {{ importing ? t('dmProfile.importing') : t('dmProfile.import') }}
                </n-button>
              </template>
              <template #empty>
                <n-empty :description="t('dmProfile.noProfile')" class="mt-[40px]" />
              </template>
            </CrmTable>
          </div>
        </n-tab-pane>
      </n-tabs>
    </CrmCard>
  </div>

  <!-- 采集进行中的全屏遮罩（E6 生成中特效） -->
  <Teleport to="body">
    <Transition name="dm-mask-fade">
      <div v-if="crawlingId" class="dm-profile-crawl-mask">
        <PulsingBadge color="#2f54eb" :size="96" :ring-width="5" pulse-text>
          <span class="text-[20px] font-semibold tracking-wide text-white">{{ t('dmProfile.crawling') }}</span>
        </PulsingBadge>
        <div class="mt-[18px] text-[13px] text-white/70">{{ t('dmProfile.crawlingHint') }}</div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
  import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { NButton, NDataTable, NEmpty, NInput, NSelect, NTabPane, NTabs, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DmAccount, DmProfile, DmProfileDetail } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn, CrmTableDataItem } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import PulsingBadge from '@/components/pure/effects/PulsingBadge.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import {
    bindDmProfileAccount,
    crawlDmProfile,
    getDmProfileAccounts,
    getDmProfileProfiles,
    importDmProfile,
    unbindDmProfileAccount,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  import type { DataTableColumns, DataTableRowKey } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const router = useRouter();

  // ==================== 账号 ====================
  const activeTab = ref<'account' | 'profile'>('account');
  const accounts = ref<DmAccount[]>([]);
  const loadingAccounts = ref(false);
  const bindForm = ref<{ platform: string | null; cookieText: string }>({ platform: 'douyin', cookieText: '' });
  const binding = ref(false);
  const crawlingId = ref('');

  const platformOptions = computed(() => [
    { label: t('dmProfile.platform.douyin'), value: 'douyin' },
    { label: `${t('dmProfile.platform.kuaishou')}（${t('dmProfile.platformPending')}）`, value: 'kuaishou' },
    { label: `${t('dmProfile.platform.xiaohongshu')}（${t('dmProfile.platformPending')}）`, value: 'xiaohongshu' },
  ]);

  const canBind = computed(() => !!bindForm.value.platform && !!bindForm.value.cookieText.trim());

  // ==================== 画像 ====================
  const statusFilter = ref<string | null>(null);
  const checkedProfileIds = ref<DataTableRowKey[]>([]);
  const importing = ref(false);

  const statusFilterOptions = computed(() => [
    { label: t('dmProfile.profile.status.ANALYZED'), value: 'ANALYZED' },
    { label: t('dmProfile.profile.status.RAW'), value: 'RAW' },
    { label: t('dmProfile.profile.status.IMPORTED'), value: 'IMPORTED' },
    { label: t('dmProfile.profile.status.FAILED'), value: 'FAILED' },
  ]);

  const { propsRes, propsEvent, loadList } = useTable<DmProfile>(
    async () => {
      const list = await getDmProfileProfiles(statusFilter.value || undefined);
      return {
        list: list as CrmTableDataItem<DmProfile>[],
        current: 1,
        pageSize: list.length,
        total: list.length,
      };
    },
    {
      tableKey: TableKeyEnum.DM_PROFILE,
      showSetting: true,
      showPagination: false,
      containerClass: '.dm-profile-table',
    }
  );

  // ==================== 渲染辅助 ====================

  function formatTime(ts?: number | string): string {
    if (!ts) {
      return '-';
    }
    const d = dayjs(ts);
    return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-';
  }

  function parseProfile(json?: string): DmProfileDetail | null {
    if (!json) {
      return null;
    }
    try {
      return JSON.parse(json) as DmProfileDetail;
    } catch {
      return null;
    }
  }

  function isUnknown(v?: string): boolean {
    return !v || v === '未知' || v.toLowerCase() === 'unknown';
  }

  function displayValue(v?: string): string {
    return isUnknown(v) ? '-' : (v as string);
  }

  function statusStyle(status: string): string {
    const colorMap: Record<string, string> = {
      LOGGED_IN: '#18a058',
      CRAWLING: '#2563eb',
      ANALYZED: '#18a058',
      IMPORTED: '#2563eb',
      EXPIRED: '#f97316',
      RAW: '#f97316',
      FAILED: '#e5484d',
    };
    const color = colorMap[status] || '#8a94a6';
    return `padding:2px 8px;border-radius:4px;font-size:12px;color:${color};background:${color}1a;`;
  }

  function renderStatus(status: string, prefix: 'account' | 'profile') {
    const key = prefix === 'account' ? `dmProfile.account.status.${status}` : `dmProfile.profile.status.${status}`;
    const text = t(key);
    // 未命中翻译时回退到状态原文，避免直接显示 i18n key
    return h('span', { style: statusStyle(status) }, text === key ? status : text);
  }

  /** 从画像 JSON 中取单个字段并做「未知」兜底，用于列表列渲染 */
  function profileField(row: DmProfile, field: keyof DmProfileDetail): string {
    const detail = parseProfile(row.profileJson);
    return detail ? displayValue(detail[field]) : '-';
  }

  function isCrawling(row: DmAccount): boolean {
    return crawlingId.value === row.id || row.status === 'CRAWLING';
  }

  // ==================== 数据加载与操作 ====================

  async function loadAccounts() {
    loadingAccounts.value = true;
    try {
      accounts.value = await getDmProfileAccounts();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      loadingAccounts.value = false;
    }
  }

  async function loadProfiles() {
    checkedProfileIds.value = [];
    try {
      await loadList();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function goGuide() {
    const { href } = router.resolve({ name: WorkbenchRouteEnum.WORKBENCH_DM_PROFILE_GUIDE });
    window.open(href, '_blank');
  }

  async function handleBind() {
    if (!canBind.value) {
      Message.warning(t('dmProfile.cookiePlaceholder'));
      return;
    }
    binding.value = true;
    try {
      await bindDmProfileAccount({
        platform: bindForm.value.platform as string,
        cookieText: bindForm.value.cookieText.trim(),
      });
      Message.success(t('dmProfile.bindSuccess'));
      bindForm.value.cookieText = '';
      await loadAccounts();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      binding.value = false;
    }
  }

  let pollTimer: number | null = null;

  function stopPolling() {
    if (pollTimer !== null) {
      window.clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  function startPolling(accountId: string) {
    stopPolling();
    pollTimer = window.setInterval(async () => {
      try {
        accounts.value = await getDmProfileAccounts();
        // 采集期间轮询刷新走静默模式，避免列表频繁闪「转圈加载」，加载态交给全屏脉冲遮罩
        await loadList(false, undefined, undefined, true);
        const account = accounts.value.find((a) => a.id === accountId);
        if (!account || account.status !== 'CRAWLING') {
          stopPolling();
          crawlingId.value = '';
          if (account?.status === 'LOGGED_IN') {
            Message.success(t('dmProfile.crawlSuccess'));
          } else if (account?.status === 'FAILED' || account?.status === 'EXPIRED') {
            Message.error(account.lastError || t('common.operationFailed'));
          }
        }
      } catch {
        // 轮询失败忽略，等待下一轮重试
      }
    }, 3000);
  }

  async function handleCrawl(row: DmAccount) {
    crawlingId.value = row.id;
    try {
      await crawlDmProfile(row.id);
      Message.info(t('dmProfile.crawlStarted'));
      await loadAccounts();
      activeTab.value = 'profile';
      startPolling(row.id);
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
      crawlingId.value = '';
    }
  }

  function handleUnbind(row: DmAccount) {
    openModal({
      type: 'error',
      title: t('dmProfile.unbind'),
      content: t('dmProfile.unbindConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await unbindDmProfileAccount(row.id);
          Message.success(t('dmProfile.unbindSuccess'));
          await loadAccounts();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleImport() {
    if (!checkedProfileIds.value.length) {
      Message.warning(t('dmProfile.selectHint'));
      return;
    }
    importing.value = true;
    try {
      const result = await importDmProfile({ profileIds: checkedProfileIds.value as string[] });
      const filtered = result.filteredCount ?? 0;
      if (result.successCount > 0) {
        Message.success(
          filtered > 0
            ? t('dmProfile.importResult', { count: result.successCount, filtered })
            : t('dmProfile.importSuccess', { count: result.successCount })
        );
      } else if (filtered > 0) {
        Message.warning(t('dmProfile.importNoValid', { filtered }));
      }
      if (result.failedNames && result.failedNames.length > 0) {
        Message.warning(t('dmProfile.importFailed', { count: result.failedNames.length }));
      }
      await loadProfiles();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      importing.value = false;
    }
  }

  // ==================== 表格列 ====================

  const accountColumns = computed<DataTableColumns<DmAccount>>(() => [
    {
      title: t('dmProfile.accountColPlatform'),
      key: 'platform',
      width: 90,
      render: (row) => t(`dmProfile.platform.${row.platform}`) || row.platform,
    },
    {
      title: t('dmProfile.accountColAccountName'),
      key: 'accountName',
      minWidth: 120,
      render: (row) => row.accountName || '-',
    },
    { title: t('dmProfile.accountColUniqueId'), key: 'uniqueId', minWidth: 120, render: (row) => row.uniqueId || '-' },
    {
      title: t('dmProfile.accountColStatus'),
      key: 'status',
      width: 90,
      render: (row) => renderStatus(row.status, 'account'),
    },
    {
      title: t('dmProfile.accountColLastCrawl'),
      key: 'lastCrawlTime',
      width: 150,
      render: (row) => formatTime(row.lastCrawlTime),
    },
    {
      title: t('dmProfile.accountColAction'),
      key: 'action',
      width: 160,
      render: (row) =>
        h('div', { class: 'flex items-center gap-[8px]' }, [
          h(
            NButton,
            {
              size: 'tiny',
              type: 'primary',
              ghost: true,
              disabled: isCrawling(row),
              loading: crawlingId.value === row.id,
              onClick: () => handleCrawl(row),
            },
            { default: () => (isCrawling(row) ? t('dmProfile.crawling') : t('dmProfile.crawl')) }
          ),
          h(
            NButton,
            { size: 'tiny', quaternary: true, type: 'error', onClick: () => handleUnbind(row) },
            { default: () => t('dmProfile.unbind') }
          ),
        ]),
    },
  ]);

  const profileColumns = computed<CrmDataTableColumn<DmProfile>[]>(() => [
    { type: 'selection' },
    { title: t('dmProfile.colNickname'), key: 'nickname', minWidth: 120, render: (row) => row.nickname || '-' },
    { title: t('dmProfile.profile.name'), key: 'name', width: 90, render: (row) => profileField(row, 'name') },
    {
      title: t('dmProfile.profile.gender'),
      key: 'gender',
      width: 70,
      render: (row) => displayValue(row.gender || parseProfile(row.profileJson)?.gender),
    },
    { title: t('dmProfile.profile.age'), key: 'age', width: 80, render: (row) => profileField(row, 'age') },
    { title: t('dmProfile.profile.city'), key: 'city', width: 90, render: (row) => profileField(row, 'city') },
    {
      title: t('dmProfile.profile.accountCategory'),
      key: 'accountCategory',
      minWidth: 120,
      render: (row) => profileField(row, 'accountCategory'),
    },
    {
      title: t('dmProfile.profile.category'),
      key: 'category',
      minWidth: 110,
      render: (row) => profileField(row, 'category'),
    },
    {
      title: t('dmProfile.profile.preference'),
      key: 'preference',
      minWidth: 140,
      ellipsis: { tooltip: true },
      render: (row) => profileField(row, 'preference'),
    },
    {
      title: t('dmProfile.profile.summary'),
      key: 'summary',
      minWidth: 200,
      ellipsis: { tooltip: true },
      render: (row) => profileField(row, 'summary'),
    },
    {
      title: t('dmProfile.profile.confidence'),
      key: 'confidence',
      width: 80,
      render: (row) => profileField(row, 'confidence'),
    },
    { title: t('dmProfile.colPhone'), key: 'phone', width: 110, render: (row) => row.phone || '-' },
    { title: t('dmProfile.colWechat'), key: 'wechat', width: 110, render: (row) => row.wechat || '-' },
    {
      title: t('dmProfile.profile.bio'),
      key: 'bio',
      minWidth: 160,
      ellipsis: { tooltip: true },
      render: (row) => row.bio || '-',
    },
    { title: t('dmProfile.colUniqueId'), key: 'uniqueId', width: 110, render: (row) => row.uniqueId || '-' },
    { title: t('dmProfile.colFollower'), key: 'followerCount', width: 80, render: (row) => row.followerCount || '-' },
    {
      title: t('dmProfile.profile.following'),
      key: 'followingCount',
      width: 80,
      render: (row) => row.followingCount || '-',
    },
    { title: t('dmProfile.profile.liked'), key: 'likedCount', width: 80, render: (row) => row.likedCount || '-' },
    {
      title: t('dmProfile.colType'),
      key: 'isGroup',
      width: 80,
      render: (row) => (row.isGroup ? t('dmProfile.group') : t('dmProfile.personal')),
    },
    { title: t('dmProfile.colStatus'), key: 'status', width: 90, render: (row) => renderStatus(row.status, 'profile') },
    {
      title: t('dmProfile.colTime'),
      key: 'createTime',
      width: 150,
      render: (row) => formatTime(row.createTime),
    },
  ]);

  onMounted(() => {
    loadAccounts();
    loadProfiles();
  });

  onBeforeUnmount(stopPolling);
</script>

<style lang="less" scoped>
  .dm-profile-page {
    background: #f5f7fa;
  }
  .dm-profile-tabs {
    :deep(.n-tabs-nav) {
      padding: 0 16px;
    }
    :deep(.n-tab-pane) {
      height: calc(100% - 44px);
    }
  }
  .dm-profile-crawl-mask {
    position: fixed;
    z-index: 3000;
    display: flex;
    justify-content: center;
    align-items: center;
    background: rgb(0 0 0 / 70%);
    inset: 0;
    flex-direction: column;
  }
  .dm-mask-fade-enter-active,
  .dm-mask-fade-leave-active {
    transition: opacity 0.2s ease;
  }
  .dm-mask-fade-enter-from,
  .dm-mask-fade-leave-to {
    opacity: 0;
  }
</style>
