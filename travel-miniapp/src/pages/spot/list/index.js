const { getSpotList, getFilters } = require('../../../api/spot')

Page({
  data: {
    spots: [],
    regions: [],
    categories: [],
    currentRegion: null,
    currentCategory: null,
    sortBy: 'heat',
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    showFilter: false
  },

  onLoad() {
    this.loadFilters()
    this.loadData()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, spots: [], hasMore: true })
    this.loadData().then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadData()
    }
  },

  async loadFilters() {
    try {
      const res = await getFilters()
      this.setData({
        regions: res.data.regions,
        categories: res.data.categories
      })
    } catch (e) {
      console.error('加载筛选项失败', e)
    }
  },

  async loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })

    try {
      const { page, pageSize, currentRegion, currentCategory, sortBy, spots } = this.data
      const res = await getSpotList({
        page,
        pageSize,
        regionId: currentRegion,
        categoryId: currentCategory,
        sortBy
      })

      const newSpots = page === 1 ? res.data.list : [...spots, ...res.data.list]
      this.setData({
        spots: newSpots,
        page: page + 1,
        hasMore: newSpots.length < res.data.total
      })
    } catch (e) {
      console.error('加载景点失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 切换筛选面板
  toggleFilter() {
    this.setData({ showFilter: !this.data.showFilter })
  },

  // 选择地区
  selectRegion(e) {
    const regionId = e.currentTarget.dataset.id
    this.setData({
      currentRegion: regionId === this.data.currentRegion ? null : regionId,
      page: 1,
      spots: [],
      hasMore: true
    })
    this.loadData()
  },

  // 选择分类
  selectCategory(e) {
    const categoryId = e.currentTarget.dataset.id
    this.setData({
      currentCategory: categoryId === this.data.currentCategory ? null : categoryId,
      page: 1,
      spots: [],
      hasMore: true
    })
    this.loadData()
  },

  // 切换排序
  changeSort(e) {
    const sortBy = e.currentTarget.dataset.sort
    if (sortBy === this.data.sortBy) return
    this.setData({
      sortBy,
      page: 1,
      spots: [],
      hasMore: true
    })
    this.loadData()
  },

  // 跳转到搜索页
  goToSearch() {
    wx.navigateTo({ url: '/pages/spot/search/index' })
  },

  // 跳转到详情页
  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/spot/detail/index?id=${id}` })
  }
})
