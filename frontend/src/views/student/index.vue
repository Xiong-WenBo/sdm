<template>
    <div class="student-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>学生信息管理</span>
                    <div>
                        <el-button type="warning" @click="handleExport">
                            <el-icon><Download /></el-icon>
                            导出名单
                        </el-button>
                        <el-button type="success" @click="handleDownloadTemplate">
                            <el-icon><Download /></el-icon>
                            下载模板
                        </el-button>
                        <el-button type="primary" @click="handleImport">
                            <el-icon><Upload /></el-icon>
                            批量导入
                        </el-button>
                        <el-button type="primary" @click="handleAdd">
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

            <el-table :data="studentList" v-loading="loading" element-loading-text="加载中..." border stripe style="width: 100%">
                <template #empty>
                    <el-empty description="暂无学生数据" />
                </template>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="studentNumber" label="学号" width="150" />
                <el-table-column prop="realName" label="姓名" width="120" />
                <el-table-column prop="className" label="班级" width="150" />
                <el-table-column prop="major" label="专业" width="180" />
                <el-table-column prop="counselorName" label="辅导员" width="120" />
                <el-table-column prop="enrollmentDate" label="入学日期" width="120" />
                <el-table-column label="住宿状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getHousingStatusType(row.housingStatus)">
                            {{ getHousingStatusText(row.housingStatus) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" fixed="right" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)" v-if="userRole === 'SUPER_ADMIN'">删除</el-button>
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
                ref="studentFormRef"
                :model="studentForm"
                :rules="studentFormRules"
                label-width="100px"
            >
                <el-form-item label="学号" prop="studentNumber">
                    <el-input v-model="studentForm.studentNumber" placeholder="请输入学号" :disabled="isEdit" />
                </el-form-item>
                <el-form-item label="姓名" prop="realName">
                    <el-input v-model="studentForm.realName" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="班级" prop="className">
                    <el-input v-model="studentForm.className" placeholder="请输入班级（如：计算机 2101 班）" />
                </el-form-item>
                <el-form-item label="专业" prop="major">
                    <el-input v-model="studentForm.major" placeholder="请输入专业" />
                </el-form-item>
                <el-form-item label="辅导员" prop="counselorId" v-if="!isCounselor">
                    <el-select v-model="studentForm.counselorId" placeholder="请选择辅导员" clearable filterable style="width: 100%">
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
                        placeholder="请选择入学日期"
                        style="width: 100%"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
            </template>
        </el-dialog>

        <el-dialog
            v-model="importDialogVisible"
            title="批量导入学生"
            width="500px"
        >
            <el-alert
                title="导入说明"
                type="info"
                show-icon
                style="margin-bottom: 20px"
                description="请先下载模板，填写后上传。默认密码为学号后六位，不足六位则为学号本身。"
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
                <div class="el-upload__text">
                    将文件拖到此处，或 <em>点击上传</em>
                </div>
            </el-upload>
            <template #footer>
                <el-button @click="importDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleImportSubmit" :loading="importing" :disabled="!selectedFile">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, UploadFilled } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const isEdit = ref(false)
const studentFormRef = ref(null)
const uploadRef = ref(null)
const selectedFile = ref(null)

const userRole = localStorage.getItem('role')
const currentUserId = Number(localStorage.getItem('userId'))
const isCounselor = userRole === 'COUNSELOR'

const searchForm = reactive({
    className: '',
    major: ''
})

const studentList = ref([])
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

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

const studentFormRules = {
    studentNumber: [
        { required: true, message: '请输入学号', trigger: 'blur' }
    ],
    realName: [
        { required: true, message: '请输入姓名', trigger: 'blur' }
    ],
    className: [
        { required: true, message: '请输入班级', trigger: 'blur' }
    ],
    major: [
        { required: true, message: '请输入专业', trigger: 'blur' }
    ]
}

const getHousingStatusType = (status) => {
    const typeMap = {
        ACTIVE: 'success',
        INACTIVE: 'info',
        NONE: 'warning'
    }
    return typeMap[status] || 'info'
}

const getHousingStatusText = (status) => {
    const textMap = {
        ACTIVE: '在住',
        INACTIVE: '已退宿',
        NONE: '未入住'
    }
    return textMap[status] || '未知'
}

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
        studentList.value = res.data.list
        pagination.total = res.data.total
    } catch (error) {
        console.error('加载学生列表失败:', error)
    } finally {
        loading.value = false
    }
}

