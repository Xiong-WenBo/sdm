-- ============================================================
-- 宿舍管理系统数据库结构
-- Dormitory Management System Database Schema
-- 
-- 版本：v0.2.0
-- 最后更新：2026-03-15
-- 数据库：MySQL 5.7+
-- 字符集：utf8mb4
-- ============================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户表 (user)
-- ============================================================
-- 存储所有系统用户的基础认证信息
-- 支持四种角色：超级管理员、宿管、辅导员、学生
-- ============================================================
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID，主键',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，用于登录，全局唯一',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  `role` ENUM('SUPER_ADMIN', 'DORM_ADMIN', 'COUNSELOR', 'STUDENT') NOT NULL COMMENT '角色：SUPER_ADMIN(超管), DORM_ADMIN(宿管), COUNSELOR(辅导员), STUDENT(学生)',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 宿舍楼表 (building)
-- ============================================================
-- 存储宿舍楼栋信息，每个楼栋由一名宿管管理
-- ============================================================
CREATE TABLE `building` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '楼栋 ID，主键',
  `name` VARCHAR(50) NOT NULL COMMENT '楼栋名称（如：学宿 1 号楼）',
  `address` VARCHAR(200) DEFAULT NULL COMMENT '地理位置',
  `floors` INT DEFAULT NULL COMMENT '总层数',
  `admin_id` BIGINT DEFAULT NULL COMMENT '宿舍管理员 ID（关联 user 表）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '楼栋描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin` (`admin_id`),
  KEY `idx_admin` (`admin_id`),
  CONSTRAINT `fk_building_admin` FOREIGN KEY (`admin_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍楼表';

