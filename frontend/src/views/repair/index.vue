<template>
    <div class="repair-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>报修管理</span>
                    <el-button type="primary" @click="handleAdd" v-if="userRole === 'STUDENT'">
                        <el-icon><Plus /></el-icon>
                        提交报修
                    </el-button>
                </div>
            </template>

            <!-- 搜索栏 -->
            <el-form :inline="true" class="search-form">
                <el-form-item label="状态筛选" v-if="userRole !== 'STUDENT'">
                    <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 150px" @change="loadRepairList">
                        <el-option label="待处理" value="PENDING" />
                        <el-option label="处理中" value="PROCESSING" />
                        <el-option label="已完成" value="COMPLETED" />
                        <el-option label="已拒绝" value="REJECTED" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="userRole === 'STUDENT'">
                    <el-button type="primary" @click="loadMyRepairs">刷新列表</el-button>
                </el-form-item>
            </el-form>

            <!-- 报修列表 -->
            <el-table :data="repairList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column label="报修人" width="120">
                    <template #default="{ row }">
                        {{ row.studentName || '本人' }}
                    </template>
                </el-table-column>
                <el-table-column prop="title" label="标题" min-width="200" />
                <el-table-column prop="category" label="类别" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getCategoryType(row.category)">
                            {{ getCategoryText(row.category) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="priority" label="优先级" width="80">
                    <template #default="{ row }">
                        <el-tag :type="getPriorityType(row.priority)" size="small">
                            {{ getPriorityText(row.priority) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getStatusType(row.status)">
                            {{ getStatusText(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="报修时间" width="180" />
                <el-table-column label="操作" fixed="right" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
                        <el-button type="warning" size="small" @click="handleHandle(row)" 
                                   v-if="canHandleRepair(row)">处理</el-button>
                        <el-button type="danger" size="small" @click="handleCancel(row)" 
                                   v-if="canCancelRepair(row)">取消</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 提交报修弹窗 -->
        <el-dialog
            v-model="addDialogVisible"
            title="提交报修"
            width="600px"
            @close="handleAddDialogClose"
        >
            <el-form
                ref="repairFormRef"
                :model="repairForm"
                :rules="repairFormRules"
                label-width="100px"
            >
                <el-form-item label="报修标题" prop="title">
                    <el-input v-model="repairForm.title" placeholder="简要描述报修问题" maxlength="100" show-word-limit />
                </el-form-item>
                <el-form-item label="问题描述" prop="description">
                    <el-input
                        v-model="repairForm.description"
                        type="textarea"
                        :rows="4"
                        placeholder="详细描述问题情况"
                        maxlength="500"
                        show-word-limit
                    />
                </el-form-item>
                <el-form-item label="报修类别" prop="category">
                    <el-select v-model="repairForm.category" placeholder="请选择类别" style="width: 100%">
                        <el-option label="电器维修" value="ELECTRIC" />
                        <el-option label="水电维修" value="PLUMBING" />
                        <el-option label="家具维修" value="FURNITURE" />
                        <el-option label="其他" value="OTHER" />
                    </el-select>
                </el-form-item>
                <el-form-item label="优先级" prop="priority">
                    <el-select v-model="repairForm.priority" placeholder="请选择优先级" style="width: 100%">
                        <el-option label="低" value="LOW" />
                        <el-option label="中" value="MEDIUM" />
                        <el-option label="高" value="HIGH" />
                        <el-option label="紧急" value="URGENT" />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
            </template>
        </el-dialog>

        <!-- 查看报修详情弹窗 -->
        <el-dialog
            v-model="viewDialogVisible"
            title="报修详情"
            width="700px"
        >
            <el-descriptions :column="2" border>
                <el-descriptions-item label="报修 ID">{{ currentRepair.id }}</el-descriptions-item>
                <el-descriptions-item label="报修人">{{ currentRepair.studentName || '本人' }}</el-descriptions-item>
                <el-descriptions-item label="标题" :span="2">{{ currentRepair.title }}</el-descriptions-item>
                <el-descriptions-item label="类别">
                    <el-tag :type="getCategoryType(currentRepair.category)">
                        {{ getCategoryText(currentRepair.category) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="优先级">
                    <el-tag :type="getPriorityType(currentRepair.priority)">
                        {{ getPriorityText(currentRepair.priority) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                    <el-tag :type="getStatusType(currentRepair.status)">
                        {{ getStatusText(currentRepair.status) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="报修时间">{{ currentRepair.createdAt }}</el-descriptions-item>
                <el-descriptions-item label="楼栋">{{ currentRepair.buildingName }}</el-descriptions-item>
                <el-descriptions-item label="房间号">{{ currentRepair.roomNumber }}</el-descriptions-item>
                <el-descriptions-item label="处理人" :span="2">{{ currentRepair.adminName || '未分配' }}</el-descriptions-item>
                <el-descriptions-item label="处理备注" :span="2">
                    {{ currentRepair.handleNote || '无' }}
                </el-descriptions-item>
                <el-descriptions-item label="处理时间" :span="2">
                    {{ currentRepair.handleTime || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="问题描述" :span="2">
                    <div class="description-content">{{ currentRepair.description }}</div>
                </el-descriptions-item>
            </el-descriptions>
            <template #footer>
                <el-button @click="viewDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>

        <!-- 处理报修弹窗 -->
        <el-dialog
            v-model="handleDialogVisible"
            title="处理报修"
            width="600px"
        >
            <el-form
                ref="handleFormRef"
                :model="handleForm"
                :rules="handleFormRules"
                label-width="100px"
            >
                <el-form-item label="当前状态">
                    <el-tag :type="getStatusType(currentRepair.status)">
                        {{ getStatusText(currentRepair.status) }}
                    </el-tag>
                </el-form-item>
                <el-form-item label="处理状态" prop="status">
                    <el-select v-model="handleForm.status" placeholder="请选择处理状态" style="width: 100%">
                        <el-option label="处理中" value="PROCESSING" />
                        <el-option label="已完成" value="COMPLETED" />
                        <el-option label="已拒绝" value="REJECTED" />
                    </el-select>
                </el-form-item>
                <el-form-item label="处理备注" prop="handleNote">
                    <el-input
                        v-model="handleForm.handleNote"
                        type="textarea"
                        :rows="4"
                        placeholder="填写处理情况"
                        maxlength="500"
                        show-word-limit
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="handleDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitHandle" :loading="handling">提交</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import { Role } from '@/utils/constants'

const loading = ref(false)
const submitting = ref(false)
const handling = ref(false)
const userRole = ref(localStorage.getItem('role') || '')

const repairList = ref([])
const filterStatus = ref('')

const addDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const handleDialogVisible = ref(false)

const repairFormRef = ref(null)
const handleFormRef = ref(null)

const repairForm = reactive({
    title: '',
    description: '',
    category: '',
    priority: 'MEDIUM'
})

const handleForm = reactive({
    status: '',
    handleNote: ''
})

const currentRepair = reactive({
    id: null,
    studentName: '',
    title: '',
    description: '',
    category: '',
    priority: '',
    status: '',
    createdAt: '',
    buildingName: '',
    roomNumber: '',
    adminName: '',
    handleNote: '',
    handleTime: ''
})

const repairFormRules = {
    title: [
        { required: true, message: '请输入报修标题', trigger: 'blur' },
        { min: 5, max: 100, message: '标题长度在 5 到 100 个字符', trigger: 'blur' }
    ],
    description: [
        { required: true, message: '请输入问题描述', trigger: 'blur' }
    ],
    category: [
        { required: true, message: '请选择报修类别', trigger: 'change' }
    ],
    priority: [
        { required: true, message: '请选择优先级', trigger: 'change' }
    ]
}

const handleFormRules = {
    status: [
        { required: true, message: '请选择处理状态', trigger: 'change' }
    ]
}

const getCategoryType = (category) => {
    const typeMap = {
        'ELECTRIC': 'warning',
        'PLUMBING': 'info',
        'FURNITURE': 'success',
        'OTHER': 'info'
    }
    return typeMap[category] || 'info'
}

const getCategoryText = (category) => {
    const textMap = {
        'ELECTRIC': '电器',
        'PLUMBING': '水电',
        'FURNITURE': '家具',
        'OTHER': '其他'
    }
    return textMap[category] || category
}

const getPriorityType = (priority) => {
    const typeMap = {
        'LOW': 'info',
        'MEDIUM': 'success',
        'HIGH': 'warning',
        'URGENT': 'danger'
    }
    return typeMap[priority] || 'info'
}

const getPriorityText = (priority) => {
    const textMap = {
        'LOW': '低',
        'MEDIUM': '中',
        'HIGH': '高',
        'URGENT': '紧急'
    }
    return textMap[priority] || priority
}

const getStatusType = (status) => {
    const typeMap = {
        'PENDING': 'info',
        'PROCESSING': 'warning',
        'COMPLETED': 'success',
        'REJECTED': 'danger'
    }
    return typeMap[status] || 'info'
}

const getStatusText = (status) => {
    const textMap = {
        'PENDING': '待处理',
        'PROCESSING': '处理中',
        'COMPLETED': '已完成',
        'REJECTED': '已拒绝'
    }
    return textMap[status] || status
}

const canHandleRepair = (row) => {
    const canManage = userRole.value === Role.DORM_ADMIN || userRole.value === Role.SUPER_ADMIN
    return canManage && (row.status === 'PENDING' || row.status === 'PROCESSING')
}

const canCancelRepair = (row) => {
    return userRole.value === Role.STUDENT && row.status === 'PENDING'
}

const loadRepairList = async () => {
    loading.value = true
    try {
        const params = {}
        if (filterStatus.value) {
            params.status = filterStatus.value
        }
        
        const res = await axios.get('/api/repair/building/list', { params })
        repairList.value = res.data
    } catch (error) {
        console.error('加载报修列表失败:', error)
    } finally {
        loading.value = false
    }
}

const loadMyRepairs = async () => {
    loading.value = true
    try {
        const params = {}
        if (filterStatus.value) {
            params.status = filterStatus.value
        }
        
        const res = await axios.get('/api/repair/my/list', { params })
        repairList.value = res.data
    } catch (error) {
        console.error('加载我的报修失败:', error)
    } finally {
        loading.value = false
    }
}

const handleAdd = () => {
    addDialogVisible.value = true
}

const handleAddDialogClose = () => {
    repairFormRef.value?.resetFields()
    repairForm.title = ''
    repairForm.description = ''
    repairForm.category = ''
    repairForm.priority = 'MEDIUM'
}

const handleSubmit = async () => {
    if (!repairFormRef.value) return
    
    await repairFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        submitting.value = true
        try {
            await axios.post('/api/repair', repairForm)
            ElMessage.success('报修提交成功')
            addDialogVisible.value = false
            loadMyRepairs()
        } catch (error) {
            console.error('提交报修失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleView = (row) => {
    currentRepair.id = row.id
    currentRepair.studentName = row.studentName
    currentRepair.title = row.title
    currentRepair.description = row.description
    currentRepair.category = row.category
    currentRepair.priority = row.priority
    currentRepair.status = row.status
    currentRepair.createdAt = row.createdAt
    currentRepair.buildingName = row.buildingName
    currentRepair.roomNumber = row.roomNumber
    currentRepair.adminName = row.adminName
    currentRepair.handleNote = row.handleNote
    currentRepair.handleTime = row.handleTime
    viewDialogVisible.value = true
}

const handleHandle = (row) => {
    currentRepair.id = row.id
    currentRepair.status = row.status
    handleForm.status = ''
    handleForm.handleNote = ''
    handleDialogVisible.value = true
}

const submitHandle = async () => {
    if (!handleFormRef.value) return
    
    await handleFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        handling.value = true
        try {
            await axios.put(`/api/repair/${currentRepair.id}/handle`, null, {
                params: {
                    status: handleForm.status,
                    handleNote: handleForm.handleNote || ''
                }
            })
            ElMessage.success('处理成功')
            handleDialogVisible.value = false
            loadRepairList()
        } catch (error) {
            console.error('处理报修失败:', error)
        } finally {
            handling.value = false
        }
    })
}

const handleCancel = (row) => {
    ElMessageBox.confirm('确定要取消该报修申请吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.put(`/api/repair/${row.id}/cancel`)
            ElMessage.success('取消成功')
            loadMyRepairs()
        } catch (error) {
            console.error('取消报修失败:', error)
        }
    }).catch(() => {})
}

onMounted(() => {
    if (userRole.value === Role.STUDENT) {
        loadMyRepairs()
    } else {
        loadRepairList()
    }
})
</script>

<style scoped>
.repair-management {
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

.description-content {
    white-space: pre-wrap;
    line-height: 1.6;
}
</style>
