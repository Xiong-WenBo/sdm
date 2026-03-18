package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long userId; // 接收用户 ID
    private String username; // 用户名（关联查询）
    private String realName; // 真实姓名（关联查询）
    private Long senderId; // 发送者 ID
    private String senderName; // 发送者姓名（关联查询）
    private String title; // 消息标题
    private String content; // 消息内容
    private String type; // 消息类型：ATTENDANCE(查寝), REPAIR(报修), LEAVE(请假), SYSTEM(系统)
    private String category; // 消息分类：SYSTEM(系统通知), REPLY(回复), REMINDER(提醒)
    private String relatedType; // 关联业务类型：REPAIR, LEAVE, ATTENDANCE 等
    private Long relatedId; // 关联业务 ID
    private String status; // 状态：UNREAD(未读), READ(已读)
    private LocalDateTime sendTime; // 发送时间
    private LocalDateTime readTime; // 阅读时间
}
