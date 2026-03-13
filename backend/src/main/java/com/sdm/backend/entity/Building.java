package com.sdm.backend.entity;

import lombok.Data;

@Data
public class Building {
    private Long id;
    private String name;
    private String address;
    private Integer floors;
    private Long adminId;
    private String adminName; // 管理员姓名（关联查询）
    private String description;
}
