import { createRouter, createWebHistory } from 'vue-router'

// 定义路由
const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { requiresAuth: true }   // 需要登录才能访问
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：检查是否已登录
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else if (to.path === '/login' && token) {
        next('/')
    } else {
        next()
    }
})

export default router