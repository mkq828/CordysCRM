<template>
  <div class="pf-invoice-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <CrmSelect
          v-model:value="queryContractId"
          :options="contractOptions"
          :placeholder="t('platformInvoice.contract')"
          clearable
          filterable
          class="w-[240px]"
        />
        <CrmSelect
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('platformInvoice.invoiceStatus')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
        <div class="flex-1" />
        <n-button type="primary" ghost @click="openAdd">{{ t('platformInvoice.addInvoice') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="pf-invoice-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>

    <!-- 新建/编辑发票 -->
    <n-modal
      v-model:show="showForm"
      preset="card"
      :title="formMode === 'add' ? t('platformInvoice.addInvoice') : t('platformInvoice.editInvoice')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformInvoice.contract') }}</span>
          <CrmSelect v-model:value="form.contractId" :options="contractOptions" filterable class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformInvoice.invoiceType') }}</span>
          <CrmSelect v-model:value="form.invoiceType" :options="invoiceTypeOptions" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformInvoice.amount') }}</span>
          <n-input-number v-model:value="form.amount" :min="0" :precision="2" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformInvoice.taxRate') }}</span>
          <n-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformInvoice.businessTitle') }}</span>
          <n-input v-model:value="form.businessTitle" class="flex-1" />
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformInvoice.remark') }}</span>
          <n-input v-model:value="form.remark" type="textarea" :rows="2" maxlength="255" show-count class="flex-1" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showForm = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="formLoading" @click="confirmSave">{{ t('common.confirm') }}</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 开票弹窗 -->
    <n-modal
      v-model:show="invoiceModal.show"
      preset="card"
      :title="t('platformInvoice.invoiceTitle')"
      class="w-[480px]"
      :mask-closable="false"
    >
      <div class="flex items-center gap-[12px]">
        <span class="w-[100px] shrink-0 text-right">{{ t('platformInvoice.invoiceNo') }}</span>
        <n-input
          v-model:value="invoiceModal.invoiceNo"
          class="flex-1"
          :placeholder="t('platformInvoice.invoiceNoPlaceholder')"
        />
      </div>
      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="invoiceModal.show = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="invoiceModal.loading" @click="submitInvoice">{{
            t('platformInvoice.confirmInvoice')
          }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { h, onMounted, reactive, ref, watch } from 'vue';
  import { NButton, NInput, NInputNumber, NModal, NTag, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import {
    PlatformInvoiceStatusEnum,
    PlatformInvoiceStatusTagMap,
    PlatformInvoiceTypeEnum,
  } from '@lib/shared/enums/platformFinanceEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { PlatformContractOption, PlatformInvoiceItem } from '@lib/shared/models/system/platformFinance';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSelect from '@/components/pure/crm-select/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    platformContractOptions,
    platformInvoiceAdd,
    platformInvoiceInvoice,
    platformInvoicePageList,
    platformInvoiceRemove,
    platformInvoiceUpdate,
    platformInvoiceVoid,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const queryContractId = ref('');
  const queryStatus = ref('');
  const tableRefreshId = ref(0);

  const contractOptions = ref<{ label: string; value: string }[]>([]);

  const statusOptions = [
    { label: t('common.all'), value: '' },
    { label: t('platformInvoice.status.NOT_INVOICED'), value: PlatformInvoiceStatusEnum.NOT_INVOICED },
    { label: t('platformInvoice.status.INVOICED'), value: PlatformInvoiceStatusEnum.INVOICED },
    { label: t('platformInvoice.status.VOIDED'), value: PlatformInvoiceStatusEnum.VOIDED },
  ];

  const invoiceTypeOptions = [
    { label: t('platformInvoice.invoiceType.NORMAL'), value: PlatformInvoiceTypeEnum.NORMAL },
    { label: t('platformInvoice.invoiceType.SPECIAL'), value: PlatformInvoiceTypeEnum.SPECIAL },
  ];

  async function loadContracts() {
    try {
      const list = await platformContractOptions();
      contractOptions.value = (list as PlatformContractOption[]).map((c) => ({
        label: c.orgName ? `${c.contractNo}（${c.orgName}）` : c.contractNo,
        value: c.id,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function fmtMoney(value?: number | string) {
    const n = Number(value ?? 0);
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function statusLabel(status: string) {
    return t(`platformInvoice.status.${status}`);
  }

  function invoiceTypeLabel(type?: string) {
    return type ? t(`platformInvoice.invoiceType.${type}`) : '-';
  }

  // 新建/编辑
  const showForm = ref(false);
  const formMode = ref<'add' | 'edit'>('add');
  const formLoading = ref(false);
  const form = reactive<{
    id: string;
    contractId: string;
    invoiceType: string;
    amount: number | null;
    taxRate: number | null;
    businessTitle: string;
    remark: string;
  }>({
    id: '',
    contractId: '',
    invoiceType: PlatformInvoiceTypeEnum.NORMAL,
    amount: null,
    taxRate: 6,
    businessTitle: '',
    remark: '',
  });

  function openAdd() {
    formMode.value = 'add';
    Object.assign(form, {
      id: '',
      contractId: '',
      invoiceType: PlatformInvoiceTypeEnum.NORMAL,
      amount: null,
      taxRate: 6,
      businessTitle: '',
      remark: '',
    });
    showForm.value = true;
  }

  function openEdit(row: PlatformInvoiceItem) {
    formMode.value = 'edit';
    Object.assign(form, {
      id: row.id,
      contractId: row.contractId,
      invoiceType: row.invoiceType || PlatformInvoiceTypeEnum.NORMAL,
      amount: row.amount == null ? null : Number(row.amount),
      taxRate: row.taxRate == null ? 6 : Number(row.taxRate),
      businessTitle: row.businessTitle || '',
      remark: row.remark || '',
    });
    showForm.value = true;
  }

  async function confirmSave() {
    if (!form.contractId) {
      Message.warning(t('platformInvoice.contractRequired'));
      return;
    }
    formLoading.value = true;
    try {
      const payload = {
        contractId: form.contractId,
        invoiceType: form.invoiceType,
        amount: form.amount ?? undefined,
        taxRate: form.taxRate ?? undefined,
        businessTitle: form.businessTitle || undefined,
        remark: form.remark || undefined,
      };
      if (formMode.value === 'add') {
        await platformInvoiceAdd(payload);
      } else {
        await platformInvoiceUpdate({ ...payload, id: form.id });
      }
      Message.success(t('platformInvoice.saveSuccess'));
      showForm.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      formLoading.value = false;
    }
  }

  // 开票
  const invoiceModal = reactive<{ show: boolean; loading: boolean; id: string; invoiceNo: string }>({
    show: false,
    loading: false,
    id: '',
    invoiceNo: '',
  });

  function openInvoice(row: PlatformInvoiceItem) {
    invoiceModal.id = row.id;
    invoiceModal.invoiceNo = '';
    invoiceModal.show = true;
  }

  async function submitInvoice() {
    invoiceModal.loading = true;
    try {
      await platformInvoiceInvoice({ id: invoiceModal.id, invoiceNo: invoiceModal.invoiceNo.trim() || undefined });
      Message.success(t('platformInvoice.invoiceSuccess'));
      invoiceModal.show = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      invoiceModal.loading = false;
    }
  }

  // 作废
  function handleVoid(row: PlatformInvoiceItem) {
    openModal({
      type: 'warning',
      title: t('platformInvoice.voidTitle'),
      content: t('platformInvoice.voidContent', { invoiceNo: row.invoiceNo || '-' }),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await platformInvoiceVoid({ id: row.id });
          Message.success(t('platformInvoice.voidSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleDelete(row: PlatformInvoiceItem) {
    openModal({
      type: 'warning',
      title: t('platformInvoice.deleteTitle'),
      content: t('platformInvoice.deleteContent', { invoiceNo: row.invoiceNo || '-' }),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await platformInvoiceRemove(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function buildActions(row: PlatformInvoiceItem): ActionsItem[] {
    const list: ActionsItem[] = [];
    if (row.invoiceStatus === PlatformInvoiceStatusEnum.NOT_INVOICED) {
      list.push({ label: t('platformInvoice.invoice'), key: 'invoice' });
      list.push({ label: t('platformInvoice.edit'), key: 'edit' });
    }
    if (row.invoiceStatus === PlatformInvoiceStatusEnum.INVOICED) {
      list.push({ label: t('platformInvoice.void'), key: 'void', danger: true });
    }
    list.push({ label: t('common.delete'), key: 'delete', danger: true });
    return list;
  }

  function handleActionSelect(row: PlatformInvoiceItem, key: string) {
    switch (key) {
      case 'invoice':
        openInvoice(row);
        break;
      case 'void':
        handleVoid(row);
        break;
      case 'edit':
        openEdit(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
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
      title: t('platformInvoice.invoiceNo'),
      key: 'invoiceNo',
      width: 150,
      ellipsis: { tooltip: true },
      render: (row: PlatformInvoiceItem) => row.invoiceNo || '-',
    },
    {
      title: t('platformInvoice.contract'),
      key: 'contractNo',
      width: 160,
      ellipsis: { tooltip: true },
      render: (row: PlatformInvoiceItem) => row.contractNo || '-',
    },
    {
      title: t('platformInvoice.tenant'),
      key: 'orgName',
      width: 160,
      ellipsis: { tooltip: true },
      render: (row: PlatformInvoiceItem) => row.orgName || '-',
    },
    {
      title: t('platformInvoice.signManager'),
      key: 'signManagerName',
      width: 110,
      ellipsis: { tooltip: true },
      render: (row: PlatformInvoiceItem) => row.signManagerName || '-',
    },
    {
      title: t('platformInvoice.followManager'),
      key: 'followManagerName',
      width: 110,
      ellipsis: { tooltip: true },
      render: (row: PlatformInvoiceItem) => row.followManagerName || '-',
    },
    {
      title: t('platformInvoice.invoiceType'),
      key: 'invoiceType',
      width: 90,
      render: (row: PlatformInvoiceItem) => invoiceTypeLabel(row.invoiceType),
    },
    {
      title: t('platformInvoice.amount'),
      key: 'amount',
      width: 120,
      align: 'right',
      render: (row: PlatformInvoiceItem) => fmtMoney(row.amount),
    },
    {
      title: t('platformInvoice.taxAmount'),
      key: 'taxAmount',
      width: 110,
      align: 'right',
      render: (row: PlatformInvoiceItem) => fmtMoney(row.taxAmount),
    },
    {
      title: t('platformInvoice.invoiceStatus'),
      key: 'invoiceStatus',
      width: 90,
      render: (row: PlatformInvoiceItem) =>
        h(
          NTag,
          { type: PlatformInvoiceStatusTagMap[row.invoiceStatus] || 'default', size: 'small' },
          { default: () => statusLabel(row.invoiceStatus) }
        ),
    },
    {
      title: t('platformInvoice.createTime'),
      key: 'createTime',
      width: 160,
      sortOrder: false,
      sorter: true,
      render: (row: PlatformInvoiceItem) => formatTime(row.createTime),
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 200,
      fixed: 'right',
      render: (row: PlatformInvoiceItem) =>
        h(CrmOperationButton, {
          groupList: buildActions(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<PlatformInvoiceItem>(platformInvoicePageList, {
    tableKey: TableKeyEnum.PLATFORM_INVOICE_TABLE,
    columns,
    showSetting: false,
    containerClass: '.pf-invoice-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  watch(tableRefreshId, () => {
    loadList();
  });

  function search() {
    const params: Record<string, unknown> = {};
    if (queryContractId.value) params.contractId = queryContractId.value;
    if (queryStatus.value) params.invoiceStatus = queryStatus.value;
    setLoadListParams(params);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function reset() {
    queryContractId.value = '';
    queryStatus.value = '';
    search();
  }

  onMounted(() => {
    loadContracts();
    search();
  });
</script>
