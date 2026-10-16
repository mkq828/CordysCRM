<template>
  <div ref="rootEl" class="scene" :class="`stage-${stage}`">
    <!-- 中心光球：orb.mp4 逐帧合成，叠加企业总览核心指标 -->
    <div :ref="setOrbEl" class="orb">
      <div class="orb-halo"></div>
      <video
        :ref="(el) => setVid('orb', 'video', el)"
        class="vsrc"
        :src="media.orb"
        :poster="media.orbPoster"
        muted
        loop
        playsinline
        autoplay
        preload="auto"
      ></video>
      <canvas :ref="(el) => setVid('orb', 'canvas', el)" class="vid"></canvas>
      <canvas :ref="setDustEl" class="orb-dust" width="1012" height="1012"></canvas>
      <div v-if="centerStats" class="orb-copy" @click="goHub">
        <div class="orb-copy__value">{{ centerStats.primaryValue }}</div>
        <div class="orb-copy__sub">{{ centerStats.subLabel }}</div>
      </div>
    </div>

    <!-- 光球 → 水母：神经丝与往返脉冲 -->
    <svg class="threads" aria-hidden="true">
      <defs>
        <filter id="bs-thread-blur" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="3.2" />
        </filter>
        <radialGradient id="bs-pulse-out">
          <stop offset="0" stop-color="#fffaf0" />
          <stop offset=".4" stop-color="#ecd39e" />
          <stop offset="1" stop-color="#ecd39e" stop-opacity="0" />
        </radialGradient>
        <radialGradient id="bs-pulse-in">
          <stop offset="0" stop-color="#ffffff" />
          <stop offset=".4" stop-color="#e3b3c4" />
          <stop offset="1" stop-color="#e3b3c4" stop-opacity="0" />
        </radialGradient>
      </defs>
      <g v-for="dim in dims" :key="dim.key" :style="{ color: dim.color }">
        <path :ref="(el) => setThread(dim.key, 'glow', el)" class="thread-glow" filter="url(#bs-thread-blur)" />
        <path :ref="(el) => setThread(dim.key, 'core', el)" class="thread-core" />
        <circle
          v-for="k in 3"
          :key="k"
          :ref="(el) => setPulse(dim.key, k - 1, el)"
          :r="k === 3 ? 4.5 : 5.5"
          :fill="k === 3 ? 'url(#bs-pulse-in)' : 'url(#bs-pulse-out)'"
        />
      </g>
      <g v-for="(node, i) in fanNodes" :key="node.key" :style="{ color: node.color }">
        <path :ref="(el) => setFanPath(i, el)" class="fan-thread" />
        <circle
          v-for="k in 2"
          :key="k"
          :ref="(el) => setFanPulse(i, k - 1, el)"
          :r="k === 2 ? 3.5 : 4.5"
          :fill="k === 2 ? 'url(#bs-pulse-in)' : 'url(#bs-pulse-out)'"
        />
      </g>
    </svg>

    <!-- 六只水母：维度入口 -->
    <template v-for="(dim, i) in dims" :key="dim.key">
      <button
        :ref="(el) => setJelly(dim.key, el)"
        class="jelly"
        :class="{ active: dim.key === selectedDim }"
        type="button"
        :aria-label="dim.label"
        @click="onDim(dim.key)"
        @pointerenter="hover = dim.key"
        @pointerleave="hover = null"
      >
        <div :ref="(el) => setInner(dim.key, el)" class="jelly-inner" :style="{ filter: jellyFilter(i) }">
          <video
            :ref="(el) => setVid(dim.key, 'video', el)"
            class="vsrc"
            :src="media.jelly"
            :poster="media.jellyPoster"
            muted
            loop
            playsinline
            autoplay
            preload="auto"
            @loadedmetadata="(e) => onJellyMeta(i, e)"
          ></video>
          <canvas :ref="(el) => setVid(dim.key, 'canvas', el)" class="vid"></canvas>
        </div>
      </button>
      <button
        :ref="(el) => setLabel(dim.key, el)"
        class="jelly-label"
        :class="{ active: dim.key === selectedDim, below: stage === 'hub', side: stage !== 'hub' }"
        type="button"
        tabindex="-1"
        :style="{ '--mod': dim.color }"
        @click="onDim(dim.key)"
        @pointerenter="hover = dim.key"
        @pointerleave="hover = null"
      >
        <span class="jl-icon"><i :style="{ background: dim.color }"></i></span>
        <span class="jl-count">{{ countOf(dim) }}</span>
        <span class="jl-name">{{ dim.label }}</span>
      </button>
    </template>

    <!-- 扇叶节点：维度下的二级可下钻项 -->
    <button
      v-for="(node, i) in fanNodes"
      :key="node.key"
      :ref="(el) => setItem(i, el)"
      class="fan-item"
      :class="{ active: node.key === activeKey }"
      :style="{ '--kc': node.color }"
      type="button"
      :aria-label="node.label"
      @click="emit('select', node.key)"
    >
      <span class="fi-dot"></span>
      <span class="fi-body"
        ><span class="fi-title">{{ node.label }}</span></span
      >
    </button>

    <!-- 右上角：重播 / 暂停 -->
    <div class="stage-tools" role="toolbar" aria-label="动画控制">
      <button type="button" :title="t('businessScreen.replay')" @click="replay">
        <svg
          width="15"
          height="15"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5" />
        </svg>
        <span>{{ t('businessScreen.replay') }}</span>
      </button>
      <button
        type="button"
        :title="paused ? t('businessScreen.resume') : t('businessScreen.pause')"
        :aria-pressed="paused"
        @click="togglePaused"
      >
        <svg
          v-if="paused"
          width="15"
          height="15"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="m8 5 11 7-11 7Z" />
        </svg>
        <svg
          v-else
          width="15"
          height="15"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M8 5v14M16 5v14" />
        </svg>
        <span>{{ paused ? t('businessScreen.resume') : t('businessScreen.pause') }}</span>
      </button>
    </div>
  </div>
