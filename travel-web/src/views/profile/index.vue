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
          <el-avatar :size="64" :src="userStore.userInfo?.avatar ? getImageUrl(userStore.userInfo.avatar) : ''" icon="User" />
          <h3 class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</h3>
          <p class="user-phone">{{ userStore.userInfo?.phone || '未绑定手机' }}</p>
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
                  :src="avatarPreview || (profileForm.avatar ? getImageUrl(profileForm.avatar) : '')"
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
              :checked="selectedCategories.includes(cat.name)"
              @change="toggleCategory(cat.name)"
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserInfo, setPreferences, changePassword, uploadAvatar } from '@/api/auth'
import { getFilters } from '@/api/spot'
import { getImageUrl } from '@/utils/request'
import { ElMessage } from 'element-plus'

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

const handleMenuSelect = (index) => {
  activeMenu.value = index
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
    selectedCategories.value = info.preferences || []
  } catch (e) { /* ignore */ }
}

const fetchCategories = async () => {
  try {
    const res = await getFilters()
    categories.value = res.data?.categories || []
  } catch (e) { /* ignore */ }
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
  } catch (e) { /* ignore */ }
  saving.value = false
}

const toggleCategory = (name) => {
  const idx = selectedCategories.value.indexOf(name)
  if (idx > -1) {
    selectedCategories.value.splice(idx, 1)
  } else {
    selectedCategories.value.push(name)
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
  } catch (e) { /* ignore */ }
  savingPwd.value = false
}

const handleSavePreference = async () => {
  if (!selectedCategories.value.length) {
    ElMessage.warning('请至少选择一个标签')
    return
  }
  savingPref.value = true
  try {
    await setPreferences(selectedCategories.value)
    userStore.updatePreferences(selectedCategories.value)
    ElMessage.success('偏好保存成功')
  } catch (e) { /* ignore */ }
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
</style>

