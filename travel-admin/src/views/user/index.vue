<!-- 用户管理页面 -->
<template>
  <div class="user-page">
    <el-card  shadow="hover">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" inline class="search-form" @submit.prevent>
        <el-form-item label="昵称">
          <el-input
            v-model="searchForm.nickname"
            placeholder="请输入昵称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
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
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">
            {{ formatPhone(row.phone) }}
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="favoriteCount" label="收藏数" width="100" />
        <el-table-column prop="ratingCount" label="评价数" width="100" />
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column prop="updatedAt" label="修改时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div style="white-space: nowrap;">
              <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
              <el-button type="primary" link @click="handleOpenPreference(row)">偏好</el-button>
              <el-button type="primary" link @click="handleOpenFavorite(row)">收藏</el-button>
              <el-button type="primary" link @click="handleOpenViewLog(row)">浏览</el-button>
              <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
            </div>
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
          @size-change="fetchUserList"
          @current-change="fetchUserList"
        />
      </div>
    </el-card>

    <!-- 用户详情对话框 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="头像">
          <el-avatar :src="currentUser.avatar" :size="60">{{ currentUser.nickname?.charAt(0) }}</el-avatar>
        </el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ formatPhone(currentUser.phone) }}</el-descriptions-item>
        <el-descriptions-item label="偏好标签" :span="2">{{ currentUser.preferences || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ currentUser.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="修改时间" :span="2">{{ currentUser.updatedAt }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ currentUser.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="收藏数">{{ currentUser.favoriteCount }}</el-descriptions-item>
        <el-descriptions-item label="评价数">{{ currentUser.ratingCount }}</el-descriptions-item>
        <el-descriptions-item label="浏览数">{{ currentUser.viewCount }}</el-descriptions-item>
      </el-descriptions>

      <div class="summary-grid" v-if="currentUser">
        <el-card shadow="never" class="summary-card">
          <template #header>
            <span>偏好画像</span>
          </template>
          <div class="summary-metric">标签数：{{ currentUser.preferenceSummary?.count ?? 0 }}</div>
          <div class="summary-tags" v-if="currentUser.preferenceSummary?.tags?.length">
            <el-tag v-for="tag in currentUser.preferenceSummary.tags" :key="tag" effect="light">{{ tag }}</el-tag>
          </div>
          <div class="summary-empty" v-else>暂无偏好标签</div>
          <div class="summary-meta">最近更新时间：{{ formatDateTime(currentUser.preferenceSummary?.updatedAt) }}</div>
        </el-card>

        <el-card shadow="never" class="summary-card">
          <template #header>
            <span>收藏行为</span>
          </template>
          <div class="summary-metric">累计收藏：{{ currentUser.favoriteCount || 0 }}</div>
          <div class="summary-line">最近收藏：{{ currentUser.favoriteSummary?.latestSpotName || '暂无' }}</div>
          <div class="summary-meta">最近收藏时间：{{ formatDateTime(currentUser.favoriteSummary?.latestCreatedAt) }}</div>
        </el-card>

        <el-card shadow="never" class="summary-card">
          <template #header>
            <span>浏览行为</span>
          </template>
          <div class="summary-metric">累计浏览：{{ currentUser.viewCount || 0 }}</div>
          <div class="summary-line">最近浏览：{{ currentUser.viewSummary?.latestSpotName || '暂无' }}</div>
          <div class="summary-line">主要来源：{{ getViewSourceLabel(currentUser.viewSummary?.topSource) }}</div>
          <div class="summary-meta">
            平均停留：{{ currentUser.viewSummary?.averageDuration ?? 0 }} 秒
            <span class="summary-divider">|</span>
            最近浏览时间：{{ formatDateTime(currentUser.viewSummary?.latestCreatedAt) }}
          </div>
        </el-card>
      </div>

      <div class="recent-orders" v-if="currentUser?.recentOrders?.length">
        <h4>最近订单</h4>
        <el-table :data="currentUser.recentOrders" size="small">
          <el-table-column prop="orderNo" label="订单号" />
          <el-table-column prop="spotName" label="景点" />
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column prop="createdAt" label="时间" width="150" />
          <el-table-column prop="updatedAt" label="修改时间" width="150" />
        </el-table>
      </div>

      <template #footer>
        <el-button v-if="currentUser" @click="handleOpenPreference(currentUser)">查看偏好</el-button>
        <el-button v-if="currentUser" @click="handleOpenFavorite(currentUser)">查看收藏</el-button>
        <el-button v-if="currentUser" @click="handleOpenViewLog(currentUser)">查看浏览</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserList, getUserDetail, resetUserPassword } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isMessageBoxDismissed } from '@/utils/message-box'

const router = useRouter()
const route = useRoute()

// 查询参数
const searchForm = reactive({
  nickname: ''
})

// 列表状态
const loading = ref(false)
const userList = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 对话框与表单状态
const detailVisible = ref(false)
const currentUser = ref(null)

const viewSourceMap = {
  home: '首页',
  search: '搜索',
  recommend: '推荐',
  guide: '攻略',
  detail: '详情'
}

// 格式化手机号显示
const formatPhone = (phone) => {
  if (!phone || !phone.trim()) return '未绑定'
  const normalized = phone.trim()
  if (/^1\d{10}$/.test(normalized)) {
    return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  }
  if (/^1\d{2}\*{4}\d{4}$/.test(normalized)) {
    return normalized
  }
  return '已隐藏'
}

const formatDateTime = (value) => {
  if (!value) return '暂无'
  return String(value).replace('T', ' ').slice(0, 19)
}

const getViewSourceLabel = (value) => {
  return viewSourceMap[value] || value || '暂无'
}

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

// 搜索操作
const handleSearch = () => {
  pagination.page = 1
  syncRouteQuery()
  fetchUserList()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.nickname = ''
  handleSearch()
}

const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.nickname) {
    nextQuery.nickname = searchForm.nickname
  }
  router.replace({ path: route.path, query: nextQuery })
}

