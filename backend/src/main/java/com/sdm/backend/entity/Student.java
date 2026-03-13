package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Student {
    private Long id;
    private Long userId;
    private String username; // 用户名（关联查询）
    private String realName; // 真实姓名（关联查询）
    private String studentNumber;
    private String className;
    private String major;
    private Long counselorId;
    private String counselorName; // 辅导员姓名（关联查询）
    private LocalDate enrollmentDate;
    private String housingStatus; // 住宿状态（ACTIVE/INACTIVE/NONE）
}
