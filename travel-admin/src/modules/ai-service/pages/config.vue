<!-- AI 配置管理页 -->
<template>
  <div class="ai-config-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">AI Scenario Config</p>
        <h1 class="page-title">AI 配置管理</h1>
        <p class="page-subtitle">面向一期 AI 客服场景的轻量配置视图，当前仅支持本地编辑与页面内预览，不涉及模型、提示词平台或后端持久化。</p>
      </div>
      <div class="hero-actions">
        <el-button @click="handleResetLocalConfig">重置本地修改</el-button>
      </div>
    </section>

    <el-alert
      type="info"
      show-icon
      :closable="false"
      class="page-alert"
      title="一期轻量配置说明"
      description="本页用于帮助运营和开发快速确认场景启停、知识域归属、场景说明与示例问题。当前修改仅保存在本页面运行时内，刷新后会恢复为 constants.js 中的默认配置。"
    />

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card summary-card--teal">
        <div class="summary-label">场景总数</div>
        <div class="summary-value">{{ summaryMetrics.total }}</div>
        <div class="summary-desc">当前一期接入的 AI 服务场景总量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card summary-card--blue">
        <div class="summary-label">已启用场景</div>
        <div class="summary-value">{{ summaryMetrics.enabled }}</div>
        <div class="summary-desc">当前允许被前台接入和测试的问题场景数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card summary-card--rose">
        <div class="summary-label">已停用场景</div>
        <div class="summary-value">{{ summaryMetrics.disabled }}</div>
        <div class="summary-desc">当前仅保留配置、暂未开放使用的场景数量</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">
      <template #header>
        <div class="card-header">
          <span>场景配置卡片</span>
          <span class="card-header__hint">配置来源：AI_SCENARIO_CONFIGS</span>
        </div>
      </template>

      <div class="config-grid">
        <ConfigScenarioCard
          v-for="item in scenarioConfigs"
          :key="item.key"
          :scenario="item"
          :domain-label="getDomainLabel(item.knowledgeDomain)"
          @edit="openEditDialog"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      width="640px"
      destroy-on-close
      title="编辑本地场景配置"
    >
      <template v-if="editingScenario">
        <div class="dialog-intro">
          <div class="dialog-intro__title">{{ editingScenario.title }}</div>
          <div class="dialog-intro__meta">
            <span>场景编码：{{ editingScenario.scenario }}</span>
            <span>当前为本地草稿编辑，不会提交后端</span>
          </div>
        </div>

        <el-form ref="formRef" :model="editForm" :rules="rules" label-position="top">
          <el-row :gutter="20">
            <el-col :md="12" :sm="24">
              <el-form-item label="是否启用" prop="enabled">
                <el-switch v-model="editForm.enabled" inline-prompt active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-col>
            <el-col :md="12" :sm="24">
              <el-form-item label="知识域" prop="knowledgeDomain">
                <el-select v-model="editForm.knowledgeDomain" class="w-full" placeholder="请选择知识域">
                  <el-option
                    v-for="item in knowledgeDomainOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="场景说明" prop="description">
            <el-input
              v-model="editForm.description"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              resize="vertical"
              placeholder="请输入面向一期客服场景的简要说明"
            />
          </el-form-item>

          <el-form-item label="示例问题" prop="exampleQuestion">
            <el-input
              v-model="editForm.exampleQuestion"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              resize="vertical"
              placeholder="请输入一个用于说明场景范围的典型问题"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitEdit">保存本地修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ConfigScenarioCard from '@/modules/ai-service/components/ConfigScenarioCard.vue'
import { AI_KNOWLEDGE_DOMAIN_LABELS, AI_SCENARIO_CONFIGS } from '@/modules/ai-service/constants.js'

// 配置页当前只维护前端本地草稿，不涉及后端持久化。
const formRef = ref()
const dialogVisible = ref(false)
const editingScenarioKey = ref('')
const scenarioConfigs = ref(cloneScenarioConfigs())

const editForm = reactive(createEmptyEditForm())

