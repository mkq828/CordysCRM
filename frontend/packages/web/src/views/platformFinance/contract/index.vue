<template>
  <div class="pf-contract-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <n-input
          v-model:value="keyword"
          :placeholder="t('platformFinance.searchPlaceholder')"
          clearable
          class="w-[280px]"
          @keydown.enter="search"
        />
        <CrmSelect
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('platformFinance.status')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
        <div class="flex-1" />
        <n-button type="primary" ghost @click="openAdd">{{ t('platformFinance.addContract') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="pf-contract-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>

    <!-- 新建/编辑合同 -->
    <n-modal
      v-model:show="showForm"
      preset="card"
      :title="formMode === 'add' ? t('platformFinance.addContract') : t('platformFinance.editContract')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.contractNo') }}</span
          >
          <n-input
            v-model:value="form.contractNo"
            :placeholder="t('platformFinance.contractNoPlaceholder')"
            class="flex-1"
          />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.tenant') }}</span
          >
          <CrmSelect
            v-model:value="form.organizationId"
            :options="orgOptions"
            filterable
            class="flex-1"
            @update:value="onOrgChange"
          />
        </div>
        <div v-if="!isCityManager" class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformFinance.signManager') }}</span>
          <CrmSelect
            v-model:value="form.signManagerId"
            :options="signManagerOptions"
            filterable
            clearable
            class="flex-1"
            :placeholder="t('platformFinance.signManagerPlaceholder')"
          />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.edition') }}</span
          >
          <CrmSelect
            v-model:value="form.editionCode"
            :options="editionOptionsList"
            class="flex-1"
            @update:value="onEditionChange"
          />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.amount') }}</span
          >
          <n-input-number v-model:value="form.amount" :min="0" :precision="2" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.validityDays') }}</span
          >
          <n-input-number v-model:value="form.validityDays" :min="1" :max="36500" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.signType') }}</span
          >
          <CrmSelect v-model:value="form.signType" :options="signTypeOptions" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformFinance.creditCode') }}</span>
          <n-input v-model:value="form.creditCode" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.contactPerson') }}</span
          >
          <n-input v-model:value="form.contactPerson" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.contactPhone') }}</span
          >
          <n-input v-model:value="form.contactPhone" class="flex-1" :maxlength="32" />
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformFinance.address') }}</span>
          <n-input v-model:value="form.address" type="textarea" :rows="2" class="flex-1" />
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right"
            ><span class="text-[var(--error-red)]">*</span>{{ t('platformFinance.attachment') }}</span
          >
          <div class="flex flex-1 flex-col gap-[8px]">
            <div v-for="att in existingAttachments" :key="att.id" class="flex items-center gap-[8px]">
              <n-image
                v-if="isImage(att.type)"
                :src="attachmentUrl(att.id)"
                :width="48"
                :height="48"
                object-fit="cover"
                class="rounded-[4px]"
                preview-disabled
                @click="previewAttachment(att.id)"
              />
              <div v-else class="text-[13px] text-[var(--text-n2)]">{{ att.name }}</div>
              <n-button size="tiny" text type="primary" @click="downloadAttachmentById(att.id, att.name || '')">
                {{ t('common.download') }}
              </n-button>
              <n-button size="tiny" text type="error" @click="removeExistingAttachment(att.id)">
                {{ t('common.delete') }}
              </n-button>
            </div>
            <div v-if="!existingAttachments.length" class="text-[13px] text-[var(--text-n4)]">
              {{ t('platformFinance.noAttachment') }}
            </div>
            <n-upload
              v-model:file-list="attachmentFileList"
              multiple
              :custom-request="uploadAttachment"
              @remove="removeAttachment"
            >
              <n-button>{{ t('platformFinance.uploadAttachment') }}</n-button>
            </n-upload>
          </div>
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformFinance.remark') }}</span>
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
    <n-image-preview v-model:show="preview.show" :src="preview.src" />
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onMounted, reactive, ref, watch } from 'vue';
  import { NButton, NImage, NImagePreview, NInput, NInputNumber, NModal, NTag, NUpload, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import {
    PlatformContractStatusEnum,
    PlatformContractStatusTagMap,
    PlatformSignTypeEnum,
  } from '@lib/shared/enums/platformFinanceEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CityManagerItem } from '@lib/shared/models/system/cityManager';
  import type { Edition } from '@lib/shared/models/system/edition';
  import type {
    PlatformAttachment,
    PlatformContractItem,
    PlatformContractSaveParams,
    PlatformContractStatus,
    PlatformOrgOption,
  } from '@lib/shared/models/system/platformFinance';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSelect from '@/components/pure/crm-select/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    cityManagerPageList,
    downloadAttachment,
    editionOptions,
    platformContractAdd,
    platformContractChangeStatus,
    platformContractOrgOptions,
    platformContractPageList,
    platformContractRemove,
    platformContractUpdate,
    uploadTempAttachment,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const userStore = useUserStore();
  const isCityManager = computed(() => userStore.isCityManager);

  const keyword = ref('');
  const queryStatus = ref<PlatformContractStatus | ''>('');
  const tableRefreshId = ref(0);

  const statusOptions = computed(() => [
    { label: t('common.all'), value: '' },
    { label: t('platformFinance.status.DRAFT'), value: PlatformContractStatusEnum.DRAFT },
    { label: t('platformFinance.status.PENDING_SIGN'), value: PlatformContractStatusEnum.PENDING_SIGN },
    { label: t('platformFinance.status.COMPLETED'), value: PlatformContractStatusEnum.COMPLETED },
    { label: t('platformFinance.status.ARCHIVED'), value: PlatformContractStatusEnum.ARCHIVED },
    { label: t('platformFinance.status.VOIDED'), value: PlatformContractStatusEnum.VOIDED },
  ]);

  const signTypeOptions = [
    { label: t('platformFinance.signType.OFFLINE'), value: PlatformSignTypeEnum.OFFLINE },
    { label: t('platformFinance.signType.ONLINE'), value: PlatformSignTypeEnum.ONLINE },
  ];

  const orgOptions = ref<{ label: string; value: string; editionCode?: string }[]>([]);
  const editionOptionsList = ref<{ label: string; value: string; yearPrice?: number; validityDays?: number }[]>([]);
  const signManagerOptions = ref<{ label: string; value: string }[]>([]);

  async function loadOptions() {
    try {
      // 签约经理下拉是 admin 维护项（CITY_MANAGER:MANAGE），城市经理不调该接口、签约经理由后端继承租户归属
      const managersPromise = isCityManager.value
        ? Promise.resolve({ list: [] })
        : cityManagerPageList({ current: 1, pageSize: 500 });
      const [orgs, editions, managers] = await Promise.all([
        platformContractOrgOptions(),
        editionOptions(),
        managersPromise,
      ]);
      orgOptions.value = (orgs as PlatformOrgOption[]).map((o) => ({
        label: o.name,
        value: o.id,
        editionCode: o.editionCode,
      }));
      editionOptionsList.value = (editions as Edition[]).map((e) => ({
        label: e.name,
        value: e.code,
        yearPrice: e.yearPrice,
        validityDays: e.validityDays,
      }));
      signManagerOptions.value = (managers.list || [])
        .filter((m: CityManagerItem) => m.status === 'ENABLED')
        .map((m: CityManagerItem) => ({ label: `${m.name}（${m.phone}）`, value: m.id }));
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

  function statusLabel(status: PlatformContractStatus) {
    return t(`platformFinance.status.${status}`);
  }

  // 新建/编辑
  const showForm = ref(false);
  const formMode = ref<'add' | 'edit'>('add');
  const formLoading = ref(false);
  const form = reactive<Omit<PlatformContractSaveParams, 'amount'> & { id: string; amount: number | null }>({
    id: '',
    contractNo: '',
    organizationId: '',
    creditCode: '',
    contactPerson: '',
    contactPhone: '',
    address: '',
    editionCode: '',
    amount: null,
    validityDays: 365,
    signType: PlatformSignTypeEnum.OFFLINE,
    signManagerId: '',
    attachmentIds: '',
    remark: '',
  });
  const attachmentFileList = ref<UploadFileInfo[]>([]);
  // naive-ui 的 fileList 只保留白名单字段（id/name/status…），自定义字段会被剥离，
  // 所以上传成功的附件 id 单独用 file.id 做 key 记录，提交时从这里取。
  const uploadedAttachmentIds = ref<Record<string, string>>({});
  // 已保存到合同的附件（编辑时回显，可预览/下载/删除）
  const existingAttachments = ref<PlatformAttachment[]>([]);
  const preview = reactive<{ show: boolean; src: string }>({ show: false, src: '' });

  function isImage(type?: string) {
    return /(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(type || '');
  }

  function attachmentUrl(id: string) {
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  function previewAttachment(id: string) {
    preview.src = attachmentUrl(id);
    preview.show = true;
  }

  async function downloadAttachmentById(id: string, name: string) {
    try {
      const res = await downloadAttachment(id);
      const url = URL.createObjectURL(new Blob([res], { type: 'application/octet-stream' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = name;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function removeExistingAttachment(id: string) {
    existingAttachments.value = existingAttachments.value.filter((a) => a.id !== id);
  }

  function openAdd() {
    formMode.value = 'add';
    Object.assign(form, {
      id: '',
      contractNo: '',
      organizationId: '',
      creditCode: '',
      contactPerson: '',
      contactPhone: '',
      address: '',
      editionCode: '',
      amount: null,
      validityDays: 365,
      signType: PlatformSignTypeEnum.OFFLINE,
      signManagerId: '',
      attachmentIds: '',
      remark: '',
    });
    attachmentFileList.value = [];
    uploadedAttachmentIds.value = {};
    existingAttachments.value = [];
    showForm.value = true;
  }

  function openEdit(row: PlatformContractItem) {
    formMode.value = 'edit';
    Object.assign(form, {
      id: row.id,
      contractNo: row.contractNo,
      organizationId: row.organizationId,
      creditCode: row.creditCode || '',
      contactPerson: row.contactPerson || '',
      contactPhone: row.contactPhone || '',
      address: row.address || '',
      editionCode: row.editionCode || '',
      amount: row.amount == null ? null : Number(row.amount),
      validityDays: row.validityDays ?? 365,
      signType: row.signType || PlatformSignTypeEnum.OFFLINE,
      signManagerId: row.signManagerId || '',
      attachmentIds: row.attachmentIds || '',
      remark: row.remark || '',
    });
    attachmentFileList.value = [];
    uploadedAttachmentIds.value = {};
    // 列表接口已带 attachmentList，直接回显已上传的附件（可预览/下载/删除）
    existingAttachments.value = row.attachmentList || [];
    showForm.value = true;
  }

  // 选版本后自动带出合同金额/有效期（可再手动改）
  function onEditionChange(code: string) {
    const edition = editionOptionsList.value.find((e) => e.value === code);
    if (!edition) return;
    if (edition.yearPrice != null) form.amount = Number(edition.yearPrice);
    if (edition.validityDays != null) form.validityDays = Number(edition.validityDays);
  }

  // 选中租户后默认带出该租户当前套餐版本（及其金额/有效期）
  function onOrgChange(orgId: string) {
    const org = orgOptions.value.find((o) => o.value === orgId);
    if (!org?.editionCode) {
      form.editionCode = '';
      return;
    }
    form.editionCode = org.editionCode;
    onEditionChange(org.editionCode);
  }

  async function uploadAttachment(options: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(options.file.file as File);
      const [attachmentId] = res.data;
      uploadedAttachmentIds.value[options.file.id] = attachmentId;
      options.onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      options.onError();
    }
  }

  function removeAttachment(options: { file: UploadFileInfo }) {
    delete uploadedAttachmentIds.value[options.file.id];
  }

  async function confirmSave() {
    if (!form.contractNo.trim()) {
      Message.warning(t('platformFinance.contractNoRequired'));
      return;
    }
    if (!form.organizationId) {
      Message.warning(t('platformFinance.tenantRequired'));
      return;
    }
    if (!form.editionCode) {
      Message.warning(t('platformFinance.editionRequired'));
      return;
    }
    if (form.amount == null) {
      Message.warning(t('platformFinance.amountRequired'));
      return;
    }
    if (form.validityDays == null) {
      Message.warning(t('platformFinance.validityDaysRequired'));
      return;
    }
    if (!form.signType) {
      Message.warning(t('platformFinance.signTypeRequired'));
      return;
    }
    if (!form.contactPerson?.trim()) {
      Message.warning(t('platformFinance.contactPersonRequired'));
      return;
    }
    if (!form.contactPhone?.trim()) {
      Message.warning(t('platformFinance.contactPhoneRequired'));
      return;
    }
    const newIds = Object.values(uploadedAttachmentIds.value).filter(Boolean);
    const existingIds = existingAttachments.value.map((a) => a.id).filter(Boolean);
    if (existingIds.length === 0 && newIds.length === 0) {
      Message.warning(t('platformFinance.attachmentRequired'));
      return;
    }
    formLoading.value = true;
    try {
      const attachmentIds = [...existingIds, ...newIds].join(',');
      const payload: PlatformContractSaveParams = {
        contractNo: form.contractNo.trim(),
        organizationId: form.organizationId,
        creditCode: form.creditCode || undefined,
        contactPerson: form.contactPerson || undefined,
        contactPhone: form.contactPhone || undefined,
        address: form.address || undefined,
        editionCode: form.editionCode || undefined,
        amount: form.amount ?? undefined,
        validityDays: form.validityDays ?? undefined,
        signType: form.signType,
        signManagerId: form.signManagerId || undefined,
        attachmentIds: attachmentIds || undefined,
        remark: form.remark || undefined,
      };
      if (formMode.value === 'add') {
        await platformContractAdd(payload);
      } else {
        await platformContractUpdate({ ...payload, id: form.id });
      }
      Message.success(t('platformFinance.saveSuccess'));
      showForm.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      formLoading.value = false;
    }
  }

  // 状态流转
  function changeStatus(row: PlatformContractItem, status: PlatformContractStatus) {
    openModal({
      type: 'default',
      title: t('platformFinance.statusChangeTitle'),
      content: t('platformFinance.statusChangeContent', {
        contractNo: row.contractNo,
        status: statusLabel(status),
      }),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await platformContractChangeStatus({ id: row.id, status });
          Message.success(t('platformFinance.statusChangeSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleDelete(row: PlatformContractItem) {
    openModal({
      type: 'warning',
      title: t('platformFinance.deleteTitle'),
      content: t('platformFinance.deleteContent', { contractNo: row.contractNo }),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await platformContractRemove(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function buildActions(row: PlatformContractItem): ActionsItem[] {
    const list: ActionsItem[] = [{ label: t('platformFinance.edit'), key: 'edit' }];
    if (row.status === PlatformContractStatusEnum.DRAFT) {
      list.push({ label: t('platformFinance.sign'), key: 'sign' });
    }
    if (row.status === PlatformContractStatusEnum.PENDING_SIGN) {
      list.push({ label: t('platformFinance.complete'), key: 'complete' });
    }
    if (row.status === PlatformContractStatusEnum.COMPLETED) {
      list.push({ label: t('platformFinance.archive'), key: 'archive' });
      list.push({ label: t('platformFinance.void'), key: 'void', danger: true });
    }
    list.push({ label: t('common.delete'), key: 'delete', danger: true });
    return list;
  }

  function handleActionSelect(row: PlatformContractItem, key: string) {
    switch (key) {
      case 'edit':
        openEdit(row);
        break;
      case 'sign':
        changeStatus(row, PlatformContractStatusEnum.PENDING_SIGN);
        break;
      case 'complete':
        changeStatus(row, PlatformContractStatusEnum.COMPLETED);
        break;
      case 'archive':
        changeStatus(row, PlatformContractStatusEnum.ARCHIVED);
        break;
      case 'void':
        changeStatus(row, PlatformContractStatusEnum.VOIDED);
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
      title: t('platformFinance.contractNo'),
      key: 'contractNo',
      width: 160,
      ellipsis: { tooltip: true },
    },
    {
      title: t('platformFinance.tenant'),
      key: 'orgName',
      width: 180,
      ellipsis: { tooltip: true },
      render: (row: PlatformContractItem) => row.orgName || '-',
    },
    {
      title: t('platformFinance.signManager'),
      key: 'signManagerName',
      width: 110,
      ellipsis: { tooltip: true },
      render: (row: PlatformContractItem) => row.signManagerName || '-',
    },
    {
      title: t('platformFinance.followManager'),
      key: 'followManagerName',
      width: 110,
      ellipsis: { tooltip: true },
      render: (row: PlatformContractItem) => row.followManagerName || '-',
    },
    {
      title: t('platformFinance.contactPerson'),
      key: 'contactPerson',
      width: 100,
      ellipsis: { tooltip: true },
      render: (row: PlatformContractItem) => row.contactPerson || '-',
    },
    {
      title: t('platformFinance.contactPhone'),
      key: 'contactPhone',
      width: 120,
      ellipsis: { tooltip: true },
      render: (row: PlatformContractItem) => row.contactPhone || '-',
    },
    {
      title: t('platformFinance.edition'),
      key: 'editionName',
      width: 120,
      render: (row: PlatformContractItem) => row.editionName || '-',
    },
    {
      title: t('platformFinance.amount'),
      key: 'amount',
      width: 130,
      align: 'right',
      render: (row: PlatformContractItem) => fmtMoney(row.amount),
    },
    {
      title: t('platformFinance.validityDays'),
      key: 'validityDays',
      width: 90,
      align: 'right',
      render: (row: PlatformContractItem) =>
        row.validityDays != null ? `${row.validityDays}${t('platformFinance.day')}` : '-',
    },
    {
      title: t('platformFinance.status'),
      key: 'status',
      width: 100,
      render: (row: PlatformContractItem) =>
        h(
          NTag,
          { type: PlatformContractStatusTagMap[row.status] || 'default', size: 'small' },
          { default: () => statusLabel(row.status) }
        ),
    },
    {
      title: t('platformFinance.attachment'),
      key: 'attachmentList',
      width: 110,
      render: (row: PlatformContractItem) => {
        const list = row.attachmentList || [];
        if (!list.length) return '-';
        const first = list[0];
        return h('div', { class: 'flex items-center gap-[6px]' }, [
          isImage(first.type)
            ? h(NImage, {
                src: attachmentUrl(first.id),
                width: 28,
                height: 28,
                objectFit: 'cover',
                class: 'cursor-pointer rounded-[2px]',
                previewDisabled: true,
                onClick: () => previewAttachment(first.id),
              })
            : h('span', { class: 'text-[12px] text-[var(--text-n3)]' }, first.name || ''),
          h(
            'span',
            { class: 'text-[12px] text-[var(--text-n3)]' },
            t('platformFinance.attachmentCount', { count: list.length })
          ),
        ]);
      },
    },
    {
      title: t('platformFinance.createTime'),
      key: 'createTime',
      width: 160,
      sortOrder: false,
      sorter: true,
      render: (row: PlatformContractItem) => formatTime(row.createTime),
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 200,
      fixed: 'right',
      render: (row: PlatformContractItem) =>
        h(CrmOperationButton, {
          groupList: buildActions(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<PlatformContractItem>(
    platformContractPageList,
    {
      tableKey: TableKeyEnum.PLATFORM_CONTRACT_TABLE,
      columns,
      showSetting: true,
      containerClass: '.pf-contract-table',
    }
  );

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
