<template>
    <div class="room-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>房间管理</span>
                    <div class="header-actions">
                        <el-button type="success" @click="handleBulkCreate">
                            <el-icon><Plus /></el-icon>
                            批量生成房间
                        </el-button>
                        <el-button type="primary" @click="handleAdd">
                            <el-icon><Plus /></el-icon>
                            新增房间
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
                        style="width: 200px"
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
                        style="width: 150px"
                    >
                        <el-option label="可住" value="AVAILABLE" />
                        <el-option label="已满" value="FULL" />
                        <el-option label="维修" value="MAINTENANCE" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <el-table
                :data="roomList"
                v-loading="loading"
                element-loading-text="加载中..."
                border
                stripe
                style="width: 100%"
            >
                <template #empty>
                    <el-empty description="暂无房间数据" />
                </template>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="buildingName" label="楼栋" width="150" />
                <el-table-column prop="roomNumber" label="房间号" width="120" />
                <el-table-column prop="floor" label="楼层" width="80" />
                <el-table-column label="入住情况" width="120">
                    <template #default="{ row }">
                        {{ row.currentOccupancy }} / {{ row.capacity }}
                    </template>
                </el-table-column>
                <el-table-column prop="gender" label="性别限制" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getGenderType(row.gender)">
                            {{ getGenderText(row.gender) }}
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
                <el-table-column label="操作" fixed="right" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                        <el-button
                            v-if="userRole === 'SUPER_ADMIN'"
                            type="danger"
                            size="small"
                            @click="handleDelete(row)"
                        >
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <Pagination
                v-model="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                @size-change="handleSizeChange"
                @current-change="handlePageChange"
            />
        </el-card>

        <el-dialog
            v-model="dialogVisible"
            :title="dialogTitle"
            width="600px"
            @close="handleDialogClose"
        >
            <el-form
                ref="roomFormRef"
                :model="roomForm"
                :rules="roomFormRules"
                label-width="100px"
            >
                <el-form-item label="所属楼栋" prop="buildingId">
                    <el-select
                        v-model="roomForm.buildingId"
                        placeholder="请选择楼栋"
                        filterable
                        style="width: 100%"
                        :disabled="userRole === 'DORM_ADMIN' && userBuildingId !== null"
                    >
                        <el-option
                            v-for="building in buildingList"
                            :key="building.id"
                            :label="building.name"
                            :value="building.id"
                        />
                    </el-select>
                    <el-alert
                        v-if="userRole === 'DORM_ADMIN' && userBuildingId"
                        type="info"
                        show-icon
                        style="margin-top: 10px"
                        description="宿管只能在自己负责的楼栋管理房间。"
                    />
                </el-form-item>
                <el-form-item label="房间号" prop="roomNumber">
                    <el-input v-model="roomForm.roomNumber" placeholder="请输入房间号，例如 101" />
                </el-form-item>
                <el-form-item label="楼层" prop="floor">
                    <el-input-number v-model="roomForm.floor" :min="1" :max="100" style="width: 100%" />
                </el-form-item>
                <el-form-item label="可住人数" prop="capacity">
                    <el-input-number v-model="roomForm.capacity" :min="1" :max="20" style="width: 100%" />
                </el-form-item>
                <el-form-item label="性别限制" prop="gender">
                    <el-radio-group v-model="roomForm.gender">
                        <el-radio label="MALE">男</el-radio>
                        <el-radio label="FEMALE">女</el-radio>
                        <el-radio label="UNISEX">不限</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="roomForm.status">
                        <el-radio label="AVAILABLE">可住</el-radio>
                        <el-radio label="FULL">已满</el-radio>
                        <el-radio label="MAINTENANCE">维修</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>

        <el-dialog
            v-model="bulkDialogVisible"
            title="批量生成房间"
            width="620px"
            @close="handleBulkDialogClose"
        >
            <el-form
                ref="bulkFormRef"
                :model="bulkForm"
                :rules="bulkFormRules"
                label-width="120px"
            >
                <el-form-item label="所属楼栋" prop="buildingId">
                    <el-select
                        v-model="bulkForm.buildingId"
                        placeholder="请选择楼栋"
                        filterable
                        style="width: 100%"
                        :disabled="userRole === 'DORM_ADMIN' && userBuildingId !== null"
                    >
                        <el-option
                            v-for="building in buildingList"
                            :key="building.id"
                            :label="building.name"
                            :value="building.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="总楼层" prop="totalFloors">
                    <el-input-number v-model="bulkForm.totalFloors" :min="1" :max="100" style="width: 100%" />
                </el-form-item>
                <el-form-item label="每层房间数" prop="roomsPerFloor">
                    <el-input-number v-model="bulkForm.roomsPerFloor" :min="1" :max="200" style="width: 100%" />
                </el-form-item>
                <el-form-item label="房间容量" prop="capacity">
                    <el-input-number v-model="bulkForm.capacity" :min="1" :max="20" style="width: 100%" />
                </el-form-item>
                <el-form-item label="性别限制" prop="gender">
                    <el-radio-group v-model="bulkForm.gender">
                        <el-radio label="MALE">男</el-radio>
                        <el-radio label="FEMALE">女</el-radio>
                        <el-radio label="UNISEX">不限</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="初始状态" prop="status">
                    <el-radio-group v-model="bulkForm.status">
                        <el-radio label="AVAILABLE">可住</el-radio>
                        <el-radio label="MAINTENANCE">维修</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <el-alert
                title="房号规则"
                type="info"
                show-icon
                :closable="false"
                description="系统按“楼层 + 两位房间序号”生成房号，例如 101、102、201。若楼栋下已有重复房号，则整批拒绝创建。"
            />
            <template #footer>
                <el-button @click="bulkDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleBulkSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const bulkDialogVisible = ref(false)
