<template>
  <view class="review-page">
    <view v-if="reviewList.length" class="review-list">
      <view v-for="item in reviewList" :key="item.id" class="review-card">
        <image class="spot-cover" :src="getContentImageUrl(item.coverImageUrl)" mode="aspectFill" />
        <view class="review-body">
          <view class="review-top">
            <text class="spot-name">{{ item.spotName || `景点 #${item.spotId}` }}</text>
            <text class="score">★ {{ item.score }}</text>
          </view>
          <text class="review-time">更新于 {{ item.updatedAt || item.createdAt || '-' }}</text>
          <text class="comment">{{ item.comment || '这条评价没有填写文字内容。' }}</text>
          <view class="actions">
            <button class="ghost-btn" @tap="openEdit(item)">编辑</button>
            <button class="danger-btn" @tap="handleDelete(item)">删除</button>
            <button class="link-btn" @tap="goSpot(item.spotId)">查看景点</button>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty-wrap">
      <image class="empty-img" src="/static/empty-image.png" mode="aspectFit" />
      <text class="empty-text">你还没有发布过评价</text>
    </view>

    <view v-if="pagination.total > reviewList.length" class="load-more">
      <button class="more-btn" @tap="fetchReviewList" :disabled="loadingMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </view>

    <view v-if="editVisible" class="popup-mask" @tap="closeEdit">
      <view class="popup-panel" @tap.stop>
        <text class="popup-title">编辑评价</text>
        <text class="popup-spot">{{ currentReview?.spotName || '-' }}</text>
        <view class="star-row">
          <text
            v-for="i in 5"
            :key="i"
            class="star"
            :class="{ active: i <= editForm.score }"
            @tap="editForm.score = i"
          >★</text>
        </view>
        <textarea
          v-model="editForm.comment"
          class="comment-input"
          maxlength="300"
          placeholder="分享你的体验"
        />
        <view class="popup-actions">
          <button class="ghost-btn" @tap="closeEdit">取消</button>
          <button class="primary-btn" @tap="submitEdit">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { deleteReview, getMyReviews, submitReview } from '@/api/review'
import { getContentImageUrl } from '@/utils/request'

const reviewList = ref([])
const loadingMore = ref(false)
const editVisible = ref(false)
const currentReview = ref(null)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const editForm = reactive({
  score: 5,
  comment: ''
})

const fetchReviewList = async (reset = false) => {
  if (loadingMore.value) return
  loadingMore.value = true
  try {
    if (reset) {
      pagination.page = 1
      reviewList.value = []
    }
    const res = await getMyReviews(pagination.page, pagination.pageSize)
    const list = res.data?.list || []
    reviewList.value = reset ? list : [...reviewList.value, ...list]
    pagination.total = res.data?.total || 0
    pagination.page += 1
  } finally {
    loadingMore.value = false
  }
}

const openEdit = (item) => {
  currentReview.value = item
  editForm.score = item.score || 5
  editForm.comment = item.comment || ''
  editVisible.value = true
}

const closeEdit = () => {
  editVisible.value = false
}

const submitEdit = async () => {
  if (!currentReview.value) return
  if (editForm.score < 1) {
    uni.showToast({ title: '请先选择评分', icon: 'none' })
    return
  }
  try {
    await submitReview({
      spotId: currentReview.value.spotId,
      score: editForm.score,
      comment: editForm.comment
    })
    uni.showToast({ title: '评价已更新', icon: 'success' })
    editVisible.value = false
    fetchReviewList(true)
  } catch (e) {
    uni.showToast({ title: '更新失败', icon: 'none' })
  }
}

const handleDelete = (item) => {
  uni.showModal({
    title: '删除评价',
    content: '确认删除这条评价吗？删除后评分会一并撤销。',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteReview(item.id)
        uni.showToast({ title: '评价已删除', icon: 'success' })
        fetchReviewList(true)
      } catch (e) {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    }
  })
}

const goSpot = (spotId) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${spotId}&source=review` })
}

onShow(() => {
  fetchReviewList(true)
})
</script>

<style scoped>
.review-page {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, #f5f7fa 0%, #eef3f8 100%);
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.review-card {
  display: flex;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 28rpx rgba(31, 45, 61, 0.06);
}

.spot-cover {
  width: 180rpx;
  height: 180rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}

.review-body {
  flex: 1;
  min-width: 0;
}

.review-top {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  align-items: flex-start;
}

.spot-name {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2937;
}

.score {
  font-size: 30rpx;
  color: #d97706;
  font-weight: 700;
}

.review-time {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.comment {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: #334155;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}

.ghost-btn,
.danger-btn,
.link-btn,
.primary-btn,
.more-btn {
  height: 68rpx;
  line-height: 68rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  padding: 0 28rpx;
}

.ghost-btn {
  color: #2563eb;
  background: #eff6ff;
}

.danger-btn {
  color: #dc2626;
  background: #fef2f2;
}

.link-btn {
  color: #0f766e;
  background: #ecfeff;
}

.primary-btn {
  color: #fff;
  background: #2563eb;
}

.load-more {
  margin-top: 24rpx;
}

.more-btn {
  color: #475569;
  background: #fff;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.empty-img {
  width: 220rpx;
  height: 220rpx;
  opacity: 0.8;
}

.empty-text {
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #94a3b8;
}

.popup-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.38);
  display: flex;
  align-items: flex-end;
}

.popup-panel {
  width: 100%;
  padding: 32rpx;
  border-radius: 32rpx 32rpx 0 0;
  background: #fff;
}

.popup-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #0f172a;
}

.popup-spot {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #64748b;
}

.star-row {
  display: flex;
  gap: 12rpx;
  margin: 28rpx 0 24rpx;
}

.star {
  font-size: 48rpx;
  color: #cbd5e1;
}

.star.active {
  color: #f59e0b;
}

.comment-input {
  width: 100%;
  height: 220rpx;
  padding: 20rpx;
  border-radius: 20rpx;
  background: #f8fafc;
  font-size: 28rpx;
  box-sizing: border-box;
}

.popup-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}
</style>
