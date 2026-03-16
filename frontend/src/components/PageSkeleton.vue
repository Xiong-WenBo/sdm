<template>
    <div class="page-skeleton">
        <el-skeleton :rows="rows" animated>
            <template #template>
                <div class="skeleton-header">
                    <el-skeleton-item variant="h1" style="width: 30%; margin-bottom: 20px;" />
                </div>
                <div class="skeleton-content">
                    <el-skeleton-item v-for="i in rows" :key="i" variant="text" style="margin-bottom: 16px;" />
                </div>
            </template>
        </el-skeleton>
    </div>
</template>

<script setup>
defineProps({
    /**
     * 骨架屏行数
     */
    rows: {
        type: Number,
        default: 8
    }
})
</script>

<style scoped>
.page-skeleton {
    padding: 24px;
}

.skeleton-header {
    margin-bottom: 24px;
}

.skeleton-content {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
}
</style>
