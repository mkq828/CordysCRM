<template>
  <div class="ai-employee-page flex h-full flex-col overflow-hidden p-[16px]">
    <CrmCard
      hide-footer
      no-content-padding
      content-height="100%"
      class="ai-employee-card min-h-0 flex-1 overflow-hidden"
    >
      <template #header>
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_crmbot" :size="16" color="var(--primary-8)" />
          <span class="text-[14px] font-semibold">{{ t('menu.aiEmployee') }}</span>
        </div>
      </template>

      <CrmSplitPanel v-model:size="splitSize" class="h-full" :max="0.6" :min="0.3" :default-size="0.42">
        <template #1>
          <div class="flex h-full flex-col border-r border-[var(--divider)]">
            <div class="px-[16px] pt-[12px] text-[13px] text-orange-500">{{ t('aiEmployee.desc') }}</div>

            <n-tabs v-model:value="activeTab" type="line" class="ai-employee-tabs mt-[4px] flex-1">
              <n-tab-pane name="catalog" :tab="t('aiEmployee.catalogTab')">
                <div class="flex h-full flex-col">
                  <div class="px-[16px] pb-[8px]">
                    <n-input
                      v-model:value="keyword"
                      clearable
                      :placeholder="t('aiEmployee.searchPlaceholder')"
                      @input="handleKeywordChange"
                    />
                  </div>

                  <div class="division-bar flex gap-[8px] overflow-x-auto px-[16px] pb-[8px]">
                    <div
                      v-for="d in divisions"
                      :key="d.division"
                      class="division-chip shrink-0 cursor-pointer rounded-[6px] px-[10px] py-[4px] text-[12px]"
                      :class="selectedDivision === d.division ? 'division-chip--active' : ''"
                      @click="handleSelectDivision(d.division)"
                    >
                      {{ divisionLabel(d.division) }} · {{ d.count }}
                    </div>
                  </div>

                  <div class="role-list min-h-0 flex-1 overflow-y-auto px-[16px] pb-[16px]">
                    <n-spin :show="loadingRoles">
                      <template v-if="roles.length === 0 && !loadingRoles">
                        <n-empty :description="t('aiEmployee.emptyRoles')" class="mt-[48px]" />
                      </template>
                      <div v-else class="flex flex-col gap-[8px]">
                        <div
                          v-for="role in roles"
                          :key="role.id"
                          class="role-card flex cursor-pointer items-center gap-[10px] rounded-[8px] border border-[var(--divider)] p-[10px] transition-colors hover:border-[var(--primary-8)]"
                          @click="openCreate(role)"
                        >
                          <span class="text-[22px] leading-none">{{ role.emoji || '🤖' }}</span>
                          <div class="min-w-0 flex-1">
                            <div class="truncate text-[13px] font-medium text-[var(--text-n1)]">{{ role.name }}</div>
                            <div class="truncate text-[12px] text-[var(--text-n3)]">{{ role.vibe }}</div>
                          </div>
                        </div>
                      </div>
                    </n-spin>
                  </div>
                </div>
              </n-tab-pane>

              <n-tab-pane name="agents" :tab="t('aiEmployee.agentsTab')">
                <div class="agent-list flex flex-col gap-[8px] px-[16px] pb-[16px]">
                  <template v-if="agents.length === 0">
                    <n-empty :description="t('aiEmployee.emptyAgents')" class="mt-[48px]" />
                  </template>
                  <div
                    v-for="agent in agents"
                    :key="agent.id"
                    class="agent-card cursor-pointer rounded-[8px] border border-[var(--divider)] p-[10px] transition-colors"
                    :class="activeAgent?.id === agent.id ? 'agent-card--active' : ''"
                    @click="selectAgent(agent)"
                  >
                    <div class="flex items-center gap-[10px]">
                      <span class="text-[22px] leading-none">{{ agent.employeeEmoji || '🤖' }}</span>
                      <div class="min-w-0 flex-1">
                        <div class="truncate text-[13px] font-medium text-[var(--text-n1)]">{{ agent.name }}</div>
                        <div class="truncate text-[12px] text-[var(--text-n3)]">{{ agent.employeeName }}</div>
                      </div>
                      <div class="flex items-center gap-[4px]" @click.stop>
                        <n-button size="tiny" quaternary @click="openEdit(agent)">{{
                          t('aiEmployee.editAgent')
                        }}</n-button>
                        <n-button size="tiny" quaternary type="error" @click="confirmDelete(agent)">
                          {{ t('aiEmployee.delete') }}
                        </n-button>
                      </div>
                    </div>
                  </div>
                </div>
              </n-tab-pane>
            </n-tabs>
          </div>
        </template>

        <template #2>
          <div class="flex h-full flex-col">
            <template v-if="activeAgent">
              <div
                class="chat-header flex items-center gap-[10px] border-b border-[var(--divider)] px-[20px] py-[12px]"
              >
                <span class="text-[22px] leading-none">{{ activeAgent.employeeEmoji || '🤖' }}</span>
                <div class="min-w-0 flex-1">
                  <div class="truncate text-[14px] font-semibold text-[var(--text-n1)]">{{ activeAgent.name }}</div>
                  <div class="truncate text-[12px] text-[var(--text-n3)]">{{ activeAgent.employeeName }}</div>
                </div>
              </div>

              <div ref="messageListRef" class="message-list min-h-0 flex-1 overflow-y-auto px-[20px] py-[16px]">
                <template v-if="messages.length === 0">
                  <div class="mt-[48px] text-center text-[13px] text-[var(--text-n3)]">
                    {{ activeAgent.employeeName }}
                  </div>
                </template>
                <div
                  v-for="(msg, index) in messages"
                  :key="index"
                  class="mb-[12px] flex"
                  :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
                >
                  <div
                    class="message-bubble max-w-[80%] whitespace-pre-wrap rounded-[10px] px-[14px] py-[10px] text-[13px] leading-[1.6]"
                    :class="msg.role === 'user' ? 'message-bubble--user' : 'message-bubble--assistant'"
                  >
                    <ShimmerText
                      v-if="msg.role === 'assistant' && index === messages.length - 1 && sending && !msg.content"
                      size="13px"
                    >
                      {{ t('aiEmployee.sending') }}
                    </ShimmerText>
                    {{ msg.content }}
                  </div>
                </div>
              </div>

              <div class="chat-input flex items-end gap-[12px] border-t border-[var(--divider)] px-[20px] py-[12px]">
                <n-input
                  v-model:value="input"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 5 }"
                  :placeholder="t('aiEmployee.chatPlaceholder')"
                  :disabled="sending"
                  @keydown.enter.exact.prevent="send"
                />
                <AiActionButton :loading="sending" :disabled="!canSend" @click="send">
                  {{ t('aiEmployee.send') }}
                </AiActionButton>
              </div>
            </template>

            <div v-else class="flex flex-1 flex-col items-center justify-center gap-[12px] text-[var(--text-n3)]">
              <CrmIcon type="iconicon_crmbot" :size="48" color="var(--text-n4)" />
              <span class="text-[13px]">{{ t('aiEmployee.emptyChat') }}</span>
            </div>
          </div>
        </template>
      </CrmSplitPanel>
    </CrmCard>

    <n-modal
      v-model:show="showCreateModal"
      :mask-closable="false"
      preset="card"
      :title="editingAgentId ? t('aiEmployee.editAgent') : t('aiEmployee.createAgent')"
      class="ai-employee-modal"
    >
      <div v-if="currentRole" class="mb-[16px] rounded-[8px] bg-[var(--fill-1)] p-[12px]">
        <div class="flex items-center gap-[8px]">
          <span class="text-[20px] leading-none">{{ currentRole.emoji || '🤖' }}</span>
          <span class="text-[14px] font-semibold text-[var(--text-n1)]">{{ currentRole.name }}</span>
        </div>
        <div v-if="currentRole.vibe" class="mt-[6px] text-[12px] text-[var(--text-n3)]">{{ currentRole.vibe }}</div>
        <div v-if="currentRole.description" class="mt-[4px] text-[12px] leading-[1.6] text-[var(--text-n2)]">
          {{ currentRole.description }}
        </div>
      </div>

      <n-form label-placement="top">
        <n-form-item :label="t('aiEmployee.agentName')">
          <n-input v-model:value="agentForm.name" :placeholder="t('aiEmployee.agentNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('aiEmployee.customPrompt')">
          <n-input
            v-model:value="agentForm.customPrompt"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            :placeholder="t('aiEmployee.customPromptPlaceholder')"
          />
        </n-form-item>
        <n-form-item :label="t('aiEmployee.knowledgeEnabled')">
          <div class="flex items-center gap-[8px]">
            <n-switch v-model:value="agentForm.knowledgeEnabled" />
            <span class="text-[12px] text-orange-500">{{ t('aiEmployee.knowledgeTip') }}</span>
          </div>
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="flex justify-end gap-[12px]">
          <n-button @click="showCreateModal = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" :disabled="!agentForm.name.trim()" @click="submitAgent">
            {{ t('aiEmployee.save') }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref } from 'vue';
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiEmployeeAgent, AiEmployeeDetail, AiEmployeeDivision, AiEmployeeItem } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import ShimmerText from '@/components/pure/effects/ShimmerText.vue';
  import AiActionButton from '@/components/business/ai-action-button/index.vue';

  import {
    addAiEmployeeAgent,
    deleteAiEmployeeAgent,
    getAiEmployeeAgents,
    getAiEmployeeCatalog,
    getAiEmployeeDetail,
    getAiEmployeeDivisions,
    streamAiEmployeeChat,
    updateAiEmployeeAgent,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
  }

  const DIVISION_LABELS: Record<string, string> = {
    'academic': '学术',
    'design': '设计',
    'engineering': '工程研发',
    'finance': '财务',
    'game-development': '游戏开发',
    'gis': '地理信息',
    'healthcare': '医疗健康',
    'marketing': '市场营销',
    'paid-media': '付费投放',
    'product': '产品',
    'project-management': '项目管理',
    'research': '研究',
    'sales': '销售',
    'security': '安全',
    'spatial-computing': '空间计算',
    'specialized': '专项',
    'support': '客户支持',
    'testing': '测试',
  };

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const activeTab = ref<'catalog' | 'agents'>('catalog');
  const splitSize = ref(0.42);
  const divisions = ref<AiEmployeeDivision[]>([]);
  const selectedDivision = ref('');
  const keyword = ref('');
  const roles = ref<AiEmployeeItem[]>([]);
  const loadingRoles = ref(false);
  const agents = ref<AiEmployeeAgent[]>([]);

  const showCreateModal = ref(false);
  const saving = ref(false);
  const editingAgentId = ref('');
  const currentRole = ref<AiEmployeeDetail | null>(null);
  const agentForm = ref({
    name: '',
    employeeId: '',
    customPrompt: '',
    knowledgeEnabled: false,
  });

  const activeAgent = ref<AiEmployeeAgent | null>(null);
  const messages = ref<ChatMessage[]>([]);
  const input = ref('');
  const sending = ref(false);
  const conversationId = ref('');
  const messageListRef = ref<HTMLElement>();

  const canSend = computed(() => sending.value === false && input.value.trim() !== '');

  function divisionLabel(division: string): string {
    return DIVISION_LABELS[division] || division;
  }

  async function loadRoles() {
    if (!selectedDivision.value) {
      return;
    }
    loadingRoles.value = true;
    try {
      roles.value = await getAiEmployeeCatalog(selectedDivision.value, keyword.value.trim() || undefined);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loadingRoles.value = false;
    }
  }

  async function loadDivisions() {
    const list = await getAiEmployeeDivisions();
    divisions.value = list || [];
    if (divisions.value.length > 0) {
      selectedDivision.value = divisions.value[0].division;
      await loadRoles();
    }
  }

  async function loadAgents() {
    agents.value = await getAiEmployeeAgents();
  }

  function handleSelectDivision(division: string) {
    selectedDivision.value = division;
    loadRoles();
  }

  function handleKeywordChange() {
    loadRoles();
  }

  async function openCreate(role: AiEmployeeItem) {
    editingAgentId.value = '';
    currentRole.value = null;
    agentForm.value = {
      name: role.name,
      employeeId: role.id,
      customPrompt: '',
      knowledgeEnabled: false,
    };
    showCreateModal.value = true;
    try {
      currentRole.value = await getAiEmployeeDetail(role.id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function openEdit(agent: AiEmployeeAgent) {
    editingAgentId.value = agent.id;
    currentRole.value = null;
    agentForm.value = {
      name: agent.name,
      employeeId: agent.employeeId,
      customPrompt: agent.customPrompt || '',
      knowledgeEnabled: agent.knowledgeEnabled === 1,
    };
    showCreateModal.value = true;
    try {
      currentRole.value = await getAiEmployeeDetail(agent.employeeId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function submitAgent() {
    if (!agentForm.value.name.trim()) {
      return;
    }
    saving.value = true;
    try {
      const payload = {
        id: editingAgentId.value || undefined,
        name: agentForm.value.name.trim(),
        employeeId: agentForm.value.employeeId,
        customPrompt: agentForm.value.customPrompt,
        knowledgeEnabled: agentForm.value.knowledgeEnabled,
      };
      if (editingAgentId.value) {
        await updateAiEmployeeAgent(payload);
        Message.success(t('common.updateSuccess'));
      } else {
        await addAiEmployeeAgent(payload);
        Message.success(t('common.addSuccess'));
      }
      showCreateModal.value = false;
      await loadAgents();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(agent: AiEmployeeAgent) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: agent.name }),
      content: t('aiEmployee.deleteTip'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      positiveButtonProps: {
        type: 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        await deleteAiEmployeeAgent(agent.id);
        Message.success(t('common.deleteSuccess'));
        if (activeAgent.value?.id === agent.id) {
          activeAgent.value = null;
          messages.value = [];
          conversationId.value = '';
        }
        await loadAgents();
      },
    });
  }

  function selectAgent(agent: AiEmployeeAgent) {
    activeAgent.value = agent;
    messages.value = [];
    conversationId.value = '';
    input.value = '';
  }

  function scrollToBottom() {
    nextTick(() => {
      const el = messageListRef.value;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }

  async function send() {
    if (!canSend.value || !activeAgent.value) {
      return;
    }
    const text = input.value.trim();
    input.value = '';
    messages.value.push({ role: 'user', content: text });
    messages.value.push({ role: 'assistant', content: '' });
    sending.value = true;
    scrollToBottom();

    try {
      const stream = streamAiEmployeeChat(activeAgent.value.id, {
        message: text,
        conversationId: conversationId.value || undefined,
      });

      // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，无需 regenerator-runtime
      for await (const event of stream) {
        if (event.type === 'run') {
          const cid = event.run?.conversationId || '';
          if (cid) {
            conversationId.value = cid;
          }
        } else if (event.type === 'chunk') {
          const last = messages.value[messages.value.length - 1];
          last.content += event.content || '';
          scrollToBottom();
        } else if (event.type === 'error') {
          const last = messages.value[messages.value.length - 1];
          last.content = event.errorMessage || t('common.operationFailed');
          throw new Error(event.errorMessage || t('common.operationFailed'));
        }
      }
    } catch (error) {
      Message.error((error as Error)?.message || t('common.operationFailed'));
    } finally {
      sending.value = false;
      scrollToBottom();
    }
  }

  onMounted(() => {
    loadDivisions();
    loadAgents();
  });
</script>

<style lang="less" scoped>
  .ai-employee-page {
    background: #f5f7fa;
  }
  .ai-employee-card {
    :deep(.crm-card__content) {
      padding: 0;
    }
  }
  .division-bar {
    scrollbar-width: none;
    &::-webkit-scrollbar {
      display: none;
    }
  }
  .division-chip {
    border: 1px solid transparent;
    white-space: nowrap;
    color: var(--text-n2);
    background: var(--fill-1);
    &--active {
      border-color: var(--primary-8);
      color: var(--primary-8);
      background: rgb(249 115 22 / 8%);
    }
  }
  .role-card,
  .agent-card {
    &:hover {
      background: var(--fill-1);
    }
  }
  .agent-card--active {
    border-color: var(--primary-8);
    background: rgb(249 115 22 / 6%);
  }
  .message-bubble--user {
    color: #ffffff;
    background: var(--primary-8);
  }
  .message-bubble--assistant {
    color: var(--text-n1);
    background: var(--fill-1);
  }
</style>
