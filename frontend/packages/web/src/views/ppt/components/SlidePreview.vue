<template>
  <div class="slide-preview flex flex-col gap-[12px]">
    <div class="flex items-center justify-between">
      <span class="text-[13px] font-semibold text-[var(--text-n1)]">{{ t('ppt.slidePreview') }}</span>
      <AiActionButton :loading="generating" :disabled="!slides.length" @click="exportPptx">
        {{ t('ppt.downloadPpt') }}
      </AiActionButton>
    </div>

    <div
      v-if="!slides.length && rawContent"
      class="slide-raw whitespace-pre-wrap text-[13px] leading-[1.6] text-[var(--text-n2)]"
    >
      {{ rawContent }}
    </div>

    <div v-if="slides.length" class="grid grid-cols-1 gap-[14px] md:grid-cols-2">
      <div v-for="(slide, index) in localSlides" :key="index" class="slide-card">
        <div class="flex items-center gap-[8px]">
          <span
            class="flex h-[22px] min-w-[22px] shrink-0 items-center justify-center rounded-[6px] px-[6px] text-[12px] font-bold text-white"
            style="background: linear-gradient(135deg, #f97316, #fb923c)"
          >
            {{ index + 1 }}
          </span>
          <n-input v-model:value="slide.title" size="small" class="slide-card__title" placeholder="页标题" />
        </div>

        <div class="flex flex-col gap-[4px]">
          <span class="slide-card__label">{{ t('ppt.bullets') }}</span>
          <n-input
            v-model:value="slide.bulletText"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 8 }"
            placeholder="每行一个要点"
          />
        </div>

        <div class="flex flex-col gap-[4px]">
          <span class="slide-card__label">{{ t('ppt.notes') }}</span>
          <n-input
            v-model:value="slide.notes"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            placeholder="销售口头讲解词"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { NInput } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiPptSlide } from '@lib/shared/models/ai';

  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import PptxGenJS from 'pptxgenjs';

  interface LocalSlide {
    title: string;
    bulletText: string;
    notes: string;
  }

  const props = defineProps<{
    title?: string;
    slides: AiPptSlide[];
    rawContent?: string;
    generating?: boolean;
  }>();

  const { t } = useI18n();

  const localSlides = ref<LocalSlide[]>([]);

  watch(
    () => props.slides,
    (slides) => {
      localSlides.value = (slides || []).map((slide) => ({
        title: slide.title || '',
        bulletText: (slide.bullets || []).join('\n'),
        notes: slide.notes || '',
      }));
    },
    { immediate: true, deep: true }
  );

  async function exportPptx() {
    const pptx = new PptxGenJS();
    pptx.layout = 'LAYOUT_WIDE';

    // 品牌配色：告别纯白底，橙色主色 + 深浅灰文字
    const BRAND = 'F97316';
    const INK = '1F2937';
    const BODY = '4B5563';

    localSlides.value.forEach((slide, index) => {
      const page = pptx.addSlide();
      const title = slide.title || t('ppt.slidePreview');
      const isCover = index === 0;
      const isClosing = index === localSlides.value.length - 1 && /致谢|谢谢|感谢|下一步|结束/.test(title);

      if (isCover || isClosing) {
        // 封面 / 结尾页：品牌橙底 + 白字 + 白色分隔线
        page.background = { color: BRAND };
        page.addShape('rect', { x: 0, y: 0, w: 13.333, h: 0.14, fill: { color: 'FB923C' } });
        page.addText(title, {
          x: 0.9,
          y: 2.5,
          w: 11.5,
          h: 1.4,
          fontSize: 40,
          bold: true,
          color: 'FFFFFF',
          valign: 'top',
        });
        page.addShape('rect', { x: 0.95, y: 4.1, w: 2.4, h: 0.06, fill: { color: 'FFFFFF' } });
        const subtitle = slide.bulletText
          .split('\n')
          .map((line) => line.trim())
          .filter(Boolean)
          .join('  ·  ');
        if (subtitle) {
          page.addText(subtitle, {
            x: 0.9,
            y: 4.4,
            w: 11.5,
            h: 1.2,
            fontSize: 16,
            color: 'FFE0C2',
            valign: 'top',
          });
        }
      } else {
        // 内容页：白底 + 顶部品牌条 + 左侧色块标题 + 橙色要点
        page.background = { color: 'FFFFFF' };
        page.addShape('rect', { x: 0, y: 0, w: 13.333, h: 0.14, fill: { color: BRAND } });
        page.addShape('roundRect', {
          x: 0.45,
          y: 0.5,
          w: 0.12,
          h: 0.62,
          rectRadius: 0.02,
          fill: { color: BRAND },
        });
        page.addText(title, {
          x: 0.72,
          y: 0.42,
          w: 11.9,
          h: 0.8,
          fontSize: 24,
          bold: true,
          color: INK,
          valign: 'middle',
        });

        const bullets = slide.bulletText
          .split('\n')
          .map((line) => line.trim())
          .filter(Boolean)
          .map((text) => ({
            text,
            options: {
              bullet: { code: '25AA', color: BRAND, indent: 14 },
              color: BODY,
              breakLine: true,
              paraSpaceAfter: 12,
            },
          }));

        if (bullets.length) {
          page.addText(bullets, {
            x: 0.9,
            y: 1.55,
            w: 11.5,
            h: 5.2,
            fontSize: 16,
            valign: 'top',
            lineSpacingMultiple: 1.25,
          });
        }
      }

      if (slide.notes.trim()) {
        page.addNotes(slide.notes.trim());
      }
    });

    await pptx.writeFile({ fileName: `${props.title || t('ppt.slidePreview')}.pptx` });
  }
</script>

<style lang="less" scoped>
  .slide-card {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 14px 16px;
    border: 1px solid rgb(249 115 22 / 16%);
    border-radius: 8px;
    background: rgb(249 115 22 / 5%);
  }
  .slide-card__title {
    :deep(.n-input__input-el) {
      font-weight: 600;
    }
  }
  .slide-card__label {
    font-size: 12px;
    font-weight: 500;
    color: var(--text-n3);
  }
  .slide-raw {
    padding: 10px 12px;
    border-radius: 6px;
    background: rgb(148 163 184 / 8%);
  }
</style>
