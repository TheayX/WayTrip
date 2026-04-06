<template>
  <div class="view-log-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">浏览行为运营</p>
        <h1 class="page-title">浏览行为</h1>
        <p class="page-subtitle">查看浏览来源、停留时长与入口表现。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchViewList">刷新数据</el-button>
      </div>
    </section>

    <section class="insight-stat-row metric-cards--order">
      <el-card shadow="hover" class="summary-card">
        <div class="insight-stat-label">筛选结果</div>
        <div class="insight-stat-value">{{ pagination.total }}</div>
        <div class="insight-stat-desc">当前条件下的浏览记录总数</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="insight-stat-label">当前页平均停留</div>
        <div class="insight-stat-value">{{ averageDuration }} 秒</div>
        <div class="insight-stat-desc">用于快速判断当前筛选结果的浏览深度</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="insight-stat-label">当前页主要来源</div>
        <div class="insight-stat-value">{{ topSourceLabel }}</div>
        <div class="insight-stat-desc">帮助判断推荐、搜索、攻略等入口的贡献</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card admin-management-card">


      <el-alert
        class="source-alert"
        type="info"
        :closable="false"
        show-icon
        title="说明：这里展示的是数据库原始来源值；推荐算法计算时会再归并到首页、搜索、攻略、推荐、默认这几个来源挡位。"
      />

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
        <el-form-item label="来源">
          <el-select
            v-model="searchForm.source"
            placeholder="全部来源"
            clearable
            style="width: 140px"
            @change="handleSearch"
            @clear="handleSearch"
          >
            <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="浏览时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="浏览行为加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchViewList">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <el-table v-else :data="tableData" v-loading="loading" class="ops-table borderless-table">
        <el-table-column prop="id" label="记录ID" width="90" />
        <el-table-column label="用户昵称" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenUser(row)">{{ row.nickname }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="景点" min-width="240">
          <template #default="{ row }">
            <div class="spot-cell">
              <el-image v-if="row.coverImage" :src="getResourceUrl(row.coverImage)" fit="cover" class="spot-cover" />
              <el-button link type="primary" @click="handleOpenSpot(row)">{{ row.spotName }}</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="120" align="center">
          <template #default="{ row }">
            <div class="source-cell">
              <el-tag effect="light">{{ getSourceLabel(row.source) }}</el-tag>
              <div class="source-bucket-text">{{ getSourceBucketLabel(row.source) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="停留时长" width="120" align="center">
          <template #default="{ row }">{{ row.duration || 0 }} 秒</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="浏览时间" width="170" align="center" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchViewList"
          @current-change="fetchViewList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteView, getViewList } from '@/modules/user-ops/api/view-log.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'
import { getResourceUrl } from '@/shared/lib/resource.js'
import { getSourceBucketLabel, getSourceLabel, sourceOptions } from '@/shared/constants/view-source.js'

const router = useRouter()
const route = useRoute()
const skipNextRouteLoad = ref(false)

// 列表状态
const loading = ref(false)
const tableData = ref([])
const dateRange = ref([])
const errorMessage = ref('')

// 查询参数
const searchForm = reactive({
  nickname: '',
  spotName: '',
  source: ''
})

// 分页参数
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 当前页平均停留时长
const averageDuration = computed(() => {
  if (!tableData.value.length) return 0
  const totalDuration = tableData.value.reduce((sum, item) => sum + (item.duration || 0), 0)
  return Math.round(totalDuration / tableData.value.length)
})

// 当前页主要来源
const topSourceLabel = computed(() => {
  if (!tableData.value.length) return '暂无'
  const sourceCounter = tableData.value.reduce((acc, item) => {
    const key = item.source || 'unknown'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
  const topSource = Object.entries(sourceCounter).sort((a, b) => b[1] - a[1])[0]?.[0]
  return getSourceLabel(topSource)
})

// 获取浏览列表
const fetchViewList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getViewList(params)
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    errorMessage.value = error?.response?.data?.message || error?.message || '请稍后重试或检查接口返回。'
  } finally {
    loading.value = false
  }
}

// 搜索操作
const handleSearch = () => {
  pagination.page = 1
  syncRouteQuery()
  fetchViewList()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.nickname = ''
  searchForm.spotName = ''
  searchForm.source = ''
  dateRange.value = []
  handleSearch()
}

// 同步路由参数
const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.nickname) nextQuery.nickname = searchForm.nickname
  if (searchForm.spotName) nextQuery.spotName = searchForm.spotName
  if (searchForm.source) nextQuery.source = searchForm.source
  if (dateRange.value?.length === 2) {
    nextQuery.startDate = dateRange.value[0]
    nextQuery.endDate = dateRange.value[1]
  }
  const currentQuery = {}
  if (typeof route.query.nickname === 'string' && route.query.nickname) currentQuery.nickname = route.query.nickname
  if (typeof route.query.spotName === 'string' && route.query.spotName) currentQuery.spotName = route.query.spotName
  if (typeof route.query.source === 'string' && route.query.source) currentQuery.source = route.query.source
  if (typeof route.query.startDate === 'string' && route.query.startDate) currentQuery.startDate = route.query.startDate
  if (typeof route.query.endDate === 'string' && route.query.endDate) currentQuery.endDate = route.query.endDate
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
}

// 回填路由参数
const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
  searchForm.spotName = typeof route.query.spotName === 'string' ? route.query.spotName : ''
  searchForm.source = typeof route.query.source === 'string' ? route.query.source : ''
  if (typeof route.query.startDate === 'string' && typeof route.query.endDate === 'string') {
    dateRange.value = [route.query.startDate, route.query.endDate]
  } else {
    dateRange.value = []
  }
}

// 跳转用户页
const handleOpenUser = (row) => {
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
}

// 跳转景点页
const handleOpenSpot = (row) => {
  router.push({
    path: '/spot',
    query: {
      keyword: row.spotName || '',
      spotId: row.spotId || ''
    }
  })
}

// 删除浏览记录
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条浏览记录吗？', '删除确认', { type: 'warning' })
    await deleteView(row.id)
    ElMessage.success('删除成功')
    fetchViewList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('删除失败')
    }
  }
}

// 页面初始化
onMounted(() => {
  applyRouteQuery()
  fetchViewList()
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    if (skipNextRouteLoad.value) {
      skipNextRouteLoad.value = false
      return
    }
    fetchViewList()
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
@use '../styles/user-ops' as userOps;

.view-log-page {
  @include userOps.page-shell;
}

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

.source-alert {
  margin-bottom: 12px;
  border-radius: 14px;
}

.source-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.source-bucket-text {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

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
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.ops-table {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.ops-table .el-button.is-link) {
  padding: 0;
  margin: 0;
  min-width: 0;
  height: auto;
}

:deep(.ops-table th.el-table__cell) {
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

:deep(.ops-table .el-table__row:hover > td.el-table__cell) {
  background: var(--wt-row-gradient-hover) !important;
}

</style>

