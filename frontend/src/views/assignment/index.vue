<template>
    <div class="assignment-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>宿舍分配管理</span>
                    <el-button type="primary" @click="handleAdd">
                        <el-icon><Plus /></el-icon>
                        分配住宿
                    </el-button>
                </div>
            </template>

            <!-- 搜索栏 -->
            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="楼栋">
                    <el-select v-model="searchForm.buildingId" placeholder="请选择楼栋" clearable filterable style="width: 200px">
                        <el-option
                            v-for="building in buildingList"
                            :key="building.id"
                            :label="building.name"
                            :value="building.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
                        <el-option label="在住" value="ACTIVE" />
                        <el-option label="已退宿" value="INACTIVE" />
                        <el-option label="待入住" value="PENDING" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 住宿分配列表 -->
            <el-table :data="assignmentList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="studentName" label="学生姓名" width="120" />
                <el-table-column prop="studentNumber" label="学号" width="150" />
                <el-table-column prop="buildingName" label="楼栋" width="150" />
                <el-table-column prop="roomNumber" label="房间号" width="100" />
                <el-table-column prop="bedNumber" label="床位号" width="80" />
                <el-table-column prop="checkInDate" label="入住日期" width="120" />
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getStatusType(row.status)">
                            {{ getStatusText(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" fixed="right" width="280">
                    <template #default="{ row }">
                        <el-button type="success" size="small" @click="handleCheckOut(row)" v-if="row.status === 'ACTIVE'">退宿</el-button>
                        <el-button type="warning" size="small" @click="handleTransfer(row)" v-if="row.status === 'ACTIVE'">调宿</el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)" v-if="userRole === 'SUPER_ADMIN'">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <el-pagination
                v-model:current-page="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handlePageChange"
                style="margin-top: 20px; justify-content: flex-end"
            />
        </el-card>

        <!-- 分配住宿弹窗 -->
        <el-dialog
            v-model="addDialogVisible"
            title="分配住宿"
            width="700px"
            @close="handleAddDialogClose"
        >
            <el-form
                ref="addFormRef"
                :model="addForm"
                :rules="addFormRules"
                label-width="100px"
            >
                <el-form-item label="学生" prop="studentId">
                    <el-select
                        v-model="addForm.studentId"
                        placeholder="请选择学生"
                        filterable
                        style="width: 100%"
                    >
                        <el-option
                            v-for="student in studentList"
                            :key="student.id"
                            :label="`${student.studentNumber} - ${student.realName} (${student.className})`"
                            :value="student.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="楼栋" prop="buildingId">
                    <el-select
                        v-model="addForm.buildingId"
                        placeholder="请选择楼栋"
                        filterable
                        style="width: 100%"
                        :disabled="userRole === 'DORM_ADMIN'"
                        @change="handleBuildingChange"
                    >
                        <el-option
                            v-for="building in buildingList"
                            :key="building.id"
                            :label="building.name"
                            :value="building.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="房间" prop="roomId">
                    <el-select
                        v-model="addForm.roomId"
                        placeholder="请选择房间"
                        filterable
                        style="width: 100%"
                        :disabled="!addForm.buildingId"
                    >
                        <el-option
                            v-for="room in availableRooms"
                            :key="room.id"
                            :label="`${room.roomNumber} (已住${room.currentOccupancy}/${room.capacity}人)`"
                            :value="room.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="床位号" prop="bedNumber">
                    <el-input v-model="addForm.bedNumber" placeholder="请输入床位号（如：A1）" />
                </el-form-item>
                <el-form-item label="入住日期" prop="checkInDate">
                    <el-date-picker
                        v-model="addForm.checkInDate"
                        type="date"
                        placeholder="请选择入住日期"
                        style="width: 100%"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleAddSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>

        <!-- 退宿弹窗 -->
        <el-dialog
            v-model="checkOutDialogVisible"
            title="退宿办理"
            width="400px"
        >
            <el-form label-width="100px">
                <el-form-item label="退宿日期">
                    <el-date-picker
                        v-model="checkOutDate"
                        type="date"
                        placeholder="请选择退宿日期"
                        style="width: 100%"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="checkOutDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleCheckOutSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>

        <!-- 调宿弹窗 -->
        <el-dialog
            v-model="transferDialogVisible"
            title="调宿办理"
            width="600px"
        >
            <el-form
                ref="transferFormRef"
                :model="transferForm"
                :rules="transferFormRules"
                label-width="100px"
            >
                <el-form-item label="新房间" prop="newRoomId">
                    <el-select
                        v-model="transferForm.newRoomId"
                        placeholder="请选择新房间"
                        filterable
                        style="width: 100%"
                    >
                        <el-option
                            v-for="room in availableRooms"
                            :key="room.id"
                            :label="`${room.roomNumber} (已住${room.currentOccupancy}/${room.capacity}人)`"
                            :value="room.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="新床位" prop="newBedNumber">
                    <el-input v-model="transferForm.newBedNumber" placeholder="请输入新床位号（如：A1）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="transferDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleTransferSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '@/utils/axios'

const loading = ref(false)
const submitting = ref(false)
const addDialogVisible = ref(false)
const checkOutDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const addFormRef = ref(null)
const transferFormRef = ref(null)

const userRole = localStorage.getItem('role')
const currentAssignmentId = ref(null)
const checkOutDate = ref(new Date().toISOString().split('T')[0])

const searchForm = reactive({
    buildingId: null,
    status: ''
})

const assignmentList = ref([])
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const buildingList = ref([])
const studentList = ref([])
const availableRooms = ref([])

const addForm = reactive({
    studentId: null,
    buildingId: null,
    roomId: null,
    bedNumber: '',
    checkInDate: new Date().toISOString().split('T')[0]
})

const transferForm = reactive({
    newRoomId: null,
    newBedNumber: ''
})

const addFormRules = {
    studentId: [
        { required: true, message: '请选择学生', trigger: 'change' }
    ],
    buildingId: [
        { required: true, message: '请选择楼栋', trigger: 'change' }
    ],
    roomId: [
        { required: true, message: '请选择房间', trigger: 'change' }
    ],
    bedNumber: [
        { required: true, message: '请输入床位号', trigger: 'blur' }
    ]
}

const transferFormRules = {
    newRoomId: [
        { required: true, message: '请选择新房间', trigger: 'change' }
    ],
    newBedNumber: [
        { required: true, message: '请输入新床位号', trigger: 'blur' }
    ]
}

const getStatusType = (status) => {
    const typeMap = {
        'ACTIVE': 'success',
        'INACTIVE': 'info',
        'PENDING': 'warning'
    }
    return typeMap[status] || 'info'
}

const getStatusText = (status) => {
    const textMap = {
        'ACTIVE': '在住',
        'INACTIVE': '已退宿',
        'PENDING': '待入住'
    }
    return textMap[status] || status
}

const loadAssignmentList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/assignment/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                buildingId: searchForm.buildingId || undefined,
                status: searchForm.status || undefined
            }
        })
        assignmentList.value = res.data.list
        pagination.total = res.data.total
    } catch (error) {
        console.error('加载住宿分配列表失败:', error)
    } finally {
        loading.value = false
    }
}