const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
}

const openUserOpsPage = (path, row) => {
  detailVisible.value = false
  router.push({
    path,
    query: {
      nickname: row.nickname || ''
    }
  })
}

const handleOpenPreference = (row) => {
  openUserOpsPage('/preference', row)
}

const handleOpenFavorite = (row) => {
  openUserOpsPage('/favorite', row)
}

const handleOpenViewLog = (row) => {
  openUserOpsPage('/view-log', row)
}

// 打开用户详情对话框
const handleDetail = async (row) => {
  try {
    const res = await getUserDetail(row.id)
    currentUser.value = res.data
    detailVisible.value = true
  } catch (e) {
    console.error('获取用户详情失败', e)
  }
}

// 重置密码
const handleResetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入用户「${row.nickname}」的新密码（至少6位）`,
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '请输入新密码',
        inputValidator: (val) => {
          if (!val || val.length < 6) return '密码长度至少6个字符'
          return true
        }
      }
    )
    await resetUserPassword(row.id, { newPassword: value })
    ElMessage.success('密码重置成功')
  } catch (e) {
    if (!isMessageBoxDismissed(e)) console.error('重置密码失败', e)
  }
}

// 页面初始化
onMounted(() => {
  applyRouteQuery()
  fetchUserList()
})

watch(
  () => route.query.nickname,
  () => {
    applyRouteQuery()
    fetchUserList()
  }
)
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

  .summary-grid {
    margin-top: 20px;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }

  .summary-card {
    min-height: 180px;
  }

  .summary-metric {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .summary-line {
    margin-top: 12px;
    color: #606266;
  }

  .summary-meta {
    margin-top: 12px;
    font-size: 12px;
    color: #909399;
    line-height: 1.6;
  }

  .summary-tags {
    margin-top: 12px;
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .summary-empty {
    margin-top: 12px;
    color: #909399;
  }

  .summary-divider {
    margin: 0 6px;
  }
}

@media (max-width: 900px) {
  .user-page {
    .summary-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