// 本地编辑规则只覆盖当前页面真正可改的几个字段。
const rules = {
  knowledgeDomain: [
    { required: true, message: '请选择知识域', trigger: 'change' }
  ],
  description: [
    {
      validator: (_rule, value, callback) => {
        if (!value || !value.trim()) {
          callback(new Error('请输入场景说明'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  exampleQuestion: [
    {
      validator: (_rule, value, callback) => {
        if (!value || !value.trim()) {
          callback(new Error('请输入示例问题'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

// 每次恢复默认配置都重新克隆常量，避免直接污染源配置对象。
function cloneScenarioConfigs() {
  return AI_SCENARIO_CONFIGS.map(item => ({ ...item }))
}

// 编辑弹窗总是从空表单初始化，避免上一次编辑残留值串进下一次。
function createEmptyEditForm() {
  return {
    enabled: true,
    knowledgeDomain: '',
    description: '',
    exampleQuestion: ''
  }
}

const knowledgeDomainOptions = computed(() => {
  return Object.entries(AI_KNOWLEDGE_DOMAIN_LABELS).map(([value, label]) => ({ value, label }))
})

// 顶部指标卡只看当前页面草稿状态，不区分是否已同步到后端。
const summaryMetrics = computed(() => {
  const total = scenarioConfigs.value.length
  const enabled = scenarioConfigs.value.filter(item => item.enabled).length

  return {
    total,
    enabled,
    disabled: total - enabled
  }
})

// 当前正在编辑的场景通过 key 映射，避免把整条对象副本散落到多个地方。
const editingScenario = computed(() => {
  return scenarioConfigs.value.find(item => item.key === editingScenarioKey.value) || null
})

const getDomainLabel = (value) => AI_KNOWLEDGE_DOMAIN_LABELS[value] || value || '未分类'

// 打开弹窗时直接把当前卡片数据同步进草稿表单，保持所见即所得。
const openEditDialog = (scenario) => {
  editingScenarioKey.value = scenario.key
  editForm.enabled = Boolean(scenario.enabled)
  editForm.knowledgeDomain = scenario.knowledgeDomain || ''
  editForm.description = scenario.description || ''
  editForm.exampleQuestion = scenario.exampleQuestion || ''
  dialogVisible.value = true
}

// 保存时只回写当前页面内存态配置，不做接口提交。
const handleSubmitEdit = async () => {
  try {
    await formRef.value?.validate()

    const target = scenarioConfigs.value.find(item => item.key === editingScenarioKey.value)
    if (!target) {
      ElMessage.error('未找到待编辑的场景配置')
      return
    }

    target.enabled = editForm.enabled
    target.knowledgeDomain = editForm.knowledgeDomain
    target.description = editForm.description.trim()
    target.exampleQuestion = editForm.exampleQuestion.trim()

    dialogVisible.value = false
    ElMessage.success('本地配置已更新')
  } catch (_error) {
    // 交由表单项展示校验信息，避免重复提示。
  }
}

// 重置会完全回到 constants 中的默认定义，清掉本页所有本地草稿改动。
const handleResetLocalConfig = () => {
  scenarioConfigs.value = cloneScenarioConfigs()
  dialogVisible.value = false
  editingScenarioKey.value = ''
  Object.assign(editForm, createEmptyEditForm())
  ElMessage.success('已恢复为默认场景配置')
}
</script>

<style lang="scss" scoped>
.ai-config-page {
  .page-alert {
    margin-bottom: 20px;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 20px;
  }

  .summary-card,
  .management-card {
    border-radius: 20px;
    border: 1px solid var(--wt-border-default);
  }

  .summary-label,
  .card-header__hint,
  .dialog-intro__meta {
    font-size: 12px;
    color: var(--wt-text-secondary);
  }

  .summary-value {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .summary-desc {
    margin-top: 10px;
    font-size: 12px;
    line-height: 1.7;
    color: var(--wt-text-regular);
  }

  .summary-card--teal {
    background: linear-gradient(135deg, color-mix(in srgb, #14b8a6 16%, white) 0%, var(--wt-surface-elevated) 100%);
  }

  .summary-card--blue {
    background: linear-gradient(135deg, color-mix(in srgb, #60a5fa 18%, white) 0%, var(--wt-surface-elevated) 100%);
  }

  .summary-card--rose {
    background: linear-gradient(135deg, color-mix(in srgb, #fb7185 18%, white) 0%, var(--wt-surface-elevated) 100%);
  }

  .card-header,
  .dialog-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .config-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .dialog-intro {
    margin-bottom: 18px;
    padding: 16px 18px;
    border-radius: 14px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .dialog-intro__title {
    font-size: 16px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .dialog-intro__meta {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
    line-height: 1.7;
  }

  .w-full {
    width: 100%;
  }

  @media (max-width: 1200px) {
    .summary-grid,
    .config-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
