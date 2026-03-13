package com.sdm.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String token;
    private String username;
    private String realName;
    private String role;
}