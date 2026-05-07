<!-- 推荐配置执行区卡片 -->
<template>
  <el-card shadow="hover" class="execution-card">
    <template #header>
      <div class="card-header">
        <div class="title-section">
          <span class="title">执行区</span>
          <el-tag effect="plain" type="warning" round>先保存，再重建矩阵</el-tag>
        </div>
      </div>
    </template>

    <div class="execution-stack">
      <div class="execution-intro feature-panel feature-panel--warning">
        配置修改会立即影响新的推荐请求；相似度矩阵只有在手动更新或定时任务执行后，才会按新参数重算。
      </div>

      <div class="execution-brief feature-panel feature-panel--soft">
        <div class="execution-brief-title">执行建议</div>
        <div class="execution-brief-text">
          修改交互权重、浏览行为修正、近邻数量 K、相似度矩阵 TTL 后，保存配置还不够，还需要手动重建相似度矩阵；热度同步权重、在线候选和用户缓存参数保存后会直接影响新请求。
        </div>
      </div>

      <div
        class="matrix-action-callout feature-panel feature-panel--success"
        :class="{ pending: matrixChangeSummary.needsRebuild, 'feature-panel--warning': matrixChangeSummary.needsRebuild }"
      >
        <div class="matrix-action-title">
          <span>重建相似度矩阵</span>
          <el-tag
            size="small"
            effect="plain"
            :type="matrixChangeSummary.needsRebuild ? 'warning' : 'success'"
            round
          >
            {{ matrixChangeSummary.needsRebuild ? '存在待重建改动' : '当前无需重建' }}
          </el-tag>
        </div>
        <div class="matrix-action-text">
          只有交互权重、浏览细化因子、TopK 和矩阵缓存相关参数会影响离线相似度矩阵。热度同步权重、缓存时长和冷启动参数不需要执行这个操作。
        </div>
      </div>

      <div class="execution-actions">
        <el-button @click="emit('reset-config')" round>
          <el-icon><RefreshLeft /></el-icon>
          恢复默认
        </el-button>
        <el-button type="primary" @click="emit('save-config')" :loading="saving" round>
          <el-icon><Check /></el-icon>
          保存配置
        </el-button>
        <el-button class="matrix-button" @click="emit('update-matrix')" :loading="updatingMatrix" round>
          <el-icon><Refresh /></el-icon>
          重建相似度矩阵
        </el-button>
      </div>

      <div class="execution-grid">
        <div class="execution-metric feature-panel">
          <div class="execution-metric-label">推荐缓存 TTL</div>
          <div class="execution-metric-value">{{ config.cache.userRecTTLMinutes }} 分钟</div>
          <div class="execution-metric-desc">用户推荐结果缓存时长</div>
        </div>
        <div class="execution-metric feature-panel">
          <div class="execution-metric-label">矩阵缓存 TTL</div>
          <div class="execution-metric-value">{{ config.cache.similarityTTLHours }} 小时</div>
          <div class="execution-metric-desc">景点相似度矩阵缓存时长</div>
        </div>
        <div class="execution-metric feature-panel">
          <div class="execution-metric-label">当前矩阵覆盖</div>
          <div class="execution-metric-value">{{ status.totalSpots ?? '-' }} 个景点</div>
          <div class="execution-metric-desc">最近一次离线矩阵计算涉及的景点数量</div>
        </div>
      </div>

      <div class="execution-notes">
        <div class="execution-note feature-panel feature-panel--soft">
          <div class="execution-note-title">推荐链路</div>
          <div class="execution-note-text">行为权重决定候选分数，热度同步权重影响热门排序和最终轻量重排。</div>
        </div>
        <div class="execution-note feature-panel feature-panel--soft">
          <div class="execution-note-title">缓存链路</div>
          <div class="execution-note-text">推荐结果和相似度矩阵都在 Redis 中缓存，但更新节奏不同。</div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { RefreshLeft, Check, Refresh } from '@element-plus/icons-vue'

// 执行动作全部通过事件上抛，确保保存、重建矩阵的副作用仍集中在页面层。
defineProps({
  status: {
    type: Object,
    required: true
  },
  config: {
    type: Object,
    required: true
  },
  matrixChangeSummary: {
    type: Object,
    required: true
  },
  saving: {
    type: Boolean,
    required: true
  },
  updatingMatrix: {
    type: Boolean,
    required: true
  }
})

// 组件本身不持有执行状态，只消费页面层传入的 loading 与摘要信息。
const emit = defineEmits(['reset-config', 'save-config', 'update-matrix'])
</script>

<style lang="scss" scoped>
.execution-card {
  border-radius: 22px;
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: var(--wt-text-primary);
  position: relative;
  padding-left: 12px;

    &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 16px;
      background: var(--el-color-primary);
      border-radius: 2px;
    }
}

.execution-stack {
  display: grid;
  gap: 16px;
}

.execution-intro {
  padding: 16px 18px;
  border-radius: 18px;
  color: color-mix(in srgb, var(--wt-tag-warning-text) 88%, var(--wt-text-primary));
  line-height: 1.7;
  font-size: 13px;
}

.execution-brief {
  padding: 16px 18px;
  border-radius: 18px;
}

.execution-brief-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.execution-brief-text {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

.matrix-action-callout {
  padding: 16px 18px;
  border-radius: 18px;
}

.matrix-action-callout.pending {
}

.matrix-action-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.matrix-action-text {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

.execution-actions {
  display: grid;
  gap: 10px;
}

.execution-actions .el-button {
  justify-content: center;
  margin-left: 0;
}

.execution-actions .matrix-button {
  border-color: color-mix(in srgb, var(--wt-accent-cyan-text) 28%, var(--wt-border-default));
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-accent-cyan-bg) 72%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-accent-cyan-bg) 42%, var(--wt-surface-muted)) 100%
  );
  color: var(--wt-accent-cyan-text);
  font-weight: 600;
}

.execution-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.execution-metric {
  padding: 16px 18px;
  border-radius: 18px;
}

.execution-metric-label {
  font-size: 12px;
  color: var(--wt-text-secondary);
}

.execution-metric-value {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.25;
}

.execution-metric-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--wt-text-regular);
}

.execution-notes {
  display: grid;
  gap: 12px;
}

.execution-note {
  padding: 16px 18px;
  border-radius: 18px;
}

.execution-note-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.execution-note-text {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

@media (max-width: 1200px) {
  .execution-grid {
    grid-template-columns: 1fr;
  }
}
</style>
