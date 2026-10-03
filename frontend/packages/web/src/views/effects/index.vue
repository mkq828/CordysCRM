<template>
  <div class="effects-page flex h-full flex-col overflow-y-auto p-[16px]">
    <div class="mb-[16px]">
      <div class="flex items-center gap-[8px]">
        <CrmIcon type="iconicon_star1" :size="18" color="var(--primary-8)" />
        <span class="text-[16px] font-semibold">{{ t('effects.title') }}</span>
      </div>
      <div class="mt-[4px] text-[13px] text-orange-500">{{ t('effects.desc') }}</div>
    </div>

    <!-- A 边框与流光 -->
    <EffectSection :title="t('effects.sectionBorder')">
      <EffectItem code="A1" :name="t('effects.a1')">
        <FlowBorder>
          <div class="demo-panel">
            <span class="demo-panel__title">A1</span>
            <span class="demo-panel__text">彩色渐变边框持续流动，适合重点卡片或弹窗高亮。</span>
          </div>
        </FlowBorder>
      </EffectItem>

      <EffectItem code="A2" :name="t('effects.a2')">
        <BorderBeam>
          <span class="demo-panel__text">一道流光周期性扫过卡片，适合加载或强调状态。</span>
        </BorderBeam>
      </EffectItem>
    </EffectSection>

    <!-- B 背景动效 -->
    <EffectSection :title="t('effects.sectionBackground')">
      <EffectItem code="B1" :name="t('effects.b1')">
        <div class="h-[140px] overflow-hidden rounded-[10px]">
          <AuroraBackground>
            <div class="flex h-full items-center justify-center text-[14px] font-semibold text-white/90">
              极光流动背景
            </div>
          </AuroraBackground>
        </div>
      </EffectItem>

      <EffectItem code="B2" :name="t('effects.b2')">
        <div class="h-[140px] overflow-hidden rounded-[10px] bg-[#0f0f23]">
          <ParticleBackground color="#8b5cf6" :count="80" />
        </div>
      </EffectItem>
    </EffectSection>

    <!-- C 文字与数字 -->
    <EffectSection :title="t('effects.sectionText')">
      <EffectItem code="C1" :name="t('effects.c1')">
        <div class="demo-panel flex h-[72px] items-center justify-center">
          <GradientText size="28px">AI 中心</GradientText>
        </div>
      </EffectItem>

      <EffectItem code="C2" :name="t('effects.c2')">
        <div class="demo-panel flex h-[72px] items-center justify-center">
          <CountUp :value="1286" :duration="2000" size="36px" />
          <span class="ml-[6px] text-[14px] text-[var(--text-n3)]">条获客内容</span>
        </div>
      </EffectItem>

      <EffectItem code="C3" :name="t('effects.c3')">
        <div class="demo-panel flex h-[72px] items-center justify-center">
          <ShimmerText size="28px">流光扫字</ShimmerText>
        </div>
      </EffectItem>
    </EffectSection>

    <!-- D 交互组件 -->
    <EffectSection :title="t('effects.sectionInteractive')">
      <EffectItem code="D1" :name="t('effects.d1')">
        <div class="demo-panel flex h-[72px] items-center justify-center">
          <ShimmerButton>立即生成</ShimmerButton>
        </div>
      </EffectItem>

      <EffectItem code="D2" :name="t('effects.d2')">
        <TiltCard>
          <div class="demo-panel">
            <span class="demo-panel__title">3D 悬浮卡片</span>
            <span class="demo-panel__text">鼠标移动时卡片跟随倾斜，并带高光反射。</span>
          </div>
        </TiltCard>
      </EffectItem>

      <EffectItem code="D3" :name="t('effects.d3')">
        <div class="flex flex-col gap-[10px]">
          <div class="glass-demo glass-demo--light">
            <GlassCard>
              <div class="flex flex-col gap-[6px]">
                <span class="demo-panel__title">玻璃拟态（亮色）</span>
                <span class="demo-panel__text">渐变透明 + 背景模糊 + 顶部高光，叠彩色底上呈液态玻璃。</span>
              </div>
            </GlassCard>
          </div>
          <div class="glass-demo glass-demo--dark">
            <GlassCard dark>
              <div class="flex flex-col gap-[6px]">
                <span class="demo-panel__title">玻璃拟态（暗色）</span>
                <span class="demo-panel__text">暗色玻璃叠科技蓝绿光晕，hover 边框提亮，适合大屏。</span>
              </div>
            </GlassCard>
          </div>
        </div>
      </EffectItem>
    </EffectSection>

    <!-- E 数据大屏（暗色科技风） -->
    <EffectSection :title="t('effects.sectionData')">
      <EffectItem code="E1" :name="t('effects.e1')">
        <div class="dark-demo grid grid-cols-2 gap-[10px]">
          <DarkStatCard title="当前库存量" value="5,617.25" unit="吨" accent="#2196f3">
            <template #icon><CrmIcon type="iconicon_star1" :size="18" /></template>
          </DarkStatCard>
          <DarkStatCard title="日均消耗量" value="1,200" unit="吨" accent="#ffa726">
            <template #icon><CrmIcon type="iconicon_star1" :size="18" /></template>
          </DarkStatCard>
        </div>
      </EffectItem>

      <EffectItem code="E2" :name="t('effects.e2')">
        <div class="dark-demo flex items-center justify-center py-[8px]">
          <RingGauge :value="4.7" :max="7" :size="120" color="#ff6b6b" label="可用天数" />
        </div>
      </EffectItem>

      <EffectItem code="E3" :name="t('effects.e3')">
        <div class="dark-demo overflow-hidden !p-0">
          <TopShimmerBar :height="4" />
          <div class="flex h-[96px] items-center justify-center text-[13px] text-white/70">
            顶部流动渐变条，常放大屏卡片顶端
          </div>
        </div>
      </EffectItem>

      <EffectItem code="E4" :name="t('effects.e4')">
        <div class="dark-demo flex items-center justify-center py-[8px]">
          <FlipClock value="110420" unit="万" label="kWh" />
        </div>
      </EffectItem>

      <EffectItem code="E5" :name="t('effects.e5')">
        <div class="dark-demo flex items-center justify-center gap-[16px] py-[8px]">
          <PulsingBadge color="#ff6b6b" :size="48">
            <CrmIcon type="iconicon_star1" :size="20" />
          </PulsingBadge>
          <PulsingBadge color="#4caf50" :size="48">
            <CrmIcon type="iconicon_star1" :size="20" />
          </PulsingBadge>
          <PulsingBadge color="#ffa726" :size="48">
            <CrmIcon type="iconicon_star1" :size="20" />
          </PulsingBadge>
        </div>
      </EffectItem>
    </EffectSection>

    <!-- F 3D 工作台 -->
    <EffectSection :title="t('effects.sectionWorkbench')">
      <EffectItem code="F1" :name="t('effects.f1')">
        <Workbench3D>
          <template #panel1>任务管理</template>
          <template #panel2>数据统计</template>
          <template #panel3>AI 画像</template>
        </Workbench3D>
      </EffectItem>
    </EffectSection>
  </div>
