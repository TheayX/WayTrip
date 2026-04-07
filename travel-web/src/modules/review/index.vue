<!-- 我的评价页 -->
<template>
  <div class="page-container">
    <AccountPageHeader title="我的评价" subtitle="查看、修改或删除你提交过的全部景点评价。" />

    <section class="review-page card">
      <div v-loading="loading">
        <div v-if="reviewList.length" class="review-list">
          <article v-for="item in reviewList" :key="item.id" class="review-card">
            <img :src="getImageUrl(item.coverImageUrl)" class="spot-cover" alt="" />
            <div class="review-main">
              <div class="review-top">
                <div>
                  <h3 class="spot-name">{{ item.spotName || `景点 #${item.spotId}` }}</h3>
                  <div class="review-meta">
                    <span class="score">★ {{ item.score }}</span>
                    <span>创建于 {{ item.createdAt || '-' }}</span>
                    <span>更新于 {{ item.updatedAt || item.createdAt || '-' }}</span>
                  </div>
                </div>
                <div class="actions">
                  <button type="button" class="review-action-button" @click="openEdit(item)">编辑</button>
                  <button type="button" class="review-action-button review-action-button--danger" @click="handleDelete(item)">删除</button>
                </div>
              </div>
              <p class="comment">{{ item.comment || '这条评价没有填写文字内容。' }}</p>
              <button type="button" class="review-action-button" @click="$router.push(buildSpotDetailRoute(item.spotId, SPOT_DETAIL_SOURCE.REVIEW))">查看景点</button>
            </div>
          </article>
        </div>
        <el-empty v-else description="你还没有发布过评价" :image-size="100" />
      </div>

      <div class="pagination-wrapper" v-if="pagination.total > pagination.pageSize">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[6, 12, 24]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchReviewList"
          @current-change="fetchReviewList"
        />
      </div>
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
          <el-input
            v-model="editForm.comment"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="分享你的体验"
          />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPageHeader from '@/modules/account/components/AccountPageHeader.vue'
import { deleteReview, getMyReviews, submitReview } from '@/modules/review/api.js'
import { getImageUrl } from '@/shared/api/client.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'

// 页面数据状态
const loading = ref(false)
const saving = ref(false)
const reviewList = ref([])
const editVisible = ref(false)
const currentReview = ref(null)

const pagination = reactive({
  page: 1,
  pageSize: 6,
  total: 0
})

const editForm = reactive({
  score: 5,
  comment: ''
})

// 数据加载方法
const fetchReviewList = async () => {
  loading.value = true
  try {
    const res = await getMyReviews(pagination.page, pagination.pageSize)
    reviewList.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 交互处理方法
const openEdit = (item) => {
  currentReview.value = item
  editForm.score = item.score || 5
  editForm.comment = item.comment || ''
  editVisible.value = true
}

const submitEdit = async () => {
  if (!currentReview.value) return
  if (!editForm.score) {
    ElMessage.warning('请先选择评分')
    return
  }
  saving.value = true
  try {
    await submitReview({
      spotId: currentReview.value.spotId,
      score: editForm.score,
      comment: editForm.comment
    })
    ElMessage.success('评价已更新')
    editVisible.value = false
    await fetchReviewList()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确认删除这条评价吗？删除后评分也会一并撤销。', '提示', {
      type: 'warning'
    })
    await deleteReview(item.id)
    ElMessage.success('评价已删除')
    if (reviewList.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1
    }
    await fetchReviewList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

// 生命周期
onMounted(() => {
  fetchReviewList()
})
</script>

<style lang="scss" scoped>
.review-page {
  margin-top: 8px;
  padding: 28px;
  border-radius: 18px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  display: flex;
  gap: 18px;
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffffff 0%, #faf8f3 100%);
}

.spot-cover {
  width: 128px;
  height: 128px;
  object-fit: cover;
  border-radius: 14px;
  flex-shrink: 0;
}

.review-main {
  flex: 1;
  min-width: 0;
}

.review-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.spot-name {
  margin: 0 0 8px;
  font-size: 20px;
}

.review-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #909399;
  font-size: 13px;
}

.score {
  color: #e6a23c;
  font-weight: 700;
}

.comment {
  margin: 18px 0 14px;
  line-height: 1.75;
  color: #303133;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.review-action-button {
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.92);
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.review-action-button:hover {
  background: #e2e8f0;
  color: #0f172a;
  transform: translateY(-1px);
}

.review-action-button--danger {
  background: #fef2f2;
  color: #b91c1c;
}

.review-action-button--danger:hover {
  background: #fee2e2;
  color: #991b1b;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .review-page {
    padding: 18px;
  }

  .review-card,
  .review-top {
    flex-direction: column;
  }

  .spot-cover {
    width: 100%;
    height: 180px;
  }

  .actions {
    justify-content: flex-end;
  }
}
</style>
