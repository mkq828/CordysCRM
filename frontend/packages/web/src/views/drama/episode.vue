<template>
  <div class="drama-episode-page">
    <!-- 顶栏：返回 + 剧名 + 本集标题 + 操作 -->
    <header class="drama-ep-top">
      <div class="drama-ep-top-left">
        <button type="button" class="drama-icon-btn" :aria-label="t('drama.backToWorkspace')" @click="goWorkspace">
          ←
        </button>
        <div class="drama-ep-title">
          <h1>{{ project?.title || '' }}</h1>
          <p>
            {{ t('drama.episodeNo', { n: activeEpisodeNumber }) }} ·
            {{ activeBody?.title || t('drama.episodeNo', { n: activeEpisodeNumber }) }}
          </p>
        </div>
      </div>
      <div class="drama-ep-top-actions">
        <button type="button" class="pf-btn pf-btn-ghost" :disabled="!activeRow || planning" @click="handlePlan">
          <ShimmerText v-if="planning" size="13px">{{ t('drama.planning') }}</ShimmerText>
          <template v-else>{{ t('drama.planFragments') }}</template>
        </button>
        <button
          type="button"
          class="pf-btn pf-btn-lime"
          :disabled="!drafts.length || genBusy"
          @click="handleGenerate()"
        >
          <ShimmerText v-if="genBusy" size="13px">{{ t('drama.generatingImage') }}</ShimmerText>
          <template v-else>{{ t('drama.generateAll') }}</template>
        </button>
        <button type="button" class="pf-btn pf-btn-ghost" :disabled="saveBusy" @click="handleSave">
          {{ t('drama.saveFragments') }}
        </button>
        <button
          type="button"
          class="pf-btn pf-btn-ghost"
          :disabled="!drafts.length || narrating"
          @click="handleNarration"
        >
          <ShimmerText v-if="narrating" size="13px">{{ t('drama.narrating') }}</ShimmerText>
          <template v-else>{{ t('drama.generateNarration') }}</template>
        </button>
        <button
          type="button"
          class="pf-btn pf-btn-ghost"
          :disabled="!drafts.length || videomaking"
          @click="handleGenerateVideo"
        >
          <ShimmerText v-if="videomaking" size="13px">{{ t('drama.videoing') }}</ShimmerText>
          <template v-else>{{ t('drama.generateVideo') }}</template>
        </button>
        <button type="button" class="pf-btn pf-btn-lime" :disabled="!drafts.length || composing" @click="handleCompose">
          <ShimmerText v-if="composing" size="13px">{{ t('drama.composing') }}</ShimmerText>
          <template v-else>{{ t('drama.compose') }}</template>
        </button>
        <button type="button" class="pf-btn pf-btn-ghost" :disabled="!hasVideo" @click="handleDownload">
          {{ t('drama.download') }}
        </button>
      </div>
    </header>

    <div class="drama-ep-steps">
      <DramaStepBar :current="episodeStage" />
    </div>

    <p v-if="error" class="pf-error drama-error drama-ep-banner">{{ error }}</p>

    <main class="drama-ep-main">
      <!-- 左栏：分集目录 -->
      <aside class="drama-ep-dir">
        <div class="drama-ep-dir-head">
          <h3>{{ t('drama.episodeDirectory') }}</h3>
        </div>
        <ul v-if="directory.length" class="drama-ep-dir-list">
          <li v-for="ep in directory" :key="ep.episodeNumber" class="drama-ep-dir-item">
            <button
              type="button"
              class="drama-ep-dir-card"
              :class="{ 'is-active': ep.episodeNumber === activeEpisodeNumber }"
              @click="activeEpisodeNumber = ep.episodeNumber"
            >
              <span class="drama-ep-dir-no">{{ ep.episodeNumber }}</span>
              <span class="drama-ep-dir-meta">
                <strong>{{ ep.title || t('drama.episodeNo', { n: ep.episodeNumber }) }}</strong>
                <small>{{ fragmentCountOf(ep.episodeNumber) }}</small>
              </span>
            </button>
            <span class="drama-ep-dir-ops">
              <button
                type="button"
                class="drama-ep-dir-op"
                :title="t('drama.rename')"
                @click.stop="openRenameEpisode(ep)"
                >✎</button
              >
              <button
                type="button"
                class="drama-ep-dir-op danger"
                :title="t('drama.delete')"
                @click.stop="handleDeleteEpisode(ep)"
                >🗑</button
              >
            </span>
          </li>
        </ul>
        <p v-else class="pf-muted">{{ t('drama.noEpisodes') }}</p>
      </aside>

      <!-- 中栏：分镜编辑器 -->
      <section class="drama-ep-editor">
        <div v-if="planning" class="drama-ep-status"
          ><ShimmerText size="15px">{{ t('drama.planning') }}</ShimmerText></div
        >
        <div v-else-if="planError" class="pf-error drama-error" style="margin-bottom: 12px">{{ planError }}</div>
        <div v-else-if="!activeRow" class="drama-ep-status">{{ t('drama.noEpisodeRow') }}</div>
        <div v-else-if="!drafts.length" class="drama-ep-empty">
          <p>{{ t('drama.noFragments') }}</p>
          <button type="button" class="pf-btn pf-btn-lime" :disabled="!canPlan" @click="handlePlan">
            {{ t('drama.planFragments') }}
          </button>
        </div>

        <div v-else class="drama-ep-fragments">
          <article
            v-for="(fragment, index) in drafts"
            :key="fragment.id || `new-${index}`"
            class="drama-ep-frag"
            :class="{ 'is-active': fragment.id === selectedId }"
            @click="selectedId = fragment.id"
          >
            <header class="drama-ep-frag-head">
              <span class="drama-ep-frag-no">#{{ index + 1 }}</span>
              <label class="drama-ep-frag-dur">
                <input v-model.number="fragment.durationSec" type="number" min="4" max="15" />
                {{ t('drama.seconds') }}
              </label>
              <div class="drama-ep-frag-actions">
                <button type="button" :disabled="index === 0" @click.stop="moveFragment(index, -1)">↑</button>
                <button type="button" :disabled="index === drafts.length - 1" @click.stop="moveFragment(index, 1)"
                  >↓</button
                >
                <button type="button" :disabled="genBusyIds.has(fragment.id)" @click.stop="generateOne(fragment)">
                  <ShimmerText v-if="genBusyIds.has(fragment.id)" size="13px">{{
                    t('drama.generatingImage')
                  }}</ShimmerText>
                  <template v-else>{{
                    fragment.coverAttachmentId ? t('drama.regenShot') : t('drama.genShot')
                  }}</template>
                </button>
                <button
                  v-if="fragment.narration"
                  type="button"
                  :disabled="previewBusyId === fragment.id"
                  @click.stop="handlePreviewVoice(fragment)"
                >
                  <ShimmerText v-if="previewBusyId === fragment.id" size="13px">{{
                    t('drama.previewing')
                  }}</ShimmerText>
                  <template v-else>{{ t('drama.previewVoice') }}</template>
                </button>
                <button type="button" class="drama-btn-danger-text" @click.stop="removeFragment(index)">
                  {{ t('drama.delete') }}
                </button>
              </div>
            </header>

            <label class="drama-ep-field">
              <span>{{ t('drama.fragmentContent') }}</span>
              <textarea v-model="fragment.content" rows="5" :placeholder="t('drama.fragmentContentPlaceholder')" />
            </label>

            <label class="drama-ep-field">
              <span>{{ t('drama.fragmentNarration') }}</span>
              <textarea v-model="fragment.narration" rows="3" :placeholder="t('drama.fragmentNarrationPlaceholder')" />
            </label>

            <label class="drama-ep-field">
              <span>{{ t('drama.fragmentVoice') }}</span>
              <n-select
                v-model:value="fragment.voice"
                :options="voiceOptions"
                filterable
                clearable
                :placeholder="t('drama.fragmentVoicePlaceholder')"
              />
            </label>

            <label class="drama-ep-field">
              <span>{{ t('drama.fragmentAssets') }}</span>
              <n-select
                v-model:value="fragment.assetIds"
                :options="assetOptions"
                multiple
                filterable
                clearable
                :placeholder="t('drama.fragmentAssetsPlaceholder')"
              />
            </label>

            <button
              v-if="fragment.coverAttachmentId"
              type="button"
              class="drama-ep-frag-cover"
              @click.stop="selectedId = fragment.id"
            >
              <img :src="coverUrl(fragment.coverAttachmentId)" :alt="t('drama.preview')" />
            </button>
            <span v-else class="drama-ep-frag-noimage">{{ t('drama.noImage') }}</span>
            <span v-if="fragment.videoAttachmentId" class="drama-ep-frag-video">🎬 {{ t('drama.hasVideo') }}</span>
            <span v-if="fragment.narrationAttachmentId" class="drama-ep-frag-voice">🎙 {{ t('drama.voiceReady') }}</span>
          </article>

          <button type="button" class="drama-ep-add" @click="addFragment">+ {{ t('drama.addFragment') }}</button>
        </div>
      </section>

      <!-- 右栏：预览 -->
      <aside class="drama-ep-preview">
        <h3>{{ t('drama.preview') }}</h3>
        <div v-if="previewVideoSrc" class="drama-ep-preview-media">
          <video :src="previewVideoSrc" controls loop playsinline />
          <p class="pf-muted drama-ep-preview-hint">{{ t('drama.previewVideoSilentHint') }}</p>
        </div>
        <div v-else-if="previewSrc" class="drama-ep-preview-media">
          <img :src="previewSrc" alt="preview" />
        </div>
        <p v-else class="pf-muted">{{ t('drama.previewEmpty') }}</p>

        <!-- 选中镜的口播配音：已生成则绿色可播放，否则灰色禁用 -->
        <div
          v-if="selectedFragment && (selectedFragment.narration || selectedFragment.narrationAttachmentId)"
          class="drama-ep-preview-voice"
        >
          <span class="drama-ep-preview-voice-label">🎙 {{ t('drama.narrationPlayer') }}</span>
          <button
            type="button"
            class="drama-ep-preview-play"
            :disabled="!selectedFragment.narrationAttachmentId"
            :title="
              selectedFragment.narrationAttachmentId
                ? playingNarrationId === selectedFragment.id
                  ? t('drama.stopVoice')
                  : t('drama.playVoice')
                : t('drama.noVoiceYet')
            "
            @click="toggleNarrationAudio(selectedFragment)"
          >
            <CrmIcon
              :type="playingNarrationId === selectedFragment.id ? 'iconicon_pause' : 'iconicon_play'"
              :size="14"
            />
          </button>
          <span v-if="selectedFragment.narrationAttachmentId" class="drama-ep-voice-badge">{{
            t('drama.voiceReady')
          }}</span>
        </div>
      </aside>
    </main>

    <!-- 底栏：分镜条 -->
    <footer v-if="drafts.length" class="drama-ep-strip">
      <button
        v-for="(fragment, index) in drafts"
        :key="fragment.id || `strip-${index}`"
        type="button"
        class="drama-ep-strip-item"
        :class="{ 'is-active': fragment.id === selectedId }"
        @click="selectedId = fragment.id"
      >
        <img v-if="fragment.coverAttachmentId" :src="coverUrl(fragment.coverAttachmentId)" :alt="`#${index + 1}`" />
        <span v-else class="drama-ep-strip-no">{{ index + 1 }}</span>
      </button>
    </footer>

    <!-- 重命名分集弹窗 -->
    <n-modal v-model:show="renameVisible" preset="card" :title="t('drama.renameEpisodeTitle')" class="!w-[420px]">
      <n-input
        v-model:value="renameValue"
        :placeholder="t('drama.renameEpisodeMessage')"
        @keyup.enter="submitRenameEpisode"
      />
      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button :disabled="renameSaving" @click="renameVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="renameSaving" @click="submitRenameEpisode">{{
            t('common.save')
          }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { NButton, NInput, NModal, NSelect, useMessage } from 'naive-ui';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AiVideoVoice,
    DramaAsset,
    DramaEpisode,
    DramaEpisodeBody,
    DramaFragment,
    DramaProject,
    DramaScript,
  } from '@lib/shared/models/ai';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import DramaStepBar from './DramaStepBar.vue';

  import {
    composeAiDramaEpisode,
    deleteAiDramaEpisode,
    downloadAttachment,
    generateAiDramaFragments,
    generateAiDramaFragmentVideos,
    generateAiDramaNarration,
    getAiDramaAssets,
    getAiDramaEpisodes,
    getAiDramaGenerateStatus,
    getAiDramaProject,
    getAiDramaScript,
    getAiDramaVoices,
    planAiDramaFragments,
    previewAiDramaFragmentVoice,
    renameAiDramaEpisode,
    saveAiDramaFragments,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import { DramaRouteEnum } from '@/enums/routeEnum';

  import './printfilm.css';
  import {
    buildOutlineDirectory,
    isSubstantialEpisodeBody,
    parseEpisodeBodies,
    parseSummary,
    resolveEpisodeTarget,
  } from './dramaWorkspaceUtils';

  type FragmentDraft = DramaFragment & { assetIds: string[] };

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const router = useRouter();
  const route = useRoute();
  const userStore = useUserStore();

  const projectId = String(route.params.projectId || '');
  const routeEpisodeNumber = Number(route.params.episodeNumber || 1);

  const project = ref<DramaProject | null>(null);
  const script = ref<DramaScript | null>(null);
  const episodes = ref<DramaEpisode[]>([]);
  const assets = ref<DramaAsset[]>([]);
  const voiceOptions = ref<AiVideoVoice[]>([]);
  const activeEpisodeNumber = ref(routeEpisodeNumber || 1);
  const drafts = ref<FragmentDraft[]>([]);
  const selectedId = ref('');
  const loading = ref(true);
  const error = ref('');

  const planBusy = ref(false);
  const saveBusy = ref(false);
  const genBusy = ref(false);
  const genBusyIds = ref<Set<string>>(new Set());
  const planError = ref('');

  // 分集重命名/删除
  const renameVisible = ref(false);
  const renameValue = ref('');
  const renameTarget = ref<number | null>(null);
  const renameSaving = ref(false);

  const narrationBusy = ref(false);
  const videoBusy = ref(false);
  const composeBusy = ref(false);
  const previewBusyId = ref('');
  const previewAudioUrl = ref('');
  // 正在播放「已生成配音」的镜头 id（右栏播放按钮的播放/停止态）
  const playingNarrationId = ref('');
  let narrationAudio: HTMLAudioElement | null = null;

  const summary = computed(() => parseSummary(script.value));
  const episodeBodies = computed(() => parseEpisodeBodies(script.value));
  const episodeCount = computed(() => resolveEpisodeTarget(summary.value, script.value, project.value));
  const directory = computed(() => buildOutlineDirectory(episodeBodies.value, episodeCount.value));

  const activeBody = computed(() => episodeBodies.value.find((ep) => ep.episodeNumber === activeEpisodeNumber.value));
  const activeRow = computed(() => episodes.value.find((ep) => ep.sortOrder === activeEpisodeNumber.value));
  const planning = computed(() => activeRow.value?.status === 'planning');
  const canPlan = computed(() => Boolean(activeRow.value && isSubstantialEpisodeBody(activeBody.value?.body)));
  const narrating = computed(() => narrationBusy.value || activeRow.value?.narrationStatus === 'running');
  const videomaking = computed(() => videoBusy.value || activeRow.value?.videoStatus === 'running');
  const composing = computed(() => composeBusy.value || activeRow.value?.composeStatus === 'running');
  const hasVideo = computed(() => Boolean(activeRow.value?.videoAttachmentId));

  // 当前所处流程步骤（流程条）：4 确认分镜 / 6 镜头视频 / 7 合成预览
  // 「画面与配音」完成即进入「镜头视频」，「镜头视频」完成即进入最后的「合成预览」
  const episodeStage = computed(() => {
    const row = activeRow.value;
    if (!row) return 4;
    // 合成完成或镜头视频已出 → 「镜头视频」这一步已点亮 ✓，停在最后的「合成预览」
    if (row.composeStatus === 'completed' || row.videoStatus === 'completed') return 7;
    // 画面与配音完成 → 进入「镜头视频」
    if (
      row.narrationStatus === 'completed' ||
      (row.fragments?.length && row.fragments.every((f) => f.coverAttachmentId))
    )
      return 6;
    return 4;
  });

  function coverUrl(attachmentId?: string | null): string {
    return attachmentId ? `${PreviewAttachmentUrl}/${attachmentId}?userId=${userStore.userInfo.id}` : '';
  }

  const assetOptions = computed(() => assets.value.map((a) => ({ label: a.name || t('drama.unnamed'), value: a.id })));
  const selectedFragment = computed(() => drafts.value.find((d) => d.id && d.id === selectedId.value) || null);
  // 右栏预览跟随当前选中的分镜：有镜头视频优先播视频，否则回退画面
  const previewVideoSrc = computed(() => coverUrl(selectedFragment.value?.videoAttachmentId));
  const previewSrc = computed(() => coverUrl(selectedFragment.value?.coverAttachmentId));

  function fragmentCountOf(episodeNumber: number): string {
    const row = episodes.value.find((ep) => ep.sortOrder === episodeNumber);
    const count = row?.fragments?.length ?? 0;
    return t('drama.fragmentCount', { count });
  }

  function goWorkspace() {
    router.push({ name: DramaRouteEnum.DRAMA_WORKSPACE, params: { projectId } });
  }

  function parseAssetIds(json?: string | null): string[] {
    if (!json) return [];
    try {
      const parsed = JSON.parse(json);
      return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
    } catch {
      return [];
    }
  }

  /** 台词里每个「角色：」标记前补换行，一行一个说话人，看得更清晰 */
  function formatDialogue(text?: string | null): string {
    if (!text) return '';
    return text.replace(/(?<=[^\n一-龥A-Za-z0-9·])(?=[一-龥A-Za-z0-9·]{1,10}(?:[（(][^（）)]*[）)])?[：:])/g, '\n');
  }

  function syncDraftsFromRow() {
    const row = activeRow.value;
    const next: FragmentDraft[] = (row?.fragments || []).map((f) => ({
      ...f,
      assetIds: parseAssetIds(f.assetIdsJson),
      narration: formatDialogue(f.narration),
    }));
    drafts.value = next;
    selectedId.value = next[0]?.id || '';
  }

  watch(activeRow, () => {
    syncDraftsFromRow();
  });

  async function load() {
    loading.value = true;
    error.value = '';
    try {
      project.value = await getAiDramaProject(projectId);
      if (project.value?.script) script.value = project.value.script;
      else script.value = await getAiDramaScript(projectId);
      episodes.value = await getAiDramaEpisodes(projectId);
      assets.value = await getAiDramaAssets(projectId);
      try {
        voiceOptions.value = await getAiDramaVoices();
      } catch {
        voiceOptions.value = [];
      }
      syncDraftsFromRow();
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.loadFailed');
    } finally {
      loading.value = false;
    }
  }

  async function refreshEpisodes() {
    episodes.value = await getAiDramaEpisodes(projectId);
    syncDraftsFromRow();
  }

  /** 分集目录改动后重拉剧本（目录真相源 episode_content 在剧本里） */
  async function reloadScript() {
    script.value = await getAiDramaScript(projectId);
  }

  function openRenameEpisode(ep: DramaEpisodeBody) {
    renameTarget.value = ep.episodeNumber;
    renameValue.value = ep.title || '';
    renameVisible.value = true;
  }

  async function submitRenameEpisode() {
    const target = renameTarget.value;
    const name = renameValue.value.trim();
    if (target == null || !name) {
      renameVisible.value = false;
      return;
    }
    renameSaving.value = true;
    try {
      await renameAiDramaEpisode(projectId, target, name);
      renameVisible.value = false;
      await reloadScript();
      Message.success(t('common.saveSuccess'));
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.episodeRenameFailed');
    } finally {
      renameSaving.value = false;
    }
  }

  function handleDeleteEpisode(ep: DramaEpisodeBody) {
    openModal({
      type: 'error',
      title: t('drama.deleteEpisodeTitle'),
      content: t('drama.deleteEpisodeConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiDramaEpisode(projectId, ep.episodeNumber);
          activeEpisodeNumber.value = 1;
          await reloadScript();
          await refreshEpisodes();
          Message.success(t('common.deleteSuccess'));
        } catch (err) {
          error.value = (err as Error)?.message || t('drama.episodeDeleteFailed');
        }
      },
    });
  }

  // ==================== 分镜规划 ====================

  async function waitPlanDone(rowId: string): Promise<DramaEpisode | null> {
    const started = Date.now();
    return new Promise((resolve) => {
      const timer = window.setInterval(async () => {
        if (Date.now() - started >= 3 * 60 * 1000) {
          window.clearInterval(timer);
          resolve(null);
          return;
        }
        try {
          const list = await getAiDramaEpisodes(projectId);
          episodes.value = list;
          const row = list.find((e) => e.id === rowId) || null;
          if (row && (row.status === 'completed' || row.status === 'failed')) {
            window.clearInterval(timer);
            resolve(row);
          }
        } catch {
          // 瞬时失败不中断轮询
        }
      }, 2000);
    });
  }

  async function handlePlan() {
    const row = activeRow.value;
    if (!row || planBusy.value) return;
    planBusy.value = true;
    planError.value = '';
    try {
      await planAiDramaFragments(row.id);
      const done = await waitPlanDone(row.id);
      if (!done) {
        Message.error(t('drama.genTimeout'));
        return;
      }
      if (done.status === 'failed') {
        planError.value = t('drama.planFailed');
        return;
      }
      await refreshEpisodes();
      Message.success(t('drama.planDone'));
    } catch (err) {
      planError.value = (err as Error)?.message || t('drama.planFailed');
    } finally {
      planBusy.value = false;
    }
  }

  // ==================== 保存 ====================

  /** 保存当前分镜草稿（含未保存的新增镜）；silent 为 true 时静默，不弹成功/失败提示 */
  async function persistDrafts(silent = false): Promise<boolean> {
    const row = activeRow.value;
    if (!row || saveBusy.value) return false;
    saveBusy.value = true;
    try {
      const fragments = drafts.value.map((f, index) => ({
        id: f.id || null,
        sortOrder: index,
        content: f.content,
        narration: f.narration,
        voice: f.voice,
        durationSec: f.durationSec,
        assetIdsJson: JSON.stringify(f.assetIds || []),
      }));
      const updated = await saveAiDramaFragments(row.id, { fragments });
      episodes.value = episodes.value.map((e) => (e.id === updated.id ? updated : e));
      syncDraftsFromRow();
      if (!silent) Message.success(t('common.saveSuccess'));
      return true;
    } catch (err) {
      if (!silent) Message.error((err as Error)?.message || t('drama.saveFailed'));
      return false;
    } finally {
      saveBusy.value = false;
    }
  }

  async function handleSave() {
    await persistDrafts(false);
  }

  // ==================== 分镜生成 ====================

  async function waitGenerateDone(rowId: string): Promise<void> {
    const started = Date.now();
    return new Promise((resolve) => {
      const timer = window.setInterval(async () => {
        if (Date.now() - started >= 10 * 60 * 1000) {
          window.clearInterval(timer);
          resolve();
          return;
        }
        try {
          const status = await getAiDramaGenerateStatus(rowId);
          // 逐镜回填：每完成一镜立刻把图片挂到对应草稿，一张一张出图，而不是等整集好了再一次性刷新
          const byId = new Map((status.fragments || []).map((f) => [f.fragmentId, f]));
          drafts.value.forEach((draft) => {
            if (!draft.id) return;
            const fs = byId.get(draft.id);
            if (!fs) return;
            if (fs.coverAttachmentId) draft.coverAttachmentId = fs.coverAttachmentId;
            if (fs.videoAttachmentId) draft.videoAttachmentId = fs.videoAttachmentId;
            if (fs.status === 'completed' || fs.status === 'failed') {
              genBusyIds.value.delete(draft.id);
            }
          });
          if (status.running <= 0) {
            window.clearInterval(timer);
            resolve();
          }
        } catch {
          // 瞬时失败不中断轮询
        }
      }, 2000);
    });
  }

  async function handleGenerate(fragmentIds?: string[]) {
    const row = activeRow.value;
    if (!row || genBusy.value) return;
    genBusy.value = true;
    if (fragmentIds) {
      fragmentIds.forEach((id) => genBusyIds.value.add(id));
    } else {
      drafts.value.forEach((f) => {
        if (f.id) genBusyIds.value.add(f.id);
      });
    }
    try {
      // 生成前先落盘本地改动，避免修改的「画面描述」提示词被旧数据覆盖
      const saved = await persistDrafts(true);
      if (!saved) return;
      await generateAiDramaFragments(row.id, { fragmentIds });
      await waitGenerateDone(row.id);
      await refreshEpisodes();
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.assetGenFailed'));
    } finally {
      genBusy.value = false;
      genBusyIds.value = new Set();
    }
  }

  function generateOne(fragment: DramaFragment) {
    if (!fragment.id) return;
    handleGenerate([fragment.id]);
  }

  // ==================== 分镜编辑 ====================

  function addFragment() {
    const draft: FragmentDraft = {
      id: '',
      episodeId: activeRow.value?.id || '',
      sortOrder: drafts.value.length,
      content: '',
      narration: '',
      voice: '',
      durationSec: 8,
      assetIdsJson: '[]',
      assetIds: [],
    };
    drafts.value = [...drafts.value, draft];
  }

  function removeFragment(index: number) {
    drafts.value = drafts.value.filter((_, i) => i !== index);
  }

  function moveFragment(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= drafts.value.length) return;
    const next = [...drafts.value];
    const [item] = next.splice(index, 1);
    next.splice(target, 0, item);
    drafts.value = next;
  }

  // ==================== 口播配音 / 合成成片 / 下载 ====================

  async function waitEpisodeStatus(rowId: string, done: (e: DramaEpisode) => boolean): Promise<DramaEpisode | null> {
    const started = Date.now();
    return new Promise((resolve) => {
      const timer = window.setInterval(async () => {
        if (Date.now() - started >= 10 * 60 * 1000) {
          window.clearInterval(timer);
          resolve(null);
          return;
        }
        try {
          const list = await getAiDramaEpisodes(projectId);
          episodes.value = list;
          const row = list.find((e) => e.id === rowId) || null;
          if (row && done(row)) {
            window.clearInterval(timer);
            resolve(row);
          }
        } catch {
          // 瞬时失败不中断轮询
        }
      }, 2000);
    });
  }

  async function handleNarration() {
    const row = activeRow.value;
    if (!row || narrationBusy.value) return;
    narrationBusy.value = true;
    try {
      await generateAiDramaNarration(row.id);
      const done = await waitEpisodeStatus(
        row.id,
        (e) => e.narrationStatus === 'completed' || e.narrationStatus === 'failed'
      );
      await refreshEpisodes();
      if (!done) {
        Message.error(t('drama.genTimeout'));
      } else if (done.narrationStatus === 'failed') {
        Message.error(t('drama.narrationFailed'));
      } else {
        Message.success(t('drama.narrationDone'));
      }
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.narrationFailed'));
    } finally {
      narrationBusy.value = false;
    }
  }

  /** 逐镜出视频进度：每出一镜立刻把视频挂到对应草稿，一个一个显示，而不是等整集好了才一次性刷新 */
  async function waitVideoDone(rowId: string): Promise<DramaEpisode | null> {
    const started = Date.now();
    return new Promise((resolve) => {
      const timer = window.setInterval(async () => {
        if (Date.now() - started >= 10 * 60 * 1000) {
          window.clearInterval(timer);
          resolve(null);
          return;
        }
        try {
          const status = await getAiDramaGenerateStatus(rowId);
          const byId = new Map((status.fragments || []).map((f) => [f.fragmentId, f]));
          drafts.value.forEach((draft) => {
            if (!draft.id) return;
            const fs = byId.get(draft.id);
            if (fs?.videoAttachmentId) draft.videoAttachmentId = fs.videoAttachmentId;
          });
          const list = await getAiDramaEpisodes(projectId);
          episodes.value = list;
          const row = list.find((e) => e.id === rowId) || null;
          if (row && (row.videoStatus === 'completed' || row.videoStatus === 'failed')) {
            window.clearInterval(timer);
            resolve(row);
          }
        } catch {
          // 瞬时失败不中断轮询
        }
      }, 2000);
    });
  }

  async function handleGenerateVideo() {
    const row = activeRow.value;
    if (!row || videoBusy.value) return;
    videoBusy.value = true;
    try {
      await generateAiDramaFragmentVideos(row.id);
      const done = await waitVideoDone(row.id);
      await refreshEpisodes();
      if (!done) {
        Message.error(t('drama.genTimeout'));
      } else if (done.videoStatus === 'failed') {
        Message.error(done.videoError || t('drama.videoFailed'));
      } else {
        Message.success(t('drama.videoDone'));
      }
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.videoFailed'));
    } finally {
      videoBusy.value = false;
    }
  }

  async function handleCompose() {
    const row = activeRow.value;
    if (!row || composeBusy.value) return;
    composeBusy.value = true;
    try {
      await composeAiDramaEpisode(row.id);
      const done = await waitEpisodeStatus(
        row.id,
        (e) => e.composeStatus === 'completed' || e.composeStatus === 'failed'
      );
      await refreshEpisodes();
      if (!done) {
        Message.error(t('drama.genTimeout'));
      } else if (done.composeStatus === 'failed') {
        Message.error(done.composeError || t('drama.composeFailed'));
      } else {
        Message.success(t('drama.composeDone'));
      }
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.composeFailed'));
    } finally {
      composeBusy.value = false;
    }
  }

  async function handleDownload() {
    const attachmentId = activeRow.value?.videoAttachmentId;
    if (!attachmentId) return;
    try {
      const res = await downloadAttachment(attachmentId);
      const url = URL.createObjectURL(new Blob([res], { type: 'video/mp4' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `${project.value?.title || 'drama'}-第${activeEpisodeNumber.value}集.mp4`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      Message.error((err as Error)?.message || t('common.operationFailed'));
    }
  }

  function base64ToBlob(base64: string, type: string): Blob {
    const bin = atob(base64);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i += 1) bytes[i] = bin.charCodeAt(i);
    return new Blob([bytes], { type });
  }

  async function handlePreviewVoice(fragment: DramaFragment) {
    if (!fragment.id || previewBusyId.value) return;
    previewBusyId.value = fragment.id;
    try {
      const res = await previewAiDramaFragmentVoice(
        fragment.id,
        fragment.voice || undefined,
        fragment.narration || undefined
      );
      const url = URL.createObjectURL(base64ToBlob(res.audio, 'audio/wav'));
      if (previewAudioUrl.value) URL.revokeObjectURL(previewAudioUrl.value);
      previewAudioUrl.value = url;
      new Audio(url).play().catch(() => {
        Message.error(t('drama.voicePreviewFailed'));
      });
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.voicePreviewFailed'));
    } finally {
      previewBusyId.value = '';
    }
  }

  /** 播放/停止选中镜已生成的配音（右栏播放按钮） */
  function toggleNarrationAudio(fragment: FragmentDraft) {
    const attachmentId = fragment.narrationAttachmentId;
    if (!fragment.id || !attachmentId) return;
    // 正在播这一镜：再点一次停止
    if (narrationAudio && playingNarrationId.value === fragment.id && !narrationAudio.paused) {
      narrationAudio.pause();
      playingNarrationId.value = '';
      return;
    }
    // 停掉上一个，再播当前镜
    if (narrationAudio) {
      narrationAudio.pause();
      narrationAudio = null;
    }
    const audio = new Audio(coverUrl(attachmentId));
    narrationAudio = audio;
    playingNarrationId.value = fragment.id;
    audio.play().catch(() => {
      Message.error(t('drama.voicePreviewFailed'));
      playingNarrationId.value = '';
    });
    audio.addEventListener('ended', () => {
      if (playingNarrationId.value === fragment.id) playingNarrationId.value = '';
    });
  }

  onMounted(async () => {
    await load();
  });
</script>

<style scoped>
  .drama-episode-page {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }
  .drama-ep-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
    border-bottom: 1px solid rgb(0 0 0 / 6%);
    gap: 16px;
  }
  .drama-ep-top-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }
  .drama-ep-title h1 {
    margin: 0;
    font-size: 18px;
    font-weight: 650;
  }
  .drama-ep-title p {
    margin: 2px 0 0;
    font-size: 13px;
    color: #8a94a6;
  }
  .drama-ep-top-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
  .drama-ep-banner {
    margin: 12px 20px 0;
  }
  .drama-ep-steps {
    padding: 10px 20px 12px;
    border-bottom: 1px solid rgb(0 0 0 / 5%);
    background: #fafafa;
  }
  .drama-ep-main {
    display: grid;
    grid-template-columns: 220px 1fr 300px;
    gap: 16px;
    padding: 16px 20px;
    flex: 1;
    min-height: 0;
  }
  .drama-ep-dir,
  .drama-ep-editor,
  .drama-ep-preview {
    overflow: auto;
    min-height: 0;
  }
  .drama-ep-dir-head h3 {
    margin: 0 0 10px;
    font-size: 14px;
  }
  .drama-ep-dir-list {
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .drama-ep-dir-card {
    display: flex;
    align-items: center;
    padding: 8px 10px;
    width: 100%;
    border: 1px solid transparent;
    border-radius: 8px;
    text-align: left;
    background: transparent;
    gap: 10px;
    cursor: pointer;
  }
  .drama-ep-dir-card.is-active {
    border-color: #b6ff00;
    background: rgb(182 255 0 / 6%);
  }
  .drama-ep-dir-no {
    width: 26px;
    height: 26px;
    font-weight: 650;
    border-radius: 6px;
    text-align: center;
    background: rgb(0 0 0 / 5%);
    flex-shrink: 0;
    line-height: 26px;
  }
  .drama-ep-dir-meta {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .drama-ep-dir-meta strong {
    overflow: hidden;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .drama-ep-dir-meta small {
    font-size: 12px;
    color: #8a94a6;
  }
  .drama-ep-dir-item {
    position: relative;
  }
  .drama-ep-dir-ops {
    position: absolute;
    top: 6px;
    right: 6px;
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.15s ease;
  }
  .drama-ep-dir-item:hover .drama-ep-dir-ops,
  .drama-ep-dir-item:focus-within .drama-ep-dir-ops {
    opacity: 1;
  }
  .drama-ep-dir-op {
    display: grid;
    padding: 0;
    width: 22px;
    height: 22px;
    font-size: 11px;
    border: 1px solid rgb(0 0 0 / 12%);
    border-radius: 6px;
    color: #8a94a6;
    background: #ffffff;
    cursor: pointer;
    place-items: center;
  }
  .drama-ep-dir-op:hover {
    background: #f5f6f8;
  }
  .drama-ep-dir-op.danger:hover {
    border-color: #fca5a5;
    color: #dc2626;
  }
  .drama-ep-status,
  .drama-ep-empty {
    padding: 32px 16px;
    text-align: center;
    color: #8a94a6;
  }
  .drama-ep-empty p {
    margin: 0 0 12px;
  }
  .drama-ep-fragments {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .drama-ep-frag {
    padding: 14px;
    border: 1px solid rgb(0 0 0 / 8%);
    border-radius: 10px;
    background: #ffffff;
  }
  .drama-ep-frag.is-active {
    border-color: #b6ff00;
  }
  .drama-ep-frag-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;
  }
  .drama-ep-frag-no {
    font-weight: 650;
    color: color-mix(in srgb, #b6ff00 70%, #111318);
  }
  .drama-ep-frag-dur {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #8a94a6;
  }
  .drama-ep-frag-dur input {
    padding: 4px 6px;
    width: 52px;
    border: 1px solid rgb(0 0 0 / 12%);
    border-radius: 6px;
  }
  .drama-ep-frag-actions {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-left: auto;
  }
  .drama-ep-frag-actions button {
    padding: 4px 8px;
    font-size: 12px;
    border: 1px solid rgb(0 0 0 / 12%);
    border-radius: 6px;
    background: #ffffff;
    cursor: pointer;
  }
  .drama-ep-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 10px;
  }
  .drama-ep-field > span {
    font-size: 12px;
    color: #8a94a6;
  }
  .drama-ep-field textarea {
    padding: 8px;
    width: 100%;
    font-size: 13px;
    border: 1px solid rgb(0 0 0 / 12%);
    border-radius: 8px;
    resize: vertical;
  }
  .drama-ep-frag-cover {
    display: block;
    margin-top: 4px;
    padding: 0;
    width: 100%;
    max-width: 320px;
    border: none;
    background: none;
    cursor: pointer;
  }
  .drama-ep-frag-cover img {
    display: block;
    width: 100%;
    border-radius: 8px;
  }
  .drama-ep-frag-noimage {
    font-size: 12px;
    color: #8a94a6;
  }
  .drama-ep-frag-video {
    display: inline-block;
    margin-top: 6px;
    padding: 2px 8px;
    font-size: 12px;
    border: 1px solid rgb(182 255 0 / 40%);
    border-radius: 6px;
    color: color-mix(in srgb, #b6ff00 70%, #111318);
    background: rgb(182 255 0 / 6%);
  }
  .drama-ep-frag-voice {
    display: inline-block;
    margin-top: 6px;
    margin-left: 6px;
    padding: 2px 8px;
    font-size: 12px;
    border: 1px solid rgb(249 115 22 / 40%);
    border-radius: 6px;
    color: #f97316;
    background: rgb(249 115 22 / 6%);
  }
  .drama-ep-add {
    padding: 10px;
    border: 1px dashed rgb(0 0 0 / 20%);
    border-radius: 8px;
    color: color-mix(in srgb, #b6ff00 70%, #111318);
    background: transparent;
    cursor: pointer;
  }
  .drama-ep-preview h3 {
    margin: 0 0 12px;
    font-size: 14px;
  }
  .drama-ep-preview-media img,
  .drama-ep-preview-media video {
    display: block;
    width: 100%;
    border-radius: 8px;
  }
  .drama-ep-preview-media video {
    background: #000000;
  }
  .drama-ep-preview-hint {
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.5;
  }
  .drama-ep-preview-voice {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid rgb(0 0 0 / 6%);
  }
  .drama-ep-preview-voice-label {
    font-size: 13px;
    font-weight: 600;
  }
  .drama-ep-voice-badge {
    padding: 1px 7px;
    font-size: 11px;
    border: 1px solid rgb(249 115 22 / 40%);
    border-radius: 6px;
    color: #f97316;
    background: rgb(249 115 22 / 6%);
  }
  .drama-ep-preview-play {
    display: inline-grid;
    padding: 0;
    width: 22px;
    height: 22px;
    border: none;
    border-radius: 999px;
    color: #111318;
    background: #b6ff00;
    cursor: pointer;
    place-items: center;
  }
  .drama-ep-preview-play:disabled {
    color: #9aa1ab;
    background: #e5e7eb;
    cursor: not-allowed;
  }
  .drama-ep-strip {
    display: flex;
    overflow-x: auto;
    padding: 10px 20px;
    border-top: 1px solid rgb(0 0 0 / 6%);
    gap: 8px;
  }
  .drama-ep-strip-item {
    overflow: hidden;
    padding: 0;
    width: 72px;
    height: 108px;
    border: 2px solid transparent;
    border-radius: 6px;
    background: #f5f6f8;
    flex-shrink: 0;
    cursor: pointer;
  }
  .drama-ep-strip-item.is-active {
    border-color: #b6ff00;
  }
  .drama-ep-strip-item img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .drama-ep-strip-no {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    font-weight: 650;
    color: #8a94a6;
  }
</style>
