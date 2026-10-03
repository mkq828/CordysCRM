<template>
  <div class="workbench" @mousemove="onMove" @mouseleave="onLeave">
    <div class="workbench__scene" :style="sceneStyle">
      <div class="workbench__grid"></div>
      <div class="workbench__panel workbench__panel--1"><slot name="panel1">面板 A</slot></div>
      <div class="workbench__panel workbench__panel--2"><slot name="panel2">面板 B</slot></div>
      <div class="workbench__panel workbench__panel--3"><slot name="panel3">面板 C</slot></div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';

  const rx = ref(0);
  const ry = ref(0);

  const sceneStyle = computed(() => `transform: rotateX(${-8 + rx.value}deg) rotateY(${ry.value}deg)`);

  function onMove(event: MouseEvent) {
    const el = event.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const px = (event.clientX - rect.left) / rect.width - 0.5;
    const py = (event.clientY - rect.top) / rect.height - 0.5;
    ry.value = px * 14;
    rx.value = -py * 8;
  }

  function onLeave() {
    rx.value = 0;
    ry.value = 0;
  }
</script>

<style scoped lang="less">
  .workbench {
    position: relative;
    overflow: hidden;
    height: 260px;
    border-radius: 14px;
    background: linear-gradient(145deg, #0c1523, #131f35);
    perspective: 1000px;
  }
  .workbench__scene {
    position: absolute;
    inset: 0;
    transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1);
    transform-style: preserve-3d;
  }
  .workbench__grid {
    position: absolute;
    inset: 30px -20px -40px;
    background-image: linear-gradient(rgb(100 180 255 / 20%) 1px, transparent 1px),
      linear-gradient(90deg, rgb(100 180 255 / 20%) 1px, transparent 1px);
    background-size: 32px 32px;
    transform: rotateX(60deg);
    transform-origin: center top;
  }
  .workbench__panel {
    --tz: 0;

    position: absolute;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 14px 18px;
    font-size: 14px;
    font-weight: 600;
    border: 1px solid rgb(100 180 255 / 40%);
    border-radius: 12px;
    color: #ffffff;
    background: rgb(16 30 50 / 85%);
    box-shadow: 0 12px 30px rgb(0 0 0 / 45%);
    backdrop-filter: blur(10px);
    animation: workbench-float 5s ease-in-out infinite;
  }
  .workbench__panel--1 {
    top: 20%;
    left: 16%;

    --tz: 40px;

    animation-delay: 0s;
  }
  .workbench__panel--2 {
    top: 30%;
    right: 16%;

    --tz: 90px;

    animation-delay: 1.2s;
  }
  .workbench__panel--3 {
    bottom: 14%;
    left: 38%;

    --tz: 140px;

    animation-delay: 2.4s;
  }

  @keyframes workbench-float {
    0%,
    100% {
      transform: translateZ(var(--tz)) translateY(0);
    }
    50% {
      transform: translateZ(var(--tz)) translateY(-10px);
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .workbench__panel {
      animation: none;
    }
  }
</style>