const loadCounselorList = async () => {
    try {
        const res = await axios.get('/api/user/list', {
            params: { page: 1, size: 1000, role: 'COUNSELOR', status: 1 }
        })
        counselorList.value = res.data.list.filter(u => u.role === 'COUNSELOR')
    } catch (error) {
        console.error('加载辅导员列表失败:', error)
        counselorList.value = []
    }
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

const handleAdd = () => {
    dialogTitle.value = '新增学生'
    isEdit.value = false
    if (isCounselor) {
        studentForm.counselorId = currentUserId
    }
    dialogVisible.value = true
    if (!isCounselor) {
        loadCounselorList()
    }
}

const handleEdit = (row) => {
    dialogTitle.value = '编辑学生'
    isEdit.value = true
    studentForm.id = row.id
    studentForm.userId = row.userId
    studentForm.studentNumber = row.studentNumber
    studentForm.realName = row.realName
    studentForm.className = row.className
    studentForm.major = row.major
    studentForm.counselorId = isCounselor ? currentUserId : row.counselorId
    studentForm.enrollmentDate = row.enrollmentDate
    dialogVisible.value = true
    if (!isCounselor) {
        loadCounselorList()
    }
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该学生吗？删除后该学生的住宿分配记录也将被删除。', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/student/${row.id}`)
            ElMessage.success('删除成功')
            loadStudentList()
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const handleSubmit = async () => {
    if (!studentFormRef.value) return

    await studentFormRef.value.validate(async (valid) => {
        if (!valid) return

        submitting.value = true
        try {
            if (isCounselor) {
                studentForm.counselorId = currentUserId
            }

            if (isEdit.value) {
                await axios.put(`/api/student/${studentForm.id}`, studentForm)
                ElMessage.success('更新成功')
            } else {
                await axios.post('/api/student', studentForm)
                ElMessage.success('创建成功，默认密码为学号后六位')
            }
            dialogVisible.value = false
            loadStudentList()
        } catch (error) {
            console.error('提交失败:', error)
        } finally {
            submitting.value = false
        }
    })
}

const handleDialogClose = () => {
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

const handleExport = async () => {
    try {
        const response = await axios.get('/api/student/export', {
            responseType: 'blob',
            transformResponse: [(data) => data]
        })

        if (!(response.data instanceof Blob)) {
            ElMessage.error('导出失败：响应数据格式错误')
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
    } catch (error) {
        console.error('导出失败:', error)
    }
}

const handleDownloadTemplate = async () => {
    try {
        const response = await axios.get('/api/student/template', {
            responseType: 'blob',
            transformResponse: [(data) => data]
        })

        if (!(response.data instanceof Blob)) {
            ElMessage.error('下载失败：响应数据格式错误')
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
    } catch (error) {
        console.error('下载模板失败:', error)
    }
}

const handleImport = () => {
    importDialogVisible.value = true
    selectedFile.value = null
    uploadRef.value?.clearFiles()
}

const handleFileChange = (file) => {
    selectedFile.value = file.raw
}

const handleImportSubmit = async () => {
    if (!selectedFile.value) return

    importing.value = true
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    try {
        const res = await axios.post('/api/student/import', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })

        if (res.code === 200) {
            ElMessage.success(res.message || '导入成功')
            importDialogVisible.value = false
            loadStudentList()
        } else {
            ElMessage.error(res.message || '导入失败')
        }
    } catch (error) {
        console.error('导入失败:', error)
    } finally {
        importing.value = false
    }
}

const handleSizeChange = () => {
    loadStudentList()
}

const handlePageChange = () => {
    loadStudentList()
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
}

.search-form {
    margin-bottom: 20px;
}
</style>
