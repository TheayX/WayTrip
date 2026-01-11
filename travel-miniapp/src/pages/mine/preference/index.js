const app = getApp()
const { setPreferences, getUserInfo } = require('../../../api/auth')

Page({
  data: {
    allTags: ['自然风光', '历史文化', '主题乐园', '海滨度假', '古镇古村', '名山大川', '博物馆', '美食之旅'],
    selectedTags: []
  },

  onLoad() {
    this.loadUserPreferences()
  },

  async loadUserPreferences() {
    try {
      const res = await getUserInfo()
      if (res.data.preferences && res.data.preferences.length > 0) {
        this.setData({ selectedTags: res.data.preferences })
      }
    } catch (e) {
      console.error('获取偏好失败', e)
    }
  },

  toggleTag(e) {
    const tag = e.currentTarget.dataset.tag
    const { selectedTags } = this.data
    const index = selectedTags.indexOf(tag)
    
    if (index > -1) {
      selectedTags.splice(index, 1)
    } else {
      if (selectedTags.length >= 5) {
        wx.showToast({ title: '最多选择5个标签', icon: 'none' })
        return
      }
      selectedTags.push(tag)
    }
    
    this.setData({ selectedTags })
  },

  async handleSave() {
    const { selectedTags } = this.data
    if (selectedTags.length === 0) {
      wx.showToast({ title: '请至少选择一个标签', icon: 'none' })
      return
    }

    try {
      wx.showLoading({ title: '保存中...', mask: true })
      await setPreferences(selectedTags)
      wx.hideLoading()
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (e) {
      wx.hideLoading()
      wx.showToast({ title: '保存失败', icon: 'none' })
    }
  }
})
