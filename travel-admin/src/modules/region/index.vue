<!-- 地区管理页面 -->
<template>
  <div class="region-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">地域结构管理</p>
        <h1 class="page-title">地区管理</h1>
        <p class="page-subtitle">维护前后台统一使用的地区层级。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading1 || loading2" @click="handleRefresh">刷新数据</el-button>
      </div>
    </section>

    <section class="summary-grid metric-cards--order">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">一级地区</div>
        <div class="summary-value">{{ level1List.length }}</div>
        <div class="summary-desc">当前已维护的一级地区数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">当前子地区</div>
        <div class="summary-value">{{ level2List.length }}</div>
        <div class="summary-desc">当前选中一级地区下的二级地区数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">当前工作区</div>
        <div class="summary-value summary-value--sm">{{ activeParentName }}</div>
        <div class="summary-desc">未选择时默认展示第一项或空状态</div>
      </el-card>
    </section>

    <el-row :gutter="20">
      <!-- 左侧：一级地区 -->
      <el-col :span="6">
        <el-card shadow="hover" class="left-card">
          <template #header>
            <div class="card-header">
              <span>一级地区</span>
              <el-button type="primary" link icon="Plus" @click="handleAddLevel1">新增</el-button>
            </div>
          </template>
          
          <div v-if="errorMessage" class="panel-error page-error-state">
            <el-result icon="error" title="地区管理加载失败" :sub-title="errorMessage">
              <template #extra>
                <el-button type="primary" @click="handleRefresh">重新加载</el-button>
              </template>
            </el-result>
          </div>

          <ul v-else class="parent-list" v-loading="loading1" element-loading-text="正在加载地区数据...">
            <li 
              v-for="item in level1List" 
              :key="item.id" 
              :class="['list-item', { active: activeParentId === item.id }]"
              @click="handleSelectParent(item.id)"
            >
              <div class="item-name">{{ item.name }}</div>
              <div class="item-actions">
                <el-icon @click.stop="handleEditLevel1(item)" class="action-icon"><Edit /></el-icon>
                <el-icon @click.stop="handleDeleteLevel1(item)" class="action-icon danger"><Delete /></el-icon>
              </div>
            </li>
            <el-empty v-if="level1List.length === 0" description="当前暂无一级地区数据" :image-size="60" />
          </ul>
        </el-card>
      </el-col>

      <!-- 右侧：二级地区 -->
      <el-col :span="18">
        <el-card shadow="hover" class="right-card">
          <template #header>
            <div class="card-header">
              <span>二级地区</span>
              <el-button type="primary" @click="handleAddLevel2" :disabled="!activeParentId">新增二级地区</el-button>
            </div>
          </template>

          <el-table
            :data="level2List"
            v-loading="loading2"
            element-loading-text="正在加载地区数据..."
            class="content-table borderless-table"
          >
            <el-table-column prop="name" label="地区名称" />
            <el-table-column prop="sortOrder" label="排序" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <div style="white-space: nowrap;">
                  <el-button type="primary" link @click="handleEditLevel2(row)">编辑</el-button>
                  <el-button type="danger" link @click="handleDeleteLevel2(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="请先选择左侧一级地区，或当前暂无子地区数据" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle"
      width="400px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="地区名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入地区名称" />
        </el-form-item>
        
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="1" :max="999" />
          <div class="form-tip">默认取当前层级最后一位；如填写已有序号，系统会自动顺延后续地区。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getRegions, createRegion, updateRegion, deleteRegion } from '@/modules/region/api.js'

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

// 列表状态
const loading1 = ref(false)
const loading2 = ref(false)
const level1List = ref([])
const level2List = ref([])
const activeParentId = ref(null)
const errorMessage = ref('')
const activeParentName = computed(() => level1List.value.find((item) => item.id === activeParentId.value)?.name || '未选择')

// 对话框与表单状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const isLevel2 = ref(false)
const submitting = ref(false)
const currentId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  parentId: 0,
  sortOrder: 1
})

