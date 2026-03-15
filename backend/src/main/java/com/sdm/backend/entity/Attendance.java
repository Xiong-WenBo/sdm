package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Attendance {
    private Long id;
    private Long studentId;
    private String studentNumber; // 学号（关联查询）
    private String studentName; // 学生姓名（关联查询）
    private String className; // 班级（关联查询）
    private String roomNumber; // 房间号（关联查询）
    private String buildingName; // 楼栋名称（关联查询）
    private LocalDate checkDate;
    private String checkTime; // MORNING, EVENING
    private String status; // NORMAL, LATE, ABSENT, LEAVE
    private String remarks;
    private Long checkerId;
    private String checkerName; // 检查人姓名（关联查询）
}
