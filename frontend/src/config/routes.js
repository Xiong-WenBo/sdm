import { Role } from '@/utils/constants'

// 路由配置
const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/',
        name: 'Layout',
        component: () => import('../layouts/MainLayout.vue'),
        redirect: '/home',
        meta: { requiresAuth: true },
        children: [
            {
                path: 'home',
                name: 'Home',
                component: () => import('../views/Home.vue'),
                meta: { 
                    title: '首页',
                    icon: 'HomeFilled',
                    roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN, Role.COUNSELOR, Role.STUDENT]
                }
            }
        ]
    },
    // 403 无权限页面
    {
        path: '/403',
        name: 'Forbidden',
        component: () => import('../views/error/403.vue'),
        meta: { title: '无权限访问' }
    },
    // 404 页面
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('../views/error/404.vue'),
        meta: { title: '页面未找到' }
    }
]

export default routes
