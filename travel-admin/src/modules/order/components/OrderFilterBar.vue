<template>
  <div class="filter-panel">
    <el-form :model="searchForm" @submit.prevent>
      <div class="filter-caption">
        <span class="filter-title">筛选订单</span>
        <span class="filter-subtitle">按订单号、景点名、状态和下单时间快速收敛当前工作区里的订单。</span>
      </div>
      <div class="filter-row">
        <div class="filter-main">
          <el-form-item class="filter-item">
            <el-input
              v-model="searchForm.orderNo"
              placeholder="搜索订单号"
              clearable
              class="filter-input"
              @keyup.enter="emit('search')"
              @clear="emit('search')"
            />
          </el-form-item>
          <el-form-item class="filter-item">
            <el-input
              v-model="searchForm.spotName"
              placeholder="搜索景点名称"
              clearable
              class="filter-input"
              @keyup.enter="emit('search')"
              @clear="emit('search')"
            />
          </el-form-item>
          <el-form-item class="filter-item">
            <el-select
              v-model="searchForm.status"
              placeholder="全部状态"
              clearable
              class="filter-select"
              :disabled="currentTab !== 'all'"
              @change="emit('search')"
              @clear="emit('search')"
            >
              <el-option label="待支付" value="pending" />
              <el-option label="已支付" value="paid" />
              <el-option label="已取消" value="cancelled" />
              <el-option label="已退款" value="refunded" />
              <el-option label="已完成" value="completed" />
            </el-select>
          </el-form-item>
          <el-button type="primary" link class="toggle-btn" @click="emit('toggle-advanced')">
            {{ showAdvanced ? '收起条件' : '更多条件' }}
          </el-button>
        </div>

        <div class="filter-actions">
          <el-button type="primary" @click="emit('search')">查询</el-button>
          <el-button @click="emit('reset')">重置</el-button>
        </div>
      </div>

      <el-alert
        v-if="currentTab !== 'all'"
        type="info"
        :closable="false"
        class="scope-alert"
        :title="`${tabLabel}工作区已限定订单状态，需精确筛选时请切回“全部”。`"
      />

      <!-- 低频条件默认折叠，避免页面顶部持续过厚 -->
      <el-collapse-transition>
        <div v-show="showAdvanced" class="advanced-panel">
          <el-form-item label="下单时间" class="filter-item">
            <el-date-picker
              :model-value="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="date-picker"
              @update:model-value="handleDateChange"
              @change="emit('search')"
            />
          </el-form-item>
        </div>
      </el-collapse-transition>
    </el-form>
  </div>
</template>

<script setup>
defineProps({
  currentTab: { type: String, required: true },
  tabLabel: { type: String, required: true },
  searchForm: { type: Object, required: true },
  dateRange: { type: Array, required: true },
  showAdvanced: { type: Boolean, default: false }
})

const emit = defineEmits(['search', 'reset', 'toggle-advanced', 'update:date-range'])

const handleDateChange = (value) => {
  emit('update:date-range', value || [])
}
</script>

<style lang="scss" scoped>
.filter-panel {
  margin-top: 16px;
  margin-bottom: 20px;
  border: 1px solid var(--wt-border-default);
  border-radius: 18px;
  background: linear-gradient(180deg, var(--wt-surface-muted) 0%, var(--wt-surface-elevated) 100%);
  padding: 16px 18px;
  box-shadow: var(--wt-shadow-soft);
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
  color: var(--wt-text-primary);
}

.filter-subtitle {
  font-size: 12px;
  line-height: 1.6;
  color: var(--wt-text-regular);
}

.filter-row,
.filter-main,
.filter-actions {
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

.filter-actions {
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
  border-top: 1px solid var(--wt-border-default);
}

.date-picker {
  width: 260px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-date-editor.el-input__wrapper) {
  border-radius: 10px;
  background: var(--wt-surface-elevated);
  box-shadow: var(--wt-shadow-soft);
}

@media (max-width: 960px) {
  .filter-row {
    flex-direction: column;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-actions :deep(.el-button) {
    width: 100%;
  }

  .filter-input,
  .filter-select,
  .date-picker {
    width: 100%;
  }
}
</style>
