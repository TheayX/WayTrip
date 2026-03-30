<!-- 我的互动页 -->
<template>
  <div class="page-container activity-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
      <el-breadcrumb-item>我的互动</el-breadcrumb-item>
    </el-breadcrumb>

    <section class="hero card">
      <div>
        <h2 class="page-title">我的互动</h2>
        <p class="page-subtitle">统一管理浏览、收藏和评价记录。</p>
      </div>
    </section>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="浏览" name="browse" />
      <el-tab-pane label="收藏" name="favorite" />
      <el-tab-pane label="评价" name="review" />
    </el-tabs>

    <section v-if="activeTab === 'browse'" class="list-section">
      <div v-if="footprints.length" class="card-list">
        <article v-for="item in footprints" :key="item.id" class="activity-card card" @click="$router.push(`/spots/${item.id}?source=footprint`)">
          <img :src="getImageUrl(item.coverImage)" class="cover" alt="" />
          <div class="content">
            <h3>{{ item.name }}</h3>
            <p>{{ item.regionName || '景点' }}</p>
            <span>浏览于 {{ formatViewedTime(item.viewedAt) }}</span>
          </div>
        </article>
      </div>
      <el-empty v-else description="你还没有浏览记录" />
    </section>

    <section v-if="activeTab === 'favorite'" class="list-section">
      <div v-if="favoriteList.length" class="card-list">
        <article v-for="spot in favoriteList" :key="spot.id" class="activity-card card" @click="$router.push(`/spots/${spot.id}?source=favorite`)">
          <img :src="getImageUrl(spot.coverImage)" class="cover" alt="" />
          <div class="content">
            <h3>{{ spot.name }}</h3>
            <p>{{ spot.regionName }} · {{ spot.categoryName }}</p>
            <div class="row">
              <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
              <span class="price">¥{{ spot.price }}</span>
            </div>
          </div>
          <el-button text type="danger" @click.stop="handleRemoveFavorite(spot.id)">取消收藏</el-button>
        </article>
      </div>
      <el-empty v-else description="暂无收藏" />
    </section>

    <section v-if="activeTab === 'review'" class="list-section">
      <div v-if="reviewList.length" class="card-list">
        <article v-for="item in reviewList" :key="item.id" class="activity-card card">
          <img :src="getImageUrl(item.coverImageUrl)" class="cover" alt="" />
          <div class="content">
            <div class="row">
              <h3>{{ item.spotName || `景点 #${item.spotId}` }}</h3>
              <span class="star-text">★ {{ item.score }}</span>
            </div>
            <p>{{ item.comment || '这条评价没有填写文字内容。' }}</p>
            <span>更新于 {{ item.updatedAt || item.createdAt || '-' }}</span>
            <div class="review-actions">
              <el-button plain size="small" @click="openEdit(item)">编辑</el-button>
              <el-button plain type="danger" size="small" @click="handleDeleteReview(item)">删除</el-button>
              <el-button text type="primary" size="small" @click="$router.push(`/spots/${item.spotId}?source=review`)">查看景点</el-button>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="你还没有发布过评价" />
    </section>

    <el-dialog v-model="editVisible" title="编辑评价" width="560px">
      <el-form :model="editForm" label-width="72px">
        <el-form-item label="景点">
          <span>{{ currentReview?.spotName || '-' }}</span>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="editForm.score" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="editForm.comment" type="textarea" :rows="4" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteReview, getMyReviews, submitReview } from '@/api/review'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
import { getViewHistory } from '@/api/spot'
import { getFootprints, setFootprints } from '@/utils/footprint'
import { getImageUrl } from '@/utils/request'

// 基础依赖与路由状态
const route = useRoute()
const router = useRouter()

// 页面数据状态
const activeTab = ref('browse')
const footprints = ref([])
const favoriteList = ref([])
const reviewList = ref([])
const editVisible = ref(false)
const saving = ref(false)
const currentReview = ref(null)
const editForm = reactive({
  score: 5,
  comment: ''
})

// 工具方法
const formatViewedTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return timestamp
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`
}

// 数据加载方法
const loadBrowseList = async () => {
  const cached = getFootprints()
  footprints.value = cached

  try {
    const res = await getViewHistory(1, 20)
    const list = res.data?.list || []
    if (list.length) {
      setFootprints(list)
      footprints.value = getFootprints()
    }
  } catch (_error) {
    // 浏览记录允许回退到本地缓存。
  }
}

const loadFavoriteList = async () => {
  const res = await getFavoriteList(1, 20)
  favoriteList.value = res.data?.list || []
}

const loadReviewList = async () => {
  const res = await getMyReviews(1, 20)
  reviewList.value = res.data?.list || []
}

const loadActiveTab = async () => {
  if (activeTab.value === 'browse') {
    await loadBrowseList()
    return
  }
  if (activeTab.value === 'favorite') {
    await loadFavoriteList()
    return
  }
  await loadReviewList()
}

// 交互处理方法
const handleTabChange = async () => {
  router.replace({
    path: '/profile/activity',
    query: activeTab.value === 'browse' ? {} : { tab: activeTab.value }
  })
  await loadActiveTab()
}

const handleRemoveFavorite = async (spotId) => {
  await removeFavorite(spotId)
  favoriteList.value = favoriteList.value.filter((item) => item.id !== spotId)
  ElMessage.success('已取消收藏')
}

const openEdit = (item) => {
  currentReview.value = item
  editForm.score = item.score || 5
  editForm.comment = item.comment || ''
  editVisible.value = true
}

const submitEdit = async () => {
  if (!currentReview.value) return

  saving.value = true
  try {
    await submitReview({
      spotId: currentReview.value.spotId,
      score: editForm.score,
      comment: editForm.comment
    })
    ElMessage.success('评价已更新')
    editVisible.value = false
    await loadReviewList()
  } finally {
    saving.value = false
  }
}

const handleDeleteReview = async (item) => {
  await ElMessageBox.confirm('确认删除这条评价吗？删除后评分也会一并撤销。', '提示', {
    type: 'warning'
  })
  await deleteReview(item.id)
  ElMessage.success('评价已删除')
  await loadReviewList()
}

// 生命周期
onMounted(() => {
  const tab = typeof route.query.tab === 'string' ? route.query.tab : 'browse'
  if (['browse', 'favorite', 'review'].includes(tab)) {
    activeTab.value = tab
  }
  loadActiveTab()
})
</script>

<style lang="scss" scoped>
.activity-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  padding: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #909399;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-card {
  padding: 16px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.cover {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.review-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