-- ============================================================
-- 3. 房间表 (room)
-- ============================================================
-- 存储宿舍房间信息，包含房间容量、入住情况、性别限制等
-- ============================================================
CREATE TABLE `room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '房间 ID，主键',
  `building_id` BIGINT NOT NULL COMMENT '所属楼栋 ID',
  `room_number` VARCHAR(20) NOT NULL COMMENT '房间号（如：101）',
  `floor` INT DEFAULT NULL COMMENT '所在楼层',
  `capacity` INT DEFAULT 4 COMMENT '可住人数（默认 4 人间）',
  `current_occupancy` INT DEFAULT 0 COMMENT '当前已住人数',
  `gender` ENUM('MALE', 'FEMALE', 'UNISEX') DEFAULT 'UNISEX' COMMENT '性别限制：MALE(男), FEMALE(女), UNISEX(不限)',
  `status` ENUM('AVAILABLE', 'FULL', 'MAINTENANCE') DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE(可住), FULL(已满), MAINTENANCE(维修)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_room` (`building_id`, `room_number`),
  KEY `idx_building` (`building_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_room_building` FOREIGN KEY (`building_id`) REFERENCES `building`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

-- ============================================================
-- 4. 学生信息表 (student)
-- ============================================================
-- 学生扩展信息表，关联 user 表，存储学号、班级、专业等
-- ============================================================
CREATE TABLE `student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学生 ID，主键',
  `user_id` BIGINT NOT NULL UNIQUE COMMENT '关联 user 表 ID',
  `student_number` VARCHAR(20) NOT NULL UNIQUE COMMENT '学号，全局唯一',
  `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称（如：计算机 2101 班）',
  `major` VARCHAR(100) DEFAULT NULL COMMENT '专业名称',
  `counselor_id` BIGINT DEFAULT NULL COMMENT '辅导员 ID（关联 user 表）',
  `enrollment_date` DATE DEFAULT NULL COMMENT '入学日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  UNIQUE KEY `uk_student_number` (`student_number`),
  KEY `idx_class` (`class_name`),
  KEY `idx_counselor` (`counselor_id`),
  CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_student_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- ============================================================
-- 5. 住宿分配表 (assignment)
-- ============================================================
-- 核心关联表，记录学生的住宿分配情况
-- 支持入住、退宿、调宿等操作
-- ============================================================
CREATE TABLE `assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分配 ID，主键',
  `student_id` BIGINT NOT NULL COMMENT '学生 ID',
  `room_id` BIGINT NOT NULL COMMENT '房间 ID',
  `bed_number` VARCHAR(10) DEFAULT NULL COMMENT '床位号（如：A1）',
  `check_in_date` DATE NOT NULL COMMENT '入住日期',
  `check_out_date` DATE DEFAULT NULL COMMENT '退宿日期',
  `status` ENUM('ACTIVE', 'INACTIVE', 'PENDING') DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE(在住), INACTIVE(退宿), PENDING(待分配)',
  `created_by` BIGINT DEFAULT NULL COMMENT '操作人 ID（管理员）',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_room` (`room_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `fk_assignment_student` FOREIGN KEY (`student_id`) REFERENCES `student`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assignment_room` FOREIGN KEY (`room_id`) REFERENCES `room`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assignment_creator` FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='住宿分配表';

-- ============================================================
-- 6. 考勤查寝表 (attendance)
-- ============================================================
-- 记录学生每日查寝情况，支持早晚两次查寝
-- 状态包括：正常、晚归、未归、请假
-- ============================================================
CREATE TABLE `attendance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录 ID，主键',
  `student_id` BIGINT NOT NULL COMMENT '学生 ID',
  `check_date` DATE NOT NULL COMMENT '查寝日期',
  `check_time` ENUM('MORNING', 'EVENING') NOT NULL COMMENT '查寝时段：MORNING(早上), EVENING(晚上)',
  `status` ENUM('NORMAL', 'LATE', 'ABSENT', 'LEAVE') DEFAULT 'NORMAL' COMMENT '状态：NORMAL(正常), LATE(晚归), ABSENT(未归), LEAVE(请假)',
  `remarks` VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
  `checker_id` BIGINT DEFAULT NULL COMMENT '检查人 ID（管理员/辅导员）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_date` (`student_id`, `check_date`, `check_time`),
  KEY `idx_date` (`check_date`),
  KEY `idx_status` (`status`),
  KEY `idx_checker` (`checker_id`),
  CONSTRAINT `fk_attendance_student` FOREIGN KEY (`student_id`) REFERENCES `student`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attendance_checker` FOREIGN KEY (`checker_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤查寝表';

-- ============================================================
-- 7. 报修表 (repair)
-- ============================================================
-- 学生提交宿舍报修申请，宿管处理报修
-- 支持优先级分类和处理进度跟踪
-- ============================================================
CREATE TABLE `repair` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报修 ID，主键',
  `student_id` BIGINT NOT NULL COMMENT '报修学生 ID',
  `room_id` BIGINT NOT NULL COMMENT '房间 ID',
  `title` VARCHAR(100) NOT NULL COMMENT '报修标题',
  `description` TEXT DEFAULT NULL COMMENT '问题详细描述',
  `category` ENUM('ELECTRIC', 'PLUMBING', 'FURNITURE', 'OTHER') DEFAULT 'OTHER' COMMENT '类别：ELECTRIC(电器), PLUMBING(水电), FURNITURE(家具), OTHER(其他)',
  `priority` ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM' COMMENT '优先级：LOW(低), MEDIUM(中), HIGH(高), URGENT(紧急)',
  `status` ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'REJECTED') DEFAULT 'PENDING' COMMENT '状态：PENDING(待处理), PROCESSING(处理中), COMPLETED(已完成), REJECTED(已拒绝)',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理人 ID（宿管员）',
  `handle_note` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报修时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_room` (`room_id`),
  KEY `idx_status` (`status`),
  KEY `idx_admin` (`admin_id`),
  KEY `idx_created` (`created_at`),
  CONSTRAINT `fk_repair_student` FOREIGN KEY (`student_id`) REFERENCES `student`(`id`),
  CONSTRAINT `fk_repair_room` FOREIGN KEY (`room_id`) REFERENCES `room`(`id`),
  CONSTRAINT `fk_repair_admin` FOREIGN KEY (`admin_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修表';

-- ============================================================
-- 8. 请假申请表 (leave_request)
-- ============================================================
-- 学生提交请假申请，辅导员审批
-- 支持病假、事假等类型
-- ============================================================
CREATE TABLE `leave_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '请假 ID，主键',
  `student_id` BIGINT NOT NULL COMMENT '学生 ID',
  `start_time` DATETIME NOT NULL COMMENT '请假开始时间',
  `end_time` DATETIME NOT NULL COMMENT '请假结束时间',
  `reason` VARCHAR(500) NOT NULL COMMENT '请假理由',
  `type` ENUM('SICK', 'PERSONAL', 'OTHER') DEFAULT 'PERSONAL' COMMENT '类型：SICK(病假), PERSONAL(事假), OTHER(其他)',
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELED') DEFAULT 'PENDING' COMMENT '状态：PENDING(待审批), APPROVED(已通过), REJECTED(已拒绝), CANCELED(已取消)',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人 ID（辅导员）',
  `approve_note` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_status` (`status`),
  KEY `idx_approver` (`approver_id`),
  KEY `idx_time_range` (`start_time`, `end_time`),
  CONSTRAINT `fk_leave_student` FOREIGN KEY (`student_id`) REFERENCES `student`(`id`),
  CONSTRAINT `fk_leave_approver` FOREIGN KEY (`approver_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

-- ============================================================
-- 9. 消息通知表 (message)
-- ============================================================
-- 系统消息通知表，支持查寝、报修、请假等通知
-- 支持站内信功能，记录已读/未读状态
-- ============================================================
CREATE TABLE `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '接收用户 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `type` VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：ATTENDANCE(查寝), REPAIR(报修), LEAVE(请假), SYSTEM(系统)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'UNREAD' COMMENT '状态：UNREAD(未读), READ(已读)',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`),
  KEY `idx_send_time` (`send_time`),
  CONSTRAINT `fk_message_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- ============================================================
-- 数据表关系说明
-- ============================================================
-- 1. user 表是核心认证表，所有角色都在此表有记录
-- 2. student 表是 user 表的扩展，存储学生特有信息
-- 3. building 表通过 admin_id 关联 user 表（宿管）
-- 4. room 表通过 building_id 关联 building 表
-- 5. assignment 表关联 student 和 room，记录住宿关系
-- 6. attendance 表关联 student 和 checker（检查人）
-- 7. repair 表关联 student、room 和 admin（处理人）
-- 8. leave_request 表关联 student 和 approver（审批人）
-- 9. message 表关联 user（接收者）
-- ============================================================

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;
