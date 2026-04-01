<template>
  <div class="preference-page">
    <el-card shadow="hover">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>用户偏好</span>
        </div>
      </template>

      <!-- 统计卡片 -->
      <div class="insight-stat-row">
        <el-card shadow="never" class="insight-stat-card">
          <div class="insight-stat-label">覆盖用户</div>
          <div class="insight-stat-value">{{ pagination.total }}</div>
          <div class="insight-stat-desc">当前筛选条件下的用户画像记录数</div>
        </el-card>
        <el-card shadow="never" class="insight-stat-card">
          <div class="insight-stat-label">当前页标签数</div>
          <div class="insight-stat-value">{{ currentPageTagCount }}</div>
          <div class="insight-stat-desc">当前页所有用户偏好标签的累计数量</div>
        </el-card>
        <el-card shadow="never" class="insight-stat-card">
          <div class="insight-stat-label">当前页高频偏好</div>
          <div class="insight-stat-value">{{ topPreferenceTag }}</div>
          <div class="insight-stat-desc">用于快速观察当前筛选结果的主导偏好方向</div>
        </el-card>
      </div>

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

      <!-- 偏好列表 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="nickname" label="用户昵称" min-width="140" />
        <el-table-column prop="phone" label="手机号" width="150">
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
        <el-table-column prop="updatedAt" label="最近更新时间" width="170" />
        <el-table-column prop="createdAt" label="注册时间" width="170" />
      </el-table>

      <!-- 分页器 -->
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

// 列表状态
const loading = ref(false)
const tableData = ref([])
const categoryOptions = ref([])

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
  try {
    const res = await getPreferenceList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
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
  router.replace({ path: route.path, query: nextQuery })
}

// 回填路由参数
const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
  searchForm.categoryId = typeof route.query.categoryId === 'string' ? Number(route.query.categoryId) : null
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
    fetchPreferenceList()
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
@use '@/modules/user-ops/user-ops.scss' as userOps;

.preference-page {
  @include userOps.page-shell;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: #909399;
}
</style>
