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
        :visit-date-range="visitDateRange"
        :show-advanced="showAdvanced"
        @search="handleSearch"
        @reset="handleReset"
        @toggle-advanced="showAdvanced = !showAdvanced"
        @update:date-range="dateRange = $event"
        @update:visit-date-range="visitDateRange = $event"
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
          @sort-change="handleSortChange"
        >
          <el-table-column label="订单号" width="176" align="left" header-align="center">
            <template #default="{ row }">
              <el-button link type="primary" class="order-link" @click="handleDetail(row)">
                {{ row.orderNo }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="景点名称" min-width="220" show-overflow-tooltip align="left">
            <template #default="{ row }">
              <el-button link type="primary" class="spot-link" :disabled="isInvalidSpot(row)" @click="handleOpenSpot(row)">{{ getDisplaySpotName(row) }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="userNickname" label="用户" width="100" align="left" />
          <el-table-column prop="totalPrice" label="支付金额" width="136" align="left" sortable="custom" :sort-orders="TABLE_SORT_ORDERS">
            <template #default="{ row }">
              <span class="metric-inline metric-inline--price">¥{{ formatCurrency(row.totalPrice) }}</span>
              <span class="quantity">({{ row.quantity }}张)</span>
            </template>
          </el-table-column>
          <el-table-column prop="visitDate" label="游玩日期" width="132" align="center" sortable="custom" :sort-orders="TABLE_SORT_ORDERS" />
          <el-table-column label="联系人" width="100" align="left">
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
          <el-table-column prop="createdAt" label="下单时间" width="188" align="center" sortable="custom" :sort-orders="TABLE_SORT_ORDERS" />
          <el-table-column label="操作" width="220" fixed="right" align="left" header-align="center">
            <template #default="{ row }">
              <div class="table-actions table-actions--start">
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
import { TABLE_SORT_ORDERS, applySortChange } from '@/shared/composables/useTableSort.js'

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
const searchForm = reactive({ orderNo: '', spotName: '', userNickname: '', status: '' })
const dateRange = ref([])
const visitDateRange = ref([])
const loading = ref(false)
const summaryLoading = ref(false)
const detailLoading = ref(false)
const errorMessage = ref('')
const orderList = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const sortState = reactive({ page: 1, sortBy: '', sortOrder: '' })
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

// 根据当前页签 key 获取对应的状态定义。
const getTabMeta = (tabKey) => tabs.find((item) => item.key === tabKey) || tabs[0]

// 组装订单列表和摘要查询共用的基础参数。
const buildBaseParams = (page, pageSize) => {
  const params = {
    orderNo: searchForm.orderNo,
    spotName: searchForm.spotName,
    userNickname: searchForm.userNickname,
    sortBy: sortState.sortBy,
    sortOrder: sortState.sortOrder,
    page,
    pageSize
  }

  if (dateRange.value?.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  if (visitDateRange.value?.length === 2) {
    params.visitStartDate = visitDateRange.value[0]
    params.visitEndDate = visitDateRange.value[1]
  }

  return params
}

// 将订单状态映射为表格标签样式。
const getStatusTagType = (status) => {
  return {
    pending: 'warning',
    paid: 'primary',
    cancelled: 'info',
    refunded: 'danger',
    completed: 'success'
  }[status] || 'info'
}

// 统一格式化订单金额展示。
const formatCurrency = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }

  const numeric = Number(value)
  return Number.isNaN(numeric) ? String(value) : numeric.toFixed(2)
}

// 抽取接口错误提示，避免各处重复拼接兜底文案。
const extractErrorMessage = (error, fallback) => {
  return error?.response?.data?.message || error?.message || fallback
}

// 解析表格里的景点展示名称。
const getDisplaySpotName = (row) => resolveSpotDisplayName(row?.spotName)

// 判断当前景点是否仍可跳转到管理页。
const isInvalidSpot = (row) => isInvalidSpotDisplay(row?.spotName)

// “已关闭”这类复合页签需要把多个状态的结果合并后再统一排序，否则分页和排序会只对单一状态生效。
const mergeCompositeList = (responses, page, pageSize) => {
  const merged = responses
    .flatMap((item) => item.data.list || [])
    .sort(compareCompositeOrders)

  const start = (page - 1) * pageSize
  return {
    list: merged.slice(start, start + pageSize),
    total: responses.reduce((sum, item) => sum + Number(item.data.total || 0), 0)
  }
}

// 按当前排序字段比较复合状态列表中的两条订单记录。
const compareCompositeOrders = (left, right) => {
  const direction = sortState.sortOrder === 'asc' ? 1 : -1
  const field = sortState.sortBy || 'createdAt'
  const leftValue = left?.[field]
  const rightValue = right?.[field]
  if (field === 'totalPrice' || field === 'quantity' || field === 'id') {
    return direction * (Number(leftValue || 0) - Number(rightValue || 0))
  }
  return direction * String(leftValue || '').localeCompare(String(rightValue || ''))
}

// 复合状态页签通过按需扩容抓取，优先保证分页结果正确，再考虑后端是否补复合筛选接口。
const fetchCompositeOrders = async (statuses) => {
  const params = buildBaseParams(1, pagination.page * pagination.pageSize)
  const responses = await Promise.all(
    statuses.map((status) => getOrderList({ ...params, status }))
  )

  return mergeCompositeList(responses, pagination.page, pagination.pageSize)
}

// 按当前页签与筛选条件加载订单列表。
const fetchOrderList = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const currentStatuses = getTabMeta(currentTab.value).statuses

    if (currentTab.value === 'all') {
      // “全部”页签允许再叠加状态筛选，因此仍透传 searchForm.status。
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
      // 单状态页签直接走后端分页，避免前端合并带来的额外请求成本。
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

// 加载订单摘要卡片需要的各状态总数。
const fetchSummaryStats = async () => {
  summaryLoading.value = true

  try {
    // 摘要卡片只依赖 total，因此每组状态只请求 1 条数据即可，避免和主表列表抢占过多查询成本。
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

// 并行刷新订单列表和摘要卡片。
const refreshDashboardData = async () => {
  await Promise.all([fetchOrderList(), fetchSummaryStats()])
}

// 订单页既会被手工筛选，也会被外部 query 跳转命中，这里统一把本地筛选状态折叠回路由，保证分享与返回行为一致。
const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.orderNo) {
    nextQuery.orderNo = searchForm.orderNo
  }
  if (searchForm.spotName) {
    nextQuery.spotName = searchForm.spotName
  }
  if (searchForm.userNickname) {
    nextQuery.userNickname = searchForm.userNickname
  }
  if (searchForm.status) {
    nextQuery.status = searchForm.status
  }
  if (dateRange.value?.length === 2) {
    nextQuery.startDate = dateRange.value[0]
    nextQuery.endDate = dateRange.value[1]
  }
  if (visitDateRange.value?.length === 2) {
    nextQuery.visitStartDate = visitDateRange.value[0]
    nextQuery.visitEndDate = visitDateRange.value[1]
  }
  const currentQuery = {}
  if (typeof route.query.orderNo === 'string' && route.query.orderNo) {
    currentQuery.orderNo = route.query.orderNo
  }
  if (typeof route.query.spotName === 'string' && route.query.spotName) {
    currentQuery.spotName = route.query.spotName
  }
  if (typeof route.query.userNickname === 'string' && route.query.userNickname) {
    currentQuery.userNickname = route.query.userNickname
  }
  if (typeof route.query.status === 'string' && route.query.status) {
    currentQuery.status = route.query.status
  }
  if (typeof route.query.startDate === 'string' && route.query.startDate) {
    currentQuery.startDate = route.query.startDate
  }
  if (typeof route.query.endDate === 'string' && route.query.endDate) {
    currentQuery.endDate = route.query.endDate
  }
  if (typeof route.query.visitStartDate === 'string' && route.query.visitStartDate) {
    currentQuery.visitStartDate = route.query.visitStartDate
  }
  if (typeof route.query.visitEndDate === 'string' && route.query.visitEndDate) {
    currentQuery.visitEndDate = route.query.visitEndDate
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
  searchForm.userNickname = typeof route.query.userNickname === 'string' ? route.query.userNickname : ''
  searchForm.status = typeof route.query.status === 'string' ? route.query.status : ''
  if (typeof route.query.startDate === 'string' && typeof route.query.endDate === 'string') {
    dateRange.value = [route.query.startDate, route.query.endDate]
    showAdvanced.value = true
  } else {
    dateRange.value = []
  }
  if (typeof route.query.visitStartDate === 'string' && typeof route.query.visitEndDate === 'string') {
    visitDateRange.value = [route.query.visitStartDate, route.query.visitEndDate]
    showAdvanced.value = true
  } else {
    visitDateRange.value = []
  }
}

// 按当前筛选条件重新拉取订单和摘要数据。
const handleSearch = () => {
  pagination.page = 1
  syncRouteQuery()
  refreshDashboardData()
}

// 清空筛选项并恢复第一页数据。
const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.spotName = ''
  searchForm.userNickname = ''
  searchForm.status = ''
  dateRange.value = []
  visitDateRange.value = []
  pagination.page = 1
  syncRouteQuery()
  refreshDashboardData()
}

// 手动刷新当前页签数据和摘要卡片。
const handleRefresh = () => {
  refreshDashboardData()
}

// 切换订单页签后按新状态重新加载列表。
const handleTabChange = (tabKey) => {
  currentTab.value = tabKey
  searchForm.status = ''
  pagination.page = 1
  syncRouteQuery()
  fetchOrderList()
}

// 同步表格排序并重新查询订单列表。
const handleSortChange = (sortPayload) => {
  applySortChange(sortState, sortPayload)
  pagination.page = 1
  fetchOrderList()
}

// 切换分页页码。
const handlePageChange = (page) => {
  pagination.page = page
  fetchOrderList()
}

// 切换分页大小后回到第一页。
const handlePageSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchOrderList()
}

// 从订单列表跳转到对应景点的管理视图。
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

// 打开订单详情抽屉并补拉完整详情数据。
const handleDetail = async (row) => {
  detailLoading.value = true
  detailVisible.value = true

  try {
    // 列表行只保留摘要字段，抽屉打开后再补拉详情，避免主表列表为低频字段付出常驻成本。
    const res = await getOrderDetail(row.id)
    currentOrder.value = res.data
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(extractErrorMessage(error, '获取订单详情失败'))
  } finally {
    detailLoading.value = false
  }
}

// 各类订单动作共用同一套确认、刷新和详情同步逻辑，避免状态流转分散到多个分支后难以保持一致。
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

// 监听外部 query 变化，同步筛选状态并刷新列表。
watch(() => route.query, () => {
  // 由当前页面主动 replace 的 query 不再触发二次拉取，避免一次筛选造成两轮重复请求。
  if (skipNextRouteLoad.value) {
    skipNextRouteLoad.value = false
    return
  }
  applyRouteQuery()
  pagination.page = 1
  refreshDashboardData()
})

// 页面挂载后按路由参数初始化筛选条件。
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

.table-actions--start {
  justify-content: flex-start;
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
