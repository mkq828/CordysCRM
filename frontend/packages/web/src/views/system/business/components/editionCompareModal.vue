<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :title="t('system.personal.compare.title')"
    class="!w-[760px]"
    :bordered="false"
  >
    <n-spin :show="loading">
      <div class="overflow-x-auto">
        <table class="compare-table">
          <thead>
            <tr>
              <th class="compare-th">{{ t('system.personal.compare.item') }}</th>
              <th v-for="e in editions" :key="e.id" class="compare-th">{{ e.name }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.label">
              <td class="compare-td compare-label">{{ row.label }}</td>
              <td v-for="(cell, i) in row.cells" :key="i" class="compare-td compare-value">
                <span v-if="cell.check === true" class="text-[var(--success-green)]">✓</span>
                <span v-else-if="cell.check === false" class="text-[var(--text-n4)]">—</span>
                <template v-else>{{ cell.text }}</template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <n-empty v-if="!loading && !editions.length" :description="t('system.personal.compare.empty')" />
    </n-spin>
  </n-modal>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NEmpty, NModal, NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { Edition } from '@lib/shared/models/system/edition';

  import { getPlanEditions } from '@/api/modules';

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const editions = ref<Edition[]>([]);
  const loading = ref(false);

  interface CompareCell {
    text: string;
    check?: boolean;
  }
  interface CompareRow {
    label: string;
    cells: CompareCell[];
  }

  const rows = computed<CompareRow[]>(() => {
    const list = editions.value;
    const result: CompareRow[] = [
      {
        label: t('system.personal.compare.price'),
        cells: list.map((e) => ({ text: e.yearPrice != null ? `¥${e.yearPrice}/年` : '—' })),
      },
      {
        label: t('system.personal.compare.seats'),
        cells: list.map((e) => ({ text: e.softLimit != null ? `${e.softLimit}人` : '—' })),
      },
      {
        label: t('system.personal.compare.quota'),
        cells: list.map((e) => ({
          text: e.aiMonthlyQuota != null ? `${e.aiMonthlyQuota}${t('system.personal.compare.quotaUnit')}` : '—',
        })),
      },
    ];
    const union = [...new Set(list.flatMap((e) => e.featureNames ?? []))];
    union.forEach((name) => {
      result.push({
        label: name,
        cells: list.map((e) =>
          e.featureNames?.includes(name) ? { text: '', check: true } : { text: '', check: false }
        ),
      });
    });
    return result;
  });

  async function load() {
    loading.value = true;
    try {
      editions.value = (await getPlanEditions()) || [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  watch(show, (v) => {
    if (v) load();
  });
</script>

<style lang="less" scoped>
  .compare-table {
    width: 100%;
    border-collapse: collapse;
  }
  .compare-th,
  .compare-td {
    padding: 10px 12px;
    border: 1px solid var(--divider-color);
    text-align: center;
  }
  .compare-th {
    font-weight: 600;
    color: var(--text-n1);
    background: var(--text-n9);
  }
  .compare-label {
    text-align: left;
    white-space: nowrap;
    color: var(--text-n1);
  }
  .compare-value {
    color: var(--text-n2);
  }
</style>
