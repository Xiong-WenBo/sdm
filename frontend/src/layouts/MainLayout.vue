<template>
    <div class="main-layout">
        <el-container>
            <!-- 左侧边栏 -->
            <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
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
                    <el-menu-item index="/home">
                        <el-icon><HomeFilled /></el-icon>
                        <template #title>首页</template>
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
                    </div>
                    <div class="header-right">
                        <div class="user-info">
                            <el-dropdown>
                                <div class="user-name">
                                    <el-avatar :size="32" :icon="User" />
                                    <span class="username">{{ userName }}</span>
                                    <el-icon><ArrowDown /></el-icon>
                                </div>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item>个人中心</el-dropdown-item>
                                        <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
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
import { User, ArrowDown, Fold, Expand, HomeFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const userName = ref('')

const activeMenu = computed(() => route.path)

const toggleCollapse = () => {
    isCollapse.value = !isCollapse.value
}

const handleLogout = () => {
    localStorage.clear()
    router.push('/login')
}

onMounted(() => {
    userName.value = localStorage.getItem('realName') || localStorage.getItem('username') || '用户'
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
}

.logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #2b3a4b;
}

.logo-img {
    width: 32px;
    height: 32px;
    margin-right: 10px;
}

.logo-text {
    color: #fff;
    font-size: 18px;
    font-weight: bold;
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
}

.collapse-icon {
    font-size: 20px;
    cursor: pointer;
    transition: all 0.3s;
}

.collapse-icon:hover {
    color: #409EFF;
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
}

.username {
    margin: 0 8px;
}

.main-content {
    background-color: #f0f2f5;
    padding: 20px;
}
</style>
