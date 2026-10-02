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

      <div v-if="result" class="flex flex-col gap-[16px] rounded-[4px] bg-[var(--text-n9)] p-[16px]">
        <div v-if="result.rawAnalysis" class="whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]">
          {{ result.rawAnalysis }}
        </div>
        <template v-else>
          <div class="flex items-center gap-[12px]">
            <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">
              {{ t('workbench.smart.advisorIntentScore') }}
            </span>
            <n-progress v-if="intentNum !== null" class="flex-1" type="line" :percentage="intentNum" />
            <span v-else class="text-[14px] font-semibold text-[var(--text-n1)]">
              {{ result.intentScore || '-' }}
            </span>
          </div>

          <div v-for="section in sections" v-show="section.items?.length" :key="section.title" class="flex gap-[12px]">
            <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">{{ section.title }}</span>
            <div class="flex flex-col gap-[4px]">
              <div v-for="(item, index) in section.items" :key="index" class="text-[13px] text-[var(--text-n1)]">
                {{ item }}
              </div>
            </div>
          </div>

          <div v-if="result.emotion" class="flex gap-[12px]">
            <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">
              {{ t('workbench.smart.advisorEmotion') }}
            </span>
            <span class="text-[13px] text-[var(--text-n1)]">{{ result.emotion }}</span>
          </div>

          <div v-if="result.churnRisk" class="flex gap-[12px]">
            <span class="w-[88px] shrink-0 text-[13px] text-[var(--text-n2)]">
              {{ t('workbench.smart.advisorChurnRisk') }}
            </span>
            <span class="text-[13px] text-[var(--text-n1)]">{{ result.churnRisk }}</span>
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
  import { NButton, NInput, NProgress, NTag, NUpload, type UploadCustomRequestOptions, useMessage } from 'naive-ui';

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

  const sections = computed(() => [
    { title: t('workbench.smart.advisorSignals'), items: result.value?.signals },
    { title: t('workbench.smart.advisorObjections'), items: result.value?.objections },
    { title: t('workbench.smart.advisorCompetitor'), items: result.value?.competitorMentions },
    { title: t('workbench.smart.advisorScripts'), items: result.value?.suggestedScripts },
  ]);

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
</script>
