<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="login-wrapper w-full">
      <div class="login-title text-[28px] font-semibold text-[var(--text-n1)]">{{ t('login.form.title') }}</div>
    </div>
    <div class="flex-1">
      <van-form ref="formRef" required class="crm-form">
        <van-cell-group inset>
          <van-field
            v-model="userInfo.username"
            :required="false"
            name="username"
            :label="t('login.form.username')"
            :placeholder="t('login.form.userName.placeholderOther')"
            :rules="[{ required: true, message: t('login.form.userName.errMsg') }]"
            class="!p-[16px] !text-[16px]"
          />
          <van-field
            v-model="userInfo.password"
            name="password"
            :required="false"
            :type="visible ? undefined : 'password'"
            :label="t('login.form.password')"
            :placeholder="t('login.form.password.placeholder')"
            :rules="[{ required: true, message: t('login.form.password.errMsg') }]"
            class="!p-[16px] !text-[16px]"
          >
            <template #button>
              <CrmIcon
                :name="visible ? 'iconicon_browse' : 'iconicon_browse_off'"
                width="24px"
                height="24px"
                color="var(--text-n4)"
                @click="handleToggleVisible"
              />
            </template>
          </van-field>
          <van-field
            v-model="userInfo.captchaCode"
            name="captchaCode"
            :required="false"
            :label="t('login.form.captcha')"
            :placeholder="t('login.form.captcha.placeholder')"
            :rules="[{ required: true, message: t('login.form.captcha.errMsg') }]"
            class="!p-[16px] !text-[16px]"
          >
            <template #button>
              <img :src="captchaImage" class="h-[32px] w-[100px]" alt="captcha" @click="refreshCaptcha" />
            </template>
          </van-field>
        </van-cell-group>
      </van-form>
      <div class="p-[16px]">
        <van-button block type="primary" :loading="loading" native-type="submit" @click="login">
          {{ t('login.form.login') }}
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { FormInstance } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { clearToken, getLoginType, isLoginExpires, setLoginExpires, setLoginType } from '@lib/shared/method/auth';
  import { encrypted } from '@lib/shared/method/index';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { getCaptcha } from '@/api/modules';
  import useAppStore from '@/store/modules/app';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const router = useRouter();

  const appStore = useAppStore();

  const userStore = useUserStore();

  const { t } = useI18n();

  const userInfo = ref<{
    authenticate: string;
    username: string;
    password: string;
    captchaId: string;
    captchaCode: string;
  }>({
    authenticate: getLoginType() || 'LOCAL',
    username: '',
    password: '',
    captchaId: '',
    captchaCode: '',
  });

  const captchaImage = ref('');
  async function refreshCaptcha() {
    userInfo.value.captchaCode = '';
    try {
      const res = await getCaptcha();
      userInfo.value.captchaId = res.captchaId;
      captchaImage.value = res.captchaImage;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const visible = ref(false);
  function handleToggleVisible() {
    visible.value = !visible.value;
  }
  const formRef = ref<FormInstance>();
  const loading = ref(false);

  async function login() {
    try {
      await formRef.value?.validate();
      loading.value = true;

      await userStore.login({
        username: encrypted(userInfo.value.username) || '',
        password: encrypted(userInfo.value.password) || '',
        authenticate: userInfo.value.authenticate,
        platform: 'MOBILE',
        captchaId: userInfo.value.captchaId,
        captchaCode: userInfo.value.captchaCode,
      });
      setLoginExpires();
      setLoginType(userInfo.value.authenticate);
      router.replace({
        name: AppRouteEnum.WORKBENCH,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      refreshCaptcha();
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    appStore.initPublicKey();
    refreshCaptcha();
    try {
      if (isLoginExpires()) {
        clearToken();
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  });
</script>

<style scoped lang="less">
  .login-wrapper {
    padding: 16px 16px 0 20px;
    height: 150px;
    background-image: url('/images/login_bg.png');
    @apply relative w-full bg-cover bg-no-repeat;
    .login-title {
      bottom: 16px;
      @apply absolute;
    }
  }
</style>
