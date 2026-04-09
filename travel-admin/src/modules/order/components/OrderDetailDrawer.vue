<!-- 订单详情抽屉 -->
<template>
  <el-drawer
    :model-value="visible"
    title="订单详情"
    size="520px"
    class="order-detail-drawer"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading" class="detail-container">
      <template v-if="detail">
        <section class="detail-hero">
          <div class="detail-header">
            <div>
              <p class="detail-order-no">{{ detail.orderNo }}</p>
              <h3 class="detail-spot-name">{{ detail.spotName }}</h3>
            </div>
            <el-tag effect="light" round :type="getStatusTagType(detail.status)">
              {{ detail.statusText }}
            </el-tag>
          </div>

          <el-image
            v-if="detail.spotImage"
            :src="detail.spotImage"
            fit="cover"
            class="detail-cover"
          />

          <div class="detail-stats">
            <div class="detail-stat-card">
              <span class="detail-stat-label">总金额</span>
              <strong class="price-large">¥{{ formatCurrency(detail.totalPrice) }}</strong>
            </div>
            <div class="detail-stat-card">
              <span class="detail-stat-label">游玩日期</span>
              <strong>{{ detail.visitDate || '--' }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <h4 class="section-title">订单基础信息</h4>
          <el-descriptions :column="1" border class="detail-descriptions">
            <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ detail.createdAt || '--' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ detail.updatedAt || '--' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <h4 class="section-title">景点与游玩信息</h4>
          <el-descriptions :column="1" border class="detail-descriptions">
            <el-descriptions-item label="景点名称">{{ detail.spotName || '--' }}</el-descriptions-item>
            <el-descriptions-item label="单价">¥{{ formatCurrency(detail.unitPrice) }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ detail.quantity || '--' }} 张</el-descriptions-item>
            <el-descriptions-item label="总价">¥{{ formatCurrency(detail.totalPrice) }}</el-descriptions-item>
            <el-descriptions-item label="游玩日期">{{ detail.visitDate || '--' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <h4 class="section-title">联系人信息</h4>
          <el-descriptions :column="1" border class="detail-descriptions">
            <el-descriptions-item label="联系人">{{ detail.contactName || '--' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.contactPhone || '--' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <h4 class="section-title">关键时间节点</h4>
          <el-timeline class="detail-timeline">
            <el-timeline-item timestamp="下单时间" :hollow="!detail.createdAt">
              {{ detail.createdAt || '--' }}
            </el-timeline-item>
            <el-timeline-item timestamp="支付时间" :hollow="!detail.paidAt">
              {{ detail.paidAt || '--' }}
            </el-timeline-item>
            <el-timeline-item timestamp="完成时间" :hollow="!detail.completedAt">
              {{ detail.completedAt || '--' }}
            </el-timeline-item>
            <el-timeline-item timestamp="取消时间" :hollow="!detail.cancelledAt">
              {{ detail.cancelledAt || '--' }}
            </el-timeline-item>
            <el-timeline-item timestamp="退款时间" :hollow="!detail.refundedAt">
              {{ detail.refundedAt || '--' }}
            </el-timeline-item>
          </el-timeline>
        </section>
      </template>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="emit('update:visible', false)">关闭</el-button>
        <el-button v-if="detail?.status === 'pending'" type="danger" @click="emit('cancel', detail)">取消订单</el-button>
        <el-button v-if="detail?.status === 'paid'" type="success" @click="emit('complete', detail)">完成订单</el-button>
        <el-button v-if="detail?.status === 'paid'" type="danger" plain @click="emit('refund', detail)">退款订单</el-button>
        <el-button v-if="detail?.status === 'completed'" type="warning" @click="emit('reopen', detail)">撤销完成</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
// 详情抽屉完全复用父层传入的格式化函数，避免状态文案在多个组件里分散定义。
defineProps({
  visible: { type: Boolean, required: true },
  loading: { type: Boolean, default: false },
  detail: { type: Object, default: null },
  formatCurrency: { type: Function, required: true },
  getStatusTagType: { type: Function, required: true }
})

// 操作按钮只发出订单动作意图，真正的接口调用与二次确认仍由父层掌控。
const emit = defineEmits(['update:visible', 'complete', 'refund', 'cancel', 'reopen'])
</script>

<style lang="scss" scoped>
.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-header,
.detail-stats,
.drawer-footer {
  display: flex;
  gap: 12px;
}

.detail-hero {
  border-radius: 18px;
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
  padding: 18px;
}

.detail-order-no {
  margin: 0 0 6px;
  color: var(--wt-text-regular);
  font-size: 13px;
}

.detail-spot-name {
  margin: 0;
  color: var(--wt-text-primary);
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
  background: var(--wt-surface-elevated);
  padding: 14px 16px;
  border: 1px solid var(--wt-border-default);
}

.detail-stat-label {
  display: block;
  color: var(--wt-text-regular);
  font-size: 12px;
  margin-bottom: 6px;
}

.price-large {
  color: #dc2626;
  font-size: 24px;
  font-weight: 700;
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
  color: var(--wt-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.detail-timeline {
  margin: 4px 0 0;
}

.drawer-footer {
  width: 100%;
  align-items: center;
  justify-content: flex-end;
}

:deep(.detail-descriptions .el-descriptions__label) {
  width: 100px;
  background: var(--el-table-header-bg-color) !important;
  color: var(--wt-text-regular);
  font-weight: 500;
}

:deep(.detail-descriptions .el-descriptions__cell) {
  padding: 12px 16px !important;
}

@media (max-width: 960px) {
  .detail-stats {
    flex-direction: column;
  }
}
</style>
