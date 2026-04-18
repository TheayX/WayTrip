<!-- AI 服务概览页 -->
<template>
  <div class="ai-overview-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">AI Service Overview</p>
        <h1 class="page-title">AI 服务概览</h1>
        <p class="page-subtitle">查看一期知识底座状态、场景覆盖与后续工作台入口。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :loading="loading" @click="loadPageData">刷新概览</el-button>
      </div>
    </section>

    <div v-if="errorMessage" class="error-state page-error-state">
      <el-result icon="error" title="AI 概览加载失败" :sub-title="errorMessage">
        <template #extra>
          <el-button type="primary" :loading="loading" @click="loadPageData">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <template v-else>
      <div v-loading="loading" class="hero-grid">
        <el-card shadow="hover" class="hero-card hero-card-docs">
          <div class="hero-card-content">
            <div class="hero-label">启用文档</div>
            <div class="hero-value">{{ metrics.enabledDocumentCount }}</div>
            <div class="hero-desc">当前可被 AI 检索使用的知识文档数量</div>
          </div>
        </el-card>
        <el-card shadow="hover" class="hero-card hero-card-chunks">
          <div class="hero-card-content">
            <div class="hero-label">知识分片总数</div>
            <div class="hero-value">{{ metrics.totalChunkCount }}</div>
            <div class="hero-desc">一期知识底座累计沉淀的向量分片规模</div>
          </div>
        </el-card>
        <el-card shadow="hover" class="hero-card hero-card-scenarios">
          <div class="hero-card-content">
            <div class="hero-label">启用场景</div>
            <div class="hero-value">{{ metrics.enabledScenarioCount }}</div>
            <div class="hero-desc">已配置并开放的一期 AI 服务场景数量</div>
          </div>
        </el-card>
        <el-card shadow="hover" class="hero-card hero-card-update">
          <div class="hero-card-content">
            <div class="hero-label">最近更新</div>
            <div class="hero-value hero-value-sm">{{ metrics.latestUpdateAt || '暂无记录' }}</div>
            <div class="hero-desc">最近一次文档更新或重建生效时间</div>
          </div>
        </el-card>
      </div>

      <el-row v-loading="loading" :gutter="24" class="content-row">
        <el-col :xl="16" :lg="15" :md="24">
          <el-card shadow="hover" class="summary-card">
            <template #header>
              <div class="card-header">
                <span>向量索引健康摘要</span>
                <div class="card-header__extra">
                  <el-button text type="primary" @click="goTo('/ai-service/knowledge')">前往维护</el-button>
                </div>
              </div>
            </template>

            <div class="vector-health-grid">
              <div class="vector-health-card" :class="{ 'vector-health-card--ok': vectorStatus.dimensionMatched, 'vector-health-card--danger': vectorStatus.dimensionMatched === false }">
                <div class="vector-health-card__label">维度一致性</div>
                <div class="vector-health-card__value">
                  {{ vectorStatus.dimensionMatched === null ? '未检测' : vectorStatus.dimensionMatched ? '一致' : '不一致' }}
                </div>
                <div class="vector-health-card__desc">
                  模型 {{ displayAiMetric(vectorStatus.modelDimension) }} 维 · 索引 {{ displayAiMetric(vectorStatus.indexDimension) }} 维
                </div>
              </div>
              <div class="vector-health-card">
                <div class="vector-health-card__label">向量模型</div>
                <div class="vector-health-card__value vector-health-card__value--sm">{{ vectorStatus.embeddingModel || '--' }}</div>
                <div class="vector-health-card__desc">{{ vectorStatus.embeddingProvider || '--' }}</div>
              </div>
              <div class="vector-health-card">
                <div class="vector-health-card__label">Redis 索引</div>
                <div class="vector-health-card__value vector-health-card__value--sm">{{ vectorStatus.indexName || '--' }}</div>
                <div class="vector-health-card__desc">{{ vectorStatus.redisHost || '--' }}:{{ vectorStatus.redisPort || '--' }}</div>
              </div>
              <div class="vector-health-card">
                <div class="vector-health-card__label">完成分片</div>
                <div class="vector-health-card__value">{{ displayAiMetric(vectorStatus.completedChunkCount) }}</div>
                <div class="vector-health-card__desc">
                  待处理 {{ displayAiMetric(vectorStatus.pendingChunkCount) }} · 失败 {{ displayAiMetric(vectorStatus.failedChunkCount) }}
                </div>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>一期工作台入口</span>
              </div>
            </template>

            <div class="entry-grid">
              <button
                v-for="item in placeholderEntries"
                :key="item.title"
                type="button"
                class="entry-card"
                :class="item.tone"
                @click="goTo(item.path)"
              >
                <div class="entry-head">
                  <div class="entry-title">{{ item.title }}</div>
                  <el-tag size="small" effect="plain" :type="item.tagType" round>{{ item.tag }}</el-tag>
                </div>
                <div class="entry-desc">{{ item.desc }}</div>
                <div class="entry-action">查看规划能力</div>
              </button>
            </div>
          </el-card>

          <el-card shadow="hover" class="summary-card">
            <template #header>
              <div class="card-header">
                <span>知识摘要</span>
              </div>
            </template>

            <div class="summary-grid">
              <div class="summary-panel">
                <div class="summary-title">最近更新文档</div>
                <template v-if="recentDocuments.length">
                  <div v-for="item in recentDocuments" :key="item.id" class="document-item">
                    <div class="document-main">
                      <span class="document-title">{{ item.title }}</span>
                      <el-tag size="small" effect="plain" :type="item.isEnabled ? 'success' : 'info'" round>
                        {{ item.isEnabled ? '启用中' : '已停用' }}
                      </el-tag>
                    </div>
                    <div class="document-meta">
                      <span>{{ item.domainLabel }}</span>
                      <span>分片 {{ item.chunkCount }}</span>
                      <span>{{ item.updatedAt || '暂无时间' }}</span>
                    </div>
                  </div>
                </template>
                <el-empty v-else description="暂无知识文档" :image-size="72" />
              </div>

              <div class="summary-panel">
                <div class="summary-title">知识域覆盖</div>
                <template v-if="domainSummaryList.length">
                  <div v-for="item in domainSummaryList" :key="item.domain" class="domain-item">
                    <div class="domain-head">
                      <span class="domain-title">{{ item.label }}</span>
                      <span class="domain-count">{{ item.enabledCount }}/{{ item.totalCount }} 篇</span>
                    </div>
                    <div class="domain-meta">
                      <span>启用分片 {{ item.enabledChunkCount }}</span>
                      <span>总分片 {{ item.totalChunkCount }}</span>
                    </div>
                  </div>
                </template>
                <el-empty v-else description="暂无知识域数据" :image-size="72" />
              </div>

              <div class="summary-panel">
                <div class="summary-title">场景接入摘要</div>
                <div v-for="item in scenarioSummaryList" :key="item.scenario" class="scenario-item">
                  <div class="scenario-head">
                    <span class="scenario-title">{{ item.title }}</span>
                    <el-tag size="small" effect="plain" :type="item.enabled ? 'success' : 'info'" round>
                      {{ item.enabled ? '已启用' : '未启用' }}
                    </el-tag>
                  </div>
                  <div class="scenario-desc">{{ item.domainLabel }} · {{ item.description }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xl="8" :lg="9" :md="24">
          <el-card shadow="hover" class="panel-card">
            <template #header>
              <div class="card-header">
                <span>快捷操作</span>
              </div>
            </template>

            <div class="quick-actions">
              <el-button type="primary" @click="loadPageData">刷新知识统计</el-button>
              <el-button @click="goTo('/ai-service/workbench')">查看工作台规划</el-button>
              <el-button @click="goTo('/ai-service/feedback')">查看反馈闭环规划</el-button>
            </div>
          </el-card>

          <el-card shadow="hover" class="panel-card">
            <template #header>
              <div class="card-header">
                <span>一期关注点</span>
              </div>
            </template>

            <div class="tips-list">
              <div v-for="item in phaseOneTips" :key="item.title" class="tips-item">
                <div class="tips-title">{{ item.title }}</div>
                <div class="tips-desc">{{ item.desc }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAiKnowledgeDocuments, getAiVectorIndexStatus } from '@/modules/ai-service/api.js'
import { AI_KNOWLEDGE_DOMAIN_LABELS, AI_SCENARIO_CONFIGS } from '@/modules/ai-service/constants.js'
import { createEmptyAiVectorStatus, displayAiMetric } from '@/modules/ai-service/utils.js'

// 概览页只聚合轻量统计，不承接重操作，便于作为 AI 模块默认入口。
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const documents = ref([])
const vectorStatus = ref(createEmptyAiVectorStatus())

// 工作台和反馈闭环先作为占位入口保留，后续能力补齐后可直接接管当前导航。
const placeholderEntries = [
  {
    title: 'AI 工作台',
    desc: '集中承接场景切换、提示词调试、RAG 查询验证与服务状态观察。',
    path: '/ai-service/workbench',
    tone: 'tone-workbench',
    tag: '即将开放',
    tagType: 'warning'
  },
  {
    title: '反馈闭环',
    desc: '沉淀人工兜底、用户反馈、命中不足样本与后续优化动作。',
    path: '/ai-service/feedback',
    tone: 'tone-feedback',
    tag: '即将开放',
    tagType: 'info'
  }
]

// 一期关注点直接写成本地静态文案，避免为了说明性内容引入额外接口。
const phaseOneTips = [
  {
    title: '先保证知识可用',
    desc: '一期先围绕已启用文档、分片规模和最近更新时间，判断知识底座是否具备稳定服务条件。'
  },
  {
    title: '场景与知识域要对齐',
    desc: '每个已启用场景都应能映射到明确知识域，避免配置已开但知识覆盖不足。'
  },
  {
    title: '为后续反馈闭环预留入口',
    desc: '工作台与反馈页先提供占位入口，后续直接承接调试、标注与运营回看能力。'
  }
]

// 列表数据先统一做一层归一化，后面所有统计都基于同一份结构计算。
const normalizedDocuments = computed(() => {
  return (documents.value || []).map((item, index) => ({
    id: item.id ?? `${item.title || 'document'}-${index}`,
    title: item.title || '未命名文档',
    knowledgeDomain: item.knowledgeDomain || 'UNKNOWN',
    domainLabel: AI_KNOWLEDGE_DOMAIN_LABELS[item.knowledgeDomain] || item.knowledgeDomain || '未分类',
    isEnabled: Number(item.isEnabled) === 1,
    chunkCount: Number(item.chunkCount) || 0,
    updatedAt: item.updatedAt || '',
    updatedTimestamp: Number.isNaN(Date.parse(item.updatedAt || '')) ? 0 : Date.parse(item.updatedAt || '')
  }))
})

// 最近更新时间统一按时间倒序，时间相同时再按 ID 稳定排序。
const sortedDocumentsByUpdatedAt = computed(() => {
  return [...normalizedDocuments.value].sort((prev, next) => {
    if (next.updatedTimestamp !== prev.updatedTimestamp) {
      return next.updatedTimestamp - prev.updatedTimestamp
    }

    return next.id.localeCompare(prev.id)
  })
})

// 顶部指标卡只展示最能反映知识底座状态的几个核心指标。
const metrics = computed(() => {
  const enabledDocuments = normalizedDocuments.value.filter(item => item.isEnabled)

  return {
    enabledDocumentCount: enabledDocuments.length,
    totalChunkCount: normalizedDocuments.value.reduce((sum, item) => sum + item.chunkCount, 0),
    enabledScenarioCount: AI_SCENARIO_CONFIGS.filter(item => item.enabled).length,
    latestUpdateAt: sortedDocumentsByUpdatedAt.value.find(item => item.updatedTimestamp > 0)?.updatedAt || ''
  }
})

const recentDocuments = computed(() => sortedDocumentsByUpdatedAt.value.filter(item => item.updatedTimestamp > 0).slice(0, 5))

// 知识域覆盖摘要用于快速判断“场景有无知识可用”，所以同时看文档数和分片数。
const domainSummaryList = computed(() => {
  const summaryMap = new Map()

  normalizedDocuments.value.forEach((item) => {
    if (!summaryMap.has(item.knowledgeDomain)) {
      summaryMap.set(item.knowledgeDomain, {
        domain: item.knowledgeDomain,
        label: AI_KNOWLEDGE_DOMAIN_LABELS[item.knowledgeDomain] || item.knowledgeDomain || '未分类',
        totalCount: 0,
        enabledCount: 0,
        totalChunkCount: 0,
        enabledChunkCount: 0
      })
    }

    const current = summaryMap.get(item.knowledgeDomain)
    current.totalCount += 1
    current.totalChunkCount += item.chunkCount
    if (item.isEnabled) {
      current.enabledCount += 1
      current.enabledChunkCount += item.chunkCount
    }
  })

  return Array.from(summaryMap.values()).sort((prev, next) => next.totalCount - prev.totalCount || next.totalChunkCount - prev.totalChunkCount)
})

// 场景摘要直接复用 constants，避免概览页和配置页出现两份场景定义。
const scenarioSummaryList = computed(() => {
  return AI_SCENARIO_CONFIGS.map(item => ({
    ...item,
    domainLabel: AI_KNOWLEDGE_DOMAIN_LABELS[item.knowledgeDomain] || item.knowledgeDomain
  }))
})

// 页面数据按“文档列表 + 向量状态”并行拉取，减少概览首屏等待时间。
const loadPageData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [documentsRes, vectorStatusRes] = await Promise.all([
      getAiKnowledgeDocuments(),
      getAiVectorIndexStatus()
    ])
    documents.value = Array.isArray(documentsRes?.data) ? documentsRes.data : []
    vectorStatus.value = {
      ...createEmptyAiVectorStatus(),
      ...(vectorStatusRes?.data || {})
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '请稍后重试或检查接口返回。'
    vectorStatus.value = createEmptyAiVectorStatus()
  } finally {
    loading.value = false
  }
}

