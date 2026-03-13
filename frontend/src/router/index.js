import { createRouter, createWebHistory } from 'vue-router'
import routes from '@/config/routes'
import { Role } from '@/utils/constants'

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：完善登录检查和角色权限验证
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    const userRole = localStorage.getItem('role')
    
    // 设置页面标题
    document.title = to.meta.title ? `${to.meta.title} - 宿舍管理系统` : '宿舍管理系统'
    
    // 需要登录验证的路由
    if (to.meta.requiresAuth) {
        // 未登录，跳转到登录页
        if (!token) {
            next('/login')
            return
        }
        
        // 检查角色权限
        if (to.meta.roles && Array.isArray(to.meta.roles)) {
            if (!userRole || !to.meta.roles.includes(userRole)) {
                next('/403')
                return
            }
        }
    }
    
    // 已登录访问登录页，重定向到首页
    if (to.path === '/login' && token) {
        next('/')
        return
    }
    
    next()
})

export default router