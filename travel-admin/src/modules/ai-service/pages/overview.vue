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
        <el-button type="primary" :loading="loading" @click="loadPageData">
          <el-icon><RefreshRight /></el-icon>
          刷新概览
        </el-button>
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
      <el-alert
        v-if="vectorStatus.needsRebuild && vectorStatus.warningMessage"
        class="page-status-alert ai-warning-alert"
        type="warning"
        show-icon
        :closable="false"
        :title="vectorStatus.warningMessage"
      />

      <section v-loading="loading" class="summary-grid metric-cards--order hero-grid">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">启用文档</div>
          <div class="summary-value">{{ metrics.enabledDocumentCount }}</div>
          <div class="summary-desc">当前可被 AI 检索使用的知识文档数量</div>
        </el-card>
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">知识分片总数</div>
          <div class="summary-value">{{ metrics.totalChunkCount }}</div>
          <div class="summary-desc">一期知识底座累计沉淀的向量分片规模</div>
        </el-card>
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">启用场景</div>
          <div class="summary-value">{{ metrics.enabledScenarioCount }}</div>
          <div class="summary-desc">已配置并开放的一期 AI 服务场景数量</div>
        </el-card>
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">最近更新</div>
          <div class="summary-value summary-value--sm">{{ metrics.latestUpdateAt || '暂无记录' }}</div>
          <div class="summary-desc">最近一次文档更新或重建生效时间</div>
        </el-card>
      </section>

      <el-row v-loading="loading" :gutter="24" class="content-row">
        <el-col :xl="16" :lg="15" :md="24">
          <el-card shadow="hover" class="admin-management-card">
            <template #header>
              <div class="card-header">
                <span>向量索引健康摘要</span>
                <div class="card-header__extra">
                  <el-button text type="primary" @click="goTo('/ai-service/knowledge')">
                    <el-icon><Connection /></el-icon>
                    前往维护
                  </el-button>
                </div>
              </div>
            </template>

            <div class="vector-health-grid">
              <div
                class="feature-panel"
                :class="{
                  'feature-panel--success': vectorStatus.retrievalReady,
                  'feature-panel--warning': vectorStatus.needsRebuild
                }"
              >
                <div class="feature-panel-title">维度一致性</div>
                <div class="feature-panel-main">
                  {{ vectorStatus.retrievalReady ? '可检索' : vectorStatus.needsRebuild ? '需重建' : '未检测' }}
                </div>
                <div class="feature-panel-sub">
                  模型 {{ displayAiMetric(vectorStatus.modelDimension) }} 维 · 索引 {{ displayAiMetric(vectorStatus.indexDimension) }} 维
                </div>
              </div>
              <div class="feature-panel feature-panel--soft">
                <div class="feature-panel-title">向量模型</div>
                <div class="feature-panel-main vector-main-sm">{{ vectorStatus.embeddingModel || '--' }}</div>
                <div class="feature-panel-sub">{{ vectorStatus.embeddingProvider || '--' }}</div>
              </div>
              <div class="feature-panel feature-panel--soft">
                <div class="feature-panel-title">Redis 索引</div>
                <div class="feature-panel-main vector-main-sm">{{ vectorStatus.indexName || '--' }}</div>
                <div class="feature-panel-sub">{{ vectorStatus.redisHost || '--' }}:{{ vectorStatus.redisPort || '--' }}</div>
              </div>
              <div class="feature-panel feature-panel--primary">
                <div class="feature-panel-title">完成分片</div>
                <div class="feature-panel-main">{{ displayAiMetric(vectorStatus.completedChunkCount) }}</div>
                <div class="feature-panel-sub">
                  待处理 {{ displayAiMetric(vectorStatus.pendingChunkCount) }} · 失败 {{ displayAiMetric(vectorStatus.failedChunkCount) }}
                </div>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="admin-management-card">
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
                class="entry-card feature-panel feature-panel--interactive"
                :class="item.tone === 'tone-workbench' ? 'feature-panel--warning' : 'feature-panel--info'"
                @click="goTo(item.path)"
              >
                <div class="feature-panel-head">
                  <div class="feature-panel-title--strong">{{ item.title }}</div>
                  <el-tag size="small" effect="plain" :type="item.tagType" round>{{ item.tag }}</el-tag>
                </div>
                <div class="feature-panel-sub">{{ item.desc }}</div>
                <div class="feature-panel-action">查看规划能力</div>
              </button>
            </div>
          </el-card>

          <el-card shadow="hover" class="admin-management-card">
            <template #header>
              <div class="card-header">
                <span>知识摘要</span>
              </div>
            </template>

            <div class="summary-grid">
              <div class="feature-panel feature-panel--soft">
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

              <div class="feature-panel feature-panel--soft">
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

              <div class="feature-panel feature-panel--soft">
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
          <el-card shadow="hover" class="admin-management-card panel-card">
            <template #header>
              <div class="card-header">
                <span>快捷操作</span>
              </div>
            </template>

            <div class="quick-actions">
              <button
                v-for="item in quickActions"
                :key="item.title"
                type="button"
                class="quick-action-card feature-panel feature-panel--interactive"
                :class="item.tone"
                @click="item.action"
              >
                <div class="quick-action-card__icon">
                  <el-icon><component :is="item.icon" /></el-icon>
                </div>
                <div class="quick-action-card__body">
                  <div class="quick-action-card__title">{{ item.title }}</div>
                  <div class="quick-action-card__desc">{{ item.desc }}</div>
                </div>
              </button>
            </div>
          </el-card>

          <el-card shadow="hover" class="admin-management-card panel-card">
            <template #header>
              <div class="card-header">
                <span>一期关注点</span>
              </div>
            </template>

            <div class="tips-list">
              <div v-for="item in phaseOneTips" :key="item.title" class="tips-item feature-panel feature-panel--soft">
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
import { ChatDotRound, Connection, RefreshRight } from '@element-plus/icons-vue'
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

