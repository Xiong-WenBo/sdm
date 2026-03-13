<template>
    <div class="profile">
        <el-row :gutter="20">
            <!-- 个人信息卡片 -->
            <el-col :span="12">
                <el-card>
                    <template #header>
                        <div class="card-header">
                            <span>个人信息</span>
                        </div>
                    </template>
                    <el-descriptions :column="1" border>
                        <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
                        <el-descriptions-item label="真实姓名">{{ userInfo.realName }}</el-descriptions-item>
                        <el-descriptions-item label="角色">
                            <el-tag :type="getRoleType(userInfo.role)">{{ getRoleName(userInfo.role) }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="手机号">
                            {{ userInfo.phone || '未填写' }}
                            <el-button type="primary" link size="small" @click="handleEdit('phone')">修改</el-button>
                        </el-descriptions-item>
                        <el-descriptions-item label="邮箱">
                            {{ userInfo.email || '未填写' }}
                            <el-button type="primary" link size="small" @click="handleEdit('email')">修改</el-button>
                        </el-descriptions-item>
                    </el-descriptions>
                </el-card>
            </el-col>

            <!-- 学生信息卡片（仅学生角色显示） -->
            <el-col :span="12" v-if="userInfo.role === 'STUDENT' && studentInfo">
                <el-card>
                    <template #header>
                        <div class="card-header">
                            <span>学生信息</span>
                        </div>
                    </template>
                    <el-descriptions :column="1" border>
                        <el-descriptions-item label="学号">{{ studentInfo.studentNumber }}</el-descriptions-item>
                        <el-descriptions-item label="班级">{{ studentInfo.className }}</el-descriptions-item>
                        <el-descriptions-item label="专业">{{ studentInfo.major || '未填写' }}</el-descriptions-item>
                        <el-descriptions-item label="入学日期">{{ studentInfo.enrollmentDate }}</el-descriptions-item>
                    </el-descriptions>
                </el-card>
            </el-col>
        </el-row>

        <!-- 安全设置 -->
        <el-card style="margin-top: 20px">
            <template #header>
                <div class="card-header">
                    <span>安全设置</span>
                </div>
            </template>
            <div class="security-item">
                <div class="security-info">
                    <span class="label">登录密码</span>
                    <span class="desc">定期修改密码可以提高账户安全性</span>
                </div>
                <el-button type="primary" @click="showPasswordDialog = true">修改密码</el-button>
            </div>
        </el-card>

        <!-- 修改密码弹窗 -->
        <el-dialog
            v-model="showPasswordDialog"
            title="修改密码"
            width="400px"
            @close="handlePasswordDialogClose"
        >
            <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordFormRules"
                label-width="80px"
            >
                <el-form-item label="旧密码" prop="oldPassword">
                    <el-input
                        v-model="passwordForm.oldPassword"
                        type="password"
                        placeholder="请输入旧密码"
                        show-password
                    />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                    <el-input
                        v-model="passwordForm.newPassword"
                        type="password"
                        placeholder="请输入新密码"
                        show-password
                    />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input
                        v-model="passwordForm.confirmPassword"
                        type="password"
                        placeholder="请确认新密码"
                        show-password
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showPasswordDialog = false">取消</el-button>
                <el-button type="primary" @click="handleChangePassword" :loading="changing">确定</el-button>
            </template>
        </el-dialog>

        <!-- 修改手机/邮箱弹窗 -->
        <el-dialog
            v-model="showContactDialog"
            :title="editType === 'phone' ? '修改手机号' : '修改邮箱'"
            width="400px"
        >
            <el-form
                ref="contactFormRef"
                :model="contactForm"
                :rules="contactFormRules"
                label-width="80px"
            >
                <el-form-item label="手机号" prop="phone" v-if="editType === 'phone'">
                    <el-input v-model="contactForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email" v-if="editType === 'email'">
                    <el-input v-model="contactForm.email" placeholder="请输入邮箱" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showContactDialog = false">取消</el-button>
                <el-button type="primary" @click="handleUpdateContact" :loading="updating">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '@/utils/axios'
import { Role, RoleName } from '@/utils/constants'

const userInfo = ref({
    id: null,
    username: '',
    realName: '',
    role: '',
    phone: '',
    email: ''
})

const studentInfo = ref(null)
const showPasswordDialog = ref(false)
const showContactDialog = ref(false)
const editType = ref('phone')
const changing = ref(false)
const updating = ref(false)

const passwordFormRef = ref(null)
const contactFormRef = ref(null)

const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const contactForm = reactive({
    phone: '',
    email: ''
})

const passwordFormRules = {
    oldPassword: [
        { required: true, message: '请输入旧密码', trigger: 'blur' }
    ],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
            validator: (rule, value, callback) => {
                if (value !== passwordForm.newPassword) {
                    callback(new Error('两次输入的密码不一致'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ]
}

const contactFormRules = {
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ]
}

const getRoleName = (role) => {
    return RoleName[role] || role
}

const getRoleType = (role) => {
    const typeMap = {
        [Role.SUPER_ADMIN]: 'danger',
        [Role.DORM_ADMIN]: 'warning',
        [Role.COUNSELOR]: 'success',
        [Role.STUDENT]: 'info'
    }
    return typeMap[role] || 'info'
}

const loadUserInfo = async () => {
    try {
        const userId = localStorage.getItem('userId')
        if (!userId) return
        
        const res = await axios.get(`/api/user/${userId}`)
        userInfo.value = res.data
        contactForm.phone = res.data.phone || ''
        contactForm.email = res.data.email || ''
        
        // 如果是学生，加载学生信息
        if (res.data.role === 'STUDENT') {
            try {
                const studentRes = await axios.get(`/api/student/${userId}`)
                studentInfo.value = studentRes.data
            } catch (error) {
                console.log('未找到学生信息')
            }
        }
    } catch (error) {
        console.error('加载用户信息失败:', error)
    }
}

const handleEdit = (type) => {
    editType.value = type
    showContactDialog.value = true
}

const handleUpdateContact = async () => {
    if (!contactFormRef.value) return
    
    await contactFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        updating.value = true
        try {
            const updateData = {}
            if (editType.value === 'phone') {
                updateData.phone = contactForm.phone
            } else {
                updateData.email = contactForm.email
            }
            
            await axios.put(`/api/user/profile/${userInfo.value.id}`, updateData)
            ElMessage.success('修改成功')
            showContactDialog.value = false
            loadUserInfo()
        } catch (error) {
            console.error('修改失败:', error)
        } finally {
            updating.value = false
        }
    })
}

const handleChangePassword = async () => {
    if (!passwordFormRef.value) return
    
    await passwordFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        changing.value = true
        try {
            await axios.put(`/api/user/password/${userInfo.value.id}`, {
                oldPassword: passwordForm.oldPassword,
                newPassword: passwordForm.newPassword
            })
            ElMessage.success('密码修改成功，请重新登录')
            showPasswordDialog.value = false
            setTimeout(() => {
                localStorage.clear()
                window.location.reload()
            }, 1500)
        } catch (error) {
            console.error('修改密码失败:', error)
        } finally {
            changing.value = false
        }
    })
}

const handlePasswordDialogClose = () => {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value?.resetFields()
}

onMounted(() => {
    loadUserInfo()
})
</script>

<style scoped>
.profile {
    padding: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.security-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
}

.security-info {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.security-info .label {
    font-weight: 500;
    font-size: 14px;
}

.security-info .desc {
    color: #909399;
    font-size: 13px;
}
</style>