const loadBuildingList = async () => {
    try {
        const res = await axios.get('/api/building/list', {
            params: { page: 1, size: 100 }
        })
        buildingList.value = res.data.list
        
        // 如果是宿管，自动选中管理的楼栋
        if (userRole === 'DORM_ADMIN') {
            const username = localStorage.getItem('username')
            const myBuilding = buildingList.value.find(b => b.adminName === username || b.adminName === localStorage.getItem('realName'))
            if (myBuilding) {
                searchForm.buildingId = myBuilding.id
                addForm.buildingId = myBuilding.id
            }
        }
    } catch (error) {
        console.error('加载楼栋列表失败:', error)
    }
}

const loadStudentList = async () => {
    try {
        const res = await axios.get('/api/student/list', {
            params: { page: 1, size: 1000 }
        })
        // student 列表包含 id, studentNumber, className, realName 等字段
        studentList.value = res.data.list || []
    } catch (error) {
        console.error('加载学生列表失败:', error)
        studentList.value = []
    }
}

const loadAvailableRooms = async (buildingId) => {
    if (!buildingId) {
        availableRooms.value = []
        return
    }
    
    try {
        const res = await axios.get('/api/assignment/available-rooms', {
            params: { buildingId }
        })
        availableRooms.value = res.data
    } catch (error) {
        console.error('加载可用房间失败:', error)
        availableRooms.value = []
    }
}

