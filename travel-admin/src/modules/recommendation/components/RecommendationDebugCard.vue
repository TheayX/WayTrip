<!-- 推荐预览与调试卡片 -->
<template>
  <el-card ref="debugCardRef" shadow="hover" class="debug-card preview-card">
    <template #header>
      <div class="card-header">
        <div class="title-section">
          <span class="title">预览与调试</span>
          <el-tag effect="plain" type="warning" round>预览区</el-tag>
        </div>
      </div>
    </template>

    <el-tabs :model-value="activePreviewTab" class="preview-tabs" @update:model-value="emit('update:active-preview-tab', $event)">
      <el-tab-pane label="推荐调试预览" name="recommendation">
        <div class="debug-toolbar">
          <div class="debug-field">
            <span class="debug-label">用户 ID</span>
            <el-input-number v-model="debugForm.userId" :min="1" :step="1" controls-position="right" />
          </div>
          <div class="debug-field">
            <span class="debug-label">返回数量</span>
            <el-input-number v-model="debugForm.limit" :min="1" :max="20" :step="1" controls-position="right" />
          </div>
          <div class="debug-field">
            <span class="debug-label">结果来源</span>
            <el-switch v-model="debugForm.refresh" inline-prompt active-text="刷新" inactive-text="缓存" />
          </div>
          <div class="debug-field">
            <span class="debug-label">后端日志</span>
            <el-switch v-model="debugForm.debug" inline-prompt active-text="控制台日志" inactive-text="静默" />
          </div>
          <div class="debug-field">
            <span class="debug-label">稳定模式</span>
            <el-switch v-model="debugForm.stable" inline-prompt active-text="稳定" inactive-text="轮换" />
          </div>
          <el-button type="primary" :loading="previewing" @click="emit('preview-recommendations')">
            调试预览
          </el-button>
        </div>

        <div class="debug-meta" v-if="debugResult">
          <el-tag size="small" :type="recommendationTypeMeta.tagType">{{ recommendationTypeMeta.label }}</el-tag>
          <el-tag v-if="debugInfo?.userNickname" size="small" type="warning">
            用户：{{ debugInfo.userNickname }}
          </el-tag>
          <el-tag size="small" :type="debugResult.needPreference ? 'warning' : 'success'">
            {{ debugResult.needPreference ? '需要补充偏好设置' : '无需偏好引导' }}
          </el-tag>
          <el-tag size="small" type="primary">返回 {{ debugItems.length }} 条</el-tag>
          <el-tag size="small" type="info">{{ debugForm.refresh ? '本次强制刷新' : '本次优先读取缓存' }}</el-tag>
        </div>

        <div v-if="debugResult" class="debug-summary">
          <div class="debug-summary-grid">
            <div v-for="card in debugSummaryCards" :key="card.label" class="summary-card">
              <div class="summary-label">{{ card.label }}</div>
              <div class="summary-value">{{ card.value }}</div>
              <div class="summary-desc">{{ card.desc }}</div>
            </div>
          </div>
        </div>

        <el-alert
          v-if="debugResult"
          class="debug-conclusion"
          :type="recommendationTypeMeta.alertType"
          :closable="false"
          show-icon
        >
          <template #title>{{ recommendationTypeMeta.title }}</template>
          {{ recommendationTypeMeta.description }}
        </el-alert>

        <div v-if="debugInfo" class="debug-pipeline">
          <div class="debug-block-title">后端调试链路</div>
          <div class="pipeline-grid">
            <div class="pipeline-card">
              <div class="pipeline-label">触发原因</div>
              <div class="pipeline-value">{{ debugInfo.triggerReason || '未返回' }}</div>
            </div>
            <div class="pipeline-card">
              <div class="pipeline-label">交互景点数</div>
              <div class="pipeline-value">{{ debugInfo.interactionCount ?? 0 }}</div>
            </div>
            <div class="pipeline-card">
              <div class="pipeline-label">原始候选数</div>
              <div class="pipeline-value">{{ debugInfo.candidateCount ?? 0 }}</div>
            </div>
            <div class="pipeline-card">
              <div class="pipeline-label">过滤后候选数</div>
              <div class="pipeline-value">{{ debugInfo.filteredCount ?? 0 }}</div>
            </div>
          </div>
        </div>

        <div v-if="behaviorStats.length" class="debug-sections">
          <div class="debug-block-title">行为来源统计</div>
          <el-alert type="info" :closable="false" class="behavior-alert" show-icon>
            多次浏览、收藏、评分、下单如果集中在同几个景点上，最终“参与推荐的交互景点数”仍然会很小。
          </el-alert>
          <el-table :data="behaviorStats" stripe size="small" class="debug-table">
            <el-table-column prop="behavior" label="行为类型" width="120" />
            <el-table-column label="记录条数" width="120">
              <template #default="{ row }">
                <span>{{ row.recordCount ?? '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="uniqueSpotCount" label="命中景点数" width="140" />
            <el-table-column prop="description" label="说明" min-width="320" />
          </el-table>
        </div>

        <div v-if="behaviorDetails.length" class="debug-sections">
          <div class="debug-block-title">用户交互行为明细</div>
          <el-collapse>
            <el-collapse-item :title="`查看明细（${behaviorDetails.length} 条）`" name="behavior-details">
              <el-table :data="behaviorDetails" stripe size="small" class="debug-table">
                <el-table-column prop="behavior" label="行为类型" width="140" />
                <el-table-column prop="spotId" label="景点ID" width="100" />
                <el-table-column prop="spotName" label="景点名称" min-width="180" />
                <el-table-column label="行为权重" width="140">
                  <template #default="{ row }">
                    <span v-if="row.score != null" class="score-text">{{ Number(row.score).toFixed(4) }}</span>
                    <span v-else class="score-empty">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="明细说明" min-width="280" />
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div v-if="debugInsights.length" class="debug-insights">
          <div class="debug-block-title">结果解读</div>
          <div class="insight-list">
            <div v-for="(insight, index) in compactDebugInsights" :key="`${index}-${insight}`" class="insight-item">
              {{ insight }}
            </div>
          </div>
        </div>

        <div v-if="debugNotes.length" class="debug-insights">
          <div class="debug-block-title">后端备注</div>
          <div class="insight-list insight-list--plain">
            <div v-for="(note, index) in debugNotes" :key="`${index}-${note}`" class="insight-item insight-item--blue">
              {{ note }}
            </div>
          </div>
        </div>

        <div v-if="debugSections.length" class="debug-sections">
          <div class="debug-block-title">关键中间结果</div>
          <el-collapse>
            <el-collapse-item
              v-for="section in debugSections"
              :key="section.key"
              :name="section.key"
              :title="`${section.title}（${section.items.length}）`"
              :class="{ 'highlight-interactions-section': section.key === 'interactions' }"
            >
              <el-table :data="section.items" stripe size="small" :class="{ 'highlight-interactions-table': section.key === 'interactions' }">
                <el-table-column prop="spotId" label="景点ID" width="100" />
                <el-table-column prop="spotName" label="景点名称" min-width="180" />
                <el-table-column label="分数/权重" width="140">
                  <template #default="{ row }">
                    <span v-if="row.score != null" class="score-text">{{ Number(row.score).toFixed(4) }}</span>
                    <span v-else class="score-empty">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="说明" min-width="260">
                  <template #default="{ row }">
                    <div v-if="section.key === 'interactions'" v-html="formatDescription(row.description)"></div>
                    <div v-else>{{ row.description }}</div>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div v-if="debugItems.length" class="debug-top-results">
          <div class="debug-block-title">Top 结果速览</div>
          <div class="top-result-list">
            <div v-for="item in topDebugItems" :key="item.id" class="top-result-card">
              <div class="top-result-rank">#{{ item.rank }}</div>
              <div class="top-result-main">
                <div class="top-result-name">{{ item.name }}</div>
                <div class="top-result-meta">
                  <span>{{ resolveCategoryDisplayName(item.categoryName) }}</span>
                  <span>{{ resolveRegionDisplayName(item.regionName) }}</span>
                </div>
              </div>
              <div class="top-result-score">
                <div class="score-label">{{ item.score == null ? '无分数' : '推荐分' }}</div>
                <div class="score-value" :class="{ 'score-value--empty': item.score == null }">
                  {{ item.scoreText }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="resultContributions.length" class="debug-sections">
          <div class="debug-block-title">结果贡献来源</div>
          <el-collapse>
            <el-collapse-item
              v-for="item in resultContributions.slice(0, 8)"
              :key="`contrib-${item.targetSpotId}`"
              :title="`${item.targetSpotName}（最终分数 ${item.finalScore == null ? '-' : Number(item.finalScore).toFixed(4)}）`"
              :name="`contrib-${item.targetSpotId}`"
            >
              <el-table :data="item.contributors" stripe size="small">
                <el-table-column prop="spotId" label="历史景点ID" width="120" />
                <el-table-column prop="spotName" label="历史景点名称" min-width="180" />
                <el-table-column label="贡献分值" width="140">
                  <template #default="{ row }">
                    <span v-if="row.score != null" class="score-text">{{ Number(row.score).toFixed(4) }}</span>
                    <span v-else class="score-empty">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="贡献说明" min-width="260" />
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div v-if="debugResult" class="debug-sections">
          <div class="debug-block-title">原始摘要</div>
          <el-collapse>
            <el-collapse-item name="raw-debug" title="查看请求与响应摘要">
              <div class="debug-output debug-output--compact">
                <pre>{{ debugOutput }}</pre>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="debug-sections">
          <div class="debug-block-title">完整推荐列表明细</div>
          <el-collapse v-if="debugItems.length">
            <el-collapse-item name="full-list" title="查看完整推荐列表及结果解读">
              <el-table :data="debugTableRows" stripe class="debug-table">
                <el-table-column prop="rank" label="排名" width="80" />
                <el-table-column prop="id" label="景点ID" width="100" />
                <el-table-column prop="name" label="景点名称" min-width="180" />
                <el-table-column prop="categoryName" label="分类" width="140" />
                <el-table-column prop="regionName" label="地区" width="140" />
                <el-table-column label="价格" width="120">
                  <template #default="{ row }">
                    <span>{{ row.priceText }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="评分概况" min-width="180">
                  <template #default="{ row }">
                    <span>{{ row.ratingText }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="推荐分数" width="160">
                  <template #default="{ row }">
                    <span v-if="row.score != null" class="score-text">{{ row.scoreText }}</span>
                    <span v-else class="score-empty">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="结果解读" min-width="260">
                  <template #default="{ row }">
                    <span>{{ row.reason }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
          <el-empty v-else :description="debugResult ? '本次请求已返回空列表，请结合上方调试输出查看原因' : '暂无调试结果'" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="相似邻居预览" name="similarity">
        <div class="debug-toolbar">
          <div class="debug-field">
            <span class="debug-label">景点 ID</span>
            <el-input-number v-model="similarityForm.spotId" :min="1" :step="1" controls-position="right" />
          </div>
          <div class="debug-field">
            <span class="debug-label">邻居数量</span>
            <el-input-number v-model="similarityForm.limit" :min="1" :max="20" :step="1" controls-position="right" />
          </div>
          <div class="preview-action-group">
            <el-button-group>
              <el-button class="cache-preview-button" :loading="similarityPreviewing" @click="emit('preview-similarity')">
                缓存预览
              </el-button>
              <el-button class="update-preview-button" :loading="similarityMatrixPreviewing" @click="emit('preview-similarity-update')">
                更新预览
              </el-button>
            </el-button-group>
          </div>
        </div>

        <div v-if="similarityResult" class="debug-summary">
          <div class="debug-summary-grid debug-summary-grid--triple">
            <div class="summary-card">
              <div class="summary-label">目标景点</div>
              <div class="summary-value summary-value--sm">{{ similarityResult.spotName || '-' }}</div>
              <div class="summary-desc">景点 ID：{{ similarityResult.spotId }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">可用邻居数</div>
              <div class="summary-value">{{ similarityResult.totalNeighbors ?? 0 }}</div>
              <div class="summary-desc">当前从 Redis 相似度矩阵读取</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">矩阵更新时间</div>
              <div class="summary-value summary-value--sm">{{ similarityResult.lastUpdateTime || '暂无记录' }}</div>
              <div class="summary-desc">用于确认预览的矩阵版本</div>
            </div>
          </div>
        </div>

        <el-table v-if="similarityResult?.neighbors?.length" :data="similarityResult.neighbors" stripe class="debug-table">
          <el-table-column prop="spotId" label="相似景点ID" width="120" />
          <el-table-column prop="spotName" label="相似景点名称" min-width="180" />
          <el-table-column prop="categoryName" label="分类" width="140" />
          <el-table-column prop="regionName" label="地区" width="140" />
          <el-table-column label="相似度" width="160">
            <template #default="{ row }">
              <span class="score-text">{{ Number(row.similarity).toFixed(6) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else :description="similarityResult ? '当前景点暂无可预览的相似邻居' : '请输入景点 ID 查看相似邻居'" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { resolveCategoryDisplayName, resolveRegionDisplayName } from '@/shared/lib/resource-display.js'

const debugCardRef = ref()

defineProps({
  activePreviewTab: { type: String, required: true },
  debugForm: { type: Object, required: true },
  similarityForm: { type: Object, required: true },
  previewing: { type: Boolean, required: true },
  similarityPreviewing: { type: Boolean, required: true },
  similarityMatrixPreviewing: { type: Boolean, required: true },
  debugResult: { type: Object, default: null },
  similarityResult: { type: Object, default: null },
  recommendationTypeMeta: { type: Object, required: true },
  debugItems: { type: Array, required: true },
  debugSummaryCards: { type: Array, required: true },
  debugInfo: { type: Object, default: null },
  behaviorStats: { type: Array, required: true },
  behaviorDetails: { type: Array, required: true },
  debugInsights: { type: Array, required: true },
  compactDebugInsights: { type: Array, required: true },
  debugNotes: { type: Array, required: true },
  debugSections: { type: Array, required: true },
  resultContributions: { type: Array, required: true },
  topDebugItems: { type: Array, required: true },
  debugOutput: { type: String, required: true },
  debugTableRows: { type: Array, required: true }
})

const emit = defineEmits([
  'update:active-preview-tab',
  'preview-recommendations',
  'preview-similarity',
  'preview-similarity-update'
])

const formatDescription = (desc) => {
  if (!desc) return ''
  // 高亮带有小数的数字，方便在长说明里快速定位权重和分数变化。
  return desc.replace(/(\d+\.\d+)/g, match => `<span class="highlight-number">${match}</span>`)
}

defineExpose({
  // 暴露根节点给页面层做滚动定位，避免在父层直接依赖内部模板结构。
  get $el() {
    return debugCardRef.value?.$el || debugCardRef.value
  }
})
</script>

<style lang="scss" scoped>
.debug-card {
  border-radius: 22px;
  border: none;
}
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.title-section { display: flex; align-items: center; gap: 12px; }
.title {
  font-size: 16px; font-weight: 600; color: var(--wt-text-primary); position: relative; padding-left: 12px;
  &::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 16px; background: var(--el-color-primary); border-radius: 2px; }
}
.preview-tabs :deep(.el-tabs__header) { margin-bottom: 18px; }
.preview-tabs :deep(.el-tabs__nav-wrap::after) { background-color: var(--wt-border-soft); }
.preview-tabs :deep(.el-tabs__item) { font-weight: 600; }
.debug-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-bottom: 16px; }
.debug-field { display: flex; align-items: center; gap: 8px; }
.debug-label { font-size: 13px; color: var(--wt-text-regular); white-space: nowrap; }
.preview-action-group { display: flex; align-items: center; }
.cache-preview-button { border-color: var(--wt-border-default); background: var(--wt-surface-muted); color: var(--wt-text-regular); }
.update-preview-button { border-color: #ff6b18; background: linear-gradient(135deg, #ff7a1a 0%, #ff5a14 100%); color: #fff; font-weight: 600; box-shadow: 0 8px 18px rgba(255, 107, 24, 0.2); }
.debug-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.debug-summary { margin-bottom: 16px; }
.debug-summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.debug-summary-grid--triple { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.summary-card { padding: 18px; border-radius: 18px; background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%); border: 1px solid var(--wt-border-default); }
.summary-label { margin-bottom: 8px; font-size: 12px; color: var(--wt-text-secondary); }
.summary-value { font-size: 24px; font-weight: 700; color: #1d4ed8; line-height: 1.2; }
.summary-value--sm { font-size: 18px; line-height: 1.4; }
.summary-desc { margin-top: 8px; font-size: 12px; line-height: 1.6; color: var(--wt-text-regular); }
.debug-conclusion, .debug-insights, .debug-top-results, .debug-pipeline, .debug-sections { margin-bottom: 16px; }
.behavior-alert { margin-bottom: 12px; }
.pipeline-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.pipeline-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-tag-warning-bg) 74%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-tag-warning-bg) 48%, var(--wt-surface-muted)) 100%
  );
  border: 1px solid color-mix(in srgb, var(--wt-tag-warning-text) 22%, var(--wt-border-default));
}
.pipeline-label { font-size: 12px; color: color-mix(in srgb, var(--wt-tag-warning-text) 82%, var(--wt-text-secondary)); }
.pipeline-value { margin-top: 8px; font-size: 18px; font-weight: 700; line-height: 1.5; color: color-mix(in srgb, var(--wt-tag-warning-text) 92%, var(--wt-text-primary)); }
.debug-block-title { margin-bottom: 10px; font-size: 14px; font-weight: 700; color: var(--wt-text-primary); }
.insight-list { display: grid; gap: 10px; }
.insight-list--plain { gap: 8px; }
.insight-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-tag-warning-bg) 72%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-tag-warning-bg) 46%, var(--wt-surface-muted)) 100%
  );
  border: 1px solid color-mix(in srgb, var(--wt-tag-warning-text) 20%, var(--wt-border-default));
  color: color-mix(in srgb, var(--wt-tag-warning-text) 88%, var(--wt-text-primary));
  line-height: 1.7;
  font-size: 13px;
}
.insight-item--blue {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-tag-info-bg) 74%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-tag-info-bg) 48%, var(--wt-surface-muted)) 100%
  );
  border-color: color-mix(in srgb, var(--wt-tag-info-text) 20%, var(--wt-border-default));
  color: color-mix(in srgb, var(--wt-tag-info-text) 86%, var(--wt-text-primary));
}
.top-result-list { display: grid; gap: 12px; }
.top-result-card { display: grid; grid-template-columns: 72px minmax(0, 1fr) 150px; align-items: center; gap: 14px; padding: 18px; border-radius: 18px; background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%); border: 1px solid var(--wt-border-default); }
.top-result-rank { width: 56px; height: 56px; display: flex; align-items: center; justify-content: center; border-radius: 16px; background: linear-gradient(135deg, #1677ff 0%, #69b1ff 100%); color: #fff; font-size: 18px; font-weight: 700; }
.top-result-name { font-size: 16px; font-weight: 700; color: var(--wt-text-primary); }
.top-result-meta { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 10px; font-size: 12px; color: var(--wt-text-secondary); }
.top-result-score { text-align: right; }
.score-label { font-size: 12px; color: var(--wt-text-secondary); }
.score-value { margin-top: 6px; font-family: 'Consolas', 'Menlo', monospace; font-size: 22px; font-weight: 700; color: #1677ff; }
.score-value--empty { color: #9ca3af; }
.debug-output { margin-bottom: 16px; padding: 16px 18px; background: linear-gradient(180deg, var(--wt-surface-muted) 0%, var(--wt-surface-elevated) 100%); border: 1px solid var(--wt-border-soft); border-radius: 16px; }
.debug-output--compact { margin-bottom: 0; }
.debug-output pre { margin: 0; white-space: pre-wrap; word-break: break-word; font-size: 13px; line-height: 1.7; color: var(--wt-text-regular); font-family: 'Consolas', 'Menlo', monospace; }
.debug-table { margin-top: 8px; }
.score-text { font-family: 'Consolas', 'Menlo', monospace; color: #1677ff; font-weight: 600; }
.score-empty { color: #bfbfbf; }

.highlight-interactions-section :deep(.el-collapse-item__header) {
  color: #ff6b18;
  font-weight: 700;
  background: linear-gradient(90deg, rgba(255, 107, 24, 0.08) 0%, transparent 100%);
  padding-left: 12px;
  border-left: 3px solid #ff6b18;
}
.highlight-interactions-table {
  border: 1px solid rgba(255, 107, 24, 0.2);
  border-radius: 8px;
  overflow: hidden;
}
:deep(.highlight-number) {
  color: #ff6b18;
  font-family: 'Consolas', 'Menlo', monospace;
  font-weight: 700;
  background: rgba(255, 107, 24, 0.1);
  padding: 0 4px;
  border-radius: 4px;
  margin: 0 2px;
}

@media (max-width: 1200px) {
  .debug-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .pipeline-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .debug-summary-grid--triple { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 768px) {
  .debug-summary-grid, .pipeline-grid, .debug-summary-grid--triple { grid-template-columns: 1fr; }
  .top-result-card { grid-template-columns: 1fr; text-align: left; }
  .top-result-score { text-align: left; }
}
</style>
