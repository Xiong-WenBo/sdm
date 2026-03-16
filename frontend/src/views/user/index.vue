<template>
    <div class="user-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>用户管理</span>
                    <el-button type="primary" @click="handleAdd">
                        <el-icon><Plus /></el-icon>
                        新增用户
                    </el-button>
                </div>
            </template>

            <!-- 搜索栏 -->
            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="用户名">
                    <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
                </el-form-item>
                <el-form-item label="角色">
                    <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
                        <el-option label="超级管理员" value="SUPER_ADMIN" />
                        <el-option label="宿舍管理员" value="DORM_ADMIN" />
                        <el-option label="辅导员" value="COUNSELOR" />
                        <el-option label="学生" value="STUDENT" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                        <el-option label="启用" :value="1" />
                        <el-option label="禁用" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 用户列表 -->
            <el-table :data="userList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="username" label="用户名" width="150" />
                <el-table-column prop="realName" label="真实姓名" width="120" />
                <el-table-column prop="role" label="角色" width="120">
                    <template #default="{ row }">
                        <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                            {{ row.status === 1 ? '启用' : '禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="创建时间" width="180" />
                <el-table-column label="操作" fixed="right" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <Pagination
                v-model="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                @size-change="handleSizeChange"
                @current-change="handlePageChange"
            />
        </el-card>

        <!-- 新增/编辑用户弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            :title="dialogTitle"
            width="500px"
            @close="handleDialogClose"
        >
            <el-form
                ref="userFormRef"
                :model="userForm"
                :rules="userFormRules"
                label-width="80px"
            >
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="userForm.username" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input
                        v-model="userForm.password"
                        type="password"
                        placeholder="请输入密码"
                        :disabled="isEdit && !resetPassword"
                        show-password
                    />
                    <el-checkbox v-if="isEdit" v-model="resetPassword" style="margin-top: 10px">
                        重置密码
                    </el-checkbox>
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="角色" prop="role">
                    <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
                        <el-option label="超级管理员" value="SUPER_ADMIN" />
                        <el-option label="宿舍管理员" value="DORM_ADMIN" />
                        <el-option label="辅导员" value="COUNSELOR" />
                        <el-option label="学生" value="STUDENT" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="userForm.status">
                        <el-radio :label="1">启用</el-radio>
                        <el-radio :label="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
                
                <!-- 学生特有字段 -->
                <template v-if="userForm.role === 'STUDENT' && !isEdit">
                    <el-divider content-position="left">学生信息</el-divider>
                    <el-form-item label="学号" prop="studentNumber">
                        <el-input v-model="userForm.studentNumber" placeholder="请输入学号" />
                    </el-form-item>
                    <el-form-item label="班级" prop="className">
                        <el-input v-model="userForm.className" placeholder="请输入班级（如：计算机 2101 班）" />
                    </el-form-item>
                    <el-form-item label="专业" prop="major">
                        <el-input v-model="userForm.major" placeholder="请输入专业" />
                    </el-form-item>
                    <el-form-item label="辅导员" prop="counselorId">
                        <el-input v-model="userForm.counselorId" placeholder="请输入辅导员 ID（可选）" type="number" />
                    </el-form-item>
                    <el-form-item label="入学日期" prop="enrollmentDate">
                        <el-date-picker
                            v-model="userForm.enrollmentDate"
                            type="date"
                            placeholder="请选择入学日期"
                            style="width: 100%"
                            format="YYYY-MM-DD"
                            value-format="YYYY-MM-DD"
                        />
                    </el-form-item>
                </template>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import { Role, RoleName } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const resetPassword = ref(false)
const userFormRef = ref(null)

const searchForm = reactive({
    username: '',
    role: '',
    status: null
})

const userList = ref([])
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const userForm = reactive({
    id: null,
    username: '',
    password: '',
    realName: '',
    role: '',
    status: 1,
    // 学生特有字段
    studentNumber: '',
    className: '',
    major: '',
    counselorId: null,
    enrollmentDate: ''
})

// 保存原始用户数据用于对比
const originalUserData = ref(null)

const userFormRules = computed(() => {
    const rules = {
        username: [
            { required: true, message: '请输入用户名', trigger: 'blur' },
            { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
            { required: !isEdit.value || resetPassword.value, message: '请输入密码', trigger: 'blur' },
            { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
        ],
        realName: [
            { required: true, message: '请输入真实姓名', trigger: 'blur' }
        ],
        role: [
            { required: true, message: '请选择角色', trigger: 'change' }
        ]
    }
    
    // 学生特有字段的验证规则（仅新增时）
    if (!isEdit.value && userForm.role === 'STUDENT') {
        rules.studentNumber = [
            { required: true, message: '请输入学号', trigger: 'blur' }
        ]
        rules.className = [
            { required: true, message: '请输入班级', trigger: 'blur' }
        ]
    }
    
    return rules
})

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

const loadUserList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/user/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                username: searchForm.username || undefined,
                role: searchForm.role || undefined,
                status: searchForm.status
            }
        })
        userList.value = res.data.list
        pagination.total = res.data.total
    } catch (error) {
        console.error('加载用户列表失败:', error)
    } finally {
        loading.value = false
    }
}