</template>

<script setup lang="ts">
  import { defineComponent, h } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AuroraBackground from '@/components/pure/effects/AuroraBackground.vue';
  import BorderBeam from '@/components/pure/effects/BorderBeam.vue';
  import CountUp from '@/components/pure/effects/CountUp.vue';
  import DarkStatCard from '@/components/pure/effects/DarkStatCard.vue';
  import FlipClock from '@/components/pure/effects/FlipClock.vue';
  import FlowBorder from '@/components/pure/effects/FlowBorder.vue';
  import GlassCard from '@/components/pure/effects/GlassCard.vue';
  import GradientText from '@/components/pure/effects/GradientText.vue';
  import ParticleBackground from '@/components/pure/effects/ParticleBackground.vue';
  import PulsingBadge from '@/components/pure/effects/PulsingBadge.vue';
  import RingGauge from '@/components/pure/effects/RingGauge.vue';
  import ShimmerButton from '@/components/pure/effects/ShimmerButton.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import TiltCard from '@/components/pure/effects/TiltCard.vue';
  import TopShimmerBar from '@/components/pure/effects/TopShimmerBar.vue';
  import Workbench3D from '@/components/pure/effects/Workbench3D.vue';

  const { t } = useI18n();

  const EffectSection = defineComponent({
    name: 'EffectSection',
    props: { title: { type: String, required: true } },
    setup(props, { slots }) {
      return () =>
        h('div', { class: 'effect-section' }, [
          h('div', { class: 'effect-section__title' }, props.title),
          h('div', { class: 'effect-section__grid' }, slots.default?.()),
        ]);
    },
  });

  const EffectItem = defineComponent({
    name: 'EffectItem',
    props: { code: { type: String, required: true }, name: { type: String, required: true } },
    setup(props, { slots }) {
      return () =>
        h('div', { class: 'effect-item' }, [
          h('div', { class: 'effect-item__head' }, [
            h('span', { class: 'effect-item__code' }, props.code),
            h('span', { class: 'effect-item__name' }, props.name),
          ]),
          h('div', { class: 'effect-item__demo' }, slots.default?.()),
        ]);
    },
  });
