<!-- AI 工作台页 -->
<template>
  <div class="ai-workbench-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">AI Workbench</p>
        <h1 class="page-title">AI 工作台</h1>
        <p class="page-subtitle">集中查看模型链路、维度健康状态，并在同一工作区完成 RAG 查询预览与向量维护操作。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="loadWorkbenchData">刷新工作台</el-button>
        <el-button type="primary" @click="goToKnowledge">前往知识库管理</el-button>
      </div>
    </section>

    <div v-if="errorMessage" class="error-state page-error-state">
      <el-result icon="error" title="AI 工作台加载失败" :sub-title="errorMessage">
        <template #extra>
          <el-button type="primary" :loading="loading" @click="loadWorkbenchData">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <template v-else>
      <section class="workbench-top-grid" v-loading="loading">
        <el-card shadow="hover" class="workbench-card chain-card">
          <template #header>
            <div class="card-header">
              <span>模型链路</span>
            </div>
          </template>

          <div class="chain-grid">
            <div class="chain-node">
              <div class="chain-node__label">聊天链路</div>
              <div class="chain-node__value">{{ vectorStatus.chatProvider || '--' }}</div>
              <div class="chain-node__desc">{{ vectorStatus.chatModel || '--' }}</div>
            </div>
            <div class="chain-node chain-node--accent">
              <div class="chain-node__label">向量链路</div>
              <div class="chain-node__value">{{ vectorStatus.embeddingProvider || '--' }}</div>
              <div class="chain-node__desc">{{ vectorStatus.embeddingModel || '--' }}</div>
            </div>
            <div class="chain-node">
              <div class="chain-node__label">Redis 索引</div>
              <div class="chain-node__value chain-node__value--sm">{{ vectorStatus.indexName || '--' }}</div>
              <div class="chain-node__desc">{{ vectorStatus.redisHost || '--' }}:{{ vectorStatus.redisPort || '--' }}</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="workbench-card health-card">
          <template #header>
            <div class="card-header">
              <span>维度健康</span>
            </div>
          </template>

          <div class="health-state" :class="{ 'health-state--ok': vectorStatus.dimensionMatched, 'health-state--danger': vectorStatus.dimensionMatched === false }">
            <div class="health-state__title">
              {{ vectorStatus.dimensionMatched === null ? '未检测到索引维度' : vectorStatus.dimensionMatched ? '当前维度一致' : '检测到维度冲突' }}
            </div>
            <div class="health-state__meta">
              <span>模型 {{ displayAiMetric(vectorStatus.modelDimension) }} 维</span>
              <span>索引 {{ displayAiMetric(vectorStatus.indexDimension) }} 维</span>
            </div>
            <div class="health-state__desc">
              {{ vectorStatus.dimensionMatched === false
                ? '请优先执行“清空后重建”，避免请求阶段出现向量维度不匹配。'
                : '如果切换了 embedding 模型，仍建议在这里确认维度后再开放 RAG 请求。' }}
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="workbench-card metrics-card">
          <template #header>
            <div class="card-header">
              <span>索引进度</span>
            </div>
          </template>

          <div class="mini-metrics">
            <div class="mini-metric">
              <span>总分片</span>
              <strong>{{ displayAiMetric(vectorStatus.totalChunkCount) }}</strong>
            </div>
            <div class="mini-metric">
              <span>待处理</span>
              <strong>{{ displayAiMetric(vectorStatus.pendingChunkCount) }}</strong>
            </div>
            <div class="mini-metric">
              <span>已完成</span>
              <strong>{{ displayAiMetric(vectorStatus.completedChunkCount) }}</strong>
            </div>
            <div class="mini-metric">
              <span>失败</span>
              <strong>{{ displayAiMetric(vectorStatus.failedChunkCount) }}</strong>
            </div>
          </div>
        </el-card>
      </section>

      <el-row :gutter="24" class="content-row">
        <el-col :xl="10" :lg="10" :md="24">
          <el-card shadow="hover" class="workbench-card maintenance-card">
            <template #header>
              <div class="card-header">
                <span>运维操作</span>
              </div>
            </template>

            <div class="maintenance-actions">
              <el-button type="warning" plain :loading="clearingVector" @click="handleClearVectorIndex">清空向量</el-button>
              <el-button type="primary" plain :loading="rebuildingAll" @click="handleRebuildAll">重建全部</el-button>
              <el-button type="danger" :loading="clearingAndRebuilding" @click="handleClearAndRebuild">清空后重建</el-button>
            </div>

            <div class="maintenance-result" v-if="maintenanceSummary.message">
              <div class="maintenance-result__title">最近维护结果</div>
              <div class="maintenance-result__message">{{ maintenanceSummary.message }}</div>
              <div class="maintenance-result__meta">
                <span>清理向量 {{ maintenanceSummary.clearedVectorCount ?? 0 }}</span>
                <span>重建文档 {{ maintenanceSummary.rebuiltDocumentCount ?? 0 }}</span>
                <span>重建分片 {{ maintenanceSummary.rebuiltChunkCount ?? 0 }}</span>
              </div>
            </div>

            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="工作台适合在切换 embedding 后先做状态确认，再进行重建与 RAG 测试。"
            />
          </el-card>
        </el-col>

        <el-col :xl="14" :lg="14" :md="24">
          <el-card shadow="hover" class="workbench-card preview-card" v-loading="previewing">
            <template #header>
              <div class="card-header">
                <span>RAG 查询预览</span>
                <div class="card-header__actions">
                  <el-button type="primary" :loading="previewing" @click="handlePreview">开始预览</el-button>
                </div>
              </div>
            </template>

            <el-form label-position="top" @submit.prevent>
              <div class="preview-form-grid">
                <el-form-item label="场景">
                  <el-select v-model="previewForm.scenario" class="w-full" placeholder="请选择场景">
                    <el-option
                      v-for="item in AI_SCENARIO_OPTIONS"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    >
                      <div class="scenario-option">
                        <span>{{ item.label }}</span>
                        <span class="scenario-option__domain">{{ getDomainLabel(item.domain) }}</span>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>

                <el-form-item label="知识域">
                  <div class="domain-pill">{{ selectedScenarioDomainLabel }}</div>
                </el-form-item>
              </div>

              <el-form-item label="测试问题">
                <el-input
                  v-model="previewForm.query"
                  type="textarea"
                  :rows="6"
                  resize="vertical"
                  maxlength="500"
                  show-word-limit
                  placeholder="例如：退款流程是什么？"
                  @keyup.ctrl.enter="handlePreview"
                />
              </el-form-item>
            </el-form>

            <el-result
              v-if="previewErrorMessage"
              icon="error"
              title="查询测试失败"
              :sub-title="previewErrorMessage"
            />
            <el-empty
              v-else-if="!hasSubmitted"
              description="输入问题后，可在这里查看当前 RAG 链路的命中结果。"
              :image-size="72"
            />
            <el-empty
              v-else-if="!previewResult || !previewResult.hitCount"
              description="当前查询没有命中知识分片。"
              :image-size="72"
            />
            <div v-else class="preview-result">
              <div class="preview-summary">
                <div class="preview-summary__item">
                  <span>命中知识域</span>
                  <strong>{{ previewDomainLabel }}</strong>
                </div>
                <div class="preview-summary__item">
                  <span>命中数量</span>
                  <strong>{{ previewResult.hitCount }}</strong>
                </div>
              </div>

              <div class="preview-hit-list">
                <div v-for="item in previewHits" :key="`${item.documentId}-${item.chunkId}`" class="preview-hit-card">
                  <div class="preview-hit-card__head">
                    <div>
                      <div class="preview-hit-card__title">{{ item.title || '未命名文档' }}</div>
                      <div class="preview-hit-card__meta">
                        <span>文档 {{ item.documentId ?? '--' }}</span>
                        <span>分片 {{ item.chunkId ?? '--' }}</span>
                        <span>{{ item.sourceType || '--' }}</span>
                      </div>
                    </div>
                    <el-tag effect="plain" round>{{ getDomainLabel(item.knowledgeDomain) }}</el-tag>
                  </div>
                  <div class="preview-hit-card__snippet">{{ item.snippet || '暂无片段内容' }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  clearAiVectorIndex,
  clearAndRebuildAiVectorIndex,
  getAiVectorIndexStatus,
  previewAiKnowledge,
  rebuildAllAiKnowledge
} from '@/modules/ai-service/api.js'
import { AI_KNOWLEDGE_DOMAIN_LABELS, AI_PREVIEW_DEFAULT_SCENARIO, AI_SCENARIO_OPTIONS } from '@/modules/ai-service/constants.js'
import {
  buildEmptyAiPreviewResult,
  createEmptyAiMaintenanceSummary,
  createEmptyAiVectorStatus,
  displayAiMetric,
  extractAiErrorMessage,
  formatAiPreviewDomains
} from '@/modules/ai-service/utils.js'

