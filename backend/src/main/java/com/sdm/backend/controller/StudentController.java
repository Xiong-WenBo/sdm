package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Student;
import com.sdm.backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/{userId}")
    public ResponseEntity<Result<Student>> getStudentByUserId(@PathVariable Long userId) {
        Student student = studentService.findByUserId(userId);
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "学生信息不存在"));
        }
        return ResponseEntity.ok(Result.success(student));
    }
}
