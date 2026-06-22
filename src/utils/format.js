/**
 * 格式化价格（两位小数）
 */
export function formatPrice(price) {
  if (price == null) return '0.00'
  return Number(price).toFixed(2)
}

/**
 * 格式化日期时间
 */
export function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  const second = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

/**
 * 订单状态映射
 */
export const ORDER_STATUS_MAP = {
  'pending_payment': '待支付',
  'pending': '待接单',
  'accepted': '已接单',
  'preparing': '备餐中',
  'prepared': '待取餐',
  'delivering': '配送中',
  'delivered': '已送达',
  'completed': '已完成',
  'cancelled': '已取消',
  'refunded': '已退款',
  'exception': '异常',
  'rejected': '已拒收'
}

/**
 * 获取订单状态文本
 */
export function getStatusText(status) {
  return ORDER_STATUS_MAP[status] || status
}

/**
 * 订单状态颜色映射 (Element Plus tag type)
 */
export function getStatusType(status) {
  const map = {
    'pending_payment': 'warning',
    'pending': 'info',
    'accepted': '',
    'preparing': 'warning',
    'prepared': 'warning',
    'delivering': '',
    'delivered': 'success',
    'completed': 'success',
    'cancelled': 'danger',
    'refunded': 'danger',
    'exception': 'danger',
    'rejected': 'danger'
  }
  return map[status] || 'info'
}

/**
 * 角色名称映射
 */
export const ROLE_MAP = {
  'user': '用户',
  'merchant': '商家',
  'rider': '骑手',
  'admin': '管理员'
}

/**
 * 资质审核文件类型映射
 */
export const DOC_TYPE_MAP = {
  'business_license': '营业执照',
  'id_card': '身份证',
  'health_cert': '健康证'
}
