<!-- 景点卡片 -->
<template>
  <article class="spot-card card" @click="$emit('select', spot)">
    <div class="spot-image-box">
      <img :src="getImageUrl(spot.coverImage)" class="spot-image" alt="" />
      <div v-if="spot.avgRating" class="rating-badge">
        <span class="score">{{ spot.avgRating }}</span>
        <span class="unit">分</span>
      </div>
    </div>
    <div class="spot-content">
      <h3 class="spot-name">{{ spot.name }}</h3>
      <div class="spot-tags">
        <span class="tag-chip plain">{{ spot.regionName || '地区待补充' }}</span>
        <span class="tag-chip">{{ spot.categoryName || '分类待补充' }}</span>
      </div>
      <div class="spot-footer">
        <div class="price-box">
          <span class="symbol">¥</span>
          <span class="num">{{ spot.price }}</span>
          <span class="suffix">起</span>
        </div>
        <div class="meta-box">
          <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

defineProps({
  spot: {
    type: Object,
    required: true
  }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
.spot-card {
  overflow: hidden;
  cursor: pointer;
}

.spot-image-box {
  position: relative;
}

.spot-image {
  width: 100%;
  height: 240px;
  object-fit: cover;
}

.rating-badge {
  position: absolute;
  left: 14px;
  bottom: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score {
  color: #f59e0b;
  font-weight: 700;
}

.unit {
  color: #64748b;
  font-size: 12px;
}

.spot-content {
  padding: 16px;
}

.spot-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.spot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e8f1ff;
  color: #2563eb;
  font-size: 13px;
}

.tag-chip.plain {
  background: #f3f4f6;
  color: #4b5563;
}

.spot-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.price-box {
  display: flex;
  align-items: baseline;
  color: #ef4444;
}

.symbol {
  font-size: 14px;
}

.num {
  font-size: 28px;
  font-weight: 700;
}

.suffix,
.meta-box {
  color: #909399;
  font-size: 13px;
}
</style>
