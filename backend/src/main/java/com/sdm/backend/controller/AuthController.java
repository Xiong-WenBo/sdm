package com.sdm.backend.controller;

import com.sdm.backend.dto.LoginRequest;
import com.sdm.backend.dto.LoginResponse;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.UserService;
import com.sdm.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.findByUsername(loginRequest.getUsername());
        if (user == null) {
            return ResponseEntity.status(401).body("用户名不存在");
        }
        boolean passwordMatch = userService.checkPassword(loginRequest.getPassword(), user.getPassword());
        if (!passwordMatch) {
            return ResponseEntity.status(401).body("密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getRealName(), user.getRole()));
    }

    @GetMapping("/test")
    public String test() {
        return "认证成功，这是测试接口";
    }
}