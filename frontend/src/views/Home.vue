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
                    <el-col :span="24">
                        <div class="welcome-text">
                            <h2>👋 你好，{{ realName }}！</h2>
                            <p class="welcome-desc">
                                欢迎使用宿舍管理系统，{{ getRoleName(role) }}。
                                您可以通过左侧菜单访问相应的功能模块。
                            </p>
                        </div>
                    </el-col>
                </el-row>

                <!-- 角色专属快捷入口 -->
                <el-row :gutter="20" style="margin-top: 30px">
                    <el-col :span="24">
                        <h3 class="section-title">快捷入口</h3>
                    </el-col>
                    
                    <!-- 超级管理员快捷入口 -->
                    <template v-if="role === 'SUPER_ADMIN'">
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/user')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><User /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>用户管理</h4>
                                    <p>管理系统用户</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/building')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><OfficeBuilding /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>楼栋管理</h4>
                                    <p>管理宿舍楼栋</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/student')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Avatar /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>学生管理</h4>
                                    <p>管理学生信息</p>
                                </div>
                            </el-card>
                        </el-col>
                    </template>

                    <!-- 宿管快捷入口 -->
                    <template v-if="role === 'DORM_ADMIN'">
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/room')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Grid /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>房间管理</h4>
                                    <p>管理本楼栋房间</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/attendance')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Document /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>查寝管理</h4>
                                    <p>录入查寝记录</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/repair')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Tools /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>报修管理</h4>
                                    <p>处理报修申请</p>
                                </div>
                            </el-card>
                        </el-col>
                    </template>

                    <!-- 辅导员快捷入口 -->
                    <template v-if="role === 'COUNSELOR'">
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/student')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Avatar /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>学生管理</h4>
                                    <p>管理班级学生</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/attendance')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Document /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>查寝管理</h4>
                                    <p>查看学生查寝</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/leave')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Calendar /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>请假管理</h4>
                                    <p>审批学生请假</p>
                                </div>
                            </el-card>
                        </el-col>
                    </template>

                    <!-- 学生快捷入口 -->
                    <template v-if="role === 'STUDENT'">
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/repair')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Tools /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>报修管理</h4>
                                    <p>提交报修申请</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/leave')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><Calendar /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>请假管理</h4>
                                    <p>提交请假申请</p>
                                </div>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover" class="quick-card" @click="navigateTo('/profile')">
                                <div class="quick-icon">
                                    <el-icon :size="40"><User /></el-icon>
                                </div>
                                <div class="quick-info">
                                    <h4>个人中心</h4>
                                    <p>查看个人信息</p>
                                </div>
                            </el-card>
                        </el-col>
                    </template>
                </el-row>
            </div>
        </el-card>
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Role, RoleName } from '@/utils/constants'
import { User, OfficeBuilding, Avatar, Grid, Document, Tools, Calendar } from '@element-plus/icons-vue'

const router = useRouter()

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

const navigateTo = (path) => {
    router.push(path)
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

.welcome-text {
    padding: 20px 0;
}

.welcome-text h2 {
    margin: 0 0 10px 0;
    color: #303133;
}

.welcome-desc {
    margin: 0;
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
}

.section-title {
    margin: 0 0 20px 0;
    color: #303133;
    font-size: 16px;
}

.quick-card {
    cursor: pointer;
    transition: all 0.3s;
    margin-bottom: 20px;
}

.quick-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.quick-card .quick-icon {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 80px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 8px;
    color: #fff;
    margin-bottom: 15px;
}

.quick-card .quick-info {
    text-align: center;
}

.quick-card .quick-info h4 {
    margin: 0 0 8px 0;
    color: #303133;
    font-size: 16px;
}

.quick-card .quick-info p {
    margin: 0;
    color: #909399;
    font-size: 13px;
}
</style>