</template>

<script lang="ts"></script>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

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

  interface Spring {
    x: number;
    v: number;
  }

  interface JellySpring extends Spring {
    cur: { x: number; y: number; s: number };
    label: { x: number; y: number; w: number; h: number } | null;
    o: number;
    lo: number;
    s: number;
  }

  interface TargetOrb {
    x: number;
    y: number;
    s: number;
  }

  interface TargetJelly {
    x: number;
    y: number;
    s: number;
    o: number;
    tilt: number;
  }

  interface Targets {
    orb: TargetOrb;
    jelly: Record<string, TargetJelly>;
    fan: { x: number; y0: number; gap: number } | null;
  }

  const props = withDefaults(
    defineProps<{
      dimensions: SceneDimension[];
      childrenByDimension?: Record<string, SceneNode[]>;
      centerStats?: SceneCenterStats | null;
      activeKey?: string | null;
      panelVisible?: boolean;
    }>(),
    {
      childrenByDimension: () => ({}),
      centerStats: null,
      activeKey: null,
      panelVisible: false,
    }
  );

  const emit = defineEmits<{
    (event: 'select', key: string): void;
  }>();

  const { t } = useI18n();

  const media = {
    orb: `${import.meta.env.BASE_URL}media/orb.mp4`,
    orbPoster: `${import.meta.env.BASE_URL}media/orb.jpg`,
    jelly: `${import.meta.env.BASE_URL}media/jellyfish.mp4`,
    jellyPoster: `${import.meta.env.BASE_URL}media/jellyfish.jpg`,
  };

  const clamp = (v: number, a: number, b: number) => Math.max(a, Math.min(b, v));
  const sp = (x: number): Spring => ({ x, v: 0 });

  function spring(st: Spring, target: number, dt: number, k = 62): number {
    const c = 2 * Math.sqrt(k);
    st.v += (-k * (st.x - target) - c * st.v) * dt;
    st.x += st.v * dt;
    return st.x;
  }

  const bez = (p0: number, p1: number, p2: number, p3: number, u: number) => {
    const m = 1 - u;
    return m * m * m * p0 + 3 * m * m * u * p1 + 3 * m * u * u * p2 + u * u * u * p3;
  };

  const rootEl = ref<HTMLElement | null>(null);
  const selectedDim = ref<string | null>(null);
  const hover = ref<string | null>(null);
  const reduced = ref(false);
  const paused = ref(false);
  const size = reactive({ w: 0, h: 0 });

  const dims = computed(() => props.dimensions);
  const fanNodes = computed<SceneNode[]>(() =>
    selectedDim.value ? props.childrenByDimension[selectedDim.value] ?? [] : []
  );
  const stage = computed(() => (selectedDim.value ? 'module' : 'hub'));

  function expandable(key: string): boolean {
    return (props.childrenByDimension[key]?.length ?? 0) > 0;
  }

  function onDim(key: string): void {
    emit('select', key);
    if (!expandable(key)) {
      selectedDim.value = null;
      return;
    }
    selectedDim.value = selectedDim.value === key ? null : key;
  }

  function goHub(): void {
    selectedDim.value = null;
    if (props.centerStats) {
      emit('select', props.centerStats.key);
    }
  }

  function countOf(dim: SceneDimension): string {
    const n = props.childrenByDimension[dim.key]?.length ?? 0;
    return n > 0 ? String(n) : '·';
  }

  function jellyFilter(i: number): string {
    const filters = [
      'hue-rotate(215deg) saturate(1.1)',
      'hue-rotate(185deg) saturate(.3) brightness(1.15)',
      'hue-rotate(118deg) saturate(1)',
      'saturate(1)',
      'hue-rotate(158deg) saturate(1.05)',
    ];
    return filters[i % filters.length];
  }

  function onJellyMeta(i: number, e: Event): void {
    const video = e.currentTarget as HTMLVideoElement;
    video.currentTime = (i * 1.37) % 6;
  }

  // —— 模板引用（函数 ref，直接存入普通对象） ——
  let orbEl: HTMLElement | null = null;
  let dustEl: HTMLCanvasElement | null = null;
  const jellyEl: Record<string, HTMLElement | null> = {};
  const jellyInner: Record<string, HTMLElement | null> = {};
  const jellyLabel: Record<string, HTMLElement | null> = {};
  const thread: Record<string, { glow: SVGPathElement | null; core: SVGPathElement | null }> = {};
  const pulses: Record<string, (SVGCircleElement | null)[]> = {};
  const vids: Record<string, { video: HTMLVideoElement | null; canvas: HTMLCanvasElement | null }> = {};
  const itemEl: (HTMLElement | null)[] = [];
  const fanPath: (SVGPathElement | null)[] = [];
  const fanPulses: (SVGCircleElement | null)[][] = [];

  function setOrbEl(node: unknown): void {
    orbEl = (node as HTMLElement) ?? null;
  }
  function setDustEl(node: unknown): void {
    dustEl = (node as HTMLCanvasElement) ?? null;
  }
  function setVid(key: string, part: 'video' | 'canvas', node: unknown): void {
    if (!vids[key]) {
      vids[key] = { video: null, canvas: null };
    }
    const pair = vids[key];
    if (part === 'video') {
      pair.video = (node as HTMLVideoElement) ?? null;
    } else {
      pair.canvas = (node as HTMLCanvasElement) ?? null;
    }
  }
  function setJelly(key: string, node: unknown): void {
    jellyEl[key] = (node as HTMLElement) ?? null;
  }
  function setInner(key: string, node: unknown): void {
    jellyInner[key] = (node as HTMLElement) ?? null;
  }
  function setLabel(key: string, node: unknown): void {
    jellyLabel[key] = (node as HTMLElement) ?? null;
  }
  function setThread(key: string, part: 'glow' | 'core', node: unknown): void {
    if (!thread[key]) {
      thread[key] = { glow: null, core: null };
    }
    const g = thread[key];
    g[part] = (node as SVGPathElement) ?? null;
  }
  function setPulse(key: string, k: number, node: unknown): void {
    if (!pulses[key]) {
      pulses[key] = [];
    }
    const arr = pulses[key];
    arr[k] = (node as SVGCircleElement) ?? null;
  }
  function setItem(i: number, node: unknown): void {
    itemEl[i] = (node as HTMLElement) ?? null;
  }
  function setFanPath(i: number, node: unknown): void {
    fanPath[i] = (node as SVGPathElement) ?? null;
  }
  function setFanPulse(i: number, k: number, node: unknown): void {
    if (!fanPulses[i]) {
      fanPulses[i] = [];
    }
    const arr = fanPulses[i];
    arr[k] = (node as SVGCircleElement) ?? null;
  }

  // —— 视频逐帧合成：部分内置浏览器不合成 <video>，画布通用 ——
  const posterCache = new Map<string, HTMLImageElement>();
  function posterOf(url: string): HTMLImageElement {
    if (!posterCache.has(url)) {
      const img = new Image();
      img.src = url;
      posterCache.set(url, img);
    }
    return posterCache.get(url)!;
  }

  interface VideoPair {
    video: HTMLVideoElement | null;
    canvas: HTMLCanvasElement | null;
    stamp?: number;
    ready?: boolean;
  }

  function paintVideo(pair: VideoPair): void {
    const { video, canvas } = pair;
    if (!video || !canvas) {
      return;
    }
    const cw = canvas.clientWidth;
    const ch = canvas.clientHeight;
    if (!cw || !ch) {
      return;
    }
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const W = Math.round(cw * dpr);
    const H = Math.round(ch * dpr);
    const resized = canvas.width !== W || canvas.height !== H;
    if (resized) {
      canvas.width = W;
      canvas.height = H;
    }
    const ready = video.readyState >= 2 && video.videoWidth > 0;
    const src: CanvasImageSource = ready ? video : posterOf(video.poster);
    const sw0 = ready ? video.videoWidth : (src as HTMLImageElement).naturalWidth;
    const sh0 = ready ? video.videoHeight : (src as HTMLImageElement).naturalHeight;
    if (!sw0 || !sh0) {
      return;
    }
    const stamp = ready ? video.currentTime : -1;
    if (!resized && stamp === pair.stamp && ready === pair.ready) {
      return;
    }
    pair.stamp = stamp;
    pair.ready = ready;
    const k = Math.max(W / sw0, H / sh0);
    const sw = W / k;
    const sh = H / k;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }
    ctx.clearRect(0, 0, W, H);
    ctx.drawImage(src, (sw0 - sw) / 2, (sh0 - sh) / 2, sw, sh, 0, 0, W, H);
  }

  // —— 光球边缘飘散的光点（银白 + 香槟金） ——
  const DUST = Array.from({ length: 90 }, (_, i) => ({
    a: i * 2.39996,
    v: 0.035 + (((i * 37) % 100) / 100) * 0.06,
    o: (i * 0.618034) % 1,
    w: (i % 2 ? 1 : -1) * (0.15 + (i % 7) * 0.05),
    s: 0.7 + (i % 5) * 0.32,
    b: 0.45 + (i % 3) * 0.25,
    c: ['236,236,232', '230,207,152', '200,205,210', '255,255,255'][i % 4],
  }));

  // —— 布局目标 ——
  // 展开态把选中维度放到扇叶序列中间，其余维度均匀分布在其两侧（维度数量可扩展，不写死 6 个槽位）。
  function slotOf(key: string): number {
    const list = dims.value.map((d) => d.key);
    const others = list.filter((k) => k !== selectedDim.value);
    const middle = Math.floor(others.length / 2);
    const order = [...others.slice(0, middle), selectedDim.value, ...others.slice(middle)].filter((v): v is string =>
      Boolean(v)
    );
    return order.indexOf(key);
  }

  function targets(t: number): Targets {
    const W = size.w;
    const H = size.h;
    const narrow = W < 760;
    const swim = reduced.value ? 0 : 1;
    const res: Targets = { orb: { x: 0, y: 0, s: 0 }, jelly: {}, fan: null };

    if (W <= 0 || H <= 0) {
      return res;
    }

    if (stage.value === 'hub') {
      const cx = props.panelVisible && !narrow ? (W - 470) / 2 : W / 2;
      const s = narrow ? clamp(W * 0.56, 180, 280) : clamp(Math.min(W, H) * 0.5, 250, 440);
      const cy = narrow ? H * 0.47 : H * 0.53;
      res.orb = { x: cx, y: cy, s };
      const rx = narrow ? W * 0.36 : Math.min(W * 0.36, 540);
      const ry = narrow ? H * 0.21 : Math.min(H * 0.32, 262);
      dims.value.forEach((m, i) => {
        const a =
          (-90 + (360 / Math.max(dims.value.length, 1)) * i + swim * 7 * Math.sin(t * 0.17 + i * 1.7)) *
          (Math.PI / 180);
        const r = 1 + swim * 0.04 * Math.sin(t * 0.23 + i);
        res.jelly[m.key] = {
          x: cx + Math.cos(a) * rx * r,
          y: cy + Math.sin(a) * ry * r + swim * 9 * Math.sin(t * 0.9 + i * 2.1),
          s: narrow ? 92 : clamp(W * 0.12, 120, 184),
          o: 1,
          tilt: swim * 6 * Math.sin(t * 0.6 + i),
        };
      });
    } else {
      const ox = Math.max(124, W * 0.09);
      const cy = H * 0.53;
      res.orb = { x: ox, y: cy, s: 178 };
      const colX = ox + 158;
      const gap = Math.min(122, (H - 150) / Math.max(dims.value.length - 1, 1));
      dims.value.forEach((m, i) => {
        const sel = m.key === selectedDim.value;
        const k = slotOf(m.key);
        res.jelly[m.key] = {
          x: colX + (sel ? 16 : 0),
          y: cy + (k - (dims.value.length - 1) / 2) * gap + swim * 5 * Math.sin(t * 0.9 + i * 2),
          s: sel ? 128 : 92,
          o: sel ? 1 : 0.6,
          tilt: swim * 3 * Math.sin(t * 0.6 + i),
        };
      });
      const panelLeft = W - clamp(W * 0.33, 380, 470) - 44;
      const n = fanNodes.value.length;
      const span = Math.min(H - 130, Math.max(0, n - 1) * 54);
      const originX = colX + 16 + 128 * 0.5 + 10 + 168;
      res.fan = {
        x: Math.min(Math.max(panelLeft - 270 - 90, originX + 60), panelLeft - 280),
        y0: cy - span / 2,
        gap: n > 1 ? span / (n - 1) : 0,
      };
    }
    return res;
  }

  // —— 弹簧状态 ——
  const S = {
    orb: { x: sp(0), y: sp(0), s: sp(0), o: sp(0) },
    dust: sp(0),
    jelly: {} as Record<
      string,
      {
        x: Spring;
        y: Spring;
        s: Spring;
        o: Spring;
        lo: Spring;
        cur: { x: number; y: number; s: number };
        label: { x: number; y: number; w: number; h: number } | null;
      }
    >,
  };

  function ensureJelly(key: string): void {
    if (!S.jelly[key]) {
      S.jelly[key] = { x: sp(0), y: sp(0), s: sp(24), o: sp(0), lo: sp(0), cur: { x: 0, y: 0, s: 0 }, label: null };
    }
  }

  let itemState: { x: Spring; y: Spring; o: Spring }[] = [];
  let rafId = 0;
  let last = 0;
  let time = 0;
  let t0 = 0;
  let fanStart = 0;
  let warned = false;

  function step(now: number): void {
    const dt = Math.max(0, Math.min(1 / 30, (now - last) / 1000));
    last = Math.max(last, now);
    if (!paused.value) {
      time += dt;
    }
    const t = time;
    const since = (now - t0) / 1000;
    const T = targets(t);
    const intro = stage.value === 'hub' && !reduced.value;

    // 光球
    const orbS = intro && since < 0.15 ? T.orb.s * 0.35 : T.orb.s;
    const ox = spring(S.orb.x, T.orb.x, dt);
    const oy = spring(S.orb.y, T.orb.y, dt);
    const os = spring(S.orb.s, orbS, dt, 40);
    const oo = spring(S.orb.o, intro && since < 0.1 ? 0 : 1, dt, 30);
    if (orbEl) {
      orbEl.style.transform = `translate3d(${ox - os / 2}px,${oy - os / 2}px,0) scale(${os / 460})`;
      orbEl.style.opacity = String(clamp(oo, 0, 1));
    }

    // 光点
    if (dustEl) {
      const di = clamp(spring(S.dust, stage.value === 'hub' ? 0.3 : 1, dt, 18), 0, 1);
      const ctx = dustEl.getContext('2d');
      if (ctx) {
        ctx.clearRect(0, 0, 1012, 1012);
        if (!reduced.value && di > 0.02) {
          const sc = Math.max(os / 460, 0.2);
          ctx.globalCompositeOperation = 'lighter';
          DUST.forEach((p) => {
            const ph = (((t * p.v + p.o) % 1) + 1) % 1;
            const r = 150 + ph * 340;
            const a = p.a + ph * p.w;
            const x = 506 + Math.cos(a) * r;
            const y = 506 + Math.sin(a) * r * 0.96;
            const al = di * Math.max(0, Math.sin(ph * Math.PI)) ** 1.3 * p.b;
            const rad = (p.s / sc) * 2.2;
            const g = ctx.createRadialGradient(x, y, 0, x, y, rad);
            g.addColorStop(0, `rgba(${p.c},${al})`);
            g.addColorStop(1, `rgba(${p.c},0)`);
            ctx.fillStyle = g;
            ctx.beginPath();
            ctx.arc(x, y, rad, 0, 7);
            ctx.fill();
          });
          ctx.globalCompositeOperation = 'source-over';
        }
      }
    }

    // 水母
    dims.value.forEach((m, i) => {
      ensureJelly(m.key);
      const j = S.jelly[m.key];
      const tg = T.jelly[m.key];
      const gated = intro && since < 1 + i * 0.17;
      const x = spring(j.x, gated ? ox : tg.x, dt);
      const y = spring(j.y, gated ? oy : tg.y, dt);
      const s = spring(j.s, gated ? 24 : tg.s, dt);
      const o = spring(j.o, gated ? 0 : tg.o, dt, 40);
      const lo = spring(j.lo, intro && since < 2 + i * 0.1 ? 0 : tg.o, dt, 40);
      j.cur = { x, y, s };

      const je = jellyEl[m.key];
      const ji = jellyInner[m.key];
      const jl = jellyLabel[m.key];
      if (je) {
        je.style.transform = `translate3d(${x - s / 2}px,${y - s * 0.375}px,0) scale(${s / 200})`;
        je.style.opacity = String(clamp(o, 0, 1));
      }
      if (ji) {
        ji.style.transform = `rotate(${tg.tilt}deg) scaleX(${i % 2 === 0 ? 1 : -1})`;
      }
      if (jl) {
        const lw = jl.offsetWidth;
        const lh = jl.offsetHeight;
        const hub = stage.value === 'hub';
        const narrow = size.w < 760;
        let lx = x + s * 0.5 + 10;
        let ly = y - lh / 2;
        if (hub || narrow) {
          lx = x - lw / 2;
          ly = y + s * 0.36;
        }
        jl.style.transform = `translate3d(${lx}px,${ly}px,0)`;
        jl.style.opacity = String(clamp(narrow && !hub && m.key !== selectedDim.value ? 0 : lo, 0, 1));
        j.label = { x: lx, y: ly, w: lw, h: lh };
      }

      // 神经丝与脉冲
      const g = thread[m.key];
      if (g?.glow && g?.core) {
        let x1: number;
        let y1: number;
        let x2: number;
        let y2: number;
        let c1x: number;
        let c1y: number;
        let c2x: number;
        let c2y: number;
        if (stage.value === 'hub') {
          const dx = x - ox;
          const dy = y - oy;
          const d = Math.hypot(dx, dy) || 1;
          const ux = dx / d;
          const uy = dy / d;
          x1 = ox + ux * os * 0.2;
          y1 = oy + uy * os * 0.2;
          x2 = x - ux * s * 0.22;
          y2 = y - uy * s * 0.1;
          const bend = 26 * Math.sin(t * 0.5 + i);
          c1x = x1 + dx * 0.35 - uy * bend;
          c1y = y1 + dy * 0.35 + ux * bend;
          c2x = x1 + dx * 0.7 + uy * bend;
          c2y = y1 + dy * 0.7 - ux * bend;
        } else {
          x1 = ox + os * 0.3;
          y1 = oy;
          x2 = x - s * 0.34;
          y2 = y;
          const dx = x2 - x1;
          c1x = x1 + dx * 0.55;
          c1y = y1;
          c2x = x2 - dx * 0.55;
          c2y = y2;
        }
        const dPath = `M${x1},${y1} C${c1x},${c1y} ${c2x},${c2y} ${x2},${y2}`;
        const active = m.key === selectedDim.value || m.key === hover.value;
        g.glow.setAttribute('d', dPath);
        g.core.setAttribute('d', dPath);
        let threadVis = 0.45;
        if (stage.value === 'hub' || active) {
          threadVis = 1;
        }
        const vis = clamp(o, 0, 1) * threadVis;
        g.glow.style.opacity = String(vis * (active ? 0.9 : 0.45));
        g.core.style.opacity = String(vis);
        (pulses[m.key] ?? []).forEach((c, k) => {
          if (!c) {
            return;
          }
          const inbound = k === 2;
          let u = (t * (inbound ? 0.23 : 0.31) + k * 0.5 + i * 0.17) % 1;
          if (inbound) {
            u = 1 - u;
          }
          c.setAttribute('cx', String(bez(x1, c1x, c2x, x2, u)));
          c.setAttribute('cy', String(bez(y1, c1y, c2y, y2, u)));
          c.style.opacity = String(reduced.value ? 0 : vis * Math.sin(u * Math.PI));
        });
      }
    });

    // 扇叶：维度 → 二级节点
    const sj = selectedDim.value ? S.jelly[selectedDim.value] : null;
    const tf = T.fan;
    if (tf && sj?.label) {
      const narrow = size.w < 760;
      const ox2 = narrow ? sj.label.x + sj.label.w / 2 : sj.label.x + sj.label.w + 6;
      const oy2 = narrow ? sj.label.y + sj.label.h + 4 : sj.cur.y;
      fanNodes.value.forEach((n, i) => {
        if (!itemState[i]) {
          itemState[i] = { x: sp(ox2), y: sp(oy2), o: sp(0) };
        }
        const st = itemState[i];
        const ready =
          reduced.value || now - fanStart > 160 + i * Math.min(90, 1800 / Math.max(fanNodes.value.length, 1));
        const tx = tf.x;
        const ty = tf.y0 + i * tf.gap + (reduced.value ? 0 : Math.sin(t * 0.8 + i * 0.9) * 2);
        const x = spring(st.x, ready ? tx : ox2, dt, 70);
        const y = spring(st.y, ready ? ty : oy2, dt, 70);
        const o = spring(st.o, ready ? 1 : 0, dt, 50);
        const ie = itemEl[i];
        if (ie) {
          ie.style.transform = `translate3d(${x}px,${y - 15}px,0)`;
          ie.style.opacity = String(clamp(o, 0, 1));
        }
        const p = fanPath[i];
        if (p) {
          const dx = x - ox2;
          const sway = reduced.value ? 0 : Math.sin(t * 1.1 + i * 0.7) * 7;
          const c1x = ox2 + dx * 0.5;
          const c1y = oy2 + sway;
          const c2x = x - dx * 0.45;
          const c2y = y - sway * 0.5;
          p.setAttribute('d', `M${ox2},${oy2} C${c1x},${c1y} ${c2x},${c2y} ${x},${y}`);
          const on = n.key === props.activeKey;
          let fanPulseVis = 0.8;
          if (n.key === props.activeKey) {
            fanPulseVis = on ? 1 : 0.35;
          }
          p.style.opacity = String(clamp(o, 0, 1) * fanPulseVis);
          p.style.strokeWidth = on ? '1.8' : '1';
          (fanPulses[i] ?? []).forEach((c, k) => {
            if (!c) {
              return;
            }
            let u = (t * (k ? 0.27 : 0.36) + i * 0.13 + k * 0.5) % 1;
            if (k) {
              u = 1 - u;
            }
            c.setAttribute('cx', String(bez(ox2, c1x, c2x, x, u)));
            c.setAttribute('cy', String(bez(oy2, c1y, c2y, y, u)));
            c.style.opacity = String(
              reduced.value ? 0 : clamp(o, 0, 1) * Math.sin(u * Math.PI) * (n.key === props.activeKey && !on ? 0.3 : 1)
            );
          });
        }
      });
    }

    // 视频画面
    Object.values(vids).forEach((pair) => paintVideo(pair));
  }

  function frame(now: number): void {
    rafId = window.requestAnimationFrame(frame);
    try {
      step(now);
    } catch (err) {
      if (!warned) {
        warned = true;
        console.error('[经营大屏] 动画帧异常', err);
      }
    }
  }

  function measure(): void {
    const el = rootEl.value;
    if (!el) {
      return;
    }
    const w = el.clientWidth;
    const h = el.clientHeight;
    size.w = w;
    size.h = h;
  }

  function syncVideo(): void {
    Object.values(vids).forEach((pair) => {
      if (!pair.video) {
        return;
      }
      if (reduced.value || paused.value || document.hidden) {
        pair.video.pause();
      } else {
        pair.video.play().catch(() => {});
      }
    });
  }

  function replay(): void {
    const now = performance.now();
    t0 = now;
    last = now;
    time = 0;
    fanStart = now;
    paused.value = false;
    selectedDim.value = null;
    const T0 = targets(0);
    S.orb.x = sp(T0.orb.x);
    S.orb.y = sp(T0.orb.y);
    S.orb.s = sp(T0.orb.s * 0.35);
    S.orb.o = sp(0);
    dims.value.forEach((m) => {
      ensureJelly(m.key);
      const j = S.jelly[m.key];
      j.x = sp(T0.orb.x);
      j.y = sp(T0.orb.y);
      j.s = sp(24);
      j.o = sp(0);
      j.lo = sp(0);
    });
    itemState = [];
    syncVideo();
  }

  function togglePaused(): void {
    paused.value = !paused.value;
    syncVideo();
  }

  let resizeObserver: ResizeObserver | null = null;
  let mediaQuery: MediaQueryList | null = null;

  function onReducedChange(event: MediaQueryListEvent): void {
    reduced.value = event.matches;
    syncVideo();
  }

  watch(selectedDim, () => {
    itemState = [];
    fanStart = performance.now();
  });

  onMounted(() => {
    reduced.value = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    mediaQuery.addEventListener('change', onReducedChange);

    t0 = performance.now();
    last = performance.now();
    fanStart = performance.now();
    measure();
    resizeObserver = new ResizeObserver(measure);
    if (rootEl.value) {
      resizeObserver.observe(rootEl.value);
    }

    const T0 = targets(0);
    S.orb.x = sp(T0.orb.x);
    S.orb.y = sp(T0.orb.y);
    S.orb.s = sp(T0.orb.s * 0.35);
    S.orb.o = sp(0);
    dims.value.forEach((m) => {
      ensureJelly(m.key);
      const j = S.jelly[m.key];
      const tg = T0.jelly[m.key];
      j.x = sp(T0.orb.x);
      j.y = sp(T0.orb.y);
      j.s = sp(24);
      j.o = sp(0);
      j.lo = sp(0);
    });

    rafId = window.requestAnimationFrame(frame);
    syncVideo();
    document.addEventListener('visibilitychange', syncVideo);
  });

  onBeforeUnmount(() => {
    if (rafId) {
      window.cancelAnimationFrame(rafId);
    }
    resizeObserver?.disconnect();
    mediaQuery?.removeEventListener('change', onReducedChange);
    document.removeEventListener('visibilitychange', syncVideo);
  });
