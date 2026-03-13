package com.sdm.backend.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Student {
    private Long id;
    private Long userId;
    private String studentNumber;
    private String className;
    private String major;
    private Long counselorId;
    private LocalDate enrollmentDate;
}
