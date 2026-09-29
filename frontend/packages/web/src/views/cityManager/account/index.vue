<template>
  <div class="cm-account-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="keyword"
          :placeholder="t('cityManager.searchPlaceholder')"
          clearable
          class="w-[280px]"
          @keydown.enter="search"
        />
        <CrmSelect
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('cityManager.status')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
        <div class="flex-1" />
        <n-button type="primary" ghost @click="openAdd">{{ t('cityManager.add') }}</n-button>
        <n-button type="primary" ghost @click="openAssign">{{ t('cityManager.assign') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="cm-account-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>

    <!-- 新增经理 -->
    <n-modal
      v-model:show="showAdd"
      preset="card"
      :title="t('cityManager.add')"
      class="w-[480px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.name') }}</span
          >
          <n-input v-model:value="addForm.name" :placeholder="t('cityManager.namePlaceholder')" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.phone') }}</span
          >
          <n-input v-model:value="addForm.phone" :placeholder="t('cityManager.phonePlaceholder')" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('cityManager.password') }}</span>
          <n-input
            v-model:value="addForm.password"
            type="password"
            show-password-on="click"
            :placeholder="t('cityManager.passwordPlaceholder')"
            class="flex-1"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showAdd = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="addLoading" @click="confirmAdd">{{ t('common.confirm') }}</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 离职二次分配 -->
    <n-modal
      v-model:show="showReassign"
      preset="card"
      :title="t('cityManager.reassignTitle')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.fromManager') }}</span
          >
          <CrmSelect
            v-model:value="reassignForm.fromManagerId"
            :options="managerOptions"
            filterable
            class="flex-1"
            @update:value="onFromChange"
          />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.toManager') }}</span
          >
          <CrmSelect
            v-model:value="reassignForm.toManagerId"
            :options="enabledManagerOptions"
            filterable
            class="flex-1"
          />
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.transferOrgs') }}</span
          >
          <div class="flex flex-1 flex-col gap-[8px]">
            <CrmSelect
              v-model:value="reassignForm.organizationIds"
              :options="transferOrgOptions"
              multiple
              filterable
              class="flex-1"
              :placeholder="t('cityManager.transferOrgs')"
            />
            <div v-if="!transferOrgOptions.length" class="text-[13px] text-[var(--text-n4)]">
              {{ t('cityManager.noOrgs') }}
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showReassign = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="reassignLoading" @click="confirmReassign">
            {{ t('common.confirm') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- 分配租户归属 -->
    <n-modal
      v-model:show="showAssign"
      preset="card"
      :title="t('cityManager.assignTitle')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.assignOrg') }}</span
          >
          <CrmSelect
            v-model:value="assignForm.organizationId"
            :options="assignOrgOptions"
            filterable
            class="flex-1"
            :placeholder="t('cityManager.assignOrg')"
          />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.signManager') }}</span
          >
          <CrmSelect
            v-model:value="assignForm.signManagerId"
            :options="assignManagerOptions"
            filterable
            class="flex-1"
          />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('cityManager.followManager') }}</span
          >
          <CrmSelect
            v-model:value="assignForm.followManagerId"
            :options="assignManagerOptions"
            filterable
            class="flex-1"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showAssign = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="assignLoading" @click="confirmAssign">
            {{ t('common.confirm') }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onMounted, reactive, ref, watch } from 'vue';
  import { NButton, NInput, NModal, NTag, useMessage } from 'naive-ui';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CityManagerItem, CityManagerOrgItem, CityManagerStatus } from '@lib/shared/models/system/cityManager';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSelect from '@/components/pure/crm-select/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    cityManagerAdd,
    cityManagerAssign,
    cityManagerDisable,
    cityManagerOrgOptions,
    cityManagerPageList,
    cityManagerReassign,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const keyword = ref('');
  const queryStatus = ref<CityManagerStatus | ''>('');
  const tableRefreshId = ref(0);

  const statusTagMap: Record<CityManagerStatus, 'success' | 'default'> = {
    ENABLED: 'success',
    DISABLED: 'default',
  };

  const statusOptions = computed(() => [
    { label: t('common.all'), value: '' },
    { label: t('cityManager.status.ENABLED'), value: 'ENABLED' },
    { label: t('cityManager.status.DISABLED'), value: 'DISABLED' },
  ]);

  function statusLabel(status: CityManagerStatus) {
    return t(`cityManager.status.${status}`);
  }

  // 下拉数据源：全量经理 + 全量租户（排除默认组织）
  const allManagers = ref<CityManagerItem[]>([]);
  const allOrgs = ref<CityManagerOrgItem[]>([]);

  // 二次分配
  const showReassign = ref(false);
  const reassignLoading = ref(false);
  const reassignForm = reactive({ fromManagerId: '', toManagerId: '', organizationIds: [] as string[] });

  // 分配租户归属
  const showAssign = ref(false);
  const assignLoading = ref(false);
  const assignForm = reactive({ organizationId: '', signManagerId: '', followManagerId: '' });

  const managerOptions = computed(() =>
    allManagers.value.map((m) => ({ label: `${m.name}（${m.phone}）`, value: m.id }))
  );

  // 目标经理只能选在职且非本人的经理
  const enabledManagerOptions = computed(() =>
    allManagers.value
      .filter((m) => m.status === 'ENABLED' && m.id !== reassignForm.fromManagerId)
      .map((m) => ({ label: `${m.name}（${m.phone}）`, value: m.id }))
  );

  // 分配归属可选经理：仅在职
  const assignManagerOptions = computed(() =>
    allManagers.value
      .filter((m) => m.status === 'ENABLED')
      .map((m) => ({ label: `${m.name}（${m.phone}）`, value: m.id }))
  );

  // 分配归属可选租户（已分配签约经理的加提示，避免误覆盖「永久」归属）
  const assignOrgOptions = computed(() =>
    allOrgs.value.map((o) => ({
      label: o.signManagerId ? `${o.name}（${t('cityManager.assigned')}）` : o.name,
      value: o.id,
    }))
  );

  // 待转移租户 = 当前「原跟进经理」名下跟进中的租户
  const transferOrgOptions = computed(() =>
    allOrgs.value
      .filter((o) => o.followManagerId === reassignForm.fromManagerId)
      .map((o) => ({ label: o.name, value: o.id }))
  );

  async function loadOptions() {
    try {
      const [managers, orgs] = await Promise.all([
        cityManagerPageList({ current: 1, pageSize: 500 }),
        cityManagerOrgOptions(),
      ]);
      allManagers.value = managers.list || [];
      allOrgs.value = (orgs as CityManagerOrgItem[]) || [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  // 新增
  const showAdd = ref(false);
  const addLoading = ref(false);
  const addForm = reactive({ name: '', phone: '', password: '' });

  function openAdd() {
    Object.assign(addForm, { name: '', phone: '', password: '' });
    showAdd.value = true;
  }

  async function confirmAdd() {
    if (!addForm.name.trim()) {
      Message.warning(t('cityManager.nameRequired'));
      return;
    }
    if (!addForm.phone.trim()) {
      Message.warning(t('cityManager.phoneRequired'));
      return;
    }
    addLoading.value = true;
    try {
      await cityManagerAdd({
        name: addForm.name.trim(),
        phone: addForm.phone.trim(),
        password: addForm.password || undefined,
      });
      Message.success(t('cityManager.addSuccess'));
      showAdd.value = false;
      tableRefreshId.value += 1;
      loadOptions();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      addLoading.value = false;
    }
  }

  // 离职禁用
  function handleDisable(row: CityManagerItem) {
    openModal({
      type: 'warning',
      title: t('cityManager.disableTitle'),
      content: t('cityManager.disableContent', { name: row.name }),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await cityManagerDisable(row.id);
          Message.success(t('cityManager.disableSuccess'));
          tableRefreshId.value += 1;
          loadOptions();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 二次分配
  function openReassign(row: CityManagerItem) {
    reassignForm.fromManagerId = row.id;
    reassignForm.toManagerId = '';
    reassignForm.organizationIds = allOrgs.value.filter((o) => o.followManagerId === row.id).map((o) => o.id);
    showReassign.value = true;
  }

  function onFromChange() {
    reassignForm.toManagerId = '';
    reassignForm.organizationIds = allOrgs.value
      .filter((o) => o.followManagerId === reassignForm.fromManagerId)
      .map((o) => o.id);
  }

  async function confirmReassign() {
    if (!reassignForm.toManagerId) {
      Message.warning(t('cityManager.toManagerRequired'));
      return;
    }
    if (!reassignForm.organizationIds.length) {
      Message.warning(t('cityManager.orgsRequired'));
      return;
    }
    reassignLoading.value = true;
    try {
      await cityManagerReassign({
        fromManagerId: reassignForm.fromManagerId,
        toManagerId: reassignForm.toManagerId,
        organizationIds: reassignForm.organizationIds,
      });
      Message.success(t('cityManager.reassignSuccess'));
      showReassign.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      reassignLoading.value = false;
    }
  }

  // 分配租户归属
  function openAssign() {
    Object.assign(assignForm, { organizationId: '', signManagerId: '', followManagerId: '' });
    showAssign.value = true;
  }

  async function confirmAssign() {
    if (!assignForm.organizationId) {
      Message.warning(t('cityManager.assignOrgRequired'));
      return;
    }
    if (!assignForm.signManagerId) {
      Message.warning(t('cityManager.signManagerRequired'));
      return;
    }
    if (!assignForm.followManagerId) {
      Message.warning(t('cityManager.followManagerRequired'));
      return;
    }
    assignLoading.value = true;
    try {
      await cityManagerAssign({
        organizationId: assignForm.organizationId,
        signManagerId: assignForm.signManagerId,
        followManagerId: assignForm.followManagerId,
      });
      Message.success(t('cityManager.assignSuccess'));
      showAssign.value = false;
      tableRefreshId.value += 1;
      loadOptions();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      assignLoading.value = false;
    }
  }

  function buildActions(row: CityManagerItem): ActionsItem[] {
    const list: ActionsItem[] = [{ label: t('cityManager.reassign'), key: 'reassign' }];
    if (row.status === 'ENABLED') {
      list.push({ label: t('cityManager.disable'), key: 'disable', danger: true });
    }
    return list;
  }

  function handleActionSelect(row: CityManagerItem, key: string) {
    switch (key) {
      case 'disable':
        handleDisable(row);
        break;
      case 'reassign':
        openReassign(row);
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
      title: t('cityManager.name'),
      key: 'name',
      width: 140,
      ellipsis: { tooltip: true },
    },
    {
      title: t('cityManager.phone'),
      key: 'phone',
      width: 140,
    },
    {
      title: t('cityManager.status'),
      key: 'status',
      width: 100,
      render: (row: CityManagerItem) =>
        h(
          NTag,
          { type: statusTagMap[row.status] || 'default', size: 'small' },
          { default: () => statusLabel(row.status) }
        ),
    },
    {
      title: t('cityManager.signedCount'),
      key: 'signedCount',
      width: 110,
      align: 'right',
    },
    {
      title: t('cityManager.followCount'),
      key: 'followCount',
      width: 110,
      align: 'right',
    },
    {
      title: t('cityManager.createTime'),
      key: 'createTime',
      width: 170,
      render: (row: CityManagerItem) => row.createTime || '-',
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 180,
      fixed: 'right',
      render: (row: CityManagerItem) =>
        h(CrmOperationButton, {
          groupList: buildActions(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<CityManagerItem>(cityManagerPageList, {
    tableKey: TableKeyEnum.CITY_MANAGER_TABLE,
    columns,
    showSetting: true,
    containerClass: '.cm-account-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  watch(tableRefreshId, () => {
    loadList();
  });

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
    loadOptions();
    search();
  });
</script>