const dialogTitle = ref('新增房间')
const isEdit = ref(false)
const roomFormRef = ref(null)
const bulkFormRef = ref(null)

const userRole = localStorage.getItem('role')

const searchForm = reactive({
    buildingId: null,
    status: ''
})

const roomList = ref([])
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const buildingList = ref([])
const userBuildingId = ref(null)

const roomForm = reactive({
    id: null,
    buildingId: null,
    roomNumber: '',
    floor: 1,
    capacity: 4,
    currentOccupancy: 0,
    gender: 'UNISEX',
    status: 'AVAILABLE'
})

const bulkForm = reactive({
    buildingId: null,
    totalFloors: 1,
    roomsPerFloor: 10,
    capacity: 4,
    gender: 'UNISEX',
    status: 'AVAILABLE'
})

const roomFormRules = {
    buildingId: [{ required: true, message: '请选择所属楼栋', trigger: 'change' }],
    roomNumber: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
    floor: [{ required: true, message: '请输入楼层', trigger: 'blur' }],
    capacity: [{ required: true, message: '请输入可住人数', trigger: 'blur' }]
}

const bulkFormRules = {
    buildingId: [{ required: true, message: '请选择所属楼栋', trigger: 'change' }],
    totalFloors: [{ required: true, message: '请输入总楼层', trigger: 'blur' }],
    roomsPerFloor: [{ required: true, message: '请输入每层房间数', trigger: 'blur' }],
    capacity: [{ required: true, message: '请输入房间容量', trigger: 'blur' }]
}

const getGenderType = (gender) => ({
    MALE: 'primary',
    FEMALE: 'danger',
    UNISEX: 'info'
}[gender] || 'info')

const getGenderText = (gender) => ({
    MALE: '男',
    FEMALE: '女',
    UNISEX: '不限'
}[gender] || gender)

const getStatusType = (status) => ({
    AVAILABLE: 'success',
    FULL: 'warning',
    MAINTENANCE: 'danger'
}[status] || 'info')

const getStatusText = (status) => ({
    AVAILABLE: '可住',
    FULL: '已满',
    MAINTENANCE: '维修'
}[status] || status)

const loadRoomList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/room/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                buildingId: searchForm.buildingId || undefined,
                status: searchForm.status || undefined
            }
        })
        roomList.value = res.data.list
        pagination.total = res.data.total
    } catch (error) {
        console.error('加载房间列表失败:', error)
    } finally {
        loading.value = false
    }
}

