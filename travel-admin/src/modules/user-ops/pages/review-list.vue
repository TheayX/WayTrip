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
      <el-form :model="searchForm" inline class="search-form admin-filter-bar" @submit.prevent>
        <div class="filter-row">
          <div class="filter-main">
            <el-form-item label="用户昵称" class="filter-item">
              <el-input
                v-model="searchForm.nickname"
                placeholder="请输入用户昵称"
                clearable
                class="form-w-168"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              />
            </el-form-item>
            <el-form-item label="景点名称" class="filter-item">
              <el-input
                v-model="searchForm.spotName"
                placeholder="请输入景点名称"
                clearable
                class="form-w-168"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              />
            </el-form-item>
            <el-form-item label="评分" class="filter-item">
              <el-select
                v-model="searchForm.scorePreset"
                placeholder="全部"
                clearable
                class="form-w-145"
                @change="handleSearch"
                @clear="handleSearch"
              >
                <el-option label="低分（1-2分）" value="low" />
                <el-option label="1 分" value="1" />
                <el-option label="2 分" value="2" />
                <el-option label="3 分" value="3" />
                <el-option label="4 分" value="4" />
                <el-option label="5 分" value="5" />
              </el-select>
            </el-form-item>
            <el-button type="primary" link class="toggle-btn" @click="showAdvanced = !showAdvanced">
              <el-icon><Filter v-if="!showAdvanced" /><CaretTop v-else /></el-icon>
              {{ showAdvanced ? '收起条件' : '更多条件' }}
            </el-button>
          </div>
          <div class="filter-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </div>

        <!-- 时间筛选属于复盘型条件，折叠后可优先保证首行检索效率。 -->
        <el-collapse-transition>
          <div v-show="showAdvanced" class="advanced-panel">
            <el-form-item label="评价时间" class="filter-item advanced-filter-item">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                class="tight-date-picker"
                @change="handleSearch"
              />
            </el-form-item>
          </div>
        </el-collapse-transition>
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
              <el-avatar :src="row.avatar" :size="36">{{ getDisplayNickname(row)?.[0] }}</el-avatar>
              <el-button
                link
                type="primary"
                class="nickname-link"
                :disabled="isDeactivatedUser(row)"
                @click="handleOpenUser(row)"
              >
                {{ getDisplayNickname(row) }}
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
              <el-button link type="primary" :disabled="isInvalidSpot(row)" @click="handleOpenSpot(row)">
                {{ getDisplaySpotName(row) }}
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CaretTop, Filter } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteReview, getReviewList } from '@/modules/user-ops/api/review.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'
import { isDeactivatedUserDisplay, isInvalidSpotDisplay, resolveSpotDisplayName, resolveUserDisplayName } from '@/shared/lib/resource-display.js'

const router = useRouter()
const route = useRoute()
const skipNextRouteLoad = ref(false)
const showAdvanced = ref(false)
const dateRange = ref([])

// 列表状态
const loading = ref(false)
const reviewList = ref([])
const errorMessage = ref('')

// 查询参数
const searchForm = reactive({
  nickname: '',
  spotName: '',
  scorePreset: ''
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
const getDisplayNickname = (row) => resolveUserDisplayName(row?.nickname)
const isDeactivatedUser = (row) => isDeactivatedUserDisplay(row?.nickname)
const getDisplaySpotName = (row) => resolveSpotDisplayName(row?.spotName)
const isInvalidSpot = (row) => isInvalidSpotDisplay(row?.spotName)

const resolveScoreRange = (scorePreset) => {
  if (!scorePreset) {
    return { minScore: undefined, maxScore: undefined }
  }
  if (scorePreset === 'low') {
    return { minScore: 1, maxScore: 2 }
  }
  const exactScore = Number(scorePreset)
  return Number.isNaN(exactScore)
    ? { minScore: undefined, maxScore: undefined }
    : { minScore: exactScore, maxScore: exactScore }
}

// 获取评价列表
const fetchReviewList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { minScore, maxScore } = resolveScoreRange(searchForm.scorePreset)
    const res = await getReviewList({
      nickname: searchForm.nickname,
      spotName: searchForm.spotName,
      minScore,
      maxScore,
      page: pagination.page,
      pageSize: pagination.pageSize,
      startDate: dateRange.value?.length === 2 ? dateRange.value[0] : undefined,
      endDate: dateRange.value?.length === 2 ? dateRange.value[1] : undefined
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
  syncRouteQuery()
  fetchReviewList()
}

const handleReset = () => {
  searchForm.nickname = ''
  searchForm.spotName = ''
  searchForm.scorePreset = ''
  dateRange.value = []
  handleSearch()
}

const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.nickname) nextQuery.nickname = searchForm.nickname
  if (searchForm.spotName) nextQuery.spotName = searchForm.spotName
  if (searchForm.scorePreset) nextQuery.scorePreset = searchForm.scorePreset
  if (dateRange.value?.length === 2) {
    nextQuery.startDate = dateRange.value[0]
    nextQuery.endDate = dateRange.value[1]
  }
  const currentQuery = {}
  if (typeof route.query.nickname === 'string' && route.query.nickname) currentQuery.nickname = route.query.nickname
  if (typeof route.query.spotName === 'string' && route.query.spotName) currentQuery.spotName = route.query.spotName
  if (typeof route.query.scorePreset === 'string' && route.query.scorePreset) currentQuery.scorePreset = route.query.scorePreset
  if (typeof route.query.startDate === 'string' && route.query.startDate) currentQuery.startDate = route.query.startDate
  if (typeof route.query.endDate === 'string' && route.query.endDate) currentQuery.endDate = route.query.endDate
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
}

const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
  searchForm.spotName = typeof route.query.spotName === 'string' ? route.query.spotName : ''
  searchForm.scorePreset = typeof route.query.scorePreset === 'string' ? route.query.scorePreset : ''
  if (typeof route.query.startDate === 'string' && typeof route.query.endDate === 'string') {
    dateRange.value = [route.query.startDate, route.query.endDate]
    showAdvanced.value = true
  } else {
    dateRange.value = []
  }
}

const handleOpenUser = (row) => {
  if (isDeactivatedUser(row)) return
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
}

// 跳转景点页，并复用景点管理页的自动定位与详情打开能力。
const handleOpenSpot = (row) => {
  if (isInvalidSpot(row)) return
  router.push({
    path: '/spot',
    query: {
      keyword: getDisplaySpotName(row),
      spotId: row.spotId || ''
    }
  })
}

// 删除评价
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评价记录吗？删除后会同步更新景点评分。', '删除确认', {
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
  applyRouteQuery()
  fetchReviewList()
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    if (skipNextRouteLoad.value) {
      skipNextRouteLoad.value = false
      return
    }
    fetchReviewList()
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
@use '@/modules/user-ops/styles/user-ops.scss' as userOps;

.review-page {
  @include userOps.page-shell;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.nickname-link {
  font-weight: 600;
}

.score-text {
  color: var(--wt-accent-amber-text);
  font-weight: 700;
  font-size: 14px;
}

.advanced-filter-item {
  margin-bottom: 0;
}

:deep(.tight-date-picker.el-date-editor) {
  width: 240px !important;
}

</style>

