package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Repair {
    private Long id;
    private Long studentId;
    private Long roomId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private Long adminId;
    private String handleNote;
    private LocalDateTime handleTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String studentNumber;
    private String studentName;
    private String roomNumber;
    private Long buildingId;
    private String buildingName;
    private String adminName;
}
