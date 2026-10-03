<template>
  <div class="tilt-card" @mousemove="onMove" @mouseleave="onLeave">
    <div class="tilt-card__inner" :style="transformStyle">
      <span class="tilt-card__glare" :style="glareStyle"></span>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';

  const rx = ref(0);
  const ry = ref(0);
  const gx = ref(50);
  const gy = ref(50);

  const transformStyle = computed(() => `perspective(800px) rotateX(${rx.value}deg) rotateY(${ry.value}deg)`);
  const glareStyle = computed(
    () => `background: radial-gradient(circle at ${gx.value}% ${gy.value}%, rgb(255 255 255 / 40%), transparent 60%)`
  );

  function onMove(event: MouseEvent) {
    const el = event.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const px = (event.clientX - rect.left) / rect.width;
    const py = (event.clientY - rect.top) / rect.height;
    ry.value = (px - 0.5) * 16;
    rx.value = -(py - 0.5) * 16;
    gx.value = px * 100;
    gy.value = py * 100;
  }

  function onLeave() {
    rx.value = 0;
    ry.value = 0;
  }
</script>

<style scoped lang="less">
  .tilt-card {
    perspective: 800px;
  }
  .tilt-card__inner {
    position: relative;
    overflow: hidden;
    transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1);
    transform-style: preserve-3d;
    will-change: transform;
  }
  .tilt-card__glare {
    position: absolute;
    inset: 0;
    z-index: 2;
    opacity: 0;
    pointer-events: none;
    transition: opacity 200ms cubic-bezier(0.23, 1, 0.32, 1);
  }

  @media (hover: hover) and (pointer: fine) {
    .tilt-card:hover .tilt-card__glare {
      opacity: 1;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .tilt-card__inner,
    .tilt-card__glare {
      transition: none;
    }
  }
</style>
