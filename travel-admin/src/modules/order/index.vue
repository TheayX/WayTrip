<!-- 订单中心页面 -->
<template>
  <div class="order-page">
    <section class="page-hero">
      <div>
        <p class="page-kicker">Transaction Workspace</p>
        <h1 class="page-title">订单中心</h1>
        <p class="page-subtitle">统一处理订单查询、状态流转与订单追踪。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading || summaryLoading" @click="handleRefresh">刷新数据</el-button>
      </div>
    </section>

    <!-- 状态摘要卡片 -->
    <section class="summary-grid" v-loading="summaryLoading">
      <button
        v-for="card in summaryCards"
        :key="card.key"
        type="button"
        class="summary-card"
        :class="{ active: currentTab === card.key }"
        @click="handleTabChange(card.key)"
      >
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-hint">{{ card.hint }}</div>
      </button>
    </section>

    <el-card shadow="never" class="workspace-card">
      <div class="workspace-head">
        <div>
          <h2 class="workspace-title">工作区</h2>
          <p class="workspace-subtitle">按状态工作区快速收敛订单处理任务。</p>
        </div>
        <el-tabs :model-value="currentTab" class="workspace-tabs" @tab-change="handleTabChange">
          <el-tab-pane v-for="tab in tabs" :key="tab.key" :label="tab.label" :name="tab.key" />
        </el-tabs>
      </div>

      <!-- 搜索筛选表单 -->
      <div class="filter-panel">
        <el-form :model="searchForm" @submit.prevent>
          <div class="filter-row">
            <div class="filter-main">
              <el-form-item class="filter-item mb-0">
                <el-input
                  v-model="searchForm.orderNo"
                  placeholder="搜索订单号"
                  clearable
                  class="filter-input"
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
                />
              </el-form-item>
              <el-form-item class="filter-item mb-0">
                <el-input
                  v-model="searchForm.spotName"
                  placeholder="搜索景点名称"
                  clearable
                  class="filter-input"
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
                />
              </el-form-item>
              <el-form-item class="filter-item mb-0">
                <el-select
                  v-model="searchForm.status"
                  placeholder="全部状态"
                  clearable
                  class="filter-select"
                  :disabled="currentTab !== 'all'"
                  @change="handleSearch"
                  @clear="handleSearch"
                >
                  <el-option label="待支付" value="pending" />
                  <el-option label="已支付" value="paid" />
                  <el-option label="已取消" value="cancelled" />
                  <el-option label="已退款" value="refunded" />
                  <el-option label="已完成" value="completed" />
                </el-select>
              </el-form-item>
              <el-button type="primary" link class="toggle-btn" @click="showAdvanced = !showAdvanced">
                {{ showAdvanced ? '收起筛选' : '更多筛选' }}
              </el-button>
            </div>

            <div class="filter-actions">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </div>
          </div>

          <el-alert
            v-if="currentTab !== 'all'"
            type="info"
            :closable="false"
            class="scope-alert"
            :title="`${getTabMeta(currentTab).label}工作区已限定订单状态，需精确筛选时请切回“全部”。`"
          />

          <!-- 低频条件默认折叠，避免页面顶部持续过厚 -->
          <el-collapse-transition>
            <div v-show="showAdvanced" class="advanced-panel">
              <el-form-item label="下单时间" class="filter-item mb-0">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  class="date-picker"
                  @change="handleSearch"
                />
              </el-form-item>
            </div>
          </el-collapse-transition>
        </el-form>
      </div>

      <div v-if="errorMessage" class="error-state">
        <el-result icon="error" title="订单数据加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchOrderList">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <template v-else>
        <!-- 订单列表 -->
        <el-table :data="orderList" v-loading="loading" class="order-table" empty-text="当前条件下暂无匹配订单">
          <el-table-column label="订单号" width="200" align="left">
            <template #default="{ row }">
              <el-button link type="primary" class="order-link" @click="handleDetail(row)">
                {{ row.orderNo }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="景点名称" min-width="180" show-overflow-tooltip align="left">
            <template #default="{ row }">
              <el-button link type="primary" class="spot-link" @click="handleOpenSpot(row)">{{ row.spotName }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="userNickname" label="用户" width="120" align="left" />
          <el-table-column label="支付金额" width="150" align="left">
            <template #default="{ row }">
              <span class="price">¥{{ formatCurrency(row.totalPrice) }}</span>
              <span class="quantity">({{ row.quantity }}张)</span>
            </template>
          </el-table-column>
          <el-table-column prop="visitDate" label="游玩日期" width="120" align="center" />
          <el-table-column label="联系人" width="160" align="left">
            <template #default="{ row }">
              <div>{{ row.contactName || '--' }}</div>
              <div class="text-gray">{{ row.contactPhone || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag effect="light" round :type="getStatusTagType(row.status)">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="下单时间" width="180" align="center" />
          <el-table-column label="操作" width="220" fixed="right" align="left">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'paid'" type="success" link @click="handleComplete(row)">完成</el-button>
                <el-button v-if="row.status === 'paid'" type="danger" link @click="handleRefund(row)">退款</el-button>
                <el-button v-if="row.status === 'pending'" type="danger" link @click="handleCancel(row)">取消</el-button>
                <el-button v-if="row.status === 'completed'" type="warning" link @click="handleReopen(row)">撤销完成</el-button>
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
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </el-card>

    <!-- 订单详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      title="订单详情"
      size="520px"
      class="order-detail-drawer"
    >
      <div v-loading="detailLoading" class="detail-container">
        <template v-if="currentOrder">
          <section class="detail-hero">
            <div class="detail-header">
              <div>
                <p class="detail-order-no">{{ currentOrder.orderNo }}</p>
                <h3 class="detail-spot-name">{{ currentOrder.spotName }}</h3>
              </div>
              <el-tag effect="light" round :type="getStatusTagType(currentOrder.status)">
                {{ currentOrder.statusText }}
              </el-tag>
            </div>

            <el-image
              v-if="currentOrder.spotImage"
              :src="currentOrder.spotImage"
              fit="cover"
              class="detail-cover"
            />

            <div class="detail-stats">
              <div class="detail-stat-card">
                <span class="detail-stat-label">总金额</span>
                <strong class="price-large">¥{{ formatCurrency(currentOrder.totalPrice) }}</strong>
              </div>
              <div class="detail-stat-card">
                <span class="detail-stat-label">游玩日期</span>
                <strong>{{ currentOrder.visitDate || '--' }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <h4 class="section-title">订单基础信息</h4>
            <el-descriptions :column="1" border class="detail-descriptions">
              <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="下单时间">{{ currentOrder.createdAt || '--' }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ currentOrder.updatedAt || '--' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h4 class="section-title">景点与游玩信息</h4>
            <el-descriptions :column="1" border class="detail-descriptions">
              <el-descriptions-item label="景点名称">{{ currentOrder.spotName || '--' }}</el-descriptions-item>
              <el-descriptions-item label="单价">¥{{ formatCurrency(currentOrder.unitPrice) }}</el-descriptions-item>
              <el-descriptions-item label="数量">{{ currentOrder.quantity || '--' }} 张</el-descriptions-item>
              <el-descriptions-item label="总价">¥{{ formatCurrency(currentOrder.totalPrice) }}</el-descriptions-item>
              <el-descriptions-item label="游玩日期">{{ currentOrder.visitDate || '--' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h4 class="section-title">联系人信息</h4>
            <el-descriptions :column="1" border class="detail-descriptions">
              <el-descriptions-item label="联系人">{{ currentOrder.contactName || '--' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ currentOrder.contactPhone || '--' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h4 class="section-title">关键时间节点</h4>
            <el-timeline class="detail-timeline">
              <el-timeline-item timestamp="下单时间" :hollow="!currentOrder.createdAt">
                {{ currentOrder.createdAt || '--' }}
              </el-timeline-item>
              <el-timeline-item timestamp="支付时间" :hollow="!currentOrder.paidAt">
                {{ currentOrder.paidAt || '--' }}
              </el-timeline-item>
              <el-timeline-item timestamp="完成时间" :hollow="!currentOrder.completedAt">
                {{ currentOrder.completedAt || '--' }}
              </el-timeline-item>
              <el-timeline-item timestamp="取消时间" :hollow="!currentOrder.cancelledAt">
                {{ currentOrder.cancelledAt || '--' }}
              </el-timeline-item>
              <el-timeline-item timestamp="退款时间" :hollow="!currentOrder.refundedAt">
                {{ currentOrder.refundedAt || '--' }}
              </el-timeline-item>
            </el-timeline>
          </section>
        </template>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button v-if="currentOrder?.status === 'pending'" type="danger" @click="handleCancel(currentOrder)">取消订单</el-button>
          <el-button v-if="currentOrder?.status === 'paid'" type="success" @click="handleComplete(currentOrder)">完成订单</el-button>
          <el-button v-if="currentOrder?.status === 'paid'" type="danger" plain @click="handleRefund(currentOrder)">退款订单</el-button>
          <el-button v-if="currentOrder?.status === 'completed'" type="warning" @click="handleReopen(currentOrder)">撤销完成</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, getOrderDetail, completeOrder, refundOrder, reopenOrder, cancelOrder } from '@/modules/order/api.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'

const router = useRouter()

const tabs = [
  { key: 'all', label: '全部', statuses: [] },
  { key: 'pending', label: '待支付', statuses: ['pending'] },
  { key: 'paid', label: '已支付', statuses: ['paid'] },
  { key: 'completed', label: '已完成', statuses: ['completed'] },
  { key: 'closed', label: '已关闭', statuses: ['cancelled', 'refunded'] }
]

const currentTab = ref('all')
const showAdvanced = ref(false)
const searchForm = reactive({ orderNo: '', spotName: '', status: '' })
const dateRange = ref([])
const loading = ref(false)
const summaryLoading = ref(false)
const detailLoading = ref(false)
const errorMessage = ref('')
const orderList = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const detailVisible = ref(false)
const currentOrder = ref(null)
const summaryStats = reactive({
  all: 0,
  pending: 0,
  paid: 0,
  completed: 0,
  closed: 0
})

const summaryCards = computed(() => ([
  { key: 'all', label: '全部订单', value: summaryStats.all, hint: '查看全部订单总量' },
  { key: 'pending', label: '待支付', value: summaryStats.pending, hint: '待支付订单需要持续关注' },
  { key: 'paid', label: '已支付', value: summaryStats.paid, hint: '已支付订单仍可完成或退款' },
  { key: 'completed', label: '已完成', value: summaryStats.completed, hint: '已完成订单用于运营复盘' },
  { key: 'closed', label: '已关闭', value: summaryStats.closed, hint: '已取消与已退款订单的汇总视图' }
]))

const getTabMeta = (tabKey) => tabs.find((item) => item.key === tabKey) || tabs[0]

const buildBaseParams = (page, pageSize) => {
  const params = {
    orderNo: searchForm.orderNo,
    spotName: searchForm.spotName,
    page,
    pageSize
  }

  if (dateRange.value?.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }

  return params
}

const getStatusTagType = (status) => {
  return {
    pending: 'warning',
    paid: 'primary',
    cancelled: 'info',
    refunded: 'danger',
    completed: 'success'
  }[status] || 'info'
}

const formatCurrency = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }

  const numeric = Number(value)
  return Number.isNaN(numeric) ? String(value) : numeric.toFixed(2)
}

const extractErrorMessage = (error, fallback) => {
  return error?.response?.data?.message || error?.message || fallback
}

const mergeCompositeList = (responses, page, pageSize) => {
  const merged = responses
    .flatMap((item) => item.data.list || [])
    .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())

  const start = (page - 1) * pageSize
  return {
    list: merged.slice(start, start + pageSize),
    total: responses.reduce((sum, item) => sum + Number(item.data.total || 0), 0)
  }
}

// 复合状态页签通过按需扩容抓取，优先保证分页结果正确，再考虑后端是否补复合筛选接口。
const fetchCompositeOrders = async (statuses) => {
  const params = buildBaseParams(1, pagination.page * pagination.pageSize)
  const responses = await Promise.all(
    statuses.map((status) => getOrderList({ ...params, status }))
  )

  return mergeCompositeList(responses, pagination.page, pagination.pageSize)
}

const fetchOrderList = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const currentStatuses = getTabMeta(currentTab.value).statuses

    if (currentTab.value === 'all') {
      const params = {
        ...buildBaseParams(pagination.page, pagination.pageSize),
        status: searchForm.status || undefined
      }
      const res = await getOrderList(params)
      orderList.value = res.data.list || []
      pagination.total = Number(res.data.total || 0)
      return
    }

    if (currentStatuses.length === 1) {
      const res = await getOrderList({
        ...buildBaseParams(pagination.page, pagination.pageSize),
        status: currentStatuses[0]
      })
      orderList.value = res.data.list || []
      pagination.total = Number(res.data.total || 0)
      return
    }

    const composite = await fetchCompositeOrders(currentStatuses)
    orderList.value = composite.list
    pagination.total = composite.total
  } catch (error) {
    orderList.value = []
    pagination.total = 0
    errorMessage.value = extractErrorMessage(error, '请稍后重试或检查接口返回。')
  } finally {
    loading.value = false
  }
}

const fetchSummaryStats = async () => {
  summaryLoading.value = true

  try {
    const [allRes, pendingRes, paidRes, completedRes, cancelledRes, refundedRes] = await Promise.all([
      getOrderList({ ...buildBaseParams(1, 1) }),
      getOrderList({ ...buildBaseParams(1, 1), status: 'pending' }),
      getOrderList({ ...buildBaseParams(1, 1), status: 'paid' }),
      getOrderList({ ...buildBaseParams(1, 1), status: 'completed' }),
      getOrderList({ ...buildBaseParams(1, 1), status: 'cancelled' }),
      getOrderList({ ...buildBaseParams(1, 1), status: 'refunded' })
    ])

    summaryStats.all = Number(allRes.data.total || 0)
    summaryStats.pending = Number(pendingRes.data.total || 0)
    summaryStats.paid = Number(paidRes.data.total || 0)
    summaryStats.completed = Number(completedRes.data.total || 0)
    summaryStats.closed = Number(cancelledRes.data.total || 0) + Number(refundedRes.data.total || 0)
  } catch (error) {
    ElMessage.warning(extractErrorMessage(error, '订单摘要加载失败'))
  } finally {
    summaryLoading.value = false
  }
}

const refreshDashboardData = async () => {
  await Promise.all([fetchOrderList(), fetchSummaryStats()])
}

const handleSearch = () => {
  pagination.page = 1
  refreshDashboardData()
}

const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.spotName = ''
  searchForm.status = ''
  dateRange.value = []
  pagination.page = 1
  refreshDashboardData()
}

const handleRefresh = () => {
  refreshDashboardData()
}

const handleTabChange = (tabKey) => {
  currentTab.value = tabKey
  searchForm.status = ''
  pagination.page = 1
  fetchOrderList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchOrderList()
}

const handlePageSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchOrderList()
}

const handleOpenSpot = (row) => {
  router.push({
    path: '/spot',
    query: {
      keyword: row.spotName || '',
      spotId: row.spotId || ''
    }
  })
}

const handleDetail = async (row) => {
  detailLoading.value = true
  detailVisible.value = true

  try {
    const res = await getOrderDetail(row.id)
    currentOrder.value = res.data
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(extractErrorMessage(error, '获取订单详情失败'))
  } finally {
    detailLoading.value = false
  }
}

const runOrderAction = async ({ row, confirmText, hintText, request, successText }) => {
  try {
    await ElMessageBox.confirm(`${confirmText}\n${hintText}`, '订单状态确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    const res = await request(row.id)
    currentOrder.value = currentOrder.value?.id === row.id ? res.data : currentOrder.value
    ElMessage.success(successText)
    await refreshDashboardData()
  } catch (error) {
    if (!isMessageBoxDismissed(error)) {
      ElMessage.error(extractErrorMessage(error, '操作失败'))
    }
  }
}

const handleComplete = (row) => runOrderAction({
  row,
  confirmText: `确认将订单【${row.orderNo}】标记为已完成？`,
  hintText: '完成后订单将进入已完成状态，并影响后续推荐行为统计。',
  request: completeOrder,
  successText: '订单已完成'
})

const handleRefund = (row) => runOrderAction({
  row,
  confirmText: `确认将订单【${row.orderNo}】标记为已退款？`,
  hintText: '当前版本为直接退款，不经过审核流。',
  request: refundOrder,
  successText: '订单已退款'
})

const handleCancel = (row) => runOrderAction({
  row,
  confirmText: `确认取消订单【${row.orderNo}】？`,
  hintText: '仅未支付订单允许取消。',
  request: cancelOrder,
  successText: '订单已取消'
})

const handleReopen = (row) => runOrderAction({
  row,
  confirmText: `确认将订单【${row.orderNo}】恢复为已支付状态？`,
  hintText: '恢复后会清除订单完成时间。',
  request: reopenOrder,
  successText: '订单已撤销完成'
})

onMounted(() => {
  refreshDashboardData()
})
</script>

<style lang="scss" scoped>
.order-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .price {
    color: #dc2626;
    font-weight: 700;
    font-size: 14px;
  }

  .quantity {
    color: #94a3b8;
    font-size: 12px;
    margin-left: 6px;
  }

  .text-gray {
    color: #94a3b8;
    font-size: 12px;
    margin-top: 2px;
  }

  .price-large {
    color: #dc2626;
    font-size: 24px;
    font-weight: 700;
  }
}

.page-hero,
.workspace-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-hero {
  padding: 4px 2px;
}

.page-kicker {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-title,
.workspace-title {
  margin: 0;
  color: #0f172a;
}

.page-title {
  font-size: 30px;
  line-height: 1.2;
}

.page-subtitle,
.workspace-subtitle {
  margin: 8px 0 0;
  color: #64748b;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.summary-card {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  padding: 18px 20px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    transform: translateY(-1px);
  }

  &.active {
    border-color: #2563eb;
    box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
  }
}

.summary-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.summary-value {
  margin-top: 12px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}

.summary-hint {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.workspace-card {
  border: none;
  border-radius: 20px;
}

.workspace-title {
  font-size: 22px;
}

.workspace-tabs {
  margin-top: -6px;
}

.filter-panel {
  margin-top: 16px;
  margin-bottom: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
  padding: 16px 18px;
}

.filter-row,
.filter-main,
.filter-actions,
.table-actions,
.detail-header,
.detail-stats,
.drawer-footer {
  display: flex;
  gap: 12px;
}

.filter-row {
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.filter-main {
  flex-wrap: wrap;
  align-items: center;
  flex: 1;
}

.filter-actions,
.drawer-footer {
  align-items: center;
  justify-content: flex-end;
}

.filter-item {
  margin-bottom: 0;
}

.filter-input,
.filter-select {
  width: 220px;
}

.toggle-btn {
  font-weight: 600;
}

.scope-alert {
  margin-top: 14px;
}

.advanced-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.date-picker {
  width: 260px;
}

.error-state {
  padding: 20px 0 8px;
}

.order-table {
  border-radius: 16px;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.order-link,
.spot-link {
  padding: 0;
  min-width: 0;
  height: auto;
  font-weight: 600;
}

.order-link {
  color: #2563eb;
}

.spot-link {
  color: #1e293b;
}

.table-actions {
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: flex-start;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-hero {
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  padding: 18px;
}

.detail-order-no {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 13px;
}

.detail-spot-name {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.detail-cover {
  width: 100%;
  height: 180px;
  margin-top: 16px;
  border-radius: 14px;
}

.detail-stats {
  margin-top: 16px;
}

.detail-stat-card {
  flex: 1;
  min-width: 0;
  border-radius: 14px;
  background: #ffffff;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
}

.detail-stat-label {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  margin: 0;
  padding-left: 10px;
  border-left: 4px solid var(--el-color-primary);
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.detail-timeline {
  margin: 4px 0 0;
}

.drawer-footer {
  width: 100%;
}

:deep(.workspace-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.filter-panel .el-input__wrapper),
:deep(.filter-panel .el-select__wrapper),
:deep(.filter-panel .el-date-editor.el-input__wrapper) {
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

:deep(.order-table th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
}

:deep(.order-table .el-table__row:hover > td.el-table__cell) {
  background: #f8fbff;
}

:deep(.detail-descriptions .el-descriptions__label) {
  width: 100px;
  background: #f8fafc !important;
  color: #64748b;
  font-weight: 500;
}

:deep(.detail-descriptions .el-descriptions__cell) {
  padding: 12px 16px !important;
}

@media (max-width: 960px) {
  .page-hero,
  .workspace-head,
  .filter-row {
    flex-direction: column;
  }

  .workspace-tabs {
    width: 100%;
  }

  .filter-actions,
  .hero-actions {
    width: 100%;
  }

  .filter-actions :deep(.el-button),
  .hero-actions :deep(.el-button) {
    width: 100%;
  }

  .filter-input,
  .filter-select,
  .date-picker {
    width: 100%;
  }

  .detail-stats {
    flex-direction: column;
  }
}
</style>