// 工作台默认落到订单顾问，方便直接联调订单边界与售后知识。
const router = useRouter()
const DEFAULT_SCENARIO = AI_PREVIEW_DEFAULT_SCENARIO

// 状态区、预览区和运维区各自维护 loading，避免一个动作拖慢整个工作台。
const loading = ref(false)
const previewing = ref(false)
const rebuildingAll = ref(false)
const clearingVector = ref(false)
const clearingAndRebuilding = ref(false)
const errorMessage = ref('')
const previewErrorMessage = ref('')
const hasSubmitted = ref(false)
const previewResult = ref(null)
const vectorStatus = ref(createEmptyAiVectorStatus())
const maintenanceSummary = reactive(createEmptyAiMaintenanceSummary())

// 预览表单只保留当前场景和问题文本，减少工作台调试心智负担。
const previewForm = reactive({
  scenario: DEFAULT_SCENARIO,
  query: ''
})

const getDomainLabel = (value) => AI_KNOWLEDGE_DOMAIN_LABELS[value] || value || '未分类'

// 场景切换后，知识域提示要实时跟着更新，方便判断当前查询会落到哪个域。
const selectedScenarioOption = computed(() => {
  return AI_SCENARIO_OPTIONS.find(item => item.value === previewForm.scenario) || null
})

