<template>
  <div class="flow-audio-player">
    <audio
      ref="audioRef"
      :src="src"
      preload="metadata"
      @loadedmetadata="onLoadedMetadata"
      @timeupdate="onTimeUpdate"
      @ended="onEnded"
      @play="playing = true"
      @pause="playing = false"
    />

    <button
      type="button"
      class="play-btn"
      :class="{ 'is-playing': playing }"
      :aria-label="playing ? '暂停' : '播放'"
      @click="toggle"
    >
      <CrmIcon :type="playing ? 'iconicon_pause' : 'iconicon_play'" :size="22" />
    </button>

    <div class="progress" @click="onSeek">
      <div class="progress-fill" :style="{ width: `${progress}%` }" />
    </div>

    <div class="eq" :class="{ 'is-playing': playing }" aria-hidden="true">
      <span v-for="i in 5" :key="i" :style="{ animationDelay: `${(i - 1) * 0.12}s` }" />
    </div>

    <span class="time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const props = defineProps<{ src: string }>();

  const audioRef = ref<HTMLAudioElement>();
  const playing = ref(false);
  const currentTime = ref(0);
  const duration = ref(0);

  const progress = computed(() => (duration.value > 0 ? Math.min(100, (currentTime.value / duration.value) * 100) : 0));

  function onLoadedMetadata() {
    const audio = audioRef.value;
    if (audio && Number.isFinite(audio.duration)) {
      duration.value = audio.duration;
    }
  }

  function onTimeUpdate() {
    currentTime.value = audioRef.value?.currentTime ?? 0;
  }

  function onEnded() {
    playing.value = false;
    currentTime.value = 0;
    if (audioRef.value) {
      audioRef.value.currentTime = 0;
    }
  }

  function toggle() {
    const audio = audioRef.value;
    if (!audio) {
      return;
    }
    if (playing.value) {
      audio.pause();
    } else {
      audio.play().catch(() => {
        playing.value = false;
      });
    }
  }

  function onSeek(event: MouseEvent) {
    const audio = audioRef.value;
    const el = event.currentTarget as HTMLElement;
    if (!audio || !duration.value) {
      return;
    }
    const rect = el.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
    audio.currentTime = ratio * duration.value;
    currentTime.value = audio.currentTime;
  }

  function formatTime(seconds: number): string {
    if (!Number.isFinite(seconds) || seconds <= 0) {
      return '00:00';
    }
    const total = Math.floor(seconds);
    const mm = Math.floor(total / 60);
    const ss = total % 60;
    return `${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`;
  }
</script>

<style scoped lang="less">
  .flow-audio-player {
    position: relative;
    display: flex;
    align-items: center;
    padding: 12px 14px;
    border-radius: 12px;
    background: linear-gradient(
      90deg,
      rgb(249 115 22 / 8%),
      rgb(236 72 153 / 8%),
      rgb(139 92 246 / 8%),
      rgb(34 211 238 / 8%)
    );
    background-size: 300% 100%;
    gap: 12px;
    animation: flow-audio-bg 6s linear infinite;

    /* 渐变描边 */
    &::before {
      position: absolute;
      padding: 1px;
      border-radius: 12px;
      background: linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee, #f97316);
      background-size: 300% 100%;
      content: '';
      inset: 0;
      animation: flow-audio-bg 6s linear infinite;
      mask: linear-gradient(#ffffff 0 0) content-box, linear-gradient(#ffffff 0 0);
      mask-composite: xor;
      mask-composite: exclude;
      pointer-events: none;
    }
  }

  @keyframes flow-audio-bg {
    to {
      background-position: 300% 0;
    }
  }
  .play-btn {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 38px;
    height: 38px;
    border: none;
    border-radius: 50%;
    color: #ffffff;
    background: linear-gradient(135deg, #f97316, #ec4899, #8b5cf6);
    background-size: 200% 200%;
    box-shadow: 0 0 12px rgb(249 115 22 / 45%);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
    flex-shrink: 0;
    cursor: pointer;
    &:hover {
      transform: scale(1.06);
      box-shadow: 0 0 18px rgb(236 72 153 / 55%);
    }
    &.is-playing {
      background-position: 100% 100%;
      box-shadow: 0 0 18px rgb(139 92 246 / 55%);
    }
  }
  .progress {
    overflow: hidden;
    height: 6px;
    border-radius: 999px;
    background: var(--fill-2);
    flex: 1;
    cursor: pointer;
  }
  .progress-fill {
    height: 100%;
    border-radius: 999px;
    background: linear-gradient(90deg, #f97316, #ec4899, #8b5cf6, #22d3ee, #f97316);
    background-size: 300% 100%;
    animation: flow-audio-bg 4s linear infinite;
    transition: width 0.1s linear;
  }
  .eq {
    display: flex;
    align-items: flex-end;
    gap: 3px;
    height: 18px;
    flex-shrink: 0;
    span {
      width: 3px;
      height: 6px;
      border-radius: 2px;
      background: linear-gradient(180deg, #f97316, #ec4899);
      transform-origin: bottom;
    }
    &.is-playing span {
      animation: eq-bounce 0.9s ease-in-out infinite;
    }
  }

  @keyframes eq-bounce {
    0%,
    100% {
      transform: scaleY(0.4);
    }
    50% {
      transform: scaleY(1);
    }
  }
  .time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--text-n3);
    font-variant-numeric: tabular-nums;
  }
</style>
