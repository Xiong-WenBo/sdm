<template>
  <el-container>
    <el-header>
      <el-row justify="space-between" align="middle">
        <h2>宿舍管理系统 - 首页</h2>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </el-row>
    </el-header>
    <el-main>
      <el-alert :title="'欢迎回来，' + realName" type="success" show-icon></el-alert>
      <p>你的角色：{{ role }}</p>
      <p>用户名：{{ username }}</p>
      <el-button @click="testAuth">测试认证接口</el-button>
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import axios from '../utils/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const username = localStorage.getItem('username')
const realName = localStorage.getItem('realName')
const role = localStorage.getItem('role')

const logout = () => {
  localStorage.clear()
  router.push('/login')
}

const testAuth = async () => {
  try {
    const res = await axios.get('/api/auth/test')
    ElMessage.success(res.data)
  } catch (error) {
    ElMessage.error('请求失败')
  }
}
</script>