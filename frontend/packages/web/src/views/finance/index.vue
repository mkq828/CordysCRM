<template>
  <div class="finance-page">
    <!-- 概览统计 -->
    <div class="overview-grid">
      <div v-for="item in overviewCards" :key="item.key" class="overview-card">
        <div class="overview-label">{{ item.label }}</div>
        <div class="overview-value" :class="{ 'is-red': item.red }">{{ item.value }}</div>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="search-bar">
      <n-input
        v-model:value="keyword"
        class="!w-[320px]"
        :placeholder="t('finance.searchPlaceholder')"
        clearable
        @keyup.enter="handleSearch"
      />
      <n-button type="primary" @click="handleSearch">{{ t('finance.search') }}</n-button>
    </div>

    <!-- 应收列表 -->
    <n-spin :show="loading" class="flex-1">
      <n-empty v-if="!loading && customers.length === 0" class="!mt-[80px]" :description="t('finance.empty')" />

      <div v-else class="customer-list">
        <div v-for="customer in customers" :key="customer.customerId" class="customer-card">
          <div class="customer-header" @click="toggleCustomer(customer.customerId)">
            <div class="flex items-center gap-[8px]">
              <span class="customer-name">{{ customer.customerName || '-' }}</span>
              <n-tag :type="customer.settled ? 'success' : 'error'" size="small">
                {{ customer.settled ? t('finance.settled') : t('finance.unsettled') }}
              </n-tag>
            </div>
            <div class="customer-amounts">
              <span>
                {{ t('finance.contractAmount') }}
                <b>{{ fmtMoney(customer.totalAmount) }}</b>
              </span>
              <span>
                {{ t('finance.verifiedAmount') }}
                <b>{{ fmtMoney(customer.verifiedAmount) }}</b>
              </span>
              <span>
                {{ t('finance.pendingAmount') }}
                <b :class="{ 'is-red': !customer.settled }">{{ fmtMoney(customer.pendingAmount) }}</b>
              </span>
            </div>
          </div>

          <template v-if="expandedCustomers.has(customer.customerId)">
            <div class="contract-list">
              <div v-for="contract in customer.contracts" :key="contract.contractId" class="contract-card">
                <div class="contract-header" @click="toggleContract(contract.contractId)">
                  <div class="flex items-center gap-[8px]">
                    <span class="contract-number">{{ contract.contractNumber || '-' }}</span>
                    <span class="contract-name">{{ contract.contractName || '-' }}</span>
                    <n-tag :type="contract.settled ? 'success' : 'error'" size="small">
                      {{ contract.settled ? t('finance.settled') : t('finance.unsettled') }}
                    </n-tag>
                  </div>
                  <div class="contract-amounts">
                    <span>
                      {{ t('finance.contractAmount') }}
                      <b>{{ fmtMoney(contract.amount) }}</b>
                    </span>
                    <span>
                      {{ t('finance.verifiedAmount') }}
                      <b>{{ fmtMoney(contract.verifiedAmount) }}</b>
                    </span>
                    <span>
                      {{ t('finance.pendingAmount') }}
                      <b :class="{ 'is-red': !contract.settled }">{{ fmtMoney(contract.pendingAmount) }}</b>
                    </span>
                  </div>
                </div>

                <template v-if="expandedContracts.has(contract.contractId)">
                  <div class="record-table">
                    <div class="record-row record-row-head">
                      <span>{{ t('finance.recordNo') }}</span>
                      <span>{{ t('finance.recordName') }}</span>
                      <span>{{ t('finance.recordAmount') }}</span>
                      <span>{{ t('finance.recordTime') }}</span>
                      <span>{{ t('finance.verifyStatus') }}</span>
                      <span>{{ t('finance.action') }}</span>
                    </div>
                    <div v-for="record in contract.records" :key="record.id" class="record-row">
                      <span class="truncate">{{ record.no || '-' }}</span>
                      <span class="truncate">{{ record.name || '-' }}</span>
                      <span>{{ fmtMoney(record.recordAmount) }}</span>
                      <span>{{ fmtTime(record.recordEndTime) }}</span>
                      <span>
                        <n-tag :type="record.verificationStatus === 'DONE' ? 'success' : 'warning'" size="small">
                          {{
                            record.verificationStatus === 'DONE' ? t('finance.verified') : t('finance.pendingVerify')
                          }}
                        </n-tag>
                      </span>
                      <span class="flex items-center gap-[6px]">
                        <n-button
                          v-if="record.verificationStatus === 'PENDING'"
                          v-permission="['FINANCE:VERIFY']"
                          size="tiny"
                          type="primary"
                          @click.stop="openVerify(record, contract)"
                        >
                          {{ t('finance.verify') }}
                        </n-button>
                        <n-button
                          v-else
                          v-permission="['FINANCE:VERIFY']"
                          size="tiny"
                          quaternary
                          type="error"
                          @click.stop="openRevoke(record)"
                        >
                          {{ t('finance.revoke') }}
                        </n-button>
                      </span>
                    </div>
                    <div v-if="!contract.records || contract.records.length === 0" class="record-empty">
                      {{ t('finance.empty') }}
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </template>
        </div>
      </div>
    </n-spin>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-bar">
      <n-pagination v-model:page="current" :page-size="pageSize" :item-count="total" @update:page="loadPage" />
    </div>

    <!-- 核销弹窗 -->
    <n-modal v-model:show="verifyModal.show" preset="card" :title="t('finance.verifyTitle')" class="!w-[560px]">
      <div class="verify-form">
        <div v-if="verifyModal.contract" class="verify-row">
          <span class="verify-label">{{ t('finance.contractName') }}：</span>
          <span class="verify-value">{{ verifyModal.contract.contractName || '-' }}</span>
        </div>
        <div v-if="verifyModal.contract" class="verify-row">
          <span class="verify-label">{{ t('finance.contractAmount') }}：</span>
          <span class="verify-value">{{ fmtMoney(verifyModal.contract.amount) }}</span>
        </div>
        <div v-if="bankAccountText" class="verify-row">
          <span class="verify-label">{{ t('finance.bankAccount') }}：</span>
          <span class="verify-value">{{ bankAccountText }}</span>
        </div>
        <div class="verify-row">
          <span class="verify-label">{{ t('finance.recordName') }}：</span>
          <span class="verify-value">{{ verifyModal.record?.name || '-' }}</span>
        </div>
        <div class="verify-row">
          <span class="verify-label">{{ t('finance.recordAmount') }}：</span>
          <span class="verify-value">{{ fmtMoney(verifyModal.record?.recordAmount) }}</span>
        </div>
        <div class="verify-row">
          <span class="verify-label">{{ t('finance.recordTime') }}：</span>
          <span class="verify-value">{{ fmtTime(verifyModal.record?.recordEndTime) }}</span>
        </div>
        <div v-if="verifyModal.record?.vouchers?.length" class="verify-row">
          <span class="verify-label">{{ t('finance.paymentVoucher') }}：</span>
          <div class="verify-value voucher-list">
            <div v-for="voucher in verifyModal.record.vouchers" :key="voucher.id" class="voucher-item">
              <n-image
                v-if="isImage(voucher.type)"
                :src="voucherUrl(voucher)"
                :width="48"
                :height="48"
                object-fit="cover"
                class="voucher-thumb"
                preview-disabled
                @click="previewVoucher(voucher)"
              />
              <div v-else class="voucher-name">{{ voucher.name }}</div>
              <n-button v-if="isImage(voucher.type)" size="tiny" text type="primary" @click="previewVoucher(voucher)">
                {{ t('common.preview') }}
              </n-button>
              <n-button size="tiny" text type="primary" @click="downloadVoucher(voucher)">
                {{ t('common.download') }}
              </n-button>
            </div>
          </div>
        </div>
        <div class="verify-row">
          <span class="verify-label">{{ t('finance.proof') }}：</span>
          <div class="verify-value">
            <n-upload v-model:file-list="proofFileList" multiple :custom-request="uploadProof" @remove="removeProof">
              <n-button>{{ t('finance.uploadProof') }}</n-button>
            </n-upload>
          </div>
        </div>
        <div class="verify-row verify-row-top">
          <span class="verify-label">{{ t('finance.verifyRemark') }}：</span>
          <div class="verify-value">
            <n-input
              v-model:value="verifyModal.remark"
              type="textarea"
              :rows="3"
              :placeholder="t('finance.verifyRemarkPlaceholder')"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="verifyModal.show = false">{{ t('finance.cancel') }}</n-button>
          <n-button type="primary" :loading="verifyModal.loading" @click="submitVerify">
            {{ t('finance.confirmVerify') }}
          </n-button>
        </div>
      </template>
    </n-modal>
    <n-image-preview v-model:show="voucherPreview.show" :src="voucherPreview.src" />

    <!-- 撤回弹窗 -->
    <n-modal v-model:show="revokeModal.show" preset="card" :title="t('finance.revokeTitle')" class="!w-[480px]">
      <div class="verify-form">
        <div class="verify-row">
          <span class="verify-label">{{ t('finance.recordName') }}：</span>
          <span class="verify-value">{{ revokeModal.record?.name || '-' }}</span>
        </div>
        <div class="verify-row">
          <span class="verify-label">{{ t('finance.recordAmount') }}：</span>
          <span class="verify-value">{{ fmtMoney(revokeModal.record?.recordAmount) }}</span>
        </div>
        <div class="verify-row verify-row-top">
          <span class="verify-label">{{ t('finance.revokeRemark') }}：</span>
          <div class="verify-value">
            <n-input
              v-model:value="revokeModal.remark"
              type="textarea"
              :rows="3"
              :placeholder="t('finance.revokeRemarkPlaceholder')"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="revokeModal.show = false">{{ t('finance.cancel') }}</n-button>
          <n-button type="primary" :loading="revokeModal.loading" @click="submitRevoke">
            {{ t('finance.confirmRevoke') }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import {
    NButton,
    NEmpty,
    NImage,
    NImagePreview,
    NInput,
    NModal,
    NPagination,
    NSpin,
    NTag,
    NUpload,
    useMessage,
  } from 'naive-ui';

  import type {
    FinanceContract,
    FinanceCustomerGroup,
    FinanceOverview,
    FinancePaymentRecord,
  } from '@lib/shared/api/modules/finance';
  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { BankAccountTypeEnum } from '@lib/shared/enums/bankAccountEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatTimeValue } from '@lib/shared/method';

  import {
    downloadAttachment,
    financeOverview,
    financePage,
    financeRevoke,
    financeVerify,
    uploadTempAttachment,
  } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const loading = ref(false);
  const keyword = ref('');
  const current = ref(1);
  const pageSize = 20;
  const total = ref(0);
  const customers = ref<FinanceCustomerGroup[]>([]);
  const overview = ref<FinanceOverview | null>(null);

  const expandedCustomers = ref<Set<string>>(new Set());
  const expandedContracts = ref<Set<string>>(new Set());

  function fmtMoney(value?: number | string) {
    const n = Number(value ?? 0);
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function fmtTime(ts?: number) {
    // 回款时间是 dateType="date" 的日期字段，仅显示到天（与合同列表一致），不拼时分
    return formatTimeValue(ts ?? '', 'date');
  }

  const overviewCards = computed(() => [
    {
      key: 'total',
      label: t('finance.totalContractAmount'),
      value: fmtMoney(overview.value?.totalContractAmount),
      red: false,
    },
    {
      key: 'verified',
      label: t('finance.verifiedAmount'),
      value: fmtMoney(overview.value?.verifiedAmount),
      red: false,
    },
    {
      key: 'pending',
      label: t('finance.pendingAmount'),
      value: fmtMoney(overview.value?.pendingAmount),
      red: Number(overview.value?.pendingAmount ?? 0) > 0,
    },
    {
      key: 'unverified',
      label: t('finance.unverifiedAmount'),
      value: fmtMoney(overview.value?.unverifiedAmount),
      red: Number(overview.value?.unverifiedAmount ?? 0) > 0,
    },
  ]);

  async function loadOverview() {
    try {
      overview.value = await financeOverview();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function loadPage() {
    loading.value = true;
    try {
      const res = await financePage({ current: current.value, pageSize, keyword: keyword.value || undefined });
      customers.value = res.list || [];
      total.value = res.total || 0;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    current.value = 1;
    loadOverview();
    loadPage();
  }

  function toggleCustomer(id: string) {
    const set = new Set(expandedCustomers.value);
    if (set.has(id)) {
      set.delete(id);
    } else {
      set.add(id);
    }
    expandedCustomers.value = set;
  }

  function toggleContract(id: string) {
    const set = new Set(expandedContracts.value);
    if (set.has(id)) {
      set.delete(id);
    } else {
      set.add(id);
    }
    expandedContracts.value = set;
  }

  // 核销
  const verifyModal = reactive<{
    show: boolean;
    loading: boolean;
    record: FinancePaymentRecord | null;
    contract: FinanceContract | null;
    remark: string;
  }>({
    show: false,
    loading: false,
    record: null,
    contract: null,
    remark: '',
  });
  const proofFileList = ref<UploadFileInfo[]>([]);
  // naive-ui 的 fileList 只保留白名单字段（id/name/status…），自定义字段会被剥离，
  // 所以上传成功的附件 id 单独用 file.id 做 key 记录，提交时从这里取。
  const uploadedProofIds = ref<Record<string, string>>({});
  const voucherPreview = reactive<{ show: boolean; src: string }>({ show: false, src: '' });

  function bankAccountTypeLabel(type?: string) {
    if (type === BankAccountTypeEnum.WECHAT) return t('finance.bankAccountTypeWechat');
    if (type === BankAccountTypeEnum.ALIPAY) return t('finance.bankAccountTypeAlipay');
    if (type === BankAccountTypeEnum.BANK_CARD) return t('finance.bankAccountTypeBankCard');
    return '';
  }

  const bankAccountText = computed(() => {
    const { record } = verifyModal;
    if (!record || (!record.bankAccountName && !record.bankAccountNo)) return '';
    return [record.bankAccountName, bankAccountTypeLabel(record.bankAccountType), record.bankAccountNo]
      .filter(Boolean)
      .join(' · ');
  });

  function isImage(type?: string) {
    return /(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(type || '');
  }

  function voucherUrl(voucher: { id: string }) {
    return `${PreviewAttachmentUrl}/${voucher.id}?userId=${userStore.userInfo.id}`;
  }

  function previewVoucher(voucher: { id: string }) {
    voucherPreview.src = voucherUrl(voucher);
    voucherPreview.show = true;
  }

  async function downloadVoucher(voucher: { id: string; name: string }) {
    try {
      const res = await downloadAttachment(voucher.id);
      const url = URL.createObjectURL(new Blob([res], { type: 'application/octet-stream' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = voucher.name;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function openVerify(record: FinancePaymentRecord, contract: FinanceContract) {
    verifyModal.record = record;
    verifyModal.contract = contract;
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
      await financeVerify({
        id: verifyModal.record.id,
        remark: verifyModal.remark || undefined,
        proofAttachmentIds: proofAttachmentIds.length ? proofAttachmentIds : undefined,
      });
      Message.success(t('finance.verifySuccess'));
      verifyModal.show = false;
      await Promise.all([loadOverview(), loadPage()]);
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
    record: FinancePaymentRecord | null;
    remark: string;
  }>({
    show: false,
    loading: false,
    record: null,
    remark: '',
  });

  function openRevoke(record: FinancePaymentRecord) {
    revokeModal.record = record;
    revokeModal.remark = '';
    revokeModal.show = true;
  }

  async function submitRevoke() {
    if (!revokeModal.record) return;
    revokeModal.loading = true;
    try {
      await financeRevoke({ id: revokeModal.record.id, remark: revokeModal.remark || undefined });
      Message.success(t('finance.revokeSuccess'));
      revokeModal.show = false;
      await Promise.all([loadOverview(), loadPage()]);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      revokeModal.loading = false;
    }
  }

  onMounted(() => {
    loadOverview();
    loadPage();
  });
</script>

<style lang="less" scoped>
  .finance-page {
    display: flex;
    padding: 16px;
    height: 100%;
    flex-direction: column;
    gap: 12px;
    .overview-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      .overview-card {
        padding: 16px;
        border: 1px solid var(--text-n8);
        border-radius: 8px;
        background-color: var(--text-n10);
        .overview-label {
          margin-bottom: 8px;
          font-size: 12px;
          color: var(--text-n4);
        }
        .overview-value {
          font-size: 20px;
          font-weight: 600;
          color: var(--text-n1);
          &.is-red {
            color: #e5484d;
          }
        }
      }
    }
    .search-bar {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .customer-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .customer-card {
      overflow: hidden;
      border: 1px solid var(--text-n8);
      border-radius: 8px;
      background-color: var(--text-n10);
      .customer-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        cursor: pointer;
        .customer-name {
          font-size: 15px;
          font-weight: 600;
          color: var(--text-n1);
        }
        .customer-amounts {
          display: flex;
          align-items: center;
          gap: 20px;
          font-size: 13px;
          color: var(--text-n3);
          b {
            margin-left: 6px;
            font-weight: 600;
            color: var(--text-n1);
            &.is-red {
              color: #e5484d;
            }
          }
        }
      }
      .contract-list {
        display: flex;
        padding: 12px;
        border-top: 1px solid var(--text-n8);
        background-color: var(--text-n9);
        flex-direction: column;
        gap: 8px;
      }
    }
    .contract-card {
      overflow: hidden;
      border: 1px solid var(--text-n8);
      border-radius: 6px;
      background-color: var(--text-n10);
      .contract-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 16px;
        cursor: pointer;
        .contract-number {
          font-size: 12px;
          color: var(--text-n4);
        }
        .contract-name {
          font-size: 14px;
          font-weight: 500;
          color: var(--text-n1);
        }
        .contract-amounts {
          display: flex;
          align-items: center;
          gap: 16px;
          font-size: 12px;
          color: var(--text-n3);
          b {
            margin-left: 4px;
            font-weight: 600;
            color: var(--text-n1);
            &.is-red {
              color: #e5484d;
            }
          }
        }
      }
      .record-table {
        padding: 8px 16px;
        border-top: 1px solid var(--text-n8);
        .record-row {
          display: grid;
          align-items: center;
          padding: 8px 0;
          font-size: 12px;
          border-bottom: 1px solid var(--text-n8);
          color: var(--text-n3);
          grid-template-columns: 100px 1fr 120px 140px 90px 110px;
          gap: 8px;
          &:last-child {
            border: none;
          }
          &.record-row-head {
            color: var(--text-n4);
          }
        }
        .record-empty {
          padding: 16px 0;
          font-size: 12px;
          text-align: center;
          color: var(--text-n4);
        }
      }
    }
    .pagination-bar {
      display: flex;
      justify-content: flex-end;
    }
  }
  .verify-form {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .verify-row {
    display: flex;
    align-items: center;
    gap: 8px;
    .verify-label {
      width: 70px;
      font-size: 13px;
      text-align: right;
      color: var(--text-n2);
      flex-shrink: 0;
    }
    .verify-value {
      flex: 1;
      min-width: 0;
      font-size: 13px;
      color: var(--text-n1);
    }
    &.verify-row-top {
      align-items: flex-start;
      .verify-label {
        line-height: 34px;
      }
    }
  }
  .voucher-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    .voucher-item {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
    }
    .voucher-thumb {
      overflow: hidden;
      border-radius: 4px;
      flex-shrink: 0;
      cursor: zoom-in;
    }
    .voucher-name {
      font-size: 13px;
      color: var(--text-n2);
    }
  }
</style>
