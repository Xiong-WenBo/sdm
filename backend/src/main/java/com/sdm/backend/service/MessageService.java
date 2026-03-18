package com.sdm.backend.service;

import com.sdm.backend.entity.Message;
import com.sdm.backend.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    public List<Message> findByUserId(Long userId) {
        return messageMapper.findByUserId(userId);
    }

    public List<Message> findByUserIdAndStatus(Long userId, String status) {
        return messageMapper.findByUserIdAndStatus(userId, status);
    }

    public List<Message> findByUserIdAndCategory(Long userId, String category) {
        return messageMapper.findByUserIdAndCategory(userId, category);
    }

    public List<Message> findByUserIdWithFilters(Long userId, String status, String category, String type) {
        return messageMapper.findByUserIdWithFilters(userId, status, category, type);
    }

    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId);
    }

    public int countByCategory(Long userId, String category) {
        return messageMapper.countByCategory(userId, category);
    }

    public Message findById(Long id) {
        return messageMapper.findById(id);
    }

    @Transactional
    public int insert(Message message) {
        if (message.getSendTime() == null) {
            message.setSendTime(LocalDateTime.now());
        }
        if (message.getStatus() == null) {
            message.setStatus("UNREAD");
        }
        if (message.getCategory() == null) {
            message.setCategory("SYSTEM");
        }
        return messageMapper.insert(message);
    }

    @Transactional
    public int batchInsert(List<Message> messages) {
        messages.forEach(message -> {
            if (message.getSendTime() == null) {
                message.setSendTime(LocalDateTime.now());
            }
            if (message.getStatus() == null) {
                message.setStatus("UNREAD");
            }
            if (message.getCategory() == null) {
                message.setCategory("SYSTEM");
            }
        });
        return messageMapper.batchInsert(messages);
    }

    @Transactional
    public int markAsRead(Long id) {
        return messageMapper.markAsRead(id);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return messageMapper.markAllAsRead(userId);
    }

    @Transactional
    public int deleteById(Long id) {
        return messageMapper.deleteById(id);
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return messageMapper.deleteByUserId(userId);
    }

    /**
     * 发送查寝通知
     */
    @Transactional
    public void sendAttendanceNotification(Long userId, String title, String content) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType("ATTENDANCE");
        message.setCategory("SYSTEM");
        message.setStatus("UNREAD");
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    /**
     * 发送业务通知（带关联信息）
     */
    @Transactional
    public void sendBusinessNotification(Long userId, Long senderId, String type, String category, 
                                        String title, String content, String relatedType, Long relatedId) {
        Message message = new Message();
        message.setUserId(userId);
        message.setSenderId(senderId);
        message.setType(type);
        message.setCategory(category);
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedType(relatedType);
        message.setRelatedId(relatedId);
        message.setStatus("UNREAD");
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    /**
     * 使用模板发送消息
     */
    @Transactional
    public void sendWithTemplate(Long userId, String templateCode, Map<String, Object> params) {
        // 实现将在后续添加
    }
}
