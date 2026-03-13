<template>
    <div class="error-page">
        <div class="error-content">
            <h1 class="error-code">403</h1>
            <h2 class="error-title">无权限访问</h2>
            <p class="error-message">抱歉，您没有权限访问该页面</p>
            <el-button type="primary" @click="goHome">返回首页</el-button>
        </div>
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
    router.push('/')
}
</script>

<style scoped>
.error-page {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.error-content {
    text-align: center;
    color: #fff;
}

.error-code {
    font-size: 120px;
    font-weight: bold;
    margin-bottom: 20px;
}

.error-title {
    font-size: 32px;
    margin-bottom: 16px;
}

.error-message {
    font-size: 16px;
    margin-bottom: 32px;
    opacity: 0.8;
}
</style>
