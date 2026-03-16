package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.service.LoginLogService;
import com.sdm.backend.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/log")
public class LogController {

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/login/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Map<String, Object>> getLoginLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Map<String, Object> data = loginLogService.getList(page, size, username, status, startTime, endTime);
        return Result.success(data);
    }

    @DeleteMapping("/login/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> deleteLoginLog(@PathVariable Long id) {
        loginLogService.delete(id);
        return Result.success();
    }

    @GetMapping("/operation/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Map<String, Object>> getOperationLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Map<String, Object> data = operationLogService.getList(page, size, username, module, operation, status, startTime, endTime);
        return Result.success(data);
    }

    @DeleteMapping("/operation/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> deleteOperationLog(@PathVariable Long id) {
        operationLogService.delete(id);
        return Result.success();
    }
}
