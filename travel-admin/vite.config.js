// Vite 配置文件 - WayTrip 管理后台
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  // Vite 插件配置
  plugins: [
    // Vue 3 插件支持
    vue(),
    // 自定义插件：在开发服务器启动后显示 API 文档链接
    {
      name: 'api-doc-link',
      configureServer(server) {
        server.httpServer?.once('listening', () => {
          setTimeout(() => {
            console.log(`  \x1b[32m➜\x1b[0m  API 文档：  \x1b[36mhttp://localhost:8080/doc.html\x1b[0m`)
          }, 100)
        })
      }
    }
  ],
  // CSS 预处理器配置（使用现代编译 API）
  css: {
    preprocessorOptions: {
      sass: { api: 'modern-compiler' },
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
  // 开发服务器配置
  server: {
    port: 3000, // 端口号
    proxy: {
      // 代理配置：将 /api 请求转发到后端服务器
      '/api': {
        target: 'http://localhost:8080', // 后端地址
        changeOrigin: true // 修改请求头中的 Origin
      }
    }
  }
})
