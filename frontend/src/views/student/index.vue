<template>
    <div class="student-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>学生信息管理</span>
                    <div class="header-actions">
                        <el-button
                            v-if="userRole === 'SUPER_ADMIN'"
                            type="success"
                            @click="openBulkCounselorDialog"
                        >
                            <el-icon><Operation /></el-icon>
                            批量分配辅导员
                        </el-button>
                        <el-button type="warning" @click="handleExport">
                            <el-icon><Download /></el-icon>
                            导出名单
                        </el-button>
                        <el-button type="success" @click="handleDownloadTemplate">
                            <el-icon><Download /></el-icon>
                            下载模板
                        </el-button>
                        <el-button v-if="userRole === 'SUPER_ADMIN'" type="primary" @click="openImportDialog">
                            <el-icon><Upload /></el-icon>
                            批量导入
                        </el-button>
                        <el-button type="primary" @click="openStudentDialog">
                            <el-icon><Plus /></el-icon>
                            新增学生
                        </el-button>
                    </div>
                </div>
            </template>

            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="班级">
                    <el-input v-model="searchForm.className" placeholder="请输入班级" clearable />
                </el-form-item>
                <el-form-item label="专业">
                    <el-input v-model="searchForm.major" placeholder="请输入专业" clearable />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>

            <el-table
                :data="studentList"
                v-loading="loading"
                border
                stripe
                style="width: 100%"
            >
                <template #empty>
                    <el-empty description="暂无学生数据" />
                </template>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="studentNumber" label="学号" width="150" />
                <el-table-column prop="realName" label="姓名" width="120" />
                <el-table-column prop="className" label="班级" min-width="140" />
                <el-table-column prop="major" label="专业" min-width="160" />
                <el-table-column prop="counselorName" label="辅导员" width="120" />
                <el-table-column prop="enrollmentDate" label="入学日期" width="120" />
                <el-table-column label="住宿状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getHousingStatusType(row.housingStatus)">
                            {{ getHousingStatusText(row.housingStatus) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" fixed="right" width="180">
                    <template #default="{ row }">
                        <div class="row-actions">
                            <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
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
                @size-change="loadStudentList"
                @current-change="loadStudentList"
            />
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" @close="resetStudentForm">
            <el-form ref="studentFormRef" :model="studentForm" :rules="studentFormRules" label-width="100px">
                <el-form-item label="学号" prop="studentNumber">
                    <el-input
                        v-model="studentForm.studentNumber"
                        placeholder="请输入学号"
                        :disabled="isEdit"
                    />
                </el-form-item>
                <el-form-item label="姓名" prop="realName">
                    <el-input v-model="studentForm.realName" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="班级" prop="className">
                    <el-input v-model="studentForm.className" placeholder="请输入班级" />
                </el-form-item>
                <el-form-item label="专业" prop="major">
                    <el-input v-model="studentForm.major" placeholder="请输入专业" />
                </el-form-item>
                <el-form-item v-if="!isCounselor" label="辅导员" prop="counselorId">
                    <el-select
                        v-model="studentForm.counselorId"
                        placeholder="请选择辅导员"
                        clearable
                        filterable
                        style="width: 100%"
                    >
                        <el-option
                            v-for="counselor in counselorList"
                            :key="counselor.id"
                            :label="counselor.realName"
                            :value="counselor.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="入学日期" prop="enrollmentDate">
                    <el-date-picker
                        v-model="studentForm.enrollmentDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        format="YYYY-MM-DD"
                        placeholder="请选择入学日期"
                        style="width: 100%"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="importDialogVisible" title="批量导入学生" width="520px">
            <el-alert
                title="导入说明"
                type="info"
                show-icon
                :closable="false"
                style="margin-bottom: 16px"
                description="请先下载模板再上传 Excel。新建学生的默认密码为学号后 6 位，不足 6 位时直接使用学号本身。"
            />
            <el-upload
                ref="uploadRef"
                drag
                :auto-upload="false"
                :on-change="handleFileChange"
                :limit="1"
                accept=".xlsx,.xls"
                style="width: 100%"
            >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">将文件拖到此处，或 <em>点击上传</em></div>
            </el-upload>
            <template #footer>
                <el-button @click="importDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="importing" :disabled="!selectedFile" @click="handleImportSubmit">
                    确定
                </el-button>
            </template>
        </el-dialog>

        <el-dialog
            v-model="bulkCounselorDialogVisible"
            title="批量分配辅导员"
            width="620px"
            @close="resetBulkCounselorForm"
        >
            <el-form ref="bulkCounselorFormRef" :model="bulkCounselorForm" label-width="130px">
                <el-form-item label="参与分配辅导员">
                    <el-select
                        v-model="bulkCounselorForm.counselorIds"
                        multiple
                        collapse-tags
                        collapse-tags-tooltip
                        clearable
                        filterable
                        placeholder="默认使用全部在职辅导员"
                        style="width: 100%"
                    >
                        <el-option
                            v-for="counselor in counselorList"
                            :key="counselor.id"
                            :label="counselor.realName"
                            :value="counselor.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="覆盖已有分配">
                    <el-switch v-model="bulkCounselorForm.overwriteExisting" />
                </el-form-item>
            </el-form>

            <el-alert
                title="策略说明"
                type="info"
                show-icon
                :closable="false"
                description="系统会尽量把同班学生整体分给同一个辅导员，并优先分配给当前学生负载较低的辅导员。默认只处理尚未绑定辅导员的学生；如果开启覆盖，系统会重新为所选范围内的学生进行分配。"
            />

            <template #footer>
                <el-button @click="bulkCounselorDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleBulkCounselorSubmit">
                    开始分配
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Operation, Plus, Upload, UploadFilled } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const bulkCounselorDialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const isEdit = ref(false)

const studentFormRef = ref(null)
const uploadRef = ref(null)
const bulkCounselorFormRef = ref(null)
const selectedFile = ref(null)

const userRole = localStorage.getItem('role')
const currentUserId = Number(localStorage.getItem('userId'))
const isCounselor = userRole === 'COUNSELOR'

const searchForm = reactive({
    className: '',
    major: ''
})

const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const studentList = ref([])
const counselorList = ref([])

const studentForm = reactive({
    id: null,
    userId: null,
    studentNumber: '',
    realName: '',
    className: '',
    major: '',
    counselorId: null,
    enrollmentDate: ''
})

const bulkCounselorForm = reactive({
    counselorIds: [],
    overwriteExisting: false
})

const studentFormRules = {
    studentNumber: [{ required: true, message: '请输入学号', trigger: 'blur' }],
    realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
    major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

const getHousingStatusType = (status) => ({
    ACTIVE: 'success',
    INACTIVE: 'info',
    NONE: 'warning'
}[status] || 'info')

const getHousingStatusText = (status) => ({
    ACTIVE: '在住',
    INACTIVE: '已退宿',
    NONE: '未入住'
}[status] || '未知')

const loadStudentList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/student/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                className: searchForm.className || undefined,
                major: searchForm.major || undefined
            }
        })
        studentList.value = res.data.list || []
        pagination.total = res.data.total || 0
    } finally {
        loading.value = false
    }
}

