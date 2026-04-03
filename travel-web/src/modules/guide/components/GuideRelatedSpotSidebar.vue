<!-- 攻略详情关联景点侧栏 -->
<template>
  <div v-if="spots.length" class="guide-sidebar">
    <div class="sidebar-card card">
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
  width: 300px;
  flex-shrink: 0;
}

.sidebar-card {
  padding: 20px;
  border-radius: 12px;
  position: sticky;
  top: 80px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.related-spot {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f5f7fa;
    border-radius: 8px;
    padding-left: 8px;
    padding-right: 8px;
  }
}

.related-img {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.related-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.related-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

@media (max-width: 992px) {
  .guide-sidebar {
    width: 100%;
  }
}
</style>
