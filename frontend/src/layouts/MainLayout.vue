<template>
    <div class="main-layout">
        <el-container>
            <!-- 左侧边栏 -->
            <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
                <div class="logo">
                    <img v-if="!isCollapse" src="@/assets/vite.svg" alt="Logo" class="logo-img">
                    <span v-if="!isCollapse" class="logo-text">宿舍管理系统</span>
                </div>
                <el-menu
                    :default-active="activeMenu"
                    background-color="#304156"
                    text-color="#bfcbd9"
                    active-text-color="#409EFF"
                    :collapse="isCollapse"
                    :collapse-transition="false"
                    router
                >
                    <el-menu-item
                        v-for="menu in menuList"
                        :key="menu.path"
                        :index="menu.path"
                    >
                        <el-icon>
                            <component :is="menu.icon" />
                        </el-icon>
                        <template #title>
                            <span>{{ menu.title }}</span>
                        </template>
                    </el-menu-item>
                </el-menu>
            </el-aside>

            <!-- 主体内容 -->
            <el-container>
                <!-- 顶部导航 -->
                <el-header class="header">
                    <div class="header-left">
                        <el-icon class="collapse-icon" @click="toggleCollapse">
                            <Fold v-if="!isCollapse" />
                            <Expand v-else />
                        </el-icon>
                        <span class="breadcrumb">
                            <el-icon><Location /></el-icon>
                            {{ currentMenuTitle }}
                        </span>
                    </div>
                    <div class="header-right">
                        <div class="user-info">
                            <el-dropdown>
                                <div class="user-name">
                                    <el-avatar :size="32" :icon="User" />
                                    <span class="username">{{ userName }}</span>
                                    <el-tag :type="roleType" size="small" effect="plain">{{ roleName }}</el-tag>
                                    <el-icon><ArrowDown /></el-icon>
                                </div>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="goToProfile">
                                            <el-icon><User /></el-icon>
                                            个人中心
                                        </el-dropdown-item>
                                        <el-dropdown-item divided @click="handleLogout">
                                            <el-icon><SwitchButton /></el-icon>
                                            退出登录
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </div>
                </el-header>

                <!-- 主内容区 -->
                <el-main class="main-content">
                    <router-view />
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
    User,
    ArrowDown,
    Fold,
    Expand,
    HomeFilled,
    Location,
    SwitchButton,
    OfficeBuilding,
    Grid,
    Avatar,
    Ticket,
    Document,
    Tools,
    Calendar
} from '@element-plus/icons-vue'
import { getMenusByRole } from '@/config/menuConfig'
import { Role, RoleName } from '@/utils/constants'

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const userName = ref('')
const userRole = ref('')

const activeMenu = computed(() => route.path)

const menuList = computed(() => {
    return getMenusByRole(userRole.value)
})

const currentMenuTitle = computed(() => {
    const menu = menuList.value.find(m => m.path === route.path)
    return menu ? menu.title : '首页'
})

const roleName = computed(() => {
    return RoleName[userRole.value] || userRole.value
})

const roleType = computed(() => {
    const typeMap = {
        [Role.SUPER_ADMIN]: 'danger',
        [Role.DORM_ADMIN]: 'warning',
        [Role.COUNSELOR]: 'success',
        [Role.STUDENT]: 'info'
    }
    return typeMap[userRole.value] || 'info'
})

const toggleCollapse = () => {
    isCollapse.value = !isCollapse.value
}

const handleLogout = () => {
    localStorage.clear()
    router.push('/login')
}

const goToProfile = () => {
    router.push('/profile')
}

onMounted(() => {
    userName.value = localStorage.getItem('realName') || localStorage.getItem('username') || '用户'
    userRole.value = localStorage.getItem('role') || ''
})
</script>

<style scoped>
.main-layout {
    height: 100vh;
}

.el-container {
    height: 100%;
}

.sidebar {
    background-color: #304156;
    transition: width 0.28s;
    overflow: hidden;
    box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
}

.logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #2b3a4b;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-img {
    width: 32px;
    height: 32px;
    margin-right: 10px;
}

.logo-text {
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    white-space: nowrap;
}

.header {
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 16px;
}

.collapse-icon {
    font-size: 20px;
    cursor: pointer;
    transition: all 0.3s;
    color: #606266;
}

.collapse-icon:hover {
    color: #409EFF;
}

.breadcrumb {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #606266;
    font-size: 14px;
}

.header-right {
    display: flex;
    align-items: center;
}

.user-info {
    cursor: pointer;
}

.user-name {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 4px;
    transition: background-color 0.3s;
}

.user-name:hover {
    background-color: #f5f7fa;
}

.username {
    margin: 0 8px;
    font-weight: 500;
}

.main-content {
    background-color: #f0f2f5;
    padding: 20px;
    overflow-y: auto;
}
</style>
