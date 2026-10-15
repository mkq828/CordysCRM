<template>
  <div class="flex min-h-0 flex-1 flex-col gap-[16px]">
    <div class="min-h-0 flex-1">
      <CrmCard hide-footer no-content-padding class="h-full">
        <div class="h-full px-[24px] pt-[24px]">
          <CrmTable
            ref="crmTableRef"
            v-bind="propsRes"
            class="model-settings-table"
            @page-change="propsEvent.pageChange"
            @page-size-change="propsEvent.pageSizeChange"
            @sorter-change="propsEvent.sorterChange"
            @filter-change="propsEvent.filterChange"
            @refresh="searchData"
          >
            <template #tableTop>
              <div class="flex gap-[12px]">
                <n-button v-permission="['SYSTEM_SETTING:ADD']" type="primary" @click="openAddDrawer">
                  {{ t('system.business.modelSettings.addModel') }}
                </n-button>
                <n-tooltip trigger="hover" :disabled="!isRouteStrategyDisabled">
                  <template #trigger>
                    <span>
                      <n-button
                        class="n-btn-outline-primary"
                        type="primary"
                        ghost
                        :disabled="isRouteStrategyDisabled"
                        @click="openRouteModal"
                      >
                        <template #icon>
                          <CrmIcon type="iconicon_set_up" :size="16" />
                        </template>
                        {{ t('system.business.modelSettings.routeStrategy') }}
                      </n-button>
                    </span>
                  </template>
                  {{ t('system.business.modelSettings.addModelBeforeRoute') }}
                </n-tooltip>
              </div>
            </template>
            <template #actionRight>
              <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" @search="searchData" />
            </template>
          </CrmTable>
        </div>
      </CrmCard>
    </div>

    <div class="min-h-0 flex-1">
      <CrmCard
        hide-footer
        no-content-padding
        class="h-full"
        :title="t('system.business.modelSettings.featureUsageTitle')"
      >
        <div class="flex h-full flex-col px-[24px] pt-[16px]">
          <div class="mb-[12px] flex items-center gap-[12px]">
            <span class="shrink-0 text-[13px]">{{ t('system.business.modelSettings.rerankModel') }}</span>
            <n-input
              v-model:value="rerankModel"
              class="!w-[220px]"
              clearable
              :disabled="!canUpdateModelSettings"
              :placeholder="t('system.business.modelSettings.rerankModelPlaceholder')"
            />
            <n-button
              size="small"
              type="primary"
              :disabled="!canUpdateModelSettings"
              :loading="savingRerank"
              @click="saveRerank"
            >
              {{ t('common.save') }}
            </n-button>
            <span class="text-xs text-orange-500">{{ t('system.business.modelSettings.rerankModelTip') }}</span>
          </div>
          <n-tabs v-model:value="activeRouteTab" type="line" size="small" class="route-tabs">
            <n-tab v-for="tab in routeTabs" :key="tab.key" :name="tab.key">
              {{ tab.label }}
            </n-tab>
          </n-tabs>
          <n-data-table
            :columns="featureColumns"
            :data="activeTabRows"
            flex-height
            class="flex-1"
            size="small"
            :bordered="false"
            :single-line="false"
          >
            <template #empty>
              <n-empty :description="t('common.noData')" :show-icon="false" />
            </template>
          </n-data-table>
        </div>
      </CrmCard>
    </div>
  </div>

  <ModelSettingsDrawer v-model:show="drawerVisible" :model="editingModel" @saved="handleModelSaved" />

  <RouteStrategyModal v-model:show="routeModalVisible" :readonly="!canUpdateModelSettings" @saved="loadFeatureUsage" />
</template>

