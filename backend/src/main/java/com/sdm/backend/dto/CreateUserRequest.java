package com.sdm.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String realName;
    private String role;
    private Integer status;
    
    // 学生特有字段
    private String studentNumber;
    private String className;
    private String major;
    private Long counselorId;
    private LocalDate enrollmentDate;
}
