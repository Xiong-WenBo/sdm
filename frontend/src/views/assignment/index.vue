<template>
    <div class="assignment-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>宿舍分配管理</span>
                    <div class="header-actions">
                        <el-button type="success" @click="openBulkDialog">
                            <el-icon><Operation /></el-icon>
                            批量自动分配
                        </el-button>
                        <el-button type="primary" @click="openAddDialog">
                            <el-icon><Plus /></el-icon>
                            分配宿舍
                        </el-button>
                    </div>
                </div>
            </template>

            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="楼栋">
                    <el-select
                        v-model="searchForm.buildingId"
                        placeholder="请选择楼栋"
                        clearable
                        filterable
                        style="width: 220px"
                        :disabled="userRole === 'DORM_ADMIN'"
                    >
                        <el-option
                            v-for="building in buildingList"
                            :key="building.id"
                            :label="building.name"
                            :value="building.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select
                        v-model="searchForm.status"
                        placeholder="请选择状态"
                        clearable
                        style="width: 160px"
                    >
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

            <el-table
                :data="assignmentList"
                v-loading="loading"
                border
                stripe
                style="width: 100%"
            >
                <template #empty>
                    <el-empty description="暂无宿舍分配数据" />
                </template>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="studentName" label="学生姓名" width="120" />
                <el-table-column prop="studentNumber" label="学号" width="150" />
                <el-table-column prop="buildingName" label="楼栋" width="140" />
                <el-table-column prop="roomNumber" label="房间号" width="100" />
                <el-table-column prop="bedNumber" label="床位号" width="90" />
                <el-table-column prop="checkInDate" label="入住日期" width="120" />
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getStatusType(row.status)">
                            {{ getStatusText(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" fixed="right" min-width="220">
                    <template #default="{ row }">
                        <div class="row-actions">
                            <el-button
                                v-if="row.status === 'ACTIVE'"
                                type="success"
                                size="small"
                                @click="openCheckOutDialog(row)"
                            >
                                退宿
                            </el-button>
                            <el-button
                                v-if="row.status === 'ACTIVE'"
                                type="warning"
                                size="small"
                                @click="openTransferDialog(row)"
                            >
                                调宿
                            </el-button>
                            <el-button
                                v-if="userRole === 'SUPER_ADMIN'"
                                type="danger"
                                size="small"
                                @click="handleDelete(row)"
                            >
                                删除
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <Pagination
                v-model="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                @size-change="loadAssignmentList"
                @current-change="loadAssignmentList"
            />
        </el-card>

        <el-dialog
            v-model="addDialogVisible"
            title="分配宿舍"
            width="720px"
            @close="resetAddForm"
        >
            <el-form ref="addFormRef" :model="addForm" :rules="addFormRules" label-width="100px">
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
                            :label="`${student.studentNumber} - ${student.realName} (${student.className || '未分班'})`"
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
                            :label="`${room.roomNumber}（已住 ${room.currentOccupancy}/${room.capacity}）`"
                            :value="room.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="床位号" prop="bedNumber">
                    <el-input v-model="addForm.bedNumber" placeholder="请输入床位号，例如 A1" />
                </el-form-item>
                <el-form-item label="入住日期" prop="checkInDate">
                    <el-date-picker
                        v-model="addForm.checkInDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        format="YYYY-MM-DD"
                        placeholder="请选择入住日期"
                        style="width: 100%"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleAddSubmit">确定</el-button>
            </template>
        </el-dialog>

        <el-dialog
            v-model="bulkDialogVisible"
            title="批量自动分配宿舍"
            width="600px"
            @close="resetBulkForm"
        >
            <el-form ref="bulkFormRef" :model="bulkForm" :rules="bulkFormRules" label-width="110px">
                <el-form-item label="楼栋" prop="buildingId">
                    <el-select
                        v-model="bulkForm.buildingId"
                        placeholder="请选择楼栋"
                        filterable
                        style="width: 100%"
                        :disabled="userRole === 'DORM_ADMIN'"
                    >
                        <el-option
                            v-for="building in buildingList"
                            :key="building.id"
                            :label="building.name"
                            :value="building.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="入住日期" prop="checkInDate">
                    <el-date-picker
                        v-model="bulkForm.checkInDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        format="YYYY-MM-DD"
                        placeholder="请选择入住日期"
                        style="width: 100%"
                    />
                </el-form-item>
            </el-form>

            <el-alert
                title="分配策略说明"
                type="info"
                show-icon
                :closable="false"
                description="系统会优先筛选当前未入住的学生，并只使用所选楼栋中的空房间。分配顺序按班级、专业、学号排序；同班同专业会尽量连续分配，新的班级会优先从新房间开始，避免不同班级混住。"
            />

            <template #footer>
                <el-button @click="bulkDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleBulkSubmit">开始分配</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="checkOutDialogVisible" title="办理退宿" width="420px">
            <el-form label-width="100px">
                <el-form-item label="退宿日期">
                    <el-date-picker
                        v-model="checkOutDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        format="YYYY-MM-DD"
                        placeholder="请选择退宿日期"
                        style="width: 100%"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="checkOutDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleCheckOutSubmit">确定</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="transferDialogVisible" title="办理调宿" width="620px" @close="resetTransferForm">
            <el-form ref="transferFormRef" :model="transferForm" :rules="transferFormRules" label-width="100px">
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
                            :label="`${room.roomNumber}（已住 ${room.currentOccupancy}/${room.capacity}）`"
                            :value="room.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="新床位号" prop="newBedNumber">
                    <el-input v-model="transferForm.newBedNumber" placeholder="请输入新床位号，例如 A1" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="transferDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleTransferSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Operation, Plus } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const addDialogVisible = ref(false)
const bulkDialogVisible = ref(false)
const checkOutDialogVisible = ref(false)
const transferDialogVisible = ref(false)

const addFormRef = ref(null)
const bulkFormRef = ref(null)
const transferFormRef = ref(null)

const userRole = localStorage.getItem('role')
const currentAssignmentId = ref(null)
const currentDormAdminBuildingId = ref(null)
const checkOutDate = ref(new Date().toISOString().split('T')[0])

const searchForm = reactive({
    buildingId: null,
    status: ''
})

const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const assignmentList = ref([])
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

const bulkForm = reactive({
    buildingId: null,
    checkInDate: new Date().toISOString().split('T')[0]
})

const transferForm = reactive({
    newRoomId: null,
    newBedNumber: '',
    buildingId: null
})

const addFormRules = {
    studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
    buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
    roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
    bedNumber: [{ required: true, message: '请输入床位号', trigger: 'blur' }],
    checkInDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }]
}

const bulkFormRules = {
    buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }]
}

