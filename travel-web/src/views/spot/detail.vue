<template>
  <div class="spot-detail" v-if="spot">
    <div class="page-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/spots' }">景点</el-breadcrumb-item>
        <el-breadcrumb-item>{{ spot.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-layout">
        <!-- 左侧：图片+详情 -->
        <div class="detail-main">
          <!-- 图片轮播 -->
          <el-carousel height="420px" v-if="spotImages.length" class="image-carousel">
            <el-carousel-item v-for="(img, idx) in spotImages" :key="idx">
              <img :src="img" class="carousel-img" alt="" @click="previewImage(idx)" />
            </el-carousel-item>
          </el-carousel>
          <div class="no-image" v-else>
            <el-empty description="暂无景点图片" />
          </div>

          <!-- 景点简介 -->
          <div class="info-section card">
            <h2 class="section-label">景点简介</h2>
            <p class="desc-text">{{ spot.description || '暂无简介' }}</p>
          </div>

          <!-- 最新评论 -->
          <div class="info-section card">
            <div class="section-header-row">
              <h2 class="section-label">最新评论</h2>
              <el-button text type="primary" @click="loadMoreComments" v-if="hasMoreComments">查看更多</el-button>
            </div>
            <div v-if="comments.length" class="comment-list">
              <div class="comment-item" v-for="comment in comments" :key="comment.id">
                <el-avatar :size="40" :src="comment.avatar" icon="User" />
                <div class="comment-body">
                  <div class="comment-top">
                    <span class="comment-name">{{ comment.nickname }}</span>
                    <span class="star-text">★ {{ comment.score }}</span>
                  </div>
                  <p class="comment-text">{{ comment.comment }}</p>
                  <span class="comment-time">{{ comment.createdAt }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无评论，快来抢沙发吧~" :image-size="80" />
          </div>
        </div>

        <!-- 右侧：购票+信息 -->
        <div class="detail-sidebar">
          <!-- 基本信息卡 -->
          <div class="sidebar-card card">
            <h1 class="spot-name">{{ spot.name }}</h1>
            <div class="spot-meta">
              <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
              <span class="meta-count">({{ spot.ratingCount || 0 }}条评价)</span>
              <el-divider direction="vertical" />
              <span>{{ spot.regionName }} · {{ spot.categoryName }}</span>
            </div>
            <div class="spot-price-row">
              <span class="big-price">¥{{ spot.price }}</span>
              <span class="price-label">/人</span>
            </div>
            <el-button type="primary" size="large" class="buy-btn" @click="handleBuy">立即购票</el-button>
            <el-button
              :type="spot.isFavorite ? 'warning' : 'default'"
              size="large"
              class="fav-btn"
              @click="toggleFavorite"
            >
              {{ spot.isFavorite ? '❤️ 已收藏' : '🤍 收藏' }}
            </el-button>
          </div>

          <!-- 详细信息 -->
          <div class="sidebar-card card">
            <div class="detail-item">
              <span class="detail-label">开放时间</span>
              <span class="detail-value">{{ spot.openTime || '暂无信息' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">景点地址</span>
              <span class="detail-value">{{ spot.address || '暂无信息' }}</span>
            </div>
          </div>

          <!-- 写评价 -->
          <div class="sidebar-card card">
            <h3 class="sidebar-title">写评价</h3>
            <div class="rating-input">
              <span class="rating-label">评分：</span>
              <el-rate v-model="ratingForm.score" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
            </div>
            <el-input
              v-model="ratingForm.comment"
              type="textarea"
              :rows="3"
              placeholder="分享你的旅行体验..."
              maxlength="500"
              show-word-limit
            />
            <el-button type="primary" class="submit-rating-btn" :loading="submittingRating" @click="handleSubmitRating">
              提交评价
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="spotImages"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
  <div v-else class="page-container">
    <el-skeleton :rows="10" animated />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getSpotDetail } from '@/api/spot'
import { addFavorite, removeFavorite, checkFavorite } from '@/api/favorite'
import { submitRating, getSpotRatings } from '@/api/rating'
import { getImageUrl } from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const spot = ref(null)
const comments = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const hasMoreComments = computed(() => comments.value.length < commentTotal.value)

const previewVisible = ref(false)
const previewIndex = ref(0)
const submittingRating = ref(false)

const ratingForm = reactive({
  score: 5,
  comment: ''
})

const spotImages = computed(() => {
  if (!spot.value) return []
  const imgs = []
  if (spot.value.coverImage) imgs.push(getImageUrl(spot.value.coverImage))
  if (spot.value.images) {
    const list = typeof spot.value.images === 'string'
      ? spot.value.images.split(',')
      : spot.value.images
    list.forEach(img => {
      const url = getImageUrl(img.trim())
      if (url && !imgs.includes(url)) imgs.push(url)
    })
  }
  return imgs
})

const previewImage = (idx) => {
  previewIndex.value = idx
  previewVisible.value = true
}

const fetchDetail = async () => {
  try {
    const res = await getSpotDetail(route.params.id)
    spot.value = res.data
  } catch (e) {
    ElMessage.error('获取景点详情失败')
  }
}

const fetchComments = async (refresh = false) => {
  try {
    if (refresh) {
      commentPage.value = 1
      comments.value = []
    }
    const res = await getSpotRatings(route.params.id, commentPage.value, 5)
    const list = res.data?.list || res.data || []
    comments.value = refresh ? list : [...comments.value, ...list]
    commentTotal.value = res.data?.total || 0
    commentPage.value++
  } catch (e) { /* ignore */ }
}

const loadMoreComments = () => {
  if (hasMoreComments.value) fetchComments()
}

const handleBuy = () => {
  if (!userStore.isLoggedIn) {
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  router.push(`/order/create/${spot.value.id}`)
}

const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  try {
    if (spot.value.isFavorite) {
      await removeFavorite(spot.value.id)
      spot.value.isFavorite = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(spot.value.id)
      spot.value.isFavorite = true
      ElMessage.success('收藏成功')
    }
  } catch (e) { /* ignore */ }
}

const handleSubmitRating = async () => {
  if (!userStore.isLoggedIn) {
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  if (!ratingForm.score) {
    ElMessage.warning('请选择评分')
    return
  }
  submittingRating.value = true
  try {
    await submitRating({
      spotId: Number(route.params.id),
      score: ratingForm.score,
      comment: ratingForm.comment
    })
    ElMessage.success('评价成功')
    ratingForm.score = 5
    ratingForm.comment = ''
    fetchComments(true)
    fetchDetail()
  } catch (e) { /* ignore */ }
  submittingRating.value = false
}

onMounted(() => {
  fetchDetail()
  fetchComments(true)
})
</script>

<style lang="scss" scoped>
.detail-layout {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.detail-main {
  flex: 1;
  min-width: 0;
}

.detail-sidebar {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Carousel */
.image-carousel {
  border-radius: 12px;
  overflow: hidden;
}

.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.no-image {
  height: 300px;
  background: #f5f7fa;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Info sections */
.info-section {
  margin-top: 16px;
  padding: 24px;
  border-radius: 12px;
}

.section-label {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.desc-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* Comments */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-body {
  flex: 1;
}

.comment-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.comment-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.comment-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 4px;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}

/* Sidebar */
.sidebar-card {
  padding: 20px;
  border-radius: 12px;
}

.spot-name {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.spot-meta {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.meta-count {
  color: #c0c4cc;
}

.spot-price-row {
  margin-bottom: 16px;
}

.big-price {
  font-size: 32px;
  font-weight: 700;
  color: #f56c6c;
}

.price-label {
  font-size: 14px;
  color: #909399;
}

.buy-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.fav-btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.detail-label {
  font-size: 14px;
  color: #909399;
  flex-shrink: 0;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  text-align: right;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.rating-input {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.rating-label {
  font-size: 14px;
  color: #606266;
}

.submit-rating-btn {
  width: 100%;
  margin-top: 12px;
  border-radius: 8px;
}

@media (max-width: 992px) {
  .detail-layout {
    flex-direction: column;
  }

  .detail-sidebar {
    width: 100%;
  }
}
</style>

