<!-- 订单中心页面 -->
<template>
  <div class="order-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">交易订单管理</p>
        <h1 class="page-title">订单中心</h1>
        <p class="page-subtitle">统一处理订单查询、状态流转与售后跟踪。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading || summaryLoading" @click="handleRefresh">刷新数据</el-button>
      </div>
    </section>

    <OrderSummaryCards
      :loading="summaryLoading"
      :current-tab="currentTab"
      :cards="summaryCards"
      @change-tab="handleTabChange"
    />



    <div class="page-action-row page-action-row--flush">
      <el-tabs :model-value="currentTab" class="workspace-tabs" @tab-change="handleTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.key" :label="tab.label" :name="tab.key" />
      </el-tabs>
    </div>

    <el-card shadow="never" class="workspace-card admin-management-card">


      <OrderFilterBar
        :current-tab="currentTab"
        :tab-label="getTabMeta(currentTab).label"
        :search-form="searchForm"
        :date-range="dateRange"
        :show-advanced="showAdvanced"
        @search="handleSearch"
        @reset="handleReset"
        @toggle-advanced="showAdvanced = !showAdvanced"
        @update:date-range="dateRange = $event"
      />

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="订单中心加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchOrderList">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <template v-else>
        <el-table
          :data="orderList"
          v-loading="loading"
          element-loading-text="正在加载订单数据..."
          class="order-table"
          empty-text="当前条件下暂无匹配订单"
        >
          <el-table-column label="订单号" width="200" align="left">
            <template #default="{ row }">
              <el-button link type="primary" class="order-link" @click="handleDetail(row)">
                {{ row.orderNo }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="景点名称" min-width="180" show-overflow-tooltip align="left">
            <template #default="{ row }">
              <el-button link type="primary" class="spot-link" :disabled="isInvalidSpot(row)" @click="handleOpenSpot(row)">{{ getDisplaySpotName(row) }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="userNickname" label="用户" width="120" align="left" />
          <el-table-column label="支付金额" width="150" align="left">
            <template #default="{ row }">
              <span class="metric-inline metric-inline--price">¥{{ formatCurrency(row.totalPrice) }}</span>
              <span class="quantity">({{ row.quantity }}张)</span>
            </template>
          </el-table-column>
          <el-table-column prop="visitDate" label="游玩日期" width="120" align="center" />
          <el-table-column label="联系人" width="160" align="left">
            <template #default="{ row }">
              <div>{{ row.contactName || '--' }}</div>
              <div class="text-subtle">{{ row.contactPhone || '--' }}</div>
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
          <el-table-column label="操作" width="220" fixed="right" align="left" header-align="center">
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

    <OrderDetailDrawer
      v-model:visible="detailVisible"
      :loading="detailLoading"
      :detail="currentOrder"
      :format-currency="formatCurrency"
      :get-status-tag-type="getStatusTagType"
      @complete="handleComplete"
      @refund="handleRefund"
      @cancel="handleCancel"
      @reopen="handleReopen"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, getOrderDetail, completeOrder, refundOrder, reopenOrder, cancelOrder } from '@/modules/order/api.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'
import { isInvalidSpotDisplay, resolveSpotDisplayName } from '@/shared/lib/resource-display.js'
import OrderFilterBar from '@/modules/order/components/OrderFilterBar.vue'
import OrderSummaryCards from '@/modules/order/components/OrderSummaryCards.vue'
import OrderDetailDrawer from '@/modules/order/components/OrderDetailDrawer.vue'

const router = useRouter()
const route = useRoute()

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
const skipNextRouteLoad = ref(false)

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

const getDisplaySpotName = (row) => resolveSpotDisplayName(row?.spotName)
const isInvalidSpot = (row) => isInvalidSpotDisplay(row?.spotName)

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

const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.orderNo) {
    nextQuery.orderNo = searchForm.orderNo
  }
  if (searchForm.spotName) {
    nextQuery.spotName = searchForm.spotName
  }
  if (searchForm.status) {
    nextQuery.status = searchForm.status
  }
  const currentQuery = {}
  if (typeof route.query.orderNo === 'string' && route.query.orderNo) {
    currentQuery.orderNo = route.query.orderNo
  }
  if (typeof route.query.spotName === 'string' && route.query.spotName) {
    currentQuery.spotName = route.query.spotName
  }
  if (typeof route.query.status === 'string' && route.query.status) {
    currentQuery.status = route.query.status
  }
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
}

// 全局搜索会通过 query 落到订单页，这里统一回填筛选状态，避免跳转后还要手工再搜一次。
const applyRouteQuery = () => {
  searchForm.orderNo = typeof route.query.orderNo === 'string' ? route.query.orderNo : ''
  searchForm.spotName = typeof route.query.spotName === 'string' ? route.query.spotName : ''
  searchForm.status = typeof route.query.status === 'string' ? route.query.status : ''
}

const handleSearch = () => {
  pagination.page = 1
  syncRouteQuery()
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
  syncRouteQuery()
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
  if (isInvalidSpot(row)) return
  router.push({
    path: '/spot',
    query: {
      keyword: getDisplaySpotName(row),
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

watch(() => route.query, () => {
  if (skipNextRouteLoad.value) {
    skipNextRouteLoad.value = false
    return
  }
  applyRouteQuery()
  pagination.page = 1
  refreshDashboardData()
})

onMounted(() => {
  applyRouteQuery()
  refreshDashboardData()
})
</script>

<style lang="scss" scoped>
.order-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .quantity {
    color: var(--wt-text-secondary);
    font-size: 12px;
    margin-left: 6px;
  }

}

.workspace-tabs {
  margin-top: -6px;
}

.order-table {
  border-radius: 18px;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}

.order-link,
.spot-link {
  padding: 0 !important;
  margin: 0 !important;
  min-width: 0;
  height: auto;
  font-weight: 600;
}

.order-link {
  color: var(--wt-accent-blue-text);
}

.spot-link {
  color: var(--wt-text-primary);

  &:hover {
    color: var(--el-color-primary);
  }
}

:deep(.workspace-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.order-table th.el-table__cell) {
  background: var(--el-table-header-bg-color);
  color: var(--wt-text-regular);
  font-weight: 600;
}

:deep(.order-table .el-table__row:hover > td.el-table__cell) {
  background: var(--wt-fill-hover);
}

:deep(.workspace-tabs .el-tabs__item.is-active) {
  color: var(--wt-text-primary);
}

:deep(.workspace-tabs .el-tabs__active-bar) {
  background: var(--el-color-primary);
}

@media (max-width: 960px) {
  .workspace-tabs,
  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
