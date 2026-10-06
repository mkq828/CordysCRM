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
          <div class="flex h-full flex-col border-r border-[var(--divider-color)]">
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

                  <div class="catalog-body flex min-h-0 flex-1">
                    <div
                      class="division-list w-[116px] shrink-0 overflow-y-auto border-r border-[var(--divider-color)] py-[4px]"
                    >
                      <div
                        v-for="d in divisions"
                        :key="d.division"
                        class="division-item cursor-pointer px-[14px] py-[7px]"
                        :class="selectedDivision === d.division ? 'division-item--active' : ''"
                        @click="handleSelectDivision(d.division)"
                      >
                        <div class="truncate text-[12px] font-medium">{{ divisionLabel(d.division) }}</div>
                        <div class="text-[11px] text-[var(--text-n3)]">{{ d.count }}</div>
                      </div>
                    </div>
                    <div class="role-list min-h-0 flex-1 overflow-y-auto px-[14px] pb-[16px] pt-[4px]">
                      <n-spin :show="loadingRoles">
                        <template v-if="roles.length === 0 && !loadingRoles">
                          <n-empty :description="t('aiEmployee.emptyRoles')" class="mt-[48px]" />
                        </template>
                        <div v-else class="flex flex-col gap-[8px]">
                          <n-tooltip
                            v-for="role in roles"
                            :key="role.id"
                            :disabled="!isRoleAdded(role.id)"
                            placement="top"
                          >
                            <template #trigger>
                              <div
                                class="role-card relative flex cursor-pointer items-center gap-[12px] overflow-hidden rounded-[8px] border border-[var(--divider-color)] p-[12px] transition-colors hover:border-[var(--primary-8)]"
                                :class="{ 'role-card--added': isRoleAdded(role.id) }"
                                @click="openCreate(role)"
                              >
                                <span class="text-[22px] leading-none">{{ role.emoji || '🤖' }}</span>
                                <div class="min-w-0 flex-1">
                                  <div class="truncate text-[13px] font-medium text-[var(--text-n1)]">{{
                                    role.name
                                  }}</div>
                                  <div class="text-[12px] leading-[1.5] text-[var(--text-n3)]">{{
                                    role.vibe || role.description
                                  }}</div>
                                </div>
                                <span v-if="isRoleAdded(role.id)" class="added-ribbon">{{
                                  t('aiEmployee.added')
                                }}</span>
                              </div>
                            </template>
                            {{ t('aiEmployee.addedTip') }}
                          </n-tooltip>
                        </div>
                      </n-spin>
                    </div>
                  </div>
                </div>
              </n-tab-pane>

              <n-tab-pane name="agents" :tab="t('aiEmployee.agentsTab')">
                <div class="agent-list flex h-full flex-col gap-[8px] overflow-y-auto px-[16px] pb-[16px]">
                  <template v-if="agents.length === 0">
                    <n-empty :description="t('aiEmployee.emptyAgents')" class="mt-[48px]" />
                  </template>
                  <div
                    v-for="agent in agents"
                    :key="agent.id"
                    class="agent-card rounded-[8px] border border-[var(--divider-color)] p-[10px] transition-colors"
                  >
                    <div class="flex items-center gap-[10px]">
                      <span class="text-[22px] leading-none">{{ agent.employeeEmoji || '🤖' }}</span>
                      <div class="min-w-0 flex-1">
                        <div class="truncate text-[13px] font-medium text-[var(--text-n1)]">{{ agent.name }}</div>
                        <div class="text-[12px] leading-[1.5] text-[var(--text-n3)]">
                          {{ agent.description || agent.employeeDescription }}
                        </div>
                      </div>
                      <div class="flex items-center gap-[4px]" @click.stop>
                        <n-button size="tiny" type="primary" secondary @click="openChat(agent)">
                          {{ t('aiEmployee.chat') }}
                        </n-button>
                        <n-button
                          size="tiny"
                          :type="isAttending(agent.id) ? 'primary' : 'default'"
                          :quaternary="!isAttending(agent.id)"
                          @click="toggleAttend(agent)"
                        >
                          {{ isAttending(agent.id) ? t('aiEmployee.attending') : t('aiEmployee.attend') }}
                        </n-button>
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

              <n-tab-pane name="minutes" :tab="t('aiEmployee.minutesTab')">
                <div class="minutes-list-pane flex h-full flex-col gap-[8px] overflow-y-auto px-[16px] pb-[16px]">
                  <template v-if="minutesList.length === 0">
                    <n-empty :description="t('aiEmployee.minutesEmpty')" class="mt-[48px]" />
                  </template>
                  <div
                    v-for="m in minutesList"
                    :key="m.id"
                    class="minutes-card cursor-pointer rounded-[8px] border border-[var(--divider-color)] p-[12px] transition-colors hover:border-[var(--primary-8)]"
                    @click="openMinutes(m)"
                  >
                    <div class="truncate text-[13px] font-medium text-[var(--text-n1)]">{{ m.title }}</div>
                    <div class="mt-[4px] truncate text-[12px] text-[var(--text-n3)]">{{ m.timeText }}</div>
                  </div>
                </div>
              </n-tab-pane>
            </n-tabs>
          </div>
        </template>

        <template #2>
          <AgentTaskChat v-if="activeChatAgent" :agent="activeChatAgent" @back="activeChatAgent = null" />
          <MeetingRoom v-else :attendees="attendees" @minutesGenerated="onMinutesGenerated" />
        </template>
      </CrmSplitPanel>
    </CrmCard>

    <n-modal
      v-model:show="showCreateModal"
      :mask-closable="false"
      preset="card"
      :title="editingAgentId ? t('aiEmployee.editAgent') : t('aiEmployee.createAgent')"
      class="ai-employee-modal !w-[560px]"
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
        <n-form-item :label="t('aiEmployee.agentDescription')">
          <n-input
            v-model:value="agentForm.description"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            :placeholder="t('aiEmployee.agentDescriptionPlaceholder')"
          />
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

    <MeetingMinutesModal v-model:visible="minutesModalVisible" :minutes="currentMinutes" />
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import {
    NButton,
    NEmpty,
    NForm,
    NFormItem,
    NInput,
    NModal,
    NSpin,
    NSwitch,
    NTabPane,
    NTabs,
    NTooltip,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    AiEmployeeAgent,
    AiEmployeeDetail,
    AiEmployeeDivision,
    AiEmployeeItem,
    AiMeetingMinutes,
  } from '@lib/shared/models/ai';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import AgentTaskChat from './components/AgentTaskChat.vue';
  import MeetingMinutesModal from './components/MeetingMinutesModal.vue';
  import MeetingRoom from './components/MeetingRoom.vue';

  import {
    addAiEmployeeAgent,
    deleteAiEmployeeAgent,
    getAiEmployeeAgents,
    getAiEmployeeCatalog,
    getAiEmployeeDetail,
    getAiEmployeeDivisions,
    getAiMeetingMinutes,
    updateAiEmployeeAgent,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  interface Attendee {
    agentId: string;
    name: string;
    emoji?: string;
    color?: string;
  }

  const DIVISION_LABELS: Record<string, string> = {
    'academic': '学术',
    'company': '公司高管',
    'design': '设计',
    'engineering': '工程研发',
    'finance': '财务',
    'game-development': '游戏开发',
    'gis': '地理信息',
    'hr': '人力资源',
    'legal': '法务合规',
    'marketing': '市场营销',
    'paid-media': '付费投放',
    'product': '产品',
    'project-management': '项目管理',
    'sales': '销售',
    'security': '安全',
    'spatial-computing': '空间计算',
    'specialized': '专项',
    'supply-chain': '供应链',
    'support': '客户支持',
    'testing': '测试',
  };

  // 部门展示顺序：公司管理运营优先，其余按与企业经营的关联度降序
  const DIVISION_ORDER: string[] = [
    'company',
    'hr',
    'finance',
    'legal',
    'supply-chain',
    'sales',
    'marketing',
    'paid-media',
    'product',
    'project-management',
    'support',
    'specialized',
    'engineering',
    'design',
    'security',
    'testing',
    'academic',
    'gis',
    'spatial-computing',
    'game-development',
  ];

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const activeTab = ref<'catalog' | 'agents' | 'minutes'>('catalog');
  const splitSize = ref(0.42);
  const divisions = ref<AiEmployeeDivision[]>([]);
  const selectedDivision = ref('');
  const keyword = ref('');
  const roles = ref<AiEmployeeItem[]>([]);
  const loadingRoles = ref(false);
  const agents = ref<AiEmployeeAgent[]>([]);
  const activeChatAgent = ref<AiEmployeeAgent | null>(null);

  const showCreateModal = ref(false);
  const saving = ref(false);
  const editingAgentId = ref('');
  const currentRole = ref<AiEmployeeDetail | null>(null);
  const agentForm = ref({
    name: '',
    employeeId: '',
    description: '',
    customPrompt: '',
    knowledgeEnabled: false,
  });

  const attendees = ref<Attendee[]>([]);
  const minutesList = ref<AiMeetingMinutes[]>([]);
  const minutesModalVisible = ref(false);
  const currentMinutes = ref<AiMeetingMinutes | null>(null);

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
    const orderIndex = (division: string) => {
      const index = DIVISION_ORDER.indexOf(division);
      return index === -1 ? DIVISION_ORDER.length : index;
    };
    divisions.value = (list || []).slice().sort((a, b) => orderIndex(a.division) - orderIndex(b.division));
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

  function isRoleAdded(employeeId: string) {
    return agents.value.some((a) => a.employeeId === employeeId);
  }

  async function openCreate(role: AiEmployeeItem) {
    if (isRoleAdded(role.id)) {
      Message.warning(t('aiEmployee.addedTip'));
      return;
    }
    editingAgentId.value = '';
    currentRole.value = null;
    agentForm.value = {
      name: role.name,
      employeeId: role.id,
      description: '',
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
      description: agent.description || '',
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
    const isNew = !editingAgentId.value;
    const newName = agentForm.value.name.trim();
    const newEmployeeId = agentForm.value.employeeId;
    try {
      const payload = {
        id: editingAgentId.value || undefined,
        name: newName,
        employeeId: newEmployeeId,
        description: agentForm.value.description,
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
      // 新建岗位后自动切到「我的岗位」
      if (isNew) {
        activeTab.value = 'agents';
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      Message.error((error as Error)?.message || t('common.operationFailed'));
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
        // 若该岗位已参会，同步移除
        attendees.value = attendees.value.filter((a) => a.agentId !== agent.id);
        // 若正在与该岗位对话，关闭对话面板
        if (activeChatAgent.value?.id === agent.id) {
          activeChatAgent.value = null;
        }
        await loadAgents();
      },
    });
  }

  function isAttending(agentId: string) {
    return attendees.value.some((a) => a.agentId === agentId);
  }

  function openChat(agent: AiEmployeeAgent) {
    activeChatAgent.value = agent;
  }

  function toggleAttend(agent: AiEmployeeAgent) {
    const idx = attendees.value.findIndex((a) => a.agentId === agent.id);
    if (idx >= 0) {
      attendees.value.splice(idx, 1);
    } else {
      attendees.value.push({
        agentId: agent.id,
        name: agent.name,
        emoji: agent.employeeEmoji,
        color: agent.employeeColor,
      });
    }
  }

  async function loadMinutes() {
    try {
      minutesList.value = await getAiMeetingMinutes();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function openMinutes(minutes: AiMeetingMinutes) {
    currentMinutes.value = minutes;
    minutesModalVisible.value = true;
  }

  function onMinutesGenerated(minutes: AiMeetingMinutes) {
    attendees.value = [];
    currentMinutes.value = minutes;
    minutesModalVisible.value = true;
    loadMinutes();
  }

  onMounted(() => {
    loadDivisions();
    loadAgents();
    loadMinutes();
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
  .ai-employee-tabs {
    display: flex;
    overflow: hidden;
    min-height: 0;
    flex-direction: column;
    :deep(.n-tabs-nav) {
      flex-shrink: 0;
    }
    :deep(.n-tab-pane) {
      overflow: hidden;
      min-height: 0;
      flex: 1;
    }
  }
  .division-list {
    scrollbar-width: none;
    &::-webkit-scrollbar {
      display: none;
    }
  }
  .role-list {
    scrollbar-width: thin;
    scrollbar-color: var(--text-n8) transparent;
    &::-webkit-scrollbar {
      width: 6px;
    }
    &::-webkit-scrollbar-track {
      background: transparent;
    }
    &::-webkit-scrollbar-thumb {
      border-radius: 99px;
      background: var(--text-n8);
    }
    &::-webkit-scrollbar-thumb:hover {
      background: var(--text-n6);
    }
  }
  .division-item {
    color: var(--text-n2);
    transition: background 0.2s;
    &:hover {
      background: var(--fill-1);
    }
    &--active {
      color: var(--primary-8);
      background: rgb(249 115 22 / 8%);
      box-shadow: inset 2px 0 0 var(--primary-8);
    }
  }
  .role-card,
  .agent-card {
    &:hover {
      background: var(--fill-1);
    }
  }
  .role-card--added {
    border-color: #b9e7cb;
  }
  .added-ribbon {
    position: absolute;
    top: 10px;
    right: -27px;
    z-index: 1;
    padding: 3px 0;
    width: 92px;
    font-size: 10.5px;
    font-weight: 600;
    text-align: center;
    color: #ffffff;
    background: linear-gradient(135deg, #4ade80, #16a34a);
    box-shadow: 0 1px 3px rgb(22 163 74 / 40%);
    line-height: 1.5;
    letter-spacing: 1px;
    transform: rotate(45deg);
    pointer-events: none;
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
