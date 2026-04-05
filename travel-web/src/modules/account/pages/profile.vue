<!-- 个人中心页 -->
<template>
  <div class="page-container profile-page">
    <AccountPageHeader title="个人中心" subtitle="管理资料、偏好和账号安全设置。">
      <template #actions>
        <el-button @click="$router.push(ACCOUNT_ROUTE_PATHS.settings)">系统设置</el-button>
      </template>
    </AccountPageHeader>

    <section class="hero card">
      <div class="hero-main">
        <el-avatar :size="72" :src="avatarPreview || getAvatarUrl(userStore.userInfo?.avatar)" icon="User" />
        <div>
          <h2 class="user-name">{{ userStore.userInfo?.nickname || '旅行家' }}</h2>
          <p class="user-phone">手机号：{{ formatPhone(userStore.userInfo?.phone) }}</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <div class="stats-card card" @click="$router.push(`${ACCOUNT_ROUTE_PATHS.activity}?tab=browse`)">
        <strong>{{ dashboardStats.viewed }}</strong>
        <span>浏览历史</span>
      </div>
      <div class="stats-card card" @click="$router.push(`${ACCOUNT_ROUTE_PATHS.activity}?tab=favorite`)">
        <strong>{{ dashboardStats.favorites }}</strong>
        <span>我的收藏</span>
      </div>
      <div class="stats-card card" @click="$router.push(`${ACCOUNT_ROUTE_PATHS.activity}?tab=review`)">
        <strong>{{ dashboardStats.reviews }}</strong>
        <span>我的评价</span>
      </div>
    </section>

    <section class="order-overview card">
      <div class="section-header">
        <h3>旅行订单</h3>
        <el-button text class="overview-link-button" @click="$router.push(ACCOUNT_ROUTE_PATHS.orders)">查看全部</el-button>
      </div>
      <div class="order-grid">
        <div class="order-card" @click="$router.push(`${ACCOUNT_ROUTE_PATHS.orders}?status=pending`)">
          <strong>{{ orderStats.pending }}</strong>
          <span>待支付</span>
        </div>
        <div class="order-card" @click="$router.push(`${ACCOUNT_ROUTE_PATHS.orders}?status=paid`)">
          <strong>{{ orderStats.paid }}</strong>
          <span>已支付</span>
        </div>
        <div class="order-card" @click="$router.push(`${ACCOUNT_ROUTE_PATHS.orders}?status=completed`)">
          <strong>{{ orderStats.completed }}</strong>
          <span>已完成</span>
        </div>
      </div>
    </section>

    <section class="entry-grid">
      <div class="entry-card card" @click="$router.push(`${ACCOUNT_ROUTE_PATHS.activity}?tab=browse`)">我的互动</div>
      <div class="entry-card card" @click="$router.push(ACCOUNT_ROUTE_PATHS.orders)">我的订单</div>
      <div class="entry-card card" @click="activeMenu = 'preference'">偏好设置</div>
      <div class="entry-card card" @click="activeMenu = 'info'">账号资料</div>
    </section>

    <section class="profile-layout">
      <aside class="profile-sidebar card">
        <el-menu :default-active="activeMenu" @select="handleMenuSelect">
          <el-menu-item index="info"><el-icon><User /></el-icon><span>基本信息</span></el-menu-item>
          <el-menu-item index="preference"><el-icon><Setting /></el-icon><span>偏好设置</span></el-menu-item>
          <el-menu-item index="password"><el-icon><Lock /></el-icon><span>修改密码</span></el-menu-item>
          <el-menu-item index="deactivate"><el-icon><Delete /></el-icon><span>注销账户</span></el-menu-item>
        </el-menu>
      </aside>

      <div class="profile-main">
        <div v-if="activeMenu === 'info'" class="section-card card">
          <h3 class="card-title">基本信息</h3>
          <el-form :model="profileForm" label-width="80px" size="large">
            <el-form-item label="头像">
              <div class="avatar-uploader" @click="handleAvatarClick">
                <el-avatar :size="80" :src="avatarPreview || getAvatarUrl(profileForm.avatar)" icon="User" />
                <div class="avatar-overlay">
                  <el-icon :size="20"><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
              </div>
              <input ref="avatarInputRef" type="file" accept="image/*" style="display:none" @change="handleAvatarChange" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" maxlength="30" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" maxlength="20" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeMenu === 'preference'" class="section-card card">
          <h3 class="card-title">偏好设置</h3>
          <p class="tip">选择你感兴趣的旅行类型，也可以清空偏好，推荐会回到热门冷启动。</p>
          <div class="preference-tags">
            <el-check-tag
              v-for="cat in categories"
              :key="cat.id"
              :checked="selectedCategories.includes(cat.id)"
              @change="toggleCategory(cat.id)"
            >
              {{ cat.name }}
            </el-check-tag>
          </div>
          <el-button type="primary" :loading="savingPref" style="margin-top: 24px" @click="handleSavePreference">保存偏好</el-button>
        </div>

        <div v-if="activeMenu === 'password'" class="section-card card">
          <h3 class="card-title">修改密码</h3>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" size="large">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="首次设置可留空" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeMenu === 'deactivate'" class="section-card card danger-section">
          <h3 class="card-title danger-title">注销账户</h3>
          <el-alert
            title="注销说明"
            type="warning"
            :closable="false"
            description="注销后你将无法使用此账户登录，所有数据会保留。使用同一账户重新登录时可以恢复。"
          />
          <el-button type="danger" style="margin-top: 24px" @click="handleDeactivate">确认注销账户</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPageHeader from '@/modules/account/components/AccountPageHeader.vue'