const selectedScenarioDomainLabel = computed(() => {
  return getDomainLabel(selectedScenarioOption.value?.domain)
})

const previewHits = computed(() => {
  return Array.isArray(previewResult.value?.hits) ? previewResult.value.hits : []
})

const previewDomainLabel = computed(() => {
  return formatAiPreviewDomains(previewResult.value?.domains, previewResult.value?.domain, AI_KNOWLEDGE_DOMAIN_LABELS)
})

// 维护结果统一回写到同一块结果区，方便对比最近一次运维动作的效果。
const syncMaintenanceSummary = (payload) => {
  Object.assign(maintenanceSummary, createEmptyAiMaintenanceSummary(), payload || {})
}

// 工作台主数据先只拉向量状态，避免把文档列表等非必要数据一起带进来。
const loadWorkbenchData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const res = await getAiVectorIndexStatus()
    vectorStatus.value = {
      ...createEmptyAiVectorStatus(),
      ...(res?.data || {})
    }
  } catch (error) {
    vectorStatus.value = createEmptyAiVectorStatus()
    errorMessage.value = extractAiErrorMessage(error, 'AI 工作台状态加载失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

// RAG 预览和独立查询测试页保持同一套兜底结构，避免两个页面展示口径不同。
const handlePreview = async () => {
  const query = previewForm.query.trim()

  hasSubmitted.value = true
  previewErrorMessage.value = ''
  previewResult.value = null

  if (!query) {
    previewErrorMessage.value = '请输入测试问题'
    return
  }

  previewing.value = true
  try {
    const res = await previewAiKnowledge({
      scenario: previewForm.scenario,
      query
    })
    previewResult.value = res?.data || buildEmptyAiPreviewResult({
      query,
      scenario: previewForm.scenario,
      domain: selectedScenarioOption.value?.domain || ''
    })
  } catch (error) {
    previewErrorMessage.value = extractAiErrorMessage(error, '命中预览失败，请稍后重试。')
  } finally {
    previewing.value = false
  }
}

// 清空向量适合在确认索引污染或准备彻底重建前使用。
const handleClearVectorIndex = async () => {
  try {
    await ElMessageBox.confirm(
      '确认清空当前知识向量吗？该操作会保留文档与分片记录，但会移除 Redis 中的向量数据。',
      '清空 AI 向量',
      {
        type: 'warning',
        confirmButtonText: '确认清空',
        cancelButtonText: '取消'
      }
    )
  } catch (_error) {
    return
  }

  clearingVector.value = true
  try {
    const res = await clearAiVectorIndex()
    syncMaintenanceSummary(res?.data)
    ElMessage.success(`已清空 ${Number(res?.data?.clearedVectorCount || 0)} 条向量数据`)
    await loadWorkbenchData()
  } catch (error) {
    ElMessage.error(extractAiErrorMessage(error, '向量清空失败'))
  } finally {
    clearingVector.value = false
  }
}

// 重建全部只按当前启用知识执行，避免把停用文档重新写回向量库。
const handleRebuildAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确认按当前 embedding 模型重建全部启用知识吗？',
      '重建全部知识',
      {
        type: 'warning',
        confirmButtonText: '确认重建',
        cancelButtonText: '取消'
      }
    )
  } catch (_error) {
    return
  }

  rebuildingAll.value = true
  try {
    const res = await rebuildAllAiKnowledge()
    syncMaintenanceSummary(res?.data)
    ElMessage.success(res?.data?.message || '已触发全部知识重建')
    await loadWorkbenchData()
  } catch (error) {
    ElMessage.error(extractAiErrorMessage(error, '全部重建失败'))
  } finally {
    rebuildingAll.value = false
  }
}

