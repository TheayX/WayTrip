<template>
  <div class="admin-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>管理员管理</span>
          <el-button type="primary" @click="handleAdd">新增管理员</el-button>
        </div>
      </template>

      <el-form :model="queryParams" inline class="search-form">
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="用户名/姓名"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
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

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="realName" label="姓名" min-width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastLoginAt) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleOpenPasswordDialog(row)">重置密码</el-button>
            <el-button
              link
              type="danger"
              :disabled="isCurrentAdmin(row)"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑管理员' : '新增管理员'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!isEdit" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="仅支持字母/数字/下划线" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
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

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="至少6位" />
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
import { useUserStore } from '@/stores/user'
import { createAdmin, deleteAdmin, getAdminList, resetAdminPassword, updateAdmin } from '@/api/admin'

const userStore = useUserStore()

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const uiStatus = ref('')
const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const isEditingCurrentAdmin = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  realName: '',
  status: 1
})

const passwordDialogVisible = ref(false)
const resettingPassword = ref(false)
const passwordFormRef = ref()
const passwordTargetId = ref(null)
const passwordForm = reactive({
  password: ''
})

const currentAdminId = computed(() => userStore.adminInfo?.id)

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

const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度需在6-32个字符之间', trigger: 'blur' }
  ]
}

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

const handleSearch = () => {
  queryParams.page = 1
  queryParams.status = uiStatus.value === '' || uiStatus.value == null ? null : Number(uiStatus.value)
  fetchData()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.status = null
  uiStatus.value = ''
  handleSearch()
}

const resetFormState = () => {
  form.username = ''
  form.password = ''
  form.realName = ''
  form.status = 1
  editingId.value = null
  isEditingCurrentAdmin.value = false
  formRef.value?.clearValidate()
}

const handleAdd = () => {
  isEdit.value = false
  resetFormState()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  resetFormState()
  editingId.value = row.id
  form.realName = row.realName
  form.status = row.status
  isEditingCurrentAdmin.value = row.id === currentAdminId.value
  dialogVisible.value = true
}

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

const handleOpenPasswordDialog = (row) => {
  passwordTargetId.value = row.id
  passwordForm.password = ''
  passwordFormRef.value?.clearValidate()
  passwordDialogVisible.value = true
}

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

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除管理员「${row.username}」吗？`, '提示', { type: 'warning' })
  await deleteAdmin(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

const isCurrentAdmin = (row) => row.id === currentAdminId.value

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.admin-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