<script setup lang="ts">
  import { computed, h, onMounted, ref, watch } from 'vue';
  import { NButton, NDataTable, NEmpty, NInput, NSwitch, NTab, NTabs, NTooltip, useMessage } from 'naive-ui';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit, formatThousands } from '@lib/shared/method';
  import type { AiModelFeatureUsage, AiModelFeatureUsageModel, AiModelItem } from '@lib/shared/models/system/aiModel';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import ModelSettingsDrawer from './modelSettingsDrawer.vue';
  import RouteStrategyModal from './routeStrategyModal.vue';

  import {
    deleteAiModel,
    getAiKnowledgeConfig,
    getAiModelDetail,
    getAiModelFeatureUsage,
    getAiModelList,
    saveAiKnowledgeConfig,
    updateAiModelStatus,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  import type { DataTableColumns } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const licenseStore = useLicenseStore();

  const keyword = ref('');
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const tableRefreshId = ref(0);
  const canUpdateModelSettings = computed(() => hasAnyPermission(['SYSTEM_SETTING:UPDATE']));

  function formatGlobalDailyLimit(value: number | null | undefined): string {
    return value === null || value === undefined ? t('common.unlimited') : formatThousands(value);
  }

  async function toggleModelStatus(row: AiModelItem, enable: boolean) {
    try {
      await updateAiModelStatus({ id: row.id });
      Message.success(enable ? t('common.enableSuccess') : t('common.disableSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const routeModalVisible = ref(false);
  function openRouteModal() {
    routeModalVisible.value = true;
  }

  const tableRemoveRefreshId = ref('');
  function handleDelete(row: AiModelItem) {
    openModal({
      type: 'error',
      title: t('system.business.modelSettings.deleteConfirmTitle', { name: characterLimit(row.displayName) }),
      content: t('system.business.modelSettings.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiModel(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRemoveRefreshId.value = row.id;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const drawerVisible = ref(false);
  const editingModel = ref<AiModelItem | null>(null);

  function openAddDrawer() {
    editingModel.value = null;
    drawerVisible.value = true;
  }

  async function handleEdit(row: AiModelItem) {
    try {
      editingModel.value = await getAiModelDetail(row.id);
      drawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleActionSelect(row: AiModelItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn<AiModelItem>[] = [
    {
      fixed: 'left',
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (_row, index) => index + 1,
    },
    {
      fixed: 'left',
      columnSelectorDisabled: true,
      title: t('system.business.modelSettings.modelName'),
      ellipsis: {
        tooltip: true,
      },
      key: 'displayName',
      width: 120,
    },
    {
      title: t('system.business.modelSettings.modelIdColumn'),
      key: 'modelName',
      ellipsis: {
        tooltip: true,
      },
      width: 120,
      render: (row) => row.modelName || '-',
    },
    {
      title: t('system.business.modelSettings.provider'),
      ellipsis: {
        tooltip: true,
      },
      key: 'provider',
      width: 120,
    },

    {
      title: t('common.status'),
      key: 'enable',
      filter: true,
      filterOptions: [
        {
          value: true,
          label: t('common.enable'),
        },
        {
          value: false,
          label: t('common.disable'),
        },
      ],
      width: 100,
      render: (row) =>
        h(NSwitch, {
          'value': row.enable,
          'disabled': !hasAnyPermission(['SYSTEM_SETTING:UPDATE']),
          'rubberBand': false,
          'onUpdate:value': (enable: boolean) => {
            if (!hasAnyPermission(['SYSTEM_SETTING:UPDATE'])) return;
            toggleModelStatus(row, enable);
          },
        }),
    },
    {
      title: t('system.business.modelSettings.globalDailyLimitColumn'),
      key: 'globalDailyLimit',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => formatGlobalDailyLimit(row.globalDailyLimit),
    },
    {
      title: t('system.business.modelSettings.todayUsage'),
      key: 'dailyTotal',
      width: 100,
      render: (row) => formatThousands(row.dailyTotal ?? 0),
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 150,
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
      width: 120,
      render: (row: AiModelItem) => {
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
      width: 120,
      sortOrder: false,
      sorter: true,
      render: (row: AiModelItem) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: [
            { label: t('common.edit'), key: 'edit', permission: ['SYSTEM_SETTING:UPDATE'] },
            { label: t('common.delete'), key: 'delete', permission: ['SYSTEM_SETTING:DELETE'] },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<AiModelItem>(getAiModelList, {
    columns,
    tableKey: TableKeyEnum.SYSTEM_MODEL_SETTINGS,
    permission: ['SYSTEM_SETTING:UPDATE', 'SYSTEM_SETTING:DELETE'],
    showSetting: true,
    containerClass: '.model-settings-table',
  });
  const isRouteStrategyDisabled = computed(() => (propsRes.value.crmPagination?.itemCount ?? 0) === 0);

  function searchData(val?: string, refreshId?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList(false, refreshId);
    if (!refreshId) {
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }

  function handleModelSaved(refreshId?: string) {
    searchData(keyword.value, refreshId);
  }

  function removeItemFromList(id: string) {
    propsRes.value.data = propsRes.value.data.filter((item) => item.id !== id) as typeof propsRes.value.data;
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: (propsRes.value.crmPagination?.itemCount ?? 1) - 1,
    };
  }

  watch(
    () => tableRefreshId.value,
    () => {
      crmTableRef.value?.clearCheckedRowKeys();
      searchData(keyword.value);
    }
  );

  watch(
    () => tableRemoveRefreshId.value,
    (val) => {
      if (val) {
        removeItemFromList(val);
      }
    }
  );

  // 「模型应用场景」只读矩阵：每个 AI 功能 → 当前解析到的模型（首选 + 备用链）
  const featureUsage = ref<AiModelFeatureUsage[]>([]);

  interface FeatureRow {
    featureName: string;
    models: AiModelFeatureUsageModel[];
    remark: string;
    noRoute: boolean;
  }

  type RouteTabKey = 'CHAT' | 'TASK' | 'IMAGE' | 'VIDEO' | 'TTS' | 'VOICE_CLONE' | 'OTHER';

  const routeTabs = computed<{ key: RouteTabKey; label: string }[]>(() => [
    { key: 'CHAT', label: t('system.business.modelSettings.routeChat') },
    { key: 'TASK', label: t('system.business.modelSettings.routeTask') },
    { key: 'IMAGE', label: t('system.business.modelSettings.routeImage') },
    { key: 'VIDEO', label: t('system.business.modelSettings.routeVideo') },
    { key: 'TTS', label: t('system.business.modelSettings.routeTts') },
    { key: 'VOICE_CLONE', label: t('system.business.modelSettings.routeVoiceClone') },
    { key: 'OTHER', label: t('system.business.modelSettings.routeOther') },
  ]);

  const activeRouteTab = ref<RouteTabKey>('CHAT');

  // 按用途路由分组：同一功能多用途时会在多个 tab 各出现一次；无模型功能归入「其他」
  const rowsByTab = computed<Record<string, FeatureRow[]>>(() => {
    const map: Record<string, FeatureRow[]> = {
      CHAT: [],
      TASK: [],
      IMAGE: [],
      VIDEO: [],
      TTS: [],
      VOICE_CLONE: [],
      OTHER: [],
    };
    featureUsage.value.forEach((feature) => {
      const remark = feature.remark || '';
      if (!feature.usages?.length) {
        map.OTHER.push({ featureName: feature.featureName, models: [], remark, noRoute: true });
        return;
      }
      feature.usages.forEach((usage) => {
        const bucket = map[usage.route];
        if (bucket) {
          bucket.push({ featureName: feature.featureName, models: usage.models || [], remark, noRoute: false });
        }
      });
    });
    return map;
  });

  const activeTabRows = computed<FeatureRow[]>(() => rowsByTab.value[activeRouteTab.value] || []);

  function renderModels(row: FeatureRow) {
    const { models, noRoute } = row;
    if (!models.length) {
      return h(
        'span',
        { class: 'text-[var(--text-n4)]' },
        noRoute ? '—' : t('system.business.modelSettings.notConfigured')
      );
    }
    const [first, ...rest] = models;
    const children = [
      h('span', { class: 'font-semibold text-[var(--text-n1)]' }, first.displayName || first.modelName || '-'),
    ];
    if (rest.length) {
      children.push(
        h(
          'span',
          { class: 'text-[var(--text-n4)]' },
          `${t('system.business.modelSettings.backupPrefix')}${rest
            .map((m) => m.displayName || m.modelName)
            .join('、')}`
        )
      );
    }
    return h('div', { class: 'flex flex-col gap-[2px]' }, children);
  }

  const featureColumns: DataTableColumns<FeatureRow> = [
    {
      title: t('system.business.modelSettings.featureName'),
      key: 'featureName',
      width: 180,
      ellipsis: { tooltip: true },
    },
    {
      title: t('system.business.modelSettings.currentModel'),
      key: 'models',
      render: (row) => renderModels(row),
    },
    {
      title: t('system.business.modelSettings.provider'),
      key: 'provider',
      width: 120,
      render: (row) => row.models[0]?.provider || '—',
    },
    {
      title: t('system.business.modelSettings.remark'),
      key: 'remark',
      width: 180,
      ellipsis: { tooltip: true },
      render: (row) => row.remark || '—',
    },
  ];

  async function loadFeatureUsage() {
    try {
      featureUsage.value = await getAiModelFeatureUsage();
    } catch (error) {
      // 读取失败时静默降级，矩阵留空，不打断模型列表主流程
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // rerank 精排模型名：知识库问答检索精排用，凭证复用阿里云 embedding 模型，未配置走默认 qwen3-rerank
  const rerankModel = ref('');
  const savingRerank = ref(false);

  async function loadRerankModel() {
    try {
      const config = await getAiKnowledgeConfig();
      if (config?.rerankModel) {
        rerankModel.value = config.rerankModel;
      }
    } catch (error) {
      // 读取失败时保留空值，不打断模型设置主流程
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function saveRerank() {
    const value = rerankModel.value.trim();
    if (!value) {
      Message.warning(t('common.notNull', { value: t('system.business.modelSettings.rerankModel') }));
      return;
    }
    savingRerank.value = true;
    try {
      await saveAiKnowledgeConfig({ rerankModel: value });
      Message.success(t('common.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      savingRerank.value = false;
    }
  }

  onMounted(() => {
    loadList();
    loadFeatureUsage();
    loadRerankModel();
  });
</script>
