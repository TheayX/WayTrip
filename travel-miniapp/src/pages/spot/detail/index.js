const app = getApp()
const { getSpotDetail } = require('../../../api/spot')
const { addFavorite, removeFavorite } = require('../../../api/favorite')

Page({
  data: {
    spot: null,
    loading: true,
    currentImageIndex: 0
  },

  onLoad(options) {
    if (options.id) {
      this.loadSpotDetail(options.id)
    }
  },

  async loadSpotDetail(id) {
    try {
      const res = await getSpotDetail(id)
      this.setData({
        spot: res.data,
        loading: false
      })
      wx.setNavigationBarTitle({ title: res.data.name })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  // 图片轮播切换
  onSwiperChange(e) {
    this.setData({ currentImageIndex: e.detail.current })
  },

  // 预览图片
  previewImage(e) {
    const current = e.currentTarget.dataset.url
    wx.previewImage({
      current,
      urls: this.data.spot.images
    })
  },

  // 切换收藏
  async toggleFavorite() {
    if (!app.checkLogin()) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    const { spot } = this.data
    try {
      if (spot.isFavorite) {
        await removeFavorite(spot.id)
        wx.showToast({ title: '已取消收藏', icon: 'success' })
      } else {
        await addFavorite(spot.id)
        wx.showToast({ title: '收藏成功', icon: 'success' })
      }
      this.setData({ 'spot.isFavorite': !spot.isFavorite })
    } catch (e) {
      wx.showToast({ title: '操作失败', icon: 'none' })
    }
  },

  // 打开地图导航
  openNavigation() {
    const { spot } = this.data
    wx.openLocation({
      latitude: parseFloat(spot.latitude),
      longitude: parseFloat(spot.longitude),
      name: spot.name,
      address: spot.address
    })
  },

  // 立即预订
  goToOrder() {
    if (!app.checkLogin()) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/order/create/index?spotId=${this.data.spot.id}`
    })
  }
})
