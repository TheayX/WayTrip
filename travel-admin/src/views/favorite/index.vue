<template>
  <div class="favorite-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>用户收藏</span>
        </div>
      </template>

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
        <el-form-item label="收藏时间">
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

      <el-table :data="tableData" v-loading="loading" stripe>
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
        <el-table-column prop="createdAt" label="收藏时间" width="170" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenViewLog(row)">浏览行为</el-button>
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
          @size-change="fetchFavoriteList"
          @current-change="fetchFavoriteList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteFavorite, getFavoriteList } from '@/api/user-insight'
import { isMessageBoxDismissed } from '@/utils/message-box'
import { getResourceUrl } from '@/utils/resource'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const tableData = ref([])
const dateRange = ref([])

const searchForm = reactive({
  nickname: '',
  spotName: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const fetchFavoriteList = async () => {
  loading.value = true
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
    const res = await getFavoriteList(params)
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  syncRouteQuery()
  fetchFavoriteList()
}

const handleReset = () => {
  searchForm.nickname = ''
  searchForm.spotName = ''
  dateRange.value = []
  handleSearch()
}

const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.nickname) nextQuery.nickname = searchForm.nickname
  if (searchForm.spotName) nextQuery.spotName = searchForm.spotName
  if (dateRange.value?.length === 2) {
    nextQuery.startDate = dateRange.value[0]
    nextQuery.endDate = dateRange.value[1]
  }
  router.replace({ path: route.path, query: nextQuery })
}

const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
  searchForm.spotName = typeof route.query.spotName === 'string' ? route.query.spotName : ''
  if (typeof route.query.startDate === 'string' && typeof route.query.endDate === 'string') {
    dateRange.value = [route.query.startDate, route.query.endDate]
  } else {
    dateRange.value = []
  }
}

const handleOpenUser = (row) => {
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
}

const handleOpenSpot = (row) => {
  router.push({ path: '/spot', query: { keyword: row.spotName || '' } })
}

const handleOpenViewLog = (row) => {
  router.push({
    path: '/view-log',
    query: {
      nickname: row.nickname || '',
      spotName: row.spotName || ''
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条收藏记录吗？', '提示', { type: 'warning' })
    await deleteFavorite(row.id)
    ElMessage.success('删除成功')
    fetchFavoriteList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  applyRouteQuery()
  fetchFavoriteList()
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    fetchFavoriteList()
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
.favorite-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

.spot-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spot-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  flex-shrink: 0;
}
</style>