const quickActions = [
  {
    title: '刷新知识统计',
    desc: '重新拉取文档统计与索引健康状态。',
    icon: RefreshRight,
    tone: 'feature-panel--primary',
    action: () => loadPageData()
  },
  {
    title: '进入工作台',
    desc: '查看链路状态并执行 RAG 查询预览。',
    icon: Connection,
    tone: 'feature-panel--warning',
    action: () => goTo('/ai-service/workbench')
  },
  {
    title: '查看反馈规划',
    desc: '回看反馈闭环的后续承接方向。',
    icon: ChatDotRound,
    tone: 'feature-panel--info',
    action: () => goTo('/ai-service/feedback')
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

    return String(next.id).localeCompare(String(prev.id))
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
  .ai-warning-alert {
    margin-bottom: 16px;
  }

  .summary-card {
    min-height: 122px;
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

  .hero-grid.summary-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .vector-health-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .vector-main-sm {
    font-size: 16px;
    word-break: break-word;
  }

  .entry-card {
    text-align: left;
  }

  .document-main,
  .domain-head,
  .scenario-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .document-title,
  .domain-title,
  .scenario-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .scenario-desc,
  .tips-desc,
  .document-meta,
  .domain-meta {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--wt-text-regular);
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
  .scenario-item + .scenario-item {
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

  .tips-list {
    display: grid;
    gap: 12px;
  }

  .tips-item {
    padding: 16px 18px;
  }

  .quick-actions {
    display: grid;
    gap: 12px;
  }

  .quick-action-card {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    text-align: left;
  }

  .quick-action-card__icon {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: color-mix(in srgb, var(--wt-surface-elevated) 72%, transparent);
    color: var(--wt-text-primary);
    font-size: 18px;
  }

  .quick-action-card__body {
    min-width: 0;
  }

  .quick-action-card__title {
    font-size: 14px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .quick-action-card__desc {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.6;
    color: var(--wt-text-secondary);
  }

  @media (max-width: 1200px) {
    .entry-grid,
    .summary-grid,
    .vector-health-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 1080px) {
    .hero-grid.summary-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .hero-grid.summary-grid,
    .entry-grid,
    .summary-grid,
    .vector-health-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
