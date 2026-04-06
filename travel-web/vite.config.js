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
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }

          // UI 组件库与图标库拆开，避免继续堆进同一个超大 chunk。
          if (id.includes('element-plus')) {
            return 'element-plus'
          }
          if (id.includes('@element-plus/icons-vue')) {
            return 'element-plus-icons'
          }

          // 基础框架相关依赖保持独立，减少业务更新时的缓存失效范围。
          if (id.includes('vue-router') || id.includes('pinia') || id.includes('/vue/')) {
            return 'vue-vendor'
          }

          // 请求层依赖单独拆出，便于后续继续演进 shared/api。
          if (id.includes('axios')) {
            return 'http-vendor'
          }

          return 'vendor'
        }
      }
    }
  },
  // 开发服务器配置
  server: {
    port: 3001, // Web 端端口号（区别于管理后台的 3000）

    // 允许通过 ngrok 等外部 Host 访问开发服务器
    // ngrok http 3001
    allowedHosts: ['lila-architraved-nonchromatically.ngrok-free.dev'],

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
