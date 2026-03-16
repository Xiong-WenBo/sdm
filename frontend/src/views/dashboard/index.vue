<template>
    <div class="dashboard">
        <!-- 超级管理员视图 -->
        <div v-if="userRole === 'SUPER_ADMIN'" class="dashboard-content">
            <el-row :gutter="20" class="stats-row">
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon building">
                            <el-icon><OfficeBuilding /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.totalBuildings || 0 }}</div>
                            <div class="stat-label">楼栋总数</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon room">
                            <el-icon><HomeFilled /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.totalRooms || 0 }}</div>
                            <div class="stat-label">房间总数</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon student">
                            <el-icon><User /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.totalStudents || 0 }}</div>
                            <div class="stat-label">学生总数</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon occupancy">
                            <el-icon><TrendCharts /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.occupancyRate || 0 }}%</div>
                            <div class="stat-label">入住率</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20" class="stats-row">
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>今日查寝</span>
                                <el-tag type="info">{{ stats.todayAttendance || 0 }}人</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">已查寝</span>
                                <span class="value success">{{ stats.todayAttendance || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">未归</span>
                                <span class="value danger">{{ stats.todayAbsent || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>待处理报修</span>
                                <el-tag type="warning">{{ stats.pendingRepairs || 0 }}</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">待处理</span>
                                <span class="value warning">{{ stats.pendingRepairs || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">总报修</span>
                                <span class="value">{{ stats.totalRepairs || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>待审批请假</span>
                                <el-tag type="warning">{{ stats.pendingLeaves || 0 }}</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">待审批</span>
                                <span class="value warning">{{ stats.pendingLeaves || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">已通过</span>
                                <span class="value success">{{ stats.approvedLeaves || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- 宿管视图 -->
        <div v-else-if="userRole === 'DORM_ADMIN'" class="dashboard-content">
            <el-row :gutter="20" class="stats-row">
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon room">
                            <el-icon><HomeFilled /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.totalRooms || 0 }}</div>
                            <div class="stat-label">房间总数</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon occupancy">
                            <el-icon><UserFilled /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.occupiedRooms || 0 }}</div>
                            <div class="stat-label">已入住房间</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon student">
                            <el-icon><User /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.totalOccupancy || 0 }}</div>
                            <div class="stat-label">入住人数</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon rate">
                            <el-icon><TrendCharts /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.occupancyRate || 0 }}%</div>
                            <div class="stat-label">入住率</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20" class="stats-row">
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>今日查寝</span>
                                <el-tag type="info">{{ stats.todayAttendance || 0 }}人</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">已查寝</span>
                                <span class="value success">{{ stats.todayAttendance || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">未归</span>
                                <span class="value danger">{{ stats.todayAbsent || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>待处理报修</span>
                                <el-tag type="warning">{{ stats.pendingRepairs || 0 }}</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">待处理</span>
                                <span class="value warning">{{ stats.pendingRepairs || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">总报修</span>
                                <span class="value">{{ stats.totalRepairs || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>本楼栋请假</span>
                                <el-tag type="info">{{ stats.buildingLeaves || 0 }}</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">请假人数</span>
                                <span class="value">{{ stats.buildingLeaves || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">本楼栋</span>
                                <span class="value">全部</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- 辅导员视图 -->
        <div v-else-if="userRole === 'COUNSELOR'" class="dashboard-content">
            <el-row :gutter="20" class="stats-row">
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon student">
                            <el-icon><User /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.totalStudents || 0 }}</div>
                            <div class="stat-label">班级人数</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon present">
                            <el-icon><CircleCheckFilled /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.todayPresent || 0 }}</div>
                            <div class="stat-label">今日在校</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon leave">
                            <el-icon><Document /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.todayLeave || 0 }}</div>
                            <div class="stat-label">今日请假</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon absent">
                            <el-icon><CircleCloseFilled /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ stats.todayAbsent || 0 }}</div>
                            <div class="stat-label">今日未归</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20" class="stats-row">
                <el-col :span="12">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>待审批请假</span>
                                <el-tag type="warning">{{ stats.pendingLeaves || 0 }}</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">待审批</span>
                                <span class="value warning">{{ stats.pendingLeaves || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">已通过</span>
                                <span class="value success">{{ stats.approvedLeaves || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="12">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>今日查寝</span>
                                <el-tag type="info">{{ stats.todayAttendance || 0 }}人</el-tag>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">在校</span>
                                <span class="value success">{{ stats.todayPresent || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">未归</span>
                                <span class="value danger">{{ stats.todayAbsent || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- 学生视图 -->
        <div v-else-if="userRole === 'STUDENT'" class="dashboard-content">
            <el-row :gutter="20" class="stats-row">
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>我的住宿</span>
                                <el-icon><HomeFilled /></el-icon>
                            </div>
                        </template>
                        <div v-if="stats.housingInfo" class="housing-info">
                            <div class="info-item">
                                <span class="label">楼栋：</span>
                                <span class="value">{{ stats.housingInfo.buildingName }}</span>
                            </div>
                            <div class="info-item">
                                <span class="label">房间：</span>
                                <span class="value">{{ stats.housingInfo.roomNumber }}</span>
                            </div>
                            <div class="info-item">
                                <span class="label">床位：</span>
                                <span class="value">{{ stats.housingInfo.bedNumber }}</span>
                            </div>
                        </div>
                        <div v-else class="no-data">暂无住宿信息</div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>我的报修</span>
                                <el-icon><Tools /></el-icon>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">待处理</span>
                                <span class="value warning">{{ stats.pendingRepairs || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">处理中</span>
                                <span class="value info">{{ stats.processingRepairs || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>我的请假</span>
                                <el-icon><Document /></el-icon>
                            </div>
                        </template>
                        <div class="detail-stat">
                            <div class="detail-item">
                                <span class="label">待审批</span>
                                <span class="value warning">{{ stats.pendingLeaves || 0 }}</span>
                            </div>
                            <div class="detail-item">
                                <span class="label">已通过</span>
                                <span class="value success">{{ stats.approvedLeaves || 0 }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20" class="stats-row">
                <el-col :span="24">
                    <el-card shadow="hover" class="stat-card">
                        <template #header>
                            <div class="card-header">
                                <span>最近查寝</span>
                                <el-icon><Calendar /></el-icon>
                            </div>
                        </template>
                        <div v-if="stats.lastAttendance" class="attendance-info">
                            <div class="info-item">
                                <span class="label">查寝日期：</span>
                                <span class="value">{{ stats.lastAttendance.date }}</span>
                            </div>
                            <div class="info-item">
                                <span class="label">查寝状态：</span>
                                <el-tag :type="getAttendanceStatusType(stats.lastAttendance.status)">
                                    {{ getAttendanceStatusText(stats.lastAttendance.status) }}
                                </el-tag>
                            </div>
                        </div>
                        <div v-else class="no-data">暂无查寝记录</div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <div v-else class="no-data">
            <el-empty description="未知角色，无法显示数据" />
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, HomeFilled, User, TrendCharts, UserFilled, CircleCheckFilled, Document, CircleCloseFilled, Tools, Calendar } from '@element-plus/icons-vue'
import axios from '@/utils/axios'

const userRole = ref(localStorage.getItem('role') || '')
const stats = ref({})
const loading = ref(false)

const loadStats = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/dashboard/stats')
        stats.value = res.data
    } catch (error) {
        console.error('加载统计数据失败:', error)
        ElMessage.error('加载统计数据失败')
    } finally {
        loading.value = false
    }
}

const getAttendanceStatusType = (status) => {
    const typeMap = {
        'PRESENT': 'success',
        'LATE': 'warning',
        'ABSENT': 'danger',
        'LEAVE': 'info'
    }
    return typeMap[status] || 'info'
}

const getAttendanceStatusText = (status) => {
    const textMap = {
        'PRESENT': '正常',
        'LATE': '晚归',
        'ABSENT': '未归',
        'LEAVE': '请假'
    }
    return textMap[status] || status
}

onMounted(() => {
    loadStats()
})
</script>

<style scoped>
.dashboard {
    padding: 20px;
    min-height: calc(100vh - 84px);
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.dashboard-content {
    max-width: 1400px;
    margin: 0 auto;
}

.stats-row {
    margin-bottom: 20px;
}

.stat-card {
    border-radius: 12px;
    transition: all 0.3s;
    margin-bottom: 20px;
}

.stat-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.stat-card :deep(.el-card__header) {
    background: #f5f7fa;
    border-bottom: none;
    padding: 15px 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
}

.stat-card {
    display: flex;
    align-items: center;
    padding: 20px;
}

.stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 15px;
    font-size: 28px;
    color: white;
}

.stat-icon.building {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.room {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.student {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.occupancy {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-icon.rate {
    background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-icon.present {
    background: linear-gradient(135deg, #30cfd0 0%, #330867 100%);
}

.stat-icon.leave {
    background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
}

.stat-icon.absent {
    background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
}

.stat-info {
    flex: 1;
}

.stat-value {
    font-size: 32px;
    font-weight: bold;
    color: #333;
    margin-bottom: 5px;
}

.stat-label {
    font-size: 14px;
    color: #999;
}

.detail-stat {
    padding: 10px 0;
}

.detail-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid #eee;
}

.detail-item:last-child {
    border-bottom: none;
}

.detail-item .label {
    color: #666;
    font-size: 14px;
}

.detail-item .value {
    font-weight: bold;
    font-size: 18px;
}

.detail-item .value.success {
    color: #67c23a;
}

.detail-item .value.warning {
    color: #e6a23c;
}

.detail-item .value.danger {
    color: #f56c6c;
}

.detail-item .value.info {
    color: #909399;
}

.housing-info,
.attendance-info {
    padding: 10px 0;
}

.info-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    font-size: 16px;
}

.info-item .label {
    color: #666;
    width: 80px;
}

.info-item .value {
    color: #333;
    font-weight: bold;
}

.no-data {
    text-align: center;
    color: #999;
    padding: 30px 0;
    font-size: 14px;
}
</style>
