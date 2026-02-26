<template>
  <div class="page-container" v-if="order">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
      <el-breadcrumb-item>订单详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="detail-layout">
      <div class="detail-main">
        <!-- 订单状态 -->
        <div class="status-card card" :class="order.status">
          <div class="status-info">
            <span class="status-icon">{{ getStatusIcon(order.status) }}</span>
            <div>
              <h2 class="status-text">{{ order.statusText }}</h2>
              <p class="status-desc">{{ getStatusDesc(order.status) }}</p>
            </div>
          </div>
        </div>

        <!-- 景点信息 -->
        <div class="info-card card" @click="$router.push(`/spots/${order.spotId}`)">
          <h3 class="card-title">景点信息</h3>
          <div class="spot-row">
            <img :src="getImageUrl(order.spotImage)" class="spot-thumb" alt="" />
            <div class="spot-info">
              <span class="spot-name">{{ order.spotName }}</span>
              <span class="spot-date">游玩日期：{{ order.visitDate }}</span>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <!-- 订单信息 -->
        <div class="info-card card">
          <h3 class="card-title">订单信息</h3>
          <div class="info-row">
            <span class="label">订单编号</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">下单时间</span>
            <span class="value">{{ order.createdAt }}</span>
          </div>
          <div class="info-row" v-if="order.paidAt">
            <span class="label">支付时间</span>
            <span class="value">{{ order.paidAt }}</span>
          </div>
        </div>

        <!-- 联系人信息 -->
        <div class="info-card card">
          <h3 class="card-title">联系人信息</h3>
          <div class="info-row">
            <span class="label">联系人</span>
            <span class="value">{{ order.contactName }}</span>
          </div>
          <div class="info-row">
            <span class="label">手机号</span>
            <span class="value">{{ order.contactPhone }}</span>
          </div>
        </div>

        <!-- 价格明细 -->
        <div class="info-card card">
          <h3 class="card-title">价格明细</h3>
          <div class="info-row">
            <span class="label">门票单价</span>
            <span class="value">¥{{ order.unitPrice }}</span>
          </div>
          <div class="info-row">
            <span class="label">购买数量</span>
            <span class="value">x{{ order.quantity }}</span>
          </div>
          <el-divider />
          <div class="info-row total">
            <span class="label">实付金额</span>
            <span class="total-price">¥{{ order.totalPrice }}</span>
          </div>
        </div>
      </div>

      <!-- 侧边操作 -->
      <div class="detail-sidebar">
        <div class="sidebar-card card" v-if="order.canPay || order.canCancel">
          <h3 class="card-title">操作</h3>
          <el-button
            v-if="order.canPay"
            type="primary"
            size="large"
            class="action-btn"
            @click="handlePay"
          >立即支付</el-button>
          <el-button
            v-if="order.canCancel"
            size="large"
            class="action-btn"
            @click="handleCancel"
          >{{ order.status === 'paid' ? '申请退款' : '取消订单' }}</el-button>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="page-container">
    <el-skeleton :rows="10" animated />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const order = ref(null)

const getStatusIcon = (status) => {
  const map = { pending: '⏳', paid: '✅', completed: '🎉', cancelled: '❌' }
  return map[status] || '📋'
}

const getStatusDesc = (status) => {
  const map = {
    pending: '请尽快完成支付',
    paid: '已支付成功，祝您旅途愉快',
    completed: '订单已完成，期待再次相遇',
    cancelled: '订单已取消'
  }
  return map[status] || ''
}

const fetchDetail = async () => {
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  }
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm('确定要支付该订单吗？', '支付确认', { type: 'info' })
    const idempotentKey = `${order.value.id}-${Date.now()}`
    await payOrder(order.value.id, idempotentKey)
    ElMessage.success('支付成功')
    fetchDetail()
  } catch (e) { /* ignore */ }
}

const handleCancel = async () => {
  try {
    const msg = order.value.status === 'paid' ? '确定要申请退款吗？' : '确定要取消订单吗？'
    await ElMessageBox.confirm(msg, '提示', { type: 'warning' })
    await cancelOrder(order.value.id)
    ElMessage.success('操作成功')
    fetchDetail()
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style lang="scss" scoped>
.detail-layout {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.detail-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.status-card {
  padding: 24px;
  border-radius: 12px;

  &.pending { background: linear-gradient(135deg, #fff7e6, #fff1cc); }
  &.paid { background: linear-gradient(135deg, #e6f7e6, #c8f0c8); }
  &.completed { background: linear-gradient(135deg, #e6f0ff, #ccdeff); }
  &.cancelled { background: linear-gradient(135deg, #ffe6e6, #ffd6d6); }
}

.status-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-icon {
  font-size: 40px;
}

.status-text {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.status-desc {
  font-size: 14px;
  color: #606266;
}

.info-card {
  padding: 20px;
  border-radius: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.spot-row {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.spot-thumb {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.spot-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spot-name {
  font-weight: 600;
}

.spot-date {
  font-size: 13px;
  color: #909399;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;

  .label { color: #909399; }
  .value { color: #303133; }

  &.total {
    font-weight: 600;
    font-size: 16px;
  }
}

.total-price {
  font-size: 22px;
  color: #f56c6c;
  font-weight: 700;
}

.sidebar-card {
  padding: 20px;
  border-radius: 12px;
  position: sticky;
  top: 80px;
}

.action-btn {
  width: 100%;
  border-radius: 8px;
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }
}

@media (max-width: 992px) {
  .detail-layout {
    flex-direction: column;
  }

  .detail-sidebar {
    width: 100%;
  }
}
</style>

