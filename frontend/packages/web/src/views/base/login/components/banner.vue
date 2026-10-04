<template>
  <div class="banner-wrap">
    <img
      class="img"
      :style="props.isPreview ? 'height: 100%;' : 'height: 100vh'"
      :src="bannerUrl"
      @error="onBannerError"
    />
    <a href="https://cordys.cn/" target="_blank" class="logo-link">
      <img :src="logoUrl" @error="onLogoError" />
    </a>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { defaultLoginImage, defaultLoginLogo } from '@/config/business';
  import { useAppStore } from '@/store';

  const props = defineProps<{
    isPreview?: boolean;
    banner?: string;
    logo?: string;
  }>();

  const appStore = useAppStore();

  const innerBanner = computed(() =>
    props.banner ? props.banner : appStore.pageConfig.loginImage[0]?.url ?? defaultLoginImage
  );

  const innerLogo = computed(() => {
    return props.isPreview && props.logo ? props.logo : appStore.pageConfig.loginLogo[0]?.url ?? defaultLoginLogo;
  });

  // 自定义图片 / logo 加载失败（如附件文件丢失）时回退到默认资源，避免登录页出现破图
  const bannerError = ref(false);
  const logoError = ref(false);
  const bannerUrl = computed(() => (bannerError.value ? defaultLoginImage : innerBanner.value));
  const logoUrl = computed(() => (logoError.value ? defaultLoginLogo : innerLogo.value));

  function onBannerError() {
    bannerError.value = true;
  }

  function onLogoError() {
    logoError.value = true;
  }
</script>

<style lang="less" scoped>
  .banner-wrap {
    @apply relative;
    .img {
      width: 100%;
      object-fit: cover;
    }
    .logo-link {
      @apply absolute;

      top: 5%;
      left: 5%;
      width: 37%;
      height: 7%;
      .logo {
        width: 100%;
        height: 100%;
      }
    }
  }
</style>
