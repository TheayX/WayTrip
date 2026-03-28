<template>
  <view class="ios-page">
    <view class="header">
      <PreferenceCategorySelector
        v-model="selectedIds"
        :categories="categories"
        title="选择你感兴趣的类型"
        subtitle="最多选择5个，也可以清空偏好，推荐会回退到热门景点。"
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
import { updatePreferences, getUserInfo } from '@/api/auth'
import { markColdStartGuideCompleted } from '@/utils/cold-start-guide'
import PreferenceCategorySelector from '@/components/PreferenceCategorySelector.vue'
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
    selectedIds.value = res.data?.preferenceCategoryIds || []
  } catch (e) {
    console.error('获取用户偏好失败', e)
  }
}

const handleLimitExceed = () => {
  uni.showToast({ title: '最多选择5个', icon: 'none' })
}

const savePreferences = async () => {
  saving.value = true
  try {
    await updatePreferences({ categoryIds: selectedIds.value })
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

onMounted(() => {
  fetchCategories()
  fetchUserPreferences()
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #F2F2F7;
  padding: 32rpx;
}

.header {
  background: #fff;
  border-radius: 28rpx;
  padding: 40rpx 32rpx;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.05);
}
</style>
