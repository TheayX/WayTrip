<!-- 管理员管理页面 -->
<template>
  <div class="admin-page">
    <el-card shadow="never">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>管理员管理</span>
          <el-button type="primary" @click="handleAdd">新增管理员</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryParams" inline class="search-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
              v-model="queryParams.keyword"
              placeholder="用户名/姓名"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
              v-model="uiStatus"
              placeholder="全部"
              clearable
              style="width: 140px"
              @change="handleSearch"
              @clear="handleSearch"
          >
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe class="borderless-table">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="140" align="left" />
        <el-table-column prop="realName" label="姓名" min-width="140" align="left" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="capsule-badge status-capsule" :class="row.status === 1 ? 'status-success' : 'status-neutral'">
              <span class="dot"></span>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.lastLoginAt) || '-' }}
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
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="warning" @click="handleOpenPasswordDialog(row)">重置密码</el-button>
              <el-dropdown trigger="click" @command="(command) => handleCommand(command, row)">
                <el-button link type="primary">
                  更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑管理员</el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-text" :disabled="isCurrentAdmin(row)">删除管理员</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchData"
            @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑管理员' : '新增管理员'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!isEdit" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="仅支持字母/数字/下划线" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0" :disabled="isEditingCurrentAdmin">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resettingPassword" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/app/store/user.js'
import { createAdmin, deleteAdmin, getAdminList, resetAdminPassword, updateAdmin } from '@/modules/system/api/admin.js'

const userStore = useUserStore()

// 列表状态
const loading = ref(false)
const total = ref(0) // 总数
const tableData = ref([]) // 表格数据
const uiStatus = ref('') // UI 绑定状态（用于下拉框）

// 查询参数
const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: null
})

// 对话框与表单状态
const dialogVisible = ref(false)
const isEdit = ref(false) // 是否为编辑模式
const isEditingCurrentAdmin = ref(false) // 是否正在编辑当前登录的管理员
const editingId = ref(null) // 正在编辑的管理员 ID
const submitting = ref(false) // 提交中状态
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  realName: '',
  status: 1
})

// 重置密码对话框状态
const passwordDialogVisible = ref(false)
const resettingPassword = ref(false)
const passwordFormRef = ref()
const passwordTargetId = ref(null) // 要重置密码的管理员 ID
const passwordForm = reactive({
  password: ''
})

// 当前管理员信息
const currentAdminId = computed(() => userStore.adminInfo?.id)

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '长度需在3-32个字符之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度需在6-32个字符之间', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 32, message: '长度不能超过32个字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 重置密码表单验证规则
const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度需在6-32个字符之间', trigger: 'blur' }
  ]
}

// 获取管理员列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminList(queryParams)
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 搜索操作
const handleSearch = () => {
  queryParams.page = 1
  queryParams.status = uiStatus.value === '' || uiStatus.value == null ? null : Number(uiStatus.value)
  fetchData()
}

// 重置搜索条件
const handleReset = () => {
  queryParams.keyword = ''
  queryParams.status = null
  uiStatus.value = ''
  handleSearch()
}

// 重置表单状态
const resetFormState = () => {
  form.username = ''
  form.password = ''
  form.realName = ''
  form.status = 1
  editingId.value = null
  isEditingCurrentAdmin.value = false
  formRef.value?.clearValidate()
}

// 新增管理员
const handleAdd = () => {
  isEdit.value = false
  resetFormState()
  dialogVisible.value = true
}

// 编辑管理员
const handleEdit = (row) => {
  isEdit.value = true
  resetFormState()
  editingId.value = row.id
  form.realName = row.realName
  form.status = row.status
  isEditingCurrentAdmin.value = row.id === currentAdminId.value
  dialogVisible.value = true
}

// 提交管理员表单
const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAdmin(editingId.value, { realName: form.realName, status: form.status })
      ElMessage.success('更新成功')
    } else {
      await createAdmin({
        username: form.username,
        password: form.password,
        realName: form.realName,
        status: form.status
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

// 打开重置密码对话框
const handleOpenPasswordDialog = (row) => {
  passwordTargetId.value = row.id
  passwordForm.password = ''
  passwordFormRef.value?.clearValidate()
  passwordDialogVisible.value = true
}

// 重置密码
const handleResetPassword = async () => {
  await passwordFormRef.value.validate()
  resettingPassword.value = true
  try {
    await resetAdminPassword(passwordTargetId.value, passwordForm.password)
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } finally {
    resettingPassword.value = false
  }
}

// 删除管理员
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除管理员「${row.username}」吗？`, '提示', { type: 'warning' })
  await deleteAdmin(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

const handleCommand = (command, row) => {
  switch (command) {
    case 'edit':
      handleEdit(row)
      break
    case 'delete':
      if (!isCurrentAdmin(row)) {
        handleDelete(row)
      }
      break
  }
}

// 判断是否为当前登录管理员
const isCurrentAdmin = (row) => row.id === currentAdminId.value

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

// 页面初始化
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.admin-page {
  .table-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }
}

.borderless-table {
  :deep(.el-table__inner-wrapper::before) {
    display: none;
  }

  :deep(td.el-table__cell),
  :deep(th.el-table__cell.is-leaf) {
    border-bottom: 1px solid #f8fafc;
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

.danger-text {
  color: #ef4444 !important;
}
</style>
