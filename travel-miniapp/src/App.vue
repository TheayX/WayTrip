<script setup>
import { onLaunch, onShow, onHide, onError, onUnhandledRejection } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getRuntimeTrace, traceRuntime } from '@/utils/runtime-trace'

onLaunch(() => {
  traceRuntime('app-launch')
  // 初始化用户状态
  const userStore = useUserStore()
  userStore.initFromStorage()
})

onShow(() => {
  traceRuntime('app-show')
})

onHide(() => {
  traceRuntime('app-hide')
})

onError((error) => {
  traceRuntime('app-error', {
    error: typeof error === 'string' ? error : JSON.stringify(error),
    recentTrace: getRuntimeTrace().slice(-10)
  })
})

onUnhandledRejection((event) => {
  traceRuntime('app-unhandled-rejection', {
    reason: typeof event?.reason === 'string' ? event.reason : JSON.stringify(event?.reason || event),
    recentTrace: getRuntimeTrace().slice(-10)
  })
})
</script>

<style>
/* 全局样式 */
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #333;
}

/* 通用样式 */
.container {
  padding: 20rpx;
}

.flex {
  display: flex;
}

.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 主题色 */
.text-primary {
  color: #409EFF;
}

.bg-primary {
  background-color: #409EFF;
}

/* 价格样式 */
.price {
  color: #ff6b6b;
  font-weight: bold;
}

/* 卡片样式 */
.card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

/* 按钮样式 */
.btn-primary {
  background-color: #409EFF;
  color: #fff;
  border: none;
  border-radius: 8rpx;
  padding: 20rpx 40rpx;
  font-size: 28rpx;
}

.btn-primary:active {
  background-color: #3a8ee6;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
  color: #999;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
}
</style>
