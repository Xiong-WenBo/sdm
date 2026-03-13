// 角色枚举
export const Role = {
    SUPER_ADMIN: 'SUPER_ADMIN',      // 超级管理员
    DORM_ADMIN: 'DORM_ADMIN',        // 宿舍管理员
    COUNSELOR: 'COUNSELOR',          // 辅导员
    STUDENT: 'STUDENT'               // 学生
}

// 角色中文映射
export const RoleName = {
    [Role.SUPER_ADMIN]: '超级管理员',
    [Role.DORM_ADMIN]: '宿舍管理员',
    [Role.COUNSELOR]: '辅导员',
    [Role.STUDENT]: '学生'
}
