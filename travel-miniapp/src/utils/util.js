/**
 * 格式化日期
 */
const formatDate = (date, fmt = 'YYYY-MM-DD') => {
  if (!date) return ''
  if (typeof date === 'string') {
    date = new Date(date)
  }
  const o = {
    'M+': date.getMonth() + 1,
    'D+': date.getDate(),
    'H+': date.getHours(),
    'm+': date.getMinutes(),
    's+': date.getSeconds()
  }
  if (/(Y+)/.test(fmt)) {
    fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length))
  }
  for (let k in o) {
    if (new RegExp('(' + k + ')').test(fmt)) {
      fmt = fmt.replace(RegExp.$1, RegExp.$1.length === 1 ? o[k] : ('00' + o[k]).substr(('' + o[k]).length))
    }
  }
  return fmt
}

/**
 * 格式化价格
 */
const formatPrice = (price) => {
  if (price === undefined || price === null) return '0.00'
  return Number(price).toFixed(2)
}

/**
 * 订单状态文本
 */
const orderStatusText = {
  'PENDING_PAYMENT': '待支付',
  'PENDING_USE': '待使用',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
}

const getOrderStatusText = (status) => {
  return orderStatusText[status] || status
}

module.exports = {
  formatDate,
  formatPrice,
  getOrderStatusText
}
