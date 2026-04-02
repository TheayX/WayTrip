<!-- 评价管理页面 -->
<template>
  <div class="review-page">
    <el-card  shadow="hover">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>评价管理</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" inline class="search-form" @submit.prevent>
        <el-form-item label="用户昵称">
          <el-input
            v-model="searchForm.nickname"
            placeholder="请输入用户昵称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="景点名称">
          <el-input
            v-model="searchForm.spotName"
            placeholder="请输入景点名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 评价列表 -->
      <el-table :data="reviewList" v-loading="loading" stripe>
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" :size="36">{{ row.nickname?.[0] }}</el-avatar>
              <span>{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="景点" min-width="220">
          <template #default="{ row }">
            <div class="spot-cell">
              <el-image
                v-if="row.coverImageUrl"
                :src="row.coverImageUrl"
                fit="cover"
                class="spot-cover"
                preview-disabled
              />
              <el-button link type="primary" @click="handleOpenSpot(row)">
                {{ row.spotName || `景点 #${row.spotId}` }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="评分" width="100">
          <template #default="{ row }">
            <span class="score-text">★ {{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="评价内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">违规删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchReviewList"
          @current-change="fetchReviewList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteReview, getReviewList } from '@/modules/user-ops/api/review.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'

const router = useRouter()

// 列表状态
const loading = ref(false)
const reviewList = ref([])

// 查询参数
const searchForm = reactive({
  nickname: '',
  spotName: ''
})

// 分页参数
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 获取评价列表
const fetchReviewList = async () => {
  loading.value = true
  try {
    const res = await getReviewList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    reviewList.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchReviewList()
}

const handleReset = () => {
  searchForm.nickname = ''
  searchForm.spotName = ''
  handleSearch()
}

// 跳转景点页，并复用景点管理页的自动定位与详情打开能力。
const handleOpenSpot = (row) => {
  router.push({
    path: '/spot',
    query: {
      keyword: row.spotName || '',
      spotId: row.spotId || ''
    }
  })
}

// 删除评价
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评价吗？删除后会同步更新景点评分。', '提示', {
      type: 'warning'
    })
    await deleteReview(row.id)
    ElMessage.success('删除成功')
    fetchReviewList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('删除失败')
    }
  }
}

// 页面初始化
onMounted(() => {
  fetchReviewList()
})
</script>

<style lang="scss" scoped>
.review-page {
  /* Global design system handles search-form and pagination-wrapper */
}

.user-cell,
.spot-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spot-cover {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}

.score-text {
  color: #f59e0b;
  font-weight: 700;
  font-size: 14px;
}
</style>
