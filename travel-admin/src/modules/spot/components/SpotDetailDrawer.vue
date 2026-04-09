<!-- 景点详情抽屉 -->
<template>
  <el-drawer
    :model-value="visible"
    :title="detail ? detail.name : '景点详情'"
    size="500px"
    @update:model-value="emitVisible"
    class="spot-detail-drawer"
  >
    <div v-if="detail" class="detail-container">
      <div class="cover-wrapper mb-6">
        <el-image :src="getImageUrl(detail.coverImage)" fit="cover" class="cover-img w-full h-48 rounded-xl shadow-sm" />
        <div class="status-badge absolute top-4 right-4 px-3 py-1 rounded-full text-xs font-bold shadow-sm" :class="detail.published ? 'text-green-600' : 'text-gray-500'">
          {{ detail.published ? '已上架' : '未上架' }}
        </div>
      </div>

      <div class="info-section mb-6 pb-6 border-b border-gray-100">
        <h2 class="text-2xl font-bold text-gray-800 mb-2 mt-0">{{ detail.name }}</h2>
        <p class="text-sm text-gray-500 flex items-center gap-2 mb-4">
          <el-icon><Location /></el-icon> {{ detail.address || '暂无地址' }}
        </p>
        
        <div class="flex gap-4">
          <div class="stat-item px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">当前价格</div>
            <div class="text-lg font-bold text-red-500">¥{{ detail.price }}</div>
          </div>
          <div class="stat-item px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">用户评分</div>
            <div class="text-lg font-bold text-gray-800 flex items-center gap-1">
              <el-icon color="#f59e0b"><StarFilled /></el-icon> {{ detail.avgRating || '暂无' }}
            </div>
          </div>
          <div class="stat-item px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">热度分数</div>
            <div class="text-lg font-bold text-gray-800">{{ detail.heatScore || 0 }}</div>
          </div>
        </div>

        <div class="stats-grid mt-4">
          <div class="mini-stat-card">
            <div class="mini-stat-label">评价量</div>
            <div class="mini-stat-value">{{ detail.reviewCount ?? 0 }}</div>
          </div>
          <div class="mini-stat-card">
            <div class="mini-stat-label">收藏量</div>
            <div class="mini-stat-value">{{ detail.favoriteCount ?? 0 }}</div>
          </div>
          <div class="mini-stat-card">
            <div class="mini-stat-label">浏览量</div>
            <div class="mini-stat-value">{{ detail.viewCount ?? 0 }}</div>
          </div>
        </div>
      </div>

      <div class="info-section mb-6">
        <h3 class="text-sm font-bold text-gray-800 mb-3 border-l-4 border-primary pl-2 uppercase tracking-winder">基础信息</h3>
        <el-descriptions :column="1" border class="custom-desc">
          <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地区">{{ detail.regionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开放时间">{{ detail.openTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经纬度">E {{ detail.longitude || '-' }}, N {{ detail.latitude || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(detail.createdAt) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(detail.updatedAt) || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="info-section mb-6">
        <h3 class="text-sm font-bold text-gray-800 mb-3 border-l-4 border-primary pl-2 uppercase tracking-winder">景点介绍</h3>
        <div class="text-sm text-gray-600 content-panel p-4 rounded-lg leading-relaxed whitespace-pre-line">
          {{ detail.description || '暂无详细介绍' }}
        </div>
      </div>

      <div class="info-section" v-if="detail.images?.length">
        <h3 class="text-sm font-bold text-gray-800 mb-3 border-l-4 border-primary pl-2 uppercase tracking-winder">详情图库</h3>
        <div class="grid grid-cols-2 gap-3">
          <el-image v-for="(img, idx) in detail.images" :key="idx" :src="getImageUrl(img)" fit="cover" class="h-24 rounded-lg shadow-sm" :preview-src-list="[getImageUrl(img)]" />
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { Location, StarFilled } from '@element-plus/icons-vue'

// 详情抽屉只处理展示与开关，不在内部复制页面层的数据加工逻辑。
defineProps({
  visible: { type: Boolean, required: true },
  detail: { type: Object, default: null },
  getImageUrl: { type: Function, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['update:visible'])

const emitVisible = (val) => {
  // 统一透传开关事件，方便父层继续沿用 v-model 风格控制抽屉显示。
  emit('update:visible', val)
}
</script>

<style lang="scss" scoped>
.mb-6 { margin-bottom: 24px; }
.mb-4 { margin-bottom: 16px; }
.mb-3 { margin-bottom: 12px; }
.mb-2 { margin-bottom: 8px; }
.mb-1 { margin-bottom: 4px; }
.mt-4 { margin-top: 16px; }
.pb-6 { padding-bottom: 24px; }
.mt-0 { margin-top: 0; }
.w-full { width: 100%; }
.h-48 { height: 192px; }
.h-24 { height: 96px; }
.p-4 { padding: 16px; }
.px-3 { padding-left: 12px; padding-right: 12px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.px-4 { padding-left: 16px; padding-right: 16px; }
.py-2 { padding-top: 8px; padding-bottom: 8px; }
.pl-2 { padding-left: 8px; }
.gap-1 { gap: 4px; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.gap-4 { gap: 16px; }

.flex { display: flex; }
.items-center { align-items: center; }
.flex-1 { flex: 1; min-width: 0; }
.grid { display: grid; }
.grid-cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }

.relative { position: relative; }
.absolute { position: absolute; }
.top-4 { top: 16px; }
.right-4 { right: 16px; }

.rounded-lg { border-radius: 8px; }
.rounded-xl { border-radius: 12px; }
.rounded-full { border-radius: 9999px; }
.shadow-sm { box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); }

.border-b { border-bottom-width: 1px; border-bottom-style: solid; }
.border-l-4 { border-left-width: 4px; border-left-style: solid; }
.border-gray-100 { border-color: var(--wt-divider-soft); }
.border-primary { border-color: var(--el-color-primary); }
.backdrop-blur-sm { backdrop-filter: blur(4px); }

.text-xs { font-size: 12px; line-height: 16px; }
.text-sm { font-size: 14px; line-height: 20px; }
.text-lg { font-size: 18px; line-height: 28px; }
.text-2xl { font-size: 24px; line-height: 32px; }

.font-bold { font-weight: 700; }
.text-gray-800 { color: var(--wt-text-primary); }
.text-gray-600 { color: var(--wt-text-regular); }
.text-gray-500 { color: var(--wt-text-regular); }
.text-red-500 { color: #ef4444; }
.text-green-600 { color: #16a34a; }

.leading-relaxed { line-height: 1.625; }
.whitespace-pre-line { white-space: pre-line; }
.uppercase { text-transform: uppercase; }
.tracking-winder { letter-spacing: 0.05em; }

.cover-wrapper {
  position: relative;
}

.status-badge {
  background: color-mix(in srgb, var(--wt-surface-elevated) 92%, transparent);
  backdrop-filter: blur(4px);
}

.stat-item {
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.mini-stat-card {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--wt-border-default);
  background: var(--wt-surface-elevated);
}

.mini-stat-label {
  font-size: 12px;
  color: var(--wt-text-secondary);
  margin-bottom: 6px;
}

.mini-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.content-panel {
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
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

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
