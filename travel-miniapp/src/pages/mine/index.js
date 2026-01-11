const app = getApp()
const { wxLogin, getUserInfo } = require('../../api/auth')

Page({
  data: {
    userInfo: null,
    isLogin: false,
    preferences: []
  },

  onShow() {
    this.checkLogin()
  },

  checkLogin() {
    const isLogin = app.checkLogin()
    this.setData({ isLogin })
    if (isLogin) {
      this.loadUserInfo()
    }
  },

  async loadUserInfo() {
    try {
      const res = await getUserInfo()
      this.setData({
        userInfo: res.data,
        preferences: res.data.preferences || []
      })
      app.globalData.userInfo = res.data
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  },

  // 微信登录
  async handleLogin() {
    try {
      wx.showLoading({ title: '登录中...', mask: true })
      const loginRes = await wx.login()
      const res = await wxLogin(loginRes.code)
      
      app.setToken(res.data.token)
      app.globalData.userInfo = res.data.user
      
      this.setData({
        isLogin: true,
        userInfo: res.data.user
      })
      
      wx.hideLoading()
      wx.showToast({ title: '登录成功', icon: 'success' })
      
      // 如果是新用户，引导设置偏好
      if (res.data.user.isNewUser) {
        this.showPreferenceGuide()
      }
    } catch (e) {
      wx.hideLoading()
      wx.showToast({ title: '登录失败', icon: 'none' })
    }
  },

  // 显示偏好设置引导
  showPreferenceGuide() {
    wx.showModal({
      title: '设置偏好',
      content: '设置您的旅游偏好，获取更精准的推荐',
      confirmText: '去设置',
      cancelText: '稍后',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/mine/preference/index' })
        }
      }
    })
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.clearLogin()
          this.setData({
            isLogin: false,
            userInfo: null,
            preferences: []
          })
          wx.showToast({ title: '已退出登录', icon: 'success' })
        }
      }
    })
  },

  // 跳转到订单列表
  goToOrders() {
    if (!this.data.isLogin) {
      return this.handleLogin()
    }
    wx.navigateTo({ url: '/pages/order/list/index' })
  },

  // 跳转到收藏列表
  goToFavorites() {
    if (!this.data.isLogin) {
      return this.handleLogin()
    }
    wx.navigateTo({ url: '/pages/favorite/index' })
  },

  // 联系客服
  handleContact() {
    wx.showModal({
      title: '联系客服',
      content: '客服电话：400-123-4567',
      showCancel: false
    })
  }
})
