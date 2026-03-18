package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageTemplate {
    private Long id;
    private String templateCode; // 模板编码（如：REPAIR_APPROVED）
    private String templateName; // 模板名称
    private String titleTemplate; // 标题模板（支持占位符 {name}, {date} 等）
    private String contentTemplate; // 内容模板（支持占位符）
    private String type; // 消息类型：ATTENDANCE, REPAIR, LEAVE, SYSTEM
    private String category; // 消息分类：SYSTEM(系统通知), REPLY(回复), REMINDER(提醒)
    private Integer enabled; // 是否启用：0-禁用，1-启用
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
