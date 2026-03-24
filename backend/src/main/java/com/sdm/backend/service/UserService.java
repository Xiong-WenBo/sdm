package com.sdm.backend.service;

import com.sdm.backend.dto.CreateUserRequest;
import com.sdm.backend.entity.Student;
import com.sdm.backend.entity.User;
import com.sdm.backend.mapper.StudentMapper;
import com.sdm.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public User findAnyByUsername(String username) {
        return userMapper.findAnyByUsername(username);
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public List<User> findAll() {
        return userMapper.findAll();
    }

    public List<User> findByPage(int page, int size) {
        int offset = (page - 1) * size;
        return userMapper.findByPage(offset, size);
    }

    public List<User> findByPageAndFilters(int page, int size, String username, String role, Integer status) {
        int offset = (page - 1) * size;
        return userMapper.findByPageAndFilters(offset, size, username, role, status);
    }

    public int countAll() {
        return userMapper.countAll();
    }

    public int countByFilters(String username, String role, Integer status) {
        return userMapper.countByFilters(username, role, status);
    }

    public int insert(User user) {
        user.setPassword(encodePassword(user.getPassword()));
        return userMapper.insert(user);
    }

    @Transactional
    public int createUserWithOptionalStudent(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        insert(user);

        if ("STUDENT".equals(request.getRole())) {
            Student student = new Student();
            student.setUserId(user.getId());
            student.setStudentNumber(request.getStudentNumber());
            student.setClassName(request.getClassName());
            student.setMajor(request.getMajor());
            student.setCounselorId(request.getCounselorId());
            student.setEnrollmentDate(request.getEnrollmentDate());
            studentMapper.insert(student);
        }

        return 1;
    }

    public int update(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(encodePassword(user.getPassword()));
        }
        return userMapper.update(user);
    }

    public int deleteById(Long id) {
        return userMapper.deleteById(id);
    }

    public List<User> findByRole(String role) {
        return userMapper.findByRole(role);
    }
}
