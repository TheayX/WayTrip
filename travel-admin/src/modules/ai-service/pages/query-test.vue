<!-- AI 查询测试页 -->
<template>
  <div class="query-test-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">AI Query Preview</p>
        <h1 class="page-title">查询测试</h1>
        <p class="page-subtitle">按场景输入测试问题，快速预览当前知识库的检索命中结果与片段内容。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :loading="previewing" @click="handlePreview">开始预览</el-button>
      </div>
    </section>

    <el-row :gutter="24" class="content-row">
      <el-col :xl="9" :lg="10" :md="24">
        <el-card shadow="hover" class="panel-card query-form-card">
          <template #header>
            <div class="card-header">
              <span>测试条件</span>
            </div>
          </template>

          <el-form label-position="top" @submit.prevent>
            <el-form-item label="场景">
              <el-select v-model="form.scenario" class="w-full" placeholder="请选择场景">
                <el-option
                  v-for="item in AI_SCENARIO_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                  <div class="scenario-option">
                    <span>{{ item.label }}</span>
                    <span class="scenario-option__domain">
                      {{ getDomainLabel(item.domain) }}
                    </span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <div class="scenario-hint">
              <div class="scenario-hint__label">当前知识域</div>
              <div class="scenario-hint__value">{{ selectedScenarioDomainLabel }}</div>
            </div>

            <el-form-item label="测试问题" class="query-field">
              <el-input
                v-model="form.query"
                type="textarea"
                :rows="10"
                resize="vertical"
                maxlength="500"
                show-word-limit
                placeholder="例如：退款多久到账？人工客服处理时效是多久？"
                @keyup.ctrl.enter="handlePreview"
              />
            </el-form-item>

            <div class="form-actions">
              <el-button type="primary" :loading="previewing" @click="handlePreview">开始预览</el-button>
            </div>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xl="15" :lg="14" :md="24">
        <el-card shadow="hover" class="panel-card result-card" v-loading="previewing">
          <template #header>
            <div class="card-header">
              <span>命中结果</span>
            </div>
          </template>

          <el-result
            v-if="errorMessage"
            icon="error"
            title="查询测试失败"
            :sub-title="errorMessage"
          />
          <el-empty
            v-else-if="!hasSubmitted"
            description="选择场景并输入问题后，可在这里查看命中结果。"
            :image-size="72"
          />
          <el-empty
            v-else-if="!result || !result.hitCount"
            description="当前查询没有命中知识分片。"
            :image-size="72"
          />
          <div v-else class="result-panel">
            <div class="result-summary-grid">
              <div class="result-summary-item">
                <div class="result-summary-item__label">命中知识域</div>
                <div class="result-summary-item__value">{{ activeDomainLabel }}</div>
              </div>
              <div class="result-summary-item">
                <div class="result-summary-item__label">命中数量</div>
                <div class="result-summary-item__value">{{ result.hitCount }}</div>
              </div>
            </div>

            <div class="result-query-card">
              <div class="result-query-card__label">查询内容</div>
              <div class="result-query-card__value">{{ result.query || form.query.trim() }}</div>
            </div>

            <div class="hit-list">
              <el-card
                v-for="item in resultHits"
                :key="`${item.documentId}-${item.chunkId}`"
                shadow="hover"
                class="hit-card"
              >
                <div class="hit-card__head">
                  <div>
                    <div class="hit-card__title">{{ item.title || '未命名文档' }}</div>
                    <div class="hit-card__meta">
                      <span>文档 ID：{{ item.documentId ?? '—' }}</span>
                      <span>分片 ID：{{ item.chunkId ?? '—' }}</span>
                    </div>
                  </div>
                  <el-tag size="small" effect="plain" round>
                    {{ getDomainLabel(item.knowledgeDomain) }}
                  </el-tag>
                </div>

                <div class="hit-card__source">
                  <span>来源类型：{{ item.sourceType || '—' }}</span>
                  <span>来源标识：{{ item.sourceRef || '—' }}</span>
                </div>

                <div class="hit-card__snippet">{{ item.snippet || '暂无片段内容' }}</div>
              </el-card>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { AI_KNOWLEDGE_DOMAIN_LABELS, AI_PREVIEW_DEFAULT_SCENARIO, AI_SCENARIO_OPTIONS } from '@/modules/ai-service/constants.js'
