// Web 端 Vite 配置文件
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  // Vue 3 插件支持
  plugins: [vue()],
  // CSS 预处理器配置（使用现代编译 API）
  css: {
    preprocessorOptions: {
      scss: { api: 'modern-compiler' }
    }
  },
  // 模块解析配置
  resolve: {
    alias: {
      // '@' 别名指向 src 目录，方便导入文件
      '@': path.resolve(__dirname, 'src')
    }
  },
  // 构建优化配置（代码分割）
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // Element Plus 组件库单独打包
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          // Vue 核心库单独打包
          'vue-vendor': ['vue', 'vue-router', 'pinia']
        }
      }
    }
  },
  // 开发服务器配置
  server: {
    port: 3001, // Web 端端口号（区别于管理后台的 3000）
    proxy: {
      // API 请求代理到后端服务器
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 静态资源（图片等）代理到后端服务器
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
