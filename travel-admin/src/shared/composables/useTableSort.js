/**
 * 管理端表格排序状态工具。
 */
export const TABLE_SORT_ORDERS = ['ascending', 'descending']

export const applySortChange = (queryParams, { prop, order }) => {
  // Element Plus 的空排序态不参与请求，后端会自动回落到默认排序。
  queryParams.sortBy = order ? prop : ''
  queryParams.sortOrder = order === 'descending' ? 'desc' : (order ? 'asc' : '')
  queryParams.page = 1
}

