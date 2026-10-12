<template>
  <img :src="currentSrc" :alt="alt" :loading="loading" :class="className" @error="onError" />
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  import { getDramaImageStylePreviewCandidates } from './templates';

  const props = withDefaults(
    defineProps<{
      styleId: string;
      alt?: string;
      className?: string;
      loading?: 'lazy' | 'eager';
    }>(),
    { alt: '', className: '', loading: 'lazy' }
  );

  const candidates = computed(() => getDramaImageStylePreviewCandidates(props.styleId));
  const index = ref(0);
  const currentSrc = computed(() => candidates.value[Math.min(index.value, candidates.value.length - 1)]);

  watch(
    () => props.styleId,
    () => {
      index.value = 0;
    }
  );

  function onError() {
    if (index.value < candidates.value.length - 1) {
      index.value += 1;
    }
  }
</script>
