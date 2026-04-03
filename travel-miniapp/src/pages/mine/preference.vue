<!-- 偏好设置页 -->
<template>
  <view class="ios-page">
    <view class="hero-card">
      <text class="hero-title">旅行偏好</text>
      <text class="hero-subtitle">选择你更常关注的景点类型，推荐结果会更贴近你的旅行口味。</text>
    </view>

    <view class="header">
      <PreferenceCategorySelector
        v-model="selectedIds"
        :categories="categories"
        title="选择你感兴趣的类型"
        subtitle="最多选择5个，也可以清空偏好，推荐会回到热门冷启动。"
        primary-text="保存设置"
        :allow-empty="true"
        :saving="saving"
        @submit="savePreferences"
        @limit-exceed="handleLimitExceed"
      />
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFilters } from '@/api/spot'
import { setPreferences, getUserInfo } from '@/api/user'
import { markColdStartGuideCompleted } from '@/utils/cold-start-guide'
import PreferenceCategorySelector from '@/components/PreferenceCategorySelector.vue'
import { useUserStore } from '@/stores/user'

// 基础依赖与用户状态
const userStore = useUserStore()

// 页面数据状态
const categories = ref([])
const selectedIds = ref([])
const saving = ref(false)

// 数据加载方法
const fetchCategories = async () => {
  try {
    const res = await getFilters()
    categories.value = res.data?.categories || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

const fetchUserPreferences = async () => {
  try {
    const res = await getUserInfo()
    selectedIds.value = res.data?.preferenceCategoryIds || []
  } catch (e) {
    console.error('获取用户偏好失败', e)
  }
}

// 交互处理方法
const handleLimitExceed = () => {
  uni.showToast({ title: '最多选择5个', icon: 'none' })
}

const savePreferences = async () => {
  saving.value = true
  try {
    await setPreferences({ categoryIds: selectedIds.value })
    const categoryNames = selectedIds.value
      .map(id => categories.value.find(cat => cat.id === id)?.name)
      .filter(Boolean)
    uni.showToast({ title: '保存成功', icon: 'success' })
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: [...selectedIds.value],
      preferenceCategoryNames: categoryNames
    })
    markColdStartGuideCompleted(userStore.userInfo?.id)
    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

// 生命周期
onMounted(() => {
  fetchCategories()
  fetchUserPreferences()
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.94), rgba(245, 247, 250, 0.9) 48%, rgba(238, 242, 247, 1) 100%),
    linear-gradient(180deg, #fafafa 0%, #eef2f7 100%);
  padding: 32rpx;
}

.hero-card {
  margin-bottom: 24rpx;
  padding: 28rpx;
  border-radius: 34rpx;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.58) 100%);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 600;
  color: #18181b;
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #52525b;
}

.header {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 40rpx;
  padding: 40rpx 32rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}
</style>
