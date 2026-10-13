<template>
  <div ref="rootEl" class="neural-scene" :class="{ 'neural-scene--reduced': reduced }">
    <canvas ref="canvasEl" class="neural-scene__canvas" @click="handleCanvasClick"></canvas>

    <!-- 中心光球：企业总览核心指标 -->
    <div v-if="centerStats" class="neural-scene__orb" @click="emit('select', centerStats.key)">
      <div class="neural-scene__orb-title">{{ centerStats.title }}</div>
      <div class="neural-scene__orb-value">{{ centerStats.primaryValue }}</div>
      <div class="neural-scene__orb-sub">{{ centerStats.subLabel }}</div>
    </div>

    <!-- 维度水母：点击展开 / 收起 -->
    <button
      v-for="dim in layout.dims"
      :key="dim.key"
      class="neural-scene__dim"
      :class="{ 'is-active': dim.key === expandedKey, 'is-selected': dim.key === activeKey }"
      :style="nodeStyle(dim.x, dim.y)"
      type="button"
      @click="handleDimensionClick(dim.key)"
    >
      <span class="neural-scene__dim-dot" :style="dotStyle(dim.color)"></span>
      <span class="neural-scene__dim-label">{{ dim.label }}</span>
    </button>

    <!-- 展开后的二级节点：叶子（可下钻） -->
    <button
      v-for="node in expandedNodes"
      :key="node.key"
      class="neural-scene__leaf"
      :class="{ 'is-selected': node.key === activeKey }"
      :style="nodeStyle(node.x, node.y)"
      type="button"
      @click="emit('select', node.key)"
    >
      <span class="neural-scene__leaf-dot" :style="dotStyle(node.color)"></span>
      <span class="neural-scene__leaf-label">{{ node.label }}</span>
    </button>
  </div>
</template>

