import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { showToast } from 'vant'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true // 携带 Session
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 可以在这里添加 token
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers['Authorization'] = `Bearer ${token}`
    // }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // 根据实际后端返回结构调整
    if (res.code !== 0) {
      showToast({
        message: res.message || '请求失败',
        type: 'fail'
      })
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      switch (error.response.status) {
        case 401:
          showToast({
            message: '未授权，请重新登录',
            type: 'fail'
          })
          // 可以跳转到登录页
          break
        case 403:
          showToast({
            message: '拒绝访问',
            type: 'fail'
          })
          break
        case 404:
          showToast({
            message: '请求的资源不存在',
            type: 'fail'
          })
          break
        case 500:
          showToast({
            message: '服务器错误',
            type: 'fail'
          })
          break
        default:
          showToast({
            message: error.response.data?.message || '请求失败',
            type: 'fail'
          })
      }
    } else if (error.request) {
      showToast({
        message: '网络连接失败',
        type: 'fail'
      })
    } else {
      showToast({
        message: error.message || '请求失败',
        type: 'fail'
      })
    }

    return Promise.reject(error)
  }
)

// 封装请求方法
export default {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  }
}
