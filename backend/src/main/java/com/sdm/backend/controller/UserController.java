package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.ChangePasswordRequest;
import com.sdm.backend.dto.CreateUserRequest;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Student;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.StudentService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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

    @GetMapping("/directory")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<List<User>>> getUserDirectory() {
        User currentUser = getAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.ok(Result.error(401, "Unauthorized"));
        }

        List<User> directory = userService.findAll().stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .peek(user -> user.setPassword(null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Result.success(directory));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<User>> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "User not found"));
        }
        user.setPassword(null);
        return ResponseEntity.ok(Result.success(user));
    }

    @GetMapping("/current")
    public ResponseEntity<Result<User>> getCurrentUser() {
        User user = getAuthenticatedUser();
        if (user != null) {
            user.setPassword(null);
            return ResponseEntity.ok(Result.success(user));
        }

        return ResponseEntity.ok(Result.error(401, "Unauthorized"));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "USER", operation = "CREATE", description = "Create user")
    public ResponseEntity<Result<Void>> createUser(@RequestBody CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "Username is required"));
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "Password is required"));
        }
        if (userService.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.ok(Result.error(400, "Username already exists"));
        }
        if (request.getRole() == null || request.getRole().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "Role is required"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        userService.insert(user);

        if ("STUDENT".equals(request.getRole())) {
            if (request.getStudentNumber() == null || request.getStudentNumber().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "Student number is required"));
            }
            if (request.getClassName() == null || request.getClassName().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "Class name is required"));
            }

            Student existingStudent = studentService.findByStudentNumber(request.getStudentNumber());
            if (existingStudent != null) {
                return ResponseEntity.ok(Result.error(400, "Student number already exists"));
            }

            Student student = new Student();
            student.setUserId(user.getId());
            student.setStudentNumber(request.getStudentNumber());
            student.setClassName(request.getClassName());
            student.setMajor(request.getMajor());
            student.setCounselorId(request.getCounselorId());
            student.setEnrollmentDate(request.getEnrollmentDate());
            studentService.createStudent(student);
        }

        return ResponseEntity.ok(Result.success(null, "User created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "USER", operation = "UPDATE", description = "Update user")
    public ResponseEntity<Result<Void>> updateUser(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return ResponseEntity.ok(Result.error(404, "User not found"));
        }

        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            User usernameExists = userService.findByUsername(user.getUsername());
            if (usernameExists != null && !usernameExists.getId().equals(id)) {
                return ResponseEntity.ok(Result.error(400, "Username already exists"));
            }
        }

        user.setId(id);
        userService.update(user);
        return ResponseEntity.ok(Result.success(null, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "USER", operation = "DELETE", description = "Delete user")
    public ResponseEntity<Result<Void>> deleteUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "User not found"));
        }
        userService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "User deleted successfully"));
    }

    @PutMapping("/password/{id}")
    public ResponseEntity<Result<Void>> changePassword(@PathVariable Long id, @RequestBody ChangePasswordRequest request) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "User not found"));
        }

        if (!userService.checkPassword(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.ok(Result.error(400, "Old password is incorrect"));
        }

        user.setPassword(request.getNewPassword());
        userService.update(user);

        return ResponseEntity.ok(Result.success(null, "Password updated successfully"));
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<Result<Void>> updateProfile(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "User not found"));
        }

        if (updates.containsKey("phone")) {
            user.setPhone((String) updates.get("phone"));
        }
        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }

        userService.update(user);
        return ResponseEntity.ok(Result.success(null, "Profile updated successfully"));
    }

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String username) {
            return userService.findByUsername(username);
        }
        return null;
    }
}
