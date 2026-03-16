<template>
    <div class="statistics-overview">
        <el-row :gutter="20">
            <!-- 入住率趋势图 -->
            <el-col :span="12">
                <el-card shadow="hover">
                    <template #header>
                        <div class="card-header">
                            <span>📊 入住率趋势</span>
                            <el-tag type="success">近 7 天</el-tag>
                        </div>
                    </template>
                    <div ref="occupancyTrendChart" class="chart-container"></div>
                </el-card>
            </el-col>

            <!-- 请假类型分布 -->
            <el-col :span="12">
                <el-card shadow="hover">
                    <template #header>
                        <div class="card-header">
                            <span>📋 请假类型分布</span>
                            <el-tag type="warning">本月</el-tag>
                        </div>
                    </template>
                    <div ref="leaveTypeChart" class="chart-container"></div>
                </el-card>
            </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
            <!-- 查寝情况统计 -->
            <el-col :span="12">
                <el-card shadow="hover">
                    <template #header>
                        <div class="card-header">
                            <span>✅ 查寝情况统计</span>
                            <el-tag type="danger">今日</el-tag>
                        </div>
                    </template>
                    <div ref="attendanceChart" class="chart-container"></div>
                </el-card>
            </el-col>

            <!-- 报修处理统计 -->
            <el-col :span="12">
                <el-card shadow="hover">
                    <template #header>
                        <div class="card-header">
                            <span>🔧 报修处理统计</span>
                            <el-tag type="info">进行中</el-tag>
                        </div>
                    </template>
                    <div ref="repairChart" class="chart-container"></div>
                </el-card>
            </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
            <!-- 各楼栋入住率对比 -->
            <el-col :span="24">
                <el-card shadow="hover">
                    <template #header>
                        <div class="card-header">
                            <span>🏢 各楼栋入住率对比</span>
                        </div>
                    </template>
                    <div ref="buildingOccupancyChart" class="chart-container" style="height: 300px;"></div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import axios from '@/utils/axios'

const props = defineProps({
    role: {
        type: String,
        default: 'SUPER_ADMIN'
    }
})

const occupancyTrendChart = ref(null)
const leaveTypeChart = ref(null)
const attendanceChart = ref(null)
const repairChart = ref(null)
const buildingOccupancyChart = ref(null)

let charts = []

// 初始化图表
const initCharts = async () => {
    // 入住率趋势图
    if (occupancyTrendChart.value) {
        const chart = echarts.init(occupancyTrendChart.value)
        charts.push(chart)
        
        try {
            const res = await axios.get('/api/dashboard/occupancy-trend')
            const option = {
                tooltip: {
                    trigger: 'axis'
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    data: res.data.dates || []
                },
                yAxis: {
                    type: 'value',
                    axisLabel: {
                        formatter: '{value}%'
                    }
                },
                series: [{
                    name: '入住率',
                    type: 'line',
                    data: res.data.rates || [],
                    smooth: true,
                    areaStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
                            { offset: 1, color: 'rgba(64, 158, 255, 0.01)' }
                        ])
                    },
                    itemStyle: {
                        color: '#409EFF'
                    }
                }]
            }
            chart.setOption(option)
        } catch (error) {
            console.error('加载入住率趋势失败:', error)
        }
    }

    // 请假类型分布图
    if (leaveTypeChart.value) {
        const chart = echarts.init(leaveTypeChart.value)
        charts.push(chart)
        
        try {
            const res = await axios.get('/api/dashboard/leave-type-distribution')
            const option = {
                tooltip: {
                    trigger: 'item'
                },
                legend: {
                    orient: 'vertical',
                    left: 'left'
                },
                series: [{
                    name: '请假类型',
                    type: 'pie',
                    radius: '60%',
                    data: res.data.types || [],
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowOffsetX: 0,
                            shadowColor: 'rgba(0, 0, 0, 0.5)'
                        }
                    }
                }]
            }
            chart.setOption(option)
        } catch (error) {
            console.error('加载请假类型分布失败:', error)
        }
    }

    // 查寝情况统计图
    if (attendanceChart.value) {
        const chart = echarts.init(attendanceChart.value)
        charts.push(chart)
        
        try {
            const res = await axios.get('/api/dashboard/attendance-status')
            const option = {
                tooltip: {
                    trigger: 'item'
                },
                series: [{
                    name: '查寝情况',
                    type: 'pie',
                    radius: ['40%', '70%'],
                    avoidLabelOverlap: false,
                    itemStyle: {
                        borderRadius: 10,
                        borderColor: '#fff',
                        borderWidth: 2
                    },
                    label: {
                        show: false,
                        position: 'center'
                    },
                    emphasis: {
                        label: {
                            show: true,
                            fontSize: 20,
                            fontWeight: 'bold'
                        }
                    },
                    labelLine: {
                        show: false
                    },
                    data: res.data.status || []
                }]
            }
            chart.setOption(option)
        } catch (error) {
            console.error('加载查寝情况失败:', error)
        }
    }

    // 报修处理统计图
    if (repairChart.value) {
        const chart = echarts.init(repairChart.value)
        charts.push(chart)
        
        try {
            const res = await axios.get('/api/dashboard/repair-status')
            const option = {
                tooltip: {
                    trigger: 'item'
                },
                series: [{
                    name: '报修状态',
                    type: 'pie',
                    radius: '60%',
                    data: res.data.status || [],
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowOffsetX: 0,
                            shadowColor: 'rgba(0, 0, 0, 0.5)'
                        }
                    }
                }]
            }
            chart.setOption(option)
        } catch (error) {
            console.error('加载报修状态失败:', error)
        }
    }

    // 各楼栋入住率对比图
    if (buildingOccupancyChart.value) {
        const chart = echarts.init(buildingOccupancyChart.value)
        charts.push(chart)
        
        try {
            const res = await axios.get('/api/dashboard/building-occupancy')
            const option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'shadow'
                    }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'value',
                    axisLabel: {
                        formatter: '{value}%'
                    }
                },
                yAxis: {
                    type: 'category',
                    data: res.data.buildings || []
                },
                series: [{
                    name: '入住率',
                    type: 'bar',
                    data: res.data.rates || [],
                    itemStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                            { offset: 0, color: '#83bff6' },
                            { offset: 0.5, color: '#188df0' },
                            { offset: 1, color: '#188df0' }
                        ])
                    },
                    label: {
                        show: true,
                        position: 'right',
                        formatter: '{c}%'
                    }
                }]
            }
            chart.setOption(option)
        } catch (error) {
            console.error('加载楼栋入住率失败:', error)
        }
    }
}

// 监听窗口大小变化
const handleResize = () => {
    charts.forEach(chart => chart.resize())
}

onMounted(() => {
    initCharts()
    window.addEventListener('resize', handleResize)
})

// 清理
const cleanup = () => {
    charts.forEach(chart => chart.dispose())
    charts = []
}

watch(() => props.role, () => {
    cleanup()
    initCharts()
})
</script>

<style scoped>
.statistics-overview {
    padding: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.chart-container {
    height: 250px;
    width: 100%;
}
</style>
