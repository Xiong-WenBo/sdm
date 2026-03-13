package com.sdm.backend.service;

import com.sdm.backend.entity.Student;
import com.sdm.backend.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentMapper studentMapper;

    public int createStudent(Student student) {
        return studentMapper.insert(student);
    }

    public Student findByUserId(Long userId) {
        return studentMapper.findByUserId(userId);
    }

    public Student findByStudentNumber(String studentNumber) {
        return studentMapper.findByStudentNumber(studentNumber);
    }

    public int updateStudent(Student student) {
        return studentMapper.update(student);
    }

    public int deleteByUserId(Long userId) {
        return studentMapper.deleteByUserId(userId);
    }

    public List<Student> findAll() {
        return studentMapper.findAll();
    }
}
