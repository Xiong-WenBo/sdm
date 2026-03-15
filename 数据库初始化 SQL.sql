-- 消息通知表
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
  `user_id` BIGINT NOT NULL COMMENT '接收用户 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `type` VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：ATTENDANCE(查寝), REPAIR(报修), LEAVE(请假), SYSTEM(系统)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'UNREAD' COMMENT '状态：UNREAD(未读), READ(已读)',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_type` (`type`),
  INDEX `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';
