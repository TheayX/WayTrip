<template>
  <div class="order-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="景点名称">
          <el-input v-model="searchForm.spotName" placeholder="请输入景点名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
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
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 订单列表 -->
      <el-table :data="orderList" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="spotName" label="景点名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="userNickname" label="用户" width="120" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ row.totalPrice }}</span>
            <span class="quantity">({{ row.quantity }}张)</span>
          </template>
        </el-table-column>
        <el-table-column prop="visitDate" label="游玩日期" width="110" />
        <el-table-column label="联系人" width="150">
          <template #default="{ row }">
            <div>{{ row.contactName }}</div>
            <div class="text-gray">{{ row.contactPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'paid'"
              type="success"
              link
              @click="handleComplete(row)"
            >完成</el-button>
            <el-button
              v-if="row.status === 'paid'"
              type="danger"
              link
              @click="handleRefund(row)"
            >退款</el-button>
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
          @size-change="fetchOrderList"
          @current-change="fetchOrderList"
        />
      </div>
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
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
        <el-descriptions-item label="支付时间" v-if="currentOrder.paidAt" :span="2">{{ currentOrder.paidAt }}</el-descriptions-item>
        <el-descriptions-item label="完成时间" v-if="currentOrder.completedAt" :span="2">{{ currentOrder.completedAt }}</el-descriptions-item>
        <el-descriptions-item label="取消时间" v-if="currentOrder.cancelledAt" :span="2">{{ currentOrder.cancelledAt }}</el-descriptions-item>
        <el-descriptions-item label="退款时间" v-if="currentOrder.refundedAt" :span="2">{{ currentOrder.refundedAt }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentOrder?.status === 'paid'"
          type="primary"
          @click="handleComplete(currentOrder)"
        >完成订单</el-button>
        <el-button
          v-if="currentOrder?.status === 'paid'"
          type="danger"
          @click="handleRefund(currentOrder)"
        >退款订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, getOrderDetail, completeOrder, refundOrder } from '@/api/order'

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  spotName: '',
  status: ''
})
const dateRange = ref([])

// 列表数据
const loading = ref(false)
const orderList = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 详情弹窗
const detailVisible = ref(false)
const currentOrder = ref(null)

// 获取订单列表
const fetchOrderList = async () => {
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
    const res = await getOrderList(params)
    orderList.value = res.data.list
    pagination.total = res.data.total
  } catch (e) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchOrderList()
}

// 重置
const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.spotName = ''
  searchForm.status = ''
  dateRange.value = []
  handleSearch()
}

// 获取状态标签类型
const getStatusType = (status) => {
  const types = {
    pending: 'warning',
    paid: 'primary',
    cancelled: 'info',
    refunded: 'danger',
    completed: 'success'
  }
  return types[status] || 'info'
}

// 查看详情
const handleDetail = async (row) => {
  try {
    const res = await getOrderDetail(row.id)
    currentOrder.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  }
}

// 完成订单
const handleComplete = async (row) => {
  try {
    await ElMessageBox.confirm('确认将此订单标记为已完成？', '提示', {
      type: 'warning'
    })
    await completeOrder(row.id)
    ElMessage.success('订单已完成')
    detailVisible.value = false
    fetchOrderList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 退款订单
const handleRefund = async (row) => {
  try {
    await ElMessageBox.confirm('确认将此订单标记为已退款？', '提示', {
      type: 'warning'
    })
    await refundOrder(row.id)
    ElMessage.success('订单已退款')
    detailVisible.value = false
    fetchOrderList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  fetchOrderList()
})
</script>

<style lang="scss" scoped>
.order-page {
  .search-form {
    margin-bottom: 20px;
  }

  .price {
    color: #f56c6c;
    font-weight: bold;
  }

  .quantity {
    color: #909399;
    font-size: 12px;
    margin-left: 4px;
  }

  .text-gray {
    color: #909399;
    font-size: 12px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .price-large {
    color: #f56c6c;
    font-size: 18px;
    font-weight: bold;
  }
}
</style>
