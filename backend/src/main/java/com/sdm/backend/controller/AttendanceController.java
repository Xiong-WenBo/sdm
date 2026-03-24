package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AttendanceService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String username) {
            return userService.findByUsername(username);
        }
        return null;
    }

    private Long getDormAdminBuildingId(User user) {
        if (user == null || !"DORM_ADMIN".equals(user.getRole())) {
            return null;
        }
        Building building = buildingService.findByAdminUserId(user.getId());
        return building != null ? building.getId() : null;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Map<String, Object>>> getAttendanceList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkDate,
            @RequestParam(required = false) String checkTime,
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
        Long counselorId = "COUNSELOR".equals(currentUser.getRole()) ? currentUser.getId() : null;

        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

        List<Attendance> attendances;
        int total;
        if (studentId != null || buildingId != null || checkDate != null || checkTime != null || status != null || counselorId != null) {
            attendances = attendanceService.findByPageAndFilters(page, size, studentId, buildingId, checkDate, checkTime, status, counselorId);
            total = attendanceService.countByFilters(studentId, buildingId, checkDate, checkTime, status, counselorId);
        } else {
            attendances = attendanceService.findByPage(page, size);
            total = attendanceService.countAll();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", attendances);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Attendance>> getAttendanceById(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id);
        if (attendance == null) {
            return ResponseEntity.ok(Result.error(404, "Attendance record not found"));
        }
        return ResponseEntity.ok(Result.success(attendance));
    }

    @GetMapping("/buildings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<List<Building>>> getAccessibleBuildings() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(Result.success(
                attendanceService.findAccessibleBuildings(currentUser.getRole(), currentUser.getId())
        ));
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<List<Attendance>>> getStudentsForCheckIn(
            @RequestParam(required = false) Long buildingId
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

        if (!"COUNSELOR".equals(currentUser.getRole()) && buildingId == null) {
            return ResponseEntity.ok(Result.error(400, "Building is required"));
        }

        List<Attendance> students = attendanceService.findStudentsForCheckIn(currentUser.getRole(), currentUser.getId(), buildingId);
        return ResponseEntity.ok(Result.success(students));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ATTENDANCE", operation = "CREATE", description = "Create attendance record")
    public ResponseEntity<Result<Void>> createAttendance(@RequestBody Attendance attendance) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        if (dormAdminBuildingId != null) {
            Attendance studentInfo = attendanceService.findStudentsInBuilding(dormAdminBuildingId)
                    .stream()
                    .filter(s -> s.getStudentId().equals(attendance.getStudentId()))
                    .findFirst()
                    .orElse(null);
            if (studentInfo == null) {
                return ResponseEntity.ok(Result.error(403, "Access denied"));
            }
        }

        Attendance existing = attendanceService.findByStudentAndDate(
                attendance.getStudentId(),
                attendance.getCheckDate(),
                attendance.getCheckTime()
        );
        if (existing != null) {
            return ResponseEntity.ok(Result.error(400, "Attendance record already exists"));
        }

        attendance.setCheckerId(currentUser.getId());
        attendanceService.insert(attendance);
        return ResponseEntity.ok(Result.success(null, "Attendance created successfully"));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ATTENDANCE", operation = "CREATE", description = "Batch create attendance records")
    public ResponseEntity<Result<Map<String, Object>>> batchCreateAttendance(@RequestBody List<Attendance> attendances) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Attendance attendance : attendances) {
            try {
                if (dormAdminBuildingId != null) {
                    Attendance studentInfo = attendanceService.findStudentsInBuilding(dormAdminBuildingId)
                            .stream()
                            .filter(s -> s.getStudentId().equals(attendance.getStudentId()))
                            .findFirst()
                            .orElse(null);
                    if (studentInfo == null) {
                        errors.add("Student " + attendance.getStudentId() + " is out of scope");
                        errorCount++;
                        continue;
                    }
                }

                attendance.setCheckerId(currentUser.getId());
                Attendance existing = attendanceService.findByStudentAndDate(
                        attendance.getStudentId(),
                        attendance.getCheckDate(),
                        attendance.getCheckTime()
                );

                if (existing != null) {
                    attendance.setId(existing.getId());
                    attendanceService.update(attendance);
                } else {
                    attendanceService.insert(attendance);
                }
                successCount++;
            } catch (Exception e) {
                errors.add("Student " + attendance.getStudentId() + ": " + e.getMessage());
                errorCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        String message = errorCount == 0
                ? "Batch attendance saved successfully"
                : "Batch attendance partially completed";
        return ResponseEntity.ok(Result.success(result, message));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ATTENDANCE", operation = "UPDATE", description = "Update attendance record")
    public ResponseEntity<Result<Void>> updateAttendance(@PathVariable Long id, @RequestBody Attendance attendance) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        Attendance existing = attendanceService.findById(id);
        if (existing == null) {
            return ResponseEntity.ok(Result.error(404, "Attendance record not found"));
        }

        if (dormAdminBuildingId != null) {
            Attendance studentInfo = attendanceService.findStudentsInBuilding(dormAdminBuildingId)
                    .stream()
                    .filter(s -> s.getStudentId().equals(existing.getStudentId()))
                    .findFirst()
                    .orElse(null);
            if (studentInfo == null) {
                return ResponseEntity.ok(Result.error(403, "Access denied"));
            }
        }

        attendance.setId(id);
        attendanceService.update(attendance);
        return ResponseEntity.ok(Result.success(null, "Attendance updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "ATTENDANCE", operation = "DELETE", description = "Delete attendance record")
    public ResponseEntity<Result<Void>> deleteAttendance(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id);
        if (attendance == null) {
            return ResponseEntity.ok(Result.error(404, "Attendance record not found"));
        }
        attendanceService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "Attendance deleted successfully"));
    }

    @GetMapping("/absent/today")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<List<Attendance>>> getAbsentStudentsToday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkDate
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
        Long counselorId = "COUNSELOR".equals(currentUser.getRole()) ? currentUser.getId() : null;

        if (checkDate == null) {
            checkDate = LocalDate.now();
        }

        List<Attendance> absentStudents = attendanceService.findByPageAndFilters(
                1,
                1000,
                null,
                dormAdminBuildingId,
                checkDate,
                "EVENING",
                "ABSENT",
                counselorId
        );

        return ResponseEntity.ok(Result.success(absentStudents));
    }
}
