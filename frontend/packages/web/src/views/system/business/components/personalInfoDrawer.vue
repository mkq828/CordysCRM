<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :title="t('system.personal.info.title')"
    :footer="false"
    :show-back="true"
    :closable="false"
    :body-content-class="bodyClass"
  >
    <n-scrollbar>
      <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" @change="searchData()" />
      </CrmCard>
      <!-- 版本与授权（租户自助查看，admin 无组织不展示） -->
      <CrmCard
        v-if="activeTab === PersonalEnum.INFO && !userStore.isAdmin"
        hide-footer
        :special-height="64"
        class="mb-[16px]"
      >
        <div class="flex font-medium text-[var(--text-n1)]">
          <n-p>{{ t('system.personal.subscription') }}</n-p>
        </div>
        <div
          class="mt-[16px] grid w-full grid-cols-4 gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[24px]"
        >
          <div class="flex flex-col gap-[4px]">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.subscription.version') }}</n-p>
            <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ subscription.versionName || '-' }}</n-p>
          </div>
          <div class="flex flex-col gap-[4px]">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.subscription.status') }}</n-p>
            <n-p class="m-[0]">
              <n-tag :type="subscriptionStatusTag" size="small" :bordered="false">{{ subscriptionStatusLabel }}</n-tag>
            </n-p>
          </div>
          <div class="flex flex-col gap-[4px]">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.subscription.expireTime') }}</n-p>
            <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ formatExpireTime }}</n-p>
          </div>
          <div class="flex flex-col gap-[4px]">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.subscription.remainDays') }}</n-p>
            <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ remainDaysLabel }}</n-p>
          </div>
        </div>

        <template v-if="subscription.contract">
          <div class="mt-[16px] flex font-medium text-[var(--text-n1)]">
            <n-p>{{ t('system.personal.contract') }}</n-p>
          </div>
          <div
            class="mt-[8px] grid w-full grid-cols-4 gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[24px]"
          >
            <div class="flex flex-col gap-[4px]">
              <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.contract.contractNo') }}</n-p>
              <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ subscription.contract.contractNo || '-' }}</n-p>
            </div>
            <div class="flex flex-col gap-[4px]">
              <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.contract.orgName') }}</n-p>
              <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ subscription.contract.orgName || '-' }}</n-p>
            </div>
            <div class="flex flex-col gap-[4px]">
              <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.contract.amount') }}</n-p>
              <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ fmtMoney(subscription.contract.amount) }}</n-p>
            </div>
            <div class="flex flex-col gap-[4px]">
              <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.contract.validityDays') }}</n-p>
              <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ validityDaysLabel }}</n-p>
            </div>
            <div class="flex flex-col gap-[4px]">
              <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.contract.signType') }}</n-p>
              <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ signTypeLabel }}</n-p>
            </div>
            <div class="flex flex-col gap-[4px]">
              <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.contract.status') }}</n-p>
              <n-p class="m-[0] font-medium text-[var(--text-n1)]">{{ contractStatusLabel }}</n-p>
            </div>
          </div>
          <div v-if="subscription.contract.attachmentList?.length" class="mt-[8px] flex items-start gap-[12px]">
            <span class="shrink-0 pt-[6px] text-[var(--text-n4)]">{{ t('system.personal.contract.attachment') }}</span>
            <div class="flex flex-wrap gap-[8px]">
              <div
                v-for="att in subscription.contract.attachmentList"
                :key="att.id"
                class="flex items-center gap-[8px]"
              >
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
                <n-button size="tiny" text type="primary" @click="downloadAttachmentById(att.id, att.name)">
                  {{ t('common.download') }}
                </n-button>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="mt-[8px] text-[13px] text-[var(--text-n4)]">{{ t('system.personal.noContract') }}</div>
      </CrmCard>
      <n-image-preview v-model:show="preview.show" :src="preview.src" />
      <CrmCard v-if="activeTab === PersonalEnum.INFO" hide-footer :special-height="64">
        <div class="flex font-medium text-[var(--text-n1)]">
          <n-p>{{ t('common.baseInfo') }}</n-p>
        </div>
        <div class="flex w-full items-center gap-[8px] py-[16px]">
          <CrmAvatar />
          <div class="flex-1">
            <div class="text-[var(--text-n1)]">{{ personalInfo.userName }}</div>
            <div class="flex items-center gap-[4px]">
              <n-tag
                v-if="personalInfo.userId === 'admin'"
                :bordered="false"
                size="small"
                :color="{
                  color: 'var(--primary-6)',
                  textColor: 'var(--primary-8)',
                }"
              >
                {{ t('common.admin') }}
              </n-tag>
              <template v-else>
                <CrmTag
                  v-for="role in personalInfo.roles"
                  :key="role.id"
                  :bordered="false"
                  size="small"
                  :color="{
                    color: 'var(--primary-6)',
                    textColor: 'var(--primary-8)',
                  }"
                >
                  {{ role.name }}
                </CrmTag>
              </template>
            </div>
          </div>
        </div>
        <div
          class="grid w-full grid-cols-3 gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[24px]"
        >
          <div class="flex">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.phone') }}</n-p>
            <n-p class="mx-[8px] my-[0] text-[var(--text-n1)]">{{ personalInfo.phone }}</n-p>
          </div>
          <div class="flex">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.email') }}</n-p>
            <n-p class="mx-[8px] my-[0] text-[var(--text-n1)]">{{ personalInfo.email }}</n-p>
          </div>
          <div v-if="!userStore.isAdmin" class="flex">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.department') }}</n-p>
            <n-p class="mx-[8px] my-[0] text-[var(--text-n1)]">{{ personalInfo.departmentName }}</n-p>
          </div>
        </div>
        <div class="py-[24px]">
          <n-button type="primary" ghost class="mx-[8px]" @click="edit">
            {{ t('common.edit') }}
          </n-button>
          <n-button @click="changePassword">
            {{ t('system.personal.changePassword') }}
          </n-button>
        </div>
      </CrmCard>
      <CrmCard v-if="activeTab === PersonalEnum.MY_PLAN" no-content-padding hide-footer :special-height="64">
        <FollowDetail
          :show-add="
            hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE', 'CLUE_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:UPDATE'])
          "
          :refresh-key="refreshKey"
          active-type="followPlan"
          wrapper-class="h-[calc(100vh-168px)]"
          virtual-scroll-height="calc(100vh - 260px)"
          follow-api-key="myPlan"
          source-id="NULL"
          :any-permission="['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ']"
        />
      </CrmCard>
      <apiKey v-if="activeTab === PersonalEnum.API_KEY" />
    </n-scrollbar>
  </CrmDrawer>
  <EditPersonalInfoModal v-model:show="showEditPersonalModal" :integration="currentInfo" @init-sync="searchData()" />
  <EditPasswordModal v-model:show="showEditPasswordModal" @init-sync="searchData()" />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NImage, NImagePreview, NP, NScrollbar, NTag, TabPaneProps } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { PersonalInfoRequest, TenantSubscription } from '@lib/shared/models/system/business';
  import { OrgUserInfo } from '@lib/shared/models/system/org';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import apiKey from './apiKey.vue';
  import EditPasswordModal from '@/views/system/business/components/editPasswordModal.vue';
  import EditPersonalInfoModal from '@/views/system/business/components/editPersonalInfoModal.vue';

  import { downloadAttachment, getPersonalInfo, getSubscription } from '@/api/modules';
  import { defaultUserInfo } from '@/config/business';
  import useModal from '@/hooks/useModal.js';
  import { useUserStore } from '@/store';
  import useLicenseStore from '@/store/modules/setting/license.js';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const userStore = useUserStore();
  const { openModal } = useModal();
  const licenseStore = useLicenseStore();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeTab = defineModel<PersonalEnum>('activeTabValue', {
    required: false,
    default: PersonalEnum.INFO,
  });

  const personalInfo = ref<OrgUserInfo>({
    ...defaultUserInfo,
  });

  // 租户套餐与合同（版本与授权）
  const subscription = ref<TenantSubscription>({});
  const preview = reactive<{ show: boolean; src: string }>({ show: false, src: '' });

  async function loadSubscription() {
    if (userStore.isAdmin) return;
    try {
      subscription.value = (await getSubscription()) || {};
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function fmtMoney(v?: number | string) {
    const n = Number(v ?? 0);
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
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

  const subscriptionStatusLabel = computed(() => {
    if (subscription.value.inGrace) return t('system.personal.subscription.inGrace');
    const s = subscription.value.status;
    return s ? t(`system.personal.subscription.status.${s}`) : '-';
  });
  const subscriptionStatusTag = computed(() => {
    if (subscription.value.inGrace) return 'warning';
    switch (subscription.value.status) {
      case 'ACTIVE':
        return 'success';
      case 'FREE':
        return 'info';
      case 'EXPIRED':
        return 'error';
      default:
        return 'default';
    }
  });
  const formatExpireTime = computed(() => {
    const ts = subscription.value.expireTime;
    return ts ? dayjs(ts).format('YYYY-MM-DD') : '-';
  });
  const remainDaysLabel = computed(() => {
    const d = subscription.value.remainDays;
    if (d == null) return '-';
    if (d >= 0) return `${d}${t('system.personal.subscription.day')}`;
    return `${t('system.personal.subscription.inGrace')}${-d}${t('system.personal.subscription.day')}`;
  });
  const validityDaysLabel = computed(() => {
    const d = subscription.value.contract?.validityDays;
    return d == null ? '-' : `${d}${t('system.personal.subscription.day')}`;
  });
  const signTypeLabel = computed(() => {
    const s = subscription.value.contract?.signType;
    return s ? t(`system.personal.contract.signType.${s}`) : '-';
  });
  const contractStatusLabel = computed(() => {
    const s = subscription.value.contract?.status;
    return s ? t(`system.personal.contract.status.${s}`) : '-';
  });

  const currentInfo = ref<PersonalInfoRequest>({
    phone: '',
    email: '',
  });

  const bodyClass = ref<string>('crm-drawer-content');

  const showEditPersonalModal = ref<boolean>(false); // 已配置
  const showEditPasswordModal = ref<boolean>(false); // 已配置

  const tabList = computed<TabPaneProps[]>(() => {
    return [
      {
        name: PersonalEnum.INFO,
        tab: t('system.personal.info'),
      },
      {
        name: PersonalEnum.MY_PLAN,
        tab: t('system.personal.plan'),
      },
      ...(hasAnyPermission(['PERSONAL_API_KEY:READ'])
        ? [
            {
              name: PersonalEnum.API_KEY,
              tab: t('system.personal.apiKey'),
            },
          ]
        : []),
    ];
  });

  async function searchData() {
    if (activeTab.value === PersonalEnum.INFO) {
      personalInfo.value = await getPersonalInfo();
      loadSubscription();
    }
  }
  function edit() {
    currentInfo.value.email = personalInfo.value.email;
    currentInfo.value.phone = personalInfo.value.phone;
    showEditPersonalModal.value = true;
  }

  function changePassword() {
    showEditPasswordModal.value = true;
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        searchData();
      }
    }
  );

  watch(
    () => activeTab.value,
    (val) => {
      if (val === PersonalEnum.INFO) {
        searchData();
      }
    }
  );

  const refreshKey = ref(0);
</script>

<style scoped lang="less"></style>
