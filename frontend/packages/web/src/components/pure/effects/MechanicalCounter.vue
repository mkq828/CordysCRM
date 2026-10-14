<template>
  <div class="mech-counter-frame" :style="{ '--mech-h': `${digitHeight}px` }">
    <div class="mech-wheels">
      <template v-for="i in digits" :key="i">
        <span v-if="showComma && i > 1 && (digits - i + 1) % 3 === 0" class="mech-comma" :style="{ fontSize: size }"
          >,</span
        >
        <div class="mech-wheel">
          <span class="mech-sheen"></span>
          <div :ref="(el) => setStrip(el, i - 1)" class="mech-strip">
            <span v-for="(n, k) in stripDigits" :key="k" class="mech-digit" :style="{ fontSize: size }">{{ n }}</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue';

  // 0-9 重复多组形成循环滚轮，组数越多越安全
  const REPEAT = 7;
  // 位置超过该值就归一化，避免滚轮条越滚越深
  const NORMALIZE_THRESHOLD = 40;
  // 过冲回弹曲线：快速启动 → 略微过冲 → 回落到目标
  const EASING = 'cubic-bezier(0.25, 1.3, 0.5, 1)';

  const props = withDefaults(
    defineProps<{
      /** 目标数值（0 ~ 10^digits - 1） */
      value: number;
      /** 位数 */
      digits?: number;
      /** 数字字号 */
      size?: string;
      /** 单个数字高度（像素），滚轮整体尺寸由它派生 */
      digitHeight?: number;
      /** 是否按千分位显示逗号 */
      showComma?: boolean;
    }>(),
    { digits: 5, size: '42px', digitHeight: 76, showComma: true }
  );

  // 从右往左（个位起）每位的滚动时长，低位更短 → 转速更快
  const durations = computed(() => Array.from({ length: props.digits }, (_, k) => 700 + k * 150));

  const stripDigits = Array.from({ length: REPEAT * 10 }, (_, k) => k % 10);
  const strips: HTMLElement[] = [];
  const positions: number[] = [];
  let currentValue = 0;
  let animating = false;
  const timers: number[] = [];

  function setStrip(el: unknown, idx: number) {
    if (el) strips[idx] = el as HTMLElement;
  }

  function setWheel(idx: number, targetDigit: number, duration: number) {
    const el = strips[idx];
    if (!el) return;

    let pos = positions[idx];
    // 位置过深先无动画归一化到 10-19 区间
    if (pos >= NORMALIZE_THRESHOLD) {
      el.style.transition = 'none';
      pos = 10 + (pos % 10);
      positions[idx] = pos;
      el.style.transform = `translateY(${-pos * props.digitHeight}px)`;
      // 强制重排，让归一化位置立即生效，再进行滚动动画
      // eslint-disable-next-line no-void
      void el.offsetHeight;
    }

    const currentDigit = pos % 10;
    // 向前滚动格数：为 0 时至少滚一圈，再加一整圈增强机械感
    let delta = (targetDigit - currentDigit + 10) % 10;
    if (delta === 0) delta = 10;
    delta += 10;

    pos += delta;
    positions[idx] = pos;
    el.style.transition = `transform ${duration}ms ${EASING}`;
    el.style.transform = `translateY(${-pos * props.digitHeight}px)`;
  }

  function setValue(newValue: number) {
    if (animating) return;
    newValue = Math.max(0, Math.min(10 ** props.digits - 1, Math.round(newValue)));
    if (newValue === currentValue) return;

    const digits = String(newValue).padStart(props.digits, '0').split('').map(Number);

    animating = true;
    let maxDuration = 0;
    for (let i = 0; i < props.digits; i++) {
      const rightIdx = props.digits - 1 - i; // 0 = 个位
      const duration = durations.value[rightIdx];
      maxDuration = Math.max(maxDuration, duration);
      setWheel(i, digits[i], duration);
    }

    currentValue = newValue;
    timers.push(
      window.setTimeout(() => {
        animating = false;
      }, maxDuration + 120)
    );
  }

  onMounted(() => {
    // 初始位置定在第 2 组的「0」，入场时从 0 滚到目标值做一次演示
    for (let i = 0; i < props.digits; i++) positions[i] = 10;
    nextTick(() => {
      timers.push(window.setTimeout(() => setValue(props.value), 300));
    });
  });

  watch(
    () => props.value,
    (v) => setValue(v)
  );

  onBeforeUnmount(() => {
    timers.forEach((t) => window.clearTimeout(t));
    timers.length = 0;
  });