<script lang="ts"></script>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

  export interface SceneDimension {
    key: string;
    label: string;
    color: string;
  }

  export interface SceneNode {
    key: string;
    label: string;
    color: string;
  }

  export interface SceneCenterStats {
    key: string;
    title: string;
    primaryValue: string;
    subLabel: string;
  }

  interface ScenePoint {
    key: string;
    label: string;
    color: string;
    x: number;
    y: number;
  }

  interface SceneLink {
    x1: number;
    y1: number;
    x2: number;
    y2: number;
    color: string;
  }

  interface SceneLayout {
    w: number;
    h: number;
    orb: { x: number; y: number; r: number };
    dims: ScenePoint[];
    leaves: ScenePoint[];
    links: SceneLink[];
  }

  const props = withDefaults(
    defineProps<{
      dimensions: SceneDimension[];
      childrenByDimension?: Record<string, SceneNode[]>;
      centerStats?: SceneCenterStats | null;
      activeKey?: string | null;
    }>(),
    {
      childrenByDimension: () => ({}),
      centerStats: null,
      activeKey: null,
    }
  );

  const emit = defineEmits<{
    (event: 'select', key: string): void;
  }>();

  const rootEl = ref<HTMLElement | null>(null);
  const canvasEl = ref<HTMLCanvasElement | null>(null);

  const expandedKey = ref<string | null>(null);
  const size = reactive({ w: 0, h: 0 });
  const reduced = ref(false);

  const layout = reactive<SceneLayout>({
    w: 0,
    h: 0,
    orb: { x: 0, y: 0, r: 1 },
    dims: [],
    leaves: [],
    links: [],
  });

  const expandedNodes = computed<ScenePoint[]>(() => layout.leaves);

  const prefersReducedMotion = (() => {
    if (typeof window === 'undefined' || !window.matchMedia) {
      return false;
    }
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  })();

  function computeLayout() {
    const { w, h } = size;
    if (w <= 0 || h <= 0) {
      return;
    }
    const cx = w / 2;
    const cy = h / 2;
    const short = Math.min(w, h);

    layout.w = w;
    layout.h = h;
    layout.orb = { x: cx, y: cy, r: Math.max(40, short * 0.12) };
    layout.links = [];

    const dims = props.dimensions;
    const count = Math.max(dims.length, 1);
    const orbitR = short * 0.3;

    layout.dims = dims.map((dim, index) => {
      const angle = (index / count) * Math.PI * 2 - Math.PI / 2;
      return {
        key: dim.key,
        label: dim.label,
        color: dim.color,
        x: cx + Math.cos(angle) * orbitR,
        y: cy + Math.sin(angle) * orbitR,
      };
    });

    const leaves: ScenePoint[] = [];
    const expanded = expandedKey.value;
    if (expanded) {
      const dimIndex = dims.findIndex((dim) => dim.key === expanded);
      const children = props.childrenByDimension[expanded] ?? [];
      const dim = layout.dims[dimIndex];
      if (dim && children.length > 0) {
        const leafR = short * 0.42;
        const baseAngle = (dimIndex / count) * Math.PI * 2 - Math.PI / 2;
        const spread = Math.min(1.6, Math.max(0.7, children.length * 0.16));
        const step = children.length > 1 ? spread / (children.length - 1) : 0;
        children.forEach((child, index) => {
          const angle = baseAngle - spread / 2 + step * index;
          const x = cx + Math.cos(angle) * leafR;
          const y = cy + Math.sin(angle) * leafR;
          leaves.push({ key: child.key, label: child.label, color: child.color, x, y });
          layout.links.push({ x1: dim.x, y1: dim.y, x2: x, y2: y, color: child.color });
        });
      }
    }
    layout.leaves = leaves;
  }

  function nodeStyle(x: number, y: number) {
    return { left: `${x}px`, top: `${y}px` };
  }

  function dotStyle(color: string) {
    return { background: color, boxShadow: `0 0 12px ${color}` };
  }

  function handleDimensionClick(key: string) {
    if (expandedKey.value === key) {
      expandedKey.value = null;
    } else {
      expandedKey.value = key;
    }
    emit('select', key);
  }

  function handleCanvasClick(event: MouseEvent) {
    const canvas = canvasEl.value;
    if (!canvas) {
      return;
    }
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const dim = layout.dims.find((d) => Math.hypot(d.x - x, d.y - y) < 42);
    if (dim) {
      handleDimensionClick(dim.key);
      return;
    }
    const leaf = layout.leaves.find((d) => Math.hypot(d.x - x, d.y - y) < 34);
    if (leaf) {
      emit('select', leaf.key);
    }
  }

  let rafId = 0;
  let lastTime = 0;
  let animationTime = 0;

  function drawFrame(time: number) {
    rafId = window.requestAnimationFrame(drawFrame);
    const dt = time - lastTime;
    lastTime = time;
    if (dt >= 0) {
      animationTime += Math.min(dt, 64);
    }
    try {
      paint();
    } catch {
      // 单帧异常不中断动画循环，避免画面停在隐形状态
    }
  }

  function paint() {
    const canvas = canvasEl.value;
    if (!canvas) {
      return;
    }
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const { w, h } = size;
    if (w <= 0 || h <= 0) {
      return;
    }
    if (canvas.width !== Math.round(w * dpr) || canvas.height !== Math.round(h * dpr)) {
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
    }
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);

    paintOrb(ctx);
    paintLinks(ctx);
    layout.dims.forEach((dim, index) => {
      paintJellyfish(ctx, dim.x, dim.y, dim.color, index);
    });
    layout.leaves.forEach((leaf) => {
      paintLeafDot(ctx, leaf.x, leaf.y, leaf.color);
    });
  }

  function paintOrb(ctx: CanvasRenderingContext2D) {
    const { x, y, r } = layout.orb;
    const pulse = reduced.value ? 0 : Math.sin(animationTime / 900) * 0.06 + 1;
    const radius = r * pulse;

    const glow = ctx.createRadialGradient(x, y, radius * 0.1, x, y, radius * 1.9);
    glow.addColorStop(0, 'rgba(80, 150, 255, 0.55)');
    glow.addColorStop(0.45, 'rgba(80, 150, 255, 0.16)');
    glow.addColorStop(1, 'rgba(80, 150, 255, 0)');
    ctx.fillStyle = glow;
    ctx.beginPath();
    ctx.arc(x, y, radius * 1.9, 0, Math.PI * 2);
    ctx.fill();

    const body = ctx.createRadialGradient(x - radius * 0.3, y - radius * 0.35, radius * 0.08, x, y, radius);
    body.addColorStop(0, '#eaf3ff');
    body.addColorStop(0.35, '#9cc4ff');
    body.addColorStop(0.75, '#4282ff');
    body.addColorStop(1, '#1c3f8f');
    ctx.fillStyle = body;
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, Math.PI * 2);
    ctx.fill();

    ctx.save();
    ctx.strokeStyle = 'rgba(170, 210, 255, 0.8)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, Math.PI * 2);
    ctx.stroke();
    ctx.restore();
  }

  function paintJellyfish(ctx: CanvasRenderingContext2D, x: number, y: number, color: string, index: number) {
    const phase = index * 1.7;
    const bob = reduced.value ? 0 : Math.sin(animationTime / 1400 + phase) * 6;
    const yy = y + bob;
    const scale = 1 + (reduced.value ? 0 : Math.sin(animationTime / 1600 + phase) * 0.05);

    ctx.save();
    ctx.translate(x, yy);
    ctx.scale(scale, scale);

    ctx.shadowColor = color;
    ctx.shadowBlur = 18;

    const dome = ctx.createLinearGradient(0, -26, 0, 6);
    dome.addColorStop(0, `${color}CC`);
    dome.addColorStop(1, `${color}22`);
    ctx.fillStyle = dome;
    ctx.beginPath();
    ctx.moveTo(-24, 2);
    ctx.quadraticCurveTo(-24, -24, 0, -28);
    ctx.quadraticCurveTo(24, -24, 24, 2);
    ctx.quadraticCurveTo(12, 8, 0, 6);
    ctx.quadraticCurveTo(-12, 8, -24, 2);
    ctx.fill();

    ctx.shadowBlur = 0;
    ctx.strokeStyle = color;
    ctx.lineWidth = 1.6;
    ctx.beginPath();
    ctx.moveTo(-24, 2);
    ctx.quadraticCurveTo(-24, -24, 0, -28);
    ctx.quadraticCurveTo(24, -24, 24, 2);
    ctx.stroke();

    const tentacleCount = 4;
    for (let i = 0; i < tentacleCount; i += 1) {
      const baseX = -18 + i * 12;
      const sway = reduced.value ? 0 : Math.sin(animationTime / 900 + phase + i * 0.9) * 6;
      ctx.strokeStyle = `${color}AA`;
      ctx.lineWidth = 1.4;
      ctx.beginPath();
      ctx.moveTo(baseX, 4);
      ctx.quadraticCurveTo(baseX + sway * 0.4, 22, baseX + sway, 38);
      ctx.stroke();
    }

    ctx.restore();
  }

  function paintLinks(ctx: CanvasRenderingContext2D) {
    layout.links.forEach((link) => {
      ctx.strokeStyle = `${link.color}55`;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(link.x1, link.y1);
      ctx.quadraticCurveTo((link.x1 + link.x2) / 2, (link.y1 + link.y2) / 2 - 8, link.x2, link.y2);
      ctx.stroke();
    });
  }

  function paintLeafDot(ctx: CanvasRenderingContext2D, x: number, y: number, color: string) {
    ctx.save();
    ctx.shadowColor = color;
    ctx.shadowBlur = 10;
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }

  function resize() {
    const el = rootEl.value;
    if (!el) {
      return;
    }
    size.w = el.clientWidth;
    size.h = el.clientHeight;
    computeLayout();
    if (reduced.value) {
      paint();
    }
  }

  let resizeObserver: ResizeObserver | null = null;
  let mediaQuery: MediaQueryList | null = null;

  function onReducedChange(event: MediaQueryListEvent) {
    reduced.value = event.matches;
    if (event.matches) {
      if (rafId) {
        window.cancelAnimationFrame(rafId);
        rafId = 0;
      }
      paint();
    } else if (!rafId) {
      lastTime = performance.now();
      rafId = window.requestAnimationFrame(drawFrame);
    }
  }

  onMounted(() => {
    reduced.value = prefersReducedMotion;
    mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    mediaQuery.addEventListener('change', onReducedChange);

    resizeObserver = new ResizeObserver(resize);
    if (rootEl.value) {
      resizeObserver.observe(rootEl.value);
    }
    resize();

    if (!reduced.value) {
      lastTime = performance.now();
      rafId = window.requestAnimationFrame(drawFrame);
    }
  });

  onBeforeUnmount(() => {
    if (rafId) {
      window.cancelAnimationFrame(rafId);
    }
    resizeObserver?.disconnect();
    mediaQuery?.removeEventListener('change', onReducedChange);
  });

  watch(
    () => [props.dimensions, props.childrenByDimension, expandedKey.value] as const,
    () => computeLayout(),
    { deep: true }
  );
