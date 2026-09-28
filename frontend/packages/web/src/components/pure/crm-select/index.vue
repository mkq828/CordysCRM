<template>
  <!-- 公用下拉选择器：与 CrmDatePicker 同理，强制把下拉浮层 teleport 到 body，避免在 modal/drawer 内被 overflow 裁剪 -->
  <n-select v-bind="$attrs" :to="'body'">
    <template v-for="(_, name) in $slots" :key="name" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}" />
    </template>
  </n-select>
</template>

<script setup lang="ts">
  import { NSelect } from 'naive-ui';

  defineOptions({ name: 'CrmSelect', inheritAttrs: false });
</script>