import { useAiKnowledgePreview } from '@/modules/ai-service/composables/useAiKnowledgePreview.js'

// 查询测试优先默认到订单顾问，便于验证订单边界、售后规则与工具优先链路。
const DEFAULT_SCENARIO = AI_PREVIEW_DEFAULT_SCENARIO

const resolveScenarioOption = (scenario) => AI_SCENARIO_OPTIONS.find(item => item.value === scenario) || null

const {
  previewing,
  errorMessage,
  result,
  hasSubmitted,
  form,
  selectedScenarioDomainLabel,
  activeDomainLabel,
  resultHits,
  handlePreview: runPreview
} = useAiKnowledgePreview({
  defaultScenario: DEFAULT_SCENARIO,
  resolveScenarioOption,
  domainLabels: AI_KNOWLEDGE_DOMAIN_LABELS
})

const getDomainLabel = (value) => AI_KNOWLEDGE_DOMAIN_LABELS[value] || value || '未分类'

// 查询测试页只保留自己的文案入口，底层 preview 状态交给共享 composable。
const handlePreview = async () => {
  await runPreview({
    emptyQueryMessage: '请输入测试问题',
    fallbackErrorMessage: '命中预览失败，请稍后重试。'
  })
}
</script>

<style lang="scss" scoped>
.query-test-page {
  .content-row {
    align-items: stretch;
  }

  .panel-card {
    height: 100%;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .w-full {
    width: 100%;
  }

  .query-form-card :deep(.el-card__body),
  .result-card :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  .scenario-option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .scenario-option__domain,
  .scenario-hint__label,
  .result-summary-item__label,
  .result-query-card__label,
  .hit-card__meta,
  .hit-card__source {
    font-size: 12px;
    color: var(--wt-text-secondary);
  }

  .scenario-hint {
    margin: 2px 0 18px;
    padding: 14px 16px;
    border-radius: 14px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .scenario-hint__value {
    margin-top: 6px;
    font-size: 15px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .query-field {
    margin-bottom: 0;
  }

  .form-actions {
    display: flex;
    justify-content: flex-start;
  }

  .result-panel,
  .hit-list {
    display: grid;
    gap: 16px;
  }

  .result-summary-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .result-summary-item,
  .result-query-card,
  .hit-card {
    border-radius: 16px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .result-summary-item {
    padding: 18px;
  }

  .result-summary-item__value {
    margin-top: 8px;
    font-size: 22px;
    font-weight: 700;
    color: var(--wt-text-primary);
    line-height: 1.3;
    word-break: break-word;
  }

  .result-query-card {
    padding: 18px;
  }

  .result-query-card__value {
    margin-top: 8px;
    font-size: 14px;
    line-height: 1.8;
    color: var(--wt-text-primary);
    white-space: pre-wrap;
    word-break: break-word;
  }

  .hit-card {
    overflow: hidden;

    :deep(.el-card__body) {
      display: grid;
      gap: 14px;
      padding: 18px;
    }
  }

  .hit-card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .hit-card__title {
    font-size: 16px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .hit-card__meta,
  .hit-card__source {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
    line-height: 1.6;
  }

  .hit-card__snippet {
    padding: 14px 16px;
    border-radius: 12px;
    background: var(--wt-overlay-bg);
    font-size: 13px;
    line-height: 1.8;
    color: var(--wt-text-regular);
    white-space: pre-wrap;
    word-break: break-word;
  }

  @media (max-width: 1200px) {
    .result-summary-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
