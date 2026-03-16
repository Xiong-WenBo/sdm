<template>
    <div class="attendance-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>查寝管理</span>
                    <div>
                        <el-button type="danger" @click="handleNotifyAbsent" :loading="notifying">
                            <el-icon><Bell /></el-icon>
                            通知未归学生
                        </el-button>
                        <el-button type="primary" @click="handleCheckIn">
                            <el-icon><Edit /></el-icon>
                            查寝录入
                        </el-button>
                    </div>
                </div>
            </template>

            <!-- 搜索栏 -->
            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="日期">
                    <el-date-picker
                        v-model="searchForm.checkDate"
                        type="date"
                        placeholder="选择日期"
                        style="width: 180px"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                    />
                </el-form-item>
                <el-form-item label="时段">
                    <el-select v-model="searchForm.checkTime" placeholder="选择时段" clearable style="width: 120px">
                        <el-option label="早上" value="MORNING" />
                        <el-option label="晚上" value="EVENING" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 120px">
                        <el-option label="正常" value="NORMAL" />
                        <el-option label="晚归" value="LATE" />
                        <el-option label="未归" value="ABSENT" />
                        <el-option label="请假" value="LEAVE" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 查寝记录列表 -->
            <el-table :data="attendanceList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="studentName" label="学生姓名" width="120" />
                <el-table-column prop="studentNumber" label="学号" width="150" />
                <el-table-column prop="className" label="班级" width="150" />
                <el-table-column prop="buildingName" label="楼栋" width="150" />
                <el-table-column prop="roomNumber" label="房间" width="100" />
                <el-table-column prop="checkDate" label="查寝日期" width="120" />
                <el-table-column prop="checkTime" label="时段" width="80">
                    <template #default="{ row }">
                        {{ row.checkTime === 'MORNING' ? '早上' : '晚上' }}
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getStatusType(row.status)">
                            {{ getStatusText(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="remarks" label="备注" min-width="150" show-overflow-tooltip />
                <el-table-column label="操作" fixed="right" width="150">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)" v-if="userRole === 'SUPER_ADMIN'">删除</el-button>
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

        <!-- 查寝录入弹窗 -->
        <el-dialog
            v-model="checkInDialogVisible"
            title="查寝录入"
            width="900px"
            @close="handleCheckInDialogClose"
        >
            <el-form :model="checkInForm" label-width="100px">
                <el-form-item label="查寝日期">
                    <el-date-picker
                        v-model="checkInForm.checkDate"
                        type="date"
                        placeholder="选择日期"
                        style="width: 100%"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                    />
                </el-form-item>
                <el-form-item label="查寝时段">
                    <el-radio-group v-model="checkInForm.checkTime">
                        <el-radio label="MORNING">早上</el-radio>
                        <el-radio label="EVENING">晚上</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>

            <el-table :data="studentList" border stripe style="width: 100%; margin-top: 20px">
                <el-table-column prop="studentNumber" label="学号" width="150" />
                <el-table-column prop="studentName" label="姓名" width="120" />
                <el-table-column prop="className" label="班级" width="150" />
                <el-table-column prop="roomNumber" label="房间" width="100" />
                <el-table-column label="状态" width="200">
                    <template #default="{ row }">
                        <el-select v-model="row.status" placeholder="选择状态" size="small" style="width: 150px">
                            <el-option label="正常" value="NORMAL" />
                            <el-option label="晚归" value="LATE" />
                            <el-option label="未归" value="ABSENT" />
                            <el-option label="请假" value="LEAVE" />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="备注" min-width="200">
                    <template #default="{ row }">
                        <el-input v-model="row.remarks" placeholder="备注" size="small" />
                    </template>
                </el-table-column>
            </el-table>

            <template #footer>
                <el-button @click="checkInDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleBatchSubmit" :loading="submitting">批量提交</el-button>
            </template>
        </el-dialog>

        <!-- 编辑查寝记录弹窗 -->
        <el-dialog
            v-model="editDialogVisible"
            title="编辑查寝记录"
            width="500px"
        >
            <el-form
                ref="editFormRef"
                :model="editForm"
                label-width="100px"
            >
                <el-form-item label="学生">
                    <span>{{ editForm.studentName }} ({{ editForm.studentNumber }})</span>
                </el-form-item>
                <el-form-item label="查寝日期">
                    <span>{{ editForm.checkDate }}</span>
                </el-form-item>
                <el-form-item label="时段">
                    <span>{{ editForm.checkTime === 'MORNING' ? '早上' : '晚上' }}</span>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-select v-model="editForm.status" style="width: 100%">
                        <el-option label="正常" value="NORMAL" />
                        <el-option label="晚归" value="LATE" />
                        <el-option label="未归" value="ABSENT" />
                        <el-option label="请假" value="LEAVE" />
                    </el-select>
                </el-form-item>
                <el-form-item label="备注" prop="remarks">
                    <el-input v-model="editForm.remarks" type="textarea" :rows="3" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="editDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleEditSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Bell } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const notifying = ref(false)
const checkInDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editFormRef = ref(null)

const userRole = localStorage.getItem('role')
const currentAttendanceId = ref(null)

const searchForm = reactive({
    checkDate: '',
    checkTime: '',
    status: ''
})

const attendanceList = ref([])
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const studentList = ref([])

const checkInForm = reactive({
    checkDate: new Date().toISOString().split('T')[0],
    checkTime: 'EVENING'
})

const editForm = reactive({
    id: null,
    studentId: null,
    studentName: '',
    studentNumber: '',
    checkDate: '',
    checkTime: '',
    status: '',
    remarks: ''
})

const getStatusType = (status) => {
    const typeMap = {
        'NORMAL': 'success',
        'LATE': 'warning',
        'ABSENT': 'danger',
        'LEAVE': 'info'
    }
    return typeMap[status] || 'info'
}

const getStatusText = (status) => {
    const textMap = {
        'NORMAL': '正常',
        'LATE': '晚归',
        'ABSENT': '未归',
        'LEAVE': '请假'
    }
    return textMap[status] || status
}

const loadAttendanceList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/attendance/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                checkDate: searchForm.checkDate || undefined,
                checkTime: searchForm.checkTime || undefined,
                status: searchForm.status || undefined
            }
        })
        attendanceList.value = res.data.list
        pagination.total = res.data.total
    } catch (error) {
        console.error('加载查寝记录失败:', error)
    } finally {
        loading.value = false
    }
}

