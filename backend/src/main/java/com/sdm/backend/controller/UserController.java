package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ResponseEntity<Result<Map<String, Object>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status
    ) {
        List<User> users;
        int total;

        if (username != null || role != null || status != null) {
            users = userService.findByPageAndFilters(page, size, username, role, status);
            total = userService.countByFilters(username, role, status);
        } else {
            users = userService.findByPage(page, size);
            total = userService.countAll();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<User>> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        user.setPassword(null);
        return ResponseEntity.ok(Result.success(user));
    }

    @PostMapping
    public ResponseEntity<Result<Void>> createUser(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "用户名不能为空"));
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "密码不能为空"));
        }
        if (userService.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.ok(Result.error(400, "用户名已存在"));
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "角色不能为空"));
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userService.insert(user);
        return ResponseEntity.ok(Result.success(null, "用户创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Void>> updateUser(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        
        // 如果修改了用户名，检查是否已存在
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            User usernameExists = userService.findByUsername(user.getUsername());
            if (usernameExists != null && !usernameExists.getId().equals(id)) {
                return ResponseEntity.ok(Result.error(400, "用户名已存在"));
            }
        }
        
        user.setId(id);
        userService.update(user);
        return ResponseEntity.ok(Result.success(null, "用户更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        userService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "用户删除成功"));
    }
}
