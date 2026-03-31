import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import router from '@/router'

const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    }
})

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

const ENGLISH_MESSAGE_MAP = [
    { keyword: 'Failed to obtain JDBC Connection', message: '无法连接数据库，请检查数据库服务和连接配置' },
    { keyword: 'Access denied for user', message: '数据库账号或密码错误，请检查数据库配置' },
    { keyword: 'Duplicate entry', message: '数据已存在，请勿重复提交' },
    { keyword: 'No active counselors available for assignment', message: '没有可用的辅导员，请先检查辅导员账号状态' },
    { keyword: 'No counselor available', message: '当前没有可用的辅导员' },
    { keyword: 'Building is required', message: '请选择楼栋' },
    { keyword: 'Total floors must be greater than 0', message: '总楼层必须大于 0' },
    { keyword: 'Rooms per floor must be greater than 0', message: '每层房间数量必须大于 0' },
    { keyword: 'Capacity must be greater than 0', message: '房间容量必须大于 0' }
]

function containsChinese(text) {
    return /[\u4e00-\u9fa5]/.test(text || '')
}

function normalizeMessage(message, fallback) {
    if (!message) {
        return fallback
    }
    if (containsChinese(message)) {
        return message
    }

    const matched = ENGLISH_MESSAGE_MAP.find(item => message.includes(item.keyword))
    if (matched) {
        return matched.message
    }

    return fallback
}

instance.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }

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
        ElMessage.error('请求发送失败，请稍后重试')
        return Promise.reject(error)
    }
)

instance.interceptors.response.use(
    response => {
        if (response.config.responseType === 'blob') {
            return response
        }

        if (response.config.loadingInstance) {
            response.config.loadingInstance.close()
        }

        const res = response.data
        if (res.code !== 200) {
            const fallback = ERROR_MESSAGES[res.code] || '请求失败，请稍后重试'
            const message = normalizeMessage(res.message, fallback)
            ElMessage.error(message)

            if (res.code === 401) {
                localStorage.clear()
                router.push('/login')
            }

            return Promise.reject(new Error(message))
        }

        return res
    },
    error => {
        let message = '网络错误，请稍后重试'

        if (error.response) {
            const status = error.response.status
            const fallback = ERROR_MESSAGES[status] || `请求失败（${status}）`
            message = normalizeMessage(error.response.data?.message, fallback)

            if (status === 401) {
                localStorage.clear()
                router.push('/login')
            }
        } else if (error.request) {
            message = '无法连接到服务器，请检查后端服务是否启动'
        } else {
            message = normalizeMessage(error.message, '请求配置错误，请稍后重试')
        }

        console.error('HTTP 错误:', error)
        ElMessage.error(message)

        if (error.config && error.config.loadingInstance) {
            error.config.loadingInstance.close()
        }

        return Promise.reject(error)
    }
)

export default instance
