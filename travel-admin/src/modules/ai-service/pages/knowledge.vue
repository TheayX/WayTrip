<!-- AI 知识库管理页 -->
<template>
  <div class="knowledge-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">AI Knowledge Base</p>
        <h1 class="page-title">知识库管理</h1>
        <p class="page-subtitle">集中维护 AI 客服一期知识文档，支持筛选、详情查看、编辑与重建操作。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading || rebuildingAll" @click="loadDocuments">刷新数据</el-button>
        <el-button type="primary" @click="openCreateDrawer">新增文档</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card summary-card--teal">
        <div class="summary-label">文档总数</div>
        <div class="summary-value">{{ summaryMetrics.total }}</div>
        <div class="summary-desc">当前知识库中已接入的文档总量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card summary-card--blue">
        <div class="summary-label">启用文档</div>
        <div class="summary-value">{{ summaryMetrics.enabled }}</div>
        <div class="summary-desc">当前允许被 AI 检索引用的文档数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card summary-card--purple">
        <div class="summary-label">分片总数</div>
        <div class="summary-value">{{ summaryMetrics.totalChunks }}</div>
        <div class="summary-desc">所有文档拆分后的知识分片累计规模</div>
      </el-card>
      <el-card shadow="hover" class="summary-card summary-card--rose">
        <div class="summary-label">筛选结果</div>
        <div class="summary-value">{{ filteredDocuments.length }}</div>
        <div class="summary-desc">符合当前筛选条件的知识文档数量</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="admin-management-card management-card">
      <template #header>
        <div class="card-header">
          <span>知识文档管理</span>
          <div class="card-header__actions">
            <el-button type="warning" plain :loading="rebuildingAll" @click="handleRebuildAll">重建全部</el-button>
          </div>
        </div>
      </template>

      <el-form :model="filters" inline class="search-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            class="form-w-220"
            placeholder="搜索标题或来源标识"
          />
        </el-form-item>
        <el-form-item label="知识域">
          <el-select v-model="filters.knowledgeDomain" clearable class="form-w-180" placeholder="全部知识域">
            <el-option v-for="item in knowledgeDomainOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="filters.sourceType" clearable class="form-w-160" placeholder="全部来源">
            <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="filters.enabled" clearable class="form-w-160" placeholder="全部状态">
            <el-option v-for="item in enabledOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="resetFilters">重置筛选</el-button>
        </el-form-item>
      </el-form>

      <div class="domain-overview">
        <div v-for="item in domainSummaryList" :key="item.domain" class="domain-overview__item">
          <div class="domain-overview__title">{{ item.label }}</div>
          <div class="domain-overview__meta">{{ item.enabledCount }}/{{ item.totalCount }} 启用 · {{ item.totalChunkCount }} 分片</div>
        </div>
      </div>

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="知识库加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" :loading="loading" @click="loadDocuments">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <template v-else>
        <el-table
          :data="filteredDocuments"
          v-loading="loading"
          class="knowledge-table"
          empty-text="当前条件下暂无知识文档"
        >
          <el-table-column prop="title" label="文档标题" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="document-title-cell">
                <span class="document-title">{{ row.title }}</span>
                <span class="document-subline">{{ row.sourceRef || '无来源标识' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="知识域" min-width="120">
            <template #default="{ row }">
              <el-tag effect="plain" round>{{ row.domainLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源类型" width="120" align="center">
            <template #default="{ row }">
              {{ row.sourceTypeLabel }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag effect="light" round :type="row.isEnabled ? 'success' : 'info'">
                {{ row.isEnabled ? '启用中' : '已停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="分片数" width="100" align="center" />
          <el-table-column label="版本" width="90" align="center">
            <template #default="{ row }">v{{ row.version }}</template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180" align="center" />
          <el-table-column label="操作" width="280" fixed="right" align="left" header-align="center">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="handleView(row)">查看</el-button>
                <el-button link type="warning" @click="handleEdit(row)">编辑</el-button>
                <el-button
                  link
                  :type="row.isEnabled ? 'danger' : 'success'"
                  :loading="Boolean(toggleLoadingMap[row.id])"
                  @click="handleToggleEnabled(row)"
                >
                  {{ row.isEnabled ? '停用' : '启用' }}
                </el-button>
                <el-button
                  link
                  type="info"
                  :loading="Boolean(rebuildLoadingMap[row.id])"
                  @click="handleRebuild(row)"
                >
                  重建
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>

    <KnowledgeDetailDrawer
      v-model:visible="detailVisible"
      :loading="detailLoading"
      :detail="currentDetail"
      :get-domain-label="getDomainLabel"
      :get-source-type-label="getSourceTypeLabel"
    />

    <KnowledgeFormDrawer
      v-model:visible="formVisible"
      :mode="formMode"
      :submitting="formSubmitting"
      :form-data="formData"
      :knowledge-domain-options="knowledgeDomainOptions"
      :source-type-options="sourceTypeOptions"
      @submit="handleSubmitForm"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAiKnowledgeDocument,
  getAiKnowledgeDocumentDetail,
  getAiKnowledgeDocuments,
  rebuildAiKnowledgeDocument,
  rebuildAllAiKnowledge,
  updateAiKnowledgeDocument,
  updateAiKnowledgeDocumentEnabled
} from '@/modules/ai-service/api.js'
import { AI_KNOWLEDGE_DOMAIN_LABELS } from '@/modules/ai-service/constants.js'
import KnowledgeDetailDrawer from '@/modules/ai-service/components/KnowledgeDetailDrawer.vue'
import KnowledgeFormDrawer from '@/modules/ai-service/components/KnowledgeFormDrawer.vue'

const MANUAL_SOURCE_TYPE = 'manual'

const sourceTypeLabelMap = {
  manual: '手工录入',
  faq: 'FAQ',
  policy: '规则文档',
  guide: '攻略内容',
  spot: '景点资料'
}

const loading = ref(false)
const detailLoading = ref(false)
const rebuildingAll = ref(false)
const formSubmitting = ref(false)
const errorMessage = ref('')
const documents = ref([])
const detailVisible = ref(false)
const formVisible = ref(false)
const formMode = ref('create')
const currentEditId = ref(null)
const currentDetail = ref(null)
const toggleLoadingMap = reactive({})
const rebuildLoadingMap = reactive({})

const filters = reactive({
  keyword: '',
  knowledgeDomain: '',
  sourceType: '',
  enabled: ''
})

const formData = reactive(createEmptyForm())

const knowledgeDomainOptions = computed(() => {
  return Object.entries(AI_KNOWLEDGE_DOMAIN_LABELS).map(([value, label]) => ({ value, label }))
})

const sourceTypeOptions = [
  { value: 'manual', label: '手工录入' },
  { value: 'faq', label: 'FAQ' },
  { value: 'policy', label: '规则文档' },
  { value: 'guide', label: '攻略内容' },
  { value: 'spot', label: '景点资料' }
]

const enabledOptions = [
  { value: 'enabled', label: '仅看启用' },
  { value: 'disabled', label: '仅看停用' }
]

function createEmptyForm() {
  return {
    title: '',
    knowledgeDomain: 'PLATFORM_POLICY',
    sourceType: MANUAL_SOURCE_TYPE,
    sourceRef: '',
    tags: '',
    content: ''
  }
}

const normalizedDocuments = computed(() => {
  return (documents.value || []).map((item, index) => {
    const title = item.title || '未命名文档'
    const sourceRef = item.sourceRef || ''
    const knowledgeDomain = item.knowledgeDomain || 'UNKNOWN'
    const sourceType = item.sourceType || MANUAL_SOURCE_TYPE

    return {
      id: item.id ?? `${title}-${index}`,
      title,
      knowledgeDomain,
      domainLabel: getDomainLabel(knowledgeDomain),
      sourceType,
      sourceTypeLabel: getSourceTypeLabel(sourceType),
      sourceRef,
      version: Number(item.version) || 1,
      isEnabled: Number(item.isEnabled) === 1,
      chunkCount: Number(item.chunkCount) || 0,
      updatedAt: item.updatedAt || '--'
    }
  })
})

const filteredDocuments = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()

  return normalizedDocuments.value.filter((item) => {
    if (filters.knowledgeDomain && item.knowledgeDomain !== filters.knowledgeDomain) {
      return false
    }

    if (filters.sourceType && item.sourceType !== filters.sourceType) {
      return false
    }

    if (filters.enabled === 'enabled' && !item.isEnabled) {
      return false
    }

    if (filters.enabled === 'disabled' && item.isEnabled) {
      return false
    }

    if (!keyword) {
      return true
    }

    return [item.title, item.sourceRef]
      .filter(Boolean)
      .some(field => String(field).toLowerCase().includes(keyword))
  })
})

const summaryMetrics = computed(() => {
  return {
    total: normalizedDocuments.value.length,
    enabled: normalizedDocuments.value.filter(item => item.isEnabled).length,
    totalChunks: normalizedDocuments.value.reduce((sum, item) => sum + item.chunkCount, 0)
  }
})

const domainSummaryList = computed(() => {
  const summaryMap = new Map()

  normalizedDocuments.value.forEach((item) => {
    if (!summaryMap.has(item.knowledgeDomain)) {
      summaryMap.set(item.knowledgeDomain, {
        domain: item.knowledgeDomain,
        label: getDomainLabel(item.knowledgeDomain),
        totalCount: 0,
        enabledCount: 0,
        totalChunkCount: 0
      })
    }

    const current = summaryMap.get(item.knowledgeDomain)
    current.totalCount += 1
    current.totalChunkCount += item.chunkCount
    if (item.isEnabled) {
      current.enabledCount += 1
    }
  })

  return Array.from(summaryMap.values()).sort((prev, next) => next.totalCount - prev.totalCount || next.totalChunkCount - prev.totalChunkCount)
})

const getDomainLabel = (value) => AI_KNOWLEDGE_DOMAIN_LABELS[value] || value || '未分类'
const getSourceTypeLabel = (value) => sourceTypeLabelMap[value] || value || '未知来源'
const extractErrorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback

const fillFormData = (payload) => {
  const nextForm = {
    ...createEmptyForm(),
    ...payload,
    title: payload?.title || '',
    knowledgeDomain: payload?.knowledgeDomain || 'PLATFORM_POLICY',
    sourceType: payload?.sourceType || MANUAL_SOURCE_TYPE,
    sourceRef: payload?.sourceRef || '',
    tags: payload?.tags || '',
    content: payload?.content || ''
  }

  Object.assign(formData, nextForm)
}

const buildPayload = () => ({
  title: formData.title.trim(),
  knowledgeDomain: formData.knowledgeDomain,
  sourceType: formData.sourceType,
  sourceRef: formData.sourceRef.trim(),
  tags: formData.tags.trim(),
  content: formData.content.trim()
})

const loadDocuments = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const res = await getAiKnowledgeDocuments()
    documents.value = Array.isArray(res?.data) ? res.data : []
  } catch (error) {
    documents.value = []
    errorMessage.value = extractErrorMessage(error, '请稍后重试或检查接口返回。')
  } finally {
    loading.value = false
  }
}

const loadDetail = async (documentId) => {
  detailLoading.value = true

  try {
    const res = await getAiKnowledgeDocumentDetail(documentId)
    const data = res?.data || null
    currentDetail.value = data
      ? {
          ...data,
          isEnabled: Number(data.isEnabled) === 1,
          chunkCount: Number(data.chunkCount) || 0,
          chunks: Array.isArray(data.chunks) ? data.chunks : []
        }
      : null
  } catch (error) {
    currentDetail.value = null
    ElMessage.error(extractErrorMessage(error, '知识详情加载失败'))
    throw error
  } finally {
    detailLoading.value = false
  }
}

const handleView = async (row) => {
  detailVisible.value = true
  currentDetail.value = null
  try {
    await loadDetail(row.id)
  } catch (_error) {
    detailVisible.value = false
  }
}

const openCreateDrawer = () => {
  formMode.value = 'create'
  currentEditId.value = null
  fillFormData(createEmptyForm())
  formVisible.value = true
}

const handleEdit = async (row) => {
  formMode.value = 'edit'
  currentEditId.value = row.id
  formVisible.value = true
  formSubmitting.value = false

  try {
    const res = await getAiKnowledgeDocumentDetail(row.id)
    fillFormData(res?.data || row)
  } catch (error) {
    formVisible.value = false
    ElMessage.error(extractErrorMessage(error, '知识详情加载失败，无法编辑'))
  }
}

const handleSubmitForm = async () => {
  formSubmitting.value = true

  try {
    const payload = buildPayload()
    if (formMode.value === 'create') {
      await createAiKnowledgeDocument(payload)
      ElMessage.success('知识文档创建成功')
    } else {
      await updateAiKnowledgeDocument(currentEditId.value, payload)
      ElMessage.success('知识文档更新成功')
    }

    formVisible.value = false
    await loadDocuments()

    if (detailVisible.value && currentEditId.value) {
      await loadDetail(currentEditId.value)
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, formMode.value === 'create' ? '知识文档创建失败' : '知识文档更新失败'))
  } finally {
    formSubmitting.value = false
  }
}

const handleToggleEnabled = async (row) => {
  const nextEnabled = row.isEnabled ? 0 : 1
  const actionText = row.isEnabled ? '停用' : '启用'

  try {
    await ElMessageBox.confirm(
      `确认${actionText}文档“${row.title}”吗？`,
      `${actionText}知识文档`,
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )
  } catch (_error) {
    return
  }

  toggleLoadingMap[row.id] = true
  try {
    await updateAiKnowledgeDocumentEnabled(row.id, nextEnabled)
    ElMessage.success(`文档已${actionText}`)
    await loadDocuments()
    if (detailVisible.value && currentDetail.value?.id === row.id) {
      await loadDetail(row.id)
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, `${actionText}失败`))
  } finally {
    toggleLoadingMap[row.id] = false
  }
}

