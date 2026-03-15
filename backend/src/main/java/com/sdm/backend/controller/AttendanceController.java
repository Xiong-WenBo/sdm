package com.sdm.backend.controller;

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
import org.springframework.web.bind.annotation.*;

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

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) principal;
            return userService.findByUsername(username);
        }
        return null;
    }

    /**
     * 获取当前宿管管理的楼栋 ID
     */
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

        // 如果是宿管，强制使用其管理的楼栋 ID
        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

        // 如果是辅导员，需要过滤本班级学生（通过 studentId 参数）
        // TODO: 根据辅导员 ID 自动设置 studentId 列表

        List<Attendance> attendances;
        int total;

        if (studentId != null || buildingId != null || checkDate != null || checkTime != null || status != null) {
            attendances = attendanceService.findByPageAndFilters(page, size, studentId, buildingId, checkDate, checkTime, status);
            total = attendanceService.countByFilters(studentId, buildingId, checkDate, checkTime, status);
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
    public ResponseEntity<Result<Attendance>> getAttendanceById(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id);
        if (attendance == null) {
            return ResponseEntity.ok(Result.error(404, "查寝记录不存在"));
        }
        return ResponseEntity.ok(Result.success(attendance));
    }

    /**
     * 获取楼栋学生列表（用于查寝录入）
     */
    @GetMapping("/students/building/{buildingId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<List<Attendance>>> getStudentsInBuilding(@PathVariable Long buildingId) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 如果是宿管，验证楼栋权限
        if (dormAdminBuildingId != null && !dormAdminBuildingId.equals(buildingId)) {
            return ResponseEntity.ok(Result.error(403, "宿管只能查看自己管理的楼栋"));
        }

        List<Attendance> students = attendanceService.findStudentsInBuilding(buildingId);
        return ResponseEntity.ok(Result.success(students));
    }

    /**
     * 获取班级学生列表（用于辅导员查寝）
     */
    @GetMapping("/students/counselor")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<Result<List<Attendance>>> getStudentsByCounselor() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(Result.error(401, "未登录"));
        }

        List<Attendance> students = attendanceService.findStudentsByCounselor(currentUser.getId());
        return ResponseEntity.ok(Result.success(students));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<Void>> createAttendance(@RequestBody Attendance attendance) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 如果是宿管，验证楼栋权限
        if (dormAdminBuildingId != null) {
            // 验证学生是否在本楼栋
            Attendance studentInfo = attendanceService.findStudentsInBuilding(dormAdminBuildingId)
                .stream()
                .filter(s -> s.getStudentId().equals(attendance.getStudentId()))
                .findFirst()
                .orElse(null);
            
            if (studentInfo == null) {
                return ResponseEntity.ok(Result.error(403, "宿管只能录入本楼栋学生"));
            }
        }

        // 检查是否已存在
        Attendance existing = attendanceService.findByStudentAndDate(
            attendance.getStudentId(), 
            attendance.getCheckDate(), 
            attendance.getCheckTime()
        );
        
        if (existing != null) {
            return ResponseEntity.ok(Result.error(400, "该学生该时段的查寝记录已存在"));
        }

        attendance.setCheckerId(currentUser.getId());
        attendanceService.insert(attendance);
        return ResponseEntity.ok(Result.success(null, "查寝记录创建成功"));
    }

    /**
     * 批量录入查寝记录
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> batchCreateAttendance(
            @RequestBody List<Attendance> attendances) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Attendance attendance : attendances) {
            try {
                // 如果是宿管，验证楼栋权限
                if (dormAdminBuildingId != null) {
                    Attendance studentInfo = attendanceService.findStudentsInBuilding(dormAdminBuildingId)
                        .stream()
                        .filter(s -> s.getStudentId().equals(attendance.getStudentId()))
                        .findFirst()
                        .orElse(null);
                    
                    if (studentInfo == null) {
                        errors.add("学生 ID " + attendance.getStudentId() + " 不在本楼栋");
                        errorCount++;
                        continue;
                    }
                }

                attendance.setCheckerId(currentUser.getId());
                
                // 检查是否已存在
                Attendance existing = attendanceService.findByStudentAndDate(
                    attendance.getStudentId(), 
                    attendance.getCheckDate(), 
                    attendance.getCheckTime()
                );
                
                if (existing != null) {
                    // 更新已有记录
                    attendance.setId(existing.getId());
                    attendanceService.update(attendance);
                } else {
                    // 插入新记录
                    attendanceService.insert(attendance);
                }
                successCount++;
            } catch (Exception e) {
                errors.add("学生 ID " + attendance.getStudentId() + ": " + e.getMessage());
                errorCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);

        if (errorCount == 0) {
            return ResponseEntity.ok(Result.success(result, "批量录入成功 " + successCount + " 条记录"));
        } else {
            return ResponseEntity.ok(Result.success(result, "部分成功：" + successCount + " 条，失败：" + errorCount + " 条"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<Void>> updateAttendance(@PathVariable Long id, @RequestBody Attendance attendance) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        Attendance existing = attendanceService.findById(id);
        if (existing == null) {
            return ResponseEntity.ok(Result.error(404, "查寝记录不存在"));
        }

        // 如果是宿管，验证楼栋权限
        if (dormAdminBuildingId != null) {
            Attendance studentInfo = attendanceService.findStudentsInBuilding(dormAdminBuildingId)
                .stream()
                .filter(s -> s.getStudentId().equals(existing.getStudentId()))
                .findFirst()
                .orElse(null);
            
            if (studentInfo == null) {
                return ResponseEntity.ok(Result.error(403, "宿管只能修改本楼栋学生"));
            }
        }

        attendance.setId(id);
        attendanceService.update(attendance);
        return ResponseEntity.ok(Result.success(null, "查寝记录更新成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Void>> deleteAttendance(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id);
        if (attendance == null) {
            return ResponseEntity.ok(Result.error(404, "查寝记录不存在"));
        }
        attendanceService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "查寝记录删除成功"));
    }

    /**
     * 查询当日未归学生（按权限筛选）
     */
    @GetMapping("/absent/today")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<List<Attendance>>> getAbsentStudentsToday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkDate
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 如果没有指定日期，默认为今天
        if (checkDate == null) {
            checkDate = LocalDate.now();
        }

        List<Attendance> absentStudents;

        // 根据角色筛选
        if (dormAdminBuildingId != null) {
            // 宿管：查询本楼栋今日未归学生
            absentStudents = attendanceService.findByPageAndFilters(1, 1000, null, dormAdminBuildingId, checkDate, "EVENING", "ABSENT");
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            // 辅导员：查询本班级今日未归学生（通过 counselor_id 筛选）
            // 这里简化处理，实际应该通过 student.counselor_id 关联查询
            absentStudents = attendanceService.findByPageAndFilters(1, 1000, null, null, checkDate, "EVENING", "ABSENT");
            // TODO: 根据辅导员 ID 过滤
        } else {
            // 超管：查询所有今日未归学生
            absentStudents = attendanceService.findByPageAndFilters(1, 1000, null, null, checkDate, "EVENING", "ABSENT");
        }

        return ResponseEntity.ok(Result.success(absentStudents));
    }
}
