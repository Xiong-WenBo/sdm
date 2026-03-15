package com.sdm.backend.service;

import com.sdm.backend.entity.Message;
import com.sdm.backend.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId);
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
        return messageMapper.insert(message);
    }

    @Transactional
    public int markAsRead(Long id) {
        return messageMapper.markAsRead(id);
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
        message.setStatus("UNREAD");
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
    }
}
