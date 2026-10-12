<template>
  <div class="drama-page pf-asset-page">
    <!-- 页头：标题 + 返回项目 -->
    <header class="pf-drama-list-head">
      <div class="pf-drama-list-title-row">
        <div>
          <h1>{{ t('drama.assetLibraryTitle') }}</h1>
          <p class="pf-muted" style="margin-top: 0.35rem">{{ t('drama.assetLibraryDesc') }}</p>
        </div>
        <div class="pf-drama-list-actions">
          <router-link class="pf-btn pf-btn-sm" :to="{ name: DramaRouteEnum.DRAMA_INDEX }">
            {{ t('drama.backToProjects') }}
          </router-link>
        </div>
      </div>

      <div class="pf-drama-list-toolbar">
        <div class="pf-pill-tabs">
          <button
            v-for="tab in TABS"
            :key="tab.value"
            type="button"
            class="pf-pill"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ t(tab.labelKey) }}
          </button>
        </div>
        <div class="pf-asset-toolbar-filters">
          <n-select
            :value="projectId || null"
            :options="projectOptions"
            :placeholder="t('drama.allProjects')"
            clearable
            style="width: 200px"
            @update:value="projectId = ($event as string) || ''"
          />
          <label class="pf-drama-search">
            <span aria-hidden>🔍</span>
            <input v-model="query" type="text" :placeholder="t('drama.searchAssets')" />
          </label>
        </div>
      </div>
    </header>

    <div class="pf-asset-toolbar-meta">
      <p class="pf-muted" style="margin: 0">
        <ShimmerText v-if="loading" size="14px">{{ t('drama.loading') }}</ShimmerText>
        <template v-else>{{ t('drama.assetLibraryMeta', { total: filtered.length }) }}</template>
      </p>
    </div>

    <div v-if="!loading && !filtered.length" class="pf-empty-state is-compact">
      <p class="pf-muted">{{ t('drama.noAssets') }}</p>
    </div>

    <div v-else class="pf-asset-grid">
      <article v-for="asset in filtered" :key="asset.id" class="pf-asset-card">
        <div class="pf-asset-thumb">
          <button v-if="previewUrl(asset)" type="button" class="pf-asset-thumb-btn" @click="openLightbox(asset)">
            <img :src="previewUrl(asset)" :alt="asset.name || ''" />
          </button>
          <span v-else>{{ kindLabel(asset) }}</span>
        </div>
        <h3>{{ asset.name || t('drama.unnamed') }}</h3>
        <p>{{ kindLabel(asset) }} · {{ projectName(asset.projectId) }}</p>
        <div class="pf-asset-card-actions">
          <router-link class="pf-btn pf-btn-sm" :to="workspaceUrl(asset.projectId)">
            {{ t('drama.openProject') }}
          </router-link>
        </div>
      </article>
    </div>

    <!-- 图片放大 -->
    <n-image-preview v-model:show="lightboxVisible" :src="lightboxSrc" />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { NImagePreview, NSelect } from 'naive-ui';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DramaAsset, DramaProject } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';

  import { getAiDramaProjects, getAllAiDramaAssets } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import { DramaRouteEnum } from '@/enums/routeEnum';

  import './printfilm.css';
  import { assetKind, type DramaAssetKind } from './dramaAssetUtils';

  type AssetTabKey = 'all' | Exclude<DramaAssetKind, 'other'>;
  const TABS: Array<{ value: AssetTabKey; labelKey: string }> = [
    { value: 'all', labelKey: 'drama.filter.all' },
    { value: 'character', labelKey: 'drama.kind.character' },
    { value: 'scene', labelKey: 'drama.kind.scene' },
    { value: 'prop', labelKey: 'drama.kind.prop' },
  ];

  const { t } = useI18n();
  const userStore = useUserStore();

  const assets = ref<DramaAsset[]>([]);
  const projects = ref<DramaProject[]>([]);
  const activeTab = ref<AssetTabKey>('all');
  const projectId = ref('');
  const query = ref('');
  const loading = ref(true);

  const lightboxSrc = ref('');
  const lightboxAlt = ref('');
  const lightboxVisible = ref(false);

  const projectOptions = computed(() => projects.value.map((p) => ({ label: p.title || `#${p.id}`, value: p.id })));

  const projectNameById = computed(() => {
    const map = new Map<string, string>();
    projects.value.forEach((p) => map.set(p.id, p.title || `#${p.id}`));
    return map;
  });

  function kindLabel(asset: DramaAsset): string {
    return t(`drama.kind.${assetKind(asset)}`);
  }

  const filtered = computed(() => {
    const q = query.value.trim().toLowerCase();
    return assets.value.filter((asset) => {
      if (projectId.value && asset.projectId !== projectId.value) return false;
      if (activeTab.value !== 'all' && assetKind(asset) !== activeTab.value) return false;
      if (!q) return true;
      const name = (asset.name || '').toLowerCase();
      const kind = kindLabel(asset).toLowerCase();
      const project = (projectNameById.value.get(asset.projectId) || '').toLowerCase();
      return name.includes(q) || kind.includes(q) || project.includes(q);
    });
  });

  function previewUrl(asset: DramaAsset): string {
    if (!asset.coverAttachmentId) return '';
    return `${PreviewAttachmentUrl}/${asset.coverAttachmentId}?userId=${userStore.userInfo.id}`;
  }

  function projectName(id: string): string {
    return projectNameById.value.get(id) || `#${id}`;
  }

  function workspaceUrl(id: string) {
    return { name: DramaRouteEnum.DRAMA_WORKSPACE, params: { projectId: id } };
  }

  function openLightbox(asset: DramaAsset) {
    const src = previewUrl(asset);
    if (!src) return;
    lightboxSrc.value = src;
    lightboxAlt.value = asset.name || t('drama.unnamed');
    lightboxVisible.value = true;
  }

  onMounted(async () => {
    loading.value = true;
    try {
      const [allAssets, allProjects] = await Promise.all([getAllAiDramaAssets(), getAiDramaProjects()]);
      assets.value = allAssets;
      projects.value = allProjects;
    } catch {
      assets.value = [];
    } finally {
      loading.value = false;
    }
  });
</script>
