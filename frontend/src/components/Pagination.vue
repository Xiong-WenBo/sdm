<template>
    <el-pagination
        :current-page="modelValue"
        :page-size="pageSize"
        :total="total"
        :page-sizes="pageSizes"
        :layout="layout"
        :background="background"
        @update:current-page="(val) => $emit('update:modelValue', val)"
        @update:page-size="(val) => $emit('update:pageSize', val)"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :class="['pagination-container', { 'center': center }]"
    />
</template>

<script setup>
const props = defineProps({
    /**
     * 当前页码
     */
    modelValue: {
        type: Number,
        default: 1
    },
    /**
     * 每页显示条数
     */
    pageSize: {
        type: Number,
        default: 10
    },
    /**
     * 总条数
     */
    total: {
        type: Number,
        default: 0
    },
    /**
     * 每页条数选项
     */
    pageSizes: {
        type: Array,
        default: () => [10, 20, 50, 100]
    },
    /**
     * 布局方式
     */
    layout: {
        type: String,
        default: 'total, sizes, prev, pager, next, jumper'
    },
    /**
     * 是否显示背景色
     */
    background: {
        type: Boolean,
        default: true
    },
    /**
     * 是否居中显示
     */
    center: {
        type: Boolean,
        default: false
    }
})

const emit = defineEmits(['update:modelValue', 'update:pageSize', 'size-change', 'current-change'])

const handleSizeChange = (size) => {
    emit('size-change', size)
}

const handleCurrentChange = (page) => {
    emit('current-change', page)
}
</script>

<style scoped>
.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.pagination-container.center {
    justify-content: center;
}

:deep(.el-pagination) {
    padding: 10px 0;
}

:deep(.el-pagination__total) {
    color: #606266;
    font-size: 14px;
}

:deep(.el-pagination__sizes) {
    margin-right: 10px;
}

:deep(.el-pager) {
    li {
        &.is-active {
            background-color: #409EFF;
            color: #fff;
        }
        
        &:hover {
            background-color: #f5f7fa;
        }
    }
}

:deep(.el-select) {
    .el-input__wrapper {
        border-radius: 4px;
    }
}
</style>