const handleRebuild = async (row) => {
  rebuildLoadingMap[row.id] = true
  try {
    await rebuildAiKnowledgeDocument(row.id)
    ElMessage.success(`已触发“${row.title}”重建`)
    await loadDocuments()
    if (detailVisible.value && currentDetail.value?.id === row.id) {
      await loadDetail(row.id)
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '文档重建失败'))
  } finally {
    rebuildLoadingMap[row.id] = false
  }
}

const handleRebuildAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确认重建全部知识文档吗？该操作会重新切分并刷新所有文档的知识分片。',
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
    const rebuiltCount = Number(res?.data?.documentCount || 0)
    ElMessage.success(rebuiltCount > 0 ? `已触发 ${rebuiltCount} 篇文档重建` : '已触发全部知识重建')
    await loadDocuments()
    if (detailVisible.value && currentDetail.value?.id) {
      await loadDetail(currentDetail.value.id)
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '全部重建失败'))
  } finally {
    rebuildingAll.value = false
  }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.knowledgeDomain = ''
  filters.sourceType = ''
  filters.enabled = ''
}

onMounted(() => {
  fillFormData(createEmptyForm())
  loadDocuments()
})
</script>

<style lang="scss" scoped>
.knowledge-page {
  .summary-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .summary-card {
    border: none;

    :deep(.el-card__body) {
      padding: 0 !important;
    }
  }


  .summary-card :deep(.el-card__body) {
    border-radius: 20px;
  }

  .summary-card--teal :deep(.el-card__body) {
    background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  }

  .summary-card--blue :deep(.el-card__body) {
    background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  }

  .summary-card--purple :deep(.el-card__body) {
    background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
  }

  .summary-card--rose :deep(.el-card__body) {
    background: linear-gradient(135deg, #be123c 0%, #fb7185 100%);
  }

  .summary-label,
  .summary-value,
  .summary-desc {
    color: #fff;
  }

  .summary-label {
    font-size: 12px;
    opacity: 0.88;
  }

  .summary-value {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 700;
  }

  .summary-desc {
    margin-top: 10px;
    font-size: 12px;
    line-height: 1.7;
    opacity: 0.92;
  }

  .summary-card :deep(.el-card__body) {
    padding: 20px !important;
    min-height: 150px;
    box-sizing: border-box;
  }

  .management-card {
    margin-top: 0;
  }

  .card-header,
  .card-header__actions,
  .table-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .card-header {
    justify-content: space-between;
  }

  .domain-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 12px;
    margin-bottom: 18px;
  }

  .domain-overview__item {
    padding: 14px 16px;
    border-radius: 14px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .domain-overview__title {
    font-size: 14px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .domain-overview__meta,
  .document-subline {
    margin-top: 6px;
    font-size: 12px;
    color: var(--wt-text-secondary);
  }

  .document-title-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .document-title {
    color: var(--wt-text-primary);
    font-weight: 700;
  }

  .table-actions {
    flex-wrap: wrap;
    gap: 0 10px;
  }

  .form-w-220 {
    width: 220px;
  }

  .form-w-180 {
    width: 180px;
  }

  .form-w-160 {
    width: 160px;
  }

  @media (max-width: 1200px) {
    .summary-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .summary-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
