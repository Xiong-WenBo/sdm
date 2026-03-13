package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Assignment {
    private Long id;
    private Long studentId;
    private String studentNumber; // 学号（关联查询）
    private String studentName; // 学生姓名（关联查询）
    private Long roomId;
    private String roomNumber; // 房间号（关联查询）
    private String buildingName; // 楼栋名称（关联查询）
    private String bedNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // ACTIVE, INACTIVE, PENDING
    private Long createdBy;
    private String creatorName; // 操作人姓名（关联查询）
}
