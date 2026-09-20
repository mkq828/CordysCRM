<template>
  <CrmCard hide-footer>
    <n-spin :show="loading" class="suggestion-detail">
      <template v-if="detail">
        <div class="suggestion-detail-header">
          <n-button text @click="goBack">
            <template #icon>
              <CrmIcon type="iconicon_arrow_left" :size="16" />
            </template>
            {{ t('suggestion.back') }}
          </n-button>
          <div v-if="canManage" class="flex items-center gap-[8px]">
            <n-select
              v-model:value="editableStatus"
              class="!w-[140px]"
              :options="statusOptions"
              @update:value="onUpdateStatus"
            />
            <n-popconfirm @positive-click="onDelete">
              <template #trigger>
                <n-button size="small" type="error" quaternary>{{ t('suggestion.delete') }}</n-button>
              </template>
              {{ t('suggestion.deleteConfirm') }}
            </n-popconfirm>
          </div>
        </div>

        <div class="suggestion-detail-body">
          <div class="flex items-start gap-[12px]">
            <n-tag size="small" :type="statusTagType[detail.status]" :bordered="false">
              {{ t(`suggestion.status.${detail.status}`) }}
            </n-tag>
            <h1 class="suggestion-detail-title">{{ detail.title }}</h1>
          </div>

          <div class="suggestion-detail-meta">
            <span>{{ detail.userName }}</span>
            <span v-if="detail.organizationName" class="suggestion-org">{{ detail.organizationName }}</span>
            <span>{{ formatTime(detail.createTime) }}</span>
          </div>

          <div class="suggestion-detail-content">{{ detail.content }}</div>

          <div v-if="detail.imageList?.length" class="suggestion-detail-images">
            <n-image
              v-for="img in detail.imageList"
              :key="img.id"
              class="suggestion-detail-image"
              :src="imageSrc(img.id)"
              :preview-src="imageSrc(img.id)"
            />
          </div>

          <div class="suggestion-detail-vote">
            <n-button :type="detail.voted ? 'primary' : 'default'" :ghost="!detail.voted" @click="handleVote">
              <template #icon>
                <CrmIcon type="iconicon_thumb_up" :size="16" />
              </template>
              {{ t('suggestion.vote') }} · {{ detail.voteCount || 0 }}
            </n-button>
          </div>
        </div>

        <n-divider />

        <div class="suggestion-comments">
          <div class="suggestion-comments-title"> {{ t('suggestion.commentCount') }}（{{ commentList.length }}） </div>

          <div class="suggestion-comment-editor">
            <div v-if="replyTarget" class="suggestion-reply-tip">
              {{ t('suggestion.replyTo') }} @{{ replyTarget.name }}
              <CrmIcon type="iconicon_close" :size="12" class="cursor-pointer" @click="replyTarget = null" />
            </div>
            <n-input
              v-model:value="commentContent"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 6 }"
              :placeholder="t('suggestion.commentPlaceholder')"
            />
            <div class="flex justify-end">
              <n-button type="primary" size="small" :loading="commentLoading" @click="submitComment">
                {{ t('suggestion.submitComment') }}
              </n-button>
            </div>
          </div>

          <div v-if="commentList.length" class="suggestion-comment-list">
            <div v-for="comment in commentList" :key="comment.id" class="suggestion-comment-item">
              <div class="suggestion-comment-head">
                <span class="suggestion-comment-user">{{ comment.userName }}</span>
                <span v-if="comment.replyUserName" class="suggestion-comment-reply">
                  {{ t('suggestion.replyTo') }} {{ comment.replyUserName }}
                </span>
                <span class="suggestion-comment-time">{{ formatTime(comment.createTime) }}</span>
              </div>
              <div class="suggestion-comment-content">{{ comment.content }}</div>
              <n-button text size="tiny" type="primary" @click="setReply(comment)">
                {{ t('suggestion.reply') }}
              </n-button>
            </div>
          </div>
          <n-empty v-else :description="t('suggestion.commentPlaceholder')" class="py-[32px]" />
        </div>
      </template>
    </n-spin>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { useRoute, useRouter } from 'vue-router';
  import { NButton, NDivider, NEmpty, NImage, NInput, NPopconfirm, NSelect, NSpin, NTag, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SuggestionCommentItem, SuggestionItem, SuggestionStatus } from '@lib/shared/models/system/suggestion';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import {
    addSuggestionComment,
    deleteSuggestion,
    getSuggestionCommentList,
    getSuggestionDetail,
    updateSuggestionStatus,
    voteSuggestion,
  } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import { SuggestionRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const suggestionId = computed(() => route.params.id as string);

  const loading = ref(false);
  const detail = ref<SuggestionItem | null>(null);
  const commentList = ref<SuggestionCommentItem[]>([]);

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

  const canManage = computed(() => userStore.isAdmin || detail.value?.userId === userStore.userInfo.id);
  const editableStatus = ref<SuggestionStatus | null>(null);

  function imageSrc(id: string) {
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  function formatTime(ts?: number) {
    return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '';
  }

  function goBack() {
    router.push({ name: SuggestionRouteEnum.SUGGESTION_INDEX });
  }

  async function loadDetail() {
    loading.value = true;
    try {
      detail.value = await getSuggestionDetail(suggestionId.value);
      editableStatus.value = detail.value.status;
    } finally {
      loading.value = false;
    }
  }

  async function loadComments() {
    commentList.value = await getSuggestionCommentList(suggestionId.value);
  }

  async function handleVote() {
    if (!detail.value) return;
    const res = await voteSuggestion(detail.value.id);
    detail.value.voteCount = res.voteCount;
    detail.value.voted = res.voted;
    Message.success(res.voted ? t('suggestion.voteSuccess') : t('suggestion.unvoteSuccess'));
  }

  async function onUpdateStatus(status: SuggestionStatus) {
    try {
      await updateSuggestionStatus({ id: suggestionId.value, status });
      if (detail.value) detail.value.status = status;
      Message.success(t('suggestion.statusSuccess'));
    } catch (error) {
      editableStatus.value = detail.value?.status || null;
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function onDelete() {
    try {
      await deleteSuggestion(suggestionId.value);
      Message.success(t('suggestion.deleteSuccess'));
      goBack();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  // ---- 评论 ----
  const commentLoading = ref(false);
  const commentContent = ref('');
  const replyTarget = ref<{ id: string; name: string } | null>(null);

  function setReply(comment: SuggestionCommentItem) {
    replyTarget.value = { id: comment.id, name: comment.userName || '' };
  }

  async function submitComment() {
    const content = commentContent.value.trim();
    if (!content) return;
    commentLoading.value = true;
    try {
      await addSuggestionComment({
        suggestionId: suggestionId.value,
        content,
        replyCommentId: replyTarget.value?.id,
      });
      commentContent.value = '';
      replyTarget.value = null;
      Message.success(t('suggestion.commentSuccess'));
      await loadComments();
      if (detail.value) {
        detail.value.commentCount = (detail.value.commentCount || 0) + 1;
      }
    } finally {
      commentLoading.value = false;
    }
  }

  onBeforeMount(async () => {
    await loadDetail();
    await loadComments();
  });
</script>

<style lang="less" scoped>
  .suggestion-detail {
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  .suggestion-detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
  }
  .suggestion-detail-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .suggestion-detail-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .suggestion-detail-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 13px;
    color: var(--text-n4);
  }
  .suggestion-org {
    padding: 1px 6px;
    border-radius: 3px;
    color: var(--text-n3);
    background-color: var(--text-n8);
  }
  .suggestion-detail-content {
    font-size: 14px;
    white-space: pre-wrap;
    color: var(--text-n2);
    line-height: 22px;
  }
  .suggestion-detail-images {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .suggestion-detail-image {
    overflow: hidden;
    width: 96px;
    height: 96px;
    border-radius: 6px;
  }
  .suggestion-detail-vote {
    display: flex;
  }
  .suggestion-comments-title {
    margin-bottom: 12px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-n1);
  }
  .suggestion-comment-editor {
    display: flex;
    margin-bottom: 16px;
    flex-direction: column;
    gap: 8px;
  }
  .suggestion-reply-tip {
    display: flex;
    align-items: center;
    padding: 4px 8px;
    font-size: 12px;
    border-radius: 4px;
    color: var(--text-n3);
    background-color: var(--text-n9);
    gap: 8px;
  }
  .suggestion-comment-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .suggestion-comment-item {
    display: flex;
    padding: 10px 12px;
    border-radius: 6px;
    background-color: var(--text-n10);
    flex-direction: column;
    gap: 4px;
  }
  .suggestion-comment-head {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
  }
  .suggestion-comment-user {
    font-weight: 500;
    color: var(--text-n1);
  }
  .suggestion-comment-reply {
    color: var(--primary-6);
  }
  .suggestion-comment-time {
    color: var(--text-n4);
  }
  .suggestion-comment-content {
    font-size: 13px;
    white-space: pre-wrap;
    color: var(--text-n2);
  }
</style>
