<!-- 评价管理页面 -->
<template>
  <div class="review-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">用户评价运营</p>
        <h1 class="page-title">评价管理</h1>
        <p class="page-subtitle">查看用户评价并处理异常内容。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchReviewList">刷新数据</el-button>
      </div>
    </section>

    <section class="insight-stat-row metric-cards--order">
      <el-card shadow="hover" class="summary-card">
        <div class="insight-stat-label">当前结果</div>
        <div class="insight-stat-value">{{ pagination.total }}</div>
        <div class="insight-stat-desc">符合当前筛选条件的评价数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="insight-stat-label">本页平均评分</div>
        <div class="insight-stat-value">{{ currentPageAverageScore }}</div>
        <div class="insight-stat-desc">用于快速判断当前页评价整体质量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="insight-stat-label">低分评价</div>
        <div class="insight-stat-value">{{ lowScoreCount }}</div>
        <div class="insight-stat-desc">本页评分小于等于 2 分的评价数量</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card admin-management-card">


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

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="评价管理加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchReviewList">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <!-- 评价列表 -->
      <el-table v-else :data="reviewList" v-loading="loading" class="review-table borderless-table">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" :size="36">{{ row.nickname?.[0] }}</el-avatar>
              <el-button link type="primary" class="nickname-link" @click="handleOpenUser(row)">
                {{ row.nickname }}
              </el-button>
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
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" align="center" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteReview, getReviewList } from '@/modules/user-ops/api/review.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'

const router = useRouter()

// 列表状态
const loading = ref(false)
const reviewList = ref([])
const errorMessage = ref('')

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
const currentPageAverageScore = computed(() => {
  if (!reviewList.value.length) return '0.0'
  const totalScore = reviewList.value.reduce((sum, item) => sum + Number(item.score || 0), 0)
  return (totalScore / reviewList.value.length).toFixed(1)
})
const lowScoreCount = computed(() => reviewList.value.filter((item) => Number(item.score || 0) <= 2).length)

// 获取评价列表
const fetchReviewList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getReviewList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    reviewList.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    reviewList.value = []
    pagination.total = 0
    errorMessage.value = error?.response?.data?.message || error?.message || '请稍后重试或检查接口返回。'
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

const handleOpenUser = (row) => {
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
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
    await ElMessageBox.confirm('确定要删除这条评价吗？删除后会同步更新景点评分。', '删除确认', {
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
@use '@/modules/user-ops/styles/user-ops.scss' as userOps;

.review-page {
  @include userOps.page-shell;
  display: flex;
  flex-direction: column;
  gap: 20px;


  .filter-caption {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 14px;
  }

  .filter-title {
    font-size: 13px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .filter-subtitle {
    font-size: 12px;
    line-height: 1.6;
    color: var(--wt-text-regular);
  }
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

.nickname-link {
  font-weight: 600;
}

.score-text {
  color: #f59e0b;
  font-weight: 700;
  font-size: 14px;
}

.review-table {
  border-radius: 18px;
  overflow: hidden;
}

:deep(.review-table .el-button.is-link) {
  padding: 0;
  margin: 0;
  min-width: 0;
  height: auto;
}

:deep(.review-table th.el-table__cell) {
  background: var(--wt-fill-hover);
  color: var(--wt-text-secondary);
  font-weight: 600;
}

:deep(.borderless-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.borderless-table td.el-table__cell),
:deep(.borderless-table th.el-table__cell.is-leaf) {
  border-bottom: 1px solid var(--wt-divider-faint);
}

:deep(.review-table .el-table__row:hover > td.el-table__cell) {
  background: var(--wt-row-gradient-hover) !important;
}

</style>

