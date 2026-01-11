<template>
  <view class="ios-page">
    <view class="header">
      <text class="title">选择你感兴趣的类型</text>
      <text class="subtitle">最多选择5个，帮助我们为你推荐更精准的内容</text>
    </view>

    <view class="category-list">
      <view 
        v-for="cat in categories" 
        :key="cat.id"
        class="category-item"
        :class="{ active: selectedIds.includes(cat.id) }"
        @click="toggleCategory(cat.id)"
      >
        <text class="category-name">{{ cat.name }}</text>
        <view class="check-icon" v-if="selectedIds.includes(cat.id)">✓</view>
      </view>
    </view>

    <view class="bottom-bar">
      <text class="selected-count">已选择 {{ selectedIds.length }}/5</text>
      <button class="save-btn" @click="savePreferences" :disabled="saving">
        {{ saving ? '保存中...' : '保存设置' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFilters } from '@/api/spot'
import { updatePreferences, getUserInfo } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const categories = ref([])
const selectedIds = ref([])
const saving = ref(false)

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
    const preferences = res.data?.preferences
    if (preferences) {
      selectedIds.value = preferences.split(',').map(Number).filter(Boolean)
    }
  } catch (e) {
    console.error('获取用户偏好失败', e)
  }
}

const toggleCategory = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    if (selectedIds.value.length < 5) {
      selectedIds.value.push(id)
    } else {
      uni.showToast({ title: '最多选择5个', icon: 'none' })
    }
  }
}

const savePreferences = async () => {
  if (selectedIds.value.length === 0) {
    uni.showToast({ title: '请至少选择一个', icon: 'none' })
    return
  }

  saving.value = true
  try {
    await updatePreferences({ categoryIds: selectedIds.value })
    uni.showToast({ title: '保存成功', icon: 'success' })
    userStore.updatePreferences(selectedIds.value.join(','))
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchUserPreferences()
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #F2F2F7;
  padding-bottom: 160rpx;
}

.header {
  padding: 40rpx 32rpx;
  background: #fff;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  color: #1C1C1E;
  display: block;
  margin-bottom: 12rpx;
}

.subtitle {
  font-size: 26rpx;
  color: #8E8E93;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  padding: 24rpx 32rpx;
  gap: 20rpx;
}

.category-item {
  width: calc(50% - 10rpx);
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid transparent;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}

.category-item.active {
  border-color: #007AFF;
  background: rgba(0, 122, 255, 0.05);
}

.category-name {
  font-size: 30rpx;
  color: #1C1C1E;
  font-weight: 500;
}

.category-item.active .category-name {
  color: #007AFF;
}

.check-icon {
  width: 40rpx;
  height: 40rpx;
  background: #007AFF;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 -1rpx 0 rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.selected-count {
  font-size: 28rpx;
  color: #8E8E93;
}

.save-btn {
  width: 240rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: #007AFF;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
  border: none;
}

.save-btn[disabled] {
  background: #C7C7CC;
}
</style>
