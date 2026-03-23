<template>
  <div class="review-page">
    <el-card  shadow="hover">
      <template #header>
        <div class="card-header">
          <span>评价管理</span>
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

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
              <span>{{ row.spotName || `景点 #${row.spotId}` }}</span>
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
      </el-table>

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
import { getReviewList } from '@/api/review'

const loading = ref(false)
const reviewList = ref([])

const searchForm = reactive({
  nickname: '',
  spotName: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

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

onMounted(() => {
  fetchReviewList()
})
</script>

<style lang="scss" scoped>
.review-page {
  .search-form {
    margin-bottom: 20px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
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
  border-radius: 8px;
  flex-shrink: 0;
}

.score-text {
  color: #e6a23c;
  font-weight: 600;
}
</style>
