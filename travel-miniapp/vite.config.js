// 小程序端 Vite 配置文件
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  // uni-app 插件（支持多端编译）
  plugins: [uni()],
  // 转译依赖（解决 uni-ui 组件库兼容性问题）
  transpileDependencies: ['@dcloudio/uni-ui'],
  // CSS 预处理器配置（使用现代编译 API）
  css: {
    preprocessorOptions: {
      sass: {
        api: 'modern-compiler',
        silenceDeprecations: ['legacy-js-api']
      },
      scss: {
        api: 'modern-compiler',
        silenceDeprecations: ['legacy-js-api']
      }
    }
  }
})
