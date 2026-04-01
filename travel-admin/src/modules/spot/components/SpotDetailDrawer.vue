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
        <div class="status-badge absolute top-4 right-4 bg-white/90 backdrop-blur-sm px-3 py-1 rounded-full text-xs font-bold shadow-sm" :class="detail.published ? 'text-green-600' : 'text-gray-500'">
          {{ detail.published ? '已上架' : '未上架' }}
        </div>
      </div>

      <div class="info-section mb-6 pb-6 border-b border-gray-100">
        <h2 class="text-2xl font-bold text-gray-800 mb-2 mt-0">{{ detail.name }}</h2>
        <p class="text-sm text-gray-500 flex items-center gap-2 mb-4">
          <el-icon><Location /></el-icon> {{ detail.address || '暂无地址' }}
        </p>
        
        <div class="flex gap-4">
          <div class="stat-item bg-gray-50 px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">当前价格</div>
            <div class="text-lg font-bold text-red-500">¥{{ detail.price }}</div>
          </div>
          <div class="stat-item bg-gray-50 px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">用户评分</div>
            <div class="text-lg font-bold text-gray-800 flex items-center gap-1">
              <el-icon color="#f59e0b"><StarFilled /></el-icon> {{ detail.avgRating || '暂无' }}
            </div>
          </div>
          <div class="stat-item bg-gray-50 px-4 py-2 rounded-lg flex-1">
            <div class="text-xs text-gray-500 mb-1">热度</div>
            <div class="text-lg font-bold text-gray-800">{{ detail.heatScore || 0 }}</div>
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
        </el-descriptions>
      </div>

      <div class="info-section mb-6">
        <h3 class="text-sm font-bold text-gray-800 mb-3 border-l-4 border-primary pl-2 uppercase tracking-winder">景点介绍</h3>
        <div class="text-sm text-gray-600 bg-gray-50 p-4 rounded-lg leading-relaxed whitespace-pre-line">
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

defineProps({
  visible: { type: Boolean, required: true },
  detail: { type: Object, default: null },
  getImageUrl: { type: Function, required: true }
})

const emit = defineEmits(['update:visible'])

const emitVisible = (val) => {
  emit('update:visible', val)
}
</script>

<style lang="scss" scoped>
.mb-6 { margin-bottom: 24px; }
.mb-4 { margin-bottom: 16px; }
.mb-3 { margin-bottom: 12px; }
.mb-2 { margin-bottom: 8px; }
.mb-1 { margin-bottom: 4px; }
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
.border-gray-100 { border-color: #f3f4f6; }
.border-primary { border-color: var(--el-color-primary); }

.bg-white\/90 { background-color: rgba(255, 255, 255, 0.9); }
.backdrop-blur-sm { backdrop-filter: blur(4px); }
.bg-gray-50 { background-color: #f9fafb; }

.text-xs { font-size: 12px; line-height: 16px; }
.text-sm { font-size: 14px; line-height: 20px; }
.text-lg { font-size: 18px; line-height: 28px; }
.text-2xl { font-size: 24px; line-height: 32px; }

.font-bold { font-weight: 700; }
.text-gray-800 { color: #1f2937; }
.text-gray-600 { color: #4b5563; }
.text-gray-500 { color: #6b7280; }
.text-red-500 { color: #ef4444; }
.text-green-600 { color: #16a34a; }

.leading-relaxed { line-height: 1.625; }
.whitespace-pre-line { white-space: pre-line; }
.uppercase { text-transform: uppercase; }
.tracking-winder { letter-spacing: 0.05em; }

.cover-wrapper {
  position: relative;
}

:deep(.custom-desc) {
  .el-descriptions__body {
    background-color: transparent;
  }
  .el-descriptions__cell {
    padding: 12px 16px !important;
  }
  .el-descriptions__label {
    background-color: #f8fafc !important;
    color: #64748b;
    font-weight: 500;
    width: 100px;
  }
}
</style>
