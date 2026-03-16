package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.DashboardService;
import com.sdm.backend.service.StudentService;
import com.sdm.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private BuildingService buildingService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) principal;
            return userService.findByUsername(username);
        }
        return null;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<Map<String, Object>>> getStats() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(Result.error(401, "未登录"));
        }

        Map<String, Object> stats;
        String role = currentUser.getRole();

        if ("SUPER_ADMIN".equals(role)) {
            stats = dashboardService.getSuperAdminStats();
        } else if ("DORM_ADMIN".equals(role)) {
            Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null) {
                return ResponseEntity.ok(Result.error(404, "未找到您管理的楼栋"));
            }
            stats = dashboardService.getDormAdminStats(building.getId());
        } else if ("COUNSELOR".equals(role)) {
            stats = dashboardService.getCounselorStats(currentUser.getId());
        } else if ("STUDENT".equals(role)) {
            Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
            stats = dashboardService.getStudentStats(studentId);
        } else {
            return ResponseEntity.ok(Result.error(403, "未知角色"));
        }

        return ResponseEntity.ok(Result.success(stats));
    }
}