// 占位入口未接路由时给出提示，避免点击后无反馈。
const goTo = async (path) => {
  try {
    await router.push(path)
  } catch (error) {
    ElMessage.warning('当前入口路由尚未接入，请先完成导航配置。')
  }
}

onMounted(() => {
  loadPageData()
})
</script>

<style lang="scss" scoped>
.ai-overview-page {
  .hero-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .hero-card {
    border: none;

    :deep(.el-card__body) {
      padding: 0 !important;
    }
  }

  .hero-card-content {
    position: relative;
    overflow: hidden;
    padding: 20px;
    color: #fff;
    border-radius: 20px;
    min-height: 152px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    box-sizing: border-box;

    &::after {
      content: '';
      position: absolute;
      top: -20px;
      right: -20px;
      width: 96px;
      height: 96px;
      border-radius: 50%;
      background: color-mix(in srgb, var(--wt-overlay-bg) 38%, transparent);
    }
  }

  .hero-card-docs .hero-card-content {
    background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  }

  .hero-card-chunks .hero-card-content {
    background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  }

  .hero-card-scenarios .hero-card-content {
    background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
  }

  .hero-card-update .hero-card-content {
    background: linear-gradient(135deg, #be123c 0%, #fb7185 100%);
  }

  .hero-label {
    font-size: 12px;
    opacity: 0.88;
  }

  .hero-value {
    margin-top: 8px;
    font-size: 24px;
    font-weight: 700;
    line-height: 1.2;
  }

  .hero-value-sm {
    font-size: 16px;
    line-height: 1.4;
    word-break: break-word;
  }

  .hero-desc {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.65;
    opacity: 0.9;
  }

  .entry-grid,
  .summary-grid,
  .vector-health-grid {
    display: grid;
    gap: 16px;
  }

  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .vector-health-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .vector-health-card {
    padding: 18px;
    border-radius: 16px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .vector-health-card--ok {
    border-color: color-mix(in srgb, #22c55e 38%, var(--wt-border-default));
  }

  .vector-health-card--danger {
    border-color: color-mix(in srgb, #f43f5e 42%, var(--wt-border-default));
  }

  .vector-health-card__label {
    font-size: 12px;
    color: var(--wt-text-secondary);
  }

  .vector-health-card__value {
    margin-top: 10px;
    font-size: 24px;
    font-weight: 700;
    color: var(--wt-text-primary);
    line-height: 1.3;
  }

  .vector-health-card__value--sm {
    font-size: 16px;
    word-break: break-word;
  }

  .vector-health-card__desc {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--wt-text-regular);
  }

  .entry-card {
    padding: 18px;
    border-radius: 14px;
    border: 1px solid var(--wt-border-default);
    background: none;
    text-align: left;
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--wt-shadow-soft);
    }
  }

  .entry-card.tone-workbench {
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, color-mix(in srgb, var(--wt-tag-warning-bg) 60%, var(--wt-surface-muted)) 100%);
  }

  .entry-card.tone-feedback {
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, color-mix(in srgb, var(--wt-tag-info-bg) 60%, var(--wt-surface-muted)) 100%);
  }

  .entry-head,
  .document-main,
  .domain-head,
  .scenario-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .entry-title,
  .document-title,
  .domain-title,
  .scenario-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .entry-desc,
  .scenario-desc,
  .tips-desc,
  .document-meta,
  .domain-meta {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--wt-text-regular);
  }

  .entry-action {
    margin-top: 14px;
    font-size: 12px;
    font-weight: 700;
    color: #245bdb;
  }

  .summary-panel {
    padding: 18px;
    border-radius: 14px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .summary-title {
    margin-bottom: 12px;
    font-size: 13px;
    color: var(--wt-text-secondary);
  }

  .document-item,
  .domain-item,
  .scenario-item,
  .tips-item {
    padding: 14px 0;
  }

  .document-item + .document-item,
  .domain-item + .domain-item,
  .scenario-item + .scenario-item,
  .tips-item + .tips-item {
    border-top: 1px solid var(--wt-border-default);
  }

  .domain-count {
    font-size: 13px;
    font-weight: 600;
    color: var(--wt-text-secondary);
  }

  .tips-title {
    font-size: 13px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .quick-actions {
    display: grid;
    gap: 10px;

    .el-button {
      margin-left: 0;
      justify-content: center;
    }
  }

  @media (max-width: 1200px) {
    .hero-grid,
    .entry-grid,
    .summary-grid,
    .vector-health-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .hero-grid,
    .entry-grid,
    .summary-grid,
    .vector-health-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
