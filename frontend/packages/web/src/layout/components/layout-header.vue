<template>
  <n-layout-header class="flex" bordered>
    <div class="flex min-w-[180px] max-w-[300px] justify-center px-[24px] py-[14px]">
      <img :src="innerLogo" class="h-[28px]" />
    </div>
    <div class="flex flex-1 items-center justify-between px-[16px]">
      <CrmTopMenu />
      <div v-if="!props.isPreview" class="flex items-center gap-[8px]">
        <n-dropdown :options="dataScopeOptions" trigger="click" @select="handleDataScopeSelect">
          <n-button class="px-[12px] font-medium" quaternary>
            {{ dataScopeLabel }}
            <template #icon>
              <CrmIcon type="iconicon_chevron_down" :size="12" />
            </template>
          </n-button>
        </n-dropdown>
        <CrmButtonGroup not-show-divider class="gap-[8px]" :list="appStore.getNavTopConfigList">
          <template #searchSlot>
            <n-button
              v-if="showSearch"
              class="p-[8px]"
              quaternary
              @click="
                () => {
                  initDuplicateCheckDrawer = true;
                  showDuplicateCheckDrawer = true;
                }
              "
            >
              <template #icon>
                <CrmIcon type="iconicon_search-outline_outlined" :size="16" />
              </template>
            </n-button>
            <span v-else></span>
          </template>
          <template #taskSlot>
            <n-button
              class="p-[8px]"
              quaternary
              @click="
                () => {
                  initTaskDrawer = true;
                  showTaskDrawer = true;
                }
              "
            >
              <n-badge value="1" dot :show="appStore.todoStatistic.total > 0">
                <CrmIcon type="iconicon_contract" :size="16" />
              </n-badge>
            </n-button>
          </template>
          <template #eventSlot>
            <n-button
              v-permission="['CUSTOMER_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ']"
              class="p-[8px]"
              quaternary
              @click="
                () => {
                  initFollowDrawer = true;
                  showFollowDrawer = true;
                }
              "
            >
              <template #icon>
                <CrmIcon type="iconicon_data_plan" :size="16" />
              </template>
            </n-button>
          </template>
          <template #languageSlot>
            <n-popselect
              v-model:value="currentLocale"
              :options="LOCALE_OPTIONS"
              trigger="hover"
              @update-value="changeLanguage"
            >
              <n-button class="p-[8px]" quaternary>
                <template #icon>
                  <LanguageOutline />
                </template>
              </n-button>
            </n-popselect>
          </template>
          <template #alertsSlot>
            <n-button class="p-[8px]" quaternary @click="showMessage">
              <n-badge value="1" dot :show="showBadge">
                <CrmIcon type="iconicon-alarmclock" :size="16" />
              </n-badge>
            </n-button>
          </template>
          <template #agentSlot>
            <n-button class="p-[8px]" quaternary @click="showAgent">
              <CrmSvg name="icon_bot" :size="16" />
            </n-button>
          </template>
          <template #versionInfoSlot>
            <n-popover position="left" content-class="w-[320px]" class="!p-[16px]">
              <div class="flex flex-col gap-[8px]">
                <img :src="innerLogo" class="h-[22px] max-w-[100px] object-contain object-left" />
                <div
                  class="flex cursor-pointer items-center gap-[8px] text-[14px] text-[var(--color-text-1)]"
                  @click="copyVersion(appStore.versionInfo.currentVersion)"
                >
                  <div class="text-[12px] leading-[20px]">
                    {{ t('settings.help.currentVersion') }}
                  </div>
                  <div class="font-semibold">
                    {{ appStore.versionInfo.currentVersion }} ({{ appStore.versionInfo.architecture }})
                  </div>
                </div>
                <div
                  class="flex cursor-pointer items-center gap-[8px] text-[14px] text-[var(--color-text-1)]"
                  @click="copyVersion(appStore.versionInfo.latestVersion)"
                >
                  <div class="text-[12px] leading-[20px]">
                    {{ t('settings.help.latestVersion') }}
                  </div>
                  <div class="font-semibold">{{ appStore.versionInfo.latestVersion }}</div>
                </div>
                <div v-if="hasAnyPermission(['LICENSE:READ'])" class="flex flex-col gap-[4px]">
                  <n-divider class="!my-0" />
                  <div class="flex flex-col gap-[8px]">
                    <div class="flex items-center justify-between">
                      <div class="flex items-center gap-[8px]">
                        <div class="font-semibold text-[var(--text-n1)]">License</div>
                        <CrmTag
                          v-if="licenseStore.isEnterpriseVersion()"
                          tooltip-disabled
                          :type="getLicenseStatus.status"
                          theme="light"
                        >
                          {{ getLicenseStatus.title }}
                        </CrmTag>
                      </div>
                      <n-button v-permission="['LICENSE:EDIT']" text type="primary" @click="handleUpdate">
                        {{ t('common.update') }}
                      </n-button>
                    </div>
                    <div v-if="licenseStore.isEnterpriseVersion()" class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.customerName') }}
                      </div>
                      <div class="font-semibold">{{ licenseStore?.licenseInfo?.corporation }}</div>
                    </div>
                    <div v-if="licenseStore.isEnterpriseVersion()" class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.productionVersion') }}
                      </div>
                      <div class="font-semibold">
                        {{
                          licenseVersionMap[licenseStore?.licenseInfo?.edition as keyof typeof licenseVersionMap] ??
                          licenseVersionMap.Standard
                        }}
                      </div>
                    </div>
                    <div v-if="licenseStore.isEnterpriseVersion()" class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.LicenseAccountCount') }}
                      </div>
                      <div class="font-semibold">
                        {{ licenseStore?.licenseInfo?.count ?? '-' }}
                      </div>
                    </div>
                    <div v-if="licenseStore.isEnterpriseVersion()" class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.authorizationTime') }}
                      </div>
                      <div class="font-semibold">{{ licenseStore?.licenseInfo?.expired ?? '-' }}</div>
                    </div>
                  </div>
                </div>
                <div class="flex items-center justify-between">
                  <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">哆咪CRM</div>
                  <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                    {{ appStore.versionInfo.copyright }}
                  </div>
                </div>
                <div v-if="appStore.versionInfo.operator" class="flex items-center justify-between">
                  <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">运营方</div>
                  <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">{{
                    appStore.versionInfo.operator
                  }}</div>
                </div>
                <n-divider class="!my-0" />
                <div class="flex items-center gap-[12px]">
                  <a
                    class="cursor-pointer text-[12px] text-[var(--text-n4)] hover:text-[var(--primary)]"
                    @click="openAgreement('service')"
                  >
                    {{ t('settings.help.agreementService') }}
                  </a>
                  <a
                    class="cursor-pointer text-[12px] text-[var(--text-n4)] hover:text-[var(--primary)]"
                    @click="openAgreement('privacy')"
                  >
                    {{ t('settings.help.agreementPrivacy') }}
                  </a>
                </div>
              </div>
              <template #trigger>
                <n-button class="p-[8px]" quaternary>
                  <template #icon>
                    <n-badge value="1" dot :show="appStore.versionInfo.hasNewVersion">
                      <CrmIcon type="iconicon_info_circle" :size="16" />
                    </n-badge>
                  </template>
                </n-button>
              </template>
            </n-popover>
          </template>
          <template #helpSlot>
            <CrmMoreAction placement="bottom-end" :options="moreActions" @select="selectMoreActions">
              <n-button class="p-[8px]" quaternary>
                <template #icon>
                  <CrmIcon type="iconicon_help_circle" :size="16" />
                </template>
              </n-button>
            </CrmMoreAction>
          </template>
        </CrmButtonGroup>
        <n-tooltip placement="bottom">
          <template #trigger>
            <n-button v-permission="['PAID_USER:READ']" class="p-[8px]" quaternary @click="goPlanApplication">
              <template #icon>
                <n-badge :value="appStore.pendingApplicationCount" :show="appStore.pendingApplicationCount > 0">
                  <CrmIcon type="iconicon_wallet" :size="16" />
                </n-badge>
              </template>
            </n-button>
          </template>
          {{ t('planApplication.pendingBadge') }}
        </n-tooltip>
      </div>
    </div>
    <MessageDrawer v-model:show="showMessageDrawer" />
    <licenseDrawer v-model:visible="showLicenseDrawer" />
    <Suspense>
      <CrmDuplicateCheckDrawer v-if="initDuplicateCheckDrawer" v-model:visible="showDuplicateCheckDrawer" />
    </Suspense>
  </n-layout-header>
  <agentDrawer v-if="initAgentDrawer" v-model:visible="showAgentDrawer" />
  <CrmFollowDrawer v-if="initFollowDrawer" v-model:visible="showFollowDrawer" />
  <CrmTaskDrawer v-if="initTaskDrawer" v-model:show="showTaskDrawer" />
  <AgreementModal v-model:show="showAgreement" v-model:active-key="agreementActiveKey" />
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import {
    NBadge,
    NButton,
    NDivider,
    NDropdown,
    NLayoutHeader,
    NPopover,
    NPopselect,
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import { LanguageOutline } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { LOCALE_OPTIONS } from '@lib/shared/locale';
  import useLocale from '@lib/shared/locale/useLocale';
  import { LocaleType } from '@lib/shared/types/global';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { lastScopedOptions } from '@/components/business/crm-duplicate-check-drawer/config';
  import CrmTaskDrawer from '@/components/business/crm-task-drawer/index.vue';
  import CrmTopMenu from '@/components/business/crm-top-menu/index.vue';
  import AgreementModal from '@/components/agreement/AgreementModal.vue';
  import licenseDrawer from '@/views/system/license/licenseDrawer.vue';
  import MessageDrawer from '@/views/system/message/components/messageDrawer.vue';

  import { addApiKey, changeLocaleBackEnd } from '@/api/modules';
  import { defaultPlatformLogo } from '@/config/business';
  import useLegacyCopy from '@/hooks/useLegacyCopy';
  import useModal from '@/hooks/useModal';
  import useAppStore from '@/store/modules/app';
  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  import { SystemRouteEnum, WorkbenchRouteEnum } from '@/enums/routeEnum';

  const agentDrawer = defineAsyncComponent(() => import('@/components/business/crm-agent-drawer/index.vue'));
  const CrmFollowDrawer = defineAsyncComponent(() => import('@/components/business/crm-follow-drawer/index.vue'));
  const CrmDuplicateCheckDrawer = defineAsyncComponent(
    () => import('@/components/business/crm-duplicate-check-drawer/index.vue')
  );

  const route = useRoute();
  const router = useRouter();

  const { loading } = useMessage();
  const { t } = useI18n();
  const { changeLocale, currentLocale } = useLocale(loading);
  const { openModal } = useModal();
  const Message = useMessage();

  const appStore = useAppStore();
  const userStore = useUserStore();
  const licenseStore = useLicenseStore();

  const props = defineProps<{
    isPreview?: boolean;
    logo?: string;
  }>();

  function changeLanguage(locale: LocaleType) {
    changeLocaleBackEnd(locale);
    changeLocale(locale);
  }

  const dataScopeOptions = computed(() => [
    { label: t('common.dataScope.self'), key: 'SELF' },
    { label: t('common.dataScope.org'), key: 'ORG' },
  ]);
  const dataScopeLabel = computed(() =>
    appStore.dataScope === 'SELF' ? t('common.dataScope.self') : t('common.dataScope.org')
  );
  function handleDataScopeSelect(key: string) {
    appStore.setDataScope(key as 'SELF' | 'ORG');
  }

  const showBadge = computed(() => {
    return !appStore.messageInfo.read;
  });

  const showMessageDrawer = ref(false);
  function showMessage() {
    showMessageDrawer.value = true;
  }

  function goPlanApplication() {
    router.push({ name: SystemRouteEnum.SYSTEM_PLAN_APPLICATION });
  }

  const showLicenseDrawer = ref(false);
  function handleUpdate() {
    showLicenseDrawer.value = true;
  }
  const getLicenseStatus = computed<{
    title: string;
    status: 'default' | 'error' | 'warning' | 'primary' | 'info' | 'success';
  }>(() => {
    switch (licenseStore.licenseInfo?.status) {
      case 'valid':
        return {
          title: t('system.license.valid'),
          status: 'success',
        };
      case 'expired':
        return {
          title: t('system.license.invalid'),
          status: 'error',
        };
      default:
        return {
          title: t('system.license.failure'),
          status: 'error',
        };
    }
  });

  const hasValidApiKey = computed(() => userStore.apiKeyList.some((key) => !key.isExpire && key.enable));
  const showAgentDrawer = ref(false);
  const initAgentDrawer = ref(false);
  function showAgent() {
    if (hasValidApiKey.value) {
      initAgentDrawer.value = true;
      showAgentDrawer.value = true;
    } else {
      openModal({
        title: t('common.noEnableApiKey'),
        type: 'warning',
        content: t('common.noEnableApiKeyTip'),
        showCancelButton: true,
        positiveText: t('common.add'),
        negativeText: t('common.cancel'),
        positiveButtonProps: { type: 'primary', size: 'medium' },
        onPositiveClick: async () => {
          await addApiKey();
          Message.success(t('common.newSuccess'));
          await userStore.initApiKeyList();
          showAgentDrawer.value = true;
        },
      });
    }
  }

  const showSearch = computed(() => lastScopedOptions.value.length);
  const initDuplicateCheckDrawer = ref(false);
  const showDuplicateCheckDrawer = ref(false);

  const initFollowDrawer = ref(false);
  const showFollowDrawer = ref(false);
  const initTaskDrawer = ref(false);
  const showTaskDrawer = ref(false);

  const { legacyCopy } = useLegacyCopy();
  function copyVersion(version: string) {
    legacyCopy(version);
  }

  const showAgreement = ref(false);
  const agreementActiveKey = ref<'service' | 'ip' | 'privacy'>('service');

  function openAgreement(key: 'service' | 'ip' | 'privacy' = 'service') {
    agreementActiveKey.value = key;
    showAgreement.value = true;
  }

  const moreActions: ActionsItem[] = [
    {
      label: t('settings.help.doc'),
      key: 'helpDoc',
      iconType: 'iconicon_help_circle',
    },
    {
      label: t('settings.help.apiDoc'),
      key: 'apiDoc',
      iconType: 'iconicon_info_circle',
    },
  ];

  function selectMoreActions(item: ActionsItem) {
    switch (item.key) {
      case 'helpDoc':
        window.open(appStore.pageConfig.helpDoc, '_blank');
        break;
      case 'apiDoc':
        const apiDocUrl = `${window.location.origin}/swagger-ui/index.html`;
        window.open(apiDocUrl, '_blank');
        break;
      default:
        break;
    }
  }

  const licenseVersionMap: Record<string, string> = {
    Standard: t('system.license.LicenseStandard'),
    Enterprise: t('system.license.LicenseEnterprise'),
    Professional: t('system.license.LicenseProfessional'),
  };

  onBeforeMount(() => {
    appStore.getVersion();
    if (route.name !== WorkbenchRouteEnum.WORKBENCH_BOARD) {
      appStore.initMessage();
    }
    appStore.connectSystemMessageSSE();
    userStore.initApiKeyList();
    appStore.initPendingApplicationCount();
  });

  const innerLogo = computed(() =>
    props.isPreview && props.logo ? props.logo : appStore.pageConfig.logoPlatform[0]?.url ?? defaultPlatformLogo
  );
</script>

<style lang="less" scoped></style>
