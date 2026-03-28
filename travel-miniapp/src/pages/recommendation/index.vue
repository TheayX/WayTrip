<template>
  <view class="recommend-page">
    <view class="top-bar">
      <view>
        <text class="page-title">{{ recommendType }}</text>
        <text class="page-subtitle">集中浏览当前推荐结果</text>
      </view>
      <text v-if="isLoggedIn" class="refresh-btn" @click="refreshList">刷新推荐</text>
    </view>

    <view class="preference-tip" v-if="isLoggedIn && needPreference" @click="showPreferencePopup">
      <text class="tip-main">选择景点分类偏好，帮助冷启动推荐更准确</text>
      <text class="tip-arrow">›</text>
    </view>

    <view class="recommend-list" v-if="isLoggedIn && recommendations.length">
      <view class="recommend-card" v-for="spot in recommendations" :key="spot.id" @click="goSpotDetail(spot.id)">
        <image class="card-image" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
        <view class="card-content">
          <view class="card-header">
            <text class="card-title">{{ spot.name }}</text>
            <text class="card-rating">★ {{ spot.avgRating || '4.5' }}</text>
          </view>
          <text class="card-desc">{{ spot.intro || '暂无介绍，点击查看详情...' }}</text>
          <view class="card-footer">
            <text class="card-tag">{{ spot.categoryName || '景点' }}</text>
            <text class="card-price">¥{{ spot.price }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="empty-state" v-else>
      <text>{{ isLoggedIn ? '当前暂无推荐结果' : '登录后查看推荐结果' }}</text>
      <view v-if="!isLoggedIn" class="empty-actions">
        <text class="empty-link" @click="goLogin">去登录</text>
      </view>
    </view>

    <view class="preference-popup" v-if="preferenceVisible" @click.self="preferenceVisible = false">
      <view class="preference-content">
        <PreferenceCategorySelector
          v-model="selectedCategories"
          :categories="categories"
          title="选择你感兴趣的景点分类"
          subtitle="你可以随时修改，也可以清空偏好后回到热门冷启动。"
          primary-text="保存设置"
          secondary-text="取消"
          :allow-empty="true"
          @submit="savePreferences"
          @secondary="preferenceVisible = false"
          @limit-exceed="handleLimitExceed"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { promptLogin } from '@/utils/auth'
import PreferenceCategorySelector from '@/components/PreferenceCategorySelector.vue'
import { useRecommendationFeed } from '@/composables/useRecommendationFeed'
import { getContentImageUrl } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
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
  savePreferences: persistPreferences
} = useRecommendationFeed(20)

const refreshList = async () => {
  if (!promptLogin('登录后可刷新推荐，是否现在去登录？')) {
    return
  }

  uni.showLoading({ title: '加载中...' })
  try {
    await refreshRecommendationList()
    uni.showToast({ title: '已刷新', icon: 'none' })
  } catch (error) {
    console.error('刷新推荐失败', error)
    uni.showToast({ title: '刷新失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

const showPreferencePopup = async () => {
  if (!promptLogin('登录后可设置推荐偏好，是否现在去登录？')) {
    return
  }
  await openPreferenceDialog()
}

const goLogin = () => {
  if (!promptLogin('登录后可查看推荐列表，是否现在去登录？')) {
    return
  }
}

const handleLimitExceed = () => {
  uni.showToast({ title: '最多选择5个', icon: 'none' })
}

const savePreferences = async () => {
  try {
    await persistPreferences()
    uni.showToast({ title: '设置成功', icon: 'success' })
    await refreshList()
  } catch (error) {
    console.error('保存偏好失败', error)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const goSpotDetail = (spotId) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${spotId}&source=recommendation` })
}

onMounted(() => {
  selectedCategories.value = [...(userStore.userInfo?.preferenceCategoryIds || [])]
  fetchRecommendationList()
})

onShow(() => {
  const updatedSpot = uni.getStorageSync('spot_detail_updated')
  if (!updatedSpot?.id) return

  const index = recommendations.value.findIndex(item => item.id === updatedSpot.id)
  if (index !== -1) {
    recommendations.value[index] = {
      ...recommendations.value[index],
      ...updatedSpot
    }
  }

  uni.removeStorageSync('spot_detail_updated')
})
</script>

<style scoped>
.recommend-page {
  min-height: 100vh;
  padding: 32rpx;
  background: #f4f6fb;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.page-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #111827;
}

.page-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.refresh-btn {
  font-size: 26rpx;
  color: #2563eb;
}

.preference-tip {
  margin-bottom: 24rpx;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.05);
}

.tip-main {
  font-size: 26rpx;
  line-height: 1.5;
  color: #2563eb;
}

.tip-arrow {
  font-size: 32rpx;
  color: #9ca3af;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.recommend-card {
  background: #ffffff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.06);
}

.card-image {
  width: 100%;
  height: 280rpx;
}

.card-content {
  padding: 24rpx;
}

.card-header,
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.card-header {
  margin-bottom: 12rpx;
}

.card-title {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #111827;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.card-rating {
  font-size: 24rpx;
  color: #f59e0b;
}

.card-desc {
  display: -webkit-box;
  margin-bottom: 18rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #6b7280;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-tag {
  font-size: 22rpx;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
}

.card-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ef4444;
}

.empty-state {
  padding: 80rpx 0;
  text-align: center;
  font-size: 28rpx;
  color: #6b7280;
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 24rpx;
  margin-top: 24rpx;
}

.empty-link {
  padding: 14rpx 28rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #2563eb;
  font-size: 24rpx;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.05);
}

.preference-popup {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.preference-content {
  width: 620rpx;
  background: #ffffff;
  border-radius: 28rpx;
  padding: 40rpx;
}
</style>
