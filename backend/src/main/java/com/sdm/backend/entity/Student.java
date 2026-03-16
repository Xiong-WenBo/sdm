package com.sdm.backend.entity;

import com.sdm.backend.annotation.ExcelColumn;
import lombok.Data;
import java.time.LocalDate;

@Data
public class Student {
    private Long id;
    private Long userId;
    private String username;
    @ExcelColumn(name = "姓名")
    private String realName;
    @ExcelColumn(name = "学号")
    private String studentNumber;
    @ExcelColumn(name = "班级")
    private String className;
    @ExcelColumn(name = "专业")
    private String major;
    private Long counselorId;
    @ExcelColumn(name = "辅导员")
    private String counselorName;
    @ExcelColumn(name = "入学日期")
    private LocalDate enrollmentDate;
    @ExcelColumn(name = "住宿状态")
    private String housingStatus;
}
