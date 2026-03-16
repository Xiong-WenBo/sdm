package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoginLog {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginTime;
    private String status;
    private String message;
}
