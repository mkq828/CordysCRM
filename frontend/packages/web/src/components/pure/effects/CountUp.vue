<template>
  <span class="count-up" :style="{ fontSize: size }">{{ display }}</span>
</template>

<script setup lang="ts">
  import { onMounted, ref, watch } from 'vue';

  const props = withDefaults(
    defineProps<{
      value: number;
      duration?: number;
      decimals?: number;
      size?: string;
    }>(),
    { duration: 1500, decimals: 0, size: '40px' }
  );

  const display = ref('0');

  function animate() {
    const from = 0;
    const to = props.value;
    const start = performance.now();

    function frame(now: number) {
      const progress = Math.min((now - start) / props.duration, 1);
      const eased = 1 - (1 - progress) ** 3;
      display.value = (from + (to - from) * eased).toFixed(props.decimals);
      if (progress < 1) {
        requestAnimationFrame(frame);
      }
    }

    requestAnimationFrame(frame);
  }

  onMounted(animate);
  watch(() => props.value, animate);
</script>

<style scoped lang="less">
  .count-up {
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
</style>