import { ACCOUNT_ROUTE_PATHS } from '@/modules/account/constants/routes.js'
import { useUserStore } from '@/modules/account/store/user.js'
import { AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { getFavoriteList } from '@/modules/favorite/api.js'
import { getOrderList } from '@/modules/order/api.js'
import { getMyReviews } from '@/modules/review/api.js'
import { getFilters } from '@/modules/spot/api.js'
import { getUserInfo, updateUserInfo, setPreferences, changePassword, uploadAvatar, deactivateAccount } from '@/modules/account/api.js'
import { getFootprints } from '@/shared/lib/footprint.js'
import { getAvatarUrl } from '@/shared/api/client.js'

// 基础依赖与路由状态
const router = useRouter()
const userStore = useUserStore()

// 页面数据状态
const activeMenu = ref('info')
const saving = ref(false)
const savingPref = ref(false)
const savingPwd = ref(false)
const categories = ref([])
const selectedCategories = ref([])
const avatarInputRef = ref(null)
const avatarPreview = ref('')
const avatarFile = ref(null)
const passwordFormRef = ref(null)

const dashboardStats = reactive({ viewed: 0, favorites: 0, reviews: 0 })
const orderStats = reactive({ pending: 0, paid: 0, completed: 0 })

const profileForm = reactive({
  nickname: '',
  phone: '',
  avatar: ''
})

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
      validator: (_rule, value, callback) => {
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

// 工具方法
const formatPhone = (phone) => {
  if (!phone || !phone.trim()) return '未绑定手机'
  const normalized = phone.trim()
  if (/^1\d{10}$/.test(normalized)) return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  if (/^1\d{2}\*{4}\d{4}$/.test(normalized)) return normalized
  return '已隐藏'
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
}

// 数据加载方法
const fetchUserData = async () => {
  const [userRes, categoryRes] = await Promise.all([
    getUserInfo(),
    getFilters()
  ])

  const info = userRes.data
  userStore.setUserInfo(info)
  profileForm.nickname = info.nickname || ''
  profileForm.phone = info.phone || ''
  profileForm.avatar = info.avatar || ''
  selectedCategories.value = info.preferenceCategoryIds || []
  categories.value = categoryRes.data?.categories || []
}

const loadOverview = async () => {
  dashboardStats.viewed = getFootprints().length

  const [favoriteRes, reviewRes, pendingRes, paidRes, completedRes] = await Promise.all([
    getFavoriteList(1, 1),
    getMyReviews(1, 1),
    getOrderList({ status: 'pending', page: 1, pageSize: 1 }),
    getOrderList({ status: 'paid', page: 1, pageSize: 1 }),
    getOrderList({ status: 'completed', page: 1, pageSize: 1 })
  ])

  dashboardStats.favorites = favoriteRes.data?.total || favoriteRes.data?.list?.length || 0
  dashboardStats.reviews = reviewRes.data?.total || reviewRes.data?.list?.length || 0
  orderStats.pending = pendingRes.data?.total || pendingRes.data?.list?.length || 0
  orderStats.paid = paidRes.data?.total || paidRes.data?.list?.length || 0
  orderStats.completed = completedRes.data?.total || completedRes.data?.list?.length || 0
}

// 交互处理方法
const handleAvatarClick = () => {
  avatarInputRef.value?.click()
}

const handleAvatarChange = (event) => {
  const file = event.target.files?.[0]
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
    let avatar = ''
    if (avatarFile.value) {
      const uploadRes = await uploadAvatar(avatarFile.value)
      avatar = uploadRes.data.url
    }

    const payload = {
      nickname: profileForm.nickname,
      phone: profileForm.phone,
      ...(avatar ? { avatar } : {})
    }

    await updateUserInfo(payload)
    userStore.setUserInfo({
      ...userStore.userInfo,
      ...payload
    })
    profileForm.avatar = avatar || profileForm.avatar
    avatarPreview.value = ''
    avatarFile.value = null
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

const toggleCategory = (id) => {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
  } else {
    selectedCategories.value.push(id)
  }
}

const handleSavePreference = async () => {
  savingPref.value = true
  try {
    const categoryNames = selectedCategories.value
      .map((id) => categories.value.find((cat) => cat.id === id)?.name)
      .filter(Boolean)
    await setPreferences(selectedCategories.value)
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: [...selectedCategories.value],
      preferenceCategoryNames: categoryNames
    })
    ElMessage.success('偏好保存成功')
  } finally {
    savingPref.value = false
  }
}