const rules = {
  name: [{ required: true, message: '请输入地区名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => {
  const levelStr = isLevel2.value ? '二级地区' : '一级地区'
  return isEdit.value ? `编辑${levelStr}` : `新增${levelStr}`
})

// 当前层级默认排序取现有最大值后一位，减少手工调整成本。
const getNextSortOrder = (list) => {
  const maxSortOrder = list.reduce((max, item) => Math.max(max, Number(item.sortOrder) || 0), 0)
  return maxSortOrder + 1
}

// 加载一级地区
const fetchLevel1 = async () => {
  loading1.value = true
  errorMessage.value = ''
  try {
    const res = await getRegions({ parentId: 0 })
    level1List.value = res.data || []
    if (level1List.value.length > 0 && !activeParentId.value) {
      handleSelectParent(level1List.value[0].id)
    } else if (level1List.value.length === 0) {
      activeParentId.value = null
      level2List.value = []
    }
  } catch (e) {
    level1List.value = []
    level2List.value = []
    activeParentId.value = null
    errorMessage.value = e?.response?.data?.message || e?.message || '请稍后重试或检查接口返回。'
  } finally {
    loading1.value = false
  }
}

// 选择一级地区
const handleSelectParent = (id) => {
  activeParentId.value = id
  fetchLevel2(id)
}

// 加载二级地区
const fetchLevel2 = async (parentId) => {
  loading2.value = true
  try {
    const res = await getRegions({ parentId })
    level2List.value = res.data || []
  } catch (e) {
    level2List.value = []
    ElMessage.error(e?.response?.data?.message || e?.message || '获取二级地区失败')
  } finally {
    loading2.value = false
  }
}

const handleRefresh = () => {
  fetchLevel1()
}

// 一级地区相关操作
const handleAddLevel1 = () => {
  isEdit.value = false
  isLevel2.value = false
  currentId.value = null
  Object.assign(form, { name: '', parentId: 0, sortOrder: getNextSortOrder(level1List.value) })
  dialogVisible.value = true
}

const handleEditLevel1 = (row) => {
  isEdit.value = true
  isLevel2.value = false
  currentId.value = row.id
  Object.assign(form, { name: row.name, parentId: 0, sortOrder: row.sortOrder })
  dialogVisible.value = true
}

const handleDeleteLevel1 = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该一级地区吗？如有子地区请先删除子地区。', '删除确认', { type: 'warning' })
    await deleteRegion(row.id)
    ElMessage.success('删除成功')
    if (activeParentId.value === row.id) {
      activeParentId.value = null
    }
    fetchLevel1()
  } catch (e) {}
}

// 二级地区相关操作
const handleAddLevel2 = () => {
  isEdit.value = false
  isLevel2.value = true
  currentId.value = null
  Object.assign(form, { name: '', parentId: activeParentId.value, sortOrder: getNextSortOrder(level2List.value) })
  dialogVisible.value = true
}

const handleEditLevel2 = (row) => {
  isEdit.value = true
  isLevel2.value = true
  currentId.value = row.id
  Object.assign(form, { name: row.name, parentId: activeParentId.value, sortOrder: row.sortOrder })
  dialogVisible.value = true
}

const handleDeleteLevel2 = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该二级地区吗？', '删除确认', { type: 'warning' })
    await deleteRegion(row.id)
    ElMessage.success('删除成功')
    fetchLevel2(activeParentId.value)
  } catch (e) {}
}

// 提交地区表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRegion(currentId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createRegion(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    
    if (isLevel2.value) {
      fetchLevel2(activeParentId.value)
    } else {
      fetchLevel1()
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 页面初始化
onMounted(() => {
  fetchLevel1()
})
</script>

<style lang="scss" scoped>
.region-page {
  display: flex;
  flex-direction: column;


  .left-card {
    border-radius: 22px;
    min-height: 520px;

    .parent-list {
      list-style: none;
      padding: 0;
      margin: 0;

      .list-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        margin-bottom: 4px;
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.2s ease;
        border: 1px solid transparent;

        &:hover {
          background-color: var(--wt-fill-hover);
          .item-actions { opacity: 1; }
        }

        &.active {
          background-color: var(--el-color-primary-light-9);
          color: var(--el-color-primary);
          border-color: var(--el-color-primary-light-7);
          font-weight: 600;
        }

        .item-actions {
          opacity: 0;
          transition: opacity 0.2s;
          display: flex;
          gap: 12px;

          .action-icon {
            color: var(--wt-text-secondary);
            font-size: 30px;
            cursor: pointer;
            padding: 4px;
            border-radius: 6px;
            transition: all 0.2s;

            &:hover { color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
            &.danger:hover { color: #ef4444; background: #fef2f2; }
          }
        }
      }
    }
  }

  .right-card {
    border-radius: 22px;
    min-height: 520px;
  }
}

.content-table {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.content-table th.el-table__cell) {
  background: var(--wt-fill-hover);
  color: var(--wt-text-secondary);
  font-weight: 600;
}


:deep(.content-table .el-table__row:hover > td.el-table__cell) {
  background: var(--wt-row-gradient-hover) !important;
}
.form-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--wt-text-secondary);
}

@media (max-width: 1200px) {
  .region-page {
    .summary-grid {
      grid-template-columns: 1fr;
    }
  }
}

</style>
