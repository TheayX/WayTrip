<template>
  <div class="user-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>

      <!-- 搜索 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="昵称">
          <el-input v-model="searchForm.nickname" placeholder="请输入昵称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户列表 -->
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" :size="40">{{ row.nickname?.charAt(0) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="favoriteCount" label="收藏数" width="100" />
        <el-table-column prop="ratingCount" label="评价数" width="100" />
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchUserList"
          @current-change="fetchUserList"
        />
      </div>
    </el-card>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="头像">
          <el-avatar :src="currentUser.avatar" :size="60">{{ currentUser.nickname?.charAt(0) }}</el-avatar>
        </el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || "未填写" }}</el-descriptions-item>
        <el-descriptions-item label="偏好标签" :span="2">{{ currentUser.preferences || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ currentUser.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ currentUser.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="收藏数">{{ currentUser.favoriteCount }}</el-descriptions-item>
        <el-descriptions-item label="评价数" :span="2">{{ currentUser.ratingCount }}</el-descriptions-item>
      </el-descriptions>

      <div class="recent-orders" v-if="currentUser?.recentOrders?.length">
        <h4>最近订单</h4>
        <el-table :data="currentUser.recentOrders" size="small">
          <el-table-column prop="orderNo" label="订单号" />
          <el-table-column prop="spotName" label="景点" />
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column prop="createdAt" label="时间" width="150" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserList, getUserDetail } from '@/api/user'

// 搜索表单
const searchForm = reactive({
  nickname: ''
})

// 列表数据
const loading = ref(false)
const userList = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 详情弹窗
const detailVisible = ref(false)
const currentUser = ref(null)

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    userList.value = res.data.list || []
    pagination.total = res.data.total
  } catch (e) {
    console.error('获取用户列表失败', e)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchUserList()
}

// 重置
const handleReset = () => {
  searchForm.nickname = ''
  handleSearch()
}

// 查看详情
const handleDetail = async (row) => {
  try {
    const res = await getUserDetail(row.id)
    currentUser.value = res.data
    detailVisible.value = true
  } catch (e) {
    console.error('获取用户详情失败', e)
  }
}

onMounted(() => {
  fetchUserList()
})
</script>

<style lang="scss" scoped>
.user-page {
  .search-form {
    margin-bottom: 20px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .recent-orders {
    margin-top: 20px;

    h4 {
      margin-bottom: 10px;
      color: #333;
    }
  }
}
</style>