const loadBuildingList = async () => {
    try {
        const res = await axios.get('/api/building/accessible')
        buildingList.value = res.data

        if (userRole === 'DORM_ADMIN' && buildingList.value.length > 0) {
            userBuildingId.value = buildingList.value[0].id
            searchForm.buildingId = buildingList.value[0].id
            bulkForm.buildingId = buildingList.value[0].id
        }
    } catch (error) {
        console.error('加载楼栋列表失败:', error)
    }
}

const handleSearch = () => {
    pagination.page = 1
    loadRoomList()
}

const handleReset = () => {
    searchForm.buildingId = userRole === 'DORM_ADMIN' ? userBuildingId.value : null
    searchForm.status = ''
    handleSearch()
}

const handleAdd = () => {
    dialogTitle.value = '新增房间'
    isEdit.value = false
    if (userRole === 'DORM_ADMIN' && userBuildingId.value) {
        roomForm.buildingId = userBuildingId.value
    }
    dialogVisible.value = true
}

const handleBulkCreate = () => {
    if (userRole === 'DORM_ADMIN' && userBuildingId.value) {
        bulkForm.buildingId = userBuildingId.value
    }
    bulkDialogVisible.value = true
}

const handleEdit = (row) => {
    dialogTitle.value = '编辑房间'
    isEdit.value = true
    roomForm.id = row.id
    roomForm.buildingId = row.buildingId
    roomForm.roomNumber = row.roomNumber
    roomForm.floor = row.floor
    roomForm.capacity = row.capacity
    roomForm.currentOccupancy = row.currentOccupancy
    roomForm.gender = row.gender
    roomForm.status = row.status
    dialogVisible.value = true
}

const handleDelete = (row) => {
    ElMessageBox.confirm(
        '确定要删除该房间吗？删除后该房间的住宿分配记录也将被删除。',
        '警告',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }
    ).then(async () => {
        try {
            await axios.delete(`/api/room/${row.id}`)
            ElMessage.success('删除成功')
            loadRoomList()
        } catch (error) {
            console.error('删除房间失败:', error)
        }
    }).catch(() => {})
}

const handleSubmit = async () => {
    if (!roomFormRef.value) return

    await roomFormRef.value.validate(async (valid) => {
        if (!valid) return

        submitting.value = true
        try {
            if (isEdit.value) {
                await axios.put(`/api/room/${roomForm.id}`, roomForm)
                ElMessage.success('更新成功')
            } else {
                await axios.post('/api/room', roomForm)
                ElMessage.success('创建成功')
            }
            dialogVisible.value = false
            loadRoomList()
        } catch (error) {
            console.error('提交房间失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleBulkSubmit = async () => {
    if (!bulkFormRef.value) return

    await bulkFormRef.value.validate(async (valid) => {
        if (!valid) return

        submitting.value = true
        try {
            await axios.post('/api/room/bulk', bulkForm)
            ElMessage.success('批量生成成功')
            bulkDialogVisible.value = false
            loadRoomList()
        } catch (error) {
            console.error('批量生成房间失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleDialogClose = () => {
    roomFormRef.value?.resetFields()
    roomForm.id = null
    roomForm.buildingId = userRole === 'DORM_ADMIN' ? userBuildingId.value : null
    roomForm.roomNumber = ''
    roomForm.floor = 1
    roomForm.capacity = 4
    roomForm.currentOccupancy = 0
    roomForm.gender = 'UNISEX'
    roomForm.status = 'AVAILABLE'
}

const handleBulkDialogClose = () => {
    bulkFormRef.value?.resetFields()
    bulkForm.buildingId = userRole === 'DORM_ADMIN' ? userBuildingId.value : null
    bulkForm.totalFloors = 1
    bulkForm.roomsPerFloor = 10
    bulkForm.capacity = 4
    bulkForm.gender = 'UNISEX'
    bulkForm.status = 'AVAILABLE'
}

const handleSizeChange = () => loadRoomList()
const handlePageChange = () => loadRoomList()

onMounted(async () => {
    await loadBuildingList()
    await loadRoomList()
})
</script>

<style scoped>
.room-management {
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
</style>