const loadCounselorList = async () => {
    const res = await axios.get('/api/user/list', {
        params: {
            page: 1,
            size: 1000,
            role: 'COUNSELOR',
            status: 1
        }
    })
    counselorList.value = (res.data.list || []).filter(user => user.role === 'COUNSELOR')
}

const handleSearch = () => {
    pagination.page = 1
    loadStudentList()
}

const handleReset = () => {
    searchForm.className = ''
    searchForm.major = ''
    handleSearch()
}

const openStudentDialog = async () => {
    dialogTitle.value = '新增学生'
    isEdit.value = false
    if (!isCounselor) {
        await loadCounselorList()
    }
    studentForm.counselorId = isCounselor ? currentUserId : null
    dialogVisible.value = true
}

const openEditDialog = async (row) => {
    dialogTitle.value = '编辑学生'
    isEdit.value = true
    if (!isCounselor) {
        await loadCounselorList()
    }
    studentForm.id = row.id
    studentForm.userId = row.userId
    studentForm.studentNumber = row.studentNumber
    studentForm.realName = row.realName
    studentForm.className = row.className
    studentForm.major = row.major
    studentForm.counselorId = isCounselor ? currentUserId : row.counselorId
    studentForm.enrollmentDate = row.enrollmentDate
    dialogVisible.value = true
}

