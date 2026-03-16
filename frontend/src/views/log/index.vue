<template>
    <div class="log-management">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>日志管理</span>
                </div>
            </template>

            <el-tabs v-model="activeTab" @tab-change="handleTabChange">
                <el-tab-pane label="登录日志" name="login">
                    <el-form :inline="true" :model="loginSearchForm" class="search-form">
                        <el-form-item label="用户名">
                            <el-input v-model="loginSearchForm.username" placeholder="请输入用户名" clearable />
                        </el-form-item>
                        <el-form-item label="状态">
                            <el-select v-model="loginSearchForm.status" placeholder="请选择状态" clearable style="width: 150px">
                                <el-option label="成功" value="SUCCESS" />
                                <el-option label="失败" value="FAILED" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="登录时间">
                            <el-date-picker
                                v-model="loginSearchForm.dateRange"
                                type="daterange"
                                range-separator="至"
                                start-placeholder="开始日期"
                                end-placeholder="结束日期"
                                value-format="YYYY-MM-DD"
                                style="width: 360px"
                            />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="handleLoginSearch">查询</el-button>
                            <el-button @click="handleLoginReset">重置</el-button>
                        </el-form-item>
                    </el-form>

                    <el-table :data="loginLogList" v-loading="loginLoading" border stripe style="width: 100%">
                        <el-table-column prop="id" label="ID" width="80" />
                        <el-table-column prop="username" label="用户名" width="150" />
                        <el-table-column prop="realName" label="真实姓名" width="120" />
                        <el-table-column prop="role" label="角色" width="120">
                            <template #default="{ row }">
                                <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="ipAddress" label="IP地址" width="140" />
                        <el-table-column prop="loginTime" label="登录时间" width="180" />
                        <el-table-column prop="status" label="状态" width="100">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
                                    {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="message" label="备注" min-width="150" show-overflow-tooltip />
                        <el-table-column label="操作" fixed="right" width="120">
                            <template #default="{ row }">
                                <el-button type="danger" size="small" @click="handleDeleteLoginLog(row)">删除</el-button>
                            </template>
                        </el-table-column>
                    </el-table>

                    <Pagination
                        v-model="loginPagination.page"
                        v-model:page-size="loginPagination.size"
                        :total="loginPagination.total"
                        @size-change="handleLoginSizeChange"
                        @current-change="handleLoginPageChange"
                    />
                </el-tab-pane>

                <el-tab-pane label="操作日志" name="operation">
                    <el-form :inline="true" :model="operationSearchForm" class="search-form">
                        <el-form-item label="用户名">
                            <el-input v-model="operationSearchForm.username" placeholder="请输入用户名" clearable />
                        </el-form-item>
                        <el-form-item label="模块">
                            <el-select v-model="operationSearchForm.module" placeholder="请选择模块" clearable style="width: 150px">
                                <el-option label="用户管理" value="USER" />
                                <el-option label="楼栋管理" value="BUILDING" />
                                <el-option label="房间管理" value="ROOM" />
                                <el-option label="学生管理" value="STUDENT" />
                                <el-option label="宿舍分配" value="ASSIGNMENT" />
                                <el-option label="查寝管理" value="ATTENDANCE" />
                                <el-option label="报修管理" value="REPAIR" />
                                <el-option label="请假管理" value="LEAVE" />
                                <el-option label="消息管理" value="MESSAGE" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="操作">
                            <el-select v-model="operationSearchForm.operation" placeholder="请选择操作" clearable style="width: 150px">
                                <el-option label="新增" value="CREATE" />
                                <el-option label="修改" value="UPDATE" />
                                <el-option label="删除" value="DELETE" />
                                <el-option label="查询" value="QUERY" />
                                <el-option label="导出" value="EXPORT" />
                                <el-option label="导入" value="IMPORT" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="状态">
                            <el-select v-model="operationSearchForm.status" placeholder="请选择状态" clearable style="width: 150px">
                                <el-option label="成功" value="SUCCESS" />
                                <el-option label="失败" value="FAILED" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="操作时间">
                            <el-date-picker
                                v-model="operationSearchForm.dateRange"
                                type="daterange"
                                range-separator="至"
                                start-placeholder="开始日期"
                                end-placeholder="结束日期"
                                value-format="YYYY-MM-DD"
                                style="width: 360px"
                            />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="handleOperationSearch">查询</el-button>
                            <el-button @click="handleOperationReset">重置</el-button>
                        </el-form-item>
                    </el-form>

                    <el-table :data="operationLogList" v-loading="operationLoading" border stripe style="width: 100%">
                        <el-table-column prop="id" label="ID" width="80" />
                        <el-table-column prop="username" label="用户名" width="120" />
                        <el-table-column prop="realName" label="真实姓名" width="100" />
                        <el-table-column prop="role" label="角色" width="100">
                            <template #default="{ row }">
                                <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="module" label="模块" width="100">
                            <template #default="{ row }">
                                {{ getModuleText(row.module) }}
                            </template>
                        </el-table-column>
                        <el-table-column prop="operation" label="操作" width="80">
                            <template #default="{ row }">
                                {{ getOperationText(row.operation) }}
                            </template>
                        </el-table-column>
                        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
                        <el-table-column prop="requestMethod" label="方法" width="80" />
                        <el-table-column prop="requestUrl" label="URL" min-width="150" show-overflow-tooltip />
                        <el-table-column prop="ipAddress" label="IP地址" width="120" />
                        <el-table-column prop="operationTime" label="操作时间" width="160" />
                        <el-table-column prop="executionTime" label="耗时(ms)" width="100" />
                        <el-table-column prop="status" label="状态" width="80">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
                                    {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" fixed="right" width="120">
                            <template #default="{ row }">
                                <el-button type="danger" size="small" @click="handleDeleteOperationLog(row)">删除</el-button>
                            </template>
                        </el-table-column>
                    </el-table>

                    <Pagination
                        v-model="operationPagination.page"
                        v-model:page-size="operationPagination.size"
                        :total="operationPagination.total"
                        @size-change="handleOperationSizeChange"
                        @current-change="handleOperationPageChange"
                    />
                </el-tab-pane>
            </el-tabs>
        </el-card>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '@/utils/axios'
import { Role, RoleName } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const activeTab = ref('login')

const loginLoading = ref(false)
const loginLogList = ref([])
const loginSearchForm = reactive({
    username: '',
    status: '',
    dateRange: []
})
const loginPagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const operationLoading = ref(false)
const operationLogList = ref([])
const operationSearchForm = reactive({
    username: '',
    module: '',
    operation: '',
    status: '',
    dateRange: []
})
const operationPagination = reactive({
    page: 1,
    size: 10,
    total: 0
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

const getModuleText = (module) => {
    const moduleMap = {
        'USER': '用户管理',
        'BUILDING': '楼栋管理',
        'ROOM': '房间管理',
        'STUDENT': '学生管理',
        'ASSIGNMENT': '宿舍分配',
        'ATTENDANCE': '查寝管理',
        'REPAIR': '报修管理',
        'LEAVE': '请假管理',
        'MESSAGE': '消息管理'
    }
    return moduleMap[module] || module
}

const getOperationText = (operation) => {
    const operationMap = {
        'CREATE': '新增',
        'UPDATE': '修改',
        'DELETE': '删除',
        'QUERY': '查询',
        'EXPORT': '导出',
        'IMPORT': '导入'
    }
    return operationMap[operation] || operation
}

const loadLoginLogList = async () => {
    loginLoading.value = true
    try {
        const res = await axios.get('/api/log/login/list', {
            params: {
                page: loginPagination.page,
                size: loginPagination.size,
                username: loginSearchForm.username || undefined,
                status: loginSearchForm.status || undefined,
                startTime: loginSearchForm.dateRange?.[0] || undefined,
                endTime: loginSearchForm.dateRange?.[1] || undefined
            }
        })
        loginLogList.value = res.data.list
        loginPagination.total = res.data.total
    } catch (error) {
        console.error('加载登录日志失败:', error)
    } finally {
        loginLoading.value = false
    }
}

const handleLoginSearch = () => {
    loginPagination.page = 1
    loadLoginLogList()
}

const handleLoginReset = () => {
    loginSearchForm.username = ''
    loginSearchForm.status = ''
    loginSearchForm.dateRange = []
    handleLoginSearch()
}

const handleLoginSizeChange = () => {
    loadLoginLogList()
}

const handleLoginPageChange = () => {
    loadLoginLogList()
}

const handleDeleteLoginLog = (row) => {
    ElMessageBox.confirm('确定要删除该登录日志吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/log/login/${row.id}`)
            ElMessage.success('删除成功')
            loadLoginLogList()
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const loadOperationLogList = async () => {
    operationLoading.value = true
    try {
        const res = await axios.get('/api/log/operation/list', {
            params: {
                page: operationPagination.page,
                size: operationPagination.size,
                username: operationSearchForm.username || undefined,
                module: operationSearchForm.module || undefined,
                operation: operationSearchForm.operation || undefined,
                status: operationSearchForm.status || undefined,
                startTime: operationSearchForm.dateRange?.[0] || undefined,
                endTime: operationSearchForm.dateRange?.[1] || undefined
            }
        })
        operationLogList.value = res.data.list
        operationPagination.total = res.data.total
    } catch (error) {
        console.error('加载操作日志失败:', error)
    } finally {
        operationLoading.value = false
    }
}

const handleOperationSearch = () => {
    operationPagination.page = 1
    loadOperationLogList()
}

const handleOperationReset = () => {
    operationSearchForm.username = ''
    operationSearchForm.module = ''
    operationSearchForm.operation = ''
    operationSearchForm.status = ''
    operationSearchForm.dateRange = []
    handleOperationSearch()
}

const handleOperationSizeChange = () => {
    loadOperationLogList()
}

const handleOperationPageChange = () => {
    loadOperationLogList()
}

const handleDeleteOperationLog = (row) => {
    ElMessageBox.confirm('确定要删除该操作日志吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/log/operation/${row.id}`)
            ElMessage.success('删除成功')
            loadOperationLogList()
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const handleTabChange = (tab) => {
    if (tab === 'login') {
        loadLoginLogList()
    } else if (tab === 'operation') {
        loadOperationLogList()
    }
}

onMounted(() => {
    loadLoginLogList()
})
</script>

<style scoped>
.log-management {
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
