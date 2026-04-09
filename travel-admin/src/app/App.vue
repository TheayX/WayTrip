<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useTheme } from '@/shared/composables/useTheme.js'

// 应用根组件只负责首屏挂载时完成全局主题初始化，避免主题逻辑散落到各页面。
const { initTheme } = useTheme()

onMounted(() => {
  // 首屏挂载时立即同步主题，避免后台刷新后出现短暂闪白或主题错位。
  initTheme()
})
</script>

