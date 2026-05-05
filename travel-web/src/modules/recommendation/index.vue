<!-- 推荐景点页 -->
<template>
  <div class="page-container recommendation-page">
    <section class="hero premium-card">
      <div>
        <p class="hero-eyebrow">Recommendation Detail</p>
        <h2 class="page-title">{{ userStore.isLoggedIn ? recommendType : '推荐景点' }}</h2>
        <p class="page-subtitle">这个页面保留为推荐结果直达页，更完整的探索与筛选已经合并到发现页。</p>
      </div>
      <div class="hero-actions">
        <el-button text @click="$router.push({ path: APP_ROUTE_PATHS.discover, query: { scene: 'recommend' } })">返回发现页</el-button>
        <el-button v-if="userStore.isLoggedIn" :loading="refreshing" type="primary" @click="handleRefresh">刷新推荐</el-button>
        <el-button v-if="userStore.isLoggedIn" @click="showPreferencePopup">偏好设置</el-button>
      </div>
    </section>

    <section v-if="userStore.isLoggedIn && needPreference" class="preference-tip premium-card" @click="showPreferencePopup">
      <div>
        <strong>你还没有设置偏好分类</strong>
        <p>先选几类感兴趣的景点，推荐结果会更稳定。</p>
      </div>
      <el-icon><ArrowRight /></el-icon>
    </section>

    <section v-if="userStore.isLoggedIn && recommendations.length" class="recommend-grid">
      <SpotCard
        v-for="spot in recommendations"
        :key="spot.id"
        :spot="spot"
        @select="$router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.RECOMMENDATION))"
      />
    </section>

    <el-empty v-else :description="userStore.isLoggedIn ? '当前暂无推荐景点' : '登录后查看推荐景点'">
      <el-button v-if="!userStore.isLoggedIn" type="primary" @click="$router.push(AUTH_ROUTE_PATHS.login)">去登录</el-button>
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
import { ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/modules/account/store/user.js'
import { useRecommendationFeed } from '@/modules/recommendation/composables/useRecommendationFeed.js'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'
import { APP_ROUTE_PATHS, AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'

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
  rotateRecommendationList,
  openPreferenceDialog,
  savePreferences
} = useRecommendationFeed(20)

// 交互处理方法
const handleRefresh = async () => {
  refreshing.value = true
  try {
    await rotateRecommendationList()
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
  padding-top: 4px;
}

.hero {
  padding: 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.14), transparent 28%),
    linear-gradient(135deg, #f8fbff 0%, #ffffff 60%, #eef5ff 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 700;
}

.page-title {
  font-size: 34px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.page-subtitle {
  margin-top: 12px;
  color: #64748b;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.preference-tip {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  cursor: pointer;
}

.preference-tip strong {
  color: #0f172a;
}

.preference-tip p {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.75;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 992px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions,
  .recommend-grid {
    width: 100%;
    grid-template-columns: 1fr;
  }
}
</style>
