<template>
  <view class="preference-page">
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
        <text class="category-icon">{{ cat.icon || '🏷️' }}</text>
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

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await getFilters()
    categories.value = res.data?.categories || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

// 获取用户当前偏好
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

// 切换选择
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

// 保存偏好
const savePreferences = async () => {
  if (selectedIds.value.length === 0) {
    uni.showToast({ title: '请至少选择一个', icon: 'none' })
    return
  }

  saving.value = true
  try {
    await updatePreferences({ categoryIds: selectedIds.value })
    uni.showToast({ title: '保存成功', icon: 'success' })
    
    // 更新本地用户信息
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
.preference-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.header {
  padding: 40rpx;
  background: #fff;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.subtitle {
  font-size: 26rpx;
  color: #999;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx;
  gap: 20rpx;
}

.category-item {
  width: calc(50% - 10rpx);
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  display: flex;
  align-items: center;
  position: relative;
  border: 2rpx solid transparent;
  transition: all 0.3s;
}

.category-item.active {
  border-color: #409EFF;
  background: #ecf5ff;
}

.category-icon {
  font-size: 48rpx;
  margin-right: 16rpx;
}

.category-name {
  font-size: 28rpx;
  color: #333;
  flex: 1;
}

.check-icon {
  width: 40rpx;
  height: 40rpx;
  background: #409EFF;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.selected-count {
  font-size: 28rpx;
  color: #666;
}

.save-btn {
  width: 240rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #409EFF;
  color: #fff;
  font-size: 30rpx;
  border-radius: 40rpx;
}

.save-btn[disabled] {
  background: #ccc;
}
</style>