const handleSearch = () => {
    pagination.page = 1
    loadUserList()
}

const handleReset = () => {
    searchForm.username = ''
    searchForm.role = ''
    searchForm.status = null
    handleSearch()
}

const handleAdd = () => {
    dialogTitle.value = '新增用户'
    isEdit.value = false
    resetPassword.value = false
    dialogVisible.value = true
}

const handleEdit = (row) => {
    dialogTitle.value = '编辑用户'
    isEdit.value = true
    resetPassword.value = false
    userForm.id = row.id
    userForm.username = row.username
    userForm.password = ''
    userForm.realName = row.realName
    userForm.role = row.role
    userForm.status = row.status
    
    // 保存原始用户数据用于对比
    originalUserData.value = {
        username: row.username,
        realName: row.realName,
        role: row.role,
        status: row.status
    }
    
    dialogVisible.value = true
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/user/${row.id}`)
            ElMessage.success('删除成功')
            loadUserList()
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const handleSubmit = async () => {
    if (!userFormRef.value) return

    await userFormRef.value.validate(async (valid) => {
        if (!valid) return

        submitting.value = true
        try {
            if (isEdit.value) {
                const updateData = {}
                
                // 只发送实际修改的字段
                if (userForm.username !== originalUserData.value?.username) {
                    updateData.username = userForm.username
                }
                if (userForm.realName !== originalUserData.value?.realName) {
                    updateData.realName = userForm.realName
                }
                if (userForm.role !== originalUserData.value?.role) {
                    updateData.role = userForm.role
                }
                if (userForm.status !== originalUserData.value?.status) {
                    updateData.status = userForm.status
                }
                if (resetPassword.value && userForm.password) {
                    updateData.password = userForm.password
                }
                
                await axios.put(`/api/user/${userForm.id}`, updateData)
                ElMessage.success('更新成功')
            } else {
                await axios.post('/api/user', userForm)
                ElMessage.success('创建成功')
            }
            dialogVisible.value = false
            loadUserList()
        } catch (error) {
            console.error('提交失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleDialogClose = () => {
    userFormRef.value?.resetFields()
    userForm.id = null
    userForm.username = ''
    userForm.password = ''
    userForm.realName = ''
    userForm.role = ''
    userForm.status = 1
    userForm.studentNumber = ''
    userForm.className = ''
    userForm.major = ''
    userForm.counselorId = null
    userForm.enrollmentDate = ''
    resetPassword.value = false
    originalUserData.value = null
}

const handleSizeChange = () => {
    loadUserList()
}

const handlePageChange = () => {
    loadUserList()
}

onMounted(() => {
    loadUserList()
})
</script>

<style scoped>
.user-management {
    height: 100%;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.search-form {
    margin-bottom: 20px;
}
</style>
