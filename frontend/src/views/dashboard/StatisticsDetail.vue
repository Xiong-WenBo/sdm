<template>
    <div class="statistics-detail">
        <el-page-header @back="$router.back()" title="返回">
            <template #content>
                <span class="page-title">数据统计详情</span>
            </template>
        </el-page-header>

        <div style="margin-top: 20px;">
            <StatisticsCharts :role="userRole" />
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import StatisticsCharts from './StatisticsCharts.vue'

const userRole = ref(localStorage.getItem('role') || 'SUPER_ADMIN')
</script>

<style scoped>
.statistics-detail {
    padding: 20px;
    background: #f5f7fa;
    min-height: calc(100vh - 84px);
}

.page-title {
    font-size: 18px;
    font-weight: bold;
}
</style>
