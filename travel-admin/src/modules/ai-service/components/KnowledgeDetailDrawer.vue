<!-- AI 知识详情抽屉 -->
<template>
  <el-drawer
    :model-value="visible"
    title="知识详情"
    size="640px"
    class="knowledge-detail-drawer"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading" class="detail-container">
      <template v-if="detail">
        <section class="detail-hero">
          <div class="detail-hero__header">
            <div>
              <p class="detail-kicker">Knowledge Document</p>
              <h3 class="detail-title">{{ detail.title || '未命名文档' }}</h3>
            </div>
            <el-tag effect="light" round :type="detail.isEnabled ? 'success' : 'info'">
              {{ detail.isEnabled ? '已启用' : '已停用' }}
            </el-tag>
          </div>

          <div class="detail-stats">
            <div class="detail-stat-card">
              <span class="detail-stat-label">知识域</span>
              <strong>{{ getDomainLabel(detail.knowledgeDomain) }}</strong>
            </div>
            <div class="detail-stat-card">
              <span class="detail-stat-label">来源类型</span>
              <strong>{{ getSourceTypeLabel(detail.sourceType) }}</strong>
            </div>
            <div class="detail-stat-card">
              <span class="detail-stat-label">分片数量</span>
              <strong>{{ detail.chunkCount }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <h4 class="section-title">核心元数据</h4>
          <el-descriptions :column="1" border class="detail-descriptions">
            <el-descriptions-item label="文档 ID">{{ detail.id ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="知识域">{{ getDomainLabel(detail.knowledgeDomain) }}</el-descriptions-item>
            <el-descriptions-item label="来源类型">{{ getSourceTypeLabel(detail.sourceType) }}</el-descriptions-item>
            <el-descriptions-item label="来源标识">{{ detail.sourceRef || '--' }}</el-descriptions-item>
            <el-descriptions-item label="标签">{{ detail.tags || '--' }}</el-descriptions-item>
            <el-descriptions-item label="版本">v{{ detail.version || 1 }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ detail.updatedAt || '--' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <h4 class="section-title">完整内容</h4>
          <div class="content-panel">{{ detail.content || '暂无内容' }}</div>
        </section>

        <section class="detail-section">
          <div class="section-header">
            <h4 class="section-title section-title--plain">分片列表</h4>
            <span class="section-hint">共 {{ detail.chunkCount }} 条</span>
          </div>

          <div v-if="detail.chunks.length" class="chunk-list">
            <div v-for="(chunk, index) in detail.chunks" :key="chunk.chunkId || index" class="chunk-card">
              <div class="chunk-card__header">
                <span class="chunk-index">分片 {{ index + 1 }}</span>
                <span class="chunk-meta">ID {{ chunk.chunkId || '--' }}</span>
              </div>
              <div class="chunk-card__body">{{ chunk.snippet || '暂无分片内容' }}</div>
              <div class="chunk-card__footer">
                <span>{{ getSourceTypeLabel(chunk.sourceType || detail.sourceType) }}</span>
                <span>{{ chunk.sourceRef || detail.sourceRef || '无来源标识' }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无分片数据" :image-size="72" />
        </section>
      </template>

      <el-empty v-else description="暂无文档详情" :image-size="72" />
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="emit('update:visible', false)">关闭</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, required: true },
  loading: { type: Boolean, default: false },
  detail: { type: Object, default: null },
  getDomainLabel: { type: Function, required: true },
  getSourceTypeLabel: { type: Function, required: true }
})

const emit = defineEmits(['update:visible'])
</script>

<style lang="scss" scoped>
.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-hero {
  border-radius: 18px;
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  color: #fff;
  padding: 20px;
}

.detail-hero__header,
.detail-stats,
.drawer-footer,
.section-header,
.chunk-card__header,
.chunk-card__footer {
  display: flex;
  gap: 12px;
}

.detail-hero__header,
.section-header,
.chunk-card__header,
.chunk-card__footer {
  align-items: center;
  justify-content: space-between;
}

.detail-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  opacity: 0.86;
}

.detail-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.detail-stats {
  margin-top: 18px;
}

.detail-stat-card {
  flex: 1;
  min-width: 0;
  border-radius: 14px;
  background: color-mix(in srgb, white 18%, transparent);
  padding: 14px 16px;
}

.detail-stat-label {
  display: block;
  font-size: 12px;
  opacity: 0.86;
  margin-bottom: 6px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  margin: 0;
  padding-left: 10px;
  border-left: 4px solid var(--el-color-primary);
  color: var(--wt-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.section-title--plain {
  padding-left: 0;
  border-left: 0;
}

.section-hint {
  font-size: 12px;
  color: var(--wt-text-secondary);
}

.content-panel {
  border-radius: 14px;
  border: 1px solid var(--wt-border-default);
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  padding: 16px;
  color: var(--wt-text-regular);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chunk-card {
  border-radius: 14px;
  border: 1px solid var(--wt-border-default);
  background: var(--wt-surface-elevated);
  padding: 14px 16px;
}

.chunk-index {
  font-size: 13px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.chunk-meta,
.chunk-card__footer {
  font-size: 12px;
  color: var(--wt-text-secondary);
}

.chunk-card__body {
  margin-top: 10px;
  color: var(--wt-text-regular);
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.chunk-card__footer {
  margin-top: 10px;
  flex-wrap: wrap;
}

.drawer-footer {
  width: 100%;
  justify-content: flex-end;
}

:deep(.detail-descriptions .el-descriptions__label) {
  width: 110px;
  background: var(--el-table-header-bg-color) !important;
  color: var(--wt-text-regular);
  font-weight: 500;
}

:deep(.detail-descriptions .el-descriptions__cell) {
  padding: 12px 16px !important;
}

@media (max-width: 768px) {
  .detail-stats {
    flex-direction: column;
  }
}
</style>
