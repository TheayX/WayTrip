<template>
  <div class="layout">
    <!-- 顶部导航 -->
    <header class="navbar">
      <div class="navbar-inner">
        <div class="navbar-left">
          <router-link to="/" class="logo">
            <span class="logo-icon">✈</span>
            <span class="logo-text">WayTrip</span>
          </router-link>
          <nav class="nav-links">
            <router-link to="/" class="nav-link" active-class="active" :class="{ active: $route.path === '/' }">首页</router-link>
            <router-link to="/spots" class="nav-link" active-class="active">景点</router-link>
            <router-link to="/guides" class="nav-link" active-class="active">攻略</router-link>
          </nav>
        </div>
        <div class="navbar-right">
          <div class="search-trigger" @click="$router.push('/search')">
            <el-icon><Search /></el-icon>
            <span>搜索景点...</span>
          </div>
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-avatar">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar || ''" icon="User" />
                <span class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="orders">
                    <el-icon><Tickets /></el-icon>我的订单
                  </el-dropdown-item>
                  <el-dropdown-item command="favorites">
                    <el-icon><Star /></el-icon>我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" round @click="$router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>

    <!-- 底部 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-left">
          <span class="footer-logo">✈ WayTrip</span>
          <p class="footer-desc">发现旅途之美，开启精彩旅程</p>
        </div>
        <div class="footer-links">
          <router-link to="/spots">热门景点</router-link>
          <router-link to="/guides">旅行攻略</router-link>
        </div>
        <div class="footer-right">
          <p>© 2026 WayTrip. All rights reserved.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'favorites':
      router.push('/favorites')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout()
        router.push('/')
      }).catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== 导航栏 ===== */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #ebeef5;
  height: 64px;
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  text-decoration: none;

  .logo-icon {
    font-size: 24px;
  }
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-link {
  padding: 8px 16px;
  font-size: 15px;
  color: #606266;
  border-radius: 8px;
  transition: all 0.2s;
  text-decoration: none;

  &:hover {
    color: #409eff;
    background: #ecf5ff;
  }

  &.active {
    color: #409eff;
    font-weight: 600;
    background: #ecf5ff;
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 20px;
  color: #909399;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #ebeef5;
    color: #606266;
  }
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover {
    background: #f5f7fa;
  }

  .user-name {
    font-size: 14px;
    color: #303133;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

/* ===== 主内容 ===== */
.main-content {
  flex: 1;
}

/* ===== 底部 ===== */
.footer {
  background: #2c3e50;
  color: #ffffffcc;
  padding: 32px 0;
  margin-top: 40px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-logo {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.footer-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #ffffff99;
}

.footer-links {
  display: flex;
  gap: 24px;

  a {
    color: #ffffffcc;
    text-decoration: none;
    font-size: 14px;
    transition: color 0.2s;

    &:hover {
      color: #fff;
    }
  }
}

.footer-right {
  font-size: 13px;
  color: #ffffff80;
}
</style>

