<template>
  <CrmCard hide-footer class="sales-advisor">
    <template #header>
      <div class="flex items-center gap-[8px]">
        <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
        <span class="text-[14px] font-semibold">{{ t('workbench.smart.advisorTitle') }}</span>
      </div>
    </template>

    <div class="flex flex-col gap-[16px] px-[24px] pb-[24px]">
      <div class="text-[13px] text-orange-500">{{ t('workbench.smart.advisorDesc') }}</div>

      <n-input
        v-model:value="message"
        type="textarea"
        :placeholder="t('workbench.smart.advisorPlaceholder')"
        :autosize="{ minRows: 5, maxRows: 12 }"
      />

      <div class="flex flex-wrap items-center gap-[8px]">
        <n-upload :custom-request="customUpload" :show-file-list="false" accept="image/*" multiple>
          <n-button size="small" :disabled="analyzing">
            <template #icon>
              <CrmIcon type="iconicon_link1" :size="16" />
            </template>
            {{ t('workbench.smart.advisorUploadTip') }}
          </n-button>
        </n-upload>
        <n-tag
          v-for="(pic, index) in pics"
          :key="pic.id"
          size="small"
          closable
          :disabled="analyzing"
          @close="removePic(index)"
        >
          {{ pic.name }}
        </n-tag>
      </div>

      <div>
        <n-button type="primary" :loading="analyzing" :disabled="!canAnalyze" @click="handleAnalyze">
          {{ analyzing ? t('workbench.smart.advisorAnalyzing') : t('workbench.smart.advisorAnalyze') }}
        </n-button>
      </div>

      <div v-if="result" class="advisor-result flex flex-col gap-[16px]">
        <div v-if="result.rawAnalysis" class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
          {{ result.rawAnalysis }}
        </div>

        <template v-else>
          <!-- 三大指标：意向评分 / 流失风险 / 情绪 -->
          <div class="grid grid-cols-3 gap-[12px]">
            <div class="advisor-metric">
              <div class="relative flex items-center justify-center py-[4px]">
                <svg viewBox="0 0 120 120" class="h-[108px] w-[108px]">
                  <defs>
                    <linearGradient id="advisor-intent-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" :stop-color="intentLevel.color" />
                      <stop offset="100%" :stop-color="intentLevel.light" />
                    </linearGradient>
                  </defs>
                  <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(148,163,184,0.16)" stroke-width="9" />
                  <circle
                    cx="60"
                    cy="60"
                    r="50"
                    fill="none"
                    stroke="url(#advisor-intent-grad)"
                    stroke-width="9"
                    stroke-linecap="round"
                    :stroke-dasharray="circumference"
                    :stroke-dashoffset="dashOffset"
                    transform="rotate(-90 60 60)"
                    class="advisor-intent-ring"
                    :style="{ filter: `drop-shadow(0 0 5px ${intentLevel.color}66)` }"
                  />
                </svg>
                <div class="absolute flex flex-col items-center">
                  <span class="text-[30px] font-bold leading-none tracking-tight" :style="{ color: intentLevel.color }">
                    {{ intentNum ?? '--' }}
                  </span>
                  <span class="mt-[5px] text-[11px] text-[var(--text-n3)]">
                    {{ t('workbench.smart.advisorIntentScore') }}
                  </span>
                </div>
              </div>
              <div class="mt-[6px] flex justify-center">
                <span class="rounded-full px-[10px] py-[2px] text-[12px] font-medium" :style="intentBadgeStyle">
                  {{ intentLevel.label }}
                </span>
              </div>
            </div>

            <div class="advisor-metric">
              <template v-if="churnLevel">
                <div
                  class="mx-auto flex h-[52px] w-[52px] items-center justify-center rounded-full"
                  :style="{
                    background: `${churnLevel.color}1a`,
                    border: `2px solid ${churnLevel.color}`,
                    boxShadow: `0 0 18px ${churnLevel.color}55`,
                  }"
                >
                  <span
                    class="h-[16px] w-[16px] rounded-full"
                    :style="{ background: churnLevel.color, boxShadow: `0 0 10px ${churnLevel.color}` }"
                  ></span>
                </div>
                <div class="mt-[8px] text-center text-[13px] font-semibold" :style="{ color: churnLevel.color }">
                  {{ churnLevel.label }}
                </div>
              </template>
              <div class="clamp-3 mt-[8px] text-center text-[11px] leading-[1.5] text-[var(--text-n3)]">
                {{ churnReason }}
              </div>
            </div>

            <div class="advisor-metric">
              <span class="text-center text-[36px] leading-none">{{ emotionEmoji }}</span>
              <div class="clamp-4 mt-[8px] text-center text-[12px] leading-[1.6] text-[var(--text-n2)]">
                {{ result.emotion || '—' }}
              </div>
            </div>
          </div>

          <!-- 成交信号 vs 异议点 -->
          <div class="grid grid-cols-2 gap-[12px]">
            <div class="advisor-list-card">
              <div class="mb-[8px] flex items-center gap-[6px]">
                <span
                  class="flex h-[18px] w-[18px] items-center justify-center rounded-full text-[11px] font-bold"
                  style="color: #10b981; background: rgb(16 185 129 / 12%)"
                >
                  ✓
                </span>
                <span class="text-[13px] font-semibold text-[var(--text-n1)]">
                  {{ t('workbench.smart.advisorSignals') }}
                </span>
              </div>
              <div v-if="result.signals?.length" class="flex flex-col gap-[6px]">
                <div
                  v-for="(item, index) in result.signals"
                  :key="index"
                  class="flex gap-[6px] text-[12px] leading-[1.5] text-[var(--text-n2)]"
                >
                  <span class="mt-[6px] h-[4px] w-[4px] shrink-0 rounded-full" style="background: #10b981"></span>
                  <span>{{ item }}</span>
                </div>
              </div>
              <div v-else class="text-[12px] text-[var(--text-n4)]">—</div>
            </div>

            <div class="advisor-list-card">
              <div class="mb-[8px] flex items-center gap-[6px]">
                <span
                  class="flex h-[18px] w-[18px] items-center justify-center rounded-full text-[11px] font-bold"
                  style="color: #ef4444; background: rgb(239 68 68 / 12%)"
                >
                  ✗
                </span>
                <span class="text-[13px] font-semibold text-[var(--text-n1)]">
                  {{ t('workbench.smart.advisorObjections') }}
                </span>
              </div>
              <div v-if="result.objections?.length" class="flex flex-col gap-[6px]">
                <div
                  v-for="(item, index) in result.objections"
                  :key="index"
                  class="flex gap-[6px] text-[12px] leading-[1.5] text-[var(--text-n2)]"
                >
                  <span class="mt-[6px] h-[4px] w-[4px] shrink-0 rounded-full" style="background: #ef4444"></span>
                  <span>{{ item }}</span>
                </div>
              </div>
              <div v-else class="text-[12px] text-[var(--text-n4)]">—</div>
            </div>
          </div>

          <!-- 竞品提及 -->
          <div v-if="result.competitorMentions?.length" class="flex items-center gap-[8px]">
            <span class="shrink-0 text-[12px] text-[var(--text-n3)]">{{ t('workbench.smart.advisorCompetitor') }}</span>
            <div class="flex flex-wrap gap-[6px]">
              <n-tag
                v-for="(item, index) in result.competitorMentions"
                :key="index"
                size="small"
                round
                :bordered="false"
                type="warning"
              >
                {{ item }}
              </n-tag>
            </div>
          </div>

          <!-- 候选话术 -->
          <div v-if="scriptCards.length" class="flex flex-col gap-[8px]">
            <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{
              t('workbench.smart.advisorScripts')
            }}</span>
            <div v-for="(item, index) in scriptCards" :key="index" class="advisor-script-card">
              <div class="flex items-start gap-[10px]">
                <span
                  class="mt-[2px] flex h-[20px] w-[20px] shrink-0 items-center justify-center rounded-[6px] text-[12px] font-bold text-white"
                  style="background: linear-gradient(135deg, #6366f1, #8b5cf6)"
                >
                  {{ index + 1 }}
                </span>
                <div class="flex-1">
                  <div v-if="item.title" class="flex flex-wrap items-center gap-[6px]">
                    <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ item.title }}</span>
                    <n-tag v-if="item.source" size="small" :bordered="false" type="info">
                      {{ item.source }}
                    </n-tag>
                  </div>
                  <div class="mt-[4px] text-[13px] leading-[1.6] text-[var(--text-n2)]">{{ item.content }}</div>
                  <div v-if="item.originalContent" class="mt-[6px]">
                    <n-button size="tiny" secondary color="#f97316" @click="toggleOriginal(index)">
                      {{
                        showOriginal[index]
                          ? t('workbench.smart.advisorHideOriginal')
                          : t('workbench.smart.advisorOriginal')
                      }}
                    </n-button>
                    <div
                      v-if="showOriginal[index]"
                      class="mt-[4px] rounded-[6px] bg-[var(--fill-2)] p-[8px] text-[12px] leading-[1.6] text-[var(--text-n3)]"
                    >
                      {{ item.originalContent }}
                    </div>
                  </div>
                </div>
                <n-button size="tiny" quaternary @click="copyScript(item.content)">
                  {{ t('workbench.smart.advisorCopy') }}
                </n-button>
              </div>
            </div>
          </div>
        </template>

        <div class="flex justify-end gap-[8px]">
          <n-button size="small" type="primary" ghost :loading="analyzing" @click="handleAnalyze">
            {{ t('workbench.smart.advisorAnalyzeAgain') }}
          </n-button>
          <n-button size="small" type="primary" @click="handleToFollow">
            {{ t('workbench.smart.advisorToFollow') }}
          </n-button>
        </div>
      </div>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="followDrawerVisible"
      :form-key="FormDesignKeyEnum.FOLLOW_RECORD"
      :initial-values="followInitialValues"
      @saved="handleFollowSaved"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NButton, NInput, NTag, NUpload, type UploadCustomRequestOptions, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SalesAdvisorAnalyzeResult } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';

  import { analyzeSalesAdvisor, uploadTempAttachment } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const message = ref('');
  const analyzing = ref(false);
  const result = ref<SalesAdvisorAnalyzeResult>();

  interface Pic {
    id: string;
    name: string;
  }
  const pics = ref<Pic[]>([]);

  const canAnalyze = computed(() => !analyzing.value && (message.value.trim() !== '' || pics.value.length > 0));

  interface ScriptCard {
    title?: string;
    content: string;
    source?: string;
    originalContent?: string;
  }

  const showOriginal = ref<Record<number, boolean>>({});

  const scriptCards = computed<ScriptCard[]>(() => {
    const recommendations = result.value?.scriptRecommendations;
    if (recommendations?.length) {
      return recommendations.map((item) => ({
        title: item.title,
        content: item.content || '',
        source: item.source,
        originalContent: item.originalContent,
      }));
    }
    return (result.value?.suggestedScripts || []).map((content) => ({ content }));
  });

  function toggleOriginal(index: number) {
    showOriginal.value[index] = !showOriginal.value[index];
  }

  const intentNum = computed(() => {
    const raw = result.value?.intentScore;
    if (!raw) {
      return null;
    }
    const num = Number(raw);
    if (!Number.isFinite(num) || num < 0 || num > 100) {
      return null;
    }
    return Math.round(num);
  });

  const circumference = 2 * Math.PI * 50;

  const intentLevel = computed(() => {
    const n = intentNum.value;
    if (n === null) {
      return { color: '#64748b', light: '#94a3b8', label: '—' };
    }
    if (n >= 75) {
      return { color: '#10b981', light: '#34d399', label: t('workbench.smart.advisorIntentHigh') };
    }
    if (n >= 45) {
      return { color: '#f97316', light: '#fb923c', label: t('workbench.smart.advisorIntentMedium') };
    }
    return { color: '#ef4444', light: '#f87171', label: t('workbench.smart.advisorIntentLow') };
  });

  const dashOffset = computed(() => {
    const n = intentNum.value;
    if (n === null) {
      return circumference;
    }
    const clamped = Math.min(100, Math.max(0, n));
    return circumference * (1 - clamped / 100);
  });

  const intentBadgeStyle = computed(() => ({
    color: intentLevel.value.color,
    background: `${intentLevel.value.color}1a`,
    border: `1px solid ${intentLevel.value.color}40`,
  }));

  const churnLevel = computed(() => {
    const s = result.value?.churnRisk || '';
    if (/^高/.test(s)) {
      return { color: '#ef4444', label: t('workbench.smart.advisorRiskHigh') };
    }
    if (/^中/.test(s)) {
      return { color: '#f97316', label: t('workbench.smart.advisorRiskMedium') };
    }
    if (/^低/.test(s)) {
      return { color: '#10b981', label: t('workbench.smart.advisorRiskLow') };
    }
    return null;
  });

  const churnReason = computed(() => {
    const s = result.value?.churnRisk || '';
    const matched = s.match(/^[高中低]\s*(.*)$/);
    return matched && matched[1] ? matched[1] : s;
  });

  const emotionTone = computed(() => {
    const s = result.value?.emotion || '';
    if (/不满|生气|失望|愤怒|抵触|消极|反感|不耐烦|拒绝|担忧|顾虑|担心|犹豫|流失|放弃/.test(s)) {
      return 'negative';
    }
    if (/满意|积极|开心|感兴趣|认可|信任|愿意|主动|乐观|热情|开放|意愿|合作|意向/.test(s)) {
      return 'positive';
    }
    return 'neutral';
  });

  const emotionEmoji = computed(() => {
    if (emotionTone.value === 'positive') {
      return '😊';
    }
    if (emotionTone.value === 'negative') {
      return '😟';
    }
    return '😐';
  });

  async function customUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
    try {
      const res = await uploadTempAttachment(file.file as File);
      const id = res.data?.[0];
      if (!id) {
        throw new Error('upload empty');
      }
      pics.value.push({ id, name: file.name });
      onFinish();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      Message.error(t('common.operationFailed'));
      onError();
    }
  }

  function removePic(index: number) {
    pics.value.splice(index, 1);
  }

  async function handleAnalyze() {
    if (!canAnalyze.value) {
      return;
    }
    try {
      analyzing.value = true;
      showOriginal.value = {};
      result.value = await analyzeSalesAdvisor({
        message: message.value.trim() || undefined,
        picIds: pics.value.map((pic) => pic.id),
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      analyzing.value = false;
    }
  }

  const followDrawerVisible = ref(false);
  const followInitialValues = ref<Record<string, any>>({});

  function buildFollowContent(): string {
    const r = result.value;
    if (!r) {
      return '';
    }
    const lines: string[] = [];
    if (r.intentScore) {
      lines.push(`意向评分：${r.intentScore}`);
    }
    if (r.signals?.length) {
      lines.push(`成交信号：${r.signals.join('；')}`);
    }
    if (r.objections?.length) {
      lines.push(`异议点：${r.objections.join('；')}`);
    }
    if (r.emotion) {
      lines.push(`情绪满意度：${r.emotion}`);
    }
    if (r.competitorMentions?.length) {
      lines.push(`竞品提及：${r.competitorMentions.join('；')}`);
    }
    if (r.churnRisk) {
      lines.push(`流失风险：${r.churnRisk}`);
    }
    if (r.suggestedScripts?.length) {
      lines.push(`候选话术：\n${r.suggestedScripts.map((script) => `- ${script}`).join('\n')}`);
    }
    return lines.join('\n');
  }

  function handleToFollow() {
    if (!result.value) {
      return;
    }
    followInitialValues.value = { content: buildFollowContent() };
    followDrawerVisible.value = true;
  }

  function handleFollowSaved() {
    Message.success(t('common.operationSuccess'));
    followDrawerVisible.value = false;
  }

  async function copyScript(text: string) {
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('workbench.smart.advisorCopied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }
</script>

<style scoped>
  .advisor-result {
    padding: 16px;
    border: 1px solid rgb(148 163 184 / 12%);
    border-radius: 8px;
    background: linear-gradient(160deg, rgb(99 102 241 / 6%), rgb(139 92 246 / 3%) 40%, transparent);
  }
  .advisor-metric {
    display: flex;
    justify-content: center;
    padding: 12px;
    min-height: 150px;
    border: 1px solid rgb(148 163 184 / 10%);
    border-radius: 8px;
    background: rgb(148 163 184 / 5%);
    flex-direction: column;
  }
  .advisor-list-card {
    padding: 12px;
    border: 1px solid rgb(148 163 184 / 10%);
    border-radius: 8px;
    background: rgb(148 163 184 / 5%);
  }
  .advisor-script-card {
    padding: 10px 12px;
    border: 1px solid rgb(99 102 241 / 16%);
    border-radius: 8px;
    background: rgb(99 102 241 / 5%);
  }
  .advisor-intent-ring {
    transition: stroke-dashoffset 0.7s ease-out;
  }
  .clamp-3 {
    display: box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .clamp-4 {
    display: box;
    -webkit-line-clamp: 4;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
</style>
