package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.LeaveRequest;
import com.sdm.backend.entity.Repair;
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

import java.time.LocalDate;
import java.util.List;
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
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String username) {
            return userService.findByUsername(username);
        }
        return null;
    }

    private ResponseEntity<Result<Map<String, Object>>> unauthorized() {
        return ResponseEntity.ok(Result.error(401, "Unauthorized"));
    }

    private ResponseEntity<Result<Map<String, Object>>> missingBuilding() {
        return ResponseEntity.ok(Result.error(404, "Assigned building not found"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR') or hasRole('STUDENT')")
    public ResponseEntity<Result<Map<String, Object>>> getStats() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return unauthorized();
        }

        Map<String, Object> stats;
        String role = currentUser.getRole();

        if ("SUPER_ADMIN".equals(role)) {
            stats = dashboardService.getSuperAdminStats();
        } else if ("DORM_ADMIN".equals(role)) {
            Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null) {
                return missingBuilding();
            }
            stats = dashboardService.getDormAdminStats(building.getId());
        } else if ("COUNSELOR".equals(role)) {
            stats = dashboardService.getCounselorStats(currentUser.getId());
        } else if ("STUDENT".equals(role)) {
            Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
            stats = dashboardService.getStudentStats(studentId);
        } else {
            return ResponseEntity.ok(Result.error(403, "Unknown role"));
        }

        return ResponseEntity.ok(Result.success(stats));
    }

    @GetMapping("/occupancy-trend")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> getOccupancyTrend() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return unauthorized();
        }

        Long buildingId = null;
        if ("DORM_ADMIN".equals(currentUser.getRole())) {
            Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null) {
                return missingBuilding();
            }
            buildingId = building.getId();
        }

        return ResponseEntity.ok(Result.success(dashboardService.getOccupancyTrend(buildingId)));
    }

    @GetMapping("/leave-type-distribution")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Map<String, Object>>> getLeaveTypeDistribution() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return unauthorized();
        }

        List<LeaveRequest> leaveRequests;
        if ("DORM_ADMIN".equals(currentUser.getRole())) {
            Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null) {
                return missingBuilding();
            }
            leaveRequests = dashboardService.getLeavesByBuildingId(building.getId());
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            leaveRequests = dashboardService.getLeavesByCounselorId(currentUser.getId());
        } else {
            leaveRequests = dashboardService.getAllLeaves();
        }

        return ResponseEntity.ok(Result.success(dashboardService.getLeaveTypeDistribution(leaveRequests)));
    }

    @GetMapping("/attendance-status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Map<String, Object>>> getAttendanceStatus() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return unauthorized();
        }

        List<Attendance> attendances = dashboardService.getAttendancesByDate(LocalDate.now());
        if ("DORM_ADMIN".equals(currentUser.getRole())) {
            Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null) {
                return missingBuilding();
            }
            String buildingName = building.getName();
            attendances = attendances.stream()
                    .filter(attendance -> buildingName != null && buildingName.equals(attendance.getBuildingName()))
                    .toList();
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            List<Long> studentIds = studentService.findByCounselorId(currentUser.getId()).stream()
                    .map(student -> student.getId())
                    .toList();
            attendances = attendances.stream()
                    .filter(attendance -> studentIds.contains(attendance.getStudentId()))
                    .toList();
        }

        return ResponseEntity.ok(Result.success(dashboardService.getAttendanceStatus(attendances)));
    }

    @GetMapping("/repair-status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> getRepairStatus() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return unauthorized();
        }

        List<Repair> repairs;
        if ("DORM_ADMIN".equals(currentUser.getRole())) {
            Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null) {
                return missingBuilding();
            }
            repairs = dashboardService.getRepairsByBuildingId(building.getId());
        } else {
            repairs = dashboardService.getAllRepairs();
        }

        return ResponseEntity.ok(Result.success(dashboardService.getRepairStatus(repairs)));
    }

    @GetMapping("/building-occupancy")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> getBuildingOccupancy() {
        return ResponseEntity.ok(Result.success(dashboardService.getBuildingOccupancy()));
    }
}
