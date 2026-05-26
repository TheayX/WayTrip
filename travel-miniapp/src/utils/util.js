// 通用展示格式化工具，集中维护日期、金额和订单状态等基础文案格式规则。
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

const formatPrice = (price) => {
  // 金额展示统一保留两位小数，避免 0、10、10.5 在不同页面表现不一致。
  if (price === undefined || price === null) return '0.00'
  return Number(price).toFixed(2)
}

const orderStatusText = {
  'PENDING_PAYMENT': '待支付',
  'PENDING_USE': '待使用',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
}

const getOrderStatusText = (status) => {
  // 未识别状态直接回传原值，方便在异常数据场景下仍能看到后端真实状态码。
  return orderStatusText[status] || status
}

export {
  formatDate,
  formatPrice,
  getOrderStatusText
}
