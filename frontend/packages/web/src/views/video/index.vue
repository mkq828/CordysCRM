<template>
  <div class="pf-studio">
    <!-- 页头 + 创作台/生成记录 切换 + 步骤条 -->
    <div class="pf-page-head">
      <div class="pf-page-head-row">
        <div>
          <h1 class="pf-page-title">{{ t('video.title') }}</h1>
          <p class="pf-muted" style="margin-top: 0.35rem">{{ t('video.desc') }}</p>
        </div>
        <div class="pf-toolbar">
          <div class="pf-pill-tabs">
            <button type="button" class="pf-pill" :class="{ active: view === 'studio' }" @click="view = 'studio'">
              {{ t('video.studioTab') }}
            </button>
            <button type="button" class="pf-pill" :class="{ active: view === 'history' }" @click="view = 'history'">
              {{ t('video.historyTab') }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="view === 'studio'" class="pf-stepper">
        <template v-for="(s, i) in steps" :key="s.key">
          <div
            class="pf-step"
            :class="{
              done: i < currentStep,
              active: i === currentStep,
              clickable: i !== currentStep && i <= maxReachedStep,
              disabled: i > maxReachedStep,
            }"
            :role="i <= maxReachedStep ? 'button' : undefined"
            @click="goToStep(i)"
          >
            <span class="pf-step-dot">{{ i < currentStep ? '✓' : i + 1 }}</span>
            <span>{{ s.label }}</span>
          </div>
          <div v-if="i < steps.length - 1" class="pf-step-line" :class="{ done: i < currentStep }" />
        </template>
      </div>
    </div>

    <!-- 生成记录 -->
    <section v-if="view === 'history'" class="pf-history">
      <div class="pf-history-head">
        <div>
          <h1>{{ t('video.taskList') }}</h1>
          <p>{{ t('video.noTask') }}</p>
        </div>
        <button type="button" class="pf-btn pf-btn-lime pf-btn-sm" @click="resetStudio">
          + {{ t('video.regenerate') }}
        </button>
      </div>
      <n-empty v-if="!tasks.length && !loading" :description="t('video.noTask')" class="mt-[24px]" />
      <n-data-table
        v-else
        :columns="columns"
        :data="tasks"
        :row-key="(row: AiVideoTask) => row.id"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </section>

    <!-- 创作台 -->
    <template v-else>
      <!-- 第 1 步：选题（模板 + 内容 + 创作摘要，1:1 复刻 printfilm） -->
      <div v-if="currentStep === 0" class="pf-create">
        <aside class="pf-create-col">
          <h3>{{ t('video.selectTemplate') }}</h3>
          <div class="pf-search">
            <input v-model="templateSearch" type="text" :placeholder="t('video.searchTemplate')" />
          </div>
          <div class="pf-pill-tabs" style="margin: 0.5rem 0 0">
            <button
              v-for="c in visibleCategories"
              :key="c"
              type="button"
              class="pf-pill"
              :class="{ active: category === c }"
              @click="category = c"
            >
              {{ c }}
            </button>
          </div>
          <div class="pf-tpl-list" style="margin-top: 0.75rem">
            <button
              v-for="tpl in filteredTemplates"
              :key="tpl.templateKey"
              type="button"
              class="pf-tpl-mini"
              :class="{ selected: form.templateKey === tpl.templateKey }"
              @click="form.templateKey = tpl.templateKey"
            >
              <img :src="tpl.cover" :alt="tpl.name" />
              <div class="min-w-0">
                <strong>{{ tpl.name }}</strong>
                <span>{{ tpl.ratio }} · {{ tpl.category[0] || '通用' }}</span>
              </div>
            </button>
            <n-empty v-if="!filteredTemplates.length" :description="t('video.noTemplate')" class="py-[24px]" />
          </div>
        </aside>

        <section class="pf-create-col">
          <h3>{{ t('video.inputContent') }}</h3>
          <div class="pf-input-tabs">
            <button
              v-for="tab in inputTabs"
              :key="tab.key"
              type="button"
              class="pf-pill"
              :class="{ 'lime active': inputMode === tab.key }"
              @click="inputMode = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <label class="pf-field">
            <span class="pf-field-label">{{ t('video.projectName') }}</span>
            <input
              v-model="form.projectName"
              class="pf-field-input"
              :placeholder="t('video.projectNamePlaceholder')"
              @input="titleTouched = true"
              @blur="autoFillProjectName"
            />
          </label>

          <div class="pf-textarea-wrap">
            <div class="pf-textarea-toolbar">
              <button
                type="button"
                class="pf-btn pf-btn-ai pf-btn-sm pf-btn-icon"
                :disabled="aiBusy || generating"
                @click="aiExpand"
              >
                <span aria-hidden>✨</span>
                {{
                  aiBusy
                    ? t('video.aiGenerating')
                    : inputMode === 'script'
                    ? t('video.aiExpandScript')
                    : t('video.aiGenerateTheme')
                }}
              </button>
              <span class="pf-muted" style="font-size: 0.75rem">
                {{ inputMode === 'script' ? t('video.aiScriptHint') : t('video.aiThemeHint') }}
              </span>
            </div>
            <textarea
              v-model="form.sourceText"
              rows="6"
              :placeholder="inputMode === 'script' ? t('video.scriptPlaceholder') : t('video.themePlaceholder')"
              @input="onSourceInput"
            />
            <span v-if="inputMode === 'theme'" class="pf-char-count"
              >{{ form.sourceText.length }}/{{ THEME_MAX_LEN }}</span
            >
            <span v-else class="pf-char-count">{{ form.sourceText.length }} 字</span>
          </div>

          <label class="pf-field">
            <span class="pf-field-label">{{ t('video.videoTypeLabel') }}</span>
            <div class="pf-pill-tabs">
              <button
                v-for="opt in videoTypeOptions"
                :key="opt.value"
                type="button"
                class="pf-pill"
                :class="{ 'lime active': form.videoType === opt.value, 'disabled': opt.disabled }"
                :disabled="opt.disabled"
                @click="form.videoType = opt.value"
              >
                {{ opt.label
                }}<span v-if="opt.disabled" class="pf-muted"> · {{ t('video.videoType.comingSoon') }}</span>
              </button>
            </div>
            <span class="pf-muted" style="display: block; margin-top: 0.25rem; font-size: 0.75rem">
              {{ videoTypeOptions.find((o) => o.value === form.videoType)?.hint }}
            </span>
          </label>

          <div class="pf-inspire">
            <div class="pf-inspire-head">
              <strong>{{ t('video.inspiration') }}</strong>
              <button type="button" class="pf-btn pf-btn-ghost pf-btn-sm pf-btn-icon" @click="shuffleInspirations">
                <span aria-hidden>↻</span>
                {{ t('video.shuffle') }}
              </button>
            </div>
            <div class="pf-chips">
              <button
                v-for="item in inspirations"
                :key="item.title"
                type="button"
                class="pf-chip"
                :title="inputMode === 'script' ? item.script.slice(0, 80) : item.theme"
                @click="applyInspiration(item)"
              >
                {{ item.title }}
              </button>
            </div>
            <p class="pf-muted" style="margin: 0.55rem 0 0; font-size: 0.78rem">
              {{ inputMode === 'script' ? t('video.inspirationHintScript') : t('video.inspirationHint') }}
            </p>
          </div>

          <div class="pf-hint" style="margin-top: 1rem">{{ t('video.summaryHint') }}</div>
          <p v-if="aiError" class="pf-error">{{ aiError }}</p>
        </section>

        <aside class="pf-create-col">
          <h3>{{ t('video.summary') }}</h3>
          <template v-if="selectedTemplate">
            <div style="margin-bottom: 0.85rem">
              <img
                :src="selectedTemplate.cover"
                :alt="selectedTemplate.name"
                style="width: 100%; border-radius: 12px; aspect-ratio: 16/9; object-fit: cover"
              />
              <strong style="display: block; margin-top: 0.5rem">{{ selectedTemplate.name }}</strong>
              <p class="pf-muted" style="margin: 0.25rem 0 0; font-size: 0.85rem">{{ selectedTemplate.description }}</p>
            </div>
          </template>
          <p v-else class="pf-muted">{{ t('video.plzSelectTemplate') }}</p>
          <div class="pf-summary-row">
            <span>{{ t('video.summaryName') }}</span>
            <span class="truncate">{{ form.projectName.trim() || '未命名作品' }}</span>
          </div>
          <div class="pf-summary-row">
            <span>{{ t('video.summaryMode') }}</span>
            <span>{{ outputModeLabel }}</span>
          </div>
          <div class="pf-summary-row">
            <span>{{ t('video.summaryDuration') }}</span>
            <span>{{ t('video.durationValue') }}</span>
          </div>
          <div class="pf-summary-row">
            <span>{{ t('video.summaryLanguage') }}</span>
            <span>{{ t('video.summaryLanguageValue') }}</span>
          </div>
          <div class="pf-summary-row">
            <span>{{ t('video.summaryInput') }}</span>
            <span>{{ inputMode === 'script' ? t('video.inputTabs.script') : t('video.inputTabs.theme') }}</span>
          </div>
          <button
            type="button"
            class="pf-btn pf-btn-lime pf-btn-block pf-btn-lg pf-btn-icon"
            style="margin-top: 1.25rem"
            :disabled="generating || aiBusy || !form.templateKey || !form.sourceText.trim()"
            @click="goStyle"
          >
            {{ generating ? t('video.creating') : t('video.nextStyle') }}
            <span v-if="!generating" aria-hidden>→</span>
          </button>
          <button
            type="button"
            class="pf-btn pf-btn-ghost pf-btn-block pf-btn-sm pf-btn-icon"
            style="margin-top: 0.55rem"
            :disabled="aiBusy || generating"
            @click="aiExpand"
          >
            <span aria-hidden>✨</span>
            {{ aiBusy ? t('video.aiGenerating') : t('video.aiNotComplete') }}
          </button>
          <p class="pf-muted" style="margin-top: 0.5rem; font-size: 0.78rem">{{ t('video.styleCarriedHint') }}</p>
        </aside>
      </div>

      <!-- 第 2 步：风格（1:1 复刻 printfilm 风格配置：项目信息 / 画面风格 / 配音音色 / 成片方式 / 图片模型 / 输出比例） -->
      <div v-else-if="currentStep === 1" class="pf-style-layout">
        <aside class="pf-create-col">
          <h3>{{ t('video.projectInfo') }}</h3>
          <template v-if="selectedTemplate">
            <div class="pf-style-side-thumb" :class="{ portrait: selectedTemplate.ratio === '9:16' }">
              <img :src="selectedTemplate.cover" :alt="selectedTemplate.name" />
            </div>
            <p style="margin: 0.5rem 0 0; font-weight: 600">{{ selectedTemplate.name }}</p>
            <div class="pf-tags">
              <span v-for="tag in selectedTemplate.category" :key="tag">{{ tag }}</span>
            </div>
          </template>
          <ul class="pf-meta-list pf-meta-left" style="margin-top: 0.85rem">
            <li>
              <span>{{ t('video.projectName') }}</span>
              <span class="pf-meta-val">{{ form.projectName.trim() || '—' }}</span>
            </li>
            <li>
              <span>{{ t('video.narrationContent') }}</span>
              <span class="pf-meta-val">{{ form.sourceText.slice(0, 60) || '—' }}</span>
            </li>
            <li>
              <span>{{ t('video.summaryDuration') }}</span>
              <span class="pf-meta-val">{{ t('video.durationValue') }}</span>
            </li>
            <li>
              <span>{{ t('video.shotCount') }}</span>
              <span class="pf-meta-val">{{ t('video.aiAuto') }}</span>
            </li>
          </ul>
          <button type="button" class="pf-btn pf-btn-ghost pf-btn-block pf-btn-sm" disabled>
            {{ t('video.previewTemplate') }}
          </button>
        </aside>

        <section class="pf-create-col">
          <div class="pf-style-block">
            <h3>{{ t('video.artStyle') }}</h3>
            <p class="pf-muted" style="margin: 0 0 0.65rem; font-size: 0.78rem">{{ t('video.styleLockedHint') }}</p>
            <div
              v-if="selectedTemplate"
              class="pf-style-opt selected"
              :class="{ portrait: selectedTemplate.ratio === '9:16' }"
              style="max-width: 280px; text-align: left"
            >
              <span class="pf-style-opt-media">
                <img :src="selectedTemplate.cover" alt="" />
                <span v-if="selectedTemplate.ratio" class="pf-style-opt-ratio">{{ selectedTemplate.ratio }}</span>
              </span>
              <div class="cap">{{ selectedTemplate.name }}</div>
              <div class="cap-sub">{{ t('video.templateStyle') }}</div>
            </div>
            <div class="pf-field" style="margin-top: 0.75rem">
              <div class="pf-field-label" style="display: flex; justify-content: space-between; align-items: center">
                <span style="display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap">
                  {{ t('video.stylePromptLabel') }}
                  <span style="font-size: 0.75rem; color: #f97316"> 💡 {{ t('video.stylePromptConsultHint') }} </span>
                </span>
                <button
                  type="button"
                  class="pf-btn pf-btn-ai pf-btn-sm pf-btn-icon"
                  :disabled="styleAiBusy"
                  @click="aiGenStylePrompt"
                >
                  <span aria-hidden>✨</span>
                  {{
                    styleAiBusy
                      ? t('video.aiGenerating')
                      : form.stylePrompt
                      ? t('video.aiRegenStyle')
                      : t('video.aiGenStyle')
                  }}
                </button>
              </div>
              <textarea
                v-model="form.stylePrompt"
                class="pf-field-input"
                rows="2"
                style="min-height: 64px; resize: vertical"
                :placeholder="t('video.stylePromptPlaceholder')"
              />
              <p class="pf-muted" style="margin: 0.35rem 0 0; font-size: 0.75rem">{{ t('video.stylePromptHint') }}</p>
            </div>
          </div>

          <div class="pf-style-block">
            <h3>
              {{ t('video.voiceSection') }}
              <button type="button" class="pf-btn pf-btn-ghost pf-btn-sm" disabled>{{ t('video.moreVoices') }}</button>
            </h3>
            <div class="pf-voice-row">
              <div
                v-for="v in voices"
                :key="v.value"
                class="pf-voice-card"
                :class="{ selected: form.voice === v.value }"
                role="button"
                tabindex="0"
                @click="form.voice = v.value"
              >
                <strong class="pf-voice-name">{{ v.label }}</strong>
                <span class="pf-voice-meta">{{ voiceGenderLabel(v) }}</span>
                <button
                  type="button"
                  class="pf-btn pf-btn-sm pf-btn-icon pf-btn-ghost pf-voice-preview"
                  :disabled="voicePreviewBusy === v.value"
                  @click="previewVoice(v)"
                >
                  {{
                    playingVoice === v.value
                      ? t('video.stopAudition')
                      : voicePreviewBusy === v.value
                      ? t('video.aiGenerating')
                      : t('video.audition')
                  }}
                </button>
              </div>
            </div>
            <p v-if="selectedVoiceLabel" class="pf-muted" style="margin: 0.55rem 0 0; font-size: 0.78rem">
              {{ t('video.voiceCurrentHint', { label: selectedVoiceLabel }) }}
            </p>
          </div>

          <div class="pf-style-block">
            <h3>{{ t('video.pipelineMode') }}</h3>
            <p class="pf-muted" style="margin: 0 0 0.65rem; font-size: 0.78rem">{{ t('video.pipelineModeHint') }}</p>
            <div class="pf-style-grid" style="grid-template-columns: repeat(2, 1fr)">
              <button
                type="button"
                class="pf-style-opt"
                :class="{ selected: mode === 'video' }"
                @click="mode = 'video'"
              >
                <img class="pf-mode-thumb" :src="modeVideoThumb" alt="" />
                <div class="cap">{{ t('video.modeVideo') }}</div>
                <div class="cap-sub">{{ t('video.modeVideoDesc') }}</div>
              </button>
              <button
                type="button"
                class="pf-style-opt"
                :class="{ selected: mode === 'image_text' }"
                @click="mode = 'image_text'"
              >
                <img class="pf-mode-thumb" :src="modeImageTextThumb" alt="" />
                <div class="cap">{{ t('video.modeImageText') }}</div>
                <div class="cap-sub">{{ t('video.modeImageTextDesc') }}</div>
              </button>
            </div>
          </div>

          <div class="pf-style-block">
            <h3>{{ t('video.imageModel') }}</h3>
            <p class="pf-muted" style="margin: 0 0 0.65rem; font-size: 0.78rem">{{ t('video.imageModelHint') }}</p>
            <div v-if="imageModels.length" class="pf-model-grid">
              <button
                v-for="m in imageModels"
                :key="m.id"
                type="button"
                class="pf-model-opt"
                :class="{ selected: selectedImageModelId === m.id }"
                @click="selectedImageModelId = m.id"
              >
                <div class="pf-model-opt-title">
                  <span>{{ m.label }}</span>
                  <span v-if="m.recommended" class="pf-model-badge">{{ t('video.recommended') }}</span>
                </div>
                <div class="pf-model-opt-desc">{{ m.description }}</div>
              </button>
            </div>
            <p v-else class="pf-muted" style="font-size: 0.8rem">{{ t('video.modeComingSoon') }}</p>
          </div>

          <div class="pf-style-block">
            <h3>{{ t('video.videoModel') }}</h3>
            <p class="pf-muted" style="margin: 0 0 0.65rem; font-size: 0.78rem">{{ t('video.videoModelHint') }}</p>
            <div v-if="videoModels.length" class="pf-model-grid">
              <button
                v-for="m in videoModels"
                :key="m.id"
                type="button"
                class="pf-model-opt"
                :class="{ selected: mode === 'video' && selectedVideoModelId === m.id }"
                :disabled="mode !== 'video'"
                @click="selectedVideoModelId = m.id"
              >
                <div class="pf-model-opt-title">
                  <span>{{ m.label }}</span>
                  <span v-if="m.recommended" class="pf-model-badge">{{ t('video.recommended') }}</span>
                </div>
                <div class="pf-model-opt-desc">{{ m.description }}</div>
              </button>
            </div>
            <p v-else class="pf-muted" style="font-size: 0.8rem">{{ t('video.modeComingSoon') }}</p>
          </div>

          <div class="pf-style-block">
            <h3>{{ t('video.ratioSection') }}</h3>
            <div class="pf-ratio-row">
              <button
                v-for="r in RATIOS"
                :key="r.id"
                type="button"
                class="pf-ratio"
                :class="{ selected: selectedRatio === r.id }"
                @click="selectedRatio = r.id"
              >
                <div class="box" :style="{ width: `${r.w}px`, height: `${r.h}px` }" />
                {{ r.label }}
              </button>
            </div>
            <p class="pf-muted" style="margin-top: 0.5rem; font-size: 0.8rem">{{ t('video.ratioHint') }}</p>
          </div>
        </section>

        <aside class="pf-create-col">
          <h3>{{ t('video.realtimePreview') }}</h3>
          <div
            class="pf-editor-preview"
            :class="{ portrait: selectedTemplate?.ratio === '9:16' }"
            style="margin-bottom: 0.85rem"
          >
            <img v-if="selectedTemplate" :src="selectedTemplate.cover" alt="" />
            <span v-else class="empty">{{ t('video.selectTemplate') }}</span>
          </div>
          <p class="pf-muted" style="font-size: 0.8rem">{{ t('video.previewPlaceholderNote') }}</p>
          <h3 style="margin-top: 1rem">{{ t('video.configOverview') }}</h3>
          <ul class="pf-meta-list">
            <li>
              <span>{{ t('video.styleItem') }}</span>
              <span>{{ selectedTemplate?.name || '—' }}</span>
            </li>
            <li>
              <span>{{ t('video.characterItem') }}</span>
              <span>{{ t('video.aiDecideCharacter') }}</span>
            </li>
            <li>
              <span>{{ t('video.dubItem') }}</span>
              <span>{{ selectedVoiceLabel || t('video.topicEmpty') }}</span>
            </li>
            <li>
              <span>{{ t('video.ratioItem') }}</span>
              <span>{{ selectedRatio }}</span>
            </li>
            <li>
              <span>{{ t('video.modeItem') }}</span>
              <span>{{ mode === 'video' ? t('video.modeVideo') : t('video.modeImageText') }}</span>
            </li>
          </ul>
          <div style="margin-top: 1rem">
            <button
              type="button"
              class="pf-btn pf-btn-ghost pf-btn-block pf-btn-sm"
              style="margin-bottom: 0.5rem"
              :disabled="!selectedVoice || voicePreviewBusy === form.voice"
              @click="selectedVoice && previewVoice(selectedVoice)"
            >
              {{ t('video.auditionVoice', { label: selectedVoiceLabel }) }}
            </button>
            <button
              v-if="hasActiveTask"
              type="button"
              class="pf-btn pf-btn-lime pf-btn-lg pf-btn-icon pf-btn-block"
              @click="returnToBoard"
            >
              {{ t('video.backToBoard') }}
              <span aria-hidden>→</span>
            </button>
            <button
              v-else
              type="button"
              class="pf-btn pf-btn-lime pf-btn-lg pf-btn-icon pf-btn-block"
              :disabled="!canGenerate || generating"
              @click="handleGenerate"
            >
              {{ generating ? t('video.generating') : t('video.generateStoryboard') }}
              <span v-if="!generating" aria-hidden>→</span>
            </button>
          </div>
          <p class="pf-muted" style="margin-top: 0.5rem; font-size: 0.78rem">{{ t('video.generateFirstHint') }}</p>
        </aside>
      </div>

      <!-- 第 3/4/5/6 步：确认分镜 / 画面与配音 / 镜头视频 / 合成预览（同一分镜台，1:1 复刻 printfilm 分镜工作台） -->
      <div v-else class="pf-board">
        <aside class="pf-create-col">
          <h3>{{ t('video.projectSettings') }}</h3>
          <img
            v-if="taskCoverUrl"
            :src="taskCoverUrl"
            alt=""
            style="width: 100%; border-radius: 12px; aspect-ratio: 16/10; object-fit: cover"
          />
          <div
            v-else
            style="
              display: grid;
              width: 100%;
              border-radius: 12px;
              color: var(--pf-muted);
              background: #e8eaee;
              aspect-ratio: 16/10;
              place-items: center;
            "
          >
            {{ t('video.selectTemplate') }}
          </div>
          <input
            ref="coverInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            style="display: none"
            @change="onCoverChange"
          />
          <input
            ref="shotImageInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            style="display: none"
            @change="onShotImageChange"
          />
          <div class="pf-side-actions">
            <button
              type="button"
              class="pf-btn pf-btn-ghost pf-btn-sm pf-btn-icon"
              :disabled="coverBusy || generating"
              @click="coverInputRef?.click()"
            >
              {{ t('video.changeCover') }}
            </button>
            <button
              type="button"
              class="pf-btn pf-btn-ghost pf-btn-sm pf-btn-icon"
              :disabled="coverBusy || generating || !hasFirstShot"
              :title="t('video.useFirstCoverTip')"
              @click="useFirstShotCover"
            >
              {{ t('video.useFirstCover') }}
            </button>
          </div>
          <ul class="pf-meta-list" style="margin-top: 0.85rem">
            <li>
              <span>{{ t('video.projectName') }}</span>
              <span>{{ activeTask?.title || '—' }}</span>
            </li>
            <li>
              <span>{{ t('video.colStatus') }}</span>
              <span>{{ activeTask?.status ? t(`video.status.${activeTask.status}`) : '—' }}</span>
            </li>
            <li>
              <span>{{ t('video.colProgress') }}</span>
              <span>{{ activeTask?.progress ?? 0 }}%</span>
            </li>
            <li>
              <span>{{ t('video.summaryDuration') }}</span>
              <span>{{ t('video.durationValue') }}</span>
            </li>
            <li>
              <span>{{ t('video.ratioSection') }}</span>
              <span>{{ selectedRatio }}</span>
            </li>
            <li>
              <span>{{ t('video.modeItem') }}</span>
              <span>{{ mode === 'video' ? t('video.modeVideo') : t('video.modeImageText') }}</span>
            </li>
            <li>
              <span>{{ t('video.styleItem') }}</span>
              <span>{{ selectedTemplate?.name || form.templateKey }}</span>
            </li>
          </ul>
          <button type="button" class="pf-btn pf-btn-ghost pf-btn-block pf-btn-sm" @click="backToEdit">
            {{ t('video.editProjectSettings') }}
          </button>
          <div class="pf-prompt-panel">
            <h4>{{ t('video.builtinPrompts') }}</h4>
            <p class="pf-muted" style="margin: 0 0 0.45rem; font-size: 0.72rem">{{ t('video.builtinPromptHint') }}</p>
            <button
              type="button"
              class="pf-prompt-chip pf-prompt-chip-btn"
              :class="{ disabled: activeTask?.status !== 'SCRIPT_READY' }"
              :disabled="activeTask?.status !== 'SCRIPT_READY'"
              @click="openPromptEdit"
            >
              <strong>{{ t('video.styleItem') }}</strong>
              <span>{{
                activeTask?.stylePrompt || selectedTemplate?.description || selectedTemplate?.name || '—'
              }}</span>
            </button>
            <button
              type="button"
              class="pf-prompt-chip pf-prompt-chip-btn"
              :class="{ disabled: activeTask?.status !== 'SCRIPT_READY' }"
              :disabled="activeTask?.status !== 'SCRIPT_READY'"
              @click="openPromptEdit"
            >
              <strong>{{ t('video.characterItem') }}</strong>
              <span>{{ activeTask?.characterPrompt || t('video.aiDecideCharacter') }}</span>
            </button>
            <button
              type="button"
              class="pf-prompt-chip pf-prompt-chip-btn"
              :class="{ disabled: activeTask?.status !== 'SCRIPT_READY' }"
              :disabled="activeTask?.status !== 'SCRIPT_READY'"
              @click="openPromptEdit"
            >
              <strong>{{ t('video.extraPromptChip') }}</strong>
              <span>{{ activeTask?.extraPrompt || form.extraText?.trim() || t('video.promptEmpty') }}</span>
            </button>
          </div>
          <p class="pf-muted" style="margin-top: 0.75rem; font-size: 0.75rem">{{ t('video.aiDisclaimer') }}</p>
        </aside>

        <div class="pf-board-main">
          <p class="pf-muted" style="margin: 0 0 0.75rem; font-size: 0.78rem">{{ phaseHint }}</p>
          <div class="pf-outline-grid">
            <article class="pf-create-col">
              <h3>{{ t('video.aiOutline') }}</h3>
              <p style="margin: 0; font-size: 0.9rem; line-height: 1.65">
                {{ activeTask?.sourceText || activeTask?.title || '—' }}
              </p>
              <div class="pf-tags">
                <span>{{ t('video.coreTheme') }}</span>
                <span>{{ inputMode === 'script' ? t('video.sourceScript') : t('video.sourceTheme') }}</span>
              </div>
            </article>
            <article class="pf-create-col pf-structure-col">
              <h3>{{ t('video.structureSummary') }}</h3>
              <ul v-if="structure.length" class="pf-meta-list">
                <li v-for="s in structure" :key="s.label">
                  <span>{{ s.label }}</span>
                  <span style="max-width: 60%; text-align: right">{{ s.text.slice(0, 36) }}</span>
                </li>
              </ul>
              <div v-else class="pf-structure-empty">
                <ShimmerText v-if="boardGenerating" size="15px">{{ t('video.structureGenerating') }}</ShimmerText>
                <span v-else class="pf-muted">{{ t('video.waitStoryboard') }}</span>
              </div>
            </article>
          </div>

          <section class="pf-shot-card">
            <div class="pf-shot-card-head">
              <h3>{{ t('video.storyboardListCount', { count: storyboard.length }) }}</h3>
            </div>
            <div
              class="pf-consult-hint"
              style="
                display: flex;
                align-items: center;
                margin: 0 0 0.85rem;
                padding: 0.5rem 0.75rem;
                font-size: 0.8rem;
                border: 1px solid rgb(249 115 22 / 35%);
                border-radius: 10px;
                color: #f97316;
                background: rgb(249 115 22 / 10%);
                gap: 0.4rem;
                line-height: 1.5;
              "
            >
              <span style="flex-shrink: 0">💡</span>
              <span>{{ t('video.storyboardConsultHint') }}</span>
            </div>
            <template v-if="!storyboard.length">
              <p v-if="boardGenerating" style="margin: 1.5rem 0; text-align: center">
                <ShimmerText size="14px">{{ t('video.splitting') }}</ShimmerText>
              </p>
              <p v-else class="pf-muted" style="margin: 1.5rem 0; text-align: center">
                {{ t('video.noStoryboardHint') }}
              </p>
            </template>
            <div v-else class="pf-shot-table-wrap">
              <table class="pf-shot-table">
                <thead>
                  <tr>
                    <th class="col-no">{{ t('video.colSceneNo') }}</th>
                    <th class="col-thumb">{{ t('video.colScene') }}</th>
                    <th class="col-narr">{{ t('video.colNarration') }}</th>
                    <th class="col-seg">{{ t('video.colSegment') }}</th>
                    <th class="col-dur">{{ t('video.colDuration') }}</th>
                    <th class="col-status">{{ t('video.colStatusShot') }}</th>
                    <th class="col-ops">{{ t('video.colAction') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(seg, i) in storyboard"
                    :key="i"
                    class="pf-shot-row-enter"
                    :style="{ animationDelay: `${i * 0.15}s` }"
                  >
                    <td class="col-no">{{ String(i + 1).padStart(2, '0') }}</td>
                    <td class="col-thumb">
                      <div class="pf-shot-thumb-wrap">
                        <button
                          type="button"
                          class="pf-shot-thumb-btn"
                          :disabled="!shotEditable"
                          :title="shotEditable ? t('video.editShotTip') : ''"
                          @click="shotEditable && openShotEdit(i)"
                        >
                          <video
                            v-if="seg.videoAttachmentId"
                            class="pf-shot-thumb"
                            :src="previewUrl(seg.videoAttachmentId)"
                            muted
                            loop
                            playsinline
                          />
                          <img
                            v-else-if="seg.imageAttachmentId"
                            class="pf-shot-thumb"
                            :src="previewUrl(seg.imageAttachmentId)"
                            :alt="`scene-${i + 1}`"
                          />
                          <div v-else class="pf-shot-thumb empty">{{ shotStatusLabel(seg) }}</div>
                        </button>
                        <button
                          v-if="seg.imageAttachmentId || seg.videoAttachmentId"
                          type="button"
                          class="pf-shot-thumb-overlay"
                          :title="t('video.preview')"
                          @click="openShotPreview(i)"
                        >
                          <svg
                            viewBox="0 0 24 24"
                            width="18"
                            height="18"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                            <circle cx="12" cy="12" r="3" />
                          </svg>
                        </button>
                      </div>
                    </td>
                    <td class="col-narr">
                      <button
                        type="button"
                        class="pf-shot-narration pf-shot-editable"
                        :disabled="activeTask?.status !== 'SCRIPT_READY'"
                        :title="t('video.editNarrationTip')"
                        @click="activeTask?.status === 'SCRIPT_READY' && openShotEdit(i)"
                      >
                        <span class="title">{{ t('video.colNarration') }} {{ i + 1 }}</span>
                        <span class="line">{{ seg.text || '—' }}</span>
                      </button>
                    </td>
                    <td class="col-seg">
                      <button
                        type="button"
                        class="pf-shot-desc pf-shot-editable"
                        :disabled="!imageEditable"
                        :title="imageEditable ? t('video.editSegmentTip') : ''"
                        @click="imageEditable && openShotEdit(i)"
                      >
                        {{ seg.image || '—' }}
                      </button>
                    </td>
                    <td class="col-dur">
                      <span class="pf-shot-duration">{{ shotDuration(seg) }}s</span>
                    </td>
                    <td class="col-status">
                      <span class="pf-shot-status" :class="{ warn: !shotDone(seg) }">
                        <span v-if="shotDone(seg)" class="mark">✓</span>
                        <ShimmerText v-if="isShotGenerating(seg)" size="12px">{{ shotStatusLabel(seg) }}</ShimmerText>
                        <template v-else>{{ shotStatusLabel(seg) }}</template>
                      </span>
                    </td>
                    <td class="col-ops">
                      <div class="pf-shot-ops">
                        <button
                          type="button"
                          class="op"
                          :disabled="!shotEditable || regenShotBusy === i"
                          @click="openShotEdit(i)"
                        >
                          {{ t('video.edit') }}
                        </button>
                        <button
                          v-if="imageEditable"
                          type="button"
                          class="op"
                          :disabled="!seg.imageAttachmentId || regenShotBusy !== null"
                          :title="seg.imageAttachmentId ? t('video.regenImageTip') : ''"
                          @click="regenShotImage(i)"
                        >
                          {{ regenShotBusy === i ? t('video.generatingShot') : t('video.regenImage') }}
                        </button>
                        <button
                          v-if="assetsReady"
                          type="button"
                          class="op"
                          :disabled="replaceImageBusy !== null"
                          :title="t('video.replaceImageTip')"
                          @click="openReplaceImage(i)"
                        >
                          {{ replaceImageBusy === i ? t('video.generatingShot') : t('video.replaceImage') }}
                        </button>
                        <button
                          v-if="videosReady"
                          type="button"
                          class="op"
                          :disabled="regenVideoBusy !== null"
                          :title="t('video.regenVideoTip')"
                          @click="regenShotVideo(i)"
                        >
                          {{ regenVideoBusy === i ? t('video.generatingShot') : t('video.regenVideo') }}
                        </button>
                        <button
                          v-if="assetsReady"
                          type="button"
                          class="op"
                          :disabled="!seg.text || shotVoiceBusy !== null"
                          @click="previewShotVoice(i)"
                        >
                          {{ playingShot === i ? t('video.stopAudition') : t('video.audition') }}
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pf-shot-footer">
              <span class="pf-muted" style="font-size: 0.82rem">
                {{ t('video.totalDuration') }}: {{ t('video.durationValue') }} | {{ t('video.scenesCount') }}:
                {{ storyboard.length }} 个 | {{ t('video.audioSegments') }}: {{ audioReady ? 1 : 0 }} 段 |
                {{ t('video.resolution') }}: {{ t('video.resolutionPreview') }}
              </span>
              <div class="pf-toolbar">
                <button
                  type="button"
                  class="pf-btn pf-btn-ghost pf-btn-sm"
                  :disabled="!storyboard.length"
                  @click="downloadStoryboardCsv"
                >
                  {{ t('video.exportStoryboard') }}
                </button>
                <button
                  type="button"
                  class="pf-btn-text"
                  :disabled="assetsBusy || composeBusy"
                  @click="handleDeleteProject"
                >
                  {{ t('video.deleteProject') }}
                </button>
              </div>
            </div>
          </section>
        </div>

        <aside class="pf-create-col">
          <h3>{{ t('video.progressTitle') }}</h3>
          <ul class="pf-progress-list">
            <li v-for="stage in stageList" :key="stage.key">
              <span>{{ stage.label }}</span>
              <span>
                <span v-if="stage.done" class="pf-check">✓</span>
                <span v-else-if="stage.run" class="pf-muted">{{ activeTask?.progress ?? 0 }}%</span>
                <span v-else class="pf-muted">…</span>
              </span>
            </li>
          </ul>
          <div class="pf-meter" style="margin-top: 1rem"><i :style="{ width: `${activeTask?.progress ?? 0}%` }" /></div>
          <p class="pf-muted" style="font-size: 0.82rem">
            <ShimmerText v-if="activeTask && IN_PROGRESS.includes(activeTask.status)" size="14px">
              {{ t(`video.status.${activeTask.status}`) }}
            </ShimmerText>
            <template v-else>
              {{ activeTask?.status ? t(`video.status.${activeTask.status}`) : t('video.generating') }}
            </template>
          </p>
          <div
            v-if="activeTask?.status === 'FAILED' && activeTask.errorMsg"
            class="pf-error"
            style="margin-top: 0.5rem"
          >
            {{ activeTask.errorMsg }}
          </div>
          <div
            v-if="activeTask?.status === 'SCRIPT_READY'"
            style="display: flex; flex-direction: column; gap: 0.5rem; margin-top: 1rem"
          >
            <button
              type="button"
              class="pf-btn pf-btn-lime pf-btn-block pf-btn-lg pf-btn-icon"
              :disabled="assetsBusy"
              @click="confirmAssets"
            >
              {{ assetsBusy ? t('video.generating') : t('video.confirmAssetsBtn') }}
              <span v-if="!assetsBusy" aria-hidden>→</span>
            </button>
            <p class="pf-muted" style="font-size: 0.75rem">{{ t('video.confirmAssetsHint') }}</p>
          </div>
          <div
            v-else-if="activeTask?.status === 'ASSETS_READY'"
            style="display: flex; flex-direction: column; gap: 0.5rem; margin-top: 1rem"
          >
            <button
              type="button"
              class="pf-btn pf-btn-lime pf-btn-block pf-btn-lg pf-btn-icon"
              :disabled="composeBusy || shotsBusy"
              @click="startComposeOrShots"
            >
              {{
                composeBusy || shotsBusy
                  ? t('video.generating')
                  : isVideoMode
                  ? t('video.generateShotsBtn')
                  : t('video.composeFinalBtn')
              }}
              <span v-if="!composeBusy && !shotsBusy" aria-hidden>→</span>
            </button>
            <p class="pf-muted" style="font-size: 0.75rem">
              {{ isVideoMode ? t('video.generateShotsHint') : t('video.composeHint') }}
            </p>
          </div>
          <div
            v-else-if="activeTask?.status === 'VIDEOS_READY'"
            style="display: flex; flex-direction: column; gap: 0.5rem; margin-top: 1rem"
          >
            <button
              type="button"
              class="pf-btn pf-btn-lime pf-btn-block pf-btn-lg pf-btn-icon"
              :disabled="composeBusy"
              @click="composeVideo"
            >
              {{ composeBusy ? t('video.generating') : t('video.composeFinalBtn') }}
              <span v-if="!composeBusy" aria-hidden>→</span>
            </button>
            <p class="pf-muted" style="font-size: 0.75rem">{{ t('video.composeFinalHint') }}</p>
          </div>
          <div
            v-if="activeTask?.status === 'DONE' && activeTask.attachmentId"
            style="display: flex; flex-direction: column; gap: 0.5rem; margin-top: 1rem"
          >
            <video
              :src="previewUrl(activeTask.attachmentId)"
              controls
              playsinline
              style="width: 100%; border-radius: 12px; background: #000000"
            />
            <button
              type="button"
              class="pf-btn pf-btn-lime pf-btn-block"
              @click="activeTask && handleDownload(activeTask)"
            >
              ↓ {{ t('video.downloadBtn') }}
            </button>
            <button type="button" class="pf-btn pf-btn-ghost" :disabled="copyBusy" @click="handleShowCopyClick">
              {{ copyBusy ? t('video.copyRegenerating') : t('video.copy') }}
            </button>
            <button type="button" class="pf-btn pf-btn-outline pf-btn-block" @click="resetStudio">
              + {{ t('video.regenerate') }}
            </button>
          </div>
          <button
            v-else-if="activeTask?.status === 'FAILED'"
            type="button"
            class="pf-btn pf-btn-lime pf-btn-sm"
            style="margin-top: 0.75rem"
            @click="retryGenerate"
          >
            ↻ {{ t('video.confirmGenerate') }}
          </button>
        </aside>
      </div>
    </template>

    <!-- 配套文案弹窗 -->
    <n-modal v-model:show="copyModalVisible" preset="card" :title="t('video.copyTitle')" class="!w-[640px]">
      <n-tabs v-model:value="activeCopyPlatform" type="line" justify-content="space-evenly">
        <n-tab-pane name="douyin" :tab="t('video.copyDouyin')" />
        <n-tab-pane name="xiaohongshu" :tab="t('video.copyXiaohongshu')" />
        <n-tab-pane name="moments" :tab="t('video.copyMoments')" />
      </n-tabs>
      <template v-if="platformCopy">
        <div v-if="platformCopy.title" class="mt-[12px] flex flex-col gap-[4px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.copyLabelTitle') }}</span>
          <span class="text-[15px] font-semibold text-[var(--text-n1)]">{{ platformCopy.title }}</span>
        </div>
        <div v-if="platformCopy.copy" class="mt-[12px] flex flex-col gap-[4px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.copyBody') }}</span>
          <div
            class="whitespace-pre-wrap rounded-[6px] bg-[rgb(148_163_184_/_8%)] p-[10px] text-[13px] leading-[1.6] text-[var(--text-n2)]"
          >
            {{ platformCopy.copy }}
          </div>
        </div>
        <div v-if="platformCopy.hashtags?.length" class="mt-[12px] flex flex-wrap items-center gap-[6px]">
          <n-tag v-for="tag in platformCopy.hashtags" :key="tag" size="small" type="warning" :bordered="false">
            {{ tag }}
          </n-tag>
        </div>
        <div class="mt-[16px]">
          <button type="button" class="pf-btn pf-btn-lime pf-btn-sm" @click="copyPlatform">{{
            t('video.copyAll')
          }}</button>
        </div>
      </template>
      <n-empty v-else :description="t('video.noCopy')" class="py-[24px]" />
    </n-modal>

    <!-- 分镜画面/视频在线预览弹窗 -->
    <n-modal
      v-model:show="previewVisible"
      preset="card"
      :title="previewMedia?.title || t('video.preview')"
      class="!w-[720px]"
    >
      <div
        v-if="previewMedia"
        class="flex items-center justify-center"
        style="overflow: hidden; border-radius: 8px; background: #000000"
      >
        <video
          v-if="previewMedia.type === 'video'"
          :src="previewMedia.url"
          controls
          autoplay
          playsinline
          style="width: 100%; max-height: 70vh"
        />
        <img
          v-else
          :src="previewMedia.url"
          :alt="previewMedia.title"
          style="width: 100%; max-height: 70vh; object-fit: contain"
        />
      </div>
    </n-modal>

    <!-- 编辑单个分镜弹窗（旁白 + 画面描述） -->
    <n-modal
      v-model:show="shotEditVisible"
      preset="card"
      :title="t('video.editShotTitle', { index: shotEditIndex + 1 })"
      class="!w-[640px]"
    >
      <div class="flex flex-col gap-[14px]">
        <div class="flex flex-col gap-[6px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.colNarration') }}</span>
          <n-input
            v-model:value="shotEditText"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 8 }"
            :disabled="!narrationEditable"
          />
          <span v-if="!narrationEditable" class="text-[12px] text-[var(--text-n3)]">
            {{ t('video.narrationLockedHint') }}
          </span>
        </div>
        <div class="flex flex-col gap-[6px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.colSegment') }}</span>
          <n-input
            v-model:value="shotEditImage"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 8 }"
            :disabled="videosReady"
          />
        </div>
        <div v-if="videosReady" class="flex flex-col gap-[6px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.videoPromptLabel') }}</span>
          <n-input
            v-model:value="shotEditVideoPrompt"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 8 }"
            :placeholder="t('video.videoPromptPlaceholder')"
          />
        </div>
        <div class="mt-[8px] flex justify-end gap-[8px]">
          <n-button @click="shotEditVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="shotSaving" @click="saveShotEdit">{{ t('common.save') }}</n-button>
        </div>
      </div>
    </n-modal>

    <!-- 编辑内置提示词弹窗（风格/角色/额外） -->
    <n-modal v-model:show="promptEditVisible" preset="card" :title="t('video.editPromptsTitle')" class="!w-[680px]">
      <div class="flex flex-col gap-[14px]">
        <div class="flex items-center justify-between gap-[8px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.promptGenHint') }}</span>
          <n-button size="small" :loading="promptGenBusy" @click="genPrompts()">
            {{ promptGenBusy ? t('video.aiGenerating') : t('video.aiRegenStyle') }}
          </n-button>
        </div>
        <div class="flex flex-col gap-[6px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.styleItem') }}</span>
          <n-input
            v-model:value="promptEditStyle"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            :placeholder="t('video.stylePromptPlaceholder')"
          />
        </div>
        <div class="flex flex-col gap-[6px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.characterItem') }}</span>
          <n-input
            v-model:value="promptEditCharacter"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            :placeholder="t('video.characterPromptPlaceholder')"
          />
        </div>
        <div class="flex flex-col gap-[6px]">
          <span class="text-[12px] text-[var(--text-n3)]">{{ t('video.extraPromptChip') }}</span>
          <n-input
            v-model:value="promptEditExtra"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            :placeholder="t('video.extraPromptPlaceholder')"
          />
        </div>
        <div class="mt-[8px] flex justify-end gap-[8px]">
          <n-button @click="promptEditVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="promptSaving" @click="savePromptEdit">{{ t('common.save') }}</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';
  import { NButton, NDataTable, NEmpty, NInput, NModal, NProgress, NTabPane, NTabs, NTag, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import modeImageTextThumb from '@/assets/video-templates/covers/brand_clean.png';
  import modeVideoThumb from '@/assets/video-templates/covers/live_cinematic.png';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiVideoModelOption, AiVideoStoryboardSegment, AiVideoTask, AiVideoVoice } from '@lib/shared/models/ai';

  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';

  import {
    composeAiVideo,
    confirmAiVideoAssets,
    createAiVideoTask,
    deleteAiVideoTask,
    downloadAttachment,
    expandAiVideoContent,
    generateAiVideoShots,
    getAiVideoModels,
    getAiVideoTasks,
    getAiVideoVoices,
    previewAiVideoShotVoice,
    previewAiVideoVoice,
    regenAiVideoShotImage,
    regenAiVideoShotVideo,
    regenerateAiVideoCopy,
    setAiVideoCover,
    setAiVideoFirstShotCover,
    updateAiVideoPrompts,
    updateAiVideoShot,
    uploadAiVideoShotImage,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import './printfilm.css';
  import type { Inspiration } from './templates';
  import { deriveTitle, INSPIRATION_POOL, VIDEO_TEMPLATES, videoTemplateCategories } from './templates';
  import type { DataTableColumns } from 'naive-ui';

  const PAGE_SIZE = 6;

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const route = useRoute();
  const userStore = useUserStore();

  const view = ref<'studio' | 'history'>('studio');
  const step = ref(0);
  // 已完成可达的最大步：随生成进度单调前进；手动回看后不会再次自动前进
  const maxReachedStep = ref(0);

  const form = reactive({
    sourceText: '',
    projectName: '',
    extraText: '',
    stylePrompt: '',
    characterPrompt: '',
    voice: 'Cherry',
    templateKey: '',
    videoType: 'talk',
  });
  const inputMode = ref<'theme' | 'script'>('theme');
  const titleTouched = ref(false);
  const category = ref('全部');
  const templateSearch = ref('');
  const inspPage = ref(0);
  const aiBusy = ref(false);
  const aiError = ref('');
  const styleAiBusy = ref(false);
  const generating = ref(false);
  const tasks = ref<AiVideoTask[]>([]);
  const loading = ref(false);
  const voices = ref<AiVideoVoice[]>([]);
  const imageModels = ref<AiVideoModelOption[]>([]);
  const videoModels = ref<AiVideoModelOption[]>([]);
  // 风格页选中的图片/视频模型（默认取推荐项，提交时带上 id）
  const selectedImageModelId = ref<string | null>(null);
  const selectedVideoModelId = ref<string | null>(null);
  // 成片方式：video=AI 视频（图生视频），image_text=静图成片
  const mode = ref<'video' | 'image_text'>('image_text');
  const voicePreviewBusy = ref<string | null>(null);
  const playingVoice = ref<string | null>(null);
  let auditionAudio: HTMLAudioElement | null = null;
  const activeTaskId = ref<string | null>(null);
  const localActiveTask = ref<AiVideoTask | null>(null);
  const coverBusy = ref(false);
  const coverInputRef = ref<HTMLInputElement | null>(null);
  // 门控按钮（确认分镜 / 生成视频）与编辑弹窗状态
  const assetsBusy = ref(false);
  const composeBusy = ref(false);
  const shotEditVisible = ref(false);
  const shotEditIndex = ref(-1);
  const shotEditText = ref('');
  const shotEditImage = ref('');
  const shotEditVideoPrompt = ref('');
  const shotSaving = ref(false);
  const promptEditVisible = ref(false);
  const promptEditStyle = ref('');
  const promptEditCharacter = ref('');
  const promptEditExtra = ref('');
  const promptSaving = ref(false);
  const promptGenBusy = ref(false);
  // 单镜重绘画面 / 逐镜口播试听状态
  const regenShotBusy = ref<number | null>(null);
  const shotVoiceBusy = ref<number | null>(null);
  const playingShot = ref<number | null>(null);
  let shotAudio: HTMLAudioElement | null = null;
  // 镜头视频阶段：生成镜头视频 / 单镜重生成 / 单镜换图
  const shotsBusy = ref(false);
  const regenVideoBusy = ref<number | null>(null);
  const replaceImageBusy = ref<number | null>(null);
  const shotImageInputRef = ref<HTMLInputElement | null>(null);
  const shotImageUploadIndex = ref(-1);

  const IN_PROGRESS = ['PENDING', 'SCRIPTING', 'IMAGING', 'TTS', 'SYNTH', 'VIDEOING'];

  // 一句话主题的最大长度（太短用户写不清受众与知识点，放宽到 300 字）
  const THEME_MAX_LEN = 300;

  // 输出比例（复刻 printfilm 3 档：9:16 / 16:9 / 1:1，出图与合成按所选比例输出）
  const RATIOS = [
    { id: '9:16', label: '9:16', w: 18, h: 32 },
    { id: '16:9', label: '16:9', w: 36, h: 20 },
    { id: '1:1', label: '1:1', w: 24, h: 24 },
  ];

  // 配套文案（抖音/小红书/朋友圈）
  interface CompanionCopy {
    title?: string;
    copy?: string;
    hashtags?: string[];
  }
  type VideoCompanionCopy = {
    douyin?: CompanionCopy;
    xiaohongshu?: CompanionCopy;
    moments?: CompanionCopy;
  };

  const copyModalVisible = ref(false);
  const currentCopy = ref<VideoCompanionCopy | null>(null);
  const activeCopyPlatform = ref<'douyin' | 'xiaohongshu' | 'moments'>('douyin');
  const copyBusy = ref(false);

  // 分镜画面/视频在线预览弹窗（图片放大 / 视频播放）
  const previewVisible = ref(false);
  const previewMedia = ref<{ type: 'image' | 'video'; url: string; title: string } | null>(null);

  // 6 步流程（与 printfilm KEPU_STEPS 一致）
  const steps = computed(() => [
    { key: 'topic', label: t('video.step.topic') },
    { key: 'style', label: t('video.step.style') },
    { key: 'confirm', label: t('video.step.confirm') },
    { key: 'assets', label: t('video.step.assets') },
    { key: 'videos', label: t('video.step.videos') },
    { key: 'compose', label: t('video.step.compose') },
  ]);

  const inputTabs = computed<{ key: 'theme' | 'script'; label: string }[]>(() => [
    { key: 'theme', label: t('video.inputTabs.theme') },
    { key: 'script', label: t('video.inputTabs.script') },
  ]);

  // 成片类型：决定脚本结构与画面策略；comic（漫剧）暂占位禁用，等人物一致性里程碑落地再放开
  const videoTypeOptions = computed<{ value: string; label: string; hint: string; disabled: boolean }[]>(() => [
    { value: 'talk', label: t('video.videoType.talk'), hint: t('video.videoType.talkHint'), disabled: false },
    { value: 'product', label: t('video.videoType.product'), hint: t('video.videoType.productHint'), disabled: false },
    {
      value: 'scenario',
      label: t('video.videoType.scenario'),
      hint: t('video.videoType.scenarioHint'),
      disabled: false,
    },
    { value: 'comic', label: t('video.videoType.comic'), hint: t('video.videoType.comicHint'), disabled: true },
  ]);

  const activeTask = computed<AiVideoTask | null>(() => {
    if (!activeTaskId.value) {
      return null;
    }
    return tasks.value.find((task) => task.id === activeTaskId.value) ?? localActiveTask.value;
  });

  // 当前展示步骤：手动导航（点击已完成步骤）+ 任务进度自动向前推进
  const currentStep = computed<number>(() => step.value);

  // 任务状态 → 可达步（2=确认分镜 3=画面与配音 4=镜头视频 5=合成预览）
  function autoStepFor(task: AiVideoTask | null): number {
    if (!task) {
      return 0;
    }
    const s = task.status;
    if (s === 'FAILED') {
      return 2;
    }
    if (s === 'DONE' || s === 'SYNTH') {
      return 5;
    }
    if (s === 'VIDEOING' || s === 'VIDEOS_READY') {
      return 4;
    }
    if (s === 'ASSETS_READY' || s === 'IMAGING' || s === 'TTS') {
      return 3;
    }
    // SCRIPT_READY / SCRIPTING / PENDING：停在确认分镜等人点按钮
    return 2;
  }

  // 任务推进时，只向前扩大可达步；用户在前沿才自动跳到最新步，手动回看时不打断
  watch(activeTask, (task) => {
    const auto = autoStepFor(task);
    if (auto > maxReachedStep.value) {
      const wasAhead = step.value >= maxReachedStep.value;
      maxReachedStep.value = auto;
      if (wasAhead) {
        step.value = auto;
      }
    }
  });

  const categories = computed(() => videoTemplateCategories(VIDEO_TEMPLATES));
  const visibleCategories = computed(() => categories.value.slice(0, 6));

  const filteredTemplates = computed(() => {
    let list = [...VIDEO_TEMPLATES];
    if (category.value === '热门推荐') {
      list = list.sort((a, b) => a.sortOrder - b.sortOrder).slice(0, 8);
    } else if (category.value !== '全部') {
      list = list.filter((tpl) => tpl.category.includes(category.value));
    }
    const q = templateSearch.value.trim().toLowerCase();
    if (q) {
      list = list.filter((tpl) => tpl.name.toLowerCase().includes(q));
    }
    return list;
  });

  const selectedTemplate = computed(() => VIDEO_TEMPLATES.find((tpl) => tpl.templateKey === form.templateKey) ?? null);

  // 成片比例：默认跟随模板，用户可在风格页手动切换；切换模板时重置为该模板默认比例
  const selectedRatio = ref<string>('9:16');
  watch(selectedTemplate, (tpl) => {
    selectedRatio.value = tpl?.ratio || '9:16';
  });

  const outputModeLabel = computed(() =>
    selectedTemplate.value?.ratio === '9:16' ? t('video.summaryModeVideo916') : t('video.summaryModeVideo169')
  );

  const selectedVoice = computed(() => voices.value.find((v) => v.value === form.voice) ?? null);
  const selectedVoiceLabel = computed(() => selectedVoice.value?.label ?? form.voice);

  const canGenerate = computed(() => !!form.sourceText.trim() && !!form.templateKey);
  const hasRunning = computed(() => tasks.value.some((task) => IN_PROGRESS.includes(task.status)));

  const inspTotal = Math.ceil(INSPIRATION_POOL.length / PAGE_SIZE);
  const inspirations = computed<Inspiration[]>(() =>
    INSPIRATION_POOL.slice(inspPage.value * PAGE_SIZE, inspPage.value * PAGE_SIZE + PAGE_SIZE)
  );

  const storyboard = computed<AiVideoStoryboardSegment[]>(() => {
    const raw = activeTask.value?.storyboardJson;
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? (parsed as AiVideoStoryboardSegment[]) : [];
    } catch {
      return [];
    }
  });

  // 结构摘要：取前 5 镜映射为 开端/发展/转折/高潮/结局（复刻 printfilm）
  const structure = computed(() => {
    const labels = ['开端', '发展', '转折', '高潮', '结局'];
    return storyboard.value.slice(0, 5).map((seg, i) => ({
      label: labels[i] || `段落 ${i + 1}`,
      text: seg.text || '—',
    }));
  });

  // 分镜脚本生成中（尚无结构摘要内容），用于结构摘要空白态展示流光「正在努力生成中」
  const boardGenerating = computed(() => {
    const task = activeTask.value;
    return !!task && IN_PROGRESS.includes(task.status);
  });

  // 整片配音完成后算 1 段（我们后端是整片 TTS，非逐镜音频）
  const audioReady = computed(() => {
    const status = activeTask.value?.status;
    return status === 'ASSETS_READY' || status === 'SYNTH' || status === 'DONE';
  });

  // 成片方式是否为「AI 视频」：该模式下分镜素材完备度以出视频为准（未出视频显示「待出视频」）
  const isVideoMode = computed(() => activeTask.value?.mode === 'video');

  // 画面描述可编辑（分镜就绪或画面配音就绪阶段）；旁白在画面配音就绪后已合成不可再改
  const imageEditable = computed(() => {
    const s = activeTask.value?.status;
    return s === 'SCRIPT_READY' || s === 'ASSETS_READY';
  });
  const narrationEditable = computed(() => activeTask.value?.status === 'SCRIPT_READY');
  const assetsReady = computed(() => activeTask.value?.status === 'ASSETS_READY');
  const videosReady = computed(() => activeTask.value?.status === 'VIDEOS_READY');
  // 分镜可点开编辑（分镜/画面/镜头视频三个阶段各能改各自的字段）
  const shotEditable = computed(() => {
    const s = activeTask.value?.status;
    return s === 'SCRIPT_READY' || s === 'ASSETS_READY' || s === 'VIDEOS_READY';
  });

  function shotStatusLabel(seg: AiVideoStoryboardSegment): string {
    if (activeTask.value?.status === 'DONE') {
      return t('video.videoReady');
    }
    if (isVideoMode.value) {
      if (activeTask.value?.status === 'VIDEOING' && !seg.videoAttachmentId) {
        return t('video.shotVideoGenerating');
      }
      if (seg.videoAttachmentId) {
        return t('video.shotVideoReady');
      }
      if (seg.imageAttachmentId || (activeTask.value && IN_PROGRESS.includes(activeTask.value.status))) {
        return seg.imageAttachmentId ? t('video.shotWaitVideo') : t('video.generatingShot');
      }
      return t('video.shotWaitVideo');
    }
    if (seg.imageAttachmentId) {
      return t('video.shotImageReady');
    }
    if (activeTask.value && IN_PROGRESS.includes(activeTask.value.status)) {
      return t('video.generatingShot');
    }
    return t('video.shotImagePending');
  }

  // 该镜是否处于「正在生成」：配图中该镜还没出图，或出视频中该镜还没产出视频
  function isShotGenerating(seg: AiVideoStoryboardSegment): boolean {
    if (activeTask.value?.status === 'IMAGING' && !seg.imageAttachmentId) {
      return true;
    }
    return isVideoMode.value && activeTask.value?.status === 'VIDEOING' && !seg.videoAttachmentId;
  }

  // 该镜是否「就绪」：静图成片看是否已出图，AI 视频看是否已出该镜视频
  function shotDone(seg: AiVideoStoryboardSegment): boolean {
    if (isVideoMode.value) {
      return !!seg.videoAttachmentId;
    }
    return !!seg.imageAttachmentId;
  }

  // 逐镜时长只读估算：后端为整片 TTS，暂无逐镜音频，按口播字数约 4 字/秒估算
  function shotDuration(seg: AiVideoStoryboardSegment): number {
    const chars = (seg.text || '').replace(/\s/g, '').length;
    if (!chars) {
      return 0;
    }
    return Math.max(1, Math.round(chars / 4));
  }

  const phaseHint = computed(() => {
    const task = activeTask.value;
    const s = task?.status;
    if (s === 'SCRIPT_READY') {
      return t('video.phaseHint.confirm');
    }
    if (s === 'VIDEOING') {
      return t('video.phaseHint.videos');
    }
    if (s === 'VIDEOS_READY') {
      return t('video.phaseHint.videosReady');
    }
    if (s && IN_PROGRESS.includes(s)) {
      return t('video.phaseHint.running');
    }
    if (s === 'ASSETS_READY') {
      return isVideoMode.value ? t('video.phaseHint.assets') : t('video.phaseHint.assetsImageText');
    }
    if (s === 'DONE') {
      return t('video.phaseHint.compose');
    }
    if (s === 'FAILED') {
      return t('video.phaseHint.failed');
    }
    return t('video.phaseHint.confirm');
  });

  // 生成进度列表（复刻 printfilm 静图成片：主题解析/分镜脚本/画面生成/配音合成/成片渲染）
  const stageList = computed(() => {
    const task = activeTask.value;
    const total = storyboard.value.length;
    const imgs = storyboard.value.filter((s) => s.imageAttachmentId).length;
    const vids = storyboard.value.filter((s) => s.videoAttachmentId).length;
    const status = task?.status ?? 'PENDING';
    const p = task?.progress ?? 0;
    const scriptDone = total > 0 || !['PENDING', 'SCRIPTING'].includes(status);
    const stages = [
      { key: 'theme', label: t('video.stage.theme'), done: true },
      { key: 'script', label: t('video.stage.script'), done: scriptDone },
      {
        key: 'image',
        label: t('video.stage.image', { done: imgs, total }),
        done: total > 0 && imgs === total,
        run: status === 'IMAGING',
      },
      {
        key: 'tts',
        label: t('video.stage.tts', { done: audioReady.value ? 1 : 0, total: 1 }),
        done: p >= 60 || audioReady.value,
        run: status === 'TTS',
      },
    ];
    if (isVideoMode.value) {
      stages.push({
        key: 'video',
        label: t('video.stage.video', { done: vids, total }),
        done: total > 0 && vids === total,
        run: status === 'VIDEOING',
      });
    }
    stages.push({
      key: 'render',
      label: t('video.stage.render'),
      done: status === 'DONE',
      run: status === 'SYNTH',
    });
    return stages;
  });

  const platformCopy = computed(() => currentCopy.value?.[activeCopyPlatform.value] ?? null);

  function previewUrl(id: string): string {
    return `${PreviewAttachmentUrl}/${id}?userId=${userStore.userInfo.id}`;
  }

  // 打开分镜画面/视频在线预览：优先预览镜头视频，否则预览配图
  function openShotPreview(index: number) {
    const seg = storyboard.value[index];
    if (!seg) {
      return;
    }
    const title = `${t('video.colSceneNo')} ${index + 1}`;
    if (seg.videoAttachmentId) {
      previewMedia.value = { type: 'video', url: previewUrl(seg.videoAttachmentId), title };
    } else if (seg.imageAttachmentId) {
      previewMedia.value = { type: 'image', url: previewUrl(seg.imageAttachmentId), title };
    } else {
      return;
    }
    previewVisible.value = true;
  }

  // 分镜台封面：更换/用首镜后回退到该附件，否则回退模板封面
  const taskCoverUrl = computed(() =>
    activeTask.value?.coverAttachmentId ? previewUrl(activeTask.value.coverAttachmentId) : selectedTemplate.value?.cover
  );

  const hasFirstShot = computed(() => storyboard.value.some((seg) => !!seg.imageAttachmentId));

  async function aiGenStylePrompt() {
    const seed = form.sourceText.trim() || form.projectName.trim() || '企业品牌宣传';
    styleAiBusy.value = true;
    aiError.value = '';
    try {
      const result = await expandAiVideoContent({ topic: seed, mode: 'style' });
      form.stylePrompt = (result.content || '').trim();
    } catch (error) {
      aiError.value = (error as Error)?.message || t('common.operationFailed');
    } finally {
      styleAiBusy.value = false;
    }
  }

  function goStyle() {
    if (form.templateKey && form.sourceText.trim()) {
      step.value = 1;
      maxReachedStep.value = Math.max(maxReachedStep.value, 1);
      // 进入风格页即自动生成风格提示词，避免每次手动点「AI 生成」
      if (!form.stylePrompt.trim()) {
        aiGenStylePrompt();
      }
    }
  }

  function goTopic() {
    step.value = 0;
  }

  function goToStep(i: number) {
    if (i < 0 || i > maxReachedStep.value) {
      return;
    }
    step.value = i;
  }

  // 已有活动任务时，风格页主按钮应为「返回分镜台」而非重建任务，避免误触清空已生成的分镜/画面
  const hasActiveTask = computed<boolean>(() => Boolean(activeTaskId.value));

  function returnToBoard() {
    const task = activeTask.value;
    step.value = task ? Math.max(2, autoStepFor(task)) : 2;
  }

  function onSourceInput() {
    if (inputMode.value === 'theme') {
      form.sourceText = form.sourceText.slice(0, THEME_MAX_LEN);
    } else {
      form.sourceText = form.sourceText.slice(0, 8000);
    }
    if (!titleTouched.value || !form.projectName.trim() || form.projectName.trim() === '未命名作品') {
      form.projectName = deriveTitle(form.sourceText);
    }
  }

  function autoFillProjectName() {
    if ((!form.projectName.trim() || form.projectName.trim() === '未命名作品') && form.sourceText.trim()) {
      form.projectName = deriveTitle(form.sourceText);
      titleTouched.value = false;
    }
  }

  function applyInspiration(item: Inspiration) {
    if (inputMode.value === 'script') {
      form.sourceText = item.script.slice(0, 8000);
    } else {
      form.sourceText = item.theme.slice(0, THEME_MAX_LEN);
    }
    form.projectName = item.title.slice(0, 24);
    titleTouched.value = false;
    aiError.value = '';
  }

  function shuffleInspirations() {
    inspPage.value = (inspPage.value + 1) % inspTotal;
  }

  async function aiExpand() {
    const seed = form.sourceText.trim() || form.projectName.trim() || '人工智能如何改变生活';
    aiBusy.value = true;
    aiError.value = '';
    try {
      const expandMode = inputMode.value === 'script' ? 'script' : 'theme';
      const result = await expandAiVideoContent({ topic: seed, mode: expandMode });
      form.sourceText = (result.content || '').slice(0, expandMode === 'theme' ? THEME_MAX_LEN : 8000);
      if (!titleTouched.value || !form.projectName.trim() || form.projectName.trim() === '未命名作品') {
        form.projectName = (result.title || '').slice(0, 24);
      }
    } catch (error) {
      aiError.value = (error as Error)?.message || t('common.operationFailed');
    } finally {
      aiBusy.value = false;
    }
  }

  function voiceGenderLabel(v: AiVideoVoice): string {
    if (v.gender === 'female') {
      return t('video.voiceGender.female');
    }
    if (v.gender === 'male') {
      return t('video.voiceGender.male');
    }
    return t('video.voiceMeta');
  }

  function stopAudition() {
    if (auditionAudio) {
      auditionAudio.pause();
      auditionAudio.src = '';
      auditionAudio = null;
    }
    playingVoice.value = null;
  }

  async function previewVoice(v: AiVideoVoice) {
    if (!v || voicePreviewBusy.value) {
      return;
    }
    if (playingVoice.value === v.value) {
      stopAudition();
      return;
    }
    stopAudition();
    voicePreviewBusy.value = v.value;
    try {
      const res = await previewAiVideoVoice(v.value);
      if (!res?.audio) {
        Message.warning(t('video.modeComingSoon'));
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
      playingVoice.value = v.value;
      auditionAudio.onended = () => {
        if (playingVoice.value === v.value) {
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
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      voicePreviewBusy.value = null;
    }
  }

  function parseCopy(json?: string): VideoCompanionCopy | null {
    if (!json) {
      return null;
    }
    try {
      return JSON.parse(json) as VideoCompanionCopy;
    } catch {
      return null;
    }
  }

  function handleShowCopy(row: AiVideoTask) {
    const copy = parseCopy(row.copyJson);
    if (!copy) {
      Message.warning(t('video.noCopy'));
      return;
    }
    currentCopy.value = copy;
    activeCopyPlatform.value = 'douyin';
    copyModalVisible.value = true;
  }

  // 第六步「配套文案」按钮：文案为空时按需重试生成一次，再打开弹窗
  async function handleShowCopyClick() {
    const row = activeTask.value;
    if (!row) {
      return;
    }
    let copy = parseCopy(row.copyJson);
    if (!copy) {
      copyBusy.value = true;
      try {
        const updated = await regenerateAiVideoCopy(row.id);
        row.copyJson = updated.copyJson;
        copy = parseCopy(updated.copyJson);
      } catch (error) {
        Message.error((error as Error)?.message || t('common.operationFailed'));
        return;
      } finally {
        copyBusy.value = false;
      }
    }
    if (!copy) {
      Message.warning(t('video.noCopy'));
      return;
    }
    currentCopy.value = copy;
    activeCopyPlatform.value = 'douyin';
    copyModalVisible.value = true;
  }

  async function writeClipboard(text: string) {
    try {
      await navigator.clipboard.writeText(text);
      Message.success(t('video.copied'));
    } catch {
      Message.error(t('common.operationFailed'));
    }
  }

  function platformFullText(data?: CompanionCopy): string {
    if (!data) {
      return '';
    }
    const parts: string[] = [];
    if (data.title) {
      parts.push(data.title);
    }
    if (data.copy) {
      parts.push(data.copy);
    }
    if (data.hashtags?.length) {
      parts.push(data.hashtags.join(' '));
    }
    return parts.join('\n\n');
  }

  async function copyPlatform() {
    const text = platformFullText(platformCopy.value ?? undefined);
    if (text) {
      await writeClipboard(text);
    }
  }

  function isInProgress(task: AiVideoTask): boolean {
    return IN_PROGRESS.includes(task.status);
  }

  function formatTime(ts?: number): string {
    if (!ts) {
      return '-';
    }
    const d = dayjs(ts);
    return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-';
  }

  function statusStyle(status: string): string {
    const map: Record<string, string> = {
      PENDING: '#f97316',
      SCRIPTING: '#2563eb',
      SCRIPT_READY: '#f97316',
      IMAGING: '#2563eb',
      TTS: '#2563eb',
      ASSETS_READY: '#0ea5e9',
      SYNTH: '#2563eb',
      DONE: '#18a058',
      FAILED: '#e5484d',
    };
    const color = map[status] || '#8a94a6';
    return `padding:2px 8px;border-radius:4px;font-size:12px;color:${color};background:${color}1a;`;
  }

  function renderStatus(status: string) {
    const key = `video.status.${status}`;
    const text = t(key);
    return h('span', { style: statusStyle(status) }, text === key ? status : text);
  }

  async function loadTasks() {
    try {
      tasks.value = await getAiVideoTasks();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  async function loadVoices() {
    try {
      const list = await getAiVideoVoices();
      voices.value = list ?? [];
      if (voices.value.length && !voices.value.some((v) => v.value === form.voice)) {
        form.voice = voices.value[0].value;
      }
    } catch {
      // 音色拉取失败忽略，保留默认值
    }
  }

  async function loadModels() {
    try {
      const result = await getAiVideoModels();
      imageModels.value = result?.imageModels ?? [];
      videoModels.value = result?.videoModels ?? [];
      // 默认选中推荐模型，避免提交时模型 id 为空
      if (!selectedImageModelId.value && imageModels.value.length) {
        selectedImageModelId.value = imageModels.value.find((m) => m.recommended)?.id ?? imageModels.value[0].id;
      }
      if (!selectedVideoModelId.value && videoModels.value.length) {
        selectedVideoModelId.value = videoModels.value.find((m) => m.recommended)?.id ?? videoModels.value[0].id;
      }
    } catch {
      // 模型拉取失败忽略，风格页展示「暂未开放」占位
    }
  }

  function syncActiveTask(task: AiVideoTask | null) {
    if (task?.id === activeTaskId.value) {
      localActiveTask.value = task;
    }
  }

  async function handleCoverFile(file: File) {
    if (!activeTaskId.value) {
      return;
    }
    coverBusy.value = true;
    try {
      const res = await setAiVideoCover(activeTaskId.value, { fileList: [file] });
      syncActiveTask(res?.data ?? null);
      await loadTasks();
      Message.success(t('video.coverUpdated'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      coverBusy.value = false;
    }
  }

  function onCoverChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0] ?? null;
    if (input) {
      input.value = '';
    }
    if (file) {
      handleCoverFile(file);
    }
  }

  async function useFirstShotCover() {
    if (!activeTaskId.value) {
      return;
    }
    coverBusy.value = true;
    try {
      const task = await setAiVideoFirstShotCover(activeTaskId.value);
      syncActiveTask(task);
      await loadTasks();
      Message.success(t('video.coverUpdated'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      coverBusy.value = false;
    }
  }

  let pollTimer: number | null = null;

  function stopPolling() {
    if (pollTimer !== null) {
      window.clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  function startPolling() {
    stopPolling();
    pollTimer = window.setInterval(async () => {
      try {
        await loadTasks();
        if (!hasRunning.value) {
          stopPolling();
        }
      } catch {
        // 轮询失败忽略，等待下一轮重试
      }
    }, 3000);
  }

  function openShotEdit(index: number) {
    const seg = storyboard.value[index];
    if (!seg) {
      return;
    }
    shotEditIndex.value = index;
    shotEditText.value = seg.text || '';
    shotEditImage.value = seg.image || '';
    shotEditVideoPrompt.value = seg.videoPrompt || '';
    shotEditVisible.value = true;
  }

  async function saveShotEdit() {
    const id = activeTaskId.value;
    if (!id || shotEditIndex.value < 0) {
      return;
    }
    shotSaving.value = true;
    try {
      const updated = await updateAiVideoShot(id, shotEditIndex.value, {
        text: shotEditText.value.trim(),
        image: shotEditImage.value.trim(),
        videoPrompt: shotEditVideoPrompt.value.trim(),
      });
      syncActiveTask(updated);
      await loadTasks();
      shotEditVisible.value = false;
      Message.success(t('video.shotUpdated'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      shotSaving.value = false;
    }
  }

  /** 试听单个分镜口播：用任务所选音色合成该镜台词并播放（同一时刻只播一条） */
  function stopShotVoice() {
    if (shotAudio) {
      shotAudio.pause();
      shotAudio.src = '';
      shotAudio = null;
    }
    playingShot.value = null;
  }

  async function previewShotVoice(index: number) {
    const id = activeTaskId.value;
    if (id == null || shotVoiceBusy.value != null) {
      return;
    }
    if (playingShot.value === index) {
      stopShotVoice();
      return;
    }
    stopShotVoice();
    shotVoiceBusy.value = index;
    try {
      const res = await previewAiVideoShotVoice(id, index);
      if (!res?.audio) {
        Message.warning(t('video.modeComingSoon'));
        return;
      }
      const binary = atob(res.audio);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i += 1) {
        bytes[i] = binary.charCodeAt(i);
      }
      const blob = new Blob([bytes], { type: 'audio/wav' });
      const url = URL.createObjectURL(blob);
      shotAudio = new Audio(url);
      playingShot.value = index;
      shotAudio.onended = () => {
        if (playingShot.value === index) {
          playingShot.value = null;
        }
      };
      shotAudio.onerror = () => {
        playingShot.value = null;
      };
      await shotAudio.play().catch(() => {
        playingShot.value = null;
      });
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      shotVoiceBusy.value = null;
    }
  }

  /** 重绘单个分镜画面：用该镜当前画面描述重新出图并回填（后端同步出图，完成即回显） */
  async function regenShotImage(index: number) {
    const id = activeTaskId.value;
    if (id == null || regenShotBusy.value != null) {
      return;
    }
    regenShotBusy.value = index;
    try {
      const updated = await regenAiVideoShotImage(id, index, {});
      syncActiveTask(updated);
      await loadTasks();
      Message.success(t('video.regenImageDone'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      regenShotBusy.value = null;
    }
  }

  /** AI 生成/换一批：按当前主题一次生成风格、角色、额外三条提示词；fillEmptyOnly 时只补齐空白项 */
  async function genPrompts(fillEmptyOnly = false) {
    const seed = activeTask.value?.sourceText?.trim() || activeTask.value?.title?.trim() || form.sourceText.trim();
    if (!seed) {
      Message.warning(t('video.themePlaceholder'));
      return;
    }
    promptGenBusy.value = true;
    try {
      const result = await expandAiVideoContent({ topic: seed, mode: 'prompts' });
      if (result?.stylePrompt && (!fillEmptyOnly || !promptEditStyle.value)) {
        promptEditStyle.value = result.stylePrompt;
      }
      if (result?.characterPrompt && (!fillEmptyOnly || !promptEditCharacter.value)) {
        promptEditCharacter.value = result.characterPrompt;
      }
      if (result?.extraPrompt && (!fillEmptyOnly || !promptEditExtra.value)) {
        promptEditExtra.value = result.extraPrompt;
      }
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      promptGenBusy.value = false;
    }
  }

  function openPromptEdit() {
    const task = activeTask.value;
    promptEditStyle.value = task?.stylePrompt || '';
    promptEditCharacter.value = task?.characterPrompt || '';
    promptEditExtra.value = task?.extraPrompt || '';
    promptEditVisible.value = true;
    // 打开弹窗即补齐三条提示词（只填空白项，已填的保留，仍可点「换一批」全部重生成）
    if (!promptEditStyle.value || !promptEditCharacter.value || !promptEditExtra.value) {
      genPrompts(true);
    }
  }

  async function savePromptEdit() {
    const id = activeTaskId.value;
    if (!id) {
      return;
    }
    promptSaving.value = true;
    try {
      const updated = await updateAiVideoPrompts(id, {
        stylePrompt: promptEditStyle.value.trim(),
        characterPrompt: promptEditCharacter.value.trim(),
        extraPrompt: promptEditExtra.value.trim(),
      });
      syncActiveTask(updated);
      await loadTasks();
      promptEditVisible.value = false;
      Message.success(t('video.promptUpdated'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      promptSaving.value = false;
    }
  }

  async function confirmAssets() {
    const id = activeTaskId.value;
    if (!id) {
      return;
    }
    assetsBusy.value = true;
    try {
      const updated = await confirmAiVideoAssets(id);
      syncActiveTask(updated);
      await loadTasks();
      Message.success(t('video.assetsStarted'));
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      assetsBusy.value = false;
    }
  }

  async function composeVideo() {
    const id = activeTaskId.value;
    if (!id) {
      return;
    }
    composeBusy.value = true;
    try {
      const updated = await composeAiVideo(id);
      syncActiveTask(updated);
      await loadTasks();
      Message.success(t('video.composeStarted'));
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      composeBusy.value = false;
    }
  }

  async function generateShots() {
    const id = activeTaskId.value;
    if (!id) {
      return;
    }
    shotsBusy.value = true;
    try {
      const updated = await generateAiVideoShots(id);
      syncActiveTask(updated);
      await loadTasks();
      Message.success(t('video.generateShotsStarted'));
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      shotsBusy.value = false;
    }
  }

  /** 画面配音就绪后的门控：AI 视频先生成镜头视频，静图成片直接合成 */
  function startComposeOrShots() {
    if (isVideoMode.value) {
      generateShots();
    } else {
      composeVideo();
    }
  }

  /** 重新生成单个镜头视频：用该镜当前视频提示词重新调 Seedance 出视频并回填（同步出视频，完成即回显） */
  async function regenShotVideo(index: number) {
    const id = activeTaskId.value;
    if (id == null || regenVideoBusy.value != null) {
      return;
    }
    regenVideoBusy.value = index;
    try {
      const updated = await regenAiVideoShotVideo(id, index, {});
      syncActiveTask(updated);
      await loadTasks();
      Message.success(t('video.regenVideoDone'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      regenVideoBusy.value = null;
    }
  }

  function openReplaceImage(index: number) {
    shotImageUploadIndex.value = index;
    shotImageInputRef.value?.click();
  }

  async function replaceShotImage(index: number, file: File) {
    const id = activeTaskId.value;
    if (!id) {
      return;
    }
    replaceImageBusy.value = index;
    try {
      const res = await uploadAiVideoShotImage(id, index, { fileList: [file] });
      syncActiveTask(res?.data ?? null);
      await loadTasks();
      Message.success(t('video.replaceImageDone'));
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      replaceImageBusy.value = null;
    }
  }

  function onShotImageChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0] ?? null;
    if (input) {
      input.value = '';
    }
    const index = shotImageUploadIndex.value;
    shotImageUploadIndex.value = -1;
    if (file && index >= 0) {
      replaceShotImage(index, file);
    }
  }

  function backToEdit() {
    stopPolling();
    activeTaskId.value = null;
    localActiveTask.value = null;
    step.value = 1;
    maxReachedStep.value = 1;
  }

  function resetStudio() {
    stopPolling();
    activeTaskId.value = null;
    localActiveTask.value = null;
    step.value = 0;
    maxReachedStep.value = 0;
    form.sourceText = '';
    form.projectName = '';
    form.extraText = '';
    form.stylePrompt = '';
    form.characterPrompt = '';
    selectedImageModelId.value = null;
    selectedVideoModelId.value = null;
    titleTouched.value = false;
    aiError.value = '';
    view.value = 'studio';
  }

  async function handleGenerate() {
    if (!form.sourceText.trim()) {
      Message.warning(t('video.themePlaceholder'));
      return;
    }
    if (!form.templateKey) {
      Message.warning(t('video.plzSelectTemplate'));
      return;
    }
    generating.value = true;
    try {
      const created = await createAiVideoTask({
        title: form.projectName.trim() || deriveTitle(form.sourceText) || '未命名作品',
        sourceText: form.sourceText.trim(),
        extraText: form.extraText?.trim(),
        stylePrompt: form.stylePrompt?.trim(),
        characterPrompt: form.characterPrompt?.trim(),
        mode: mode.value,
        videoType: form.videoType,
        imageModelId: selectedImageModelId.value ?? undefined,
        videoModelId: selectedVideoModelId.value ?? undefined,
        voice: form.voice,
        templateKey: form.templateKey,
        ratio: selectedRatio.value,
      });
      activeTaskId.value = created.id;
      localActiveTask.value = created;
      step.value = 2;
      maxReachedStep.value = 2;
      Message.success(t('video.generateSuccess'));
      await loadTasks();
      if (!tasks.value.some((task) => task.id === created.id)) {
        tasks.value.unshift(created);
      }
      startPolling();
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      generating.value = false;
    }
  }

  async function retryGenerate() {
    await handleGenerate();
  }

  async function handleDownload(row: AiVideoTask) {
    if (!row.attachmentId) {
      return;
    }
    try {
      const res = await downloadAttachment(row.attachmentId);
      const url = URL.createObjectURL(new Blob([res], { type: 'application/octet-stream' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `${row.title}.mp4`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    }
  }

  function handleDelete(row: AiVideoTask) {
    openModal({
      type: 'error',
      title: t('video.delete'),
      content: t('video.deleteConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiVideoTask(row.id);
          Message.success(t('common.deleteSuccess'));
          await loadTasks();
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        }
      },
    });
  }

  function csvEscape(value: unknown): string {
    const s = String(value ?? '');
    return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  }

  /** 导出分镜脚本 CSV（复刻 printfilm：镜号/旁白/画面描述/时长/状态） */
  function downloadStoryboardCsv() {
    const header = [
      t('video.colNo'),
      t('video.colNarration'),
      t('video.colImageDesc'),
      t('video.colDuration'),
      t('video.colStatusShot'),
    ];
    const rows = storyboard.value.map((seg, i) =>
      [i + 1, seg.text || '', seg.image || '', shotDuration(seg), shotStatusLabel(seg)].map(csvEscape).join(',')
    );
    const bom = '﻿';
    const blob = new Blob([bom + [header.join(','), ...rows].join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `${activeTask.value?.title || 'storyboard'}_storyboard.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
    Message.success(t('video.exportedStoryboard'));
  }

  /** 删除当前项目：素材与成片一并清除，确认后回到创作台 */
  function handleDeleteProject() {
    const id = activeTaskId.value;
    if (!id) {
      return;
    }
    openModal({
      type: 'error',
      title: t('video.deleteProject'),
      content: t('video.deleteProjectConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiVideoTask(id);
          Message.success(t('common.deleteSuccess'));
          stopPolling();
          activeTaskId.value = null;
          localActiveTask.value = null;
          step.value = 0;
          maxReachedStep.value = 0;
          await loadTasks();
          view.value = 'studio';
        } catch (error) {
          Message.error((error as Error)?.message || t('common.operationFailed'));
        }
      },
    });
  }

  const columns = computed<DataTableColumns<AiVideoTask>>(() => [
    {
      title: t('video.colTitle'),
      key: 'title',
      minWidth: 240,
      render: (row) =>
        h('div', { class: 'flex flex-col gap-[2px]' }, [
          h('span', { class: 'text-[13px]' }, row.title),
          row.status === 'FAILED' && row.errorMsg
            ? h('span', { class: 'text-[12px] text-[#e5484d]' }, row.errorMsg)
            : null,
        ]),
    },
    {
      title: t('video.colStatus'),
      key: 'status',
      width: 110,
      render: (row) => renderStatus(row.status),
    },
    {
      title: t('video.colProgress'),
      key: 'progress',
      width: 140,
      render: (row) =>
        h(NProgress, {
          type: 'line',
          percentage: row.status === 'DONE' ? 100 : row.progress ?? 0,
          processing: isInProgress(row),
          indicatorPlacement: 'inside',
          height: 16,
        }),
    },
    {
      title: t('video.colTime'),
      key: 'createTime',
      width: 150,
      render: (row) => h('span', { class: 'text-[12px]' }, formatTime(row.createTime)),
    },
    {
      title: t('video.colAction'),
      key: 'action',
      width: 220,
      render: (row) =>
        h('div', { class: 'flex items-center gap-[8px]' }, [
          row.status === 'DONE' && row.attachmentId
            ? h(
                NButton,
                { size: 'tiny', text: true, type: 'primary', onClick: () => handleDownload(row) },
                { default: () => t('video.download') }
              )
            : null,
          row.copyJson
            ? h(
                NButton,
                { size: 'tiny', text: true, type: 'info', onClick: () => handleShowCopy(row) },
                { default: () => t('video.copy') }
              )
            : null,
          h(
            NButton,
            { size: 'tiny', text: true, type: 'error', onClick: () => handleDelete(row) },
            { default: () => t('video.delete') }
          ),
        ]),
    },
  ]);

  onMounted(async () => {
    loading.value = true;
    try {
      // 从获客内容「做成视频」跳转过来时，带主题/文案进来预填
      const titleParam = route.query.title;
      const extraParam = route.query.extraText;
      if (typeof titleParam === 'string' && titleParam) {
        form.sourceText = titleParam;
        form.projectName = deriveTitle(titleParam);
      }
      if (typeof extraParam === 'string' && extraParam) {
        form.extraText = extraParam;
      }
      if (VIDEO_TEMPLATES.length && !VIDEO_TEMPLATES.some((tpl) => tpl.templateKey === form.templateKey)) {
        form.templateKey = VIDEO_TEMPLATES[0].templateKey;
      }
      await loadVoices();
      await loadModels();
      await loadTasks();
      if (hasRunning.value) {
        startPolling();
      }
    } finally {
      loading.value = false;
    }
  });

  onBeforeUnmount(() => {
    stopPolling();
    stopAudition();
    stopShotVoice();
  });
</script>

<style scoped>
  .pf-prompt-chip:disabled {
    opacity: 0.55;
    cursor: default;
  }
  .pf-shot-duration {
    font-size: 0.82rem;
    color: var(--pf-muted);
    font-variant-numeric: tabular-nums;
  }
</style>
