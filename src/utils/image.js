const BASE_URL = ''

/**
 * 获取图片完整 URL
 * @param {string} path - 图片路径（如 /uploads/xxx 或完整 URL）
 * @returns {string} 完整 URL
 */
export function getImageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  return `${BASE_URL}${path}`
}
