package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.Message;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AssignmentService assignmentService;

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

    /**
     * 查询当日未归且未请假的学生列表（用于后续通知和统计）
     */
    @GetMapping("/absent/without-leave")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<List<Attendance>>> getAbsentStudentsWithoutLeave(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkDate
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
        
        // 如果没有指定日期，默认为今天
        if (checkDate == null) {
            checkDate = LocalDate.now();
        }
        
        List<Attendance> absentStudents;
        
        // 根据角色筛选未归学生
        if (dormAdminBuildingId != null) {
            // 宿管：查询本楼栋今日未归学生
            absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, null, dormAdminBuildingId, checkDate, "EVENING", "ABSENT"
            );
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            // 辅导员：查询本班级今日未归学生
            absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, currentUser.getId(), null, checkDate, "EVENING", "ABSENT"
            );
        } else {
            // 超管：查询所有今日未归学生
            absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, null, null, checkDate, "EVENING", "ABSENT"
            );
        }
        
        // 获取这些学生的 IDs
        if (absentStudents.isEmpty()) {
            return ResponseEntity.ok(Result.success(new ArrayList<>()));
        }
        
        List<Long> absentStudentIds = absentStudents.stream()
            .map(Attendance::getStudentId)
            .toList();
        
        // 查询这些学生在指定日期是否有请假申请（PENDING 或 APPROVED 状态）
        List<Long> onLeaveStudentIds = studentService.findStudentsOnLeave(absentStudentIds, checkDate);
        
        // 过滤掉请假的学生，返回未请假且未归的学生
        List<Attendance> absentWithoutLeave = absentStudents.stream()
            .filter(student -> !onLeaveStudentIds.contains(student.getStudentId()))
            .toList();
        
        return ResponseEntity.ok(Result.success(absentWithoutLeave));
    }

    /**
     * 手动触发查询当日未归且未请假学生并发送通知
     */
    @PostMapping("/absent/today")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Map<String, Object>>> notifyAbsentStudentsToday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkDate
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 如果没有指定日期，默认为今天
        if (checkDate == null) {
            checkDate = LocalDate.now();
        }

        Map<String, Object> result = new HashMap<>();
        int notifiedCounselors = 0;
        int notifiedStudents = 0;
        int notifiedDormAdmins = 0;
        int totalAbsent = 0;

        // 根据角色筛选未归且未请假的学生
        if (dormAdminBuildingId != null) {
            // 宿管：查询本楼栋今日未归且未请假学生
            List<Attendance> absentWithoutLeaveStudents = getAbsentWithoutLeaveStudents(
                null, dormAdminBuildingId, checkDate
            );
            
            if (!absentWithoutLeaveStudents.isEmpty()) {
                // 通知辅导员（本楼栋有哪些班级的学生未归且未请假）
                notifiedCounselors = notifyCounselorsOfAbsentStudents(absentWithoutLeaveStudents, checkDate);
                
                // 通知学生本人
                notifiedStudents = notifyStudentsOfAbsent(absentWithoutLeaveStudents, checkDate);
                
                totalAbsent = absentWithoutLeaveStudents.size();
            }
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            // 辅导员：查询本班级今日未归且未请假学生
            List<Attendance> absentWithoutLeaveStudents = getAbsentWithoutLeaveStudents(
                currentUser.getId(), null, checkDate
            );
            
            if (!absentWithoutLeaveStudents.isEmpty()) {
                // 通知学生本人
                notifiedStudents = notifyStudentsOfAbsent(absentWithoutLeaveStudents, checkDate);
                
                totalAbsent = absentWithoutLeaveStudents.size();
            }
        } else {
            // 超管：查询所有今日未归且未请假学生
            List<Attendance> absentWithoutLeaveStudents = getAbsentWithoutLeaveStudents(
                null, null, checkDate
            );
            
            if (!absentWithoutLeaveStudents.isEmpty()) {
                // 按楼栋分组通知宿管
                notifiedDormAdmins = notifyDormAdminsOfAbsentStudents(absentWithoutLeaveStudents, checkDate);
                
                // 按班级分组通知辅导员
                notifiedCounselors = notifyCounselorsOfAbsentStudents(absentWithoutLeaveStudents, checkDate);
                
                // 通知学生本人
                notifiedStudents = notifyStudentsOfAbsent(absentWithoutLeaveStudents, checkDate);
                
                totalAbsent = absentWithoutLeaveStudents.size();
            }
        }

        result.put("totalAbsent", totalAbsent);
        result.put("notifiedDormAdmins", notifiedDormAdmins);
        result.put("notifiedCounselors", notifiedCounselors);
        result.put("notifiedStudents", notifiedStudents);

        String message = String.format("发现 %d 名未归且未请假学生，已通知 %d 名宿管、%d 名辅导员、%d 名学生",
            totalAbsent, notifiedDormAdmins, notifiedCounselors, notifiedStudents);
        
        return ResponseEntity.ok(Result.success(result, message));
    }

    /**
     * 查询未归且未请假的学生列表
     */
    private List<Attendance> getAbsentWithoutLeaveStudents(Long counselorId, Long buildingId, LocalDate checkDate) {
        List<Attendance> absentStudents;
        
        // 查询未归学生
        if (buildingId != null) {
            // 宿管：查询本楼栋
            absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, null, buildingId, checkDate, "EVENING", "ABSENT"
            );
        } else if (counselorId != null) {
            // 辅导员：查询本班级
            absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, counselorId, null, checkDate, "EVENING", "ABSENT"
            );
        } else {
            // 超管：查询全部
            absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, null, null, checkDate, "EVENING", "ABSENT"
            );
        }
        
        if (absentStudents.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取未归学生 IDs
        List<Long> absentStudentIds = absentStudents.stream()
            .map(Attendance::getStudentId)
            .toList();
        
        // 查询这些学生在指定日期是否有请假申请（PENDING 或 APPROVED 状态）
        List<Long> onLeaveStudentIds = studentService.findStudentsOnLeave(absentStudentIds, checkDate);
        
        // 过滤掉请假的学生，返回未归且未请假的学生
        return absentStudents.stream()
            .filter(student -> !onLeaveStudentIds.contains(student.getStudentId()))
            .toList();
    }

    /**
     * 构建未归学生通知内容
     */
    private String buildAbsentStudentContent(List<Attendance> students, LocalDate date) {
        StringBuilder content = new StringBuilder("查寝时间：" + date + " 晚上\n\n");
        content.append("未归学生名单：\n");
        
        for (Attendance student : students) {
            content.append("- ")
                   .append(student.getStudentName())
                   .append(" (")
                   .append(student.getStudentNumber())
                   .append(") - ")
                   .append(student.getClassName())
                   .append("\n");
        }
        
        return content.toString();
    }

    /**
     * 通知宿管（本楼栋有学生未归）
     */
    private int notifyDormAdminsOfAbsentStudents(List<Attendance> students, LocalDate date) {
        // 按楼栋分组
        Map<Long, List<Attendance>> byBuilding = new HashMap<>();
        for (Attendance student : students) {
            // 通过 assignment 表查询学生所在的楼栋
            Long buildingId = getBuildingIdByStudentId(student.getStudentId());
            if (buildingId != null) {
                byBuilding.computeIfAbsent(buildingId, k -> new ArrayList<>()).add(student);
            }
        }
        
        int notified = 0;
        for (Map.Entry<Long, List<Attendance>> entry : byBuilding.entrySet()) {
            Long buildingId = entry.getKey();
            Building building = buildingService.findById(buildingId);
            
            if (building != null && building.getAdminId() != null) {
                String title = String.format("【查寝通知】%s 有 %d 名学生未归", 
                    building.getName(), entry.getValue().size());
                String content = buildAbsentStudentContent(entry.getValue(), date);
                messageService.sendBusinessNotification(
                    building.getAdminId(),
                    null,
                    "ATTENDANCE",
                    "SYSTEM",
                    title,
                    content,
                    "ATTENDANCE",
                    null
                );
                notified++;
            }
        }
        return notified;
    }

    /**
     * 根据学生 ID 查询所在楼栋 ID
     */
    private Long getBuildingIdByStudentId(Long studentId) {
        // 通过 assignment 表查询学生当前所在的楼栋
        List<com.sdm.backend.entity.Assignment> assignments = assignmentService.findByStudentId(studentId);
        if (!assignments.isEmpty()) {
            // 返回第一个分配的楼栋（通常学生只住一个房间）
            return assignments.get(0).getRoomId() != null ? 
                   assignmentService.findBuildingIdByRoomId(assignments.get(0).getRoomId()) : null;
        }
        return null;
    }

    /**
     * 通知辅导员（本班级有学生未归）
     */
    private int notifyCounselorsOfAbsentStudents(List<Attendance> students, LocalDate date) {
        // 按辅导员 ID 分组（而不是按班级）
        Map<Long, List<Attendance>> byCounselor = new HashMap<>();
        
        for (Attendance student : students) {
            // 通过 student_id 查询学生的辅导员 ID
            Long counselorId = studentService.getCounselorIdByStudentId(student.getStudentId());
            
            if (counselorId != null) {
                byCounselor.computeIfAbsent(counselorId, k -> new ArrayList<>()).add(student);
            }
        }
        
        int notified = 0;
        // 给每个辅导员发送通知（包含该辅导员负责的所有未归学生）
        for (Map.Entry<Long, List<Attendance>> entry : byCounselor.entrySet()) {
            Long counselorId = entry.getKey();
            List<Attendance> counselorStudents = entry.getValue();
            
            if (counselorId != null) {
                String title = String.format("【查寝通知】你有 %d 名学生未归", counselorStudents.size());
                String content = buildAbsentStudentContent(counselorStudents, date);
                messageService.sendBusinessNotification(
                    counselorId, 
                    null, 
                    "ATTENDANCE", 
                    "SYSTEM", 
                    title, 
                    content, 
                    "ATTENDANCE", 
                    null
                );
                notified++;
            }
        }
        return notified;
    }

    /**
     * 通知学生本人（你未归）
     */
    private int notifyStudentsOfAbsent(List<Attendance> students, LocalDate date) {
        int notified = 0;
        for (Attendance student : students) {
            // 通过 student_id 找到 user_id
            Long userId = studentService.getUserIdByStudentId(student.getStudentId());
            
            if (userId != null) {
                String title = "【查寝通知】你今晚未归";
                String content = String.format("查寝时间：%s 晚上\n\n你今晚被查寝记录为未归状态。\n如有特殊情况，请及时向辅导员和宿管说明。", date);
                messageService.sendAttendanceNotification(userId, title, content);
                notified++;
            }
        }
        return notified;
    }
}