</script>

<style lang="less" scoped>
  .effects-page {
    background: #f5f7fa;
  }
  .demo-panel {
    display: flex;
    padding: 16px;
    flex-direction: column;
    gap: 6px;
  }
  .demo-panel__title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .demo-panel__text {
    font-size: 12px;
    line-height: 1.6;
    color: var(--text-n3);
  }
  .dark-demo {
    padding: 12px;
    border-radius: 10px;
    background: linear-gradient(145deg, #0c1523, #131f35);
  }
  .glass-demo {
    position: relative;
    overflow: hidden;
    padding: 14px;
    border-radius: 12px;
  }
  .glass-demo--light {
    background: radial-gradient(circle at 12% 12%, #ff6b35 0%, transparent 55%),
      radial-gradient(circle at 88% 22%, #e6007e 0%, transparent 55%),
      radial-gradient(circle at 50% 88%, #7c3aed 0%, transparent 60%), linear-gradient(135deg, #0ea5e9, #6366f1);
  }
  .glass-demo--dark {
    background: radial-gradient(circle at 18% 20%, #1e90ff 0%, transparent 50%),
      radial-gradient(circle at 82% 82%, #00c9a7 0%, transparent 52%), #0b1220;
  }
  .glass-demo--dark .demo-panel__title {
    color: #ffffff;
  }
  .glass-demo--dark .demo-panel__text {
    color: rgb(255 255 255 / 72%);
  }
</style>

<!-- EffectSection / EffectItem 用 h() 渲染，scoped 样式不会挂到这些元素上，需放全局块，否则 grid 不生效（一行一个） -->
<style lang="less">
  .effect-section {
    margin-bottom: 24px;
  }
  .effect-section__title {
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .effect-section__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px;
  }
  .effect-item {
    padding: 10px;
    border: 1px solid var(--border-1, #e5e7eb);
    border-radius: 12px;
    background: #ffffff;
  }
  .effect-item__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
  .effect-item__code {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    padding: 0 6px;
    min-width: 28px;
    height: 20px;
    font-size: 12px;
    font-weight: 600;
    border-radius: 4px;
    color: #ffffff;
    background: var(--primary-8);
  }
  .effect-item__name {
    font-size: 13px;
    color: var(--text-n2);
  }
  .effect-item__demo {
    overflow: hidden;
  }
</style>
