const { get, post } = require('../../utils/request')

Page({
  data: {
    banners: [],
    hotSpots: [],
    recommendations: [],
    showPreferenceGuide: false,
    loading: false
  },

  onLoad() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      await Promise.all([
        this.loadBanners(),
        this.loadHotSpots(),
        this.loadRecommendations()
      ])
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadBanners() {
    try {
      const res = await get('/home/banners', {}, { showLoading: false })
      this.setData({ banners: res.data || [] })
    } catch (e) {
      console.error('加载轮播图失败', e)
    }
  },

  async loadHotSpots() {
    try {
      const res = await get('/home/hot', { limit: 6 }, { showLoading: false })
      this.setData({ hotSpots: res.data || [] })
    } catch (e) {
      console.error('加载热门推荐失败', e)
    }
  },

  async loadRecommendations() {
    const app = getApp()
    if (!app.checkLogin()) {
      return
    }
    try {
      const res = await get('/recommendations', { limit: 6 }, { showLoading: false })
      this.setData({
        recommendations: res.data?.spots || [],
        showPreferenceGuide: res.data?.showPreferenceGuide || false
      })
    } catch (e) {
      console.error('加载个性化推荐失败', e)
    }
  },

  // 点击轮播图
  onBannerTap(e) {
    const { spotId } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/spot/detail/index?id=${spotId}`
    })
  },

  // 点击景点
  onSpotTap(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/spot/detail/index?id=${id}`
    })
  },

  // 刷新推荐
  async onRefreshRecommendations() {
    try {
      const res = await post('/recommendations/refresh', { limit: 6 })
      this.setData({ recommendations: res.data?.spots || [] })
      wx.showToast({ title: '已刷新', icon: 'success' })
    } catch (e) {
      console.error('刷新推荐失败', e)
    }
  }
})
