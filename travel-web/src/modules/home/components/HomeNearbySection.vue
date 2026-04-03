<!-- 首页附近探索区 -->
<template>
  <section class="nearby-section">
    <div class="section-header">
      <div>
        <p class="section-kicker">Nearby</p>
        <h2 class="section-title">附近探索</h2>
      </div>
      <button type="button" class="section-link" @click="$emit('more')">查看更多</button>
    </div>

    <div class="nearby-panel premium-card">
      <div class="nearby-copy">
        <span class="copy-pill">{{ headline }}</span>
        <h3>把距离感放回旅行决策里。</h3>
        <p>{{ summary }}</p>
        <el-button type="primary" :loading="loading" @click="$emit('action')">{{ actionText }}</el-button>
      </div>

      <div v-if="spots.length" class="nearby-list">
        <article v-for="spot in spots.slice(0, 3)" :key="spot.id" class="nearby-item" @click="$emit('select', spot)">
          <img :src="getImageUrl(spot.coverImage)" class="nearby-image" alt="" />
          <div class="nearby-info">
            <div class="nearby-info-top">
              <h4>{{ spot.name }}</h4>
              <span>{{ formatDistance(spot.distanceKm) }}</span>
            </div>
            <p>{{ spot.regionName || '附近区域' }}</p>
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
.nearby-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.section-link {
  border: none;
  padding: 0;
  background: transparent;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease;
}

.section-link:hover {
  color: #0f172a;
}

.nearby-panel {
  padding: 26px;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 22px;
  align-items: stretch;
}

.nearby-copy {
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.copy-pill {
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
}

.nearby-copy h3 {
  margin-top: 16px;
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.nearby-copy p {
  margin: 14px 0 22px;
  color: #64748b;
  line-height: 1.75;
}

.nearby-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.nearby-item {
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.nearby-item:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 24px 30px -24px rgba(15, 23, 42, 0.35);
}

.nearby-image {
  width: 100%;
  height: 178px;
  object-fit: cover;
}

.nearby-info {
  padding: 14px 14px 16px;
}

.nearby-info-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.nearby-info h4 {
  font-size: 16px;
  color: #0f172a;
}

.nearby-info p {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.6;
}

.nearby-info span {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
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
