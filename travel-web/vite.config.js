// Web 端 Vite 配置文件
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

const parseAllowedHosts = (value) => {
  if (!value) {
    return undefined
  }

  const hosts = value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  return hosts.length > 0 ? hosts : undefined
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'
  const devHost = env.VITE_DEV_HOST || undefined
  const allowedHosts = parseAllowedHosts(env.VITE_DEV_ALLOWED_HOSTS)

  return {
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

      // 默认不强绑特定 Host，只有在 Nginx 反代或内网穿透时再通过环境变量显式指定。
      ...(devHost ? { host: devHost } : {}),

      // 免费 ngrok 域名会频繁变化，因此允许的外部 Host 通过环境变量临时注入。
      ...(allowedHosts ? { allowedHosts } : {}),

      proxy: {
        // API 请求代理到后端服务器
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        },
        // 静态资源（图片等）代理到后端服务器
        '/uploads': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    }
  }
})