const transferFormRules = {
    newRoomId: [{ required: true, message: '请选择新房间', trigger: 'change' }],
    newBedNumber: [{ required: true, message: '请输入新床位号', trigger: 'blur' }]
}

const getStatusType = (status) => ({
    ACTIVE: 'success',
    INACTIVE: 'info',
    PENDING: 'warning'
}[status] || 'info')

const getStatusText = (status) => ({
    ACTIVE: '在住',
    INACTIVE: '已退宿',
    PENDING: '待入住'
}[status] || status)

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
    } finally {
        loading.value = false
    }
}

const loadBuildingList = async () => {
    const res = await axios.get('/api/building/accessible')
    buildingList.value = Array.isArray(res.data) ? res.data : []

    if (userRole === 'DORM_ADMIN' && buildingList.value.length > 0) {
        currentDormAdminBuildingId.value = buildingList.value[0].id
        searchForm.buildingId = currentDormAdminBuildingId.value
        addForm.buildingId = currentDormAdminBuildingId.value
        bulkForm.buildingId = currentDormAdminBuildingId.value
    }
}

const loadStudentList = async () => {
    const res = await axios.get('/api/student/list', {
        params: { page: 1, size: 1000 }
    })
    studentList.value = res.data.list || []
}

const loadAvailableRooms = async (buildingId) => {
    if (!buildingId) {
        availableRooms.value = []
        return
    }

    const res = await axios.get('/api/assignment/available-rooms', {
        params: { buildingId }
    })
    availableRooms.value = Array.isArray(res.data) ? res.data : []
}

const handleSearch = () => {
    pagination.page = 1
    loadAssignmentList()
}

const handleReset = () => {
    searchForm.buildingId = userRole === 'DORM_ADMIN' ? currentDormAdminBuildingId.value : null
    searchForm.status = ''
    handleSearch()
}

