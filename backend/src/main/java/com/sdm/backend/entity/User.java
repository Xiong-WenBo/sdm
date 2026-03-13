package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;
    private Integer status;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}