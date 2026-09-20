<template>
  <CrmCard hide-footer>
    <div class="suggestion-page">
      <div class="suggestion-header">
        <n-button type="primary" @click="openAdd">{{ t('suggestion.add') }}</n-button>
        <div class="flex items-center gap-[12px]">
          <n-select
            v-model:value="statusFilter"
            class="!w-[160px]"
            clearable
            :options="statusOptions"
            :placeholder="t('suggestion.status.all')"
            @update:value="onStatusChange"
          />
          <CrmSearchInput v-model:value="keyword" :placeholder="t('suggestion.searchPlaceholder')" @search="onSearch" />
        </div>
      </div>

      <n-spin :show="loading">
        <div v-if="list.length" class="suggestion-list">
          <div v-for="item in list" :key="item.id" class="suggestion-card" @click="goDetail(item.id)">
            <div class="flex items-start justify-between gap-[16px]">
              <div class="flex min-w-0 flex-1 flex-col gap-[8px]">
                <div class="flex items-center gap-[8px]">
                  <n-tag size="small" :type="statusTagType[item.status]" :bordered="false">
                    {{ t(`suggestion.status.${item.status}`) }}
                  </n-tag>
                  <span class="suggestion-title">{{ item.title }}</span>
                </div>
                <div class="suggestion-content">{{ item.content }}</div>
                <div class="suggestion-meta">
                  <span class="suggestion-user">{{ item.userName }}</span>
                  <span v-if="item.organizationName" class="suggestion-org">{{ item.organizationName }}</span>
                  <span>{{ formatTime(item.createTime) }}</span>
                </div>
              </div>
              <div class="flex shrink-0 flex-col items-center gap-[8px]" @click.stop>
                <n-button
                  size="small"
                  :type="item.voted ? 'primary' : 'default'"
                  :ghost="!item.voted"
                  :loading="votingId === item.id"
                  @click="handleVote(item)"
                >
                  <template #icon>
                    <CrmIcon type="iconicon_thumb_up" :size="14" />
                  </template>
                  {{ item.voteCount || 0 }}
                </n-button>
                <div class="flex items-center gap-[4px] text-[12px] text-[var(--text-n4)]">
                  <CrmIcon type="iconicon_comment" :size="14" />
                  {{ item.commentCount || 0 }}
                </div>
              </div>
            </div>
          </div>
        </div>
        <n-empty v-else-if="!loading" :description="t('suggestion.empty')" class="py-[80px]" />
      </n-spin>

      <div v-if="total > 0" class="flex justify-end py-[16px]">
        <n-pagination v-model:page="current" :page-size="pageSize" :item-count="total" @update:page="loadList" />
      </div>
    </div>

    <CrmDrawer
      v-model:show="showAdd"
      :width="800"
      :title="t('suggestion.add')"
      :loading="addLoading"
      @confirm="submitAdd"
    >
      <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
        <n-form-item :label="t('suggestion.title')" path="title">
          <n-input
            v-model:value="formModel.title"
            :maxlength="200"
            show-count
            :placeholder="t('suggestion.title.placeholder')"
          />
        </n-form-item>
        <n-form-item :label="t('suggestion.content')" path="content">
          <n-input
            v-model:value="formModel.content"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 10 }"
            :placeholder="t('suggestion.content.placeholder')"
          />
        </n-form-item>
        <n-form-item :label="t('suggestion.uploadImages')">
          <n-upload
            v-model:file-list="imageFileList"
            list-type="image-card"
            accept="image/jpeg,image/png"
            :max="9"
            :custom-request="uploadImage"
          />
        </n-form-item>
      </n-form>
    </CrmDrawer>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';
  import {
    type FormInst,
    type FormRules,
    NButton,
    NEmpty,
    NForm,
    NFormItem,
    NInput,
    NPagination,
    NSelect,
    NSpin,
    NTag,
    NUpload,
    type UploadCustomRequestOptions,
    type UploadFileInfo,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SuggestionItem, SuggestionStatus } from '@lib/shared/models/system/suggestion';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';

  import { addSuggestion, getSuggestionPage, uploadTempAttachment, voteSuggestion } from '@/api/modules';

  import { SuggestionRouteEnum } from '@/enums/routeEnum';

  const router = useRouter();
  const { t } = useI18n();
  const Message = useMessage();

  const loading = ref(false);
  const list = ref<SuggestionItem[]>([]);
  const total = ref(0);
  const current = ref(1);
  const pageSize = 10;
  const keyword = ref('');
  const statusFilter = ref<string | null>(null);
  const votingId = ref('');

  const statusOptions = computed(() => {
    const statuses: SuggestionStatus[] = ['PENDING', 'ADOPTED', 'DEVELOPING', 'RELEASED', 'REJECTED'];
    return statuses.map((s) => ({ label: t(`suggestion.status.${s}`), value: s }));
  });

  const statusTagType: Record<SuggestionStatus, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    PENDING: 'warning',
    ADOPTED: 'info',
    DEVELOPING: 'info',
    RELEASED: 'success',
    REJECTED: 'error',
  };

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '';
  }

  async function loadList() {
    loading.value = true;
    try {
      const res = await getSuggestionPage({
        current: current.value,
        pageSize,
        keyword: keyword.value || undefined,
        status: (statusFilter.value as SuggestionStatus) || '',
      });
      list.value = res.list || [];
      total.value = res.total || 0;
    } finally {
      loading.value = false;
    }
  }

  function onSearch(val: string) {
    keyword.value = val;
    current.value = 1;
    loadList();
  }

  function onStatusChange() {
    current.value = 1;
    loadList();
  }

  function goDetail(id: string) {
    router.push({ name: SuggestionRouteEnum.SUGGESTION_DETAIL, params: { id } });
  }

  async function handleVote(item: SuggestionItem) {
    if (votingId.value) return;
    votingId.value = item.id;
    try {
      const res = await voteSuggestion(item.id);
      item.voteCount = res.voteCount;
      item.voted = res.voted;
      Message.success(res.voted ? t('suggestion.voteSuccess') : t('suggestion.unvoteSuccess'));
    } finally {
      votingId.value = '';
    }
  }

  // ---- 新增建议 ----
  const showAdd = ref(false);
  const addLoading = ref(false);
  const formRef = ref<FormInst | null>(null);
  const formModel = reactive({
    title: '',
    content: '',
  });
  const imageFileList = ref<UploadFileInfo[]>([]);

  const rules: FormRules = {
    title: {
      required: true,
      message: t('suggestion.title.required'),
      trigger: ['input', 'blur'],
    },
    content: {
      required: true,
      message: t('suggestion.content.required'),
      trigger: ['input', 'blur'],
    },
  };

  function openAdd() {
    formModel.title = '';
    formModel.content = '';
    imageFileList.value = [];
    showAdd.value = true;
  }

  async function uploadImage({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const f = file as UploadFileInfo;
      if (!f.url && f.file) {
        f.url = URL.createObjectURL(f.file as File);
      }
      const res = await uploadTempAttachment(f.file as File);
      const [attachmentId] = res.data;
      f.id = attachmentId;
      f.status = 'finished';
      onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      onError();
    }
  }

  function submitAdd() {
    formRef.value?.validate(async (errors) => {
      if (errors) return;
      addLoading.value = true;
      try {
        const imageIds = imageFileList.value.filter((f) => f.status === 'finished').map((f) => f.id);
        await addSuggestion({
          title: formModel.title,
          content: formModel.content,
          imageIds,
        });
        Message.success(t('suggestion.addSuccess'));
        showAdd.value = false;
        current.value = 1;
        loadList();
      } finally {
        addLoading.value = false;
      }
    });
  }

  onBeforeMount(() => {
    loadList();
  });
</script>

<style lang="less" scoped>
  .suggestion-page {
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  .suggestion-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
  }
  .suggestion-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .suggestion-card {
    padding: 14px 16px;
    border: 1px solid var(--text-n8);
    border-radius: 6px;
    background-color: var(--text-n10);
    transition: all 0.2s ease;
    cursor: pointer;
    &:hover {
      border-color: var(--primary-5);
      box-shadow: 0 2px 8px rgb(0 0 0 / 6%);
    }
  }
  .suggestion-title {
    overflow: hidden;
    font-size: 15px;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n1);
  }
  .suggestion-content {
    display: box;
    overflow: hidden;
    font-size: 13px;
    color: var(--text-n3);
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }
  .suggestion-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: var(--text-n4);
  }
  .suggestion-user {
    color: var(--text-n2);
  }
  .suggestion-org {
    padding: 1px 6px;
    border-radius: 3px;
    color: var(--text-n3);
    background-color: var(--text-n8);
  }
</style>
