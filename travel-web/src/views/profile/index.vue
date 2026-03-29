<template>
  <div class="page-container">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>个人中心</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="profile-layout">
      <!-- 左侧导航 -->
      <div class="profile-sidebar card">
        <div class="user-header">
          <el-avatar :size="64" :src="getAvatarUrl(userStore.userInfo?.avatar)" icon="User" />
          <h3 class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</h3>
          <p class="user-phone">{{ formatPhone(userStore.userInfo?.phone) }}</p>
        </div>
        <el-menu :default-active="activeMenu" @select="handleMenuSelect">
          <el-menu-item index="info">
            <el-icon><User /></el-icon>
            <span>基本信息</span>
          </el-menu-item>
          <el-menu-item index="preference">
            <el-icon><Setting /></el-icon>
            <span>偏好设置</span>
          </el-menu-item>
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </el-menu-item>
          <el-menu-item index="orders" @click="$router.push('/orders')">
            <el-icon><Tickets /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="favorites" @click="$router.push('/favorites')">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>
          <el-menu-item index="deactivate">
            <el-icon><Delete /></el-icon>
            <span>注销账户</span>
          </el-menu-item>
          <el-menu-item index="reviews" @click="$router.push('/reviews')">
            <el-icon><ChatDotRound /></el-icon>
            <span>我的评价</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧内容 -->
      <div class="profile-main">
        <!-- 基本信息 -->
        <div v-if="activeMenu === 'info'" class="section-card card">
          <h3 class="card-title">基本信息</h3>
          <el-form :model="profileForm" label-width="80px" size="large">
            <el-form-item label="头像">
              <div class="avatar-uploader" @click="handleAvatarClick">
                <el-avatar
                  :size="80"
                  :src="avatarPreview || getAvatarUrl(profileForm.avatar)"
                  icon="User"
                />
                <div class="avatar-overlay">
                  <el-icon :size="20"><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
              </div>
              <input ref="avatarInputRef" type="file" accept="image/*" style="display:none" @change="handleAvatarChange" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="30" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="20" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 偏好设置 -->
        <div v-if="activeMenu === 'preference'" class="section-card card">
          <h3 class="card-title">偏好设置</h3>
          <p class="tip">选择你感兴趣的旅行类型，我们将为你推荐更精准的内容</p>
          <div class="preference-tags">
            <el-check-tag
              v-for="cat in categories"
              :key="cat.id"
              :checked="selectedCategories.includes(cat.id)"
              @change="toggleCategory(cat.id)"
              class="pref-tag"
            >{{ cat.name }}</el-check-tag>
          </div>
          <el-button type="primary" :loading="savingPref" @click="handleSavePreference" style="margin-top: 24px">
            保存偏好
          </el-button>
        </div>

        <!-- 修改密码 -->
        <div v-if="activeMenu === 'password'" class="section-card card">
          <h3 class="card-title">修改密码</h3>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px" size="large">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码（首次设置可留空）" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 注销账户 -->
        <div v-if="activeMenu === 'deactivate'" class="section-card card danger-section">
          <h3 class="card-title danger-title">注销账户</h3>
          <div class="danger-warning">
            <el-alert
              title="注销说明"
              type="warning"
              :closable="false"
              description="注销后你将无法使用此账户登录，所有数据将被保留。使用同一微信号重新登录可恢复账户。"
            />
          </div>
          <div class="danger-info">
            <div class="info-item">
              <span class="info-icon">•</span>
              <span>注销后无法立即使用此账户</span>
            </div>
            <div class="info-item">
              <span class="info-icon">•</span>
              <span>你的订单记录、收藏等数据将被保留</span>
            </div>
            <div class="info-item">
              <span class="info-icon">•</span>
              <span>使用同一微信号重新登录可恢复账户</span>
            </div>
            <div class="info-item">
              <span class="info-icon">•</span>
              <span>此操作无法撤销，请谨慎考虑</span>
            </div>
          </div>
          <el-button type="danger" @click="handleDeactivate" style="margin-top: 24px">
            确认注销账户
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserInfo, setPreferences, changePassword, uploadAvatar, deactivateAccount } from '@/api/user'
import { getFilters } from '@/api/spot'
import { getAvatarUrl } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const userStore = useUserStore()
const activeMenu = ref('info')
const saving = ref(false)
const savingPref = ref(false)
const categories = ref([])
const selectedCategories = ref([])

const profileForm = reactive({
  nickname: '',
  phone: '',
  avatar: ''
})

// 头像相关
const avatarInputRef = ref(null)
const avatarPreview = ref('')
const avatarFile = ref(null)

const formatPhone = (phone) => {
  if (!phone || !phone.trim()) return '未绑定手机'
  const normalized = phone.trim()
  if (/^1\d{10}$/.test(normalized)) {
    return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  }
  if (/^1\d{2}\*{4}\d{4}$/.test(normalized)) {
    return normalized
  }
  return '已隐藏'
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
}

const logProfileError = (message, error) => {
  if (import.meta.env.DEV) {
    console.error(message, error)
  }
}

const getErrorMessage = (error, fallback) => {
  return error?.response?.data?.message || error?.data?.message || error?.message || fallback
}