const handleSearch = () => {
    pagination.page = 1
    loadAssignmentList()
}

const handleReset = () => {
    searchForm.buildingId = null
    searchForm.status = ''
    handleSearch()
}

const handleAdd = () => {
    addDialogVisible.value = true
    loadStudentList()
}

const handleBuildingChange = (buildingId) => {
    loadAvailableRooms(buildingId)
    addForm.roomId = null
}

const handleCheckOut = (row) => {
    currentAssignmentId.value = row.id
    checkOutDialogVisible.value = true
}

const handleTransfer = (row) => {
    currentAssignmentId.value = row.id
    transferForm.newRoomId = null
    transferForm.newBedNumber = ''
    
    // 加载可用房间（同楼栋）
    // 这里简化处理，加载所有可用房间
    loadAvailableRoomsForTransfer()
    
    transferDialogVisible.value = true
}

const loadAvailableRoomsForTransfer = async () => {
    try {
        const res = await axios.get('/api/assignment/available-rooms')
        availableRooms.value = res.data
    } catch (error) {
        console.error('加载可用房间失败:', error)
        availableRooms.value = []
    }
}

const handleAddSubmit = async () => {
    if (!addFormRef.value) return

    await addFormRef.value.validate(async (valid) => {
        if (!valid) return

        submitting.value = true
        try {
            await axios.post('/api/assignment', addForm)
            ElMessage.success('分配成功')
            addDialogVisible.value = false
            loadAssignmentList()
        } catch (error) {
            console.error('分配失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleCheckOutSubmit = async () => {
    submitting.value = true
    try {
        await axios.put(`/api/assignment/${currentAssignmentId.value}/checkout`, null, {
            params: { checkOutDate: checkOutDate.value }
        })
        ElMessage.success('退宿成功')
        checkOutDialogVisible.value = false
        loadAssignmentList()
    } catch (error) {
        console.error('退宿失败:', error)
    } finally {
        submitting.value = false
    }
}

const handleTransferSubmit = async () => {
    if (!transferFormRef.value) return

    await transferFormRef.value.validate(async (valid) => {
        if (!valid) return

        submitting.value = true
        try {
            await axios.put(`/api/assignment/${currentAssignmentId.value}/transfer`, null, {
                params: {
                    newRoomId: transferForm.newRoomId,
                    newBedNumber: transferForm.newBedNumber
                }
            })
            ElMessage.success('调宿成功')
            transferDialogVisible.value = false
            loadAssignmentList()
        } catch (error) {
            console.error('调宿失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该住宿记录吗？此操作不可恢复！', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/assignment/${row.id}`)
            ElMessage.success('删除成功')
            loadAssignmentList()
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const handleAddDialogClose = () => {
    addFormRef.value?.resetFields()
    addForm.studentId = null
    addForm.buildingId = null
    addForm.roomId = null
    addForm.bedNumber = ''
    addForm.checkInDate = new Date().toISOString().split('T')[0]
    availableRooms.value = []
}

const handleSizeChange = () => {
    loadAssignmentList()
}

const handlePageChange = () => {
    loadAssignmentList()
}

onMounted(() => {
    loadBuildingList()
    loadAssignmentList()
})
</script>

<style scoped>
.assignment-management {
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