const loadStudents = async () => {
    try {
        // 根据角色加载不同楼栋或班级的学生
        let url = '/api/attendance/students/building/1' // 默认楼栋 1
        
        if (userRole === 'DORM_ADMIN') {
            // 宿管需要传入楼栋 ID，这里简化处理，实际应该从后端获取
            url = '/api/attendance/students/building/1'
        } else if (userRole === 'COUNSELOR') {
            url = '/api/attendance/students/counselor'
        } else {
            // 超管默认加载所有楼栋（这里简化为楼栋 1）
            url = '/api/attendance/students/building/1'
        }
        
        const res = await axios.get(url)
        studentList.value = res.data.map(s => ({
            ...s,
            status: 'NORMAL',
            remarks: ''
        }))
    } catch (error) {
        console.error('加载学生列表失败:', error)
        studentList.value = []
    }
}

const handleSearch = () => {
    pagination.page = 1
    loadAttendanceList()
}

const handleReset = () => {
    searchForm.checkDate = ''
    searchForm.checkTime = ''
    searchForm.status = ''
    handleSearch()
}

const handleNotifyAbsent = () => {
    ElMessageBox.confirm(
        '确定要查询并通知今日未归学生吗？系统将自动发送站内信给相关人员。',
        '提示',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }
    ).then(async () => {
        notifying.value = true
        try {
            const res = await axios.post('/api/notification/absent/today')
            
            if (res.code === 200) {
                ElMessage.success(res.message || '通知发送成功')
                // 刷新未读消息数量
                window.dispatchEvent(new CustomEvent('update-unread-count'))
            } else {
                ElMessage.error(res.message || '通知发送失败')
            }
        } catch (error) {
            console.error('通知失败:', error)
            ElMessage.error(error.response?.data?.message || '通知发送失败')
        } finally {
            notifying.value = false
        }
    }).catch(() => {})
}

const handleCheckIn = () => {
    checkInForm.checkDate = new Date().toISOString().split('T')[0]
    checkInForm.checkTime = 'EVENING'
    loadStudents()
    checkInDialogVisible.value = true
}

const handleEdit = (row) => {
    currentAttendanceId.value = row.id
    editForm.id = row.id
    editForm.studentId = row.studentId
    editForm.studentName = row.studentName
    editForm.studentNumber = row.studentNumber
    editForm.checkDate = row.checkDate
    editForm.checkTime = row.checkTime
    editForm.status = row.status
    editForm.remarks = row.remarks || ''
    editDialogVisible.value = true
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该查寝记录吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/attendance/${row.id}`)
            ElMessage.success('删除成功')
            loadAttendanceList()
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const handleBatchSubmit = async () => {
    submitting.value = true
    try {
        const attendances = studentList.value.map(s => ({
            studentId: s.studentId,
            checkDate: checkInForm.checkDate,
            checkTime: checkInForm.checkTime,
            status: s.status || 'NORMAL',
            remarks: s.remarks || ''
        }))
        
        const res = await axios.post('/api/attendance/batch', attendances)
        
        // 响应拦截器已经返回 res.data，直接使用
        console.log('批量录入响应:', res)
        
        if (res.code === 200) {
            ElMessage.success(res.message || '批量录入成功')
            checkInDialogVisible.value = false
            // 立即刷新列表
            await loadAttendanceList()
        } else {
            ElMessage.error(res.message || '批量录入失败')
        }
    } catch (error) {
        console.error('批量录入失败:', error)
        // 如果是网络错误或其他异常
        if (error.response) {
            console.error('错误响应:', error.response.data)
            ElMessage.error(error.response.data?.message || '批量录入失败')
        } else {
            ElMessage.error('批量录入失败：' + error.message)
        }
    } finally {
        submitting.value = false
    }
}

const handleEditSubmit = async () => {
    submitting.value = true
    try {
        await axios.put(`/api/attendance/${currentAttendanceId.value}`, {
            status: editForm.status,
            remarks: editForm.remarks
        })
        ElMessage.success('更新成功')
        editDialogVisible.value = false
        loadAttendanceList()
    } catch (error) {
        console.error('更新失败:', error)
    } finally {
        submitting.value = false
    }
}

const handleCheckInDialogClose = () => {
    studentList.value = []
}

const handleSizeChange = () => {
    loadAttendanceList()
}

const handlePageChange = () => {
    loadAttendanceList()
}

onMounted(() => {
    loadAttendanceList()
})
</script>

<style scoped>
.attendance-management {
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
