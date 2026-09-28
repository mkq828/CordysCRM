<template>
  <!-- 宽限期提醒：屏幕居中、醒目，登录后弹一次（不是顶部横幅，避免像浏览器授权提示） -->
  <n-modal
    v-model:show="showGraceModal"
    preset="dialog"
    type="warning"
    :title="t('paidUser.graceBannerTitle')"
    :positive-text="t('paidUser.graceBannerConfirm')"
    :mask-closable="false"
    @positive-click="showGraceModal = false"
  >
    <div class="text-[14px] leading-6">{{ t('paidUser.graceBanner') }}</div>
  </n-modal>
  <n-layout class="default-layout">
    <LayoutHeader
      v-if="
        !route.name?.toString().includes(DashboardRouteEnum.DASHBOARD) &&
        !route.name?.toString().includes(TenderRouteEnum.TENDER)
      "
      :is-preview="innerProps.isPreview"
      :logo="innerLogo"
    />
    <n-layout class="flex-1" has-sider>
      <LayoutSider @open-personal-info="handleOpenPersonalInfo" />
      <PageContent />
    </n-layout>
  </n-layout>
  <PersonalInfoDrawer v-model:visible="showPersonalInfo" :active-tab-value="personalTab" />
  <!-- AI 聊天机器人悬浮入口已隐藏（用户要求） -->
  <!-- <AiChatFloatingEntry /> -->
  <FeedbackBug />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { NLayout, NModal } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  // import AiChatFloatingEntry from '@/components/business/ai-chat/components/AiChatFloatingEntry.vue';
  import FeedbackBug from '@/components/business/feedback-bug/index.vue';
  import LayoutHeader from './components/layout-header.vue';
  import LayoutSider from './components/layout-sider.vue';
  import PageContent from './page-content.vue';
  import PersonalInfoDrawer from '@/views/system/business/components/personalInfoDrawer.vue';

  import { defaultPlatformLogo } from '@/config/business';
  import useUserStore from '@/store/modules/user';

  import { DashboardRouteEnum, TenderRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const { t } = useI18n();
  const userStore = useUserStore();
  const inGrace = computed(() => !!userStore.userInfo.planInGrace);
  // 宽限期提示：登录进入后居中弹一次，点「知道了」后本次会话不再弹。
  // 用 watch(immediate) 而不是 onMounted：planInGrace 由 /login、/is-login 每次实时返回，
  // 刷新时值在 onMounted 之后才到达（异步 is-login），onMounted 读不到；且不落盘（见 user store），
  // 账号从宽限期变停用后不会有旧值残留，避免和「服务已到期」重复提示。
  const showGraceModal = ref(false);

  watch(
    inGrace,
    (val) => {
      if (val) showGraceModal.value = true;
    },
    { immediate: true }
  );

  interface Props {
    isPreview?: boolean;
    logo?: string;
  }

  const props = defineProps<Props>();

  const innerProps = ref<Props>(props);
  const personalTab = ref(PersonalEnum.INFO);
  const showPersonalInfo = ref<boolean>(false);

  function handleOpenPersonalInfo(tab: PersonalEnum) {
    personalTab.value = tab;
    showPersonalInfo.value = true;
  }

  watch(
    () => props.logo,
    () => {
      innerProps.value = { ...props };
    }
  );
  const innerLogo = computed(() =>
    props.isPreview && innerProps.value.logo ? innerProps.value.logo : defaultPlatformLogo
  );
</script>

<style lang="less">
  .default-layout {
    @apply flex;

    height: 100vh;
    .n-layout-scroll-container {
      @apply flex w-full flex-col overflow-hidden;
    }
  }
</style>
