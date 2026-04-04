<template>
  <div class="preference-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">用户画像运营</p>
        <h1 class="page-title">用户偏好</h1>
        <p class="page-subtitle">查看用户画像标签与兴趣分布。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchPreferenceList">刷新数据</el-button>
      </div>
    </section>

    <section class="insight-stat-row">
      <el-card shadow="hover" class="insight-stat-card">
        <div class="insight-stat-label">覆盖用户</div>
        <div class="insight-stat-value">{{ pagination.total }}</div>
        <div class="insight-stat-desc">当前筛选条件下的用户画像记录数</div>
      </el-card>
      <el-card shadow="hover" class="insight-stat-card">
        <div class="insight-stat-label">当前页标签数</div>
        <div class="insight-stat-value">{{ currentPageTagCount }}</div>
        <div class="insight-stat-desc">当前页所有用户偏好标签的累计数量</div>
      </el-card>
      <el-card shadow="hover" class="insight-stat-card">
        <div class="insight-stat-label">当前页高频偏好</div>
        <div class="insight-stat-value">{{ topPreferenceTag }}</div>
        <div class="insight-stat-desc">用于快速观察当前筛选结果的主导偏好方向</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">
      <template #header>
        <div class="card-header">
          <span>偏好画像</span>
        </div>
      </template>

      <el-form :model="searchForm" inline class="search-form" @submit.prevent>
        <div class="filter-caption">
          <span class="filter-title">筛选用户画像</span>
          <span class="filter-subtitle">按用户昵称和偏好分类快速定位画像标签，再回到收藏、浏览和推荐结果页交叉验证。</span>
        </div>
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
        <el-form-item label="偏好分类">
          <el-select
            v-model="searchForm.categoryId"
            placeholder="全部"
            clearable
            filterable
            style="width: 220px"
            @change="handleSearch"
            @clear="handleSearch"
          >
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="用户偏好加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchPreferenceList">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <el-table v-else :data="tableData" v-loading="loading" class="ops-table borderless-table">
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="用户昵称" min-width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenUser(row)">{{ row.nickname }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="150" align="center">
          <template #default="{ row }">{{ formatPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column label="偏好标签" min-width="260">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="tag in row.preferenceTags || []" :key="tag" effect="light" round>{{ tag }}</el-tag>
              <span v-if="!row.preferenceTags?.length" class="empty-text">未设置</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近更新时间" width="170" align="center" />
        <el-table-column prop="createdAt" label="注册时间" width="170" align="center" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchPreferenceList"
          @current-change="fetchPreferenceList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPreferenceList } from '@/modules/user-ops/api/preference.js'
import { getFilters } from '@/modules/spot/api.js'

const router = useRouter()
const route = useRoute()
const skipNextRouteLoad = ref(false)

// 列表状态
const loading = ref(false)
const tableData = ref([])
const categoryOptions = ref([])
const errorMessage = ref('')

// 查询参数
const searchForm = reactive({
  nickname: '',
  categoryId: null
})

// 分页参数
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 当前页统计
const currentPageTagCount = computed(() => {
  return tableData.value.reduce((sum, item) => sum + (item.preferenceTags?.length || 0), 0)
})

const topPreferenceTag = computed(() => {
  const counter = tableData.value.reduce((acc, item) => {
    ;(item.preferenceTags || []).forEach((tag) => {
      acc[tag] = (acc[tag] || 0) + 1
    })
    return acc
  }, {})
  return Object.entries(counter).sort((a, b) => b[1] - a[1])[0]?.[0] || '暂无'
})

// 格式化手机号显示
const formatPhone = (phone) => {
  if (!phone || !phone.trim()) return '未绑定'
  const normalized = phone.trim()
  if (/^1\d{10}$/.test(normalized)) {
    return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  }
  return normalized
}

// 构建分类选项
const buildCategoryOptions = (nodes = [], level = 0) => {
  return nodes.reduce((acc, node) => {
    acc.push({
      id: node.id,
      label: `${'  '.repeat(level)}${level > 0 ? '└ ' : ''}${node.name}`
    })
    if (Array.isArray(node.children) && node.children.length) {
      acc.push(...buildCategoryOptions(node.children, level + 1))
    }
    return acc
  }, [])
}

// 获取筛选项
const fetchFilters = async () => {
  const res = await getFilters()
  categoryOptions.value = buildCategoryOptions(res.data.categoryTree || [])
}

// 获取偏好列表
const fetchPreferenceList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getPreferenceList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
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
  fetchPreferenceList()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.nickname = ''
  searchForm.categoryId = null
  handleSearch()
}

// 同步路由参数
const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.nickname) nextQuery.nickname = searchForm.nickname
  if (searchForm.categoryId != null) nextQuery.categoryId = String(searchForm.categoryId)
  const currentQuery = {}
  if (typeof route.query.nickname === 'string' && route.query.nickname) currentQuery.nickname = route.query.nickname
  if (typeof route.query.categoryId === 'string' && route.query.categoryId) currentQuery.categoryId = route.query.categoryId
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
}

// 回填路由参数
const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
  searchForm.categoryId = typeof route.query.categoryId === 'string' ? Number(route.query.categoryId) : null
}

const handleOpenUser = (row) => {
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
}

// 页面初始化
onMounted(() => {
  applyRouteQuery()
  fetchFilters()
  fetchPreferenceList()
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    if (skipNextRouteLoad.value) {
      skipNextRouteLoad.value = false
      return
    }
    fetchPreferenceList()
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
@use '../styles/user-ops' as userOps;

.preference-page {
  @include userOps.page-shell;

  .management-card {
    border-radius: 22px;
  }
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
  color: #0f172a;
}

.filter-subtitle {
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: #909399;
}

.ops-table {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.ops-table .el-button.is-link) {
  padding: 0;
  min-width: 0;
  height: auto;
}

:deep(.ops-table th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
}

:deep(.borderless-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.borderless-table td.el-table__cell),
:deep(.borderless-table th.el-table__cell.is-leaf) {
  border-bottom: 1px solid #f8fafc;
}

:deep(.ops-table .el-table__row:hover > td.el-table__cell) {
  background: linear-gradient(90deg, rgba(248, 250, 252, 0.5) 0%, #f1f5f9 50%, rgba(248, 250, 252, 0.5) 100%) !important;
}

</style>
