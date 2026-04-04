<!-- 用户管理页面 -->
<template>
  <div class="user-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">用户运营管理</p>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">查看用户信息、行为摘要与运营入口。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchUserList">刷新数据</el-button>
      </div>
    </section>

    <section class="insight-stat-row">
      <el-card shadow="hover" class="insight-stat-card">
        <div class="insight-stat-label">当前结果</div>
        <div class="insight-stat-value">{{ pagination.total }}</div>
        <div class="insight-stat-desc">符合当前搜索条件的用户数量</div>
      </el-card>
      <el-card shadow="hover" class="insight-stat-card">
        <div class="insight-stat-label">本页订单总数</div>
        <div class="insight-stat-value">{{ currentPageOrderCount }}</div>
        <div class="insight-stat-desc">当前页用户累计订单量，用于判断消费活跃度</div>
      </el-card>
      <el-card shadow="hover" class="insight-stat-card">
        <div class="insight-stat-label">本页互动总数</div>
        <div class="insight-stat-value">{{ currentPageEngagementCount }}</div>
        <div class="insight-stat-desc">当前页收藏、评价与浏览等行为汇总</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" inline class="search-form" @submit.prevent>
        <div class="filter-caption">
          <span class="filter-title">筛选用户</span>
          <span class="filter-subtitle">按昵称快速定位用户，再进入偏好、收藏和浏览行为页继续分析。</span>
        </div>
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

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="用户管理加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchUserList">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <!-- 用户列表 -->
      <el-table v-else :data="userList" v-loading="loading" class="user-table borderless-table">
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" :size="40">{{ row.nickname?.charAt(0) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column label="昵称" min-width="120">
          <template #default="{ row }">
            <el-button link type="primary" class="nickname-link" @click="handleDetail(row)">
              {{ row.nickname }}
            </el-button>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
              <el-dropdown trigger="click" @command="(command) => handleCommand(command, row)">
                <el-button link type="primary">
                  更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                    <el-dropdown-item command="preference">用户偏好</el-dropdown-item>
                    <el-dropdown-item command="favorite">用户收藏</el-dropdown-item>
                    <el-dropdown-item command="view-log">浏览行为</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
          <div class="summary-line">
            主要来源：{{ getViewSourceLabel(currentUser.viewSummary?.topSource) }}
            <span class="summary-line-muted">（{{ getSourceBucketLabel(currentUser.viewSummary?.topSource) }}）</span>
          </div>
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
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { getUserList, getUserDetail, resetUserPassword } from '@/modules/user-ops/api/user.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'
import { getSourceBucketLabel, getSourceLabel as resolveSourceLabel } from '@/shared/constants/view-source.js'

const router = useRouter()
const route = useRoute()
const skipNextRouteLoad = ref(false)

// 查询参数
const searchForm = reactive({
  nickname: ''
})

// 列表状态
const loading = ref(false)
const userList = ref([])
const errorMessage = ref('')
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 对话框与表单状态
const detailVisible = ref(false)
const currentUser = ref(null)
const currentPageOrderCount = computed(() => userList.value.reduce((sum, item) => sum + Number(item.orderCount || 0), 0))
const currentPageEngagementCount = computed(() => userList.value.reduce((sum, item) => {
  return sum + Number(item.favoriteCount || 0) + Number(item.ratingCount || 0) + Number(item.viewCount || 0)
}, 0))

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

// 格式化时间
const formatDateTime = (value) => {
  if (!value) return '暂无'
  return String(value).replace('T', ' ').slice(0, 19)
}

// 获取浏览来源文案
const getViewSourceLabel = (value) => resolveSourceLabel(value || '暂无')

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getUserList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    userList.value = res.data.list || []
    pagination.total = res.data.total
  } catch (e) {
    userList.value = []
    pagination.total = 0
    errorMessage.value = e?.response?.data?.message || e?.message || '请稍后重试或检查接口返回。'
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
  const currentQuery = route.query.nickname ? { nickname: route.query.nickname } : {}
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
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

const handleCommand = (command, row) => {
  switch (command) {
    case 'detail':
      handleDetail(row)
      break
    case 'preference':
      handleOpenPreference(row)
      break
    case 'favorite':
      handleOpenFavorite(row)
      break
    case 'view-log':
      handleOpenViewLog(row)
      break
  }
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
    if (skipNextRouteLoad.value) {
      skipNextRouteLoad.value = false
      return
    }
    fetchUserList()
  }
)
</script>

<style lang="scss" scoped>
@use '@/modules/user-ops/styles/user-ops.scss' as userOps;

.user-page {
  @include userOps.page-shell;
  display: flex;
  flex-direction: column;

  .management-card {
    border-radius: 22px;
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

  .recent-orders {
    margin-top: 24px;
    h4 {
      margin-bottom: 12px;
      color: #0f172a;
      font-weight: 600;
    }
  }

  .summary-grid {
    margin-top: 24px;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .summary-card {
    min-height: 180px;
    border-radius: 16px !important;
    border: 1px solid #f1f5f9 !important;
  }

  .summary-metric {
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
  }

  .summary-line {
    margin-top: 12px;
    color: #475569;
  }

  .summary-line-muted {
    color: #94a3b8;
  }

  .summary-meta {
    margin-top: 12px;
    font-size: 12px;
    color: #94a3b8;
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
    color: #94a3b8;
  }

  .summary-divider {
    margin: 0 6px;
    color: #e2e8f0;
  }

  .table-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }
}

.user-table {
  border-radius: 18px;
  overflow: hidden;
}

:deep(.user-table .el-button.is-link) {
  padding: 0;
  margin: 0;
  min-width: 0;
  height: auto;
}

.nickname-link {
  font-weight: 600;
}

:deep(.user-table th.el-table__cell) {
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

:deep(.user-table .el-table__row:hover > td.el-table__cell) {
  background: linear-gradient(90deg, rgba(248, 250, 252, 0.5) 0%, #f1f5f9 50%, rgba(248, 250, 252, 0.5) 100%) !important;
}

@media (max-width: 900px) {
  .user-page {
    .summary-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
