package com.sdm.backend.service;

import com.sdm.backend.entity.User;
import com.sdm.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
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

    public List<User> findMessagingDirectory(Long currentUserId) {
        return userMapper.findAll().stream()
            .filter(user -> user.getStatus() != null && user.getStatus() == 1)
            .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
            .peek(user -> user.setPassword(null))
            .toList();
    }
}
