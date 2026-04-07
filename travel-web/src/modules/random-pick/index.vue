<!-- 随心一选页 -->
<template>
  <div class="page-container random-pick-page">
    <section class="hero-card premium-card">
      <div class="hero-copy">
        <p class="eyebrow">Random Pick</p>
        <h1 class="title">把选择交给今天的运气</h1>
        <p class="subtitle">这个页面不追求信息量，而是给用户一个更轻、更快的随机出发入口。</p>
      </div>
      <div class="action-bar action-bar-desktop">
        <el-button size="large" @click="drawSpot" :loading="loading">换一个</el-button>
        <el-button size="large" type="primary" :disabled="!spot" @click="goDetail">去看看</el-button>
      </div>
    </section>

    <section v-if="spot" class="picked-card premium-card" @click="goDetail">
      <img :src="getImageUrl(spot.coverImage)" class="picked-image" alt="" />
      <div class="picked-content">
        <span class="picked-label">今天的随机结果</span>
        <h2>{{ spot.name }}</h2>
        <p class="picked-subtitle">{{ spot.regionName || '旅行目的地' }} · {{ spot.categoryName || '景点推荐' }}</p>
        <p class="picked-desc">{{ spot.description || spot.intro || fallbackCopy }}</p>

        <div class="meta-row">
          <span class="meta-pill">★ {{ formatRating(spot.avgRating) }}</span>
          <span class="meta-pill">{{ formatPrice(spot.price) }}</span>
          <span class="meta-pill">随机灵感</span>
        </div>
      </div>
    </section>

    <section v-else-if="loading" class="state-card premium-card loading-card">
      <div class="loading-orb"></div>
      <h2>正在随机选择景点</h2>
      <p>从现有景点里随机抽取一个目的地。</p>
    </section>

    <section v-else class="state-card premium-card">
      <el-empty description="暂时没有抽到合适的景点">
        <template #description>
          <p>暂时没有抽到合适的景点，可以再试一次。</p>
        </template>
      </el-empty>
    </section>

    <section class="tips-card premium-card">
      <p class="eyebrow">Today's Reason</p>
      <h2>今日灵感</h2>
      <p>{{ randomReason }}</p>
    </section>

    <section class="action-bar action-bar-mobile">
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
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'

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
  router.push(buildSpotDetailRoute(spot.value.id, SPOT_DETAIL_SOURCE.RANDOM_PICK))
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
  padding-top: 4px;
  padding-bottom: 32px;
}

.hero-card,
.state-card,
.tips-card {
  padding: 26px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  background:
    radial-gradient(circle at top right, rgba(124, 58, 237, 0.16), transparent 28%),
    linear-gradient(135deg, #faf7ff 0%, #ffffff 58%, #f5f3ff 100%);
}

.eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 700;
}

.title {
  font-size: 36px;
  line-height: 1.08;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.subtitle,
.picked-subtitle,
.picked-desc,
.loading-card p,
.tips-card p {
  color: #64748b;
  line-height: 1.85;
}

.subtitle {
  margin-top: 12px;
}

.picked-card {
  overflow: hidden;
  display: grid;
  grid-template-columns: 1.05fr minmax(0, 1fr);
  cursor: pointer;
}

.picked-image {
  width: 100%;
  height: 100%;
  min-height: 360px;
  object-fit: cover;
}

.picked-content {
  padding: 28px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.picked-label {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f5f3ff;
  color: #7c3aed;
  font-size: 12px;
  font-weight: 700;
  width: fit-content;
}

.picked-content h2,
.tips-card h2,
.loading-card h2 {
  margin-top: 16px;
  font-size: 34px;
  line-height: 1.12;
  color: #0f172a;
  letter-spacing: -0.03em;
}

.picked-desc {
  margin-top: 14px;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
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

.action-bar {
  display: flex;
  gap: 16px;
}

.action-bar-mobile {
  display: none;
}

@media (max-width: 992px) {
  .hero-card,
  .picked-card {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .action-bar-desktop {
    display: none;
  }

  .action-bar-mobile {
    display: flex;
  }
}

@media (max-width: 768px) {
  .title,
  .picked-content h2 {
    font-size: 30px;
  }

  .picked-image {
    min-height: 260px;
  }

  .action-bar-mobile {
    flex-direction: column;
  }
}
</style>
