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
                          size="tiny"
                          type="primary"
                          @click.stop="openVerify(record)"
                        >
                          {{ t('finance.verify') }}
                        </n-button>
                        <n-button v-else size="tiny" quaternary type="error" @click.stop="openRevoke(record)">
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
      <div class="modal-info">
        <div>{{ t('finance.recordName') }}：{{ verifyModal.record?.name || '-' }}</div>
        <div>{{ t('finance.recordAmount') }}：{{ fmtMoney(verifyModal.record?.recordAmount) }}</div>
        <div>{{ t('finance.recordTime') }}：{{ fmtTime(verifyModal.record?.recordEndTime) }}</div>
      </div>
      <div class="form-item">
        <div class="form-label">{{ t('finance.proof') }}</div>
        <n-upload v-model:file-list="proofFileList" multiple :custom-request="uploadProof" :default-upload="false">
          <n-button>{{ t('finance.uploadProof') }}</n-button>
        </n-upload>
      </div>
      <div class="form-item">
        <div class="form-label">{{ t('finance.verifyRemark') }}</div>
        <n-input
          v-model:value="verifyModal.remark"
          type="textarea"
          :rows="3"
          :placeholder="t('finance.verifyRemarkPlaceholder')"
        />
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

    <!-- 撤回弹窗 -->
    <n-modal v-model:show="revokeModal.show" preset="card" :title="t('finance.revokeTitle')" class="!w-[480px]">
      <div class="modal-info">
        <div>{{ t('finance.recordName') }}：{{ revokeModal.record?.name || '-' }}</div>
        <div>{{ t('finance.recordAmount') }}：{{ fmtMoney(revokeModal.record?.recordAmount) }}</div>
      </div>
      <div class="form-item">
        <div class="form-label">{{ t('finance.revokeRemark') }}</div>
        <n-input
          v-model:value="revokeModal.remark"
          type="textarea"
          :rows="3"
          :placeholder="t('finance.revokeRemarkPlaceholder')"
        />
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
  import { NButton, NEmpty, NInput, NModal, NPagination, NSpin, NTag, NUpload, useMessage } from 'naive-ui';

  import type { FinanceCustomerGroup, FinanceOverview, FinancePaymentRecord } from '@lib/shared/api/modules/finance';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { financeOverview, financePage, financeRevoke, financeVerify, uploadTempAttachment } from '@/api/modules';

  import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();

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
    if (!ts) return '-';
    const d = new Date(ts);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
      d.getMinutes()
    )}`;
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
    remark: string;
  }>({
    show: false,
    loading: false,
    record: null,
    remark: '',
  });
  const proofFileList = ref<UploadFileInfo[]>([]);

  function openVerify(record: FinancePaymentRecord) {
    verifyModal.record = record;
    verifyModal.remark = '';
    proofFileList.value = [];
    verifyModal.show = true;
  }

  async function uploadProof(options: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(options.file.file as File);
      const [attachmentId] = res.data;
      (options.file as unknown as { attachmentId?: string }).attachmentId = attachmentId;
      options.onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      options.onError();
    }
  }

  async function submitVerify() {
    if (!verifyModal.record) return;
    verifyModal.loading = true;
    try {
      const proofAttachmentIds = proofFileList.value
        .map((f) => (f as unknown as { attachmentId?: string }).attachmentId)
        .filter(Boolean) as string[];
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
    @apply flex h-full flex-col gap-[12px] p-[16px];
    .overview-grid {
      @apply grid grid-cols-4 gap-[12px];
      .overview-card {
        @apply rounded-[8px] border border-[var(--text-n8)] bg-[var(--text-n10)] p-[16px];
        .overview-label {
          @apply mb-[8px] text-[12px] text-[var(--text-n4)];
        }
        .overview-value {
          @apply text-[20px] font-semibold text-[var(--text-n1)];
          &.is-red {
            color: #e5484d;
          }
        }
      }
    }
    .search-bar {
      @apply flex items-center gap-[8px];
    }
    .customer-list {
      @apply flex flex-col gap-[12px];
    }
    .customer-card {
      @apply overflow-hidden rounded-[8px] border border-[var(--text-n8)] bg-[var(--text-n10)];
      .customer-header {
        @apply flex cursor-pointer items-center justify-between px-[16px] py-[12px];
        .customer-name {
          @apply text-[15px] font-semibold text-[var(--text-n1)];
        }
        .customer-amounts {
          @apply flex items-center gap-[20px] text-[13px] text-[var(--text-n3)];
          b {
            @apply ml-[6px] font-semibold text-[var(--text-n1)];
            &.is-red {
              color: #e5484d;
            }
          }
        }
      }
      .contract-list {
        @apply flex flex-col gap-[8px] border-t border-[var(--text-n8)] bg-[var(--text-n9)] p-[12px];
      }
    }
    .contract-card {
      @apply overflow-hidden rounded-[6px] border border-[var(--text-n8)] bg-[var(--text-n10)];
      .contract-header {
        @apply flex cursor-pointer items-center justify-between px-[16px] py-[10px];
        .contract-number {
          @apply text-[12px] text-[var(--text-n4)];
        }
        .contract-name {
          @apply text-[14px] font-medium text-[var(--text-n1)];
        }
        .contract-amounts {
          @apply flex items-center gap-[16px] text-[12px] text-[var(--text-n3)];
          b {
            @apply ml-[4px] font-semibold text-[var(--text-n1)];
            &.is-red {
              color: #e5484d;
            }
          }
        }
      }
      .record-table {
        @apply border-t border-[var(--text-n8)] px-[16px] py-[8px];
        .record-row {
          @apply grid grid-cols-[100px_1fr_120px_140px_90px_110px] items-center gap-[8px] border-b border-[var(--text-n8)] py-[8px] text-[12px] text-[var(--text-n3)];
          &:last-child {
            @apply border-none;
          }
          &.record-row-head {
            @apply text-[var(--text-n4)];
          }
        }
        .record-empty {
          @apply py-[16px] text-center text-[12px] text-[var(--text-n4)];
        }
      }
    }
    .pagination-bar {
      @apply flex justify-end;
    }
    .modal-info {
      @apply mb-[12px] flex flex-col gap-[6px] rounded-[6px] bg-[var(--text-n9)] p-[12px] text-[13px] text-[var(--text-n2)];
    }
    .form-item {
      @apply mb-[16px];
      .form-label {
        @apply mb-[8px] text-[13px] text-[var(--text-n2)];
      }
    }
  }
</style>
