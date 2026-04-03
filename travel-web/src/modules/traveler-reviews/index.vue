<!-- 游客口碑页 -->
<template>
  <div class="page-container reviews-page">
    <section class="hero-card">
      <div>
        <p class="hero-eyebrow">Traveler Reviews</p>
        <h1 class="hero-title">游客口碑</h1>
        <p class="hero-desc">先看真实游客怎么说，再决定要不要去。</p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <strong>{{ positiveReviews.length }}</strong>
          <span>高分种草</span>
        </div>
        <div class="hero-stat">
          <strong>{{ negativeReviews.length }}</strong>
          <span>真实避雷</span>
        </div>
      </div>
    </section>

    <section class="tab-bar card">
      <el-segmented v-model="activeTab" :options="tabs" />
    </section>

    <section v-if="activeReviews.length" class="review-list">
      <article v-for="item in activeReviews" :key="item.id" class="review-card card" @click="goSpotDetail(item.spotId)">
        <div class="review-header">
          <div class="user-box">
            <img :src="getAvatarUrl(item.avatar)" class="avatar" alt="" />
            <div class="user-meta">
              <strong>{{ item.nickname || '匿名用户' }}</strong>
              <span>{{ item.spotName || '景点待补充' }}</span>
            </div>
          </div>
          <span class="score-badge" :class="Number(item.score) >= 4 ? 'positive' : 'negative'">
            {{ item.score }} 分
          </span>
        </div>

        <p class="comment">{{ item.comment }}</p>

        <div class="review-footer">
          <span>{{ item.createdAt || '最近发布' }}</span>
          <span class="footer-link">查看景点详情</span>
        </div>
      </article>
    </section>

    <section v-else-if="!loading" class="empty-wrap card">
      <el-empty :description="emptyStateTitle">
        <template #description>
          <p>{{ emptyStateDesc }}</p>
        </template>
      </el-empty>
    </section>

    <section v-if="loading" class="loading-row card">
      <p>正在整理游客评价...</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchTravelerReviewFeed } from '@/modules/traveler-reviews/api.js'
import { getAvatarUrl } from '@/shared/api/client.js'

const router = useRouter()

const tabs = [
  { label: '高分种草', value: 'positive' },
  { label: '真实避雷', value: 'negative' }
]
const activeTab = ref('positive')
const positiveReviews = ref([])
const negativeReviews = ref([])
const loading = ref(false)

const activeReviews = computed(() => (activeTab.value === 'positive' ? positiveReviews.value : negativeReviews.value))
const emptyStateTitle = computed(() => (activeTab.value === 'positive' ? '暂时没有高分种草内容' : '暂时没有真实避雷内容'))
const emptyStateDesc = computed(() => (activeTab.value === 'positive' ? '当前没有高分评论内容。' : '当前没有低分评论内容。'))

const loadTravelerReviewFeed = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const reviewFeed = await fetchTravelerReviewFeed()
    positiveReviews.value = reviewFeed.positive
    negativeReviews.value = reviewFeed.negative
  } catch (error) {
    console.error('加载游客口碑失败', error)
    ElMessage.error('加载口碑失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goSpotDetail = (spotId) => {
  if (!spotId) return
  router.push(`/spots/${spotId}?source=traveler-reviews`)
}

onMounted(() => {
  loadTravelerReviewFeed()
})
</script>

<style lang="scss" scoped>
.reviews-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-top: 8px;
  padding-bottom: 32px;
}

.hero-card,
.tab-bar,
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
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 56%, #eef2ff 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.24em;
  color: #1d4ed8;
  text-transform: uppercase;
}

.hero-title {
  font-size: 36px;
  margin-bottom: 12px;
}

.hero-desc,
.loading-row p,
.comment {
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

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  padding: 22px;
  cursor: pointer;
}

.review-header,
.review-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  background: #e5e7eb;
}

.user-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-meta strong {
  color: #111827;
}

.user-meta span,
.review-footer span {
  color: #64748b;
}

.score-badge {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: 999px;
  font-weight: 700;
}

.score-badge.positive {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.score-badge.negative {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.comment {
  margin-top: 18px;
  margin-bottom: 18px;
}

.footer-link {
  color: #2563eb !important;
}

@media (max-width: 992px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
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

  .review-header,
  .review-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
