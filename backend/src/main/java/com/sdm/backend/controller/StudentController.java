package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Student;
import com.sdm.backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Map<String, Object>>> getStudentList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 这里简化处理，返回所有学生
        // TODO: 实现分页和筛选
        List<Student> students = studentService.findAll();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", students);
        result.put("total", students.size());
        result.put("page", page);
        result.put("size", size);
        
        return ResponseEntity.ok(Result.success(result));
    }
}
