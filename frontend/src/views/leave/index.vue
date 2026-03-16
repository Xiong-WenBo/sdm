<template>
    <div class="leave-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>请假管理</span>
                    <el-button type="primary" @click="handleAdd" v-if="userRole === 'STUDENT'">
                        <el-icon><Plus /></el-icon>
                        提交请假
                    </el-button>
                </div>
            </template>

            <!-- 搜索栏 -->
            <el-form :inline="true" class="search-form">
                <el-form-item label="状态筛选" v-if="userRole !== 'STUDENT'">
                    <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 150px" @change="loadLeaveList">
                        <el-option label="待审批" value="PENDING" />
                        <el-option label="已通过" value="APPROVED" />
                        <el-option label="已拒绝" value="REJECTED" />
                        <el-option label="已取消" value="CANCELED" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="userRole === 'STUDENT'">
                    <el-button type="primary" @click="loadMyLeaves">刷新列表</el-button>
                </el-form-item>
            </el-form>

            <!-- 请假列表 -->
            <el-table :data="leaveList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column label="请假人" width="120">
                    <template #default="{ row }">
                        {{ row.studentName || '本人' }}
                    </template>
                </el-table-column>
                <el-table-column label="请假时间" min-width="200">
                    <template #default="{ row }">
                        {{ formatDateTime(row.startTime) }}<br>
                        <span style="color: #999; font-size: 12px">至 {{ formatDateTime(row.endTime) }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="type" label="类型" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getTypeType(row.type)">
                            {{ getTypeText(row.type) }}
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
                <el-table-column prop="createdAt" label="申请时间" width="180" />
                <el-table-column label="操作" fixed="right" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
                        <el-button type="warning" size="small" @click="handleApprove(row)" 
                                   v-if="canApproveLeave(row)">审批</el-button>
                        <el-button type="danger" size="small" @click="handleCancel(row)" 
                                   v-if="canCancelLeave(row)">取消</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 提交请假弹窗 -->
        <el-dialog
            v-model="addDialogVisible"
            title="提交请假"
            width="600px"
            @close="handleAddDialogClose"
        >
            <el-form
                ref="leaveFormRef"
                :model="leaveForm"
                :rules="leaveFormRules"
                label-width="100px"
            >
                <el-form-item label="请假类型" prop="type">
                    <el-select v-model="leaveForm.type" placeholder="请选择类型" style="width: 100%">
                        <el-option label="病假" value="SICK" />
                        <el-option label="事假" value="PERSONAL" />
                        <el-option label="其他" value="OTHER" />
                    </el-select>
                </el-form-item>
                <el-form-item label="开始时间" prop="startTime">
                    <el-date-picker
                        v-model="leaveForm.startTime"
                        type="datetime"
                        placeholder="选择开始时间"
                        style="width: 100%"
                        format="YYYY-MM-DD HH:mm:ss"
                    />
                </el-form-item>
                <el-form-item label="结束时间" prop="endTime">
                    <el-date-picker
                        v-model="leaveForm.endTime"
                        type="datetime"
                        placeholder="选择结束时间"
                        style="width: 100%"
                        format="YYYY-MM-DD HH:mm:ss"
                    />
                </el-form-item>
                <el-form-item label="请假理由" prop="reason">
                    <el-input
                        v-model="leaveForm.reason"
                        type="textarea"
                        :rows="4"
                        placeholder="请输入请假理由"
                        maxlength="500"
                        show-word-limit
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
            </template>
        </el-dialog>

        <!-- 查看请假详情弹窗 -->
        <el-dialog
            v-model="viewDialogVisible"
            title="请假详情"
            width="700px"
        >
            <el-descriptions :column="2" border>
                <el-descriptions-item label="请假 ID">{{ currentLeave.id }}</el-descriptions-item>
                <el-descriptions-item label="请假人">{{ currentLeave.studentName || '本人' }}</el-descriptions-item>
                <el-descriptions-item label="班级" :span="2">{{ currentLeave.className }}</el-descriptions-item>
                <el-descriptions-item label="类型">
                    <el-tag :type="getTypeType(currentLeave.type)">
                        {{ getTypeText(currentLeave.type) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                    <el-tag :type="getStatusType(currentLeave.status)">
                        {{ getStatusText(currentLeave.status) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="开始时间">{{ formatDateTime(currentLeave.startTime) }}</el-descriptions-item>
                <el-descriptions-item label="结束时间">{{ formatDateTime(currentLeave.endTime) }}</el-descriptions-item>
                <el-descriptions-item label="申请时间">{{ currentLeave.createdAt }}</el-descriptions-item>
                <el-descriptions-item label="审批人" :span="2">{{ currentLeave.approverName || '待审批' }}</el-descriptions-item>
                <el-descriptions-item label="审批意见" :span="2">
                    {{ currentLeave.approveNote || '无' }}
                </el-descriptions-item>
                <el-descriptions-item label="请假理由" :span="2">
                    <div class="reason-content">{{ currentLeave.reason }}</div>
                </el-descriptions-item>
            </el-descriptions>
            <template #footer>
                <el-button @click="viewDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>

        <!-- 审批请假弹窗 -->
        <el-dialog
            v-model="approveDialogVisible"
            title="审批请假"
            width="600px"
        >
            <el-form
                ref="approveFormRef"
                :model="approveForm"
                :rules="approveFormRules"
                label-width="100px"
            >
                <el-form-item label="当前状态">
                    <el-tag :type="getStatusType(currentLeave.status)">
                        {{ getStatusText(currentLeave.status) }}
                    </el-tag>
                </el-form-item>
                <el-form-item label="审批结果" prop="status">
                    <el-select v-model="approveForm.status" placeholder="请选择审批结果" style="width: 100%">
                        <el-option label="通过" value="APPROVED" />
                        <el-option label="拒绝" value="REJECTED" />
                    </el-select>
                </el-form-item>
                <el-form-item label="审批意见" prop="approveNote">
                    <el-input
                        v-model="approveForm.approveNote"
                        type="textarea"
                        :rows="4"
                        placeholder="填写审批意见"
                        maxlength="500"
                        show-word-limit
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="approveDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitApprove" :loading="approving">提交</el-button>
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
const approving = ref(false)
const userRole = ref(localStorage.getItem('role') || '')

const leaveList = ref([])
const filterStatus = ref('')

const addDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const approveDialogVisible = ref(false)

const leaveFormRef = ref(null)
const approveFormRef = ref(null)

const leaveForm = reactive({
    type: 'PERSONAL',
    startTime: '',
    endTime: '',
    reason: ''
})

const approveForm = reactive({
    status: '',
    approveNote: ''
})

const currentLeave = reactive({
    id: null,
    studentName: '',
    className: '',
    type: '',
    status: '',
    startTime: '',
    endTime: '',
    createdAt: '',
    approverName: '',
    approveNote: '',
    reason: ''
})

const leaveFormRules = {
    type: [
        { required: true, message: '请选择请假类型', trigger: 'change' }
    ],
    startTime: [
        { required: true, message: '请选择开始时间', trigger: 'change' }
    ],
    endTime: [
        { required: true, message: '请选择结束时间', trigger: 'change' }
    ],
    reason: [
        { required: true, message: '请输入请假理由', trigger: 'blur' }
    ]
}

const approveFormRules = {
    status: [
        { required: true, message: '请选择审批结果', trigger: 'change' }
    ]
}

const getTypeType = (type) => {
    const typeMap = {
        'SICK': 'danger',
        'PERSONAL': 'warning',
        'OTHER': 'info'
    }
    return typeMap[type] || 'info'
}

const getTypeText = (type) => {
    const textMap = {
        'SICK': '病假',
        'PERSONAL': '事假',
        'OTHER': '其他'
    }
    return textMap[type] || type
}

const getStatusType = (status) => {
    const typeMap = {
        'PENDING': 'info',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'CANCELED': 'info'
    }
    return typeMap[status] || 'info'
}

const getStatusText = (status) => {
    const textMap = {
        'PENDING': '待审批',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'CANCELED': '已取消'
    }
    return textMap[status] || status
}

const formatDateTime = (dateTime) => {
    if (!dateTime) return ''
    const date = new Date(dateTime)
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

const canApproveLeave = (row) => {
    return userRole.value === Role.COUNSELOR && row.status === 'PENDING'
}

const canCancelLeave = (row) => {
    return userRole.value === Role.STUDENT && row.status === 'PENDING'
}

const loadLeaveList = async () => {
    loading.value = true
    try {
        const params = {}
        if (filterStatus.value) {
            params.status = filterStatus.value
        }
        
        let url = '/api/leave/building/list'
        if (userRole.value === Role.COUNSELOR || userRole.value === Role.SUPER_ADMIN) {
            url = '/api/leave/counselor/list'
        }
        
        const res = await axios.get(url, { params })
        leaveList.value = res.data
    } catch (error) {
        console.error('加载请假列表失败:', error)
    } finally {
        loading.value = false
    }
}

const loadMyLeaves = async () => {
    loading.value = true
    try {
        const params = {}
        if (filterStatus.value) {
            params.status = filterStatus.value
        }
        
        const res = await axios.get('/api/leave/my/list', { params })
        leaveList.value = res.data
    } catch (error) {
        console.error('加载我的请假失败:', error)
    } finally {
        loading.value = false
    }
}

const handleAdd = () => {
    addDialogVisible.value = true
}

const handleAddDialogClose = () => {
    leaveFormRef.value?.resetFields()
    leaveForm.type = 'PERSONAL'
    leaveForm.startTime = ''
    leaveForm.endTime = ''
    leaveForm.reason = ''
}

const handleSubmit = async () => {
    if (!leaveFormRef.value) return
    
    await leaveFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        // 验证结束时间不能早于开始时间
        if (new Date(leaveForm.endTime) <= new Date(leaveForm.startTime)) {
            ElMessage.error('结束时间必须晚于开始时间')
            return
        }
        
        submitting.value = true
        try {
            await axios.post('/api/leave', leaveForm)
            ElMessage.success('请假提交成功')
            addDialogVisible.value = false
            loadMyLeaves()
        } catch (error) {
            console.error('提交请假失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleView = (row) => {
    currentLeave.id = row.id
    currentLeave.studentName = row.studentName
    currentLeave.className = row.className
    currentLeave.type = row.type
    currentLeave.status = row.status
    currentLeave.startTime = row.startTime
    currentLeave.endTime = row.endTime
    currentLeave.createdAt = row.createdAt
    currentLeave.approverName = row.approverName
    currentLeave.approveNote = row.approveNote
    currentLeave.reason = row.reason
    viewDialogVisible.value = true
}

const handleApprove = (row) => {
    currentLeave.id = row.id
    currentLeave.status = row.status
    approveForm.status = ''
    approveForm.approveNote = ''
    approveDialogVisible.value = true
}

const submitApprove = async () => {
    if (!approveFormRef.value) return
    
    await approveFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        approving.value = true
        try {
            await axios.put(`/api/leave/${currentLeave.id}/approve`, null, {
                params: {
                    status: approveForm.status,
                    approveNote: approveForm.approveNote || ''
                }
            })
            ElMessage.success('审批成功')
            approveDialogVisible.value = false
            loadLeaveList()
        } catch (error) {
            console.error('审批失败:', error)
        } finally {
            approving.value = false
        }
    })
}

const handleCancel = (row) => {
    ElMessageBox.confirm('确定要取消该请假申请吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.put(`/api/leave/${row.id}/cancel`)
            ElMessage.success('取消成功')
            loadMyLeaves()
        } catch (error) {
            console.error('取消请假失败:', error)
        }
    }).catch(() => {})
}

onMounted(() => {
    if (userRole.value === Role.STUDENT) {
        loadMyLeaves()
    } else {
        loadLeaveList()
    }
})
</script>

<style scoped>
.leave-management {
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

.reason-content {
    white-space: pre-wrap;
    line-height: 1.6;
}
</style>
