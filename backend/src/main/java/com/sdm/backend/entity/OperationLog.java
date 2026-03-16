package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String operation;
    private String module;
    private String description;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime operationTime;
    private Long executionTime;
    private String status;
    private String errorMessage;
}
