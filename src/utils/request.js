import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const BASE_URL = ''

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
})

// 请求拦截器 - 附带 Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - ApiResponse 解包 & Token 刷新 & 401 处理
let isRefreshing = false
let pendingRequests = []

const processPendingRequests = (token) => {
  pendingRequests.forEach(cb => cb(token))
  pendingRequests = []
}

const addPendingRequest = (cb) => {
  pendingRequests.push(cb)
}

request.interceptors.response.use(
  response => {
    // 解包后端 ApiResponse<T> 统一响应格式: { code, message, data }
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        response.data = body.data
      } else {
        // 业务错误码 — 交由调用方处理
        return Promise.reject({ code: body.code, message: body.message || '请求失败' })
      }
    }
    return response
  },
  async error => {
    const originalRequest = error.config

    // 处理业务错误码（HTTP 200 但 code !== 200）
    // 此分支在上方成功拦截器已处理，通过 Promise.reject 传递到此

    if (error.response?.status === 401 && !originalRequest._retry) {
      // 排除登录和注册接口
      if (originalRequest.url?.includes('/api/auth/login') || originalRequest.url?.includes('/api/auth/register')) {
        return Promise.reject(error)
      }

      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        localStorage.clear()
        router.push('/login')
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise(resolve => {
          addPendingRequest(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 刷新 Token 的响应也会被成功拦截器解包，所以 res.data 直接就是 { accessToken }
        const res = await axios.post('/api/auth/refresh', null, {
          headers: { Authorization: `Bearer ${refreshToken}` }
        })
        // 解包 ApiResponse
        const refreshData = res.data?.data || res.data
        const newAccessToken = refreshData?.accessToken || refreshData?.accessToken
        localStorage.setItem('accessToken', newAccessToken)
        processPendingRequests(newAccessToken)
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return request(originalRequest)
      } catch (refreshError) {
        processPendingRequests(null)
        localStorage.clear()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 其他 HTTP 错误统一提示
    // 优先提取后端 ApiResponse 或 Spring Boot 默认错误格式中的消息
    let msg = '请求失败'
    const respData = error.response?.data
    if (respData) {
      if (typeof respData === 'string') {
        msg = respData.substring(0, 200)
      } else {
        msg = respData.message || respData.error || msg
      }
    }
    const statusCode = error.response?.status || 0
    const fullMsg = statusCode ? `[${statusCode}] ${msg}` : msg
    if (error.response?.status !== 401) {
      ElMessage.error(fullMsg)
    }
    return Promise.reject(error)
  }
)

export default request
