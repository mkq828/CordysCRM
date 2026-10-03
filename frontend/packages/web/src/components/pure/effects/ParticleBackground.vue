<template>
  <canvas ref="canvasRef" class="particle-bg"></canvas>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  const props = withDefaults(
    defineProps<{
      color?: string;
      count?: number;
    }>(),
    { color: '#6366f1', count: 60 }
  );

  const canvasRef = ref<HTMLCanvasElement>();
  let ctx: CanvasRenderingContext2D | null = null;
  let rafId = 0;
  let particles: Array<{ x: number; y: number; vx: number; vy: number; r: number }> = [];

  function resize() {
    const canvas = canvasRef.value;
    if (!canvas || !canvas.parentElement) {
      return;
    }
    canvas.width = canvas.parentElement.clientWidth;
    canvas.height = canvas.parentElement.clientHeight;
  }

  function init() {
    const canvas = canvasRef.value;
    if (!canvas) {
      return;
    }
    particles = Array.from({ length: props.count }, () => ({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      r: Math.random() * 2 + 0.5,
    }));
  }

  function tick() {
    const canvas = canvasRef.value;
    if (!canvas || !ctx) {
      return;
    }
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    particles.forEach((p) => {
      p.x += p.vx;
      p.y += p.vy;
      if (p.x < 0 || p.x > canvas.width) {
        p.vx *= -1;
      }
      if (p.y < 0 || p.y > canvas.height) {
        p.vy *= -1;
      }
      ctx!.beginPath();
      ctx!.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx!.fillStyle = props.color;
      ctx!.globalAlpha = 0.6;
      ctx!.fill();
    });
    rafId = requestAnimationFrame(tick);
  }

  onMounted(() => {
    const canvas = canvasRef.value;
    if (!canvas) {
      return;
    }
    ctx = canvas.getContext('2d');
    resize();
    init();
    tick();
  });

  onBeforeUnmount(() => {
    cancelAnimationFrame(rafId);
  });
</script>

<style scoped lang="less">
  .particle-bg {
    display: block;
    width: 100%;
    height: 100%;
  }
</style>
