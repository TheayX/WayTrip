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
        <el-table-column prop="nickname" label="用户昵称" width="140" />
        <el-table-column label="景点" min-width="240">
          <template #default="{ row }">
            <div class="spot-cell">
              <el-image v-if="row.coverImage" :src="getResourceUrl(row.coverImage)" fit="cover" class="spot-cover" />
              <span>{{ row.spotName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="收藏时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
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
          @size-change="fetchFavoriteList"
          @current-change="fetchFavoriteList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteFavorite, getFavoriteList } from '@/api/user-insight'
import { isMessageBoxDismissed } from '@/utils/message-box'
import { getResourceUrl } from '@/utils/resource'

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
  fetchFavoriteList()
}

const handleReset = () => {
  searchForm.nickname = ''
  searchForm.spotName = ''
  dateRange.value = []
  handleSearch()
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
  fetchFavoriteList()
})
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
