<template>
  <div class="edition-page flex h-full flex-col gap-[16px] overflow-hidden">
    <!-- 版本列表 -->
    <CrmCard :title="t('edition.editionList')" hide-footer auto-height>
      <template #header-extra>
        <n-button v-permission="['PAID_USER:UPDATE']" type="primary" @click="openEditionEdit()">
          {{ t('edition.addEdition') }}
        </n-button>
      </template>
      <n-data-table
        :columns="editionColumns"
        :data="editions"
        :loading="editionLoading"
        :bordered="false"
        :single-line="false"
        size="small"
        :scroll-x="960"
      />
    </CrmCard>

    <!-- 功能列表 -->
    <CrmCard :title="t('edition.featureList')" hide-footer auto-height>
      <template #header-extra>
        <n-button v-permission="['PAID_USER:UPDATE']" type="primary" @click="openFeatureEdit()">
          {{ t('edition.addFeature') }}
        </n-button>
      </template>
      <n-data-table
        :columns="featureColumns"
        :data="features"
        :loading="featureLoading"
        :bordered="false"
        :single-line="false"
        size="small"
        :scroll-x="640"
      />
    </CrmCard>

    <!-- 版本新增/编辑弹窗 -->
    <n-modal
      v-model:show="showEdition"
      preset="card"
      :title="editionForm.id ? t('edition.editEdition') : t('edition.addEdition')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <n-scrollbar style="max-height: 60vh">
        <div class="flex flex-col gap-[16px]">
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.code') }}</span>
            <n-input v-model:value="editionForm.code" class="flex-1" :disabled="!!editionForm.id" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.name') }}</span>
            <n-input v-model:value="editionForm.name" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.yearPrice') }}</span>
            <n-input-number v-model:value="editionForm.yearPrice" :min="0" :precision="2" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.firstYearPrice') }}</span>
            <n-input-number v-model:value="editionForm.firstYearPrice" :min="0" :precision="2" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.softLimit') }}</span>
            <n-input-number v-model:value="editionForm.softLimit" :min="1" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.validityDays') }}</span>
            <n-input-number v-model:value="editionForm.validityDays" :min="1" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.sort') }}</span>
            <n-input-number v-model:value="editionForm.sort" :min="0" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('edition.status') }}</span>
            <n-switch v-model:value="editionForm.status" :checked-value="1" :unchecked-value="0">
              <template #checked>{{ t('edition.status.enabled') }}</template>
              <template #unchecked>{{ t('edition.status.disabled') }}</template>
            </n-switch>
          </div>
          <div class="flex items-start gap-[12px]">
            <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('edition.features') }}</span>
            <n-checkbox-group v-model:value="editionForm.featureIds" class="flex-1">
              <n-space vertical>
                <n-checkbox v-for="f in features" :key="f.id" :value="f.id" :label="f.name" />
              </n-space>
            </n-checkbox-group>
          </div>
        </div>
      </n-scrollbar>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showEdition = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="editionSaving" @click="confirmEdition">{{ t('common.confirm') }}</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 功能新增/编辑弹窗 -->
    <n-modal
      v-model:show="showFeature"
      preset="card"
      :title="featureForm.id ? t('edition.editFeature') : t('edition.addFeature')"
      class="w-[480px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[90px] shrink-0 text-right">{{ t('edition.featureCode') }}</span>
          <n-input v-model:value="featureForm.featureCode" class="flex-1" :disabled="!!featureForm.id" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[90px] shrink-0 text-right">{{ t('edition.featureName') }}</span>
          <n-input v-model:value="featureForm.name" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[90px] shrink-0 text-right">{{ t('edition.featureCategory') }}</span>
          <n-input v-model:value="featureForm.category" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[90px] shrink-0 text-right">{{ t('edition.featureEnable') }}</span>
          <n-switch v-model:value="featureForm.enable" />
        </div>
        <div class="ml-[102px] text-xs text-orange-500">{{ t('edition.featureEnableTip') }}</div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showFeature = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="featureSaving" @click="confirmFeature">{{ t('common.confirm') }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { h, onMounted, ref } from 'vue';
  import {
    NButton,
    NCheckbox,
    NCheckboxGroup,
    NDataTable,
    NInput,
    NInputNumber,
    NModal,
    NScrollbar,
    NSpace,
    NSwitch,
    NTag,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { Edition, Feature } from '@lib/shared/models/system/edition';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import {
    deleteEdition,
    editionFeatureDelete,
    editionFeatureIdsByEditionId,
    editionFeatureList,
    editionFeatureSave,
    editionList,
    saveEdition,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import type { DataTableColumns } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const editions = ref<Edition[]>([]);
  const features = ref<Feature[]>([]);
  const editionLoading = ref(false);
  const featureLoading = ref(false);

  async function loadData() {
    editionLoading.value = true;
    featureLoading.value = true;
    try {
      const [editionRes, featureRes] = await Promise.all([editionList(), editionFeatureList()]);
      editions.value = editionRes;
      features.value = featureRes;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      editionLoading.value = false;
      featureLoading.value = false;
    }
  }

  function formatPrice(v?: number) {
    return v == null ? '-' : `¥${v}`;
  }

  // 版本新增/编辑
  const showEdition = ref(false);
  const editionSaving = ref(false);
  const editionForm = ref<{
    id: string;
    code: string;
    name: string;
    yearPrice: number | null;
    firstYearPrice: number | null;
    softLimit: number | null;
    validityDays: number | null;
    sort: number | null;
    status: number;
    featureIds: string[];
  }>({
    id: '',
    code: '',
    name: '',
    yearPrice: null,
    firstYearPrice: null,
    softLimit: null,
    validityDays: null,
    sort: null,
    status: 1,
    featureIds: [],
  });

  async function openEditionEdit(row?: Edition) {
    editionForm.value = {
      id: row?.id || '',
      code: row?.code || '',
      name: row?.name || '',
      yearPrice: row?.yearPrice ?? null,
      firstYearPrice: row?.firstYearPrice ?? null,
      softLimit: row?.softLimit ?? null,
      validityDays: row?.validityDays ?? null,
      sort: row?.sort ?? null,
      status: row?.status ?? 1,
      featureIds: [],
    };
    if (row?.id) {
      try {
        editionForm.value.featureIds = await editionFeatureIdsByEditionId(row.id);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
    }
    showEdition.value = true;
  }

  async function confirmEdition() {
    if (!editionForm.value.code.trim()) {
      Message.warning(t('edition.codeRequired'));
      return;
    }
    if (!editionForm.value.name.trim()) {
      Message.warning(t('edition.nameRequired'));
      return;
    }
    editionSaving.value = true;
    try {
      await saveEdition({
        id: editionForm.value.id || undefined,
        code: editionForm.value.code.trim(),
        name: editionForm.value.name.trim(),
        yearPrice: editionForm.value.yearPrice ?? undefined,
        firstYearPrice: editionForm.value.firstYearPrice ?? undefined,
        softLimit: editionForm.value.softLimit ?? undefined,
        validityDays: editionForm.value.validityDays ?? undefined,
        sort: editionForm.value.sort ?? undefined,
        status: editionForm.value.status,
        featureIds: editionForm.value.featureIds,
      });
      Message.success(t('edition.saveSuccess'));
      showEdition.value = false;
      loadData();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      editionSaving.value = false;
    }
  }

  function handleDeleteEdition(row: Edition) {
    openModal({
      type: 'warning',
      title: t('edition.deleteTip'),
      content: t('edition.deleteTipContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteEdition(row.id);
          Message.success(t('edition.deleteSuccess'));
          loadData();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 功能新增/编辑
  const showFeature = ref(false);
  const featureSaving = ref(false);
  const featureForm = ref<{ id: string; featureCode: string; name: string; category: string; enable: boolean }>({
    id: '',
    featureCode: '',
    name: '',
    category: '',
    enable: true,
  });

  function openFeatureEdit(row?: Feature) {
    featureForm.value = {
      id: row?.id || '',
      featureCode: row?.featureCode || '',
      name: row?.name || '',
      category: row?.category || '',
      enable: row?.enable ?? true,
    };
    showFeature.value = true;
  }

  async function confirmFeature() {
    if (!featureForm.value.featureCode.trim()) {
      Message.warning(t('edition.featureCodeRequired'));
      return;
    }
    if (!featureForm.value.name.trim()) {
      Message.warning(t('edition.featureNameRequired'));
      return;
    }
    featureSaving.value = true;
    try {
      await editionFeatureSave({
        id: featureForm.value.id || undefined,
        featureCode: featureForm.value.featureCode.trim(),
        name: featureForm.value.name.trim(),
        category: featureForm.value.category.trim(),
        enable: featureForm.value.enable,
      });
      Message.success(t('edition.saveSuccess'));
      showFeature.value = false;
      loadData();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      featureSaving.value = false;
    }
  }

  function handleDeleteFeature(row: Feature) {
    openModal({
      type: 'warning',
      title: t('edition.featureDeleteTip'),
      content: t('edition.featureDeleteTipContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await editionFeatureDelete(row.id);
          Message.success(t('edition.deleteSuccess'));
          loadData();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 版本列表列
  const editionColumns: DataTableColumns<Edition> = [
    { title: t('edition.code'), key: 'code', width: 120 },
    { title: t('edition.name'), key: 'name', width: 120 },
    {
      title: t('edition.yearPrice'),
      key: 'yearPrice',
      width: 110,
      render: (row) => formatPrice(row.yearPrice),
    },
    {
      title: t('edition.firstYearPrice'),
      key: 'firstYearPrice',
      width: 130,
      render: (row) => formatPrice(row.firstYearPrice),
    },
    {
      title: t('edition.softLimit'),
      key: 'softLimit',
      width: 100,
      render: (row) => row.softLimit ?? '-',
    },
    {
      title: t('edition.validityDays'),
      key: 'validityDays',
      width: 110,
      render: (row) => row.validityDays ?? '-',
    },
    {
      title: t('edition.sort'),
      key: 'sort',
      width: 70,
      render: (row) => row.sort ?? '-',
    },
    {
      title: t('edition.status'),
      key: 'status',
      width: 90,
      render: (row) =>
        h(
          NTag,
          { type: row.status === 1 ? 'success' : 'error', size: 'small' },
          { default: () => (row.status === 1 ? t('edition.status.enabled') : t('edition.status.disabled')) }
        ),
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 140,
      render: (row) =>
        h('div', { class: 'flex gap-[8px]' }, [
          h(
            NButton,
            { text: true, type: 'primary', size: 'small', onClick: () => openEditionEdit(row) },
            { default: () => t('common.edit') }
          ),
          h(
            NButton,
            { text: true, type: 'error', size: 'small', onClick: () => handleDeleteEdition(row) },
            { default: () => t('common.delete') }
          ),
        ]),
    },
  ];

  // 功能列表列
  const featureColumns: DataTableColumns<Feature> = [
    { title: t('edition.featureCode'), key: 'featureCode', width: 160 },
    { title: t('edition.featureName'), key: 'name', width: 160 },
    { title: t('edition.featureCategory'), key: 'category', width: 120, render: (row) => row.category || '-' },
    {
      title: t('edition.featureEnable'),
      key: 'enable',
      width: 100,
      render: (row) =>
        h(
          NTag,
          { type: row.enable ? 'success' : 'default', size: 'small' },
          { default: () => (row.enable ? t('edition.status.enabled') : t('edition.status.disabled')) }
        ),
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 140,
      render: (row) =>
        h('div', { class: 'flex gap-[8px]' }, [
          h(
            NButton,
            { text: true, type: 'primary', size: 'small', onClick: () => openFeatureEdit(row) },
            { default: () => t('common.edit') }
          ),
          h(
            NButton,
            { text: true, type: 'error', size: 'small', onClick: () => handleDeleteFeature(row) },
            { default: () => t('common.delete') }
          ),
        ]),
    },
  ];

  onMounted(() => {
    loadData();
  });
</script>

<style lang="less" scoped>
  .edition-page {
    @apply flex h-full flex-col overflow-hidden;
  }
</style>
