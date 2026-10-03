<template>
  <div class="aurora-bg">
    <span class="aurora-bg__blob aurora-bg__blob--1"></span>
    <span class="aurora-bg__blob aurora-bg__blob--2"></span>
    <span class="aurora-bg__blob aurora-bg__blob--3"></span>
    <span class="aurora-bg__blob aurora-bg__blob--4"></span>
    <div class="aurora-bg__content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped lang="less">
  .aurora-bg {
    position: relative;
    overflow: hidden;
    width: 100%;
    height: 100%;
    background: #0f0f23;
  }
  .aurora-bg__blob {
    position: absolute;
    border-radius: 50%;
    opacity: 0.7;
    filter: blur(60px);
    mix-blend-mode: screen;
  }
  .aurora-bg__blob--1 {
    top: -40px;
    left: -40px;
    width: 320px;
    height: 320px;
    background: #f97316;
    animation: aurora-1 12s ease-in-out infinite;
  }
  .aurora-bg__blob--2 {
    top: 10%;
    right: -60px;
    width: 360px;
    height: 360px;
    background: #8b5cf6;
    animation: aurora-2 14s ease-in-out infinite;
  }
  .aurora-bg__blob--3 {
    bottom: -80px;
    left: 30%;
    width: 300px;
    height: 300px;
    background: #ec4899;
    animation: aurora-3 16s ease-in-out infinite;
  }
  .aurora-bg__blob--4 {
    right: 10%;
    bottom: 5%;
    width: 280px;
    height: 280px;
    background: #22d3ee;
    animation: aurora-4 18s ease-in-out infinite;
  }
  .aurora-bg__content {
    position: relative;
    z-index: 1;
    height: 100%;
  }

  @keyframes aurora-1 {
    0%,
    100% {
      transform: translate(0, 0) scale(1);
    }
    50% {
      transform: translate(40px, 30px) scale(1.2);
    }
  }

  @keyframes aurora-2 {
    0%,
    100% {
      transform: translate(0, 0) scale(1);
    }
    50% {
      transform: translate(-50px, 20px) scale(0.9);
    }
  }

  @keyframes aurora-3 {
    0%,
    100% {
      transform: translate(0, 0) scale(1);
    }
    50% {
      transform: translate(30px, -40px) scale(1.15);
    }
  }

  @keyframes aurora-4 {
    0%,
    100% {
      transform: translate(0, 0) scale(1);
    }
    50% {
      transform: translate(-30px, -20px) scale(1.1);
    }
  }
</style>
