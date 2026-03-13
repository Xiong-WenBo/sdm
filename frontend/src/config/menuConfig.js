import { Role } from '@/utils/constants'

// 菜单配置
export const menus = [
    {
        path: '/home',
        name: 'Home',
        title: '首页',
        icon: 'HomeFilled',
        roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN, Role.COUNSELOR, Role.STUDENT]
    },
    {
        path: '/user',
        name: 'User',
        title: '用户管理',
        icon: 'User',
        roles: [Role.SUPER_ADMIN]
    },
    {
        path: '/building',
        name: 'Building',
        title: '楼栋管理',
        icon: 'OfficeBuilding',
        roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN]
    },
    {
        path: '/room',
        name: 'Room',
        title: '房间管理',
        icon: 'Grid',
        roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN]
    },
    {
        path: '/student',
        name: 'Student',
        title: '学生管理',
        icon: 'Avatar',
        roles: [Role.SUPER_ADMIN, Role.COUNSELOR]
    },
    {
        path: '/assignment',
        name: 'Assignment',
        title: '宿舍分配',
        icon: 'Ticket',
        roles: [Role.SUPER_ADMIN]
    },
    {
        path: '/attendance',
        name: 'Attendance',
        title: '查寝管理',
        icon: 'Document',
        roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN]
    },
    {
        path: '/repair',
        name: 'Repair',
        title: '报修管理',
        icon: 'Tools',
        roles: [Role.SUPER_ADMIN, Role.DORM_ADMIN, Role.STUDENT]
    },
    {
        path: '/leave',
        name: 'Leave',
        title: '请假管理',
        icon: 'Calendar',
        roles: [Role.SUPER_ADMIN, Role.COUNSELOR, Role.STUDENT]
    }
]

/**
 * 根据角色获取菜单
 * @param {string} role - 用户角色
 * @returns {Array} 过滤后的菜单列表
 */
export function getMenusByRole(role) {
    if (!role) return []
    return menus.filter(menu => menu.roles.includes(role))
}