</script>

<style scoped lang="less">
  // 内凹窗口：新拟物内阴影，与侧边栏键帽同一套视觉语言
  .mech-counter-frame {
    display: inline-flex;
    align-items: center;
    padding: 16px 20px;
    border-radius: 22px;
    background: #e4eaf1;
    box-shadow: inset 7px 7px 16px rgb(138 156 174 / 60%), inset -7px -7px 16px rgb(255 255 255 / 98%),
      0 1px 0 rgb(255 255 255 / 70%);
  }
  .mech-wheels {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  // 单个滚轮：深色凹槽 + 内阴影
  .mech-wheel {
    position: relative;
    overflow: hidden;
    width: calc(var(--mech-h) * 0.76);
    height: var(--mech-h);
    border-radius: 11px;
    background: linear-gradient(180deg, #10151b 0%, #1b222a 42%, #1b222a 58%, #10151b 100%);
    box-shadow: inset 0 4px 10px rgb(0 0 0 / 95%), inset 0 -4px 10px rgb(0 0 0 / 75%),
      inset 0 0 0 1px rgb(255 255 255 / 4.5%), 0 1px 0 rgb(255 255 255 / 35%);
  }

  // 上下遮罩：模拟滚轮弧度阴影
  .mech-wheel::before,
  .mech-wheel::after {
    position: absolute;
    right: 0;
    left: 0;
    z-index: 3;
    height: calc(var(--mech-h) * 0.37);
    content: '';
    pointer-events: none;
  }
  .mech-wheel::before {
    top: 0;
    background: linear-gradient(180deg, rgb(8 12 16 / 96%) 0%, rgb(8 12 16 / 55%) 45%, transparent 100%);
  }
  .mech-wheel::after {
    bottom: 0;
    background: linear-gradient(0deg, rgb(8 12 16 / 96%) 0%, rgb(8 12 16 / 55%) 45%, transparent 100%);
  }

  // 玻璃反光条
  .mech-sheen {
    position: absolute;
    top: 50%;
    right: 6%;
    left: 6%;
    z-index: 2;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgb(255 255 255 / 9%), transparent);
    pointer-events: none;
    transform: translateY(-0.5px);
  }

  // 数字滚轮条
  .mech-strip {
    will-change: transform;
    transform: translateZ(0);
  }
  .mech-digit {
    display: block;
    height: var(--mech-h);
    font-family: 'SF Mono', SFMono-Regular, Menlo, Consolas, 'Roboto Mono', 'Courier New', monospace;
    font-weight: 500;
    text-align: center;
    color: #f7edd8;
    text-shadow: 0 0 16px rgb(255 220 150 / 55%), 0 0 5px rgb(255 220 150 / 35%);
    line-height: var(--mech-h);
    font-variant-numeric: tabular-nums;
    user-select: none;
  }

  // 千分位逗号
  .mech-comma {
    padding: 0 1px;
    font-family: 'SF Mono', SFMono-Regular, Menlo, Consolas, 'Roboto Mono', 'Courier New', monospace;
    font-weight: 500;
    color: #f7edd8;
    text-shadow: 0 0 16px rgb(255 220 150 / 55%), 0 0 5px rgb(255 220 150 / 35%);
    align-self: center;
    line-height: var(--mech-h);
    user-select: none;
  }

  @media (prefers-reduced-motion: reduce) {
    .mech-strip {
      transition: none !important;
    }
  }
</style>
