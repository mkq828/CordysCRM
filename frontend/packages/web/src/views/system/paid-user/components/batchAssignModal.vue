<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :title="t('paidUser.batchAssignTitle')"
    class="w-[680px]"
    :mask-closable="false"
  >
    <div class="flex flex-col gap-[16px]">
      <!-- 全部设为：一键把选中租户的跟进经理（可选补空白签约经理）设为同一人 -->
      <div class="flex items-center gap-[12px] rounded border border-[var(--divider-color)] px-[12px] py-[10px]">
        <span class="shrink-0 text-sm text-[var(--text-n2)]">{{ t('paidUser.batchAssignAllTo') }}</span>
        <CrmSelect
          v-model:value="allTo"
          :options="managerOptions"
          :placeholder="t('paidUser.batchAssignAllToPlaceholder')"
          filterable
          clearable
          class="w-[220px]"
        />
        <n-checkbox v-model:checked="fillBlankSign">
          {{ t('paidUser.batchAssignFillBlankSign') }}
        </n-checkbox>
        <n-button secondary :disabled="!allTo" @click="applyAllTo">
          {{ t('paidUser.batchAssignApplyFollow') }}
        </n-button>
      </div>

      <!-- 逐行分配 -->
      <div class="flex flex-col gap-[8px]">
        <div class="flex items-center gap-[12px] px-[4px] text-xs text-[var(--text-n4)]">
          <span class="flex-1">{{ t('paidUser.orgName') }}</span>
          <span class="w-[200px]">{{ t('paidUser.signManager') }}</span>
          <span class="w-[200px]">{{ t('paidUser.followManager') }}</span>
        </div>
        <div
          v-for="row in assignRows"
          :key="row.organizationId"
          class="flex items-center gap-[12px] rounded border border-[var(--divider-color)] px-[12px] py-[8px]"
        >
          <span class="flex-1 truncate text-sm text-[var(--text-n1)]">{{ row.orgName }}</span>
          <CrmSelect
            v-model:value="row.signManagerId"
            :options="managerOptions"
            :placeholder="t('paidUser.signManager')"
            filterable
            clearable
            class="w-[200px]"
          />
          <CrmSelect
            v-model:value="row.followManagerId"
            :options="managerOptions"
            :placeholder="t('paidUser.followManager')"
            filterable
            clearable
            class="w-[200px]"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-[12px]">
        <n-button @click="show = false">{{ t('common.cancel') }}</n-button>
        <n-button type="primary" :loading="submitting" @click="submit">{{ t('common.confirm') }}</n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NButton, NCheckbox, NModal, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CityManagerItem } from '@lib/shared/models/system/cityManager';
  import type { TenantPlanItem } from '@lib/shared/models/system/tenant-plan';

  import CrmSelect from '@/components/pure/crm-select/index.vue';

  import { cityManagerBatchAssign, cityManagerPageList } from '@/api/modules';

  interface AssignRow {
    organizationId: string;
    orgName: string;
    signManagerId: string | null;
    followManagerId: string | null;
  }

  const { t } = useI18n();
  const Message = useMessage();

  const show = defineModel<boolean>('show', { default: false });
  const props = defineProps<{
    rows: TenantPlanItem[];
  }>();

  const emit = defineEmits<{
    (e: 'success'): void;
  }>();

  const managerOptions = ref<{ label: string; value: string }[]>([]);
  const assignRows = ref<AssignRow[]>([]);
  const allTo = ref<string | null>(null);
  const fillBlankSign = ref(false);
  const submitting = ref(false);

  const hasRows = computed(() => assignRows.value.length > 0);

  async function loadManagers() {
    try {
      const res = await cityManagerPageList({ current: 1, pageSize: 500, status: 'ENABLED' });
      managerOptions.value = (res.list || []).map((m: CityManagerItem) => ({ label: m.name, value: m.id }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function initRows() {
    assignRows.value = (props.rows ?? []).map((row) => ({
      organizationId: row.organizationId,
      orgName: row.orgName,
      signManagerId: row.signManagerId ?? null,
      followManagerId: row.followManagerId ?? null,
    }));
    allTo.value = null;
    fillBlankSign.value = false;
  }

  function applyAllTo() {
    if (!allTo.value) return;
    assignRows.value.forEach((row) => {
      row.followManagerId = allTo.value;
      if (fillBlankSign.value && !row.signManagerId) {
        row.signManagerId = allTo.value;
      }
    });
  }

  async function submit() {
    if (!hasRows.value) {
      Message.warning(t('paidUser.batchAssignEmpty'));
      return;
    }
    submitting.value = true;
    try {
      await cityManagerBatchAssign({
        items: assignRows.value.map((row) => ({
          organizationId: row.organizationId,
          signManagerId: row.signManagerId ?? undefined,
          followManagerId: row.followManagerId ?? undefined,
        })),
      });
      Message.success(t('paidUser.batchAssignSuccess'));
      show.value = false;
      emit('success');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      submitting.value = false;
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (val) {
        initRows();
        loadManagers();
      }
    }
  );
</script>
