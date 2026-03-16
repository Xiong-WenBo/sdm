<template>
    <div class="building-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>楼栋管理</span>
                    <el-button type="primary" @click="handleAdd">
                        <el-icon><Plus /></el-icon>
                        新增楼栋
                    </el-button>
                </div>
            </template>

            <!-- 搜索栏 -->
            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="楼栋名称">
                    <el-input v-model="searchForm.name" placeholder="请输入楼栋名称" clearable />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 楼栋列表 -->
            <el-table :data="buildingList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="楼栋名称" width="200" />
                <el-table-column prop="address" label="地理位置" width="200" />
                <el-table-column prop="floors" label="总层数" width="100" />
                <el-table-column prop="adminName" label="管理员" width="120" />
                <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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

        <!-- 新增/编辑楼栋弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            :title="dialogTitle"
            width="600px"
            @close="handleDialogClose"
        >
            <el-form
                ref="buildingFormRef"
                :model="buildingForm"
                :rules="buildingFormRules"
                label-width="100px"
            >
                <el-form-item label="楼栋名称" prop="name">
                    <el-input v-model="buildingForm.name" placeholder="请输入楼栋名称（如：学宿 1 号楼）" />
                </el-form-item>
                <el-form-item label="地理位置" prop="address">
                    <el-input v-model="buildingForm.address" placeholder="请输入地理位置（如：校园东区）" />
                </el-form-item>
                <el-form-item label="总层数" prop="floors">
                    <el-input-number v-model="buildingForm.floors" :min="1" :max="100" style="width: 100%" />
                </el-form-item>
                <el-form-item label="管理员" prop="adminId">
                    <el-select
                        v-model="buildingForm.adminId"
                        placeholder="请选择楼栋管理员"
                        clearable
                        filterable
                        style="width: 100%"
                    >
                        <el-option
                            v-for="admin in availableAdmins"
                            :key="admin.id"
                            :label="admin.adminName"
                            :value="admin.id"
                        />
                    </el-select>
                    <el-alert
                        v-if="buildingForm.adminId"
                        type="info"
                        show-icon
                        style="margin-top: 10px"
                        description="每个管理员只能管理一栋楼"
                    />
                </el-form-item>
                <el-form-item label="描述" prop="description">
                    <el-input
                        v-model="buildingForm.description"
                        type="textarea"
                        :rows="3"
                        placeholder="请输入楼栋描述（可选）"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增楼栋')
const isEdit = ref(false)
const buildingFormRef = ref(null)

const searchForm = reactive({
    name: ''
})

const buildingList = ref([])
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const buildingForm = reactive({
    id: null,
    name: '',
    address: '',
    floors: null,
    adminId: null,
    description: ''
})

const availableAdmins = ref([])

const buildingFormRules = {
    name: [
        { required: true, message: '请输入楼栋名称', trigger: 'blur' }
    ],
    floors: [
        { required: true, message: '请输入总层数', trigger: 'blur' }
    ]
}

const loadBuildingList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/building/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                name: searchForm.name || undefined
            }
        })
        buildingList.value = res.data.list
        pagination.total = res.data.total
    } catch (error) {
        console.error('加载楼栋列表失败:', error)
    } finally {
        loading.value = false
    }
}

const loadAvailableAdmins = async () => {
    try {
        const res = await axios.get('/api/building/available-admins')
        availableAdmins.value = res.data
    } catch (error) {
        console.error('加载可用管理员失败:', error)
    }
}

const handleSearch = () => {
    pagination.page = 1
    loadBuildingList()
}

const handleReset = () => {
    searchForm.name = ''
    handleSearch()
}

const handleAdd = () => {
    dialogTitle.value = '新增楼栋'
    isEdit.value = false
    dialogVisible.value = true
    loadAvailableAdmins()
}

const handleEdit = (row) => {
    dialogTitle.value = '编辑楼栋'
    isEdit.value = true
    buildingForm.id = row.id
    buildingForm.name = row.name
    buildingForm.address = row.address
    buildingForm.floors = row.floors
    buildingForm.adminId = row.adminId
    buildingForm.description = row.description
    dialogVisible.value = true
    loadAvailableAdmins()
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该楼栋吗？删除后该楼栋下的所有房间也将被删除。', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/building/${row.id}`)
            ElMessage.success('删除成功')
            loadBuildingList()
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const handleSubmit = async () => {
    if (!buildingFormRef.value) return

    await buildingFormRef.value.validate(async (valid) => {
        if (!valid) return

        submitting.value = true
        try {
            if (isEdit.value) {
                await axios.put(`/api/building/${buildingForm.id}`, buildingForm)
                ElMessage.success('更新成功')
            } else {
                await axios.post('/api/building', buildingForm)
                ElMessage.success('创建成功')
            }
            dialogVisible.value = false
            loadBuildingList()
        } catch (error) {
            console.error('提交失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleDialogClose = () => {
    buildingFormRef.value?.resetFields()
    buildingForm.id = null
    buildingForm.name = ''
    buildingForm.address = ''
    buildingForm.floors = null
    buildingForm.adminId = null
    buildingForm.description = ''
}

const handleSizeChange = () => {
    loadBuildingList()
}

const handlePageChange = () => {
    loadBuildingList()
}

onMounted(() => {
    loadBuildingList()
})
</script>

<style scoped>
.building-management {
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
