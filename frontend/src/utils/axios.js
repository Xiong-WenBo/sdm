import axios from 'axios'
import { ElMessage, ElLoading } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    }
})

// 统一错误提示映射
const ERROR_MESSAGES = {
    400: '请求参数错误',
    401: '登录已过期，请重新登录',
    403: '抱歉，您没有访问权限',
    404: '请求的资源不存在',
    405: '不支持的请求方法',
    500: '服务器内部错误，请稍后重试',
    502: '网关错误',
    503: '服务暂时不可用',
    504: '网关超时'
}

// 请求拦截器
instance.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        
        // 如果配置了 showLoading，显示全局 loading
        if (config.showLoading) {
            config.loadingInstance = ElLoading.service({
                lock: true,
                text: config.loadingText || '加载中...',
                background: 'rgba(0, 0, 0, 0.7)'
            })
        }
        
        return config
    },
    error => {
        console.error('请求错误:', error)
        ElMessage.error('请求发送失败：' + error.message)
        return Promise.reject(error)
    }
)

// 响应拦截器
instance.interceptors.response.use(
    response => {
        // 如果是 blob 类型（文件下载），直接返回
        if (response.config.responseType === 'blob') {
            return response
        }
        
        // 关闭 loading
        if (response.config.loadingInstance) {
            response.config.loadingInstance.close()
        }
        
        const res = response.data
        
        // 如果响应码不是 200，说明业务层面有错误
        if (res.code !== 200) {
            // 统一错误提示
            const message = res.message || ERROR_MESSAGES[res.code] || '请求失败'
            ElMessage.error(message)
            
            // 401：未授权，清除 token 并跳转登录页
            if (res.code === 401) {
                localStorage.clear()
                router.push('/login')
            }
            
            // 403：无权限，跳转到 403 页面（可选）
            if (res.code === 403) {
                // router.push('/403')  // 如果需要跳转 403 页面
            }
            
            return Promise.reject(new Error(message))
        }
        
        return res
    },
    error => {
        // HTTP 错误处理
        let message = '网络错误，请稍后重试'
        
        if (error.response) {
            // 服务器返回错误响应
            const status = error.response.status
            message = error.response.data?.message || ERROR_MESSAGES[status] || `请求失败 (${status})`
            
            // 特殊处理 401 和 403
            if (status === 401) {
                localStorage.clear()
                router.push('/login')
            }
        } else if (error.request) {
            // 请求已发送但没有收到响应
            message = '无法连接到服务器，请检查后端是否启动'
        } else {
            // 请求配置出错
            message = error.message || '请求配置错误'
        }
        
        console.error('HTTP 错误:', error)
        ElMessage.error(message)
        // 关闭 loading
        if (error.config && error.config.loadingInstance) {
            error.config.loadingInstance.close()
        }
        
        return Promise.reject(error)
    }
)

export default instance