const handleChangePassword = async () => {
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
  } finally {
    savingPwd.value = false
  }
}

const handleDeactivate = async () => {
  await ElMessageBox.confirm('注销后你将无法使用此账户登录，确定继续吗？', '确认注销', {
    type: 'warning'
  })
  await deactivateAccount()
  userStore.logout()
  ElMessage.success('账户已注销')
  router.replace(AUTH_ROUTE_PATHS.login)
}

const handleLogout = async () => {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    type: 'warning'
  })
  userStore.logout()
  router.replace(AUTH_ROUTE_PATHS.login)
}

// 生命周期
onMounted(async () => {
  await Promise.all([fetchUserData(), loadOverview()])
})
</script>

<style lang="scss" scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.user-phone {
  color: #909399;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.stats-grid,
.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stats-card,
.entry-card {
  padding: 22px;
  cursor: pointer;
  text-align: center;
}

.stats-card strong,
.order-card strong {
  display: block;
  font-size: 30px;
  color: #111827;
}

.stats-card span,
.order-card span {
  display: block;
  margin-top: 10px;
  color: #64748b;
}

.order-overview {
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

:deep(.el-button.overview-link-button) {
  color: #475569;
  background: transparent;
  border-color: transparent;
}

:deep(.el-button.overview-link-button:hover) {
  color: #0f172a;
  background: rgba(241, 245, 249, 0.92);
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.order-card {
  padding: 22px;
  border-radius: 16px;
  text-align: center;
  background: #f8fafc;
  cursor: pointer;
}

.profile-layout {
  display: flex;
  gap: 24px;
}

.profile-sidebar {
  width: 240px;
  flex-shrink: 0;
  border-radius: 12px;
  padding: 24px 0;
  height: fit-content;
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
  color: #909399;
  margin-bottom: 16px;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;

  &:hover .avatar-overlay {
    opacity: 1;
  }
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.danger-section {
  background: #fef0f0;
  border: 1px solid #fde2e4;
}

.danger-title {
  color: #f56c6c;
}

@media (max-width: 992px) {
  .stats-grid,
  .entry-grid,
  .order-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .profile-layout {
    flex-direction: column;
  }

  .profile-sidebar {
    width: 100%;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .stats-grid,
  .entry-grid,
  .order-grid {
    grid-template-columns: 1fr;
  }
}
</style>
