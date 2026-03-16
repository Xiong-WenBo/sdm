package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Message;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.MessageService;
import com.sdm.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) principal;
            return userService.findByUsername(username);
        }
        return null;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<Map<String, Object>>> getMessageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        
        List<Message> messages;
        int total;

        if (status != null && !status.isEmpty()) {
            messages = messageService.findByUserIdAndStatus(currentUser.getId(), status);
            total = messages.size();
        } else {
            messages = messageService.findByUserId(currentUser.getId());
            total = messages.size();
        }

        // 手动分页
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, messages.size());
        List<Message> pagedMessages = messages.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pagedMessages);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/unread/count")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<Integer>> getUnreadCount() {
        User currentUser = getCurrentUser();
        int count = messageService.countUnread(currentUser.getId());
        return ResponseEntity.ok(Result.success(count));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Message>> getMessageById(@PathVariable Long id) {
        Message message = messageService.findById(id);
        if (message == null) {
            return ResponseEntity.ok(Result.error(404, "消息不存在"));
        }
        
        User currentUser = getCurrentUser();
        if (!message.getUserId().equals(currentUser.getId()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.error(403, "无权查看此消息"));
        }
        
        return ResponseEntity.ok(Result.success(message));
    }

    @PutMapping("/{id}/read")
    @Log(module = "MESSAGE", operation = "UPDATE", description = "标记消息为已读")
    public ResponseEntity<Result<Void>> markAsRead(@PathVariable Long id) {
        Message message = messageService.findById(id);
        if (message == null) {
            return ResponseEntity.ok(Result.error(404, "消息不存在"));
        }
        
        User currentUser = getCurrentUser();
        if (!message.getUserId().equals(currentUser.getId()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.error(403, "无权操作此消息"));
        }
        
        messageService.markAsRead(id);
        return ResponseEntity.ok(Result.success(null, "已标记为已读"));
    }

    @DeleteMapping("/{id}")
    @Log(module = "MESSAGE", operation = "DELETE", description = "删除消息")
    public ResponseEntity<Result<Void>> deleteMessage(@PathVariable Long id) {
        Message message = messageService.findById(id);
        if (message == null) {
            return ResponseEntity.ok(Result.error(404, "消息不存在"));
        }
        
        User currentUser = getCurrentUser();
        if (!message.getUserId().equals(currentUser.getId()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.error(403, "无权删除此消息"));
        }
        
        messageService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "删除成功"));
    }
}
