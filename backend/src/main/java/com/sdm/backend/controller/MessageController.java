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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
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

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String username) {
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
        List<Message> messages = hasAnyFilter(status, category, type)
                ? messageService.findByUserIdWithFilters(currentUser.getId(), status, category, type)
                : messageService.findByUserId(currentUser.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("list", paginate(messages, page, size));
        result.put("total", messages.size());
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
            return ResponseEntity.ok(Result.error(404, "Message not found"));
        }

        User currentUser = getCurrentUser();
        if (!message.getUserId().equals(currentUser.getId()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.error(403, "Access denied"));
        }

        return ResponseEntity.ok(Result.success(message));
    }

    @PutMapping("/{id}/read")
    @Log(module = "MESSAGE", operation = "UPDATE", description = "Mark message as read")
    public ResponseEntity<Result<Void>> markAsRead(@PathVariable Long id) {
        Message message = messageService.findById(id);
        if (message == null) {
            return ResponseEntity.ok(Result.error(404, "Message not found"));
        }

        User currentUser = getCurrentUser();
        if (!message.getUserId().equals(currentUser.getId()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.error(403, "Access denied"));
        }

        messageService.markAsRead(id);
        return ResponseEntity.ok(Result.success(null, "Marked as read"));
    }

    @PutMapping("/read-all")
    @Log(module = "MESSAGE", operation = "UPDATE", description = "Mark all messages as read")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<Void>> markAllAsRead() {
        User currentUser = getCurrentUser();
        messageService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(Result.success(null, "All messages marked as read"));
    }

    @DeleteMapping("/{id}")
    @Log(module = "MESSAGE", operation = "DELETE", description = "Delete message")
    public ResponseEntity<Result<Void>> deleteMessage(@PathVariable Long id) {
        Message message = messageService.findById(id);
        if (message == null) {
            return ResponseEntity.ok(Result.error(404, "Message not found"));
        }

        User currentUser = getCurrentUser();
        if (!message.getUserId().equals(currentUser.getId()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.error(403, "Access denied"));
        }

        messageService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "Deleted successfully"));
    }

    @PostMapping("/send")
    @Log(module = "MESSAGE", operation = "CREATE", description = "Send message")
    public ResponseEntity<Result<Void>> sendMessage(@RequestBody Map<String, Object> params) {
        try {
            User currentUser = getCurrentUser();
            Long userId = Long.valueOf(params.get("userId").toString());
            String title = (String) params.get("title");
            String content = (String) params.get("content");
            String type = (String) params.get("type");
            String category = (String) params.getOrDefault("category", "SYSTEM");
            String relatedType = (String) params.get("relatedType");
            Long relatedId = parseOptionalLong(params.get("relatedId"));

            messageService.sendBusinessNotification(
                    userId, currentUser.getId(), type, category, title, content, relatedType, relatedId
            );

            return ResponseEntity.ok(Result.success(null, "Sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "Send failed: " + e.getMessage()));
        }
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "MESSAGE", operation = "BROADCAST", description = "Send broadcast")
    public ResponseEntity<Result<Void>> sendBroadcast(@RequestBody Map<String, Object> params) {
        try {
            User currentUser = getCurrentUser();
            String targetRole = (String) params.get("targetRole");
            String title = (String) params.get("title");
            String content = (String) params.get("content");
            String category = (String) params.getOrDefault("category", "SYSTEM");

            List<User> targetUsers = "ALL".equals(targetRole)
                    ? userService.findAll()
                    : userService.findByRole(targetRole);

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

            return ResponseEntity.ok(Result.success(null, "Broadcast sent to " + sent + " users"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "Send failed: " + e.getMessage()));
        }
    }

    private boolean hasAnyFilter(String status, String category, String type) {
        return hasText(status) || hasText(category) || hasText(type);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<Message> paginate(List<Message> messages, int page, int size) {
        if (messages == null || messages.isEmpty() || page < 1 || size < 1) {
            return Collections.emptyList();
        }

        int fromIndex = (page - 1) * size;
        if (fromIndex >= messages.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + size, messages.size());
        return messages.subList(fromIndex, toIndex);
    }

    private Long parseOptionalLong(Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();
        if (stringValue.isBlank()) {
            return null;
        }

        return Long.valueOf(stringValue);
    }
}
