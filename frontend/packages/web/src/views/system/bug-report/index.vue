<template>
  <div class="bug-report-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="keyword"
          :placeholder="t('bugReport.keywordPlaceholder')"
          clearable
          class="w-[280px]"
          @keydown.enter="search"
        />
        <n-select
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('bugReport.status')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="crm-bug-report-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>

    <CrmDrawer v-model:show="showDetail" :footer="false" :title="t('bugReport.detailTitle')" :width="680">
      <n-spin :show="detailLoading">
        <div v-if="detail" class="flex flex-col gap-4">
          <n-descriptions label-placement="left" :column="2" bordered size="small">
            <n-descriptions-item :label="t('bugReport.user')" :span="2">
              {{ detail.userName || '-' }}（{{ detail.userId || '-' }}）
            </n-descriptions-item>
            <n-descriptions-item :label="t('bugReport.route')" :span="2">{{ detail.route || '-' }}</n-descriptions-item>
            <n-descriptions-item :label="t('bugReport.version')">{{ detail.version || '-' }}</n-descriptions-item>
            <n-descriptions-item :label="t('bugReport.screen')">{{ detail.screen || '-' }}</n-descriptions-item>
            <n-descriptions-item :label="t('bugReport.roles')" :span="2">{{ detail.roles || '-' }}</n-descriptions-item>
            <n-descriptions-item :label="t('bugReport.traceId')" :span="2">{{
              detail.traceId || '-'
            }}</n-descriptions-item>
            <n-descriptions-item :label="t('bugReport.createTime')" :span="2">
              {{ formatTime(detail.createTime) }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('bugReport.userAgent')" :span="2">
              {{ detail.userAgent || '-' }}
            </n-descriptions-item>
          </n-descriptions>

          <div>
            <div class="mb-1 text-sm font-medium">{{ t('bugReport.description') }}</div>
            <div class="whitespace-pre-wrap text-sm">{{ detail.description || '-' }}</div>
          </div>
          <div v-if="detail.steps">
            <div class="mb-1 text-sm font-medium">{{ t('bugReport.steps') }}</div>
            <div class="whitespace-pre-wrap text-sm">{{ detail.steps }}</div>
          </div>
          <div v-if="detail.screenshot">
            <div class="mb-1 text-sm font-medium">{{ t('bugReport.screenshot') }}</div>
            <n-image :src="detail.screenshot" object-fit="contain" />
          </div>
          <div>
            <div class="mb-1 text-sm font-medium">{{ t('bugReport.recentErrors') }}</div>
            <pre class="max-h-[240px] overflow-auto rounded bg-[var(--text-n9)] p-2 text-xs">{{
              formatJson(detail.recentErrors)
            }}</pre>
          </div>
          <div>
            <div class="mb-1 text-sm font-medium">{{ t('bugReport.failedRequests') }}</div>
            <pre class="max-h-[240px] overflow-auto rounded bg-[var(--text-n9)] p-2 text-xs">{{
              formatJson(detail.failedRequests)
            }}</pre>
          </div>
          <div class="flex justify-end">
            <n-button type="primary" @click="handleDownload">{{ t('bugReport.download') }}</n-button>
          </div>
        </div>
      </n-spin>
    </CrmDrawer>
  </div>
</template>

<script setup lang="ts">
  import { h, ref } from 'vue';
  import { NButton, NDescriptions, NDescriptionsItem, NImage, NInput, NSelect, NSpin, NTag } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { downloadByteFile } from '@lib/shared/method';
  import type { BugReportDetail, BugReportItem } from '@lib/shared/models/system/bugReport';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { bugReportDetail, bugReportList } from '@/api/modules/system/bugReport';

  const { t } = useI18n();

  const keyword = ref('');
  const queryStatus = ref<string>('');

  const statusOptions = [
    { label: t('common.all'), value: '' },
    { label: t('bugReport.status.pending'), value: 'PENDING' },
    { label: t('bugReport.status.done'), value: 'DONE' },
  ];

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function statusTagType(status: string) {
    return status === 'DONE' ? 'success' : 'warning';
  }

  function statusLabel(status: string) {
    return status === 'DONE' ? t('bugReport.status.done') : t('bugReport.status.pending');
  }

  function formatJson(json?: string): string {
    if (!json) return '（无）';
    try {
      return JSON.stringify(JSON.parse(json), null, 2);
    } catch {
      return json;
    }
  }

  // 详情
  const showDetail = ref(false);
  const detailLoading = ref(false);
  const detail = ref<BugReportDetail | null>(null);

  async function openDetail(row: BugReportItem) {
    showDetail.value = true;
    detailLoading.value = true;
    detail.value = null;
    try {
      detail.value = await bugReportDetail(row.id);
    } finally {
      detailLoading.value = false;
    }
  }

  function buildMarkdown(d: BugReportDetail): string {
    const lines: string[] = [];
    lines.push('# 问题反馈报告', '');
    lines.push('## 环境信息');
    lines.push(`- 提交时间：${formatTime(d.createTime)}`);
    lines.push(`- 企业（organizationId）：${d.organizationId || '-'}`);
    lines.push(`- 用户：${d.userName || '-'}（${d.userId || '-'}）`);
    lines.push(`- 角色：${d.roles || '-'}`);
    lines.push(`- 页面路由：${d.route || '-'}`);
    lines.push(`- 系统版本：${d.version || '-'}`);
    lines.push(`- 浏览器：${d.userAgent || '-'}`);
    lines.push(`- 屏幕分辨率：${d.screen || '-'}`);
    lines.push(`- 语言：${d.language || '-'}`, '');
    lines.push('## 问题描述');
    lines.push(d.description || '（无）', '');
    lines.push('## 复现步骤');
    lines.push(d.steps || '（无）', '');
    if (d.screenshot) {
      lines.push('## 截图', `![截图](${d.screenshot})`, '');
    }
    if (d.traceId) {
      lines.push('## traceId', d.traceId, '');
    }
    lines.push('## 最近运行错误');
    lines.push(formatJson(d.recentErrors), '');
    lines.push('## 最近失败接口');
    lines.push(formatJson(d.failedRequests), '');
    return lines.join('\n');
  }

  function handleDownload() {
    if (!detail.value) return;
    const md = buildMarkdown(detail.value);
    const d = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const fileName = `bug-report-${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(
      d.getHours()
    )}${pad(d.getMinutes())}${pad(d.getSeconds())}.md`;
    downloadByteFile(new Blob([md], { type: 'text/markdown;charset=utf-8' }), fileName);
  }

  const columns: CrmDataTableColumn[] = [
    {
      fixed: 'left',
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (_row: unknown, rowIndex: number) => rowIndex + 1,
    },
    {
      title: t('bugReport.createTime'),
      key: 'createTime',
      width: 160,
      sortOrder: false,
      sorter: true,
      render: (row: BugReportItem) => formatTime(row.createTime),
    },
    {
      title: t('bugReport.user'),
      key: 'userName',
      width: 120,
      ellipsis: { tooltip: true },
    },
    {
      title: t('bugReport.route'),
      key: 'route',
      width: 160,
      ellipsis: { tooltip: true },
      render: (row: BugReportItem) => row.route || '-',
    },
    {
      title: t('bugReport.description'),
      key: 'description',
      minWidth: 240,
      ellipsis: { tooltip: true },
    },
    {
      title: t('bugReport.traceId'),
      key: 'traceId',
      width: 150,
      ellipsis: { tooltip: true },
      render: (row: BugReportItem) => row.traceId || '-',
    },
    {
      title: t('bugReport.status'),
      key: 'status',
      width: 100,
      render: (row: BugReportItem) =>
        h(NTag, { type: statusTagType(row.status), size: 'small' }, { default: () => statusLabel(row.status) }),
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 100,
      fixed: 'right',
      render: (row: BugReportItem) =>
        h(
          NButton,
          { text: true, type: 'primary', onClick: () => openDetail(row) },
          { default: () => t('bugReport.detail') }
        ),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<BugReportItem>(bugReportList, {
    tableKey: TableKeyEnum.SYSTEM_BUG_REPORT_TABLE,
    columns,
    showSetting: true,
    containerClass: '.crm-bug-report-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  function search() {
    const params: Record<string, unknown> = {};
    if (keyword.value.trim()) params.keyword = keyword.value.trim();
    if (queryStatus.value) params.status = queryStatus.value;
    setLoadListParams(params);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function reset() {
    keyword.value = '';
    queryStatus.value = '';
    search();
  }

  onMounted(() => {
    search();
  });
</script>

<style lang="less" scoped>
  .bug-report-page {
    @apply flex h-full flex-col overflow-hidden;
  }
</style>
