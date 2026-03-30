<template>
    <div class="message-center">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>消息中心</span>
                    <div class="header-actions">
                        <el-button type="success" @click="handleSendPrivateMessage">
                            <el-icon><ChatDotRound /></el-icon>
                            主动私信
                        </el-button>
                        <el-button
                            v-if="currentUser?.role === 'SUPER_ADMIN'"
                            type="danger"
                            @click="handleSendBroadcast"
                        >
                            <el-icon><Bell /></el-icon>
                            通知广播
                        </el-button>
                        <el-button type="primary" size="small" @click="handleRefresh">
                            <el-icon><Refresh /></el-icon>
                            刷新
                        </el-button>
                    </div>
                </div>
            </template>

            <el-form :inline="true" class="search-form">
                <el-form-item label="消息状态">
                    <el-select
                        v-model="filterStatus"
                        placeholder="全部"
                        clearable
                        style="width: 120px"
                        @change="loadMessageList"
                    >
                        <el-option label="未读" value="UNREAD" />
                        <el-option label="已读" value="READ" />
                    </el-select>
                </el-form-item>
                <el-form-item label="消息分类">
                    <el-select
                        v-model="filterCategory"
                        placeholder="全部"
                        clearable
                        style="width: 150px"
                        @change="loadMessageList"
                    >
                        <el-option
                            v-for="cat in availableCategories"
                            :key="cat.value"
                            :label="cat.label"
                            :value="cat.value"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="消息类型">
                    <el-select
                        v-model="filterType"
                        placeholder="全部"
                        clearable
                        style="width: 120px"
                        @change="loadMessageList"
                    >
                        <el-option label="查寝" value="ATTENDANCE" />
                        <el-option label="报修" value="REPAIR" />
                        <el-option label="请假" value="LEAVE" />
                        <el-option label="系统" value="SYSTEM" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="success" @click="handleMarkAllAsRead" :disabled="unreadCount === 0">
                        一键已读全部
                    </el-button>
                </el-form-item>
            </el-form>

            <el-table :data="messageList" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="title" label="标题" min-width="220">
                    <template #default="{ row }">
                        <el-tag v-if="row.status === 'UNREAD'" type="danger" size="small" style="margin-right: 6px">
                            新
                        </el-tag>
                        <el-tag v-if="row.category === 'REPLY'" type="success" size="small" style="margin-right: 6px">
                            私信
                        </el-tag>
                        {{ row.title }}
                    </template>
                </el-table-column>
                <el-table-column prop="senderName" label="发送人" width="120">
                    <template #default="{ row }">
                        <span v-if="row.senderName">{{ row.senderName }}</span>
                        <span v-else class="muted-text">系统</span>
                    </template>
                </el-table-column>
                <el-table-column prop="type" label="类型" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getTypeType(row.type)">
                            {{ getTypeText(row.type) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="category" label="分类" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getCategoryType(row.category)" size="small">
                            {{ getCategoryText(row.category) }}
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
                <el-table-column label="操作" width="260" fixed="right">
                    <template #default="{ row }">
                        <div class="action-buttons">
                            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
                            <el-button
                                v-if="canReply(row)"
                                type="success"
                                size="small"
                                @click="handleReply(row)"
                            >
                                回复
                            </el-button>
                            <el-button
                                v-if="row.status === 'UNREAD'"
                                type="success"
                                size="small"
                                @click="handleMarkAsRead(row)"
                            >
                                标为已读
                            </el-button>
                            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                        </div>
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
            v-model="viewDialogVisible"
            :title="currentMessage.title"
            width="600px"
        >
            <el-descriptions :column="1" border>
                <el-descriptions-item label="发送人">
                    <span v-if="currentMessage.senderName">{{ currentMessage.senderName }}</span>
                    <span v-else class="muted-text">系统</span>
                </el-descriptions-item>
                <el-descriptions-item label="消息类型">
                    <el-tag :type="getTypeType(currentMessage.type)">
                        {{ getTypeText(currentMessage.type) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="消息分类">
                    <el-tag :type="getCategoryType(currentMessage.category)" size="small">
                        {{ getCategoryText(currentMessage.category) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="发送时间">
                    {{ currentMessage.sendTime }}
                </el-descriptions-item>
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
                <el-button v-if="canReply(currentMessage)" type="success" @click="handleReplyFromView">
                    回复
                </el-button>
                <el-button
                    v-if="currentMessage.status === 'UNREAD'"
                    type="primary"
                    @click="handleMarkAsReadFromView"
                >
                    标记为已读
                </el-button>
            </template>
        </el-dialog>

        <el-dialog
            v-model="sendMessageDialogVisible"
            :title="isBroadcast ? '发送通知广播' : '主动私信'"
            width="600px"
        >
            <el-form :model="messageForm" label-width="100px">
                <el-form-item v-if="!isBroadcast" label="接收用户">
                    <el-select
                        v-model="messageForm.userId"
                        placeholder="请选择接收用户"
                        filterable
                        style="width: 100%"
                    >
                        <el-option
                            v-for="user in userList"
                            :key="user.id"
                            :label="`${user.realName} (${user.username})`"
                            :value="user.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item v-else label="广播范围">
                    <el-select v-model="messageForm.targetRole" style="width: 100%">
                        <el-option label="所有人" value="ALL" />
                        <el-option label="学生" value="STUDENT" />
                        <el-option label="辅导员" value="COUNSELOR" />
                        <el-option label="宿管" value="DORM_ADMIN" />
                    </el-select>
                </el-form-item>
                <el-form-item label="消息标题">
                    <el-input v-model="messageForm.title" placeholder="请输入消息标题" />
                </el-form-item>
                <el-form-item label="消息内容">
                    <el-input
                        v-model="messageForm.content"
                        type="textarea"
                        :rows="6"
                        placeholder="请输入消息内容"
                    />
                </el-form-item>
                <el-form-item label="消息分类">
                    <el-select v-model="messageForm.category" style="width: 100%">
                        <el-option
                            v-for="cat in availableCategories"
                            :key="cat.value"
                            :label="cat.label"
                            :value="cat.value"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="sendMessageDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleConfirmSendMessage" :loading="sending">发送</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, ChatDotRound, Refresh } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const filterStatus = ref('')
const filterCategory = ref('')
const filterType = ref('')
const viewDialogVisible = ref(false)
const sendMessageDialogVisible = ref(false)
const sending = ref(false)
const unreadCount = ref(0)
const userList = ref([])
const isBroadcast = ref(false)
const currentUser = ref(null)
const selectedUserId = ref(null)
const selectedUserName = ref('')
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
    category: 'SYSTEM',
    senderId: null,
    senderName: '',
    status: '',
    sendTime: ''
})

const messageForm = reactive({
    userId: null,
    targetRole: 'ALL',
    title: '',
    content: '',
    category: 'REPLY'
})

const availableCategories = computed(() => {
    const role = currentUser.value?.role
    const categories = [{ value: 'REPLY', label: '回复消息' }]

    if (role === 'COUNSELOR' || role === 'DORM_ADMIN' || role === 'SUPER_ADMIN') {
        categories.push({ value: 'REMINDER', label: '提醒消息' })
    }

    if (role === 'SUPER_ADMIN') {
        categories.push({ value: 'SYSTEM', label: '系统通知' })
    }

    return categories
})

const canReply = (message) => message.senderId !== null && message.senderId !== undefined

const getTypeType = (type) => ({
    ATTENDANCE: 'warning',
    REPAIR: 'info',
    LEAVE: 'success',
    SYSTEM: 'danger'
}[type] || 'info')

const getTypeText = (type) => ({
    ATTENDANCE: '查寝',
    REPAIR: '报修',
    LEAVE: '请假',
    SYSTEM: '系统'
}[type] || type)

const getCategoryType = (category) => ({
    SYSTEM: 'info',
    REPLY: 'success',
    REMINDER: 'warning'
}[category] || 'info')

const getCategoryText = (category) => ({
    SYSTEM: '系统通知',
    REPLY: '回复消息',
    REMINDER: '提醒消息'
}[category] || category)

const loadMessageList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/message/list', {
            params: {
                page: pagination.page,
                size: pagination.size,
                status: filterStatus.value || undefined,
                category: filterCategory.value || undefined,
                type: filterType.value || undefined
            }
        })
        messageList.value = res.data.list
        pagination.total = res.data.total
        await updateUnreadCount()
    } catch (error) {
        console.error('加载消息列表失败:', error)
    } finally {
        loading.value = false
    }
}

