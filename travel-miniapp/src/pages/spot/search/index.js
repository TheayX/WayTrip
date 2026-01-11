const { searchSpots } = require('../../../api/spot')

Page({
  data: {
    keyword: '',
    spots: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    searched: false
  },

  onLoad() {
    // 聚焦搜索框
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && this.data.keyword) {
      this.loadData()
    }
  },

  // 输入关键词
  onInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  // 搜索
  onSearch() {
    const { keyword } = this.data
    if (!keyword.trim()) {
      wx.showToast({ title: '请输入关键词', icon: 'none' })
      return
    }
    this.setData({
      page: 1,
      spots: [],
      hasMore: true,
      searched: true
    })
    this.loadData()
  },

  // 清空
  onClear() {
    this.setData({
      keyword: '',
      spots: [],
      searched: false
    })
  },

  async loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })

    try {
      const { keyword, page, pageSize, spots } = this.data
      const res = await searchSpots(keyword, page, pageSize)

      const newSpots = page === 1 ? res.data.list : [...spots, ...res.data.list]
      this.setData({
        spots: newSpots,
        page: page + 1,
        hasMore: newSpots.length < res.data.total
      })
    } catch (e) {
      console.error('搜索失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 跳转到详情页
  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/spot/detail/index?id=${id}` })
  }
})
