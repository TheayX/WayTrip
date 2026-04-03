<!-- 景点详情相似推荐区 -->
<template>
  <div v-if="items.length" class="info-section card">
    <div class="section-header-row">
      <h2 class="section-label">看了又看</h2>
      <span class="section-hint">{{ updateTime ? `更新于 ${updateTime}` : '相似景点' }}</span>
    </div>
    <div class="similar-list">
      <article v-for="item in items" :key="item.spotId" class="similar-item" @click="$emit('select', item)">
        <img :src="getImageUrl(item.coverImage)" class="similar-image" alt="" />
        <div class="similar-content">
          <h3>{{ item.spotName }}</h3>
          <p>{{ item.regionName || '周边景点' }} · {{ item.categoryName || '推荐' }}</p>
          <div class="similar-bottom">
            <span class="price">¥{{ item.price || 0 }}</span>
            <span class="similar-score">相似度 {{ formatSimilarity(item.similarity) }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

defineProps({
  items: {
    type: Array,
    default: () => []
  },
  updateTime: {
    type: String,
    default: ''
  },
  formatSimilarity: {
    type: Function,
    required: true
  }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
.info-section {
  margin-top: 16px;
  padding: 24px;
  border-radius: 12px;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-label {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.section-hint {
  color: #909399;
  font-size: 13px;
}

.similar-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.similar-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  cursor: pointer;
}

.similar-image {
  width: 120px;
  height: 90px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.similar-content {
  flex: 1;
  min-width: 0;
}

.similar-content h3 {
  margin-bottom: 8px;
  font-size: 15px;
}

.similar-content p {
  color: #909399;
  font-size: 13px;
  margin-bottom: 10px;
}

.similar-bottom {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.similar-score {
  color: #409eff;
  font-size: 12px;
}

@media (max-width: 992px) {
  .similar-list {
    grid-template-columns: 1fr;
  }
}
</style>