// 清空后重建用于 embedding 切换后的彻底刷新，是工作台里风险最高的维护动作。
const handleClearAndRebuild = async () => {
  try {
    await ElMessageBox.confirm(
      '确认执行“清空后重建”吗？该操作会先删除现有向量，再按当前 embedding 模型重建索引。',
      '清空后重建 AI 向量',
      {
        type: 'warning',
        confirmButtonText: '确认执行',
        cancelButtonText: '取消'
      }
    )
  } catch (_error) {
    return
  }

  clearingAndRebuilding.value = true
  try {
    const res = await clearAndRebuildAiVectorIndex()
    syncMaintenanceSummary(res?.data)
    ElMessage.success(res?.data?.message || '已完成清空并重建')
    await loadWorkbenchData()
  } catch (error) {
    ElMessage.error(extractAiErrorMessage(error, '清空后重建失败'))
  } finally {
    clearingAndRebuilding.value = false
  }
}

// 知识库管理页承接更细的文档级维护，所以从工作台直接跳过去。
const goToKnowledge = () => router.push('/ai-service/knowledge')

onMounted(() => {
  loadWorkbenchData()
})
</script>

<style lang="scss" scoped>
.ai-workbench-page {
  .workbench-top-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.3fr) minmax(0, 0.9fr) minmax(0, 0.9fr);
    gap: 16px;
  }

  .workbench-card {
    height: 100%;
  }

  .card-header,
  .card-header__actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .card-header {
    justify-content: space-between;
  }

  .chain-grid,
  .mini-metrics {
    display: grid;
    gap: 12px;
  }

  .chain-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chain-node,
  .mini-metric,
  .preview-hit-card {
    padding: 18px;
    border-radius: 16px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .chain-node--accent {
    border-color: color-mix(in srgb, #2563eb 38%, var(--wt-border-default));
  }

  .chain-node__label,
  .mini-metric span,
  .preview-hit-card__meta,
  .scenario-option__domain,
  .health-state__meta,
  .maintenance-result__title,
  .maintenance-result__meta {
    font-size: 12px;
    color: var(--wt-text-secondary);
  }

  .chain-node__value,
  .health-state__title,
  .preview-summary__item strong {
    margin-top: 8px;
    font-size: 22px;
    font-weight: 700;
    color: var(--wt-text-primary);
    line-height: 1.3;
  }

  .chain-node__value--sm {
    font-size: 16px;
    word-break: break-word;
  }

  .chain-node__desc,
  .health-state__desc,
  .maintenance-result__message,
  .preview-hit-card__snippet {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--wt-text-regular);
    word-break: break-word;
  }

  .health-state {
    padding: 20px;
    border-radius: 18px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, color-mix(in srgb, #0f172a 92%, white) 0%, color-mix(in srgb, #1e293b 92%, white) 100%);
  }

  .health-state--ok {
    border-color: color-mix(in srgb, #22c55e 42%, var(--wt-border-default));
  }

  .health-state--danger {
    border-color: color-mix(in srgb, #f43f5e 46%, var(--wt-border-default));
  }

  .health-state__title,
  .health-state__meta,
  .health-state__desc {
    color: #f8fafc;
  }

  .mini-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mini-metric strong {
    display: block;
    margin-top: 8px;
    font-size: 24px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .maintenance-actions,
  .maintenance-result,
  .preview-result,
  .preview-hit-list {
    display: grid;
    gap: 16px;
  }

  .maintenance-actions {
    margin-bottom: 16px;

    .el-button {
      margin-left: 0;
    }
  }

  .maintenance-result {
    padding: 16px;
    border-radius: 16px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .maintenance-result__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
  }

  .preview-form-grid,
  .preview-summary {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .w-full {
    width: 100%;
  }

  .domain-pill,
  .preview-summary__item {
    padding: 14px 16px;
    border-radius: 14px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
    min-height: 52px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .domain-pill,
  .preview-hit-card__title {
    font-size: 14px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .scenario-option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .preview-hit-card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .preview-hit-card__meta {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px 14px;
  }

  .preview-hit-card__snippet {
    padding: 14px 16px;
    border-radius: 12px;
    background: var(--wt-overlay-bg);
    white-space: pre-wrap;
  }

  @media (max-width: 1200px) {
    .workbench-top-grid,
    .chain-grid,
    .preview-form-grid,
    .preview-summary {
      grid-template-columns: 1fr;
    }

    .mini-metrics {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .mini-metrics {
      grid-template-columns: 1fr;
    }
  }
}
</style>
