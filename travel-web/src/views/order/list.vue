<template>
  <div class="page-container">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>我的订单</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 状态筛选 -->
    <el-tabs v-model="currentTab" @tab-change="handleTabChange" class="order-tabs">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <!-- 订单列表 -->
    <div v-loading="loading" class="order-list">
      <div v-for="order in orderList" :key="order.id" class="order-card card" @click="$router.push(`/orders/${order.id}`)">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small">{{ order.statusText }}</el-tag>
        </div>
        <div class="order-body">
          <img :src="getImageUrl(order.spotImage)" class="order-img" alt="" />
          <div class="order-info">
            <h3 class="order-spot-name">{{ order.spotName }}</h3>
            <p class="order-date">游玩日期：{{ order.visitDate }}</p>
            <div class="order-price-row">
              <span class="price">¥{{ order.totalPrice }}</span>
              <span class="order-qty">x{{ order.quantity }}</span>
            </div>
          </div>
        </div>
        <div class="order-actions" v-if="order.status === 'pending' || order.status === 'paid'">
          <el-button
            v-if="order.canCancel"
            size="small"
            @click.stop="handleCancel(order)"
          >{{ order.status === 'paid' ? '申请退款' : '取消订单' }}</el-button>
          <el-button
            v-if="order.status === 'pending'"
            type="primary"
            size="small"
            @click.stop="handlePay(order)"
          >去支付</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && orderList.length === 0" description="暂无订单" />

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderList, cancelOrder, payOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'pending' },
  { label: '已支付', value: 'paid' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const currentTab = ref('')
const orderList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const statusType = (status) => {
  const map = { pending: 'warning', paid: 'success', completed: 'info', cancelled: 'danger' }
  return map[status] || 'info'
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    if (currentTab.value) params.status = currentTab.value
    const res = await getOrderList(params)
    orderList.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  loading.value = false
}

const handleTabChange = () => {
  page.value = 1
  fetchOrders()
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm(
      order.status === 'paid' ? '确定要申请退款吗？' : '确定要取消订单吗？',
      '提示',
      { type: 'warning' }
    )
    await cancelOrder(order.id)
    ElMessage.success('操作成功')
    fetchOrders()
  } catch (e) { /* ignore */ }
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm('确定要支付该订单吗？', '支付确认', { type: 'info' })
    const idempotentKey = `${order.id}-${Date.now()}`
    await payOrder(order.id, idempotentKey)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.order-tabs {
  margin-bottom: 8px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.order-card {
  padding: 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-no {
  font-size: 13px;
  color: #909399;
}

.order-body {
  display: flex;
  gap: 16px;
}

.order-img {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.order-info {
  flex: 1;
}

.order-spot-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.order-date {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.order-price-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-qty {
  font-size: 13px;
  color: #c0c4cc;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>

