<template>
  <div class="dm-profile-panel flex h-full flex-col gap-[16px] overflow-y-auto p-[16px]">
    <template v-if="loading">
      <div class="flex flex-1 items-center justify-center">
        <n-spin />
      </div>
    </template>

    <template v-else-if="!hasDmProfile">
      <div class="flex flex-1 flex-col items-center justify-center gap-[8px]">
        <n-empty :description="t('customer.profileNoDmProfile')" />
        <span class="text-[13px] text-orange-500">{{ t('customer.profileNoDmProfileHint') }}</span>
      </div>
    </template>

    <template v-else>
      <CrmCard :title="t('customer.profileDmProfile')" hide-footer auto-height>
        <CrmDescription :descriptions="descriptions" :column="2" :one-line-value="false" />
        <CrmDescription class="mt-[16px]" :descriptions="summaryDescription" :column="1" :one-line-value="false" />
      </CrmCard>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NEmpty, NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DmProfileSnapshot } from '@lib/shared/models/ai';
  import type { CustomerProfileResponse } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription from '@/components/pure/crm-description/index.vue';

  import { getCustomerProfile } from '@/api/modules';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();

  const loading = ref(false);
  const profile = ref<CustomerProfileResponse>();

  async function load() {
    if (!props.sourceId) {
      return;
    }
    loading.value = true;
    try {
      profile.value = await getCustomerProfile(props.sourceId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function normalize(value?: string): string {
    if (!value || value === '未知' || value.toLowerCase() === 'unknown') {
      return '';
    }
    return value;
  }

  const dmProfile = computed<DmProfileSnapshot>(() => (profile.value?.dmProfile as DmProfileSnapshot) || {});

  const hasDmProfile = computed(() => !!profile.value?.dmProfile);

  const descriptions = computed(() => {
    const p = dmProfile.value;
    let groupText = '';
    if (p.isGroup === true) {
      groupText = t('dmProfile.group');
    } else if (p.isGroup === false) {
      groupText = t('dmProfile.personal');
    }
    return [
      { label: t('dmProfile.colNickname'), value: normalize(p.nickname) },
      { label: t('dmProfile.profile.name'), value: normalize(p.name) },
      { label: t('dmProfile.profile.gender'), value: normalize(p.gender) },
      { label: t('dmProfile.profile.age'), value: normalize(p.age) },
      { label: t('dmProfile.profile.city'), value: normalize(p.city) },
      { label: t('dmProfile.profile.accountCategory'), value: normalize(p.accountCategory) },
      { label: t('dmProfile.profile.category'), value: normalize(p.category) },
      { label: t('dmProfile.profile.preference'), value: normalize(p.preference) },
      { label: t('dmProfile.profile.confidence'), value: normalize(p.confidence) },
      { label: t('dmProfile.colPhone'), value: normalize(p.phone) },
      { label: t('dmProfile.colWechat'), value: normalize(p.wechat) },
      { label: t('dmProfile.profile.bio'), value: normalize(p.bio) },
      { label: t('dmProfile.colUniqueId'), value: normalize(p.uniqueId) },
      { label: t('dmProfile.colFollower'), value: normalize(p.followerCount) },
      { label: t('dmProfile.profile.following'), value: normalize(p.followingCount) },
      { label: t('dmProfile.profile.liked'), value: normalize(p.likedCount) },
      { label: t('dmProfile.colType'), value: groupText },
    ];
  });

  // 话术汇总内容较长，单独在最下方独占一行展示，避免和其他字段挤在两列里
  const summaryDescription = computed(() => [
    { label: t('dmProfile.profile.summary'), value: normalize(dmProfile.value.summary) },
  ]);

  watch(
    () => props.sourceId,
    () => load(),
    { immediate: true }
  );
</script>

<style lang="less" scoped></style>
