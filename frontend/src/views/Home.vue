<template>
    <div class="home">
        <el-card class="welcome-card">
            <template #header>
                <div class="card-header">
                    <span>欢迎使用宿舍管理系统</span>
                </div>
            </template>
            <div class="welcome-content">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-descriptions title="用户信息" :column="1" border>
                            <el-descriptions-item label="用户名">{{ username }}</el-descriptions-item>
                            <el-descriptions-item label="姓名">{{ realName }}</el-descriptions-item>
                            <el-descriptions-item label="角色">
                                <el-tag :type="getRoleType(role)">{{ getRoleName(role) }}</el-tag>
                            </el-descriptions-item>
                        </el-descriptions>
                    </el-col>
                    <el-col :span="12">
                        <div class="quick-actions">
                            <h3>快捷操作</h3>
                            <el-space direction="vertical">
                                <el-button type="primary" @click="testAuth">测试认证接口</el-button>
                            </el-space>
                        </div>
                    </el-col>
                </el-row>
            </div>
        </el-card>
    </div>
</template>

<script setup>
import { Role, RoleName } from '@/utils/constants'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const username = localStorage.getItem('username')
const realName = localStorage.getItem('realName')
const role = localStorage.getItem('role')

const getRoleName = (roleKey) => {
    return RoleName[roleKey] || roleKey
}

const getRoleType = (roleKey) => {
    const typeMap = {
        [Role.SUPER_ADMIN]: 'danger',
        [Role.DORM_ADMIN]: 'warning',
        [Role.COUNSELOR]: 'success',
        [Role.STUDENT]: 'info'
    }
    return typeMap[roleKey] || 'info'
}

const testAuth = async () => {
    try {
        const res = await axios.get('/api/auth/test')
        ElMessage.success(res.data)
    } catch (error) {
        // 错误已在 axios 拦截器中处理
    }
}
</script>

<style scoped>
.home {
    padding: 20px;
}

.welcome-card {
    margin-bottom: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.welcome-content {
    padding: 10px 0;
}

.quick-actions h3 {
    margin-bottom: 16px;
}
</style>
