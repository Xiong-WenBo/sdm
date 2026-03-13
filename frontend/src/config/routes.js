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
            },
            {
                path: 'user',
                name: 'User',
                component: () => import('../views/user/index.vue'),
                meta: { 
                    title: '用户管理',
                    icon: 'User',
                    roles: [Role.SUPER_ADMIN]
                }
            },
            {
                path: 'building',
                name: 'Building',
                component: () => import('../views/building/index.vue'),
                meta: { 
                    title: '楼栋管理',
                    icon: 'OfficeBuilding',
                    roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN]
                }
            },
            {
                path: 'room',
                name: 'Room',
                component: () => import('../views/room/index.vue'),
                meta: { 
                    title: '房间管理',
                    icon: 'Grid',
                    roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN]
                }
            },
            {
                path: 'student',
                name: 'Student',
                component: () => import('../views/student/index.vue'),
                meta: { 
                    title: '学生管理',
                    icon: 'Avatar',
                    roles: [Role.SUPER_ADMIN, Role.COUNSELOR]
                }
            },
            {
                path: 'assignment',
                name: 'Assignment',
                component: () => import('../views/assignment/index.vue'),
                meta: { 
                    title: '宿舍分配',
                    icon: 'Ticket',
                    roles: [Role.SUPER_ADMIN]
                }
            },
            {
                path: 'attendance',
                name: 'Attendance',
                component: () => import('../views/attendance/index.vue'),
                meta: { 
                    title: '查寝管理',
                    icon: 'Document',
                    roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN]
                }
            },
            {
                path: 'repair',
                name: 'Repair',
                component: () => import('../views/repair/index.vue'),
                meta: { 
                    title: '报修管理',
                    icon: 'Tools',
                    roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN, Role.STUDENT]
                }
            },
            {
                path: 'leave',
                name: 'Leave',
                component: () => import('../views/leave/index.vue'),
                meta: { 
                    title: '请假管理',
                    icon: 'Calendar',
                    roles: [Role.SUPER_ADMIN, Role.COUNSELOR, Role.STUDENT]
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