const updateUnreadCount = async () => {
    try {
        const res = await axios.get('/api/message/unread/count')
        unreadCount.value = res.data
    } catch (error) {
        console.error('获取未读数量失败:', error)
    }
}

const handleRefresh = () => {
    loadMessageList()
    window.dispatchEvent(new CustomEvent('update-unread-count'))
}

const handleView = (row) => {
    currentMessage.id = row.id
    currentMessage.title = row.title
    currentMessage.content = row.content
    currentMessage.type = row.type
    currentMessage.category = row.category || 'SYSTEM'
    currentMessage.senderId = row.senderId
    currentMessage.senderName = row.senderName
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

const handleMarkAllAsRead = async () => {
    try {
        await axios.put('/api/message/read-all')
        ElMessage.success('已全部标记为已读')
        loadMessageList()
        window.dispatchEvent(new CustomEvent('update-unread-count'))
    } catch (error) {
        console.error('全部标记已读失败:', error)
    }
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除这条消息吗？', '警告', {
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

const handleSendPrivateMessage = () => {
    messageForm.userId = null
    messageForm.targetRole = 'ALL'
    messageForm.title = ''
    messageForm.content = ''
    messageForm.category = 'REPLY'
    selectedUserId.value = null
    selectedUserName.value = ''
    isBroadcast.value = false
    loadUserList()
    sendMessageDialogVisible.value = true
}

const handleSendBroadcast = () => {
    messageForm.targetRole = 'ALL'
    messageForm.title = ''
    messageForm.content = ''
    messageForm.category = 'SYSTEM'
    isBroadcast.value = true
    sendMessageDialogVisible.value = true
}

const handleReply = (row) => {
    messageForm.userId = row.senderId
    messageForm.title = `Re: ${row.title}`
    messageForm.content = ''
    messageForm.category = 'REPLY'
    isBroadcast.value = false
    loadUserList()
    sendMessageDialogVisible.value = true
}

const handleReplyFromView = () => {
    handleReply(currentMessage)
}

const loadUserList = async () => {
    try {
        const res = await axios.get('/api/user/directory')
        userList.value = res.data || []
        if (selectedUserId.value) {
            messageForm.userId = selectedUserId.value
        }
    } catch (error) {
        console.error('加载用户列表失败:', error)
    }
}

const handleConfirmSendMessage = async () => {
    if (!isBroadcast.value && !messageForm.userId) {
        ElMessage.warning('请选择接收用户')
        return
    }
    if (!messageForm.title || !messageForm.content) {
        ElMessage.warning('请填写消息标题和内容')
        return
    }

    sending.value = true
    try {
        if (isBroadcast.value) {
            await axios.post('/api/message/broadcast', {
                targetRole: messageForm.targetRole,
                title: messageForm.title,
                content: messageForm.content,
                category: messageForm.category
            })
            ElMessage.success('广播发送成功')
        } else {
            await axios.post('/api/message/send', {
                userId: messageForm.userId,
                title: messageForm.title,
                content: messageForm.content,
                type: 'SYSTEM',
                category: messageForm.category
            })
            ElMessage.success('发送成功')
        }
        sendMessageDialogVisible.value = false
        loadMessageList()
        window.dispatchEvent(new CustomEvent('update-unread-count'))
    } catch (error) {
        console.error('发送失败:', error)
        ElMessage.error(`发送失败：${error.response?.data?.message || '未知错误'}`)
    } finally {
        sending.value = false
    }
}

const handleSizeChange = () => loadMessageList()
const handlePageChange = () => loadMessageList()

window.addEventListener('send-private-message', (event) => {
    if (event.detail) {
        selectedUserId.value = event.detail.userId
        selectedUserName.value = event.detail.userName
        handleSendPrivateMessage()
    }
})

onMounted(async () => {
    try {
        const res = await axios.get('/api/user/current')
        currentUser.value = res.data
    } catch (error) {
        console.error('获取当前用户信息失败:', error)
    }

    loadMessageList()
    updateUnreadCount()
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
    gap: 12px;
    flex-wrap: wrap;
}

.header-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.header-actions :deep(.el-button) {
    margin-left: 0;
}

.search-form {
    margin-bottom: 20px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px 0;
}

.search-form :deep(.el-form-item) {
    margin-right: 16px;
    margin-bottom: 12px;
}

.action-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.action-buttons :deep(.el-button) {
    margin-left: 0;
}

.content-label {
    width: 100px;
}

.message-content {
    white-space: pre-wrap;
    line-height: 1.6;
}

.muted-text {
    color: #999;
}

@media (max-width: 1200px) {
    .search-form :deep(.el-form-item) {
        margin-right: 12px;
    }
}
</style>
