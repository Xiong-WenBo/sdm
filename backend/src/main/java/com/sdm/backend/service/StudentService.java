package com.sdm.backend.service;

import com.sdm.backend.entity.Student;
import com.sdm.backend.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService {

    @Value("${app.student.initial-password-fallback:123123}")
    private String initialPasswordFallback;

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

    public List<Student> findByCounselorId(Long counselorId) {
        return studentMapper.findByCounselorId(counselorId);
    }

    public List<Student> findUnassignedForDormAssignment() {
        return studentMapper.findUnassignedForDormAssignment();
    }

    public Long getUserIdByStudentId(Long studentId) {
        Student student = studentMapper.findById(studentId);
        return student != null ? student.getUserId() : null;
    }

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

    public List<Student> findStudentsForExport(String role, Long currentUserId) {
        if ("COUNSELOR".equals(role) && currentUserId != null) {
            return studentMapper.findByCounselorId(currentUserId);
        }
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

    @org.springframework.transaction.annotation.Transactional
    public int createStudentUser(Student student, String password) {
        com.sdm.backend.entity.User user = new com.sdm.backend.entity.User();
        user.setUsername(student.getStudentNumber());
        user.setPassword(password);
        user.setRealName(student.getRealName());
        user.setRole("STUDENT");
        user.setStatus(1);
        userService.insert(user);

        student.setUserId(user.getId());
        return studentMapper.insert(student);
    }

    @org.springframework.transaction.annotation.Transactional
    public int updateStudentInfo(Student student) {
        if (student.getRealName() != null) {
            com.sdm.backend.entity.User user = new com.sdm.backend.entity.User();
            user.setId(student.getUserId());
            user.setRealName(student.getRealName());
            userService.update(user);
        }

        return studentMapper.update(student);
    }

    public com.sdm.backend.entity.User findByUsername(String username) {
        return userService.findByUsername(username);
    }

    public String generateInitialPassword(String studentNumber) {
        String normalized = studentNumber == null ? "" : studentNumber.trim();
        if (normalized.isEmpty()) {
            return initialPasswordFallback;
        }
        if (normalized.length() <= 6) {
            return normalized;
        }
        return normalized.substring(normalized.length() - 6);
    }

    public Long getCounselorIdByStudentId(Long studentId) {
        Student student = studentMapper.findById(studentId);
        return student != null ? student.getCounselorId() : null;
    }

    public List<Long> findStudentsOnLeave(List<Long> studentIds, LocalDate date) {
        java.time.LocalDateTime startOfDay = date.atStartOfDay();
        java.time.LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return studentMapper.findStudentsOnLeave(studentIds, startOfDay, endOfDay);
    }
}
