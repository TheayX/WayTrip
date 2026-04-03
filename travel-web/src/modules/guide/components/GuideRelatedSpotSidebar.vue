<!-- 攻略详情关联景点侧栏 -->
<template>
  <div v-if="spots.length" class="guide-sidebar">
    <div class="sidebar-card premium-card">
      <p class="sidebar-kicker">Related Spots</p>
      <h3 class="sidebar-title">相关景点</h3>
      <div
        v-for="spot in spots"
        :key="spot.id"
        class="related-spot"
        @click="$emit('select', spot)"
      >
        <img :src="getImageUrl(spot.coverImage)" class="related-img" alt="" />
        <div class="related-info">
          <span class="related-name">{{ spot.name }}</span>
          <span class="related-meta">{{ spot.regionName || '精选景点' }}</span>
          <span class="price">¥{{ spot.price }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

defineProps({
  spots: {
    type: Array,
    default: () => []
  }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
.guide-sidebar {
  width: 320px;
  flex-shrink: 0;
}

.sidebar-card {
  padding: 20px;
  position: sticky;
  top: 150px;
}

.sidebar-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.sidebar-title {
  font-size: 22px;
  color: #0f172a;
}

.related-spot {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 1px solid #eef2f7;
  transition: transform 0.2s ease;
}

.related-spot:last-child {
  border-bottom: none;
}

.related-spot:hover {
  transform: translateX(2px);
}

.related-img {
  width: 92px;
  height: 72px;
  object-fit: cover;
  border-radius: 12px;
  flex-shrink: 0;
}

.related-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.related-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.related-meta {
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 992px) {
  .guide-sidebar {
    width: 100%;
  }

  .sidebar-card {
    position: static;
  }
}
</style>
