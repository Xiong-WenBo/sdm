import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const instance = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    }
})

// 请求拦截器
instance.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => {
        ElMessage.error('请求发送失败：' + error.message)
        return Promise.reject(error)
    }
)

// 响应拦截器
instance.interceptors.response.use(
    response => {
        const res = response.data
        
        // 如果响应码不是 200，说明业务层面有错误
        if (res.code !== 200) {
            ElMessage.error(res.message || '请求失败')
            
            // 401：未授权，清除 token 并跳转登录页
            if (res.code === 401) {
                localStorage.clear()
                router.push('/login')
            }
            
            // 403：无权限
            if (res.code === 403) {
                ElMessage.error('无权限访问')
            }
            
            return Promise.reject(new Error(res.message || '请求失败'))
        }
        
        return res
    },
    error => {
        // HTTP 错误处理
        let message = '网络错误，请稍后重试'
        
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    message = '未授权，请重新登录'
                    localStorage.clear()
                    router.push('/login')
                    break
                case 403:
                    message = '拒绝访问'
                    break
                case 404:
                    message = '请求资源不存在'
                    break
                case 500:
                    message = '服务器内部错误'
                    break
                default:
                    message = error.response.data?.message || `请求失败 (${error.response.status})`
            }
        } else if (error.request) {
            message = '无法连接到服务器，请检查后端是否启动'
        } else {
            message = error.message || '请求配置错误'
        }
        
        ElMessage.error(message)
        return Promise.reject(error)
    }
)

export default instance
