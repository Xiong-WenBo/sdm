-- ============================================
-- 宿舍管理系统 - 操作日志和登录日志表
-- ============================================

-- 1. 登录日志表
CREATE TABLE IF NOT EXISTS login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role VARCHAR(20) COMMENT '用户角色',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(255) COMMENT '用户代理',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '登录状态：SUCCESS/FAILED',
    message VARCHAR(255) COMMENT '备注信息',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_login_time (login_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 2. 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role VARCHAR(20) COMMENT '用户角色',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT',
    module VARCHAR(50) NOT NULL COMMENT '操作模块：USER/BUILDING/ROOM/STUDENT/ASSIGNMENT/ATTENDANCE/REPAIR/LEAVE/MESSAGE',
    description VARCHAR(500) COMMENT '操作描述',
    request_method VARCHAR(10) COMMENT '请求方法：GET/POST/PUT/DELETE',
    request_url VARCHAR(255) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(255) COMMENT '用户代理',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    execution_time BIGINT COMMENT '执行耗时（毫秒）',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作状态：SUCCESS/FAILED',
    error_message TEXT COMMENT '错误信息',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_operation_time (operation_time),
    INDEX idx_module (module),
    INDEX idx_operation (operation),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
