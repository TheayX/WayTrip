<!-- 景点详情相似推荐区 -->
<template>
  <div v-if="items.length" class="info-section premium-card">
    <div class="section-header-row">
      <div>
        <p class="section-kicker">Similar Spots</p>
        <h2 class="section-label">看了又看</h2>
      </div>
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
  padding: 24px;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.section-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.section-label {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.03em;
}

.section-hint {
  color: #64748b;
  font-size: 13px;
}

.similar-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.similar-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  cursor: pointer;
}

.similar-image {
  width: 120px;
  height: 96px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
}

.similar-content {
  flex: 1;
  min-width: 0;
}

.similar-content h3 {
  margin-bottom: 8px;
  font-size: 16px;
  color: #0f172a;
}

.similar-content p {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 10px;
  line-height: 1.7;
}

.similar-bottom {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.similar-score {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 992px) {
  .similar-list {
    grid-template-columns: 1fr;
  }
}
</style>
