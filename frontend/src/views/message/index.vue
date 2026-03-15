<template>
    <div class="message-center">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>消息中心</span>
                    <el-button type="primary" size="small" @click="handleRefresh">
                        <el-icon><Refresh /></el-icon>
                        刷新
                    </el-button>
                </div>
            </template>

            <!-- 消息类型筛选 -->
            <el-form :inline="true" class="search-form">
                <el-form-item label="消息状态">
                    <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 120px" @change="loadMessageList">
                        <el-option label="未读" value="UNREAD" />
                        <el-option label="已读" value="READ" />
                    </el-select>
                </el-form-item>
            </el-form>

            <!-- 消息列表 -->
            <el-table :data="messageList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="title" label="标题" min-width="200">
                    <template #default="{ row }">
                        <el-tag v-if="row.status === 'UNREAD'" type="danger" size="small" style="margin-right: 5px">新</el-tag>
                        {{ row.title }}
                    </template>
                </el-table-column>
                <el-table-column prop="type" label="类型" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getTypeType(row.type)">
                            {{ getTypeText(row.type) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="sendTime" label="发送时间" width="180" />
                <el-table-column prop="status" label="状态" width="80">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 'UNREAD' ? 'danger' : 'success'" size="small">
                            {{ row.status === 'UNREAD' ? '未读' : '已读' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
                        <el-button type="success" size="small" @click="handleMarkAsRead(row)" v-if="row.status === 'UNREAD'">标为已读</el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

        <!-- 查看消息弹窗 -->
        <el-dialog
            v-model="viewDialogVisible"
            :title="currentMessage.title"
            width="600px"
        >
            <el-descriptions :column="1" border>
                <el-descriptions-item label="消息类型">
                    <el-tag :type="getTypeType(currentMessage.type)">
                        {{ getTypeText(currentMessage.type) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="发送时间">{{ currentMessage.sendTime }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                    <el-tag :type="currentMessage.status === 'UNREAD' ? 'danger' : 'success'" size="small">
                        {{ currentMessage.status === 'UNREAD' ? '未读' : '已读' }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="内容" label-class-name="content-label">
                    <div class="message-content">{{ currentMessage.content }}</div>
                </el-descriptions-item>
            </el-descriptions>
            <template #footer>
                <el-button @click="viewDialogVisible = false">关闭</el-button>
                <el-button type="primary" @click="handleMarkAsReadFromView" v-if="currentMessage.status === 'UNREAD'">标记为已读</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from '@/utils/axios'

const loading = ref(false)
const filterStatus = ref('')
const viewDialogVisible = ref(false)

const messageList = ref([])
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

const currentMessage = reactive({
    id: null,
    title: '',
    content: '',
    type: '',
    status: '',
    sendTime: ''
})

const getTypeType = (type) => {
    const typeMap = {
        'ATTENDANCE': 'warning',
        'REPAIR': 'info',
        'LEAVE': 'success',
        'SYSTEM': 'danger'
    }
    return typeMap[type] || 'info'
}

const getTypeText = (type) => {
    const textMap = {
        'ATTENDANCE': '查寝',
        'REPAIR': '报修',
        'LEAVE': '请假',
        'SYSTEM': '系统'
    }
    return textMap[type] || type
}

const loadMessageList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/message/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                status: filterStatus.value || undefined
            }
        })
        messageList.value = res.data.list
        pagination.total = res.data.total
    } catch (error) {
        console.error('加载消息列表失败:', error)
    } finally {
        loading.value = false
    }
}

const handleRefresh = () => {
    loadMessageList()
    // 触发父组件的未读消息数量更新
    window.dispatchEvent(new CustomEvent('update-unread-count'))
}

const handleView = (row) => {
    currentMessage.id = row.id
    currentMessage.title = row.title
    currentMessage.content = row.content
    currentMessage.type = row.type
    currentMessage.status = row.status
    currentMessage.sendTime = row.sendTime
    viewDialogVisible.value = true
}

const handleMarkAsRead = async (row) => {
    try {
        await axios.put(`/api/message/${row.id}/read`)
        ElMessage.success('已标记为已读')
        loadMessageList()
        window.dispatchEvent(new CustomEvent('update-unread-count'))
    } catch (error) {
        console.error('标记失败:', error)
    }
}

const handleMarkAsReadFromView = async () => {
    await handleMarkAsRead({ id: currentMessage.id })
    currentMessage.status = 'READ'
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该消息吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/message/${row.id}`)
            ElMessage.success('删除成功')
            loadMessageList()
            window.dispatchEvent(new CustomEvent('update-unread-count'))
        } catch (error) {
            console.error('删除失败:', error)
        }
    }).catch(() => {})
}

const handleSizeChange = () => {
    loadMessageList()
}

const handlePageChange = () => {
    loadMessageList()
}

onMounted(() => {
    loadMessageList()
})
</script>

<style scoped>
.message-center {
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

.content-label {
    width: 100px;
}

.message-content {
    white-space: pre-wrap;
    line-height: 1.6;
}
</style>
