// Vite 配置文件 - WayTrip 管理后台
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'

  return {
    // Vite 插件配置
    plugins: [
      // Vue 3 插件支持
      vue(),
      // 文档链接跟随当前代理目标，避免提示信息与实际联调地址不一致
      {
        name: 'api-doc-link',
        configureServer(server) {
          server.httpServer?.once('listening', () => {
            setTimeout(() => {
              console.log(`  \x1b[32m➜\x1b[0m  API 文档：  \x1b[36m${proxyTarget}/doc.html\x1b[0m`)
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
    build: {
      rollupOptions: {
        output: {
          // 按依赖域拆包，降低首屏主包体积，避免后台各模块共享大依赖全部堆进同一 chunk。
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return
            }

            if (id.includes('node_modules/vue/') || id.includes('node_modules/@vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) {
              return 'vue-vendor'
            }

            if (id.includes('node_modules/element-plus/') || id.includes('node_modules/@element-plus/')) {
              return 'element-plus'
            }

            if (id.includes('node_modules/echarts/')) {
              return 'echarts'
            }

            if (id.includes('node_modules/wangeditor/')) {
              return 'editor'
            }

            if (id.includes('node_modules/axios/')) {
              return 'network'
            }

            return 'vendor'
          }
        }
      }
    },
    // 开发服务器配置
    server: {
      port: 3000,
      proxy: {
        // 接口请求代理到后端服务
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        },
        // 上传后的静态资源也需要代理，否则管理端预览会跨到错误地址
        '/uploads': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    }
  }
})
