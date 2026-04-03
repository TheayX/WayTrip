<!-- 首页附近探索区 -->
<template>
  <section class="section">
    <div class="section-header">
      <h2 class="section-title">附近探索</h2>
      <el-button text type="primary" @click="$emit('more')">查看更多</el-button>
    </div>
    <div class="nearby-panel card">
      <div class="nearby-copy">
        <h3>{{ headline }}</h3>
        <p>{{ summary }}</p>
        <el-button type="primary" :loading="loading" @click="$emit('action')">{{ actionText }}</el-button>
      </div>
      <div v-if="spots.length" class="nearby-list">
        <article v-for="spot in spots.slice(0, 3)" :key="spot.id" class="nearby-item" @click="$emit('select', spot)">
          <img :src="getImageUrl(spot.coverImage)" class="nearby-image" alt="" />
          <div class="nearby-info">
            <h4>{{ spot.name }}</h4>
            <p>{{ spot.regionName || '附近区域' }}</p>
            <span>{{ formatDistance(spot.distanceKm) }}</span>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂未加载附近景点" :image-size="80" />
    </div>
  </section>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

defineProps({
  headline: {
    type: String,
    required: true
  },
  summary: {
    type: String,
    required: true
  },
  actionText: {
    type: String,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  spots: {
    type: Array,
    default: () => []
  },
  formatDistance: {
    type: Function,
    required: true
  }
})

defineEmits(['more', 'action', 'select'])
</script>

<style lang="scss" scoped>
.section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.nearby-panel {
  padding: 22px;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  align-items: start;
}

.nearby-copy h3 {
  margin-bottom: 12px;
  font-size: 24px;
}

.nearby-copy p {
  margin-bottom: 18px;
  color: #606266;
  line-height: 1.7;
}

.nearby-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.nearby-item {
  background: #f8fafc;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
}

.nearby-image {
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.nearby-info {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nearby-info p,
.nearby-info span {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .nearby-panel {
    grid-template-columns: 1fr;
  }

  .nearby-list {
    grid-template-columns: 1fr;
  }
}
</style>
