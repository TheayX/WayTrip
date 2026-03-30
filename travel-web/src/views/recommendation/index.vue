<!-- 推荐景点页 -->
<template>
  <div class="page-container recommendation-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>推荐景点</el-breadcrumb-item>
    </el-breadcrumb>

    <section class="hero card">
      <div>
        <h2 class="page-title">{{ userStore.isLoggedIn ? recommendType : '推荐景点' }}</h2>
        <p class="page-subtitle">集中浏览当前推荐结果，并可随时调整偏好。</p>
      </div>
      <div class="hero-actions">
        <el-button v-if="userStore.isLoggedIn" :loading="refreshing" type="primary" @click="handleRefresh">刷新推荐</el-button>
        <el-button v-if="userStore.isLoggedIn" @click="showPreferencePopup">偏好设置</el-button>
      </div>
    </section>

    <section v-if="userStore.isLoggedIn && needPreference" class="preference-tip card" @click="showPreferencePopup">
      <span>你还没有设置偏好分类，先选几类感兴趣的景点，推荐会更稳定。</span>
      <el-icon><ArrowRight /></el-icon>
    </section>

    <section v-if="userStore.isLoggedIn && recommendations.length" class="recommend-grid">
      <article
        v-for="spot in recommendations"
        :key="spot.id"
        class="recommend-card card"
        @click="$router.push(`/spots/${spot.id}?source=recommendation`)"
      >
        <img :src="getImageUrl(spot.coverImage)" class="recommend-image" alt="" />
        <div class="recommend-content">
          <div class="recommend-top">
            <h3 class="recommend-name">{{ spot.name }}</h3>
            <span class="star-text">★ {{ spot.avgRating || '4.5' }}</span>
          </div>
          <p class="recommend-desc">{{ spot.intro || '暂无介绍，点击查看详情。' }}</p>
          <div class="recommend-bottom">
            <span class="tag">{{ spot.categoryName || '景点' }}</span>
            <span class="price">¥{{ spot.price }}</span>
          </div>
        </div>
      </article>
    </section>

    <el-empty v-else :description="userStore.isLoggedIn ? '当前暂无推荐景点' : '登录后查看推荐景点'">
      <el-button v-if="!userStore.isLoggedIn" type="primary" @click="$router.push('/login')">去登录</el-button>
    </el-empty>

    <el-dialog v-model="preferenceVisible" title="选择你感兴趣的景点分类" width="520px" :close-on-click-modal="false">
      <div class="preference-tags">
        <el-check-tag
          v-for="category in categories"
          :key="category.id"
          :checked="selectedCategories.includes(category.id)"
          @change="toggleCategory(category.id)"
        >
          {{ category.name }}
        </el-check-tag>
      </div>
      <template #footer>
        <el-button @click="preferenceVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSavePreference">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useRecommendationFeed } from '@/composables/useRecommendationFeed'
import { getImageUrl } from '@/utils/request'

// 基础依赖与用户状态
const userStore = useUserStore()
const refreshing = ref(false)
const saving = ref(false)
const {
  recommendations,
  needPreference,
  categories,
  selectedCategories,
  preferenceVisible,
  recommendType,
  fetchRecommendationList,
  refreshRecommendationList,
  openPreferenceDialog,
  savePreferences
} = useRecommendationFeed(20)

// 交互处理方法
const handleRefresh = async () => {
  refreshing.value = true
  try {
    await refreshRecommendationList()
    ElMessage.success('推荐已刷新')
  } finally {
    refreshing.value = false
  }
}

const showPreferencePopup = async () => {
  await openPreferenceDialog()
}

const toggleCategory = (id) => {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
  } else {
    selectedCategories.value.push(id)
  }
}

const handleSavePreference = async () => {
  saving.value = true
  try {
    await savePreferences()
    ElMessage.success(selectedCategories.value.length ? '偏好设置成功' : '已清空偏好')
    await handleRefresh()
  } finally {
    saving.value = false
  }
}

// 生命周期
onMounted(() => {
  fetchRecommendationList()
})
</script>

<style lang="scss" scoped>
.recommendation-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero {
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #909399;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.preference-tip {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #409eff;
  cursor: pointer;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.recommend-card {
  display: flex;
  cursor: pointer;
}

.recommend-image {
  width: 220px;
  height: 170px;
  object-fit: cover;
  flex-shrink: 0;
}

.recommend-content {
  flex: 1;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.recommend-top,
.recommend-bottom {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.recommend-name {
  font-size: 18px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-desc {
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 900px) {
  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .recommend-card {
    flex-direction: column;
  }

  .recommend-image {
    width: 100%;
    height: 220px;
  }
}
</style>
