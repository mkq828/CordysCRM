<template>
  <div class="dark-stat" :style="{ '--stat-accent': accent }">
    <div class="dark-stat__head">
      <span class="dark-stat__name">{{ title }}</span>
      <span class="dark-stat__bubble"><slot name="icon" /></span>
    </div>
    <div class="dark-stat__value">
      {{ value }}<span v-if="unit" class="dark-stat__unit">{{ unit }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      title: string;
      value: string | number;
      unit?: string;
      /** 强调色（图标/微光），十六进制 */
      accent?: string;
    }>(),
    { unit: '', accent: '#2196f3' }
  );
</script>

<style scoped lang="less">
  .dark-stat {
    padding: 18px 20px;
    border: 1px solid rgb(255 255 255 / 10%);
    border-radius: 12px;
    background: linear-gradient(135deg, rgb(16 30 50 / 80%), rgb(25 40 65 / 90%));
    box-shadow: 0 5px 15px rgb(0 0 0 / 30%);
    transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1), border-color 200ms cubic-bezier(0.23, 1, 0.32, 1),
      box-shadow 200ms cubic-bezier(0.23, 1, 0.32, 1);
  }
  .dark-stat__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;
  }
  .dark-stat__name {
    font-size: 14px;
    color: #a0a8b8;
  }
  .dark-stat__bubble {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 38px;
    height: 38px;
    font-size: 18px;
    border: 1px solid rgb(255 255 255 / 12%);
    border-radius: 50%;
    color: var(--stat-accent);
    background: rgb(255 255 255 / 6%);
  }
  .dark-stat__value {
    font-size: 30px;
    font-weight: 700;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .dark-stat__unit {
    margin-left: 6px;
    font-size: 14px;
    font-weight: 500;
    color: #a0a8b8;
  }

  @media (hover: hover) and (pointer: fine) {
    .dark-stat:hover {
      transform: translateY(-5px);
      border-color: rgb(100 180 255 / 45%);
      box-shadow: 0 10px 26px rgb(0 114 255 / 25%);
    }
  }
</style>
