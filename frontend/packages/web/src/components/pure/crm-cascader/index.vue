<template>
  <!-- 公用级联选择器：与 CrmDatePicker 同理，强制把浮层 teleport 到 body，避免在 modal/drawer 内被 overflow 裁剪 -->
  <n-cascader v-bind="$attrs" :to="'body'">
    <template v-for="(_, name) in $slots" :key="name" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}" />
    </template>
  </n-cascader>
</template>

<script setup lang="ts">
  import { NCascader } from 'naive-ui';

  defineOptions({ name: 'CrmCascader', inheritAttrs: false });
</script>