const fetchUserInfo = async () => {
  try {
    const res = await getUserInfo()
    const info = res.data
    userStore.setUserInfo(info)
    profileForm.nickname = info.nickname || ''
    profileForm.phone = info.phone || ''
    profileForm.avatar = info.avatar || ''
    avatarPreview.value = ''
    avatarFile.value = null
    selectedCategories.value = info.preferenceCategoryIds || []
  } catch (e) {
    logProfileError('获取用户信息失败', e)
    ElMessage.error(getErrorMessage(e, '加载用户信息失败'))
  }
}

const fetchCategories = async () => {
  try {
    const res = await getFilters()
    categories.value = res.data?.categories || []
  } catch (e) {
    logProfileError('获取偏好分类失败', e)
    ElMessage.error(getErrorMessage(e, '加载偏好分类失败'))
  }
}

// 点击头像触发文件选择
const handleAvatarClick = () => {
  avatarInputRef.value?.click()
}

// 选择头像文件后预览
const handleAvatarChange = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片大小不能超过2MB')
    return
  }
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
}

const saveProfile = async () => {
  saving.value = true
  try {
    let newAvatarUrl = ''
    // 如果有选择新头像，先上传
    if (avatarFile.value) {
      const uploadRes = await uploadAvatar(avatarFile.value)
      newAvatarUrl = uploadRes.data.url
    }
    const updateData = {
      nickname: profileForm.nickname,
      phone: profileForm.phone
    }
    if (newAvatarUrl) {
      updateData.avatar = newAvatarUrl
    }
    await updateUserInfo(updateData)
    userStore.setUserInfo({
      ...userStore.userInfo,
      nickname: profileForm.nickname,
      phone: profileForm.phone,
      ...(newAvatarUrl ? { avatar: newAvatarUrl } : {})
    })
    if (newAvatarUrl) {
      profileForm.avatar = newAvatarUrl
      avatarPreview.value = ''
      avatarFile.value = null
    }
    ElMessage.success('保存成功')
  } catch (e) {
    logProfileError('保存用户资料失败', e)
    ElMessage.error(getErrorMessage(e, '保存失败'))
  }
  saving.value = false
}

const toggleCategory = (categoryId) => {
  const idx = selectedCategories.value.indexOf(categoryId)
  if (idx > -1) {
    selectedCategories.value.splice(idx, 1)
  } else {
    selectedCategories.value.push(categoryId)
  }
}

// ======== 修改密码 ========
const savingPwd = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度为6-50个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate()
  savingPwd.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword || undefined,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    logProfileError('修改密码失败', e)
    ElMessage.error(getErrorMessage(e, '密码修改失败'))
  }
  savingPwd.value = false
}

// ======== 注销账户 ========
const handleDeactivate = () => {
  ElMessageBox.confirm(
    '注销后你将无法使用此账户登录，确定要继续吗？',
    '确认注销账户',
    {
      confirmButtonText: '确认注销',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await deactivateAccount()
        ElMessage.success('账户已注销，已为您登出')
        setTimeout(() => {
          userStore.logout()
          router.push('/login')
        }, 1500)
      } catch (e) {
        logProfileError('注销账户失败', e)
        ElMessage.error(getErrorMessage(e, '注销账户失败'))
      }
    })
    .catch(() => {})
}

const handleSavePreference = async () => {
  if (!selectedCategories.value.length) {
    ElMessage.warning('请至少选择一个标签')
    return
  }
  savingPref.value = true
  try {
    const categoryNames = selectedCategories.value
      .map(id => categories.value.find(cat => cat.id === id)?.name)
      .filter(Boolean)
    await setPreferences(selectedCategories.value)
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: [...selectedCategories.value],
      preferenceCategoryNames: categoryNames
    })
    ElMessage.success('偏好保存成功')
  } catch (e) {
    logProfileError('保存偏好失败', e)
    ElMessage.error(getErrorMessage(e, '偏好保存失败'))
  }
  savingPref.value = false
}

onMounted(() => {
  fetchUserInfo()
  fetchCategories()
})
</script>

<style lang="scss" scoped>
.profile-layout {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.profile-sidebar {
  width: 240px;
  flex-shrink: 0;
  border-radius: 12px;
  padding: 24px 0;
  height: fit-content;
  position: sticky;
  top: 80px;
}

.user-header {
  text-align: center;
  padding: 0 20px 20px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.user-name {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 600;
}

.user-phone {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.profile-main {
  flex: 1;
}

.section-card {
  padding: 24px;
  border-radius: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}

.tip {
  font-size: 14px;
  color: #909399;
  margin-bottom: 16px;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.pref-tag {
  padding: 8px 20px;
  font-size: 14px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  width: 80px;
  height: 80px;

  &:hover .avatar-overlay {
    opacity: 1;
  }
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
  gap: 2px;
}

.danger-section {
  background: #fef0f0;
  border: 1px solid #fde2e4;
}

.danger-title {
  color: #f56c6c;
}

.danger-warning {
  margin: 20px 0;
}

.danger-info {
  margin: 20px 0;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  border-left: 3px solid #f56c6c;
}

.info-item {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-icon {
  color: #f56c6c;
  flex-shrink: 0;
}
</style>

