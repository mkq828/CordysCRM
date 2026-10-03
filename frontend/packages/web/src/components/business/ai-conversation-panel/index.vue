<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="mb-[8px] flex items-center gap-[8px] px-[16px] pt-[16px]">
      <span class="min-w-0 flex-1 truncate text-[13px] font-semibold text-[var(--text-n1)]">{{ title }}</span>
      <n-tooltip trigger="hover" :delay="300">
        <template #trigger>
          <n-button size="tiny" type="primary" ghost class="n-btn-outline-primary" @click="emit('new')">
            <template #icon>
              <CrmIcon type="iconicon_add" :size="14" />
            </template>
          </n-button>
        </template>
        {{ t('aiConversation.newConversation') }}
      </n-tooltip>
    </div>

    <div class="px-[16px] pb-[8px]">
      <CrmSearchInput v-model:value="keyword" :placeholder="t('aiConversation.searchPlaceholder')" />
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto px-[8px] pb-[16px]">
      <n-spin :show="loading" class="h-full">
        <n-empty
          v-if="!list.length && !loading"
          :description="t('aiConversation.empty')"
          :show-icon="false"
          class="mt-[24px]"
        />
        <div v-else class="flex flex-col gap-[2px]">
          <div
            v-for="item in list"
            :key="item.id"
            class="group flex min-h-[34px] cursor-pointer items-center gap-[8px] rounded-[4px] px-[8px] py-[6px]"
            :class="activeId === item.id ? 'bg-[var(--text-n9)]' : 'hover:bg-[var(--text-n9)]'"
            @click="handleSelect(item)"
          >
            <template v-if="editingId === item.id">
              <n-input
                ref="renameInputRef"
                v-model:value="renameValue"
                size="small"
                :maxlength="255"
                @blur="saveRename(item)"
                @keydown.enter.prevent="saveRename(item)"
                @keydown.esc.prevent="cancelRename"
                @click.stop
              />
            </template>
            <template v-else>
              <span
                class="min-w-0 flex-1 truncate text-[13px]"
                :class="activeId === item.id ? 'text-[var(--primary-8)]' : 'text-[var(--text-n2)]'"
              >
                {{ item.title }}
              </span>
              <span
                v-if="item.id === generatingId"
                class="flex shrink-0 items-center gap-[4px] text-[11px] text-orange-500"
              >
                <CrmIcon type="iconicon_loading" :size="12" class="animate-spin" />
                {{ t('aiConversation.generating') }}
              </span>
              <span class="shrink-0 text-[11px] text-[var(--text-n4)]">
                {{ formatItemTime(item.updateTime || item.createTime) }}
              </span>
              <div class="hidden shrink-0 items-center gap-[2px] group-hover:flex">
                <n-tooltip trigger="hover" :delay="300">
                  <template #trigger>
                    <n-button size="tiny" quaternary @click.stop="startRename(item)">
                      <template #icon>
                        <CrmIcon type="iconicon_edit" :size="14" />
                      </template>
                    </n-button>
                  </template>
                  {{ t('aiConversation.renameTip') }}
                </n-tooltip>
                <n-popconfirm @positive-click="handleDelete(item)">
                  <template #trigger>
                    <n-button size="tiny" quaternary @click.stop>
                      <template #icon>
                        <CrmIcon type="iconicon_delete" :size="14" />
                      </template>
                    </n-button>
                  </template>
                  {{ t('aiConversation.deleteTip') }}
                </n-popconfirm>
              </div>
            </template>
          </div>
        </div>
      </n-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { nextTick, onMounted, ref, watch } from 'vue';
  import { InputInst, NButton, NEmpty, NInput, NPopconfirm, NSpin, NTooltip, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentConversationDetail, AgentConversationItem } from '@lib/shared/models/ai';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';

  import {
    deleteAgentConversation,
    getAgentConversationDetail,
    getAgentConversationPage,
    renameAgentConversation,
  } from '@/api/modules';

  const props = withDefaults(
    defineProps<{
      featureCode: string;
      activeId?: string;
      title?: string;
      generatingId?: string;
    }>(),
    {
      activeId: '',
      title: '',
      generatingId: '',
    }
  );

  const emit = defineEmits<{
    (e: 'select', detail: AgentConversationDetail): void;
    (e: 'new'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const keyword = ref('');
  const list = ref<AgentConversationItem[]>([]);
  const loading = ref(false);
  const editingId = ref('');
  const renameValue = ref('');
  const renameInputRef = ref<InputInst | null>(null);
  let searchTimer: ReturnType<typeof setTimeout> | undefined;

  async function load(): Promise<void> {
    loading.value = true;
    try {
      const res = await getAgentConversationPage({
        featureCode: props.featureCode,
        current: 1,
        pageSize: 50,
        keyword: keyword.value.trim() || undefined,
      });
      list.value = res?.list || [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function formatItemTime(ts?: number): string {
    if (!ts) {
      return '';
    }
    const d = dayjs(ts);
    if (d.isSame(dayjs(), 'day')) {
      return d.format('HH:mm');
    }
    if (d.isSame(dayjs(), 'year')) {
      return d.format('MM-DD HH:mm');
    }
    return d.format('YYYY-MM-DD');
  }

  async function handleSelect(item: AgentConversationItem): Promise<void> {
    if (editingId.value === item.id) {
      return;
    }
    try {
      const detail = await getAgentConversationDetail(item.id);
      emit('select', detail);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function startRename(item: AgentConversationItem): void {
    editingId.value = item.id;
    renameValue.value = item.title || '';
    nextTick(() => {
      renameInputRef.value?.focus();
    });
  }

  function cancelRename(): void {
    editingId.value = '';
    renameValue.value = '';
  }

  async function saveRename(item: AgentConversationItem): Promise<void> {
    const { id } = item;
    if (editingId.value !== id) {
      return;
    }
    const title = renameValue.value.trim();
    editingId.value = '';
    renameValue.value = '';
    if (!title || title === item.title) {
      return;
    }
    try {
      await renameAgentConversation(id, { title });
      list.value = list.value.map((it) => (it.id === id ? { ...it, title } : it));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleDelete(item: AgentConversationItem): Promise<void> {
    try {
      await deleteAgentConversation(item.id);
      Message.success(t('aiConversation.deleted'));
      if (props.activeId === item.id) {
        emit('new');
      }
      await load();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(keyword, () => {
    if (searchTimer) {
      clearTimeout(searchTimer);
    }
    searchTimer = setTimeout(() => {
      load();
    }, 300);
  });

  onMounted(load);

  defineExpose({ reload: load });
</script>

<style scoped lang="less"></style>
