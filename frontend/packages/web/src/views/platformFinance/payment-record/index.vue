<template>
  <div class="pf-payment-page flex h-full flex-col overflow-hidden">
    <CrmCard hide-footer auto-height class="mb-[16px]">
      <div class="flex items-center gap-[12px]">
        <CrmSelect
          v-model:value="queryContractId"
          :options="contractOptions"
          :placeholder="t('platformPayment.contract')"
          clearable
          filterable
          class="w-[240px]"
        />
        <CrmSelect
          v-model:value="queryStatus"
          :options="statusOptions"
          :placeholder="t('platformPayment.verificationStatus')"
          clearable
          class="w-[160px]"
        />
        <n-button type="primary" @click="search">{{ t('common.search') }}</n-button>
        <n-button class="outline--secondary" @click="reset">{{ t('common.reset') }}</n-button>
        <div class="flex-1" />
        <n-button v-if="!isCityManager" class="outline--secondary" @click="openConfig">{{
          t('platformPayment.paymentSetting')
        }}</n-button>
        <n-button type="primary" ghost @click="openAdd">{{ t('platformPayment.pay') }}</n-button>
      </div>
    </CrmCard>

    <CrmCard no-content-padding hide-footer class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="pf-payment-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>

    <!-- 回款（一次性全额） -->
    <n-modal
      v-model:show="showForm"
      preset="card"
      :title="t('platformPayment.pay')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <div class="flex flex-col gap-[16px]">
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.contract') }}</span>
          <CrmSelect
            v-model:value="form.contractId"
            :options="contractOptions"
            filterable
            class="flex-1"
            @update:value="onContractChange"
          />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.recordNo') }}</span>
          <n-input v-model:value="form.recordNo" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.amount') }}</span>
          <n-input-number v-model:value="form.amount" :min="0" :precision="2" class="flex-1" disabled />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.paymentType') }}</span>
          <CrmSelect v-model:value="form.paymentType" :options="paymentTypeOptions" class="flex-1" />
        </div>
        <div class="flex items-center gap-[12px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.bankAccount') }}</span>
          <n-input v-model:value="form.bankAccount" class="flex-1" />
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformPayment.paymentVoucher') }}</span>
          <div class="flex-1">
            <n-upload
              v-model:file-list="voucherFileList"
              multiple
              :custom-request="uploadVoucher"
              @remove="removeVoucher"
            >
              <n-button>{{ t('platformPayment.uploadVoucher') }}</n-button>
            </n-upload>
          </div>
        </div>
        <div class="flex items-start gap-[12px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformPayment.remark') }}</span>
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

    <!-- 核销弹窗 -->
    <n-modal v-model:show="verifyModal.show" preset="card" :title="t('platformPayment.verifyTitle')" class="w-[560px]">
      <div class="flex flex-col gap-[12px]">
        <div class="flex items-center gap-[8px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.contract') }}：</span>
          <span>{{ verifyModal.record?.contractNo || '-' }}</span>
        </div>
        <div class="flex items-center gap-[8px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.amount') }}：</span>
          <span>{{ fmtMoney(verifyModal.record?.amount) }}</span>
        </div>
        <div class="flex items-start gap-[8px]">
          <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.paymentVoucher') }}：</span>
          <div class="flex flex-col gap-[8px]">
            <div
              v-for="voucher in verifyModal.record?.voucherList || []"
              :key="voucher.id"
              class="flex items-center gap-[8px]"
            >
              <n-image
                v-if="isImage(voucher.type)"
                :src="voucherUrl(voucher.id)"
                :width="48"
                :height="48"
                object-fit="cover"
                class="rounded-[4px]"
                preview-disabled
                @click="previewAttachment(voucher.id)"
              />
              <div v-else class="text-[13px] text-[var(--text-n2)]">{{ voucher.name }}</div>
              <n-button size="tiny" text type="primary" @click="downloadAttachmentById(voucher.id, voucher.name)">
                {{ t('common.download') }}
              </n-button>
            </div>
            <div v-if="!verifyModal.record?.voucherList?.length" class="text-[13px] text-[var(--text-n4)]">-</div>
          </div>
        </div>
        <div class="flex items-start gap-[8px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformPayment.proof') }}：</span>
          <div class="flex-1">
            <n-upload v-model:file-list="proofFileList" multiple :custom-request="uploadProof" @remove="removeProof">
              <n-button>{{ t('platformPayment.uploadProof') }}</n-button>
            </n-upload>
          </div>
        </div>
        <div class="flex items-start gap-[8px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformPayment.verifyRemark') }}：</span>
          <div class="flex-1">
            <n-input
              v-model:value="verifyModal.remark"
              type="textarea"
              :rows="3"
              :placeholder="t('platformPayment.verifyRemarkPlaceholder')"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="verifyModal.show = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="verifyModal.loading" @click="submitVerify">{{
            t('platformPayment.confirmVerify')
          }}</n-button>
        </div>
      </template>
    </n-modal>
    <n-image-preview v-model:show="preview.show" :src="preview.src" />

    <!-- 撤回弹窗 -->
    <n-modal v-model:show="revokeModal.show" preset="card" :title="t('platformPayment.revokeTitle')" class="w-[480px]">
      <div class="flex flex-col gap-[12px]">
        <div class="flex items-start gap-[8px]">
          <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ t('platformPayment.revokeRemark') }}：</span>
          <div class="flex-1">
            <n-input
              v-model:value="revokeModal.remark"
              type="textarea"
              :rows="3"
              :placeholder="t('platformPayment.revokeRemarkPlaceholder')"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="revokeModal.show = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="revokeModal.loading" @click="submitRevoke">{{
            t('platformPayment.confirmRevoke')
          }}</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 收款设置弹窗 -->
    <n-modal
      v-model:show="showConfig"
      preset="card"
      :title="t('platformPayment.paymentSetting')"
      class="w-[560px]"
      :mask-closable="false"
    >
      <n-spin :show="configLoading">
        <div class="flex flex-col gap-[16px]">
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.companyName') }}</span>
            <n-input v-model:value="configForm.companyName" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.invoiceTitle') }}</span>
            <n-input v-model:value="configForm.invoiceTitle" class="flex-1" />
          </div>
          <div class="flex items-center gap-[12px]">
            <span class="w-[100px] shrink-0 text-right">{{ t('platformPayment.taxRate') }}</span>
            <n-input-number v-model:value="configForm.taxRate" :min="0" :max="100" :precision="2" class="flex-1" />
          </div>
          <div class="my-[4px] h-px bg-[var(--border-color)]" />
          <div class="text-[13px] font-medium">{{ t('platformPayment.paymentAccounts') }}</div>
          <div v-for="acc in bankAccounts" :key="acc.accountType" class="flex items-start gap-[12px]">
            <span class="w-[100px] shrink-0 pt-[6px] text-right">{{ paymentTypeLabel(acc.accountType) }}</span>
            <div class="flex flex-1 flex-col gap-[8px]">
              <n-input v-model:value="acc.accountName" :placeholder="t('platformPayment.accountName')" />
              <n-input v-model:value="acc.accountNo" :placeholder="t('platformPayment.accountNo')" />
              <n-input v-model:value="acc.bankName" :placeholder="t('platformPayment.bankNamePlaceholder')" />
            </div>
          </div>
        </div>
      </n-spin>
      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showConfig = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="configSaving" @click="confirmConfig">{{ t('common.confirm') }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onMounted, reactive, ref, watch } from 'vue';
  import {
    NButton,
    NImage,
    NImagePreview,
    NInput,
    NInputNumber,
    NModal,
    NSpin,
    NTag,
    NUpload,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import {
    PlatformPaymentTypeEnum,
    PlatformPaymentVerificationStatusEnum,
    PlatformPaymentVerificationStatusTagMap,
  } from '@lib/shared/enums/platformFinanceEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    PlatformBankAccount,
    PlatformContractOption,
    PlatformPaymentRecordItem,
  } from '@lib/shared/models/system/platformFinance';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSelect from '@/components/pure/crm-select/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    downloadAttachment,
    platformBankAccountList,
    platformBankAccountSave,
    platformContractOptions,
    platformGetConfig,
    platformPaymentRecordAdd,
    platformPaymentRecordPageList,
    platformPaymentRecordRemove,
    platformPaymentRecordRevoke,
    platformPaymentRecordVerify,
    platformUpdateConfig,
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

  const queryContractId = ref('');
  const queryStatus = ref('');
  const tableRefreshId = ref(0);

  const contractOptions = ref<{ label: string; value: string; amount?: number | string }[]>([]);

  const statusOptions = [
    { label: t('common.all'), value: '' },
    { label: t('platformPayment.status.PENDING'), value: PlatformPaymentVerificationStatusEnum.PENDING },
    { label: t('platformPayment.status.DONE'), value: PlatformPaymentVerificationStatusEnum.DONE },
  ];

  const paymentTypeOptions = [
    { label: t('platformPayment.paymentType.TRANSFER'), value: PlatformPaymentTypeEnum.TRANSFER },
    { label: t('platformPayment.paymentType.ALIPAY'), value: PlatformPaymentTypeEnum.ALIPAY },
    { label: t('platformPayment.paymentType.WECHAT'), value: PlatformPaymentTypeEnum.WECHAT },
    { label: t('platformPayment.paymentType.OFFLINE'), value: PlatformPaymentTypeEnum.OFFLINE },
  ];

  // 我方收款账号（每种收款方式一条），新增回款时按收款方式自动带出收款账户
  interface BankAccountForm {
    accountType: string;
    accountName: string;
    accountNo: string;
    bankName: string;
  }
  const bankAccounts = ref<BankAccountForm[]>([]);

  async function loadBankAccounts() {
    try {
      const list = (await platformBankAccountList()) as PlatformBankAccount[];
      bankAccounts.value = list.map((a) => ({
        accountType: a.accountType,
        accountName: a.accountName || '',
        accountNo: a.accountNo || '',
        bankName: a.bankName || '',
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function bankAccountOf(type: string): string {
    const matched = bankAccounts.value.find((a) => a.accountType === type);
    if (!matched) return '';
    return [matched.accountName, matched.accountNo].filter(Boolean).join(' ');
  }

  async function loadContracts() {
    try {
      const list = await platformContractOptions();
      contractOptions.value = (list as PlatformContractOption[]).map((c) => ({
        label: c.orgName ? `${c.contractNo}（${c.orgName}）` : c.contractNo,
        value: c.id,
        amount: c.amount,
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
    return t(`platformPayment.status.${status}`);
  }

  function paymentTypeLabel(type?: string) {
    return type ? t(`platformPayment.paymentType.${type}`) : '-';
  }

  function isImage(type?: string) {
    return /(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(type || '');
  }

  function voucherUrl(id: string) {
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  const preview = reactive<{ show: boolean; src: string }>({ show: false, src: '' });

  function previewAttachment(id: string) {
    preview.src = voucherUrl(id);
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

  // 回款
  const showForm = ref(false);
  const formLoading = ref(false);
  const form = reactive<{
    id: string;
    contractId: string;
    recordNo: string;
    amount: number | null;
    paymentType: string;
    bankAccount: string;
    remark: string;
  }>({
    id: '',
    contractId: '',
    recordNo: '',
    amount: null,
    paymentType: PlatformPaymentTypeEnum.TRANSFER,
    bankAccount: '',
    remark: '',
  });
  const voucherFileList = ref<UploadFileInfo[]>([]);
  // naive-ui 的 fileList 只保留白名单字段（id/name/status…），自定义字段会被剥离，
  // 所以上传成功的附件 id 单独用 file.id 做 key 记录，提交时从这里取。
  const uploadedVoucherIds = ref<Record<string, string>>({});

  // 选「收款方式」自动带出对应收款账户（一次性回款无编辑，始终按收款方式带出）
  watch(
    () => form.paymentType,
    (type) => {
      form.bankAccount = bankAccountOf(type);
    }
  );

  // 选合同后回款金额固定 = 合同金额（一次性全额，不可改）
  function onContractChange(contractId: string) {
    const contract = contractOptions.value.find((c) => c.value === contractId);
    form.amount = contract?.amount == null ? null : Number(contract.amount);
  }

  function openAdd() {
    Object.assign(form, {
      id: '',
      contractId: '',
      recordNo: '',
      amount: null,
      paymentType: PlatformPaymentTypeEnum.TRANSFER,
      bankAccount: '',
      remark: '',
    });
    // 默认收款方式（对公转账）在值未变化时不会触发 watch，手动带出对应收款账户
    form.bankAccount = bankAccountOf(PlatformPaymentTypeEnum.TRANSFER);
    voucherFileList.value = [];
    uploadedVoucherIds.value = {};
    showForm.value = true;
  }

  async function uploadVoucher(options: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(options.file.file as File);
      const [attachmentId] = res.data;
      uploadedVoucherIds.value[options.file.id] = attachmentId;
      options.onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      options.onError();
    }
  }

  function removeVoucher(options: { file: UploadFileInfo }) {
    delete uploadedVoucherIds.value[options.file.id];
  }

  async function confirmSave() {
    if (!form.contractId) {
      Message.warning(t('platformPayment.contractRequired'));
      return;
    }
    formLoading.value = true;
    try {
      const voucherAttachmentIds = Object.values(uploadedVoucherIds.value).filter(Boolean).join(',');
      const payload = {
        contractId: form.contractId,
        recordNo: form.recordNo.trim() || undefined,
        amount: form.amount ?? undefined,
        paymentType: form.paymentType,
        bankAccount: form.bankAccount || undefined,
        voucherAttachmentIds: voucherAttachmentIds || undefined,
        remark: form.remark || undefined,
      };
      await platformPaymentRecordAdd(payload);
      Message.success(t('platformPayment.saveSuccess'));
      showForm.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      formLoading.value = false;
    }
  }

  function handleDelete(row: PlatformPaymentRecordItem) {
    openModal({
      type: 'warning',
      title: t('platformPayment.deleteTitle'),
      content: t('platformPayment.deleteContent', { recordNo: row.recordNo || '-' }),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await platformPaymentRecordRemove(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 核销
  const verifyModal = reactive<{
    show: boolean;
    loading: boolean;
    record: PlatformPaymentRecordItem | null;
    remark: string;
  }>({ show: false, loading: false, record: null, remark: '' });
  const proofFileList = ref<UploadFileInfo[]>([]);
  // naive-ui 的 fileList 只保留白名单字段（id/name/status…），自定义字段会被剥离，
  // 所以上传成功的附件 id 单独用 file.id 做 key 记录，提交时从这里取。
  const uploadedProofIds = ref<Record<string, string>>({});

  function openVerify(row: PlatformPaymentRecordItem) {
    verifyModal.record = row;
    verifyModal.remark = '';
    proofFileList.value = [];
    uploadedProofIds.value = {};
    verifyModal.show = true;
  }

  async function uploadProof(options: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(options.file.file as File);
      const [attachmentId] = res.data;
      uploadedProofIds.value[options.file.id] = attachmentId;
      options.onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      options.onError();
    }
  }

  function removeProof(options: { file: UploadFileInfo }) {
    delete uploadedProofIds.value[options.file.id];
  }

  async function submitVerify() {
    if (!verifyModal.record) return;
    verifyModal.loading = true;
    try {
      const proofAttachmentIds = Object.values(uploadedProofIds.value).filter(Boolean);
      await platformPaymentRecordVerify({
        id: verifyModal.record.id,
        remark: verifyModal.remark || undefined,
        proofAttachmentIds: proofAttachmentIds.length ? proofAttachmentIds : undefined,
      });
      Message.success(t('platformPayment.verifySuccess'));
      verifyModal.show = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      verifyModal.loading = false;
    }
  }

  // 撤回
  const revokeModal = reactive<{
    show: boolean;
    loading: boolean;
    record: PlatformPaymentRecordItem | null;
    remark: string;
  }>({
    show: false,
    loading: false,
    record: null,
    remark: '',
  });

  function openRevoke(row: PlatformPaymentRecordItem) {
    revokeModal.record = row;
    revokeModal.remark = '';
    revokeModal.show = true;
  }

  async function submitRevoke() {
    if (!revokeModal.record) return;
    revokeModal.loading = true;
    try {
      await platformPaymentRecordRevoke({ id: revokeModal.record.id, remark: revokeModal.remark || undefined });
      Message.success(t('platformPayment.revokeSuccess'));
      revokeModal.show = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      revokeModal.loading = false;
    }
  }

  // 收款设置
  const showConfig = ref(false);
  const configLoading = ref(false);
  const configSaving = ref(false);
  const configForm = reactive<{
    companyName: string;
    invoiceTitle: string;
    taxRate: number | null;
  }>({
    companyName: '',
    invoiceTitle: '',
    taxRate: 6,
  });

  async function openConfig() {
    showConfig.value = true;
    configLoading.value = true;
    try {
      const cfg = await platformGetConfig();
      configForm.companyName = cfg.companyName || '';
      configForm.invoiceTitle = cfg.invoiceTitle || '';
      configForm.taxRate = cfg.taxRate == null ? 6 : Number(cfg.taxRate);
      await loadBankAccounts();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      configLoading.value = false;
    }
  }

  async function confirmConfig() {
    configSaving.value = true;
    try {
      await platformUpdateConfig({
        companyName: configForm.companyName,
        invoiceTitle: configForm.invoiceTitle,
        taxRate: configForm.taxRate == null ? undefined : String(configForm.taxRate),
      });
      await platformBankAccountSave(
        bankAccounts.value.map((a) => ({
          accountType: a.accountType,
          accountName: a.accountName,
          accountNo: a.accountNo,
          bankName: a.bankName,
        }))
      );
      Message.success(t('platformPayment.configSaveSuccess'));
      showConfig.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      configSaving.value = false;
    }
  }

  function buildActions(row: PlatformPaymentRecordItem): ActionsItem[] {
    const list: ActionsItem[] = [];
    // 核销/撤回仅 admin：城市合伙人只登记/编辑/删除核销前的回款
    if (!isCityManager.value) {
      if (row.verificationStatus === PlatformPaymentVerificationStatusEnum.PENDING) {
        list.push({ label: t('platformPayment.verify'), key: 'verify' });
      } else {
        list.push({ label: t('platformPayment.revoke'), key: 'revoke', danger: true });
      }
    }
    // 已核销锁定，不可删除（后端同口径）
    if (row.verificationStatus !== PlatformPaymentVerificationStatusEnum.DONE) {
      list.push({ label: t('common.delete'), key: 'delete', danger: true });
    }
    return list;
  }

  function handleActionSelect(row: PlatformPaymentRecordItem, key: string) {
    switch (key) {
      case 'verify':
        openVerify(row);
        break;
      case 'revoke':
        openRevoke(row);
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
      title: t('platformPayment.recordNo'),
      key: 'recordNo',
      width: 150,
      ellipsis: { tooltip: true },
      render: (row: PlatformPaymentRecordItem) => row.recordNo || '-',
    },
    {
      title: t('platformPayment.contract'),
      key: 'contractNo',
      width: 160,
      ellipsis: { tooltip: true },
      render: (row: PlatformPaymentRecordItem) => row.contractNo || '-',
    },
    {
      title: t('platformPayment.tenant'),
      key: 'orgName',
      width: 160,
      ellipsis: { tooltip: true },
      render: (row: PlatformPaymentRecordItem) => row.orgName || '-',
    },
    {
      title: t('platformPayment.signManager'),
      key: 'signManagerName',
      width: 110,
      ellipsis: { tooltip: true },
      render: (row: PlatformPaymentRecordItem) => row.signManagerName || '-',
    },
    {
      title: t('platformPayment.followManager'),
      key: 'followManagerName',
      width: 110,
      ellipsis: { tooltip: true },
      render: (row: PlatformPaymentRecordItem) => row.followManagerName || '-',
    },
    {
      title: t('platformPayment.amount'),
      key: 'amount',
      width: 130,
      align: 'right',
      render: (row: PlatformPaymentRecordItem) => fmtMoney(row.amount),
    },
    {
      title: t('platformPayment.paymentType'),
      key: 'paymentType',
      width: 100,
      render: (row: PlatformPaymentRecordItem) => paymentTypeLabel(row.paymentType),
    },
    {
      title: t('platformPayment.verificationStatus'),
      key: 'verificationStatus',
      width: 100,
      render: (row: PlatformPaymentRecordItem) =>
        h(
          NTag,
          { type: PlatformPaymentVerificationStatusTagMap[row.verificationStatus] || 'default', size: 'small' },
          { default: () => statusLabel(row.verificationStatus) }
        ),
    },
    {
      title: t('platformPayment.verifyTime'),
      key: 'verifyTime',
      width: 160,
      render: (row: PlatformPaymentRecordItem) => formatTime(row.verifyTime),
    },
    {
      title: t('platformPayment.createTime'),
      key: 'createTime',
      width: 160,
      sortOrder: false,
      sorter: true,
      render: (row: PlatformPaymentRecordItem) => formatTime(row.createTime),
    },
    {
      key: 'operation',
      title: t('common.operation'),
      width: 200,
      fixed: 'right',
      render: (row: PlatformPaymentRecordItem) =>
        h(CrmOperationButton, {
          groupList: buildActions(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<PlatformPaymentRecordItem>(
    platformPaymentRecordPageList,
    {
      tableKey: TableKeyEnum.PLATFORM_PAYMENT_RECORD_TABLE,
      columns,
      showSetting: true,
      containerClass: '.pf-payment-table',
    }
  );

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  watch(tableRefreshId, () => {
    loadList();
  });

  function search() {
    const params: Record<string, unknown> = {};
    if (queryContractId.value) params.contractId = queryContractId.value;
    if (queryStatus.value) params.verificationStatus = queryStatus.value;
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
    loadBankAccounts();
    search();
  });
</script>
