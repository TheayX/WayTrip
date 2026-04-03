<!-- 近期热看页 -->
<template>
  <div class="page-container trending-page">
    <section class="hero-card">
      <div>
        <p class="hero-eyebrow">Trending Views</p>
        <h1 class="hero-title">近期热看</h1>
        <p class="hero-desc">看看最近一段时间里，大家浏览更多的景点。</p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <strong>{{ trendingSpots.length }}</strong>
          <span>热门景点</span>
        </div>
        <div class="hero-stat">
          <strong>{{ trendingDays }}</strong>
          <span>统计天数</span>
        </div>
      </div>
    </section>

    <section v-if="trendingSpots.length" class="spot-grid">
      <SpotCard
        v-for="item in trendingSpots"
        :key="item.id"
        :spot="item"
        @select="goSpotDetail(item.id)"
      />
    </section>

    <section v-else-if="!loading" class="empty-wrap card">
      <el-empty description="最近还没有可展示的浏览热点">
        <template #description>
          <p>等有更多浏览记录后，这里会自动更新。</p>
        </template>
      </el-empty>
    </section>

    <section v-if="loading" class="loading-row card">
      <p>正在整理近期热看的景点...</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { fetchTrendingViewSpots } from '@/modules/trending-views/api.js'

const router = useRouter()

const trendingSpots = ref([])
const trendingDays = ref(14)
const loading = ref(false)

const loadTrendingSpots = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const result = await fetchTrendingViewSpots()
    trendingSpots.value = result.list
    trendingDays.value = result.days
  } catch (error) {
    console.error('加载近期热看失败', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goSpotDetail = (id) => {
  router.push(`/spots/${id}?source=trending-views`)
}

onMounted(() => {
  loadTrendingSpots()
})
</script>

<style lang="scss" scoped>
.trending-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-top: 8px;
  padding-bottom: 32px;
}

.hero-card,
.empty-wrap,
.loading-row {
  padding: 24px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
  background: linear-gradient(135deg, #fffbeb 0%, #ffffff 56%, #fff7ed 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.24em;
  color: #d97706;
  text-transform: uppercase;
}

.hero-title {
  font-size: 36px;
  margin-bottom: 12px;
}

.hero-desc,
.loading-row p {
  color: #64748b;
  line-height: 1.8;
}

.hero-stats {
  display: flex;
  gap: 14px;
}

.hero-stat {
  min-width: 140px;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
}

.hero-stat strong {
  display: block;
  font-size: 28px;
  color: #111827;
}

.hero-stat span {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 992px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .spot-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 30px;
  }

  .hero-stats {
    width: 100%;
    flex-direction: column;
  }
}
</style>
