<template>
  <el-table
    :data="tableData"
    v-loading="loading"
    class="premium-table borderless-table"
    :row-class-name="getRowClassName"
    @selection-change="handleSelectionChange"
  >
    <el-table-column type="selection" width="50" align="center" />
    <el-table-column prop="id" label="ID" width="70" align="center" />
    <el-table-column label="封面" width="90" align="center">
      <template #default="{ row }">
        <el-image :src="getImageUrl(row.coverImage)" class="cover-img" fit="cover" />
      </template>
    </el-table-column>
    <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip align="left">
      <template #default="{ row }">
        <el-button link type="primary" class="guide-title-link" @click="emit('view', row)">{{ row.title }}</el-button>
      </template>
    </el-table-column>
    <el-table-column prop="category" label="分类" width="120" align="center">
      <template #default="{ row }">
        <el-tag effect="plain" type="info" size="small">{{ row.category || '未分类' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="viewCount" label="浏览量" width="100" align="center">
      <template #default="{ row }">
        <span class="count-text">{{ row.viewCount ?? 0 }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }">
        <div class="capsule-badge status-capsule" :class="row.published ? 'status-success' : 'status-neutral'">
          <span class="dot"></span>
          {{ row.published ? '已发布' : '未发布' }}
        </div>
      </template>
    </el-table-column>
    <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
      <template #default="{ row }">
        {{ formatDate(row.createdAt) }}
      </template>
    </el-table-column>
    <el-table-column prop="updatedAt" label="修改时间" width="180" align="center">
      <template #default="{ row }">
        {{ formatDate(row.updatedAt) }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="160" fixed="right" align="center">
      <template #default="{ row }">
        <div class="table-actions">
          <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>

          <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
            <el-button link type="primary">
              更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                <el-dropdown-item command="toggle-publish" :class="row.published ? 'danger-text' : 'success-text'">
                  {{ row.published ? '下架攻略' : '发布攻略' }}
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided class="danger-text">删除攻略</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'

defineProps({
  tableData: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  getImageUrl: { type: Function, required: true },
  formatDate: { type: Function, required: true },
  getRowClassName: { type: Function, required: true }
})

const emit = defineEmits(['selection-change', 'view', 'edit', 'toggle-publish', 'delete'])

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

const handleCommand = (command, row) => {
  switch (command) {
    case 'detail':
      emit('view', row)
      break
    case 'toggle-publish':
      emit('toggle-publish', row)
      break
    case 'delete':
      emit('delete', row)
      break
  }
}
</script>

<style lang="scss" scoped>
.cover-img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.guide-title-link {
  padding: 0;
  min-width: 0;
  height: auto;
  font-weight: 600;
  color: #1e293b;

  &:hover {
    color: var(--el-color-primary);
  }
}

:deep(.guide-title-link.el-button.is-link) {
  padding-left: 0;
  padding-right: 0;
}

:deep(.guide-title-link .el-button__text) {
  display: inline-block;
  text-align: left;
}

.count-text {
  font-weight: 600;
  color: #1e293b;
  font-variant-numeric: tabular-nums;
}

.borderless-table {
  :deep(.el-table__inner-wrapper::before) {
    display: none;
  }

  :deep(td.el-table__cell),
  :deep(th.el-table__cell.is-leaf) {
    border-bottom: 1px solid #f8fafc;
  }

  :deep(.el-table__row) {
    transition: all 0.2s ease;

    td {
      padding: 12px 0;
    }
  }

  :deep(.el-table__row:hover > td.el-table__cell) {
    background: linear-gradient(90deg, rgba(248, 250, 252, 0.5) 0%, #f1f5f9 50%, rgba(248, 250, 252, 0.5) 100%) !important;
  }
}

.capsule-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;

  &.status-capsule {
    gap: 6px;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
    }
  }

  &.status-success {
    background-color: #ecfdf5;
    color: #059669;

    .dot {
      background-color: #10b981;
    }
  }

  &.status-neutral {
    background-color: #f1f5f9;
    color: #475569;

    .dot {
      background-color: #94a3b8;
    }
  }
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.danger-text {
  color: #ef4444 !important;
}

.success-text {
  color: #10b981 !important;
}
</style>
