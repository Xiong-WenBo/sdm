package com.sdm.backend.service;

import com.sdm.backend.entity.LoginLog;
import com.sdm.backend.mapper.LoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginLogService {

    @Autowired
    private LoginLogMapper loginLogMapper;

    public void save(LoginLog loginLog) {
        loginLogMapper.insert(loginLog);
    }

    public Map<String, Object> getList(int page, int size, String username, String status, String startTime, String endTime) {
        int offset = (page - 1) * size;
        LocalDateTime start = startTime != null && !startTime.isEmpty() ? LocalDateTime.parse(startTime + "T00:00:00") : null;
        LocalDateTime end = endTime != null && !endTime.isEmpty() ? LocalDateTime.parse(endTime + "T23:59:59") : null;

        List<LoginLog> list;
        int total;

        if ((username == null || username.isEmpty()) && (status == null || status.isEmpty()) && start == null && end == null) {
            list = loginLogMapper.findByPage(offset, size);
            total = loginLogMapper.countAll();
        } else {
            list = loginLogMapper.findByPageAndFilters(offset, size, username, status, start, end);
            total = loginLogMapper.countByFilters(username, status, start, end);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public void delete(Long id) {
        loginLogMapper.deleteById(id);
    }
}
