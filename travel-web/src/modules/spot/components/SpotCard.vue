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
        <span class="tag-chip plain">{{ spot.regionName || '地区待补充' }}</span>
        <div class="price-box">
          <span class="symbol">¥</span>
          <span class="num">{{ spot.price }}</span>
          <span class="suffix">起</span>
        </div>
      </div>
      <h3 class="spot-name">{{ spot.name }}</h3>
      <div class="spot-tags">
        <span class="tag-chip">{{ spot.categoryName || '分类待补充' }}</span>
        <span class="tag-chip subtle">{{ spot.avgRating ? `评分 ${spot.avgRating}` : '评分待补充' }}</span>
      </div>
      <div class="spot-footer">
        <span class="spot-meta">查看详情</span>
        <span class="spot-rating">立即浏览</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

// 景点卡片保持纯展示职责，点击后的路由跳转由父层统一处理。
defineProps({
  spot: {
    type: Object,
    required: true
  }
})

// 选中事件上抛后，列表和推荐区块都可以复用同一张卡片。
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
  margin-top: 14px;
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
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.tag-chip.plain {
  background: #f3f4f6;
  color: #475569;
}

.tag-chip.subtle {
  background: #fffdf7;
  color: #8a6a2f;
}

.spot-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.spot-meta {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
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
