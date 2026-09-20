<template>
  <div ref="bankCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pt-[16px]">
        <bankAccountTable />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import bankAccountTable from './components/bankAccountTable.vue';
</script>

<style scoped></style>
