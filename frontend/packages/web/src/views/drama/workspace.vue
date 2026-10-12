<template>
  <div class="drama-workspace">
    <!-- 顶栏：返回 + 可编辑标题 + 资产库入口 -->
    <header class="drama-workspace-top">
      <div class="drama-workspace-top-left">
        <button type="button" class="drama-icon-btn" :aria-label="t('drama.back')" @click="goBack">←</button>
        <input
          v-if="titleEditing"
          v-model="titleDraftTop"
          class="drama-title-input"
          autofocus
          @blur="saveTitle"
          @keyup.enter="saveTitle"
          @keyup.escape="cancelTitleEdit"
        />
        <button v-else type="button" class="drama-title-display" @click="startTitleEdit">
          {{ project?.title || '' }}
        </button>
      </div>
      <div class="drama-workspace-top-right">
        <button
          type="button"
          class="drama-assets-entry-btn"
          :class="{ 'is-active': view === 'assets' }"
          @click="view = view === 'assets' ? 'outline' : 'assets'"
        >
          📦 {{ t('drama.assets') }}
        </button>
      </div>
    </header>

    <div class="drama-workspace-steps">
      <DramaStepBar :current="view === 'assets' ? 3 : 2" />
    </div>

    <div v-if="error" class="pf-error drama-error drama-workspace-banner">{{ error }}</div>

    <main class="drama-workspace-main">
      <div v-if="loading" class="drama-workspace-status"
        ><ShimmerText size="15px">{{ t('drama.loading') }}</ShimmerText></div
      >
      <div v-else-if="!project" class="drama-workspace-status">{{ t('drama.projectNotFound') }}</div>

      <!-- 资产库视图 -->
      <AssetsStep v-else-if="view === 'assets'" :project-id="projectId" :summary-ready="summaryReady" />

      <!-- 剧情大纲视图 -->
      <div v-else class="drama-outline-page">
        <div class="drama-outline-toolbar">
          <div class="drama-outline-toolbar-left">
            <DramaImageStyleModal
              :value="imageStyleId"
              variant="field"
              :field-label="t('drama.styleProjectLabel')"
              :title="t('drama.stylePickTitle')"
              @update:value="handleStyleChange"
            />
            <span class="drama-outline-toolbar-meta">
              {{ episodeCount ? t('drama.episodeTotal', { count: episodeCount }) : t('drama.episodeUndecided') }}
              <template v-if="episodeGenerating || episodeStatus === 'running'">
                <template v-if="progress.total > 0">
                  ·
                  <ShimmerText size="13px">{{
                    t('drama.generatingProgress', { done: progress.done, total: progress.total })
                  }}</ShimmerText>
                </template>
                <template v-else>
                  · <ShimmerText size="13px">{{ t('drama.episodeGenerating') }}</ShimmerText>
                </template>
              </template>
            </span>
          </div>
          <div class="drama-outline-toolbar-actions">
            <button
              type="button"
              class="drama-outline-chip-btn"
              :class="{ 'is-on': metaModal === 'source' }"
              @click="openSourceModal"
            >
              {{ t('drama.wholeCreative') }}
            </button>
            <button
              type="button"
              class="drama-outline-chip-btn"
              :class="{ 'is-on': metaModal === 'summary' }"
              @click="openSummaryModal"
            >
              {{ t('drama.globalSettings') }}
            </button>
            <button
              v-if="episodeError || episodeStatus === 'failed'"
              type="button"
              class="drama-outline-chip-btn"
              @click="handleRegenerateEpisodes"
            >
              {{ t('drama.regenAllEpisodes') }}
            </button>
          </div>
        </div>

        <!-- 生成中：流光扫字状态条 -->
        <div v-if="pipelineRunning" class="drama-outline-generating">
          <ShimmerText size="15px">{{ pipelineStageLabel }}</ShimmerText>
        </div>

        <div class="drama-outline-v2 drama-outline">
          <!-- 左栏：分集目录 -->
          <aside class="drama-outline-sidebar">
            <div class="drama-outline-sidebar-head">
              <div>
                <h3>{{ t('drama.episodeDirectory') }}</h3>
                <p>{{ t('drama.episodeDirectoryHint') }}</p>
              </div>
              <button type="button" class="drama-outline-add-btn" :disabled="!canAdd" @click="handleAddEpisode">
                + {{ t('drama.addEpisode') }}
              </button>
            </div>
            <ul v-if="displayEpisodes.length" class="drama-outline-ep-list">
              <li v-for="ep in displayEpisodes" :key="ep.episodeNumber" class="drama-outline-ep-item">
                <button
                  type="button"
                  class="drama-outline-ep-card"
                  :class="{ 'is-active': ep.episodeNumber === (selected?.episodeNumber ?? activeEpisodeNumber) }"
                  @click="activeEpisodeNumber = ep.episodeNumber"
                >
                  <span class="drama-outline-ep-thumb">{{ ep.episodeNumber }}</span>
                  <span class="drama-outline-ep-meta">
                    <strong>{{ ep.title || t('drama.episodeNo', { n: ep.episodeNumber }) }}</strong>
                    <small v-if="ep.creative">{{ ep.creative }}</small>
                    <em v-if="isSubstantialEpisodeBody(ep.body)">{{ t('drama.episodeDone') }}</em>
                    <em v-else-if="ep.origin === 'manual'">{{ t('drama.episodeManual') }}</em>
                    <em v-else>{{ t('drama.episodePending') }}</em>
                  </span>
                  <span class="drama-outline-ep-more">›</span>
                </button>
                <span class="drama-outline-ep-ops">
                  <button
                    type="button"
                    class="drama-outline-ep-op"
                    :title="t('drama.rename')"
                    @click.stop="openRenameEpisode(ep)"
                    >✎</button
                  >
                  <button
                    type="button"
                    class="drama-outline-ep-op danger"
                    :title="t('drama.delete')"
                    @click.stop="handleDeleteEpisode(ep)"
                    >🗑</button
                  >
                </span>
              </li>
            </ul>
            <p v-else class="pf-muted" style="padding: 0 6px">{{ t('drama.noEpisodes') }}</p>
          </aside>

          <!-- 右栏：本集创意/摘要/剧本 -->
          <section class="drama-outline-main">
            <div class="drama-outline-region-label">{{ t('drama.region.storyDesc') }}</div>
            <div v-if="!selected" class="drama-outline-detail-empty">
              {{ t('drama.noSelectedEpisode') }}
            </div>
            <article v-else class="drama-outline-detail">
              <header class="drama-outline-detail-head">
                <div class="drama-outline-detail-title">
                  <h2>{{ selected.title || t('drama.episodeNo', { n: selected.episodeNumber }) }}</h2>
                  <span v-if="imageStyleLabel" class="drama-outline-style-badge">{{ imageStyleLabel }}</span>
                  <div class="drama-outline-detail-meta">
                    <span class="drama-outline-tag">{{ t('drama.episodeNo', { n: selected.episodeNumber }) }}</span>
                    <span v-if="isSubstantialEpisodeBody(selected.body)" class="drama-outline-tag">{{
                      t('drama.episodeDone')
                    }}</span>
                  </div>
                </div>
                <button
                  type="button"
                  class="drama-btn-primary"
                  :disabled="!isSubstantialEpisodeBody(selected.body)"
                  @click="openEpisodeEditor"
                >
                  🎬 {{ t('drama.openEpisodeEditor') }}
                </button>
              </header>

              <div v-if="localError" class="pf-error drama-error" style="margin-bottom: 12px">{{ localError }}</div>

              <div class="drama-outline-sections">
                <section
                  v-for="meta in sectionMetas"
                  :key="meta.key"
                  class="drama-outline-section"
                  :class="{ 'is-open': openSections.has(meta.key) }"
                >
                  <header class="drama-outline-section-head">
                    <div class="drama-outline-section-title">
                      <span class="drama-outline-section-icon" :class="`is-${meta.key}`" aria-hidden>{{
                        meta.icon
                      }}</span>
                      <div>
                        <strong>{{ meta.title }}</strong>
                        <p>{{ meta.hint }}</p>
                      </div>
                    </div>
                    <div class="drama-outline-section-actions">
                      <button
                        type="button"
                        class="drama-outline-text-btn"
                        :disabled="busy"
                        @click="startEdit(meta.key)"
                      >
                        ✏️ {{ t('drama.edit') }}
                      </button>
                      <button type="button" class="drama-outline-text-btn" @click="copySection(meta.key)">
                        📋 {{ t('drama.copy') }}
                      </button>
                      <button type="button" class="drama-outline-text-btn" @click="toggleSection(meta.key)">
                        {{ openSections.has(meta.key) ? `▲ ${t('drama.collapse')}` : `▼ ${t('drama.expand')}` }}
                      </button>
                    </div>
                  </header>
                  <div v-if="openSections.has(meta.key)" class="drama-outline-section-body">
                    <template v-if="editingSection === meta.key">
                      <textarea
                        v-model="sectionDraft"
                        class="drama-ep-section-textarea"
                        :rows="meta.key === 'body' ? 14 : 8"
                        :placeholder="meta.placeholder"
                        :disabled="saving"
                      />
                      <div class="drama-outline-edit-actions" style="margin-top: 10px">
                        <button type="button" class="drama-btn-ghost" :disabled="saving" @click="cancelEdit"
                          >取消</button
                        >
                        <button
                          type="button"
                          class="drama-btn-primary"
                          :disabled="saving"
                          @click="saveSection(meta.key)"
                        >
                          {{ t('common.save') }}
                        </button>
                      </div>
                    </template>
                    <p v-else class="drama-pre drama-outline-section-text">{{
                      sectionText(meta.key) || meta.placeholder
                    }}</p>
                  </div>
                </section>
              </div>
            </article>
          </section>
        </div>
      </div>
    </main>

    <!-- 整剧创意弹窗 -->
    <n-modal v-model:show="sourceVisible" preset="card" :title="t('drama.wholeCreative')" class="!w-[620px]">
      <div class="drama-outline-meta-modal-body">
        <textarea
          v-if="sourceEditing"
          v-model="sourceDraft"
          class="drama-ep-section-textarea"
          rows="14"
          :disabled="sourceSaving"
        />
        <p v-else class="drama-pre">{{ script?.sourceText?.trim() || t('drama.noCreative') }}</p>
      </div>
      <template #footer>
        <template v-if="sourceEditing">
          <div class="flex justify-end gap-[8px]">
            <n-button :disabled="sourceSaving" @click="sourceEditing = false">{{ t('drama.cancelEdit') }}</n-button>
            <n-button type="primary" :loading="sourceSaving" @click="saveSourceEdit">{{ t('common.save') }}</n-button>
          </div>
        </template>
        <template v-else>
          <div class="flex justify-end gap-[8px]">
            <n-button @click="sourceEditing = true">{{ t('drama.edit') }}</n-button>
          </div>
        </template>
      </template>
    </n-modal>

    <!-- 全局设定弹窗 -->
    <n-modal v-model:show="summaryVisible" preset="card" :title="t('drama.globalSettings')" class="!w-[620px]">
      <div class="drama-outline-meta-modal-body">
        <p v-if="summaryGenerating || summaryStatus === 'running'" class="drama-loader">
          <ShimmerText size="14px">{{ t('drama.summaryGenerating') }}</ShimmerText>
        </p>
        <p v-if="summaryError || summaryStatus === 'failed'" class="pf-error drama-error">{{
          summaryError || t('drama.summaryFailed')
        }}</p>

        <div v-if="summaryEditing && summaryDraft" class="drama-outline-edit">
          <label class="drama-outline-edit-field">
            <span>{{ t('drama.field.seriesTitle') }}</span>
            <input
              type="text"
              :value="sf('seriesTitle')"
              @input="setSummaryField('seriesTitle', ($event.target as HTMLInputElement).value)"
            />
          </label>
          <label class="drama-outline-edit-field">
            <span>{{ t('drama.field.episodeCount') }}</span>
            <input
              type="text"
              :value="sf('episodeCount')"
              @input="setSummaryField('episodeCount', ($event.target as HTMLInputElement).value)"
            />
          </label>
          <label class="drama-outline-edit-field">
            <span>{{ t('drama.field.storyType') }}</span>
            <input
              type="text"
              :value="sf('storyType')"
              @input="setSummaryField('storyType', ($event.target as HTMLInputElement).value)"
            />
          </label>
          <label class="drama-outline-edit-field">
            <span>{{ t('drama.field.oneLineStory') }}</span>
            <textarea
              rows="3"
              :value="sf('oneLineStory')"
              @input="setSummaryField('oneLineStory', ($event.target as HTMLTextAreaElement).value)"
            />
          </label>
          <label class="drama-outline-edit-field">
            <span>{{ t('drama.field.synopsis') }}</span>
            <textarea
              rows="6"
              :value="sf('synopsis')"
              @input="setSummaryField('synopsis', ($event.target as HTMLTextAreaElement).value)"
            />
          </label>
        </div>

        <div v-else-if="summary && summaryStatus === 'completed'" class="drama-summary-structured">
          <section v-if="summary.seriesTitle" class="drama-summary-field">
            <h4>{{ t('drama.field.seriesTitle') }}</h4>
            <p class="drama-pre">{{ summary.seriesTitle }}</p>
          </section>
          <section v-if="summary.storyType" class="drama-summary-field">
            <h4>{{ t('drama.field.storyType') }}</h4>
            <p class="drama-pre">{{ summary.storyType }}</p>
          </section>
          <section v-if="summary.oneLineStory" class="drama-summary-field">
            <h4>{{ t('drama.field.oneLineStory') }}</h4>
            <p class="drama-pre">{{ summary.oneLineStory }}</p>
          </section>
          <section v-if="summary.synopsis" class="drama-summary-field">
            <h4>{{ t('drama.field.synopsis') }}</h4>
            <p class="drama-pre">{{ summary.synopsis }}</p>
          </section>
          <section v-if="characters.length" class="drama-summary-field">
            <h4>{{ t('drama.field.characters') }}</h4>
            <p class="drama-pre">{{ characterNames }}</p>
          </section>
        </div>

        <p v-else-if="!summary && summaryStatus !== 'running' && !summaryError" class="pf-muted">{{
          t('drama.noSummary')
        }}</p>
      </div>
      <template #footer>
        <template v-if="summaryEditing && summaryDraft">
          <div class="flex justify-end gap-[8px]">
            <n-button :disabled="summarySaving" @click="cancelSummaryEdit">{{ t('drama.cancelEdit') }}</n-button>
            <n-button type="primary" :loading="summarySaving" @click="saveSummaryEdit">{{ t('common.save') }}</n-button>
          </div>
        </template>
        <template v-else-if="summary && summaryStatus === 'completed'">
          <div class="flex justify-end gap-[8px]">
            <n-button @click="startSummaryEdit">{{ t('drama.edit') }}</n-button>
          </div>
        </template>
        <template v-else>
          <div class="flex justify-end gap-[8px]">
            <n-button type="primary" :loading="summaryGenerating" @click="regenerateSummary">{{
              t('drama.regenerate')
            }}</n-button>
          </div>
        </template>
      </template>
    </n-modal>

    <!-- 重命名分集弹窗 -->
    <n-modal v-model:show="renameVisible" preset="card" :title="t('drama.renameEpisodeTitle')" class="!w-[420px]">
      <div class="drama-outline-meta-modal-body">
        <n-input
          v-model:value="renameValue"
          :placeholder="t('drama.renameEpisodeMessage')"
          @keyup.enter="submitRenameEpisode"
        />
      </div>
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
  import { NButton, NInput, NModal, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DramaEpisodeBody, DramaProject, DramaScript } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AssetsStep from './assetsStep.vue';
  import DramaImageStyleModal from './DramaImageStyleModal.vue';
  import DramaStepBar from './DramaStepBar.vue';

  import {
    addAiDramaEpisode,
    deleteAiDramaEpisode,
    generateAiDramaEpisodeScript,
    generateAiDramaScriptSummary,
    getAiDramaProject,
    getAiDramaScript,
    renameAiDramaEpisode,
    updateAiDramaProject,
    updateAiDramaScript,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import { DramaRouteEnum } from '@/enums/routeEnum';

  import './printfilm.css';
  import {
    autoMissingEpisodeCount,
    buildEpisodeContentUpdate,
    buildOutlineDirectory,
    getEpisodeContentStatus,
    getImageStyleId,
    getSummaryStatus,
    hasSubstantialEpisode,
    isSubstantialEpisodeBody,
    mergeDirectoryEpisodeBodies,
    parseEpisodeBodies,
    parseScriptParams,
    parseSummary,
    resolveEpisodeTarget,
  } from './dramaWorkspaceUtils';
  import { getImageStyleLabel } from './templates';

  type SectionKey = 'creative' | 'summary' | 'body';
  interface SectionMeta {
    key: SectionKey;
    icon: string;
    title: string;
    hint: string;
    placeholder: string;
  }
  interface SummaryDraft {
    seriesTitle?: string;
    episodeCount?: number | string;
    storyType?: string;
    oneLineStory?: string;
    synopsis?: string;
    [key: string]: unknown;
  }

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const router = useRouter();
  const route = useRoute();
  const projectId = String(route.params.projectId || '');

  const project = ref<DramaProject | null>(null);
  const script = ref<DramaScript | null>(null);
  const loading = ref(true);
  const error = ref('');

  const view = ref<'outline' | 'assets'>('outline');
  const titleEditing = ref(false);
  const titleDraftTop = ref('');

  // 自动流水线状态
  const summaryGenerating = ref(false);
  const episodeGenerating = ref(false);
  const summaryError = ref('');
  const episodeError = ref('');
  const progress = ref({ done: 0, total: 0 });
  let pipelineToken = 0;

  // 目录与编辑状态
  const activeEpisodeNumber = ref(1);
  const openSections = ref<Set<SectionKey>>(new Set(['creative', 'summary', 'body']));
  const editingSection = ref<SectionKey | null>(null);
  const sectionDraft = ref('');
  const saving = ref(false);
  const adding = ref(false);
  const localError = ref('');

  // 分集重命名/删除
  const renameVisible = ref(false);
  const renameValue = ref('');
  const renameTarget = ref<number | null>(null);
  const renameSaving = ref(false);

  // 弹窗状态
  const sourceVisible = ref(false);
  const sourceDraft = ref('');
  const sourceEditing = ref(false);
  const sourceSaving = ref(false);
  const summaryVisible = ref(false);
  const summaryEditing = ref(false);
  const summaryDraft = ref<SummaryDraft | null>(null);
  const summarySaving = ref(false);
  const metaModal = ref<'source' | 'summary' | null>(null);

  const sectionMetas = computed<SectionMeta[]>(() => [
    {
      key: 'creative',
      icon: '💡',
      title: t('drama.section.creative'),
      hint: t('drama.section.creativeHint'),
      placeholder: t('drama.section.creativePlaceholder'),
    },
    {
      key: 'summary',
      icon: '🧭',
      title: t('drama.section.summary'),
      hint: t('drama.section.summaryHint'),
      placeholder: t('drama.section.summaryPlaceholder'),
    },
    {
      key: 'body',
      icon: '📖',
      title: t('drama.section.body'),
      hint: t('drama.section.bodyHint'),
      placeholder: t('drama.section.bodyPlaceholder'),
    },
  ]);

  const summary = computed(() => parseSummary(script.value));
  const summaryStatus = computed(() => getSummaryStatus(script.value));
  const episodeStatus = computed(() => getEpisodeContentStatus(script.value));
  const episodeCount = computed(() => resolveEpisodeTarget(summary.value, script.value, project.value));
  const imageStyleId = computed(() => getImageStyleId(script.value));
  const imageStyleLabel = computed(() => getImageStyleLabel(imageStyleId.value));
  const episodeBodies = computed(() => parseEpisodeBodies(script.value));
  const directoryEpisodes = computed(() => buildOutlineDirectory(episodeBodies.value, episodeCount.value));
  const displayEpisodes = computed(() => mergeDirectoryEpisodeBodies(directoryEpisodes.value, episodeBodies.value));
  const selected = computed(
    () =>
      displayEpisodes.value.find((ep) => ep.episodeNumber === activeEpisodeNumber.value) ||
      displayEpisodes.value[0] ||
      null
  );
  const characters = computed(() => {
    const chars = summary.value?.characters;
    return Array.isArray(chars) ? (chars as Array<Record<string, unknown>>) : [];
  });
  const characterNames = computed(() =>
    characters.value
      .map((c) => String(c.name || ''))
      .filter(Boolean)
      .join('、')
  );

  const summaryReady = computed(() => summaryStatus.value === 'completed' || Boolean(summary.value));
  const canAdd = computed(
    () => summaryReady.value && !episodeGenerating.value && !adding.value && directoryEpisodes.value.length < 120
  );
  const busy = computed(() => episodeGenerating.value || saving.value || adding.value);
  // 自动流水线是否在跑（摘要 或 分集剧本任一阶段）
  const pipelineRunning = computed(
    () =>
      summaryGenerating.value ||
      episodeGenerating.value ||
      summaryStatus.value === 'running' ||
      episodeStatus.value === 'running'
  );
  const pipelineStageLabel = computed(() =>
    summaryGenerating.value || summaryStatus.value === 'running'
      ? t('drama.genSummaryShimmer')
      : t('drama.genEpisodesShimmer')
  );

  function goBack() {
    router.push({ name: DramaRouteEnum.DRAMA_INDEX });
  }

  function openEpisodeEditor() {
    const episodeNumber = selected.value?.episodeNumber;
    if (!episodeNumber) return;
    router.push({
      name: DramaRouteEnum.DRAMA_EPISODE,
      params: { projectId, episodeNumber: String(episodeNumber) },
    });
  }

  async function reloadProject() {
    const p = await getAiDramaProject(projectId);
    project.value = p;
    if (p?.script) script.value = p.script;
    titleDraftTop.value = p?.title || '';
    return p;
  }

  // ==================== 标题编辑 ====================

  function startTitleEdit() {
    titleDraftTop.value = project.value?.title || '';
    titleEditing.value = true;
  }

  function cancelTitleEdit() {
    titleEditing.value = false;
    titleDraftTop.value = project.value?.title || '';
  }

  async function saveTitle() {
    const next = titleDraftTop.value.trim();
    titleEditing.value = false;
    if (!next || !project.value || next === project.value.title) {
      titleDraftTop.value = project.value?.title || '';
      return;
    }
    try {
      const updated = await updateAiDramaProject(projectId, { title: next });
      project.value = updated;
      titleDraftTop.value = updated.title;
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.titleSaveFailed');
    }
  }

  // ==================== 自动流水线 ====================

  async function pollScriptUntil(predicate: (s: DramaScript) => boolean, token?: number): Promise<DramaScript | null> {
    const started = Date.now();
    return new Promise<DramaScript | null>((resolve, reject) => {
      const timer = window.setInterval(async () => {
        if (token !== undefined && token !== pipelineToken) {
          window.clearInterval(timer);
          resolve(null);
          return;
        }
        if (Date.now() - started >= 45 * 60 * 1000) {
          window.clearInterval(timer);
          reject(new Error(t('drama.genTimeout')));
          return;
        }
        try {
          const s = await getAiDramaScript(projectId);
          if (!s) return;
          script.value = s;
          const prog = parseScriptParams(s).episode_content_progress;
          if (prog && (prog.total || prog.done)) {
            progress.value = { done: Number(prog.done) || 0, total: Number(prog.total) || 0 };
          }
          if (predicate(s)) {
            window.clearInterval(timer);
            resolve(s);
          }
        } catch {
          // 瞬时网络失败不中断轮询，等待下一轮重试
        }
      }, 2000);
    });
  }

  async function runSummary(token?: number) {
    summaryGenerating.value = true;
    summaryError.value = '';
    try {
      await generateAiDramaScriptSummary({ projectId });
      const finished = await pollScriptUntil((s) => {
        const st = getSummaryStatus(s);
        return st === 'completed' || st === 'failed';
      }, token);
      if (!finished) return;
      if (getSummaryStatus(finished) === 'failed') {
        summaryError.value = parseScriptParams(finished).summary_error || t('drama.summaryFailed');
        return;
      }
      await reloadProject();
    } catch (err) {
      summaryError.value = (err as Error)?.message || t('drama.summaryFailed');
    } finally {
      summaryGenerating.value = false;
    }
  }

  async function runEpisodeScripts(token?: number, force = false) {
    episodeGenerating.value = true;
    episodeError.value = '';
    try {
      const res = await generateAiDramaEpisodeScript({ projectId, batchSize: 1, force });
      script.value = res.script;
      progress.value = { done: res.totalGenerated, total: res.totalTarget };
      const finished = await pollScriptUntil((s) => {
        const st = getEpisodeContentStatus(s);
        return st === 'completed' || st === 'failed';
      }, token);
      if (!finished) return;
      if (getEpisodeContentStatus(finished) === 'failed') {
        episodeError.value = parseScriptParams(finished).episode_content_error || t('drama.episodeFailed');
        return;
      }
      await reloadProject();
    } catch (err) {
      episodeError.value = (err as Error)?.message || t('drama.episodeFailed');
    } finally {
      episodeGenerating.value = false;
    }
  }

  async function runPipeline() {
    const token = ++pipelineToken;
    let current = script.value;
    const status = getSummaryStatus(current);
    if (!current?.summaryJson && status !== 'completed') {
      if (status === 'running') {
        summaryGenerating.value = true;
        const waited = await pollScriptUntil((s) => {
          const st = getSummaryStatus(s);
          return st === 'completed' || st === 'failed';
        }, token);
        summaryGenerating.value = false;
        if (!waited || token !== pipelineToken) return;
        if (getSummaryStatus(waited) === 'failed') {
          summaryError.value = parseScriptParams(waited).summary_error || t('drama.summaryFailed');
          return;
        }
        current = waited;
      } else {
        await runSummary(token);
        if (token !== pipelineToken) return;
        current = script.value;
      }
      if (!current || token !== pipelineToken) return;
    }

    const target = resolveEpisodeTarget(parseSummary(current), current, project.value);
    const bodies = parseEpisodeBodies(current);
    const epStatus = getEpisodeContentStatus(current);
    const autoMissing = autoMissingEpisodeCount(bodies, target);
    const complete = epStatus !== 'failed' && autoMissing === 0 && hasSubstantialEpisode(bodies);
    if (complete) return;

    if (parseSummary(current) || getSummaryStatus(current) === 'completed') {
      if (epStatus === 'running') {
        episodeGenerating.value = true;
        await pollScriptUntil((s) => {
          const st = getEpisodeContentStatus(s);
          return st === 'completed' || st === 'failed';
        }, token);
        episodeGenerating.value = false;
        if (token !== pipelineToken) return;
        const done = script.value;
        if (done && getEpisodeContentStatus(done) === 'failed') {
          episodeError.value = parseScriptParams(done).episode_content_error || t('drama.episodeFailed');
        }
        await reloadProject();
        return;
      }
      await runEpisodeScripts(token);
    }
  }

  // ==================== 分集目录 ====================

  async function handleAddEpisode() {
    if (!canAdd.value) return;
    adding.value = true;
    localError.value = '';
    try {
      const res = await addAiDramaEpisode({ projectId });
      script.value = res.script;
      await reloadProject();
      activeEpisodeNumber.value = res.episodeNumber;
      editingSection.value = 'creative';
      sectionDraft.value = '';
    } catch (err) {
      localError.value = (err as Error)?.message || t('drama.addEpisodeFailed');
    } finally {
      adding.value = false;
    }
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
      await reloadProject();
      Message.success(t('common.saveSuccess'));
    } catch (err) {
      localError.value = (err as Error)?.message || t('drama.episodeRenameFailed');
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
          await reloadProject();
          Message.success(t('common.deleteSuccess'));
        } catch (err) {
          localError.value = (err as Error)?.message || t('drama.episodeDeleteFailed');
        }
      },
    });
  }

  function handleRegenerateEpisodes() {
    if (episodeGenerating.value) return;
    openModal({
      type: 'warning',
      title: t('drama.regenEpisodesTitle'),
      content: t('drama.regenEpisodesConfirm'),
      positiveText: t('drama.regenerate'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        await runEpisodeScripts(undefined, true);
      },
    });
  }

  // ==================== 分区卡片 ====================

  function toggleSection(key: SectionKey) {
    const next = new Set(openSections.value);
    if (next.has(key)) {
      next.delete(key);
    } else {
      next.add(key);
    }
    openSections.value = next;
  }

  function startEdit(key: SectionKey) {
    if (!selected.value) return;
    editingSection.value = key;
    sectionDraft.value = selected.value[key] || '';
  }

  function cancelEdit() {
    editingSection.value = null;
    sectionDraft.value = '';
  }

  function sectionText(key: SectionKey): string {
    return selected.value?.[key] || '';
  }

  async function saveSection(key: SectionKey) {
    if (!selected.value?.episodeNumber) return;
    saving.value = true;
    localError.value = '';
    try {
      const num = selected.value.episodeNumber;
      const next: DramaEpisodeBody[] = displayEpisodes.value.map((ep) => {
        if (ep.episodeNumber !== num) return ep;
        if (key === 'creative') return { ...ep, creative: sectionDraft.value };
        if (key === 'summary') return { ...ep, summary: sectionDraft.value };
        return { ...ep, body: sectionDraft.value };
      });
      const updated = await updateAiDramaScript(projectId, { episodeContentJson: buildEpisodeContentUpdate(next) });
      script.value = updated;
      editingSection.value = null;
      Message.success(t('common.saveSuccess'));
    } catch (err) {
      localError.value = (err as Error)?.message || t('drama.saveFailed');
    } finally {
      saving.value = false;
    }
  }

  async function copySection(key: SectionKey) {
    const text = sectionText(key);
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('drama.copied'));
    } catch {
      Message.error(t('drama.copyFailed'));
    }
  }

  // ==================== 弹窗：创意 / 全局设定 ====================

  function openSourceModal() {
    sourceDraft.value = script.value?.sourceText || '';
    sourceEditing.value = false;
    metaModal.value = 'source';
    sourceVisible.value = true;
  }

  async function saveSourceEdit() {
    sourceSaving.value = true;
    try {
      const updated = await updateAiDramaScript(projectId, { source: sourceDraft.value });
      script.value = updated;
      sourceEditing.value = false;
      Message.success(t('common.saveSuccess'));
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.sourceSaveFailed');
    } finally {
      sourceSaving.value = false;
    }
  }

  function openSummaryModal() {
    summaryEditing.value = false;
    summaryDraft.value = null;
    metaModal.value = 'summary';
    summaryVisible.value = true;
  }

  function startSummaryEdit() {
    if (!summary.value) return;
    summaryDraft.value = { ...(summary.value as SummaryDraft) };
    summaryEditing.value = true;
  }

  function cancelSummaryEdit() {
    summaryEditing.value = false;
    summaryDraft.value = null;
  }

  function sf(key: 'seriesTitle' | 'storyType' | 'oneLineStory' | 'synopsis' | 'episodeCount'): string {
    const v = summaryDraft.value?.[key];
    return v == null ? '' : String(v);
  }

  function setSummaryField(key: string, value: unknown) {
    if (!summaryDraft.value) return;
    if (key === 'episodeCount') {
      const v = String(value ?? '').trim();
      summaryDraft.value = { ...summaryDraft.value, episodeCount: v === '' ? '' : Number(v) };
      return;
    }
    summaryDraft.value = { ...summaryDraft.value, [key]: value };
  }

  async function saveSummaryEdit() {
    if (!summaryDraft.value) return;
    summarySaving.value = true;
    try {
      const updated = await updateAiDramaScript(projectId, { summaryJson: JSON.stringify(summaryDraft.value) });
      script.value = updated;
      summaryEditing.value = false;
      summaryDraft.value = null;
      Message.success(t('common.saveSuccess'));
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.summarySaveFailed');
    } finally {
      summarySaving.value = false;
    }
  }

  function regenerateSummary() {
    runSummary();
  }

  // ==================== 风格 ====================

  async function handleStyleChange(styleId: string | null) {
    try {
      const updated = await updateAiDramaScript(projectId, { imageStyleId: styleId ?? undefined });
      script.value = updated;
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.styleFailed');
    }
  }

  // 切换集数时重置编辑态
  watch(
    () => selected.value?.episodeNumber,
    () => {
      editingSection.value = null;
      sectionDraft.value = '';
      localError.value = '';
    }
  );

  onMounted(async () => {
    loading.value = true;
    try {
      await reloadProject();
      if (project.value) {
        await runPipeline();
      }
    } catch (err) {
      error.value = (err as Error)?.message || t('drama.loadFailed');
    } finally {
      loading.value = false;
    }
  });
</script>
