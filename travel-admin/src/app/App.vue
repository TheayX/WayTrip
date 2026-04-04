<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useTheme } from '@/shared/composables/useTheme.js'

const { initTheme } = useTheme()

onMounted(() => {
  initTheme()
})
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
