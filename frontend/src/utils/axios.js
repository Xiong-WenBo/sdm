import axios from 'axios'

// 创建 axios 实例
const instance = axios.create({
    baseURL: 'http://localhost:8080',  // 后端 API 基础地址
    timeout: 10000
})

// 请求拦截器：自动添加 token
instance.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器：处理 401 未授权
instance.interceptors.response.use(
    response => {
        return response
    },
    error => {
        if (error.response && error.response.status === 401) {
            localStorage.clear()
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default instance