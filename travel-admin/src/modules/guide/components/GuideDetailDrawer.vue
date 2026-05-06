<!-- 攻略详情抽屉 -->
<template>
  <el-drawer
    :model-value="visible"
    :title="resolveGuideDisplayText(detail?.title) === '--' ? '攻略详情' : resolveGuideDisplayText(detail?.title)"
    size="520px"
    class="guide-detail-drawer"
    @update:model-value="emitVisible"
  >
    <div v-if="detail" class="detail-container">
      <div class="cover-wrapper mb-6">
        <el-image :src="getImageUrl(detail.coverImage)" fit="cover" class="cover-img w-full h-48 rounded-xl shadow-sm" />
        <div class="status-badge" :class="detail.published ? 'published' : 'unpublished'">
          {{ detail.published ? '已发布' : '未发布' }}
        </div>
      </div>

      <div class="info-section mb-6 pb-6 border-b border-gray-100">
        <h2 class="text-2xl font-bold text-gray-800 mb-2 mt-0">{{ resolveGuideDisplayText(detail.title) }}</h2>
        <p class="text-sm text-gray-500 mb-4">{{ resolveCategoryDisplayName(detail.category) }}</p>

        <div class="flex gap-4">
          <div class="stat-item px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">浏览量</div>
            <div class="text-lg font-bold text-gray-800">{{ detail.viewCount ?? 0 }}</div>
          </div>
          <div class="stat-item px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">关联景点</div>
            <div class="text-lg font-bold text-gray-800">{{ detail.spotOptions?.length || 0 }}</div>
          </div>
        </div>
      </div>

      <div class="info-section mb-6">
        <h3 class="section-title">基础信息</h3>
        <el-descriptions :column="1" border class="custom-desc">
          <el-descriptions-item label="分类">{{ resolveCategoryDisplayName(detail.category) }}</el-descriptions-item>
          <el-descriptions-item label="创建者">{{ resolveAdminDisplayName(detail.adminName) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(detail.createdAt) || '--' }}</el-descriptions-item>
          <el-descriptions-item label="修改时间">{{ formatDate(detail.updatedAt) || '--' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="info-section mb-6">
        <h3 class="section-title">关联景点</h3>
        <div v-if="detail.spotOptions?.length" class="spot-tags">
          <el-tag
            v-for="spot in detail.spotOptions"
            :key="spot.id"
            class="spot-tag"
            :type="spot.isDeleted === 1 ? 'danger' : (spot.published === 1 ? 'success' : 'warning')"
            effect="light"
          >
            {{ resolveSpotRecordDisplayName(spot) }}
          </el-tag>
        </div>
        <el-empty v-else description="暂无关联景点" :image-size="60" />
      </div>

      <div class="info-section">
        <h3 class="section-title">攻略内容</h3>
        <div class="content-panel">
          <div v-if="containsHtml(detail.content)" v-html="detail.content"></div>
          <div v-else class="plain-content">{{ resolveGuideDisplayText(detail.content) }}</div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import {
  resolveAdminDisplayName,
  resolveCategoryDisplayName,
  resolveGuideDisplayText,
  resolveSpotRecordDisplayName
} from '@/shared/lib/resource-display.js'

// 详情内容可能来自富文本或纯文本，两种展示方式在这里统一兜底。
defineProps({
  visible: { type: Boolean, required: true },
  detail: { type: Object, default: null },
  getImageUrl: { type: Function, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['update:visible'])

const emitVisible = (value) => {
  emit('update:visible', value)
}

// 历史数据里既有富文本也有纯文本，这里先做轻量判断再决定展示方式。
const containsHtml = (content) => /<[^>]+>/.test(content || '')
</script>

<style lang="scss" scoped>
.mb-6 { margin-bottom: 24px; }
.mb-4 { margin-bottom: 16px; }
.mb-2 { margin-bottom: 8px; }
.mb-1 { margin-bottom: 4px; }
.pb-6 { padding-bottom: 24px; }
.mt-0 { margin-top: 0; }
.w-full { width: 100%; }
.h-48 { height: 192px; }
.px-4 { padding-left: 16px; padding-right: 16px; }
.py-2 { padding-top: 8px; padding-bottom: 8px; }
.gap-4 { gap: 16px; }

.flex { display: flex; }
.flex-1 { flex: 1; min-width: 0; }

.rounded-lg { border-radius: 8px; }
.rounded-xl { border-radius: 12px; }
.shadow-sm { box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); }

.text-xs { font-size: 12px; line-height: 16px; }
.text-sm { font-size: 14px; line-height: 20px; }
.text-lg { font-size: 18px; line-height: 28px; }
.text-2xl { font-size: 24px; line-height: 32px; }

.font-bold { font-weight: 700; }
.text-gray-800 { color: var(--wt-text-primary); }
.text-gray-500 { color: var(--wt-text-regular); }

.border-b { border-bottom-width: 1px; border-bottom-style: solid; }
.border-gray-100 { border-color: var(--wt-divider-soft); }

.cover-wrapper {
  position: relative;
}

.status-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 6px 12px;
  border-radius: 9999px;
  background: color-mix(in srgb, var(--wt-surface-elevated) 92%, transparent);
  backdrop-filter: blur(4px);
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);

  &.published {
    color: var(--wt-accent-emerald-text);
  }

  &.unpublished {
    color: var(--wt-text-secondary);
  }
}

.section-title {
  margin: 0 0 12px;
  padding-left: 8px;
  border-left: 4px solid var(--el-color-primary);
  font-size: 14px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.spot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spot-tag {
  margin-right: 0;
}

.content-panel {
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
  border-radius: 12px;
  padding: 16px;
  color: var(--wt-text-regular);
  line-height: 1.7;
  word-break: break-word;
}

.stat-item {
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
}

.plain-content {
  white-space: pre-line;
}

:deep(.content-panel img) {
  max-width: 100%;
  border-radius: 8px;
}

:deep(.custom-desc) {
  .el-descriptions__body {
    background-color: transparent;
  }

  .el-descriptions__cell {
    padding: 12px 16px !important;
  }

  .el-descriptions__label {
    background-color: var(--el-table-header-bg-color) !important;
    color: var(--wt-text-regular);
    font-weight: 500;
    width: 100px;
  }
}
</style>