</script>

<style lang="less" scoped>
  .scene {
    position: absolute;
    inset: 0;
    pointer-events: none;
  }
  .scene > * {
    pointer-events: auto;
  }

  /* —— 光球 —— */
  .orb {
    --orb-m0: 58%;

    position: absolute;
    top: 0;
    left: 0;
    width: 460px;
    height: 460px;
    transform-origin: 0 0;
    will-change: transform;
    pointer-events: none;
  }
  .stage-module .orb {
    --orb-m0: 24%;
  }
  .vsrc {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
    pointer-events: none;
  }
  .orb-dust {
    position: absolute;
    top: -60%;
    left: -60%;
    width: 220%;
    height: 220%;
    pointer-events: none;
  }
  .orb .vid {
    position: relative;
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    mask-image: radial-gradient(
      circle closest-side at 50% 50%,
      #000000 var(--orb-m0),
      rgb(0 0 0 / 82%) calc(var(--orb-m0) + (100% - var(--orb-m0)) * 0.3),
      rgb(0 0 0 / 50%) calc(var(--orb-m0) + (100% - var(--orb-m0)) * 0.56),
      rgb(0 0 0 / 20%) calc(var(--orb-m0) + (100% - var(--orb-m0)) * 0.8),
      transparent 100%
    );
    filter: brightness(0.86) contrast(1.1);
  }
  .orb-halo {
    position: absolute;
    inset: -48%;
    border-radius: 50%;
    background: radial-gradient(
      circle closest-side,
      rgb(196 197 193 / 40%) 0%,
      rgb(186 187 183 / 33%) 30%,
      rgb(166 167 163 / 24%) 44%,
      rgb(146 147 143 / 17%) 52%,
      rgb(116 117 114 / 10%) 63%,
      rgb(86 87 84 / 4.5%) 76%,
      rgb(60 61 59 / 1.2%) 88%,
      transparent 100%
    );
    filter: blur(14px);
    animation: bs-breathe 5.5s ease-in-out infinite;
  }
  @keyframes bs-breathe {
    50% {
      transform: scale(1.08);
      opacity: 0.75;
    }
  }

  /* 中心指标文字 */
  .orb-copy {
    position: absolute;
    inset: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
    flex-direction: column;
    gap: 4px;
    pointer-events: none;
    transition: opacity 0.5s;
  }
  .stage-module .orb-copy {
    opacity: 0;
  }
  .orb-copy__value {
    font-size: 30px;
    font-weight: 600;
    letter-spacing: 0.01em;
    color: #f4f4f1;
    text-shadow: 0 0 22px rgb(230 207 152 / 35%);
  }
  .orb-copy__sub {
    font-size: 12px;
    color: #c9cac5;
  }

  /* —— 神经丝 —— */
  .threads {
    position: absolute;
    overflow: visible;
    width: 100%;
    height: 100%;
    inset: 0;
    pointer-events: none !important;
  }
  .thread-glow {
    fill: none;
    stroke: currentcolor;
    stroke-width: 6;
    opacity: 0.45;
  }
  .thread-core {
    fill: none;
    stroke: currentcolor;
    stroke-width: 1.3;
    stroke-linecap: round;
  }
  .fan-thread {
    fill: none;
    stroke: currentcolor;
    stroke-width: 1;
    stroke-linecap: round;
    transition: stroke-width 0.3s;
  }

  /* —— 水母 —— */
  .jelly {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 1;
    padding: 0;
    width: 200px;
    height: 150px;
    border: 0;
    background: none;
    transition: filter 0.4s;
    transform-origin: 0 0;
    mix-blend-mode: screen;
    will-change: transform, opacity;
  }
  .jelly-inner {
    position: relative;
    width: 100%;
    height: 100%;
  }
  .jelly .vid {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    mask-image: radial-gradient(ellipse 62% 64% at 50% 46%, #000000 55%, transparent 100%);
  }
  .jelly:hover,
  .jelly.active {
    filter: brightness(1.35) drop-shadow(0 0 12px rgb(255 255 255 / 25%));
  }
  .jelly:focus-visible {
    filter: brightness(1.5);
    outline: none;
  }

  /* 标签始终压在水母点击区之上 */
  .jelly-label {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 2;
    display: grid;
    align-items: center;
    overflow: hidden;
    padding: 10px 13px 11px;
    min-width: 132px;
    border: 1px solid rgb(255 255 255 / 14%);
    border-radius: 14px;
    text-align: left;
    color: #ffffff;
    background: linear-gradient(135deg, rgb(255 255 255 / 12%) 0%, rgb(255 255 255 / 3%) 100%);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 14%);
    transition: border-color 0.3s, box-shadow 0.3s, background 0.3s, backdrop-filter 0.3s;
    grid-template-columns: auto 1fr;
    grid-template-areas: 'icon count' 'name name';
    column-gap: 8px;
    backdrop-filter: blur(14px) saturate(140%);
    will-change: transform, opacity;
  }
  .jelly-label::before {
    content: '';
    position: absolute;
    top: 0;
    left: -20%;
    width: 60%;
    height: 100%;
    background: linear-gradient(105deg, transparent 0%, rgb(255 255 255 / 10%) 50%, transparent 100%);
    transform: skewX(-18deg);
    pointer-events: none;
    opacity: 0;
    transition: opacity 0.3s;
  }
  .jelly-label.below {
    justify-items: start;
  }
  .jelly-label:hover {
    border-color: rgb(100 180 255 / 60%);
    background: linear-gradient(135deg, rgb(255 255 255 / 16%) 0%, rgb(255 255 255 / 5%) 100%);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 22%), 0 0 22px rgb(0 114 255 / 26%);
    backdrop-filter: blur(14px) saturate(140%);
  }
  .jelly-label:hover::before {
    opacity: 1;
  }
  .jelly-label.active {
    border-color: rgb(100 180 255 / 60%);
    background: linear-gradient(
      135deg,
      color-mix(in srgb, var(--mod) 16%, rgb(255 255 255 / 12%)) 0%,
      rgb(255 255 255 / 4%) 100%
    );
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 22%), 0 0 26px color-mix(in srgb, var(--mod) 28%, transparent);
    backdrop-filter: blur(14px) saturate(140%);
  }
  .jelly-label.active::before {
    opacity: 1;
  }
  .jl-icon {
    grid-area: icon;
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    border-radius: 7px;
    background: color-mix(in srgb, var(--mod) 14%, transparent);
  }
  .jl-icon i {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    color: var(--mod);
    box-shadow: 0 0 8px currentcolor;
  }
  .jl-count {
    grid-area: count;
    font-size: 22px;
    font-weight: 600;
    line-height: 1;
  }
  .jl-name {
    grid-area: name;
    margin-top: 6px;
    font-size: 12.5px;
    font-weight: 500;
    white-space: nowrap;
  }

  /* —— 扇叶节点 —— */
  .fan-item {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 3;
    display: flex;
    align-items: center;
    padding: 0;
    height: 30px;
    border: 0;
    text-align: left;
    color: #ecece9;
    background: none;
    gap: 9px;
    will-change: transform, opacity;
  }
  .fi-dot {
    margin-left: -4.5px;
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: var(--kc);
    box-shadow: 0 0 0 3px color-mix(in srgb, var(--kc) 18%, transparent), 0 0 14px var(--kc);
    flex: none;
  }
  .fi-body {
    display: grid;
    padding: 3px 10px;
    border: 1px solid transparent;
    border-radius: 9px;
    transition: background 0.25s, border-color 0.25s;
    gap: 2px;
  }
  .fi-title {
    overflow: hidden;
    max-width: 230px;
    font-size: 12.5px;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.25;
  }
  .fan-item:hover .fi-body {
    border-color: rgb(232 232 226 / 11%);
    background: rgb(255 255 255 / 5%);
  }
  .fan-item.active .fi-body {
    border-color: color-mix(in srgb, var(--kc) 55%, transparent);
    background: color-mix(in srgb, var(--kc) 14%, rgb(18 19 19 / 82%));
    box-shadow: 0 0 22px color-mix(in srgb, var(--kc) 20%, transparent);
  }

  /* —— 右上角动画控制 —— */
  .stage-tools {
    position: absolute;
    top: 80px;
    right: 28px;
    z-index: 5;
    display: flex;
    gap: 2px;
    padding: 4px;
    border: 1px solid rgb(232 232 226 / 11%);
    border-radius: 999px;
    background: linear-gradient(160deg, rgb(52 53 52 / 50%), rgb(14 15 15 / 70%));
    backdrop-filter: blur(18px) saturate(1.25);
  }
  .stage-tools button {
    display: inline-flex;
    align-items: center;
    padding: 6px 12px;
    font-size: 12px;
    border: 0;
    border-radius: 999px;
    color: #a5a6a1;
    background: none;
    gap: 6px;
    cursor: pointer;
  }
  .stage-tools button:hover,
  .stage-tools button[aria-pressed='true'] {
    color: #ecece9;
    background: rgb(255 255 255 / 7%);
  }

  @media (prefers-reduced-motion: reduce) {
    .orb-halo {
      animation-duration: 0.01ms !important;
      animation-delay: 0ms !important;
    }
  }
</style>
