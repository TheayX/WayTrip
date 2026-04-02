<!-- 订单管理页面 -->
<template>
  <div class="order-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
        </div>
      </template>

      <!-- 搜索筛选表单 -->
      <el-form :model="searchForm" inline class="search-form" @submit.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="searchForm.orderNo"
            placeholder="请输入订单号"
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
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px" @change="handleSearch" @clear="handleSearch">
            <el-option label="待支付" value="pending" />
            <el-option label="已支付" value="paid" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="已退款" value="refunded" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
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

      <!-- 订单列表 -->
      <el-table :data="orderList" v-loading="loading" class="borderless-table">
        <el-table-column prop="orderNo" label="订单号" width="180" align="left" />
        <el-table-column label="景点名称" min-width="180" show-overflow-tooltip align="left">
          <template #default="{ row }">
            <el-button link type="primary" class="spot-link" @click="handleOpenSpot(row)">{{ row.spotName }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="userNickname" label="用户" width="120" align="left" />
        <el-table-column label="支付金额" width="130" align="left">
          <template #default="{ row }">
            <span class="price">¥{{ row.totalPrice }}</span>
            <span class="quantity">({{ row.quantity }}张)</span>
          </template>
        </el-table-column>
        <el-table-column prop="visitDate" label="游玩日期" width="110" align="center" />
        <el-table-column label="联系人" width="150" align="left">
          <template #default="{ row }">
            <div>{{ row.contactName }}</div>
            <div class="text-gray">{{ row.contactPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <div class="capsule-badge status-capsule" :class="'status-' + (getStatusType(row.status) === 'info' ? 'neutral' : getStatusType(row.status))">
              <span class="dot"></span>
              {{ row.statusText }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="160" align="center" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
              
              <!-- 根据状态显示不同动作 -->
              <template v-if="row.status === 'paid'">
                <el-button type="success" link @click="handleComplete(row)">完成</el-button>
                <el-button type="danger" link @click="handleRefund(row)">退款</el-button>
              </template>
              <template v-if="row.status === 'pending'">
                <el-button type="danger" link @click="handleCancel(row)">取消</el-button>
              </template>
              <template v-if="row.status === 'completed'">
                <el-button type="warning" link @click="handleReopen(row)">撤销完成</el-button>
              </template>
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
          @size-change="fetchOrderList"
          @current-change="fetchOrderList"
        />
      </div>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="620px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="景点名称" :span="2">{{ currentOrder.spotName }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ currentOrder.unitPrice }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentOrder.quantity }}张</el-descriptions-item>
        <el-descriptions-item label="总金额" :span="2">
          <span class="price-large">¥{{ currentOrder.totalPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="游玩日期">{{ currentOrder.visitDate }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(currentOrder.status)">{{ currentOrder.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentOrder.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentOrder.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">{{ currentOrder.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="修改时间" :span="2">{{ currentOrder.updatedAt }}</el-descriptions-item>
        <el-descriptions-item v-if="currentOrder.paidAt" label="支付时间" :span="2">{{ currentOrder.paidAt }}</el-descriptions-item>
        <el-descriptions-item v-if="currentOrder.completedAt" label="完成时间" :span="2">{{ currentOrder.completedAt }}</el-descriptions-item>
        <el-descriptions-item v-if="currentOrder.cancelledAt" label="取消时间" :span="2">{{ currentOrder.cancelledAt }}</el-descriptions-item>
        <el-descriptions-item v-if="currentOrder.refundedAt" label="退款时间" :span="2">{{ currentOrder.refundedAt }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentOrder?.status === 'paid'" type="success" @click="handleComplete(currentOrder)">完成订单</el-button>
        <el-button v-if="currentOrder?.status === 'paid'" type="danger" @click="handleRefund(currentOrder)">退款订单</el-button>
        <el-button v-if="currentOrder?.status === 'pending'" type="danger" @click="handleCancel(currentOrder)">取消订单</el-button>
        <el-button v-if="currentOrder?.status === 'completed'" type="warning" @click="handleReopen(currentOrder)">撤销完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, getOrderDetail, completeOrder, refundOrder, reopenOrder, cancelOrder } from '@/modules/order/api.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'

const router = useRouter()
const searchForm = reactive({ orderNo: '', spotName: '', status: '' })
const dateRange = ref([])
const loading = ref(false)
const orderList = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const detailVisible = ref(false)
const currentOrder = ref(null)

const fetchOrderList = async () => {
  loading.value = true
  try {
    const params = { ...searchForm, page: pagination.page, pageSize: pagination.pageSize }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getOrderList(params)
    orderList.value = res.data.list
    pagination.total = res.data.total
  } catch (e) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchOrderList() }
const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.spotName = ''
  searchForm.status = ''
  dateRange.value = []
  handleSearch()
}

const getStatusType = (status) => {
  return { pending: 'warning', paid: 'primary', cancelled: 'info', refunded: 'danger', completed: 'success' }[status] || 'info'
}

const handleAction = (command, row) => {
  const map = { complete: handleComplete, refund: handleRefund, cancel: handleCancel, reopen: handleReopen }
  map[command]?.(row)
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
  try {
    const res = await getOrderDetail(row.id)
    currentOrder.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  }
}

const handleComplete = async (row) => {
  try {
    await ElMessageBox.confirm('确认将此订单标记为已完成？', '提示', { type: 'warning' })
    await completeOrder(row.id)
    ElMessage.success('订单已完成')
    detailVisible.value = false
    fetchOrderList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) ElMessage.error('操作失败')
  }
}

const handleRefund = async (row) => {
  try {
    await ElMessageBox.confirm('确认将此订单标记为已退款？', '提示', { type: 'warning' })
    await refundOrder(row.id)
    ElMessage.success('订单已退款')
    detailVisible.value = false
    fetchOrderList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) ElMessage.error('操作失败')
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确认取消此未支付订单？', '提示', { type: 'warning' })
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    detailVisible.value = false
    fetchOrderList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) ElMessage.error('操作失败')
  }
}

const handleReopen = async (row) => {
  try {
    await ElMessageBox.confirm('确认撤销此订单的完成状态？', '提示', { type: 'warning' })
    await reopenOrder(row.id)
    ElMessage.success('订单已撤销完成')
    detailVisible.value = false
    fetchOrderList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) ElMessage.error('操作失败')
  }
}

onMounted(() => { fetchOrderList() })
</script>

<style lang="scss" scoped>
.order-page {
  .price { color: #ef4444; font-weight: 700; font-size: 14px; }
  .quantity { color: #94a3b8; font-size: 12px; margin-left: 6px; }
  .text-gray { color: #94a3b8; font-size: 12px; margin-top: 2px; }
  .price-large { color: #ef4444; font-size: 20px; font-weight: 700; }

  .table-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    justify-content: flex-start;
  }
}

.spot-link {
  padding: 0;
  min-width: 0;
  height: auto;
  font-weight: 600;
  color: #1e293b;
}

:deep(.spot-link.el-button.is-link) {
  padding-left: 0;
  padding-right: 0;
}

:deep(.s-success) { color: #10b981 !important; }
:deep(.s-danger)  { color: #ef4444 !important; }
</style>
