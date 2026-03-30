<!-- 设置页 -->
<template>
  <div class="page-container settings-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>设置</el-breadcrumb-item>
    </el-breadcrumb>

    <section class="settings-card card">
      <div class="settings-item" @click="contactService">
        <span>联系客服</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="settings-item" @click="showAbout">
        <span>关于我们</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="settings-item" @click="clearCache">
        <span>清除缓存</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearFootprints } from '@/utils/footprint'
import { clearLocationCache } from '@/utils/location'

// 交互处理方法
const contactService = () => {
  ElMessageBox.alert('客服电话：400-123-4567', '联系客服', {
    confirmButtonText: '知道了'
  })
}

const showAbout = () => {
  ElMessageBox.alert('WayTrip v1.0.0\n基于协同过滤的个性化旅游推荐。', '关于我们', {
    confirmButtonText: '知道了'
  })
}

const clearCache = async () => {
  await ElMessageBox.confirm('确认清除本地缓存吗？这会清空浏览足迹、定位缓存和冷启动状态。', '清除缓存', {
    type: 'warning'
  })
  clearFootprints()
  clearLocationCache()
  Object.keys(localStorage).forEach((key) => {
    if (key.startsWith('cold_start_guide:')) {
      localStorage.removeItem(key)
    }
  })
  ElMessage.success('缓存已清除')
}
</script>

<style lang="scss" scoped>
.settings-card {
  border-radius: 18px;
}

.settings-item {
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;

  &:last-child {
    border-bottom: none;
  }
}
</style>
