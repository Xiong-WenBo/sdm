package com.sdm.backend.service;

import com.sdm.backend.entity.Student;
import com.sdm.backend.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserService userService;

    public int createStudent(Student student) {
        return studentMapper.insert(student);
    }

    public Student findByUserId(Long userId) {
        return studentMapper.findByUserId(userId);
    }

    public Student findByStudentNumber(String studentNumber) {
        return studentMapper.findByStudentNumber(studentNumber);
    }

    /**
     * 根据辅导员 ID 查询学生列表
     */
    public List<Student> findByCounselorId(Long counselorId) {
        // 通过 student 表的 counselor_id 字段查询
        return studentMapper.findAll().stream()
            .filter(s -> counselorId.equals(s.getCounselorId()))
            .toList();
    }

    /**
     * 获取学生的用户 ID
     */
    public Long getUserIdByStudentId(Long studentId) {
        Student student = studentMapper.findById(studentId);
        return student != null ? student.getUserId() : null;
    }

    /**
     * 根据用户 ID 获取学生 ID
     */
    public Long getStudentIdByUserId(Long userId) {
        Student student = studentMapper.findByUserId(userId);
        return student != null ? student.getId() : null;
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

    public List<Student> findByPage(int page, int size) {
        int offset = (page - 1) * size;
        return studentMapper.findByPage(offset, size);
    }

    public List<Student> findByPageAndFilters(int page, int size, String className, String major, Long counselorId) {
        int offset = (page - 1) * size;
        return studentMapper.findByPageAndFilters(offset, size, className, major, counselorId);
    }

    public int countAll() {
        return studentMapper.countAll();
    }

    public int countByFilters(String className, String major, Long counselorId) {
        return studentMapper.countByFilters(className, major, counselorId);
    }

    public Student findById(Long id) {
        return studentMapper.findById(id);
    }

    public Student findByStudentNumberExclude(String studentNumber, Long excludeUserId) {
        return studentMapper.findByStudentNumberExclude(studentNumber, excludeUserId);
    }

    /**
     * 创建学生用户（同时创建 user 和 student 记录）
     */
    @org.springframework.transaction.annotation.Transactional
    public int createStudentUser(Student student, String password) {
        // 1. 创建 user 记录
        com.sdm.backend.entity.User user = new com.sdm.backend.entity.User();
        user.setUsername(student.getStudentNumber());
        user.setPassword(password);
        user.setRealName(student.getRealName());
        user.setRole("STUDENT");
        user.setStatus(1);
        userService.insert(user);

        // 2. 设置 user_id 并创建 student 记录
        student.setUserId(user.getId());
        return studentMapper.insert(student);
    }

    /**
     * 更新学生信息（同时更新 student 表和 user 表的 real_name）
     */
    @org.springframework.transaction.annotation.Transactional
    public int updateStudentInfo(Student student) {
        // 1. 更新 user 表的 real_name
        if (student.getRealName() != null) {
            com.sdm.backend.entity.User user = new com.sdm.backend.entity.User();
            user.setId(student.getUserId());
            user.setRealName(student.getRealName());
            userService.update(user);
        }
        
        // 2. 更新 student 表的其他字段
        return studentMapper.update(student);
    }

    /**
     * 根据用户名查找用户
     */
    public com.sdm.backend.entity.User findByUsername(String username) {
        return userService.findByUsername(username);
    }

    /**
     * 根据学生 ID 查询辅导员 ID
     * @param studentId 学生 ID
     * @return 辅导员 ID
     */
    public Long getCounselorIdByStudentId(Long studentId) {
        Student student = studentMapper.findById(studentId);
        return student != null ? student.getCounselorId() : null;
    }

    /**
     * 查询在指定日期有请假申请的学生 ID 列表
     * @param studentIds 学生 ID 列表
     * @param date 日期
     * @return 有请假申请的学生 ID 列表
     */
    public List<Long> findStudentsOnLeave(List<Long> studentIds, LocalDate date) {
        // 查询这些学生在指定日期有请假记录（PENDING 或 APPROVED 状态）
        // 请假时间范围包含指定日期
        java.time.LocalDateTime startOfDay = date.atStartOfDay();
        java.time.LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        return studentMapper.findStudentsOnLeave(studentIds, startOfDay, endOfDay);
    }
}
