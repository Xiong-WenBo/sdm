package com.sdm.backend.controller;

import com.sdm.backend.dto.LoginRequest;
import com.sdm.backend.dto.LoginResponse;
import com.sdm.backend.dto.Result;
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
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.findByUsername(loginRequest.getUsername());
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户名或密码错误"));
        }
        boolean passwordMatch = userService.checkPassword(loginRequest.getPassword(), user.getPassword());
        if (!passwordMatch) {
            return ResponseEntity.ok(Result.error(401, "用户名或密码错误"));
        }
        String token = jwtUtil.generateToken(user.getUsername());
        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getRealName(), user.getRole());
        return ResponseEntity.ok(Result.success(response, "登录成功"));
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("认证成功，这是测试接口");
    }
}