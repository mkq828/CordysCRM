<template>
  <CrmOverviewDrawer
    ref="crmOverviewDrawerRef"
    v-model:show="show"
    v-model:active-tab="activeTab"
    :tab-list="tabList"
    :button-list="buttonList"
    :title="sourceName"
    :form-key="FormDesignKeyEnum.CUSTOMER"
    :source-id="props.sourceId"
    :formViewSize="formViewSize"
    default-layout="vertical"
    show-tab-setting
    @button-pop-update="handleTransferPopUpdate"
    @button-select="handleButtonSelect"
    @saved="handleSaved"
  >
    <template #transferPopContent>
      <TransferForm
        ref="transferFormRef"
        v-model:form="transferForm"
        :module-type="ModuleConfigEnum.CUSTOMER_MANAGEMENT"
      />
    </template>
    <template #left>
      <div class="flex h-full flex-col overflow-hidden">
        <div
          class="flex shrink-0 flex-wrap items-center gap-[8px] border-b border-[var(--divider-color)] px-[16px] py-[8px]"
        >
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('customer.tags') }}</span>
          <CrmTag
            v-for="tag in customerTags"
            :key="tag.id || tag.name"
            size="small"
            theme="dark"
            :color="tagColorOf(tag)"
            tooltip-disabled
          >
            {{ tag.name }}
          </CrmTag>
          <span v-if="!customerTags.length" class="text-[12px] text-[var(--text-n4)]">{{
            t('customer.tagsEmpty')
          }}</span>
        </div>
        <div class="min-h-0 flex-1 overflow-hidden">
          <CrmFormCreate
            ref="formCreateRef"
            embedded
            is-edit
            :need-init-detail="true"
            :source-id="props.sourceId"
            :form-key="FormDesignKeyEnum.CUSTOMER"
            :columns="layout === 'vertical' ? 3 : undefined"
            :readonly="collaborationType === 'READ_ONLY' || !hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE'])"
            class="p-[16px_24px]"
            @init="handleFormInit"
            @saved="handleSaved"
            @ready="handleFormReady"
          />
        </div>
      </div>
    </template>
    <template #right>
      <div class="h-full pt-[16px]">
        <customerProfilePanel v-if="activeTab === 'profile'" :source-id="props.sourceId" />
        <dmProfilePanel v-else-if="activeTab === 'dmProfile'" :source-id="props.sourceId" />
        <ContactTable
          v-else-if="activeTab === 'contact'"
          :refresh-key="refreshKey"
          :source-id="props.sourceId"
          :initial-source-name="sourceName"
          :readonly="collaborationType === 'READ_ONLY' || props.readonly"
          :form-key="FormDesignKeyEnum.CUSTOMER_CONTACT"
        />
        <FollowDetail
          v-else-if="['followRecord', 'followPlan'].includes(activeTab) && show"
          :active-type="(activeTab as 'followRecord'| 'followPlan')"
          wrapper-class="h-[calc(100vh-162px)]"
          virtual-scroll-height="calc(100vh - 254px)"
          :follow-api-key="FormDesignKeyEnum.CUSTOMER"
          :source-id="props.sourceId"
          :refresh-key="refreshKey"
          :initial-source-name="sourceName"
          :show-add="
            collaborationType !== 'READ_ONLY' && hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) && !props.readonly
          "
          :show-action="
            collaborationType !== 'READ_ONLY' && hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) && !props.readonly
          "
          :parentFormKey="FormDesignKeyEnum.CUSTOMER"
        />
        <CrmHeaderTable
          v-else-if="activeTab === 'headRecord'"
          :form-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
          :source-id="props.sourceId"
          :load-list-api="getCustomerHeaderList"
        />
        <customerRelation
          v-else-if="activeTab === 'relation'"
          :source-id="props.sourceId"
          :readonly="collaborationType === 'READ_ONLY' || props.readonly"
        />
        <CrmCard v-else-if="activeTab === 'opportunityInfo'" no-content-bottom-padding hide-footer>
          <opportunityTable
            :source-id="props.sourceId"
            :customer-name="sourceName"
            is-customer-tab
            :form-key="FormDesignKeyEnum.CUSTOMER_OPPORTUNITY"
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
          />
        </CrmCard>
        <collaborator
          v-else-if="activeTab === 'collaborator'"
          :source-id="props.sourceId"
          :readonly="collaborationType === 'READ_ONLY' || props.readonly"
        />
        <ContractTimeline
          v-else-if="activeTab === 'contract'"
          :form-key="FormDesignKeyEnum.CONTRACT"
          :source-id="props.sourceId"
        />
        <ContractTimeline
          v-else-if="activeTab === 'contractPayment'"
          :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
          :source-id="props.sourceId"
        />
        <ContractTimeline
          v-else-if="activeTab === 'contractPaymentRecord'"
          :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD"
          :source-id="props.sourceId"
        />
        <ContractTimeline
          v-else-if="activeTab === 'invoice'"
          :form-key="FormDesignKeyEnum.INVOICE"
          :source-id="props.sourceId"
        />
        <CrmCard v-else-if="activeTab === 'order'" hide-footer no-content-bottom-padding>
          <OrderTable
            :formKey="FormDesignKeyEnum.CUSTOMER_ORDER"
            :sourceId="props.sourceId"
            isCustomerTab
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
            @open-contract-drawer="handleOpenContractDrawer"
          />
        </CrmCard>
      </div>
      <CrmMoveModal
        v-model:show="showMoveModal"
        :reason-key="ReasonTypeEnum.CUSTOMER_POOL_RS"
        :source-id="props.sourceId"
        :name="sourceName"
        type="warning"
        @refresh="handleRemoveSuccess"
      />
      <ContractDetailDrawer
        v-model:visible="showContractDetailDrawer"
        :sourceId="activeSourceId"
        @showCustomerDrawer="handleOpenCustomerDrawer"
      />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CustomerTag } from '@lib/shared/models/customer';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import collaborator from './collaborator.vue';
  import customerProfilePanel from './customerProfilePanel.vue';
  import customerRelation from './customerRelation.vue';
  import dmProfilePanel from './dmProfilePanel.vue';
  import ContractTimeline from '@/views/contract/contract/components/contractTimeline.vue';
  import ContractDetailDrawer from '@/views/contract/contract/components/detail.vue';
  import opportunityTable from '@/views/opportunity/components/opportunityTable.vue';
  import OrderTable from '@/views/order/order/components/orderTable.vue';

  import {
    deleteCustomer,
    fetchCustomerBusinessInfo,
    getCustomerHeaderList,
    getCustomerTags,
    updateCustomer,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const FollowDetail = defineAsyncComponent(() => import('@/components/business/crm-follow-detail/index.vue'));
  const CrmFormCreate = defineAsyncComponent(() => import('@/components/business/crm-form-create/index.vue'));

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'saved'): void;
    (e: 'deleted'): void;
    (e: 'transfer'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const crmOverviewDrawerRef = ref<InstanceType<typeof CrmOverviewDrawer>>();
  const layout = computed(() => crmOverviewDrawerRef.value?.layout);

  const refreshKey = ref(0);
  const transferLoading = ref(false);
  const fetchBusinessInfoLoading = ref(false);
  const collaborationType = ref<CollaborationType>();
  const sourceName = ref('');
  const customerTags = ref<CustomerTag[]>([]);

  async function loadCustomerTags() {
    if (!props.sourceId) {
      return;
    }
    try {
      customerTags.value = await getCustomerTags(props.sourceId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  // 标签按状态区分颜色：后端返回的语义色直接填充为深色底 + 白字，未配置时兜底橙色
  function tagColorOf(tag: CustomerTag): { color: string; borderColor: string; textColor: string } {
    const color = tag.color || '#f97316';
    return { color, borderColor: color, textColor: '#ffffff' };
  }

  watch(
    () => [show.value, props.sourceId],
    () => {
      if (show.value && props.sourceId) {
        loadCustomerTags();
      }
    },
    { immediate: true }
  );
  const formCreateRef = ref<InstanceType<typeof CrmFormCreate>>();
  const buttonList = computed<ActionsItem[]>(() => {
    if (collaborationType.value || props.readonly) {
      return [];
    }
    return [
      {
        label: t('customer.fetchBusinessInfo'),
        key: 'fetchBusinessInfo',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        disabled: fetchBusinessInfoLoading.value,
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      {
        label: t('common.save'),
        key: 'save',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      {
        label: t('common.transfer'),
        key: 'transfer',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:TRANSFER'],
        popConfirmProps: {
          loading: transferLoading.value,
          title: t('common.transfer'),
          positiveText: t('common.confirm'),
          iconType: 'primary',
        },
        popSlotContent: 'transferPopContent',
      },
      {
        label: t('customer.moveToOpenSea'),
        key: 'moveToOpenSea',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
      },
      {
        label: t('common.delete'),
        key: 'delete',
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:DELETE'],
      },
    ];
  });

  const activeTab = ref('contact');
  const tabList = computed<TabContentItem[]>(() => {
    const fullList = [
      {
        name: 'profile',
        tab: t('customer.profile'),
        enable: true,
      },
      {
        name: 'dmProfile',
        tab: t('customer.profileDmProfile'),
        enable: true,
      },
      {
        name: 'followRecord',
        tab: t('crmFollowRecord.followRecord'),
        enable: true,
      },
      {
        name: 'contact',
        tab: t('opportunity.contactInfo'),
        enable: true,
        permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
      },
      {
        name: 'followPlan',
        tab: t('common.plan'),
        enable: true,
      },
      {
        name: 'headRecord',
        tab: t('common.headRecord'),
        enable: true,
      },
      {
        name: 'relation',
        tab: t('customer.relation'),
        enable: true,
      },
      {
        name: 'opportunityInfo',
        tab: t('customer.opportunityInfo'),
        enable: true,
        permission: ['OPPORTUNITY_MANAGEMENT:READ'],
      },
      {
        name: 'collaborator',
        tab: t('customer.collaborator'),
        enable: true,
      },
      {
        name: 'contract',
        tab: t('module.contract'),
        enable: true,
        permission: ['CONTRACT:READ'],
      },
      {
        name: 'contractPayment',
        tab: t('module.paymentPlan'),
        enable: true,
        permission: ['CONTRACT_PAYMENT_PLAN:READ'],
      },
      {
        name: 'contractPaymentRecord',
        tab: t('module.paymentRecord'),
        enable: true,
        permission: ['CONTRACT_PAYMENT_RECORD:READ'],
      },
      {
        name: 'invoice',
        tab: t('module.invoice'),
        enable: true,
        permission: ['CONTRACT_INVOICE:READ'],
      },
      {
        name: 'order',
        tab: t('module.order'),
        enable: true,
        permission: ['ORDER:READ'],
      },
    ];
    if (collaborationType.value) {
      return fullList.filter((item) => item.name !== 'collaborator');
    }
    return fullList;
  });

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferForm = ref<any>({
    owner: null,
    belongToPublicPool: null,
  });

  function resetTransferForm() {
    transferForm.value = {
      owner: null,
      belongToPublicPool: null,
    };
  }

  function handleTransferPopUpdate(key: string, visible: boolean) {
    if (key === 'transfer' && visible) {
      resetTransferForm();
    }
  }

  // 转移
  async function transfer() {
    try {
      transferLoading.value = true;
      await updateCustomer({
        id: props.sourceId,
        owner: transferForm.value.owner,
      });
      Message.success(t('common.transferSuccess'));
      resetTransferForm();
      formCreateRef.value?.reload();
      emit('transfer');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      transferLoading.value = false;
    }
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('customer.deleteTitleTip'),
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomer(props.sourceId);
          Message.success(t('common.deleteSuccess'));
          emit('deleted');
          show.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 移入公海
  const showMoveModal = ref(false);
  function handleMoveToPublicPool() {
    showMoveModal.value = true;
  }

  // 获取工商信息：企查查为主、联网兜底，写入后刷新表单
  async function handleFetchBusinessInfo() {
    if (fetchBusinessInfoLoading.value) {
      return;
    }
    fetchBusinessInfoLoading.value = true;
    try {
      const result = await fetchCustomerBusinessInfo(props.sourceId);
      const filled = result?.filled || [];
      if (filled.length) {
        // 联网兜底结果可能不准（可能是同名公司），用警示口径；qcc 权威源走成功口径
        const fromWeb = result?.source === 'web' || result?.source === 'mixed';
        if (fromWeb) {
          Message.warning(t('customer.fetchBusinessInfoFromWeb', { fields: filled.join('、') }));
        } else {
          Message.success(t('customer.fetchBusinessInfoSuccess', { fields: filled.join('、') }));
        }
      } else {
        Message.warning(t('customer.fetchBusinessInfoEmpty'));
      }
      formCreateRef.value?.reload();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      fetchBusinessInfoLoading.value = false;
    }
  }

  function handleButtonSelect(key: string) {
    if (key === 'save') {
      formCreateRef.value?.submit();
    } else if (key === 'delete') {
      handleDelete();
    } else if (key === 'pop-transfer') {
      transfer();
    } else if (key === 'moveToOpenSea') {
      handleMoveToPublicPool();
    } else if (key === 'fetchBusinessInfo') {
      handleFetchBusinessInfo();
    }
  }

  function handleSaved() {
    refreshKey.value += 1;
    emit('saved');
  }

  const formViewSize = ref<FormViewSize>('large');
  function handleFormInit(_title: string, viewSize?: FormViewSize) {
    formViewSize.value = viewSize || 'large';
  }

  function handleFormReady(_sourceName?: string, _collaborationType?: CollaborationType) {
    collaborationType.value = _collaborationType;
    sourceName.value = _sourceName || '';
  }

  const showContractDetailDrawer = ref(false);
  const activeSourceId = ref<string>('');
  function handleOpenContractDrawer(params: { id: string }) {
    activeSourceId.value = params.id;
    showContractDetailDrawer.value = true;
  }

  function handleRemoveSuccess() {
    show.value = false;
    emit('deleted');
  }

  function handleOpenCustomerDrawer() {
    showContractDetailDrawer.value = false;
  }
</script>

<style lang="less" scoped></style>
