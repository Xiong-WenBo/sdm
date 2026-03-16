import { ElLoading } from 'element-plus'

let loadingInstance = null
let loadingCount = 0

/**
 * 显示全局 Loading
 * @param {string} text - Loading 提示文字
 */
export const showLoading = (text = '加载中...') => {
    loadingCount++
    if (loadingCount === 1) {
        loadingInstance = ElLoading.service({
            lock: true,
            text: text,
            background: 'rgba(0, 0, 0, 0.7)',
            spinner: 'el-icon-loading'
        })
    }
}

/**
 * 隐藏全局 Loading
 */
export const hideLoading = () => {
    loadingCount--
    if (loadingCount <= 0) {
        if (loadingInstance) {
            loadingInstance.close()
            loadingInstance = null
        }
        loadingCount = 0
    }
}

/**
 * 强制关闭所有 Loading
 */
export const closeAllLoading = () => {
    if (loadingInstance) {
        loadingInstance.close()
        loadingInstance = null
    }
    loadingCount = 0
}

export default {
    showLoading,
    hideLoading,
    closeAllLoading
}
