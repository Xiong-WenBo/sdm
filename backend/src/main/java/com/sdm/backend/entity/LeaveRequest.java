package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LeaveRequest {
    private Long id;
    private Long studentId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String type;
    private String status;
    private Long approverId;
    private String approveNote;
    private LocalDateTime createdAt;
    
    private String studentNumber;
    private String studentName;
    private String className;
    private String counselorName;
    private String approverName;
}
