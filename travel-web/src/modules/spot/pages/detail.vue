<!-- 景点详情页 -->
<template>
  <div v-if="spot" class="spot-detail">
    <div class="page-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/spots' }">景点</el-breadcrumb-item>
        <el-breadcrumb-item>{{ spot.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-layout">
        <div class="detail-main">
          <el-carousel v-if="spotImages.length" height="420px" class="image-carousel">
            <el-carousel-item v-for="(img, idx) in spotImages" :key="idx">
              <img :src="img" class="carousel-img" alt="" @click="previewImage(idx)" />
            </el-carousel-item>
          </el-carousel>
          <div v-else class="no-image">
            <el-empty description="暂无景点图片" />
          </div>

          <div class="info-section card">
            <h2 class="section-label">景点简介</h2>
            <p class="desc-text">{{ spot.description || '暂无简介' }}</p>
          </div>

          <SpotSimilarSection
            :items="similarSpots"
            :update-time="similarUpdateTime"
            :format-similarity="formatSimilarity"
            @select="router.push(`/spots/${$event.spotId}?source=similar`)"
          />

          <div class="info-section card">
            <div class="section-header-row">
              <h2 class="section-label">最新评论</h2>
              <el-button v-if="hasMoreComments" text type="primary" @click="loadMoreComments">查看更多</el-button>
            </div>

            <div v-if="comments.length" class="comment-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <el-avatar :size="40" :src="getAvatarUrl(comment.avatar)" icon="User" />
                <div class="comment-body">
                  <div class="comment-top">
                    <span class="comment-name">{{ comment.nickname }}</span>
                    <div class="comment-actions">
                      <span class="star-text">★ {{ comment.score }}</span>
                      <el-button
                        v-if="canDeleteComment(comment)"
                        text
                        type="danger"
                        class="delete-comment-btn"
                        @click="handleDeleteComment(comment)"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                  <p class="comment-text">{{ comment.comment }}</p>
                  <span class="comment-time">{{ comment.createdAt }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无评论，快来抢沙发" :image-size="80" />
          </div>
        </div>

        <div ref="ratingSectionRef">
          <SpotDetailSidebar
            :spot="spot"
            :distance-text="distanceText"
            :rating-form="ratingForm"
            :submitting-rating="submittingRating"
            @buy="handleBuy"
            @toggle-favorite="toggleFavorite"
            @submit-rating="handleSubmitRating"
            @update:score="ratingForm.score = $event"
            @update:comment="ratingForm.comment = $event"
          />
        </div>
      </div>
    </div>

    <el-image-viewer
      v-if="previewVisible"
      :url-list="spotImages"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>

  <div v-else-if="loading" class="page-container">
    <el-skeleton :rows="10" animated />
  </div>

  <div v-else class="page-container invalid-state">
    <el-empty description="景点信息不存在或参数无效">
      <el-button type="primary" @click="router.push('/spots')">返回景点列表</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/modules/account/store/user.js'
import SpotDetailSidebar from '@/modules/spot/components/SpotDetailSidebar.vue'
import SpotSimilarSection from '@/modules/spot/components/SpotSimilarSection.vue'
import { getSpotDetail, getSimilarSpots, recordSpotView } from '@/modules/spot/api.js'
import { addFavorite, removeFavorite } from '@/modules/favorite/api.js'
import { deleteReview, getSpotReviews, submitReview } from '@/modules/review/api.js'
import { getLocationSnapshot } from '@/shared/lib/location.js'
import { saveSpotFootprint } from '@/shared/lib/footprint.js'
import { getAvatarUrl, getImageUrl } from '@/shared/api/client.js'

const SPOT_DETAIL_UPDATED_KEY = 'spot_detail_updated'

// 基础依赖与路由状态
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

let enterTime = 0
let viewSource = 'detail'

// 计算属性
const spotId = computed(() => {
  const id = Number(route.params.id)
  return Number.isInteger(id) && id > 0 ? id : null
})

const loading = ref(true)
const spot = ref(null)
const comments = ref([])
const similarSpots = ref([])
const similarUpdateTime = ref('')
const currentLocation = ref(null)
const commentPage = ref(1)
const commentTotal = ref(0)
const previewVisible = ref(false)
const previewIndex = ref(0)
const submittingRating = ref(false)
const ratingSectionRef = ref(null)

const ratingForm = reactive({
  score: 5,
  comment: ''
})

const hasMoreComments = computed(() => comments.value.length < commentTotal.value)

const spotImages = computed(() => {
  if (!spot.value) return []

  const images = []
  if (spot.value.coverImage) {
    images.push(getImageUrl(spot.value.coverImage))
  }

  if (spot.value.images) {
    const list = typeof spot.value.images === 'string' ? spot.value.images.split(',') : spot.value.images
    list.forEach((img) => {
      const raw = typeof img === 'string' ? img.trim() : ''
      if (!raw) return
      const url = getImageUrl(raw)
      if (url && !images.includes(url)) {
        images.push(url)
      }
    })
  }

  return images
})

const distanceText = computed(() => {
  if (!spot.value || !currentLocation.value) return ''

  const spotLatitude = Number(spot.value.latitude)
  const spotLongitude = Number(spot.value.longitude)
  const userLatitude = Number(currentLocation.value.latitude)
  const userLongitude = Number(currentLocation.value.longitude)
  if (![spotLatitude, spotLongitude, userLatitude, userLongitude].every(Number.isFinite)) {
    return ''
  }

  return formatDistance(calculateDistanceKm(userLatitude, userLongitude, spotLatitude, spotLongitude))
})

// 交互处理方法
const previewImage = (idx) => {
  previewIndex.value = idx
  previewVisible.value = true
}

const syncSpotPreview = (data) => {
  if (!data?.id) return

  localStorage.setItem(SPOT_DETAIL_UPDATED_KEY, JSON.stringify({
    id: data.id,
    name: data.name,
    coverImage: data.coverImage,
    intro: data.intro || data.description || '',
    regionName: data.regionName,
    categoryName: data.categoryName,
    price: data.price,
    avgRating: data.avgRating,
    isFavorite: data.isFavorite
  }))
}

// 数据加载方法
const fetchDetail = async () => {
  if (!spotId.value) {
    spot.value = null
    loading.value = false
    return
  }

  try {
    const res = await getSpotDetail(spotId.value)
    spot.value = res.data || null
    if (spot.value) {
      saveSpotFootprint(spot.value)
      syncSpotPreview(spot.value)
    }
  } catch (_error) {
    spot.value = null
    ElMessage.error('获取景点详情失败')
  } finally {
    loading.value = false
  }
}

const fetchComments = async (refresh = false) => {
  if (!spotId.value) {
    comments.value = []
    commentTotal.value = 0
    return
  }

  if (refresh) {
    commentPage.value = 1
    comments.value = []
  }

  const res = await getSpotReviews(spotId.value, commentPage.value, 5)
  const list = Array.isArray(res.data?.list) ? res.data.list : Array.isArray(res.data) ? res.data : []
  comments.value = refresh ? list : [...comments.value, ...list]
  commentTotal.value = res.data?.total || list.length
  commentPage.value += 1
}

const fetchSimilarList = async () => {
  if (!spotId.value) {
    similarSpots.value = []
    similarUpdateTime.value = ''
    return
  }

  try {
    const res = await getSimilarSpots(spotId.value, 6)
    similarSpots.value = res.data?.neighbors || []
    similarUpdateTime.value = res.data?.lastUpdateTime || ''
  } catch (_error) {
    similarSpots.value = []
    similarUpdateTime.value = ''
  }
}

const loadMoreComments = () => {
  if (hasMoreComments.value) {
    fetchComments()
  }
}

// 工具方法
const canDeleteComment = (comment) => userStore.isLoggedIn && comment.userId === userStore.userInfo?.id

const calculateDistanceKm = (fromLatitude, fromLongitude, toLatitude, toLongitude) => {
  const toRadians = (degree) => degree * Math.PI / 180
  const earthRadiusKm = 6371
  const deltaLatitude = toRadians(toLatitude - fromLatitude)
  const deltaLongitude = toRadians(toLongitude - fromLongitude)
  const a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
    + Math.cos(toRadians(fromLatitude)) * Math.cos(toRadians(toLatitude))
    * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2)
  return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return ''
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

const formatSimilarity = (value) => {
  if (typeof value !== 'number') return '0.00'
  return value.toFixed(2)
}

// 页面跳转方法
const handleBuy = () => {
  if (!spot.value?.id) {
    ElMessage.warning('当前景点信息无效')
    return
  }

  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  router.push(`/order/create/${spot.value.id}`)
}

const toggleFavorite = async () => {
  if (!spot.value?.id) {
    ElMessage.warning('当前景点信息无效')
    return
  }

  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  try {
    if (spot.value.isFavorite) {
      await removeFavorite(spot.value.id)
      spot.value.isFavorite = false
      syncSpotPreview(spot.value)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(spot.value.id)
      spot.value.isFavorite = true
      syncSpotPreview(spot.value)
      ElMessage.success('收藏成功')
    }
  } catch (_error) {
    ElMessage.error('收藏操作失败')
  }
}

const handleSubmitRating = async () => {
  if (!spotId.value) {
    ElMessage.warning('当前景点信息无效')
    return
  }

  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  if (!ratingForm.score) {
    ElMessage.warning('请选择评分')
    return
  }

  submittingRating.value = true
  try {
    await submitReview({
      spotId: spotId.value,
      score: ratingForm.score,
      comment: ratingForm.comment
    })
    ElMessage.success('评价成功')
    ratingForm.score = 5
    ratingForm.comment = ''
    await fetchComments(true)
    await fetchDetail()
  } catch (_error) {
    ElMessage.error('提交评价失败')
  } finally {
    submittingRating.value = false
  }
}

const handleDeleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确认删除这条评价吗？删除后评分会一并撤销。', '提示', {
      type: 'warning'
    })
    await deleteReview(comment.id)
    ElMessage.success('评价已删除')
    await fetchComments(true)
    await fetchDetail()
  } catch (_error) {
    // 用户取消时不需要额外提示。
  }
}

// 生命周期
onMounted(() => {
  enterTime = Date.now()
  viewSource = route.query.source || 'detail'

  if (!spotId.value) {
    loading.value = false
    ElMessage.warning('景点参数无效，无法加载详情')
    return
  }

  fetchDetail()
  fetchSimilarList()
  fetchComments(true)
  getLocationSnapshot().then((snapshot) => {
    currentLocation.value = snapshot.current
  })
  if (route.query.openReview === '1') {
    nextTick(() => {
      ratingSectionRef.value?.scrollIntoView?.({ behavior: 'smooth', block: 'center' })
    })
  }
})

onUnmounted(() => {
  if (spot.value?.id && userStore.isLoggedIn && enterTime > 0) {
    const duration = Math.floor((Date.now() - enterTime) / 1000)
    recordSpotView(spot.value.id, viewSource, duration).catch(() => {})
  }
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

.section-hint {
  color: #909399;
  font-size: 13px;
}

.desc-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

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
  align-items: center;
  margin-bottom: 4px;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.delete-comment-btn {
  padding: 0;
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

.invalid-state {
  padding: 48px 0;
}

@media (max-width: 992px) {
  .detail-layout {
    flex-direction: column;
  }
}
</style>
