<!-- 景点卡片 -->
<template>
  <article class="spot-card premium-card" @click="$emit('select', spot)">
    <div class="spot-image-box">
      <img :src="getImageUrl(spot.coverImage)" class="spot-image" alt="" />
      <div v-if="spot.avgRating" class="rating-badge">
        <span class="score">{{ spot.avgRating }}</span>
        <span class="unit">评分</span>
      </div>
    </div>
    <div class="spot-content">
      <div class="spot-head">
        <h3 class="spot-name">{{ spot.name }}</h3>
        <div class="price-box">
          <span class="symbol">¥</span>
          <span class="num">{{ spot.price }}</span>
          <span class="suffix">起</span>
        </div>
      </div>
      <div class="spot-tags">
        <span class="tag-chip plain">{{ spot.regionName || '地区待补充' }}</span>
        <span class="tag-chip">{{ spot.categoryName || '分类待补充' }}</span>
      </div>
      <div class="spot-footer">
        <span class="spot-meta">适合继续深入浏览详情与相关推荐</span>
        <span class="spot-rating">评分 {{ spot.avgRating || '-' }}</span>
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

.spot-card:hover {
  transform: translateY(-2px);
}

.spot-image-box {
  position: relative;
}

.spot-image {
  width: 100%;
  height: 250px;
  object-fit: cover;
}

.rating-badge {
  position: absolute;
  left: 16px;
  bottom: 16px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  display: flex;
  align-items: center;
  gap: 4px;
}

.score {
  color: #c8a95b;
  font-weight: 700;
}

.unit {
  color: #64748b;
  font-size: 12px;
}

.spot-content {
  padding: 18px 18px 20px;
}

.spot-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.spot-name {
  flex: 1;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
  color: #0f172a;
}

.spot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0 16px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #e8f1ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.tag-chip.plain {
  background: #f3f4f6;
  color: #475569;
}

.spot-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.spot-meta {
  color: #64748b;
  font-size: 13px;
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
.spot-rating {
  color: #64748b;
  font-size: 13px;
}
</style>