</script>

<style lang="less" scoped>
  .neural-scene {
    position: relative;
    overflow: hidden;
    width: 100%;
    height: 100%;
    user-select: none;
  }
  .neural-scene__canvas {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
  }
  .neural-scene__orb {
    position: absolute;
    top: 50%;
    left: 50%;
    z-index: 2;
    display: flex;
    justify-content: center;
    align-items: center;
    width: 220px;
    text-align: center;
    flex-direction: column;
    gap: 4px;
    transform: translate(-50%, -50%);
    cursor: pointer;
  }
  .neural-scene__orb-title {
    font-size: 14px;
    letter-spacing: 2px;
    color: #bcd6ff;
  }
  .neural-scene__orb-value {
    font-size: 30px;
    font-weight: 700;
    line-height: 1.1;
    color: #eaf3ff;
    text-shadow: 0 0 16px rgb(66 130 255 / 80%);
  }
  .neural-scene__orb-sub {
    font-size: 12px;
    color: #9fc2ee;
  }
  .neural-scene__dim {
    position: absolute;
    z-index: 3;
    display: flex;
    align-items: center;
    padding: 4px;
    min-width: 84px;
    border: none;
    background: transparent;
    flex-direction: column;
    gap: 6px;
    cursor: pointer;
    transform: translate(-50%, -50%);
  }
  .neural-scene__dim-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }
  .neural-scene__dim-label {
    font-size: 13px;
    letter-spacing: 1px;
    color: #d6e3f7;
    text-shadow: 0 1px 4px rgb(0 0 0 / 60%);
  }
  .neural-scene__dim.is-active .neural-scene__dim-label {
    color: #ffffff;
  }
  .neural-scene__leaf {
    position: absolute;
    z-index: 3;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    border: 1px solid rgb(255 255 255 / 12%);
    border-radius: 999px;
    background: rgb(11 18 32 / 78%);
    cursor: pointer;
    transform: translate(-50%, -50%);
    transition: border-color 180ms cubic-bezier(0.23, 1, 0.32, 1), background 180ms cubic-bezier(0.23, 1, 0.32, 1);
  }
  .neural-scene__leaf:hover,
  .neural-scene__leaf.is-selected {
    border-color: rgb(120 180 255 / 70%);
    background: rgb(20 32 54 / 90%);
  }
  .neural-scene__leaf-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }
  .neural-scene__leaf-label {
    font-size: 12px;
    color: #d6e3f7;
  }
</style>
