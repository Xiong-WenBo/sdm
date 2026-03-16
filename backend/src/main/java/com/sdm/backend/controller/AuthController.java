package com.sdm.backend.controller;

import com.sdm.backend.dto.LoginRequest;
import com.sdm.backend.dto.LoginResponse;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.LoginLog;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.LoginLogService;
import com.sdm.backend.service.UserService;
import com.sdm.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginLogService loginLogService;

    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(loginRequest.getUsername());
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setIpAddress(getIpAddress(request));
        loginLog.setUserAgent(request.getHeader("User-Agent"));

        User user = userService.findByUsername(loginRequest.getUsername());
        if (user == null) {
            loginLog.setStatus("FAILED");
            loginLog.setMessage("用户名不存在");
            loginLogService.save(loginLog);
            return ResponseEntity.ok(Result.error(401, "用户名或密码错误"));
        }
        
        loginLog.setUserId(user.getId());
        loginLog.setRealName(user.getRealName());
        loginLog.setRole(user.getRole());
        
        boolean passwordMatch = userService.checkPassword(loginRequest.getPassword(), user.getPassword());
        if (!passwordMatch) {
            loginLog.setStatus("FAILED");
            loginLog.setMessage("密码错误");
            loginLogService.save(loginLog);
            return ResponseEntity.ok(Result.error(401, "用户名或密码错误"));
        }
        
        String token = jwtUtil.generateToken(user.getUsername());
        LoginResponse response = new LoginResponse(user.getId(), token, user.getUsername(), user.getRealName(), user.getRole());
        
        loginLog.setStatus("SUCCESS");
        loginLog.setMessage("登录成功");
        loginLogService.save(loginLog);
        
        return ResponseEntity.ok(Result.success(response, "登录成功"));
    }
    
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("认证成功，这是测试接口");
    }
}