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
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type
    ) {
        User currentUser = getCurrentUser();
        
        List<Message> messages;
        
        // 使用增强的筛选功能
        if (status != null || category != null || type != null) {
            messages = messageService.findByUserIdWithFilters(currentUser.getId(), status, category, type);
        } else if (status != null && !status.isEmpty()) {
            messages = messageService.findByUserIdAndStatus(currentUser.getId(), status);
        } else if (category != null && !category.isEmpty()) {
            messages = messageService.findByUserIdAndCategory(currentUser.getId(), category);
        } else {
            messages = messageService.findByUserId(currentUser.getId());
        }
        
        int total = messages.size();

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

    @GetMapping("/unread/count/by-category")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<Map<String, Object>>> getUnreadCountByCategory() {
        User currentUser = getCurrentUser();
        
        Map<String, Object> result = new HashMap<>();
        result.put("ATTENDANCE", messageService.countByCategory(currentUser.getId(), "ATTENDANCE"));
        result.put("REPAIR", messageService.countByCategory(currentUser.getId(), "REPAIR"));
        result.put("LEAVE", messageService.countByCategory(currentUser.getId(), "LEAVE"));
        result.put("SYSTEM", messageService.countByCategory(currentUser.getId(), "SYSTEM"));
        result.put("REPLY", messageService.countByCategory(currentUser.getId(), "REPLY"));
        result.put("REMINDER", messageService.countByCategory(currentUser.getId(), "REMINDER"));
        
        return ResponseEntity.ok(Result.success(result));
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

    @PutMapping("/read-all")
    @Log(module = "MESSAGE", operation = "UPDATE", description = "一键已读全部")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<Void>> markAllAsRead() {
        User currentUser = getCurrentUser();
        messageService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(Result.success(null, "已标记全部为已读"));
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

    @PostMapping("/send")
    @Log(module = "MESSAGE", operation = "CREATE", description = "发送消息")
    public ResponseEntity<Result<Void>> sendMessage(@RequestBody Map<String, Object> params) {
        try {
            User currentUser = getCurrentUser();
            Long userId = Long.valueOf(params.get("userId").toString());
            String title = (String) params.get("title");
            String content = (String) params.get("content");
            String type = (String) params.get("type");
            String category = (String) params.getOrDefault("category", "SYSTEM");
            String relatedType = (String) params.get("relatedType");
            Long relatedId = relatedType != null ? Long.valueOf(params.get("relatedId").toString()) : null;

            messageService.sendBusinessNotification(
                userId, currentUser.getId(), type, category, title, content, relatedType, relatedId
            );

            return ResponseEntity.ok(Result.success(null, "发送成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "发送失败：" + e.getMessage()));
        }
    }

    /**
     * 发送广播通知（仅超管可用）
     */
    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "MESSAGE", operation = "BROADCAST", description = "发送通知广播")
    public ResponseEntity<Result<Void>> sendBroadcast(@RequestBody Map<String, Object> params) {
        try {
            User currentUser = getCurrentUser();
            String targetRole = (String) params.get("targetRole");
            String title = (String) params.get("title");
            String content = (String) params.get("content");
            String category = (String) params.getOrDefault("category", "SYSTEM");

            List<User> targetUsers;
            if ("ALL".equals(targetRole)) {
                // 发送给所有人
                targetUsers = userService.findAll();
            } else {
                // 发送给特定角色
                targetUsers = userService.findByRole(targetRole);
            }

            // 批量发送
            int sent = 0;
            for (User user : targetUsers) {
                messageService.sendBusinessNotification(
                    user.getId(), 
                    currentUser.getId(), 
                    "SYSTEM", 
                    category, 
                    title, 
                    content, 
                    null, 
                    null
                );
                sent++;
            }

            return ResponseEntity.ok(Result.success(null, "广播发送成功，共发送 " + sent + " 条消息"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "发送失败：" + e.getMessage()));
        }
    }
}
