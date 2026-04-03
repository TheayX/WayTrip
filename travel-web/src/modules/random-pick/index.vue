<!-- 随心一选页 -->
<template>
  <div class="page-container random-pick-page">
    <section v-if="spot" class="hero-card" @click="goDetail">
      <img :src="getImageUrl(spot.coverImage)" class="hero-image" alt="" />
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <p class="eyebrow">随心一选</p>
        <h1 class="title">{{ spot.name }}</h1>
        <p class="subtitle">{{ spot.regionName || '旅行目的地' }} · {{ spot.categoryName || '景点推荐' }}</p>
        <p class="desc">{{ spot.description || spot.intro || fallbackCopy }}</p>

        <div class="meta-row">
          <span class="meta-pill">★ {{ formatRating(spot.avgRating) }}</span>
          <span class="meta-pill">{{ formatPrice(spot.price) }}</span>
        </div>
      </div>
    </section>

    <section v-else-if="loading" class="state-card loading-card">
      <div class="loading-orb"></div>
      <h2>正在随机选择景点</h2>
      <p>从现有景点里随机抽取一个目的地。</p>
    </section>

    <section v-else class="state-card">
      <el-empty description="暂时没有抽到合适的景点">
        <template #description>
          <p>暂时没有抽到合适的景点，可以再试一次。</p>
        </template>
      </el-empty>
    </section>

    <section class="tips-card card">
      <h2>今日灵感</h2>
      <p>{{ randomReason }}</p>
    </section>

    <section class="action-bar">
      <el-button size="large" @click="drawSpot" :loading="loading">换一个</el-button>
      <el-button size="large" type="primary" :disabled="!spot" @click="goDetail">去看看</el-button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getImageUrl } from '@/shared/api/client.js'
import { fetchRandomPickSpot } from '@/modules/random-pick/api.js'

const router = useRouter()

// 页面数据状态
const spot = ref(null)
const loading = ref(false)
const lastSpotId = ref(null)

const randomPickReasons = [
  '今天不纠结，直接把选择交给运气。',
  '换个平时不会主动点开的目的地，可能更有惊喜。',
  '先出发，再决定要不要认真做攻略。',
  '不想做决定的时候，随机反而更像旅行。'
]

const fallbackCopy = '给自己一个随机出发的理由，今天就去看看新的风景。'
const randomReason = ref(randomPickReasons[0])

const getRandomReason = () => {
  const index = Math.floor(Math.random() * randomPickReasons.length)
  return randomPickReasons[index]
}

const formatRating = (value) => {
  const rating = Number(value)
  if (!Number.isFinite(rating) || rating <= 0) return '暂无评分'
  return `${rating.toFixed(1)} 分`
}

const formatPrice = (value) => {
  const price = Number(value)
  if (!Number.isFinite(price)) return '价格待定'
  if (price <= 0) return '¥0 免费'
  return `¥${price}`
}

// 随心一选先复用景点列表做抽样，保证首版能闭环，后续切正式接口时不影响页面层。
const drawSpot = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const nextSpot = await fetchRandomPickSpot({ excludeSpotId: lastSpotId.value })
    spot.value = nextSpot || null
    lastSpotId.value = nextSpot?.id || null
    randomReason.value = getRandomReason()
  } catch (error) {
    console.error('加载随心一选失败', error)
    ElMessage.error('随机抽取失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goDetail = () => {
  if (!spot.value?.id) return
  router.push(`/spots/${spot.value.id}?source=random-pick`)
}

onMounted(() => {
  drawSpot()
})
</script>

<style lang="scss" scoped>
.random-pick-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 8px;
  padding-bottom: 32px;
}

.hero-card {
  position: relative;
  min-height: 540px;
  border-radius: 28px;
  overflow: hidden;
  cursor: pointer;
  background:
    radial-gradient(circle at top, rgba(124, 58, 237, 0.22), transparent 36%),
    linear-gradient(135deg, #1f1637 0%, #31224f 60%, #0f172a 100%);
  box-shadow: 0 24px 60px rgba(31, 22, 55, 0.24);
}

.hero-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(17, 24, 39, 0.14) 0%, rgba(17, 24, 39, 0.78) 100%);
}

.hero-content {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 36px;
  color: #fff;
}

.eyebrow {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
}

.title {
  margin-top: 16px;
  font-size: 44px;
  line-height: 1.15;
}

.subtitle {
  margin-top: 12px;
  font-size: 18px;
  color: rgba(255, 255, 255, 0.86);
}

.desc {
  margin-top: 16px;
  max-width: 760px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.92);
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.state-card,
.tips-card {
  padding: 24px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.loading-orb {
  width: 88px;
  height: 88px;
  border-radius: 28px;
  background: linear-gradient(135deg, #7c3aed 0%, #c4b5fd 100%);
  box-shadow: 0 18px 36px rgba(124, 58, 237, 0.22);
}

.loading-card h2,
.tips-card h2 {
  margin-top: 18px;
  margin-bottom: 10px;
  font-size: 24px;
}

.loading-card p,
.tips-card p {
  color: #64748b;
  line-height: 1.8;
}

.action-bar {
  display: flex;
  gap: 16px;
}

@media (max-width: 768px) {
  .hero-card {
    min-height: 460px;
  }

  .hero-content {
    padding: 24px;
  }

  .title {
    font-size: 32px;
  }

  .action-bar {
    flex-direction: column;
  }
}
</style>
