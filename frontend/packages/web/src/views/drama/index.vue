<template>
  <div class="drama-page pf-drama-list">
    <!-- 页头：标题 + 新建项目 -->
    <header class="pf-drama-list-head">
      <div class="pf-drama-list-title-row">
        <div>
          <h1>{{ t('drama.title') }}</h1>
          <p class="pf-muted" style="margin-top: 0.35rem">{{ t('drama.desc') }}</p>
        </div>
        <div class="pf-drama-list-actions">
          <button type="button" class="pf-btn pf-btn-lime pf-btn-sm" :disabled="!available" @click="showCreate = true">
            + {{ t('drama.newProject') }}
          </button>
        </div>
      </div>

      <div class="pf-drama-list-toolbar">
        <div class="pf-pill-tabs">
          <button
            v-for="key in FILTER_KEYS"
            :key="key"
            type="button"
            class="pf-pill"
            :class="{ active: filter === key }"
            @click="filter = key"
          >
            {{ t(`drama.filter.${key}`) }}
          </button>
        </div>
        <label class="pf-drama-search">
          <span aria-hidden>🔍</span>
          <input v-model="query" type="text" :placeholder="t('drama.searchPlaceholder')" />
        </label>
      </div>
    </header>

    <!-- 内联新建面板（AI 生剧本） -->
    <section v-if="showCreate" class="drama-agent-panel pf-drama-create-panel">
      <div class="drama-agent-panel-head">
        <div class="drama-agent-tabs" role="tablist">
          <button type="button" role="tab" aria-selected class="active"> ✨ {{ t('drama.aiScript') }} </button>
        </div>
        <button type="button" class="pf-link pf-drama-collapse" @click="showCreate = false">
          {{ t('drama.collapse') }}
        </button>
      </div>
      <div class="drama-agent-tips" role="note">
        <span aria-hidden>✨</span>
        <span>{{ t('drama.tip') }}</span>
      </div>
      <div class="drama-agent-ai">
        <label class="drama-agent-ai-label" for="drama-agent-story">{{ t('drama.storyLabel') }}</label>
        <div class="drama-agent-ai-field" :class="{ 'has-value': storyText.trim() }">
          <textarea
            id="drama-agent-story"
            v-model="storyText"
            :disabled="busy"
            :placeholder="t('drama.storyPlaceholder')"
            rows="7"
            :maxlength="CREATIVE_MAX_LENGTH + 50"
          />
          <span class="drama-agent-char-count" :class="charCountClass" aria-live="polite">
            {{ storyLen }}<span class="drama-agent-char-sep">/</span>{{ CREATIVE_MAX_LENGTH }}
          </span>
        </div>
        <div class="drama-agent-ai-footer">
          <div class="drama-agent-ai-options">
            <DramaImageStyleModal v-model:value="imageStyleId" :disabled="busy" />
            <span class="drama-agent-opt-divider" aria-hidden />
            <div class="pf-pill-tabs">
              <button
                v-for="count in EPISODE_COUNT_PRESETS"
                :key="count"
                type="button"
                class="pf-pill"
                :class="{ active: episodeCount === count }"
                :disabled="busy"
                @click="episodeCount = count"
              >
                {{ t('drama.episodeCount', { count }) }}
              </button>
            </div>
            <n-input-number
              :value="episodeCount"
              :min="1"
              :max="EPISODE_MAX_COUNT"
              :show-button="false"
              :disabled="busy"
              size="small"
              :placeholder="t('drama.episodeCustom')"
              style="width: 92px"
              @update:value="onCustomEpisode"
            />
          </div>
          <button
            type="button"
            class="pf-btn pf-btn-lime drama-agent-generate-btn"
            :disabled="!canGenerate"
            @click="handleGenerate"
          >
            <ShimmerText v-if="busy" size="13px">{{ t('drama.generating') }}</ShimmerText>
            <template v-else>✨ {{ t('drama.generate') }}</template>
          </button>
        </div>
      </div>
    </section>

    <!-- 版本门控提示 -->
    <div v-if="available === false" class="pf-empty-state is-compact" style="margin-top: 1rem">
      <p class="pf-muted">{{ t('drama.locked') }}</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="pf-error drama-error drama-agent-error">{{ error }}</div>

    <!-- 列表内容 -->
    <div v-if="loading" class="pf-drama-card-grid" aria-busy="true">
      <div v-for="i in 6" :key="i" class="pf-drama-card is-skeleton" />
    </div>
    <div v-else-if="items.length === 0" class="pf-empty-state">
      <div class="pf-empty-illust" aria-hidden>📁</div>
      <h2>{{ t('drama.emptyTitle') }}</h2>
      <p class="pf-muted">{{ t('drama.emptyDesc') }}</p>
      <button type="button" class="pf-btn pf-btn-lime" @click="showCreate = true">
        {{ t('drama.newProject') }}
      </button>
    </div>
    <div v-else-if="filteredItems.length === 0" class="pf-empty-state is-compact">
      <p class="pf-muted">{{ t('drama.noMatch') }}</p>
      <button type="button" class="pf-btn pf-btn-ghost pf-btn-sm" @click="clearFilter">
        {{ t('drama.clearFilter') }}
      </button>
    </div>
    <div v-else class="pf-drama-card-grid">
      <button type="button" class="pf-drama-card pf-drama-card-new" @click="showCreate = true">
        <span class="pf-drama-card-plus" aria-hidden>+</span>
        <strong>{{ t('drama.newProject') }}</strong>
      </button>
      <article
        v-for="item in filteredItems"
        :key="item.id"
        class="pf-drama-card"
        :class="{ 'is-selected': selected.has(item.id) }"
      >
        <button
          type="button"
          class="pf-drama-card-cover"
          :aria-label="`${t('drama.newProject')} ${item.title}`"
          @click="openProject(item)"
        >
          <img
            v-if="item.coverAttachmentId"
            :src="previewUrl(item.coverAttachmentId)"
            alt=""
            class="pf-drama-card-cover-img"
            loading="lazy"
          />
          <span v-else class="pf-drama-card-cover-fallback">{{ verticalTitleLabel(item.title) }}</span>
          <label
            class="drama-project-row-check"
            :class="{ 'is-visible': selected.has(item.id) || selectionMode }"
            @click.stop
          >
            <input type="checkbox" :checked="selected.has(item.id)" @change="toggleSelect(item.id)" />
          </label>
        </button>
        <div class="pf-drama-card-body">
          <div class="pf-drama-card-top">
            <button type="button" class="pf-drama-card-title" @click="openProject(item)">
              {{ item.title }}
            </button>
            <n-dropdown trigger="click" :options="cardMenuOptions" @select="(key) => onCardMenu(key, item)">
              <button type="button" class="pf-btn pf-btn-ghost pf-btn-sm pf-btn-icon">⋯</button>
            </n-dropdown>
          </div>
          <p class="pf-drama-card-meta">{{ cardMeta(item) }}</p>
          <p class="pf-drama-card-time">{{ formatTime(item.updateTime ?? item.createTime) }}</p>
        </div>
      </article>
    </div>

    <!-- 多选删除条 -->
    <div v-if="selected.size > 0" class="drama-project-selection-bar">
      <div class="drama-project-selection-inner">
        <span>{{ t('drama.selected', { count: selected.size }) }}</span>
        <button type="button" class="drama-project-selection-cancel" :disabled="deleting" @click="selected = new Set()">
          {{ t('drama.cancelSelect') }}
        </button>
        <button type="button" class="drama-project-selection-delete" :disabled="deleting" @click="handleDeleteSelected">
          🗑 {{ deleting ? t('drama.deleting') : t('drama.delete') }}
        </button>
      </div>
    </div>

    <!-- 重命名弹窗 -->
    <n-modal v-model:show="renameVisible" preset="card" :title="t('drama.renameTitle')" class="!w-[420px]">
      <div class="flex flex-col gap-[14px]">
        <n-input v-model:value="renameValue" :placeholder="t('drama.renameMessage')" @keyup.enter="submitRename" />
        <div class="flex justify-end gap-[8px]">
          <n-button @click="renameVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="renameSaving" @click="submitRename">{{ t('common.save') }}</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { type DropdownOption, NButton, NDropdown, NInput, NInputNumber, NModal, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DramaProject } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import DramaImageStyleModal from './DramaImageStyleModal.vue';

  import {
    createAiDramaProject,
    deleteAiDramaProject,
    getAiDramaAvailable,
    getAiDramaProjects,
    updateAiDramaProject,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import { DramaRouteEnum } from '@/enums/routeEnum';

  import './printfilm.css';
  import { EPISODE_COUNT_PRESETS } from './templates';

  const CREATIVE_MIN_LENGTH = 20;
  const CREATIVE_MAX_LENGTH = 50000;
  const EPISODE_MAX_COUNT = 200;

  type ProjectFilter = 'all' | 'running' | 'done' | 'draft';
  const FILTER_KEYS: ProjectFilter[] = ['all', 'running', 'done', 'draft'];

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const userStore = useUserStore();
  const router = useRouter();

  const available = ref<boolean | null>(null);
  const items = ref<DramaProject[]>([]);
  const loading = ref(true);
  const error = ref('');

  const storyText = ref('');
  const episodeCount = ref(12);
  const imageStyleId = ref<string | null>(null);
  const busy = ref(false);
  const showCreate = ref(false);

  const selected = ref<Set<string>>(new Set());
  const deleting = ref(false);
  const filter = ref<ProjectFilter>('all');
  const query = ref('');

  const renameVisible = ref(false);
  const renameValue = ref('');
  const renameSaving = ref(false);
  const renameTarget = ref<DramaProject | null>(null);

  const cardMenuOptions: DropdownOption[] = [
    { label: t('drama.rename'), key: 'rename' },
    { label: t('drama.delete'), key: 'delete' },
  ];

  const storyLen = computed(() => storyText.value.trim().length);
  const canGenerate = computed(
    () =>
      storyLen.value >= CREATIVE_MIN_LENGTH &&
      storyLen.value <= CREATIVE_MAX_LENGTH &&
      !busy.value &&
      available.value !== false
  );
  const charCountClass = computed(() => {
    if (storyLen.value > CREATIVE_MAX_LENGTH) {
      return ' is-over';
    }
    if (storyLen.value >= CREATIVE_MIN_LENGTH) {
      return ' is-ok';
    }
    return '';
  });
  const selectionMode = computed(() => selected.value.size > 0);

  const filteredItems = computed(() =>
    items.value.filter((item) => {
      if (filter.value === 'draft' && item.hasScript) {
        return false;
      }
      if (filter.value === 'running' && !(item.hasScript && (item.episodeCount || 0) > 0)) {
        return false;
      }
      if (filter.value === 'done' && !(item.hasScript && (item.episodeCount || 0) >= 8)) {
        return false;
      }
      const q = query.value.trim().toLowerCase();
      if (q && !(item.title || '').toLowerCase().includes(q)) {
        return false;
      }
      return true;
    })
  );

  function previewUrl(id: string): string {
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  function verticalTitleLabel(name?: string | null, max = 12): string {
    const clean = (name || '').replace(/\s+/g, '');
    if (clean.length <= max) {
      return clean;
    }
    return `${clean.slice(0, max - 1)}…`;
  }

  function formatTime(ts?: number | null): string {
    if (!ts) {
      return '-';
    }
    const d = dayjs(ts);
    return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-';
  }

  function cardMeta(item: DramaProject): string {
    if (!item.hasScript) {
      return t('drama.draft');
    }
    return t('drama.written', { episode: item.episodeCount ?? 0, asset: item.assetCount ?? 0 });
  }

  async function loadProjects() {
    const rows = await getAiDramaProjects();
    items.value = rows ?? [];
    const ids = new Set(items.value.map((r) => r.id));
    selected.value = new Set([...selected.value].filter((id) => ids.has(id)));
  }

  function toggleSelect(id: string) {
    const next = new Set(selected.value);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    selected.value = next;
  }

  function openProject(item: DramaProject) {
    if (selectionMode.value) {
      toggleSelect(item.id);
      return;
    }
    router.push({ name: DramaRouteEnum.DRAMA_WORKSPACE, params: { projectId: item.id } });
  }

  function clearFilter() {
    filter.value = 'all';
    query.value = '';
  }

  function onCustomEpisode(value: number | null) {
    if (value == null) {
      return;
    }
    episodeCount.value = Math.max(1, Math.min(EPISODE_MAX_COUNT, Math.floor(value)));
  }

  async function handleGenerate() {
    const source = storyText.value.trim();
    if (source.length < CREATIVE_MIN_LENGTH) {
      error.value = t('drama.creativeTooShort');
      return;
    }
    if (source.length > CREATIVE_MAX_LENGTH) {
      error.value = t('drama.creativeTooLong');
      return;
    }
    busy.value = true;
    error.value = '';
    try {
      await createAiDramaProject({
        source,
        episodeCount: episodeCount.value,
        imageStyleId: imageStyleId.value ?? undefined,
        title: source.slice(0, 40),
      });
      Message.success(t('common.createSuccess'));
      showCreate.value = false;
      storyText.value = '';
      imageStyleId.value = null;
      await loadProjects();
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.createFailed');
    } finally {
      busy.value = false;
    }
  }

  function openRename(item: DramaProject) {
    renameTarget.value = item;
    renameValue.value = item.title || '';
    renameVisible.value = true;
  }

  async function submitRename() {
    const target = renameTarget.value;
    const name = renameValue.value.trim();
    if (!target || !name || name === target.title) {
      renameVisible.value = false;
      return;
    }
    renameSaving.value = true;
    try {
      await updateAiDramaProject(target.id, { title: name });
      Message.success(t('common.saveSuccess'));
      renameVisible.value = false;
      await loadProjects();
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.renameFailed');
    } finally {
      renameSaving.value = false;
    }
  }

  function handleDeleteOne(item: DramaProject) {
    openModal({
      type: 'error',
      title: t('drama.deleteTitle'),
      content: t('drama.deleteOne', { title: item.title }),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiDramaProject(item.id);
          selected.value = new Set([...selected.value].filter((id) => id !== item.id));
          Message.success(t('common.deleteSuccess'));
          await loadProjects();
        } catch (err) {
          error.value = (err as Error)?.message || t('drama.deleteFailed');
        }
      },
    });
  }

  function onCardMenu(key: string | number, item: DramaProject) {
    if (key === 'rename') {
      openRename(item);
    } else if (key === 'delete') {
      handleDeleteOne(item);
    }
  }

  async function handleDeleteSelected() {
    const ids = [...selected.value];
    if (ids.length === 0) {
      return;
    }
    openModal({
      type: 'error',
      title: t('drama.deleteTitle'),
      content: t('drama.deleteMany', { count: ids.length }),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        deleting.value = true;
        try {
          await Promise.all(ids.map((id) => deleteAiDramaProject(id)));
          selected.value = new Set();
          Message.success(t('common.deleteSuccess'));
          await loadProjects();
        } catch (err) {
          error.value = (err as Error)?.message || t('drama.deleteFailed');
        } finally {
          deleting.value = false;
        }
      },
    });
  }

  onMounted(async () => {
    loading.value = true;
    try {
      try {
        available.value = await getAiDramaAvailable();
      } catch {
        available.value = true;
      }
      await loadProjects();
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.loadFailed');
    } finally {
      loading.value = false;
    }
  });
</script>
