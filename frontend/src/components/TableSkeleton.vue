<template>
    <div class="table-skeleton">
        <el-skeleton :rows="rows" animated>
            <template #template>
                <el-skeleton-item variant="header" style="height: 40px; margin-bottom: 16px;" />
                <el-skeleton-item v-for="i in rows" :key="i" variant="text" style="margin-bottom: 12px;" />
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
        default: 5
    }
})
</script>

<style scoped>
.table-skeleton {
    padding: 20px;
    background: #fff;
    border-radius: 4px;
}
</style>
