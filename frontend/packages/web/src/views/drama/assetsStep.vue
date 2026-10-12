<template>
  <div class="drama-assets-step">
    <!-- 顶部：资产库标题 + 分类统计 -->
    <header class="drama-assets-hero">
      <div class="drama-step-hero-main">
        <div class="drama-step-hero-icon" aria-hidden>📦</div>
        <div>
          <h2>{{ t('drama.assets') }}</h2>
          <p class="drama-step-hero-sub">
            {{ t('drama.assetsHeroSub', { total: assets.length, filtered: filtered.length, pending: pendingCount }) }}
          </p>
        </div>
      </div>
    </header>

    <div class="drama-assets-tips" role="note">
      <span aria-hidden>✨</span>
      <span>{{ t('drama.assetsTips') }}</span>
    </div>

    <!-- 分类 Tab + 操作 -->
    <div class="drama-assets-toolbar">
      <div class="drama-asset-tabs">
        <button
          v-for="tab in ASSET_TABS"
          :key="tab.key"
          type="button"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ t(tab.labelKey) }}
        </button>
      </div>
      <div class="drama-actions">
        <button type="button" class="pf-btn pf-btn-ghost" @click="openCreate">
          {{ t('drama.createAsset') }}
        </button>
        <router-link class="pf-btn pf-btn-ghost" :to="{ name: DramaRouteEnum.DRAMA_ASSETS }">
          {{ t('drama.browseAllAssets') }}
        </router-link>
        <button type="button" class="pf-btn pf-btn-ghost" :disabled="reseedBusy || seeding" @click="handleReseed">
          <ShimmerText v-if="reseedBusy" size="13px">{{ t('drama.seeding') }}</ShimmerText>
          <template v-else>{{ t('drama.reseedAssets') }}</template>
        </button>
      </div>
    </div>

    <p v-if="loading || seeding" class="drama-muted"
      ><ShimmerText size="14px">{{ t('drama.assetsLoading') }}</ShimmerText></p
    >

    <div v-if="!loading && filtered.length" class="drama-asset-grid">
      <article
        v-for="asset in filtered"
        :key="asset.id"
        class="drama-asset-card drama-asset-card-clickable"
        role="button"
        tabindex="0"
        @click="openDetail(asset)"
        @keydown.enter="openDetail(asset)"
        @keydown.space.prevent="openDetail(asset)"
      >
        <button
          v-if="previewUrl(asset)"
          type="button"
          class="drama-asset-thumb-btn"
          :title="t('drama.viewLarge')"
          @click.stop="openLightbox(asset)"
        >
          <img :src="previewUrl(asset)" :alt="asset.name || ''" />
        </button>
        <div v-else class="drama-asset-placeholder">{{ asset.type || 'asset' }}</div>
        <h3>{{ asset.name || t('drama.unnamed') }}</h3>
        <p>{{ kindLabel(asset) }}</p>
        <div class="drama-asset-card-actions" @click.stop @keydown.stop>
          <button
            type="button"
            class="pf-btn pf-btn-sm"
            :disabled="busyIds.has(asset.id)"
            @click="generateAsset(asset)"
          >
            <ShimmerText v-if="busyIds.has(asset.id)" size="13px">{{ t('drama.generatingImage') }}</ShimmerText>
            <template v-else>{{ genLabel(asset) }}</template>
          </button>
          <button
            type="button"
            class="pf-btn pf-btn-sm drama-btn-danger-text"
            :disabled="busyIds.has(asset.id)"
            @click="confirmDelete(asset)"
          >
            {{ t('drama.delete') }}
          </button>
        </div>
      </article>
    </div>

    <p v-if="!loading && !filtered.length" class="drama-muted">{{ t('drama.noAssets') }}</p>

    <!-- 资产详情弹窗 -->
    <n-modal
      v-model:show="detailVisible"
      preset="card"
      class="drama-asset-detail-modal"
      :title="detailAsset?.name || t('drama.assetDetail')"
      style="max-width: 680px"
    >
      <div v-if="detailAsset" class="drama-asset-detail">
        <button
          type="button"
          class="drama-asset-detail-media"
          :disabled="!previewUrl(detailAsset)"
          :title="previewUrl(detailAsset) ? t('drama.viewLarge') : undefined"
          @click="openLightbox(detailAsset)"
        >
          <img v-if="previewUrl(detailAsset)" :src="previewUrl(detailAsset)" :alt="detailAsset.name || ''" />
          <div v-else class="drama-asset-placeholder">{{ detailAsset.type || 'asset' }}</div>
        </button>

        <p class="drama-muted drama-asset-detail-meta">
          {{ kindLabel(detailAsset) }} · {{ assetHasImage(detailAsset) ? t('drama.hasImage') : t('drama.noImage') }}
        </p>

        <div class="drama-asset-detail-extra">
          <input
            ref="uploadInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            style="display: none"
            :disabled="uploading"
            @change="handleUpload"
          />
          <button type="button" class="pf-btn pf-btn-sm" :disabled="uploading" @click="uploadInputRef?.click()">
            {{
              uploading
                ? t('drama.uploading')
                : assetHasImage(detailAsset)
                ? t('drama.changeImage')
                : t('drama.uploadImage')
            }}
          </button>
          <button
            type="button"
            class="pf-btn pf-btn-sm drama-btn-danger-text"
            :disabled="busyIds.has(detailAsset.id)"
            @click="confirmDelete(detailAsset)"
          >
            {{ t('drama.delete') }}
          </button>
        </div>

        <p class="drama-muted drama-asset-ref-hint">{{ t('drama.uploadAsReferenceHint') }}</p>

        <label class="drama-field">
          <span>{{ t('drama.imagePrompt') }}</span>
          <textarea v-model="promptDraft" rows="8" :placeholder="t('drama.imagePromptPlaceholder')" />
        </label>

        <div v-if="assetKind(detailAsset) === 'character'" class="drama-doubao-hint" role="note">
          <div class="drama-doubao-hint-head">
            <span class="drama-doubao-hint-title">💡 {{ t('drama.doubaoHintTitle') }}</span>
            <button type="button" class="pf-btn pf-btn-sm" @click="copyDoubaoPrompt">
              {{ t('drama.copyDoubaoPrompt') }}
            </button>
          </div>
          <p class="drama-doubao-hint-steps">{{ t('drama.doubaoHintSteps') }}</p>
          <p class="drama-doubao-hint-note">{{ t('drama.doubaoHintNote') }}</p>
        </div>

        <div v-if="assetKind(detailAsset) === 'character'" class="drama-field">
          <span>{{ t('drama.assetVoice') }}</span>
          <div class="drama-asset-voice-row">
            <n-select
              v-model:value="detailVoice"
              :options="voiceOptions"
              filterable
              clearable
              class="drama-asset-voice-select"
              :placeholder="t('drama.assetVoicePlaceholder')"
            />
            <button
              type="button"
              class="pf-btn pf-btn-sm"
              :disabled="!detailVoice || voicePreviewBusy === detailVoice"
              @click="previewVoice"
            >
              {{
                playingVoice === detailVoice
                  ? t('drama.stopPreview')
                  : voicePreviewBusy === detailVoice
                  ? t('drama.previewing')
                  : t('drama.previewVoice')
              }}
            </button>
            <n-button type="primary" size="small" :loading="voiceSaving" @click="saveVoice">
              {{ voiceSaving ? t('drama.saving') : t('drama.saveVoice') }}
            </n-button>
          </div>
          <p class="drama-muted drama-asset-ref-hint">{{ t('drama.assetVoiceHint') }}</p>
          <router-link class="drama-asset-voice-clone-link" :to="{ name: VoiceCloneRouteEnum.VOICE_CLONE_INDEX }">
            {{ t('drama.gotoVoiceClone') }}
          </router-link>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="detailVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button
            :disabled="promptSaving || promptDraft.trim() === readVisualPrompt(detailAsset)"
            @click="savePrompt"
          >
            {{ promptSaving ? t('drama.saving') : t('drama.savePrompt') }}
          </n-button>
          <n-button
            type="primary"
            :disabled="busyIds.has(detailAsset?.id || '') || !promptDraft.trim()"
            :loading="busyIds.has(detailAsset?.id || '')"
            @click="generateFromDetail"
          >
            <ShimmerText v-if="busyIds.has(detailAsset?.id || '')" size="13px">{{
              t('drama.generatingImage')
            }}</ShimmerText>
            <template v-else>{{ genLabel(detailAsset) }}</template>
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- 新建资产弹窗 -->
    <n-modal
      v-model:show="createVisible"
      preset="card"
      class="drama-asset-detail-modal"
      :title="t('drama.createAsset')"
      style="max-width: 560px"
    >
      <div class="drama-asset-detail">
        <div class="drama-field">
          <span>{{ t('drama.assetType') }}</span>
          <div class="drama-asset-tabs">
            <button
              v-for="tab in ASSET_TABS"
              :key="tab.key"
              type="button"
              :class="{ active: createType === tab.key }"
              @click="createType = tab.key"
            >
              {{ t(tab.labelKey) }}
            </button>
          </div>
        </div>

        <label class="drama-field">
          <span>{{ t('drama.assetName') }}</span>
          <input v-model="createName" type="text" :placeholder="t('drama.assetNamePlaceholder')" />
        </label>

        <label class="drama-field">
          <span>{{ t('drama.imagePrompt') }}</span>
          <textarea v-model="createPrompt" rows="4" :placeholder="t('drama.createAssetPromptPlaceholder')" />
        </label>

        <div class="drama-field">
          <span>{{ t('drama.uploadImage') }}</span>
          <input
            ref="createUploadRef"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            style="display: none"
            :disabled="creating"
            @change="handleCreateFile"
          />
          <div class="drama-asset-detail-extra">
            <button type="button" class="pf-btn pf-btn-sm" :disabled="creating" @click="createUploadRef?.click()">
              {{ createFile ? t('drama.changeImage') : t('drama.uploadImage') }}
            </button>
            <span v-if="createFile" class="drama-muted">{{ createFile.name }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-[8px]">
          <n-button @click="createVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :disabled="creating || !canCreate" :loading="creating" @click="submitCreate">
            {{ creating ? t('drama.creating') : t('drama.createAsset') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- 图片放大 -->
    <n-image-preview v-model:show="lightboxVisible" :src="lightboxSrc" />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { NButton, NImagePreview, NModal, NSelect, useMessage } from 'naive-ui';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiVideoVoice, DramaAsset } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';

  import {
    createAiDramaAsset,
    deleteAiDramaAsset,
    generateAiDramaAssetImage,
    getAiDramaAssets,
    getAiDramaProject,
    getAiDramaVoices,
    previewAiVideoVoice,
    seedAiDramaAssets,
    updateAiDramaAsset,
    uploadAiDramaAssetImage,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import { DramaRouteEnum, VoiceCloneRouteEnum } from '@/enums/routeEnum';

  import './printfilm.css';
  import {
    assetHasImage,
    assetKind,
    assetNeedsImageGeneration,
    type DramaAssetKind,
    parseAssetParams,
    readProjectAssetsSeedCreated,
    readProjectAssetsSeedStatus,
    readVisualPrompt,
  } from './dramaAssetUtils';

  type AssetTabKey = Exclude<DramaAssetKind, 'other'>;
  const ASSET_TABS: Array<{ key: AssetTabKey; labelKey: string }> = [
    { key: 'character', labelKey: 'drama.kind.character' },
    { key: 'scene', labelKey: 'drama.kind.scene' },
    { key: 'prop', labelKey: 'drama.kind.prop' },
  ];
  const ASSET_ASPECT_RATIO: Record<AssetTabKey, string> = {
    character: '3:4',
    scene: '16:9',
    prop: '1:1',
  };

  const props = defineProps<{
    projectId: string;
    summaryReady: boolean;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const userStore = useUserStore();

  const assets = ref<DramaAsset[]>([]);
  const activeTab = ref<AssetTabKey>('character');
  const loading = ref(true);
  const seeding = ref(false);
  const reseedBusy = ref(false);
  const busyIds = ref<Set<string>>(new Set());

  const detailAsset = ref<DramaAsset | null>(null);
  const detailVisible = ref(false);
  const promptDraft = ref('');
  const promptSaving = ref(false);
  const uploading = ref(false);
  const uploadInputRef = ref<HTMLInputElement | null>(null);

  const voiceOptions = ref<AiVideoVoice[]>([]);
  const detailVoice = ref<string | null>(null);
  const voiceSaving = ref(false);
  const voicePreviewBusy = ref<string | null>(null);
  const playingVoice = ref<string | null>(null);
  let auditionAudio: HTMLAudioElement | null = null;

  const lightboxSrc = ref('');
  const lightboxAlt = ref('');
  const lightboxVisible = ref(false);

  const createVisible = ref(false);
  const createType = ref<AssetTabKey>('scene');
  const createName = ref('');
  const createPrompt = ref('');
  const createFile = ref<File | null>(null);
  const creating = ref(false);
  const createUploadRef = ref<HTMLInputElement | null>(null);

  const filtered = computed(() => assets.value.filter((a) => assetKind(a) === activeTab.value));
  const pendingCount = computed(
    () => filtered.value.filter((a) => assetNeedsImageGeneration(a) && !busyIds.value.has(a.id)).length
  );
  const canCreate = computed(() => !!(createName.value.trim() || createPrompt.value.trim() || createFile.value));

  function previewUrl(asset: DramaAsset | null | undefined): string {
    if (!asset?.coverAttachmentId) return '';
    return `${PreviewAttachmentUrl}/${asset.coverAttachmentId}?userId=${userStore.userInfo.id}`;
  }

  function kindLabel(asset: DramaAsset): string {
    return t(`drama.kind.${assetKind(asset)}`);
  }

  function genLabel(asset: DramaAsset | null | undefined): string {
    if (!asset) return t('drama.genImage');
    if (busyIds.value.has(asset.id)) return t('drama.generatingImage');
    return assetHasImage(asset) ? t('drama.regenImage') : t('drama.genImage');
  }

  function replaceAsset(next: DramaAsset) {
    assets.value = assets.value.map((a) => (a.id === next.id ? next : a));
    if (detailAsset.value?.id === next.id) detailAsset.value = next;
  }

  function markBusy(id: string, busy: boolean) {
    const next = new Set(busyIds.value);
    if (busy) {
      next.add(id);
    } else {
      next.delete(id);
    }
    busyIds.value = next;
  }

  function waitAssetsDone(assetId: string): Promise<DramaAsset | null> {
    const started = Date.now();
    return new Promise((resolve) => {
      const timer = window.setInterval(async () => {
        if (Date.now() - started >= 3 * 60 * 1000) {
          window.clearInterval(timer);
          resolve(null);
          return;
        }
        try {
          const list = await getAiDramaAssets(props.projectId);
          assets.value = list;
          const found = list.find((a) => a.id === assetId) || null;
          if (found && (found.status === 'completed' || found.status === 'failed')) {
            window.clearInterval(timer);
            resolve(found);
          }
        } catch {
          // 瞬时网络失败不中断轮询，等待下一轮重试
        }
      }, 2000);
    });
  }

  async function generateAsset(asset: DramaAsset) {
    if (busyIds.value.has(asset.id)) return;
    markBusy(asset.id, true);
    try {
      const updated = await generateAiDramaAssetImage(asset.id);
      replaceAsset(updated);
      const done = await waitAssetsDone(asset.id);
      if (!done) {
        Message.error(t('drama.genTimeout'));
        return;
      }
      if (done.status === 'failed') {
        Message.error(t('drama.assetGenFailed'));
      } else {
        Message.success(t('drama.assetGenDone'));
      }
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.assetGenFailed'));
    } finally {
      markBusy(asset.id, false);
    }
  }

  function confirmDelete(asset: DramaAsset) {
    if (busyIds.value.has(asset.id)) return;
    openModal({
      type: 'warning',
      title: t('drama.assetDeleteTitle'),
      content: t('drama.assetDeleteConfirm', { name: asset.name || t('drama.unnamed') }),
      positiveText: t('drama.delete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiDramaAsset(asset.id);
          assets.value = assets.value.filter((a) => a.id !== asset.id);
          if (detailAsset.value?.id === asset.id) detailVisible.value = false;
        } catch (err) {
          Message.error((err as Error)?.message || t('drama.deleteFailed'));
        }
      },
    });
  }

  // ==================== 详情弹窗 ====================

  function openDetail(asset: DramaAsset) {
    detailAsset.value = asset;
    promptDraft.value = readVisualPrompt(asset);
    detailVoice.value = asset.voice || null;
    detailVisible.value = true;
  }

  function buildPromptParams(asset: DramaAsset, prompt: string): string {
    const prev = parseAssetParams(asset);
    const next: Record<string, unknown> = { ...prev, visualPrompt: prompt.trim() };
    const kind = assetKind(asset);
    if (kind === 'character' || kind === 'scene') {
      next.visualImage = prompt.trim();
    }
    return JSON.stringify(next);
  }

  async function savePrompt() {
    if (!detailAsset.value) return;
    promptSaving.value = true;
    try {
      const updated = await updateAiDramaAsset(detailAsset.value.id, {
        paramsJson: buildPromptParams(detailAsset.value, promptDraft.value),
      });
      replaceAsset(updated);
      Message.success(t('common.saveSuccess'));
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.saveFailed'));
    } finally {
      promptSaving.value = false;
    }
  }

  function buildDoubaoPrompt(): string {
    // 豆包为中文产品，提示词固定中文；造型描述取当前「生图提示词」，为空则用兜底
    const visual = promptDraft.value.trim() || t('drama.doubaoPromptVisualFallback');
    return t('drama.doubaoPromptTemplate', { visual });
  }

  async function copyDoubaoPrompt() {
    try {
      await navigator.clipboard.writeText(buildDoubaoPrompt());
      Message.success(t('drama.copied'));
    } catch {
      Message.error(t('drama.copyFailed'));
    }
  }

  async function saveVoice() {
    if (!detailAsset.value) return;
    voiceSaving.value = true;
    try {
      // 空串触发后端 trimToNull 清空绑定
      const updated = await updateAiDramaAsset(detailAsset.value.id, { voice: detailVoice.value || '' });
      replaceAsset(updated);
      Message.success(t('drama.voiceSaved'));
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.saveFailed'));
    } finally {
      voiceSaving.value = false;
    }
  }

  function stopAudition() {
    if (auditionAudio) {
      auditionAudio.pause();
      auditionAudio.src = '';
      auditionAudio = null;
    }
    playingVoice.value = null;
  }

  async function previewVoice() {
    const voice = detailVoice.value;
    if (!voice || voicePreviewBusy.value) {
      return;
    }
    if (playingVoice.value === voice) {
      stopAudition();
      return;
    }
    stopAudition();
    voicePreviewBusy.value = voice;
    try {
      const res = await previewAiVideoVoice(voice);
      if (!res?.audio) {
        Message.warning(t('drama.voicePreviewEmpty'));
        return;
      }
      const binary = atob(res.audio);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i += 1) {
        bytes[i] = binary.charCodeAt(i);
      }
      const blob = new Blob([bytes], { type: 'audio/wav' });
      const url = URL.createObjectURL(blob);
      auditionAudio = new Audio(url);
      playingVoice.value = voice;
      auditionAudio.onended = () => {
        if (playingVoice.value === voice) {
          playingVoice.value = null;
        }
      };
      auditionAudio.onerror = () => {
        playingVoice.value = null;
      };
      await auditionAudio.play().catch(() => {
        playingVoice.value = null;
      });
    } catch (error) {
      Message.error((error as Error)?.message || t('drama.voicePreviewFailed'));
    } finally {
      voicePreviewBusy.value = null;
    }
  }

  async function generateFromDetail() {
    if (!detailAsset.value) return;
    if (promptDraft.value.trim() !== readVisualPrompt(detailAsset.value).trim()) {
      await savePrompt();
      if (!detailAsset.value) return;
    }
    detailVisible.value = false;
    await generateAsset(detailAsset.value);
  }

  function openLightbox(asset: DramaAsset) {
    const src = previewUrl(asset);
    if (!src) return;
    lightboxSrc.value = src;
    lightboxAlt.value = asset.name || t('drama.unnamed');
    lightboxVisible.value = true;
  }

  async function handleUpload(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !detailAsset.value) return;
    uploading.value = true;
    try {
      const res = await uploadAiDramaAssetImage(detailAsset.value.id, { fileList: [file] });
      replaceAsset(res.data);
      Message.success(t('drama.uploadDone'));
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.uploadFailed'));
    } finally {
      uploading.value = false;
      if (input) input.value = '';
    }
  }

  // ==================== 新建资产 ====================

  function openCreate() {
    createType.value = activeTab.value;
    createName.value = '';
    createPrompt.value = '';
    createFile.value = null;
    createVisible.value = true;
  }

  function handleCreateFile(e: Event) {
    const input = e.target as HTMLInputElement;
    createFile.value = input.files?.[0] || null;
  }

  async function submitCreate() {
    if (creating.value) return;
    creating.value = true;
    try {
      const kind = createType.value;
      const aspectRatio = ASSET_ASPECT_RATIO[kind];
      const prompt = createPrompt.value.trim();
      const paramsJson = JSON.stringify({
        visualPrompt: prompt,
        visualImage: prompt,
        kind,
        aspectRatio,
      });
      const created = await createAiDramaAsset({
        projectId: props.projectId,
        type: kind,
        assetType: 'image',
        name: createName.value.trim() || t('drama.unnamed'),
        paramsJson,
      });
      let asset = created;
      if (createFile.value) {
        const res = await uploadAiDramaAssetImage(created.id, { fileList: [createFile.value] });
        asset = res.data;
      }
      assets.value = [...assets.value, asset];
      activeTab.value = kind;
      createVisible.value = false;
      Message.success(t('drama.assetCreated'));
      openDetail(asset);
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.createAssetFailed'));
    } finally {
      creating.value = false;
    }
  }

  // ==================== 抽取资产 ====================

  function waitSeedDone(): Promise<string> {
    const started = Date.now();
    return new Promise((resolve) => {
      const timer = window.setInterval(async () => {
        if (Date.now() - started >= 3 * 60 * 1000) {
          window.clearInterval(timer);
          resolve('failed');
          return;
        }
        try {
          const p = await getAiDramaProject(props.projectId);
          const status = readProjectAssetsSeedStatus(p);
          if (status === 'done' || status === 'failed') {
            window.clearInterval(timer);
            resolve(status);
          }
        } catch {
          // 瞬时网络失败不中断轮询
        }
      }, 2000);
    });
  }

  async function seedAndWait(refreshPrompts: boolean): Promise<string> {
    const res = await seedAiDramaAssets({ projectId: props.projectId, refreshPrompts });
    if (res.status === 'generating') {
      return waitSeedDone();
    }
    return res.status;
  }

  async function autoSeedIfNeeded() {
    if (assets.value.length === 0 && props.summaryReady) {
      seeding.value = true;
      try {
        await seedAndWait(false);
        assets.value = await getAiDramaAssets(props.projectId);
      } catch (err) {
        Message.error((err as Error)?.message || t('drama.seedFailed'));
      } finally {
        seeding.value = false;
      }
    }
  }

  async function handleReseed() {
    if (reseedBusy.value || seeding.value) return;
    openModal({
      type: 'warning',
      title: t('drama.reseedTitle'),
      content: t('drama.reseedConfirm'),
      positiveText: t('drama.reseedAssets'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        reseedBusy.value = true;
        try {
          const status = await seedAndWait(true);
          assets.value = await getAiDramaAssets(props.projectId);
          if (status === 'failed') {
            Message.error(t('drama.seedFailed'));
            return;
          }
          const created = readProjectAssetsSeedCreated(await getAiDramaProject(props.projectId));
          Message.success(t('drama.seedDone', { count: created }));
        } catch (err) {
          Message.error((err as Error)?.message || t('drama.seedFailed'));
        } finally {
          reseedBusy.value = false;
        }
      },
    });
  }

  onMounted(async () => {
    loading.value = true;
    try {
      assets.value = await getAiDramaAssets(props.projectId);
      await autoSeedIfNeeded();
    } catch (err) {
      Message.error((err as Error)?.message || t('drama.loadFailed'));
    } finally {
      loading.value = false;
    }
    try {
      voiceOptions.value = await getAiDramaVoices();
    } catch {
      voiceOptions.value = [];
    }
  });
</script>
