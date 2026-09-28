<template>
  <!--
    公用日期选择器：强制把浮层 teleport 到 body。
    Naive 的 n-date-picker 在 n-modal / n-drawer 内时，useAdjustedTo 会把浮层渲染进
    modal 的卡片里（.n-modal.n-card 有 overflow:hidden，外层还有 scrollbar 的 overflow:hidden/scroll），
    导致日历面板被裁剪（遮挡）。统一在这里修，后续页面直接用本组件即可，不要再逐个加 to。
  -->
  <!--
    fast-month-select：选完「年-月」后自动收起月份快速跳转浮层，回到日期格。
    Naive 默认 fastYearSelect/fastMonthSelect 均为 false，日期/日期时间选择器里点「年-月」
    打开月份浮层后，选完年、月并不会自动收起，浮层会一直盖在日期格上（用户反馈「遮挡日期选择」）。
  -->
  <n-date-picker v-bind="$attrs" :to="'body'" :fast-month-select="true">
    <template v-for="(_, name) in $slots" :key="name" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}" />
    </template>
  </n-date-picker>
</template>

<script setup lang="ts">
  import { NDatePicker } from 'naive-ui';

  defineOptions({ name: 'CrmDatePicker', inheritAttrs: false });
</script>
