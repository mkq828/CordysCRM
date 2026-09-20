<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-bank-account-list-table"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="actionConfig"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @refresh="searchData"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button v-permission="['BANK_ACCOUNT:ADD']" type="primary" @click="handleNewClick">
          {{ t('contract.bankAccount.add') }}
        </n-button>
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </template>
  </CrmTable>
  <bankAccountDrawer
    v-model:visible="bankAccountDrawerVisible"
    :source-id="activeSourceId"
    @load="() => searchData()"
    @cancel="handleCancel"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, NImage, NTag, useMessage } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { BankAccountTypeEnum } from '@lib/shared/enums/bankAccountEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { BankAccountItem } from '@lib/shared/models/contract';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import bankAccountDrawer from './bankAccountDrawer.vue';

  import { deleteBankAccount, getBankAccountList } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  function typeLabel(type: string) {
    switch (type) {
      case BankAccountTypeEnum.WECHAT:
        return t('contract.bankAccount.typeWechat');
      case BankAccountTypeEnum.ALIPAY:
        return t('contract.bankAccount.typeAlipay');
      default:
        return t('contract.bankAccount.typeBankCard');
    }
  }

  function typeTagType(type: string) {
    if (type === BankAccountTypeEnum.WECHAT) return 'success';
    if (type === BankAccountTypeEnum.ALIPAY) return 'info';
    return 'default';
  }

  function qrcodePreviewUrl(qrcode: string) {
    return `${PreviewPictureUrl}/${qrcode}?userId=${userStore.userInfo.id}`;
  }

  const keyword = ref('');
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeSourceId = ref('');
  const { openModal } = useModal();
  const tableRefreshId = ref(0);

  const bankAccountDrawerVisible = ref(false);
  function handleNewClick() {
    activeSourceId.value = '';
    bankAccountDrawerVisible.value = true;
  }
  function handleEdit(id: string) {
    activeSourceId.value = id;
    bankAccountDrawerVisible.value = true;
  }

  function deleteHandler(row: BankAccountItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('contract.bankAccount.deleteContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteBankAccount(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleCancel() {
    activeSourceId.value = '';
  }

  function handleActionSelect(row: BankAccountItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'delete':
        deleteHandler(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
      fixed: 'left',
      width: 46,
    },
    {
      fixed: 'left',
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (row: BankAccountItem, rowIndex: number) => rowIndex + 1,
    },
    {
      title: t('contract.bankAccount.name'),
      key: 'name',
      sortOrder: false,
      sorter: true,
      width: 200,
      fixed: 'left',
      columnSelectorDisabled: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('contract.bankAccount.type'),
      key: 'type',
      sortOrder: false,
      sorter: true,
      width: 120,
      render: (row: BankAccountItem) =>
        h(NTag, { type: typeTagType(row.type), size: 'small' }, { default: () => typeLabel(row.type) }),
    },
    {
      title: t('contract.bankAccount.openingBank'),
      key: 'openingBank',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.bankAccount.bankAccount'),
      key: 'bankAccount',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.bankAccount.accountHolder'),
      key: 'accountHolder',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 160,
    },
    {
      title: t('contract.bankAccount.qrcode'),
      key: 'qrcode',
      width: 100,
      render: (row: BankAccountItem) =>
        row.qrcode
          ? h(NImage, { 'src': qrcodePreviewUrl(row.qrcode), 'width': 48, 'height': 48, 'object-fit': 'cover' })
          : h('span', {}, '-'),
    },
    {
      title: t('common.remark'),
      key: 'remark',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 200,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      sortOrder: false,
      sorter: true,
      width: 200,
      render: (row: BankAccountItem) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 200,
      sortOrder: false,
      sorter: true,
      render: (row: BankAccountItem) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: BankAccountItem) =>
        h(CrmOperationButton, {
          groupList: [
            { label: t('common.edit'), key: 'edit', permission: ['BANK_ACCOUNT:UPDATE'] },
            { label: t('common.delete'), key: 'delete', permission: ['BANK_ACCOUNT:DELETE'] },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const actionConfig: BatchActionConfig = {
    baseAction: [],
  };

  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTable(getBankAccountList, {
    tableKey: TableKeyEnum.CONTRACT_BANK_ACCOUNT,
    showSetting: true,
    columns,
    containerClass: '.crm-bank-account-list-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const isAdvancedSearchMode = ref(false);

  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  const filterConfigList = computed<FilterFormItem[]>(() => [
    {
      title: t('contract.bankAccount.name'),
      dataIndex: 'name',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.bankAccount.type'),
      dataIndex: 'type',
      type: FieldTypeEnum.SELECT,
      selectProps: {
        options: [
          { label: t('contract.bankAccount.typeBankCard'), value: BankAccountTypeEnum.BANK_CARD },
          { label: t('contract.bankAccount.typeWechat'), value: BankAccountTypeEnum.WECHAT },
          { label: t('contract.bankAccount.typeAlipay'), value: BankAccountTypeEnum.ALIPAY },
        ],
      },
    },
    {
      title: t('contract.bankAccount.openingBank'),
      dataIndex: 'openingBank',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.bankAccount.bankAccount'),
      dataIndex: 'bankAccount',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.bankAccount.accountHolder'),
      dataIndex: 'accountHolder',
      type: FieldTypeEnum.INPUT,
    },
    ...baseFilterConfigList,
  ]);

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );

  onBeforeMount(() => {
    searchData();
  });
</script>

<style scoped lang="less"></style>
