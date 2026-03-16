package com.sdm.backend.service;

import com.sdm.backend.entity.OperationLog;
import com.sdm.backend.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    public void save(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }

    public Map<String, Object> getList(int page, int size, String username, String module, String operation, String status, String startTime, String endTime) {
        int offset = (page - 1) * size;
        LocalDateTime start = startTime != null && !startTime.isEmpty() ? LocalDateTime.parse(startTime + "T00:00:00") : null;
        LocalDateTime end = endTime != null && !endTime.isEmpty() ? LocalDateTime.parse(endTime + "T23:59:59") : null;

        List<OperationLog> list;
        int total;

        if ((username == null || username.isEmpty()) && (module == null || module.isEmpty()) 
                && (operation == null || operation.isEmpty()) && (status == null || status.isEmpty())
                && start == null && end == null) {
            list = operationLogMapper.findByPage(offset, size);
            total = operationLogMapper.countAll();
        } else {
            list = operationLogMapper.findByPageAndFilters(offset, size, username, module, operation, status, start, end);
            total = operationLogMapper.countByFilters(username, module, operation, status, start, end);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public void delete(Long id) {
        operationLogMapper.deleteById(id);
    }
}
