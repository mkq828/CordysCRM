<template>
  <CrmCard hide-footer :special-height="licenseStore.expiredDuring ? 128 : 64">
    <n-data-table
      :single-line="false"
      :columns="columns"
      :data="data"
      :paging="false"
      class="message-table"
      :pagination="false"
      :loading="loading"
      :max-height="licenseStore.expiredDuring ? 'calc(100vh - 318px)' : 'calc(100vh - 254px)'"
    />
    <expirationSettingDrawer
      v-model:visible="showExpirationSetting"
      :detail="activeDetail"
      :show-time-setting="activeDetail?.event.includes(showTimeSettingEvent)"
      @ok="initMessageList"
    />
    <n-modal
      v-model:show="showTemplateEditor"
      preset="dialog"
      :title="t('system.message.templateEditTitle')"
      :positive-text="t('common.save')"
      :negative-text="t('common.cancel')"
      @positive-click="saveTemplate"
    >
      <div class="mb-[8px] text-[var(--text-n4)]">{{ t('system.message.templateHint') }}</div>
      <n-input v-model:value="templateText" type="textarea" :rows="6" :placeholder="activeTemplateRow?.eventName" />
    </n-modal>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { DataTableColumn, NDataTable, NInput, NModal, useMessage } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageConfigItem, MessageTaskDetailDTOItem } from '@lib/shared/models/system/message';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import expirationSettingDrawer from './expirationSettingDrawer.vue';
  import SwitchPopConfirm from './switchPopConfirm.vue';

  import { batchSaveMessageTask, getConfigSynchronization, getMessageTask, saveMessageTask } from '@/api/modules';
  import { platFormNameMap, platformType } from '@/config/business';
  import { useAppStore, useUserStore } from '@/store';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  const Message = useMessage();

  const { t } = useI18n();
  const licenseStore = useLicenseStore();
  const appStore = useAppStore();
  const userStore = useUserStore();

  const enableSystemMessage = ref(false);
  const enableEmailMessage = ref(false);
  const enableThirdPartyMessage = ref(false);
  const enableSmsMessage = ref(false);
  const enableSystemLoading = ref(false);
  const noticeEnableMapKey: Record<string, keyof MessageTaskDetailDTOItem> = {
    [CompanyTypeEnum.WECOM]: 'weComEnable',
    [CompanyTypeEnum.DINGTALK]: 'dingTalkEnable',
    [CompanyTypeEnum.LARK]: 'larkEnable',
  };

  const thirdPartyEnableKey = computed<keyof MessageTaskDetailDTOItem>(() => {
    const syncPlatformType = appStore.activePlatformResource.syncResource;
    return noticeEnableMapKey[syncPlatformType];
  });
  const data = ref<MessageConfigItem[]>([]);
  const loading = ref(false);

  async function initMessageList() {
    try {
      loading.value = true;
      const result = await getMessageTask();

      enableSystemMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.sysEnable));
      enableEmailMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.emailEnable));
      enableThirdPartyMessage.value = result.every((e) => {
        return e.messageTaskDetailDTOList.every((c) => c[thirdPartyEnableKey.value]);
      });
      enableSmsMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.smsEnable));

      data.value = result
        .map((item) =>
          item.messageTaskDetailDTOList.map((child) => ({
            ...child,
            ...item,
            moduleName: item.moduleName || item.module,
            eventName: child.eventName || child.event,
          }))
        )
        .flat();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleToggleSystemMessage(row: MessageConfigItem, type: string, cancel?: () => void) {
    try {
      enableSystemLoading.value = true;
      await saveMessageTask({
        module: row.module,
        event: row.event,
        emailEnable: type === 'email' ? !row.emailEnable : row.emailEnable,
        sysEnable: type === 'system' ? !row.sysEnable : row.sysEnable,
        smsEnable: type === 'sms' ? !row.smsEnable : row.smsEnable,
        [thirdPartyEnableKey.value]:
          type === 'weChat' ? !row[thirdPartyEnableKey.value] : row[thirdPartyEnableKey.value],
      });
      Message.success(t('common.saveSuccess'));
      cancel?.();
      initMessageList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      enableSystemLoading.value = false;
    }
  }

  async function toggleGlobalMessage(type: string, cancel?: () => void) {
    try {
      enableSystemLoading.value = true;

      const params: {
        sysEnable: boolean | undefined;
        emailEnable: boolean | undefined;
        weComEnable: boolean | undefined;
        dingTalkEnable: boolean | undefined;
        larkEnable: boolean | undefined;
        smsEnable: boolean | undefined;
      } = {
        sysEnable: undefined,
        emailEnable: undefined,
        weComEnable: undefined,
        dingTalkEnable: undefined,
        larkEnable: undefined,
        smsEnable: undefined,
      };

      if (type === 'system') {
        params.sysEnable = !enableSystemMessage.value;
      } else if (type === 'email') {
        params.emailEnable = !enableEmailMessage.value;
      } else if (type === 'weChat') {
        params[thirdPartyEnableKey.value as keyof typeof params] = !enableThirdPartyMessage.value;
      } else if (type === 'sms') {
        params.smsEnable = !enableSmsMessage.value;
      }

      await batchSaveMessageTask(params);
      Message.success(t('common.saveSuccess'));
      cancel?.();
      initMessageList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      enableSystemLoading.value = false;
    }
  }

  const needSetEndTimeEvent = [
    'BUSINESS_QUOTATION_EXPIRED',
    'BUSINESS_QUOTATION_EXPIRING',
    'CONTRACT_ARCHIVED',
    'CONTRACT_VOID',
    'CONTRACT_EXPIRING',
    'CONTRACT_EXPIRED',
    'CONTRACT_PAYMENT_EXPIRED',
    'CONTRACT_PAYMENT_EXPIRING',
  ];

  const showTimeSettingEvent = 'EXPIRING';

  const isEnableNoticeConfig = ref<boolean>(false);
  const platformName = computed(() => platFormNameMap[appStore.activePlatformResource.syncResource]);
  const isSyncFromThirdChecked = computed(() => appStore.activePlatformResource.sync);

  const showExpirationSetting = ref(false);
  const activeDetail = ref<MessageConfigItem>();
  function settingMessage(e: MouseEvent, row: MessageConfigItem) {
    showExpirationSetting.value = true;
    activeDetail.value = row;
  }

  // 模板文案编辑（通知文案从 i18n 迁到配置表，留空回退系统默认文案）
  const showTemplateEditor = ref(false);
  const activeTemplateRow = ref<MessageConfigItem>();
  const templateText = ref('');
  function editTemplate(row: MessageConfigItem) {
    activeTemplateRow.value = row;
    templateText.value = row.template || '';
    showTemplateEditor.value = true;
  }
  async function saveTemplate() {
    if (!activeTemplateRow.value) return;
    try {
      enableSystemLoading.value = true;
      await saveMessageTask({
        module: activeTemplateRow.value.module,
        event: activeTemplateRow.value.event,
        sysEnable: activeTemplateRow.value.sysEnable,
        emailEnable: activeTemplateRow.value.emailEnable,
        weComEnable: activeTemplateRow.value.weComEnable,
        dingTalkEnable: activeTemplateRow.value.dingTalkEnable,
        larkEnable: activeTemplateRow.value.larkEnable,
        smsEnable: activeTemplateRow.value.smsEnable,
        template: templateText.value,
      });
      Message.success(t('common.saveSuccess'));
      showTemplateEditor.value = false;
      initMessageList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      enableSystemLoading.value = false;
    }
  }
  const columns = computed<DataTableColumn[]>(() => [
    {
      title: t('system.message.Feature'),
      key: 'moduleName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      className: 'message-table-module-name',
      rowSpan: (rowData: { messageTaskDetailDTOList?: MessageConfigItem[] }) => {
        return rowData?.messageTaskDetailDTOList?.length ?? 0;
      },
    },
    {
      title: t('system.message.notificationScenario'),
      key: 'eventName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => {
        const actions: any[] = [];
        if (hasAnyPermission(['SYSTEM_NOTICE:UPDATE'])) {
          if (needSetEndTimeEvent.includes(row.event as string)) {
            actions.push(
              h(CrmIcon, {
                type: 'iconicon_set_up',
                size: 16,
                class: 'cursor-pointer text-[var(--primary-8)]',
                onClick: (e: MouseEvent) => settingMessage(e, row as unknown as MessageConfigItem),
              })
            );
          }
          actions.push(
            h(CrmIcon, {
              type: 'iconicon_edit',
              size: 16,
              class: 'cursor-pointer text-[var(--primary-8)]',
              onClick: () => editTemplate(row as unknown as MessageConfigItem),
            })
          );
        }
        return h(
          'div',
          {
            class: 'one-line-text flex items-center justify-between gap-[8px] w-[196px]',
          },
          [h('span', row.eventName as string), h('div', { class: 'flex items-center gap-[8px]' }, actions)]
        );
      },
    },
    {
      title: () => {
        return h(SwitchPopConfirm, {
          title: t('system.message.confirmCloseSystemNotify'),
          titleColumnText: t('system.message.systemMessage'),
          value: enableSystemMessage.value,
          loading: enableSystemLoading.value,
          content: t('system.message.confirmCloseSystemNotifyContent'),
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
          onChange: (cancel?: () => void) => toggleGlobalMessage('system', cancel),
        });
      },
      key: 'systemMessage',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => {
        return h(SwitchPopConfirm, {
          title: t('system.message.confirmCloseSystemNotify'),
          value: row.sysEnable as boolean,
          loading: enableSystemLoading.value,
          content: t('system.message.confirmCloseSystemNotifyContent'),
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
          onChange: (cancel?: () => void) =>
            handleToggleSystemMessage(row as unknown as MessageConfigItem, 'system', cancel),
        });
      },
    },
    // 邮件/短信通道仅平台管理员可见：邮件要租户自配 SMTP、短信是平台级凭据且第一版未接入，
    // 对租户是「空头开关」，直接隐藏，避免误导。
    ...(userStore.isAdmin
      ? ([
          {
            title: () => {
              return h(SwitchPopConfirm, {
                titleColumnText: t('system.message.emailReminder'),
                value: enableEmailMessage.value,
                loading: enableSystemLoading.value,
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
                onChange: (cancel?: () => void) => toggleGlobalMessage('email', cancel),
              });
            },
            key: 'emailReminder',
            width: 200,
            ellipsis: {
              tooltip: true,
            },
            render: (row: any) => {
              return h(SwitchPopConfirm, {
                value: row.emailEnable as boolean,
                loading: enableSystemLoading.value,
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
                onChange: (cancel?: () => void) =>
                  handleToggleSystemMessage(row as unknown as MessageConfigItem, 'email', cancel),
              });
            },
          },
          {
            title: () => {
              return h(SwitchPopConfirm, {
                titleColumnText: t('system.message.smsMessage'),
                value: enableSmsMessage.value,
                loading: enableSystemLoading.value,
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
                onChange: (cancel?: () => void) => toggleGlobalMessage('sms', cancel),
              });
            },
            key: 'smsMessage',
            width: 200,
            ellipsis: {
              tooltip: true,
            },
            render: (row: any) => {
              return h(SwitchPopConfirm, {
                value: row.smsEnable as boolean,
                loading: enableSystemLoading.value,
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
                onChange: (cancel?: () => void) =>
                  handleToggleSystemMessage(row as unknown as MessageConfigItem, 'sms', cancel),
              });
            },
          },
        ] as DataTableColumn[])
      : []),
    ...(isEnableNoticeConfig.value
      ? [
          {
            title: () => {
              return h(SwitchPopConfirm, {
                title: t('system.message.confirmCloseWeChatNotice'),
                titleColumnText: t('system.message.platformNotice', { type: platformName.value }),
                value: enableThirdPartyMessage.value,
                loading: enableSystemLoading.value,
                content: t('system.message.confirmCloseSystemNotifyContent'),
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) || !isSyncFromThirdChecked.value,
                toolTipContent: !isSyncFromThirdChecked.value
                  ? t('system.message.weComSwitchTip', { type: platformName.value })
                  : '',
                onChange: (cancel?: () => void) => {
                  if (!isSyncFromThirdChecked.value) return;
                  toggleGlobalMessage('weChat', cancel);
                },
              });
            },
            key: thirdPartyEnableKey.value,
            width: 200,
            ellipsis: {
              tooltip: true,
            },
            render: (row: any) => {
              return h(SwitchPopConfirm, {
                title: t('system.message.confirmCloseWeChatNotice'),
                value: row[thirdPartyEnableKey.value] as boolean,
                loading: enableSystemLoading.value,
                content: t('system.message.confirmCloseSystemNotifyContent'),
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) || !isSyncFromThirdChecked.value,
                toolTipContent: !isSyncFromThirdChecked.value
                  ? t('system.message.weComSwitchTip', { type: platformName.value })
                  : '',
                onChange: (cancel?: () => void) => {
                  if (!isSyncFromThirdChecked.value) return;
                  handleToggleSystemMessage(row as unknown as MessageConfigItem, 'weChat', cancel);
                },
              });
            },
          },
        ]
      : []),
  ]);

  async function initIntegration() {
    try {
      const res = await getConfigSynchronization();
      if (res) {
        const platFormConfig = res.find(
          (item) => platformType.includes(item.type) && item.type === appStore.activePlatformResource.syncResource
        );
        isEnableNoticeConfig.value =
          !!platFormConfig && !!platFormConfig.config && !!platFormConfig.config?.startEnable;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    // TODO license 先放开
    // if (licenseStore.hasLicense()) {
    //   initIntegration();
    // }
    initIntegration();
    initMessageList();
  });
</script>

<style lang="less" scoped>
  :deep(.message-table) {
    .n-data-table-thead {
      .n-data-table-tr {
        th {
          background: var(--text-n9);
        }
      }
    }
    .message-table-module-name {
      &.n-data-table-td {
        background: var(--text-n9);
      }
    }
  }
</style>