const openAddDialog = async () => {
    await loadStudentList()
    if (addForm.buildingId) {
        await loadAvailableRooms(addForm.buildingId)
    }
    addDialogVisible.value = true
}

const openBulkDialog = () => {
    bulkDialogVisible.value = true
}

const openCheckOutDialog = (row) => {
    currentAssignmentId.value = row.id
    checkOutDate.value = new Date().toISOString().split('T')[0]
    checkOutDialogVisible.value = true
}

const openTransferDialog = async (row) => {
    currentAssignmentId.value = row.id
    transferForm.newRoomId = null
    transferForm.newBedNumber = ''
    transferForm.buildingId = row.buildingId || searchForm.buildingId || currentDormAdminBuildingId.value
    await loadAvailableRooms(transferForm.buildingId)
    transferDialogVisible.value = true
}

const handleBuildingChange = async (buildingId) => {
    addForm.roomId = null
    await loadAvailableRooms(buildingId)
}

const handleAddSubmit = async () => {
    if (!addFormRef.value) {
        return
    }

    const valid = await addFormRef.value.validate().catch(() => false)
    if (!valid) {
        return
    }

    submitting.value = true
    try {
        await axios.post('/api/assignment', addForm)
        ElMessage.success('宿舍分配成功')
        addDialogVisible.value = false
        resetAddForm()
        await loadAssignmentList()
    } finally {
        submitting.value = false
    }
}

const handleBulkSubmit = async () => {
    if (!bulkFormRef.value) {
        return
    }

    const valid = await bulkFormRef.value.validate().catch(() => false)
    if (!valid) {
        return
    }

    submitting.value = true
    try {
        const res = await axios.post('/api/assignment/bulk-auto', bulkForm)
        const summary = res.data || {}
        ElMessage.success(
            `批量分配完成：候选 ${summary.candidateCount || 0} 人，成功 ${summary.assignedCount || 0} 人，未分配 ${summary.unassignedCount || 0} 人，使用房间 ${summary.usedRooms || 0} 间`
        )
        bulkDialogVisible.value = false
        resetBulkForm()
        await loadAssignmentList()
    } finally {
        submitting.value = false
    }
}

const handleCheckOutSubmit = async () => {
    submitting.value = true
    try {
        await axios.put(`/api/assignment/${currentAssignmentId.value}/checkout`, null, {
            params: { checkOutDate: checkOutDate.value }
        })
        ElMessage.success('退宿成功')
        checkOutDialogVisible.value = false
        await loadAssignmentList()
    } finally {
        submitting.value = false
    }
}

const handleTransferSubmit = async () => {
    if (!transferFormRef.value) {
        return
    }

    const valid = await transferFormRef.value.validate().catch(() => false)
    if (!valid) {
        return
    }

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
        resetTransferForm()
        await loadAssignmentList()
    } finally {
        submitting.value = false
    }
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除这条宿舍分配记录吗？此操作不可恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        await axios.delete(`/api/assignment/${row.id}`)
        ElMessage.success('删除成功')
        await loadAssignmentList()
    }).catch(() => {})
}

const resetAddForm = () => {
    addFormRef.value?.resetFields()
    addForm.studentId = null
    addForm.buildingId = userRole === 'DORM_ADMIN' ? currentDormAdminBuildingId.value : null
    addForm.roomId = null
    addForm.bedNumber = ''
    addForm.checkInDate = new Date().toISOString().split('T')[0]
    availableRooms.value = []
}

const resetBulkForm = () => {
    bulkFormRef.value?.resetFields()
    bulkForm.buildingId = userRole === 'DORM_ADMIN' ? currentDormAdminBuildingId.value : null
    bulkForm.checkInDate = new Date().toISOString().split('T')[0]
}

const resetTransferForm = () => {
    transferFormRef.value?.resetFields()
    transferForm.newRoomId = null
    transferForm.newBedNumber = ''
    transferForm.buildingId = null
    availableRooms.value = []
}

onMounted(async () => {
    await loadBuildingList()
    await loadAssignmentList()
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
    gap: 12px;
    flex-wrap: wrap;
}

.header-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.search-form {
    margin-bottom: 20px;
}

.row-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}
</style>