const resetStudentForm = () => {
    studentFormRef.value?.resetFields()
    studentForm.id = null
    studentForm.userId = null
    studentForm.studentNumber = ''
    studentForm.realName = ''
    studentForm.className = ''
    studentForm.major = ''
    studentForm.counselorId = isCounselor ? currentUserId : null
    studentForm.enrollmentDate = ''
}

const handleSubmit = async () => {
    if (!studentFormRef.value) {
        return
    }

    const valid = await studentFormRef.value.validate().catch(() => false)
    if (!valid) {
        return
    }

    submitting.value = true
    try {
        if (isCounselor) {
            studentForm.counselorId = currentUserId
        }

        if (isEdit.value) {
            await axios.put(`/api/student/${studentForm.id}`, studentForm)
            ElMessage.success('学生信息更新成功')
        } else {
            await axios.post('/api/student', studentForm)
            ElMessage.success('学生创建成功，默认密码为学号后 6 位')
        }
        dialogVisible.value = false
        resetStudentForm()
        await loadStudentList()
    } finally {
        submitting.value = false
    }
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该学生吗？删除后无法恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        await axios.delete(`/api/student/${row.id}`)
        ElMessage.success('删除成功')
        await loadStudentList()
    }).catch(() => {})
}

const handleExport = async () => {
    const response = await axios.get('/api/student/export', {
        responseType: 'blob',
        transformResponse: [(data) => data]
    })

    if (!(response.data instanceof Blob)) {
        ElMessage.error('导出失败：返回数据格式不正确')
        return
    }

    const url = window.URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '学生名单.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
}

const handleDownloadTemplate = async () => {
    const response = await axios.get('/api/student/template', {
        responseType: 'blob',
        transformResponse: [(data) => data]
    })

    if (!(response.data instanceof Blob)) {
        ElMessage.error('模板下载失败：返回数据格式不正确')
        return
    }

    const url = window.URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '学生导入模板.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
}

const openImportDialog = () => {
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    importDialogVisible.value = true
}

const handleFileChange = (file) => {
    selectedFile.value = file.raw
}

const handleImportSubmit = async () => {
    if (!selectedFile.value) {
        return
    }

    importing.value = true
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    try {
        await axios.post('/api/student/import', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
        ElMessage.success('导入完成')
        importDialogVisible.value = false
        await loadStudentList()
    } finally {
        importing.value = false
    }
}

const openBulkCounselorDialog = async () => {
    await loadCounselorList()
    bulkCounselorDialogVisible.value = true
}

const resetBulkCounselorForm = () => {
    bulkCounselorFormRef.value?.resetFields?.()
    bulkCounselorForm.counselorIds = []
    bulkCounselorForm.overwriteExisting = false
}

const handleBulkCounselorSubmit = async () => {
    submitting.value = true
    try {
        const res = await axios.post('/api/student/bulk-assign-counselors', {
            counselorIds: bulkCounselorForm.counselorIds,
            overwriteExisting: bulkCounselorForm.overwriteExisting
        })
        const summary = res.data || {}
        ElMessage.success(
            `批量分配完成：候选 ${summary.candidateCount || 0} 人，更新 ${summary.updatedCount || 0} 人，涉及辅导员 ${summary.counselorCount || 0} 人，班级分组 ${summary.groupCount || 0} 组`
        )
        bulkCounselorDialogVisible.value = false
        resetBulkCounselorForm()
        await loadStudentList()
    } finally {
        submitting.value = false
    }
}

onMounted(() => {
    loadStudentList()
})
</script>

<style scoped>
.student-management {
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
    gap: 8px;
}
</style>
