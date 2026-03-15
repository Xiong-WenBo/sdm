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
     * 手动触发查询当日未归学生并发送通知
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

        // 根据角色筛选
        if (dormAdminBuildingId != null) {
            // 宿管：查询本楼栋今日未归学生
            List<Attendance> absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, null, dormAdminBuildingId, checkDate, "EVENING", "ABSENT"
            );
            
            if (!absentStudents.isEmpty()) {
                // 通知辅导员（本楼栋有哪些班级的学生未归）
                notifiedCounselors = notifyCounselorsOfAbsentStudents(absentStudents, checkDate);
                
                // 通知学生本人
                notifiedStudents = notifyStudentsOfAbsent(absentStudents, checkDate);
                
                totalAbsent = absentStudents.size();
            }
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            // 辅导员：查询本班级今日未归学生
            List<Attendance> absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, null, null, checkDate, "EVENING", "ABSENT"
            );
            // TODO: 根据辅导员 ID 过滤本班级学生
            
            if (!absentStudents.isEmpty()) {
                // 通知学生本人
                notifiedStudents = notifyStudentsOfAbsent(absentStudents, checkDate);
                
                totalAbsent = absentStudents.size();
            }
        } else {
            // 超管：查询所有今日未归学生
            List<Attendance> absentStudents = attendanceService.findByPageAndFilters(
                1, 1000, null, null, checkDate, "EVENING", "ABSENT"
            );
            
            if (!absentStudents.isEmpty()) {
                // 按楼栋分组通知宿管
                notifiedDormAdmins = notifyDormAdminsOfAbsentStudents(absentStudents, checkDate);
                
                // 按班级分组通知辅导员
                notifiedCounselors = notifyCounselorsOfAbsentStudents(absentStudents, checkDate);
                
                // 通知学生本人
                notifiedStudents = notifyStudentsOfAbsent(absentStudents, checkDate);
                
                totalAbsent = absentStudents.size();
            }
        }

        result.put("totalAbsent", totalAbsent);
        result.put("notifiedDormAdmins", notifiedDormAdmins);
        result.put("notifiedCounselors", notifiedCounselors);
        result.put("notifiedStudents", notifiedStudents);

        String message = String.format("发现 %d 名未归学生，已通知 %d 名宿管、%d 名辅导员、%d 名学生",
            totalAbsent, notifiedDormAdmins, notifiedCounselors, notifiedStudents);
        
        return ResponseEntity.ok(Result.success(result, message));
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
            // 这里简化处理，假设每个学生的 buildingName 相同
            // 实际应该通过 assignment 表查询楼栋
            Long buildingId = 1L; // TODO: 获取实际楼栋 ID
            byBuilding.computeIfAbsent(buildingId, k -> new ArrayList<>()).add(student);
        }
        
        int notified = 0;
        for (Map.Entry<Long, List<Attendance>> entry : byBuilding.entrySet()) {
            Building building = buildingService.findById(entry.getKey());
            if (building != null && building.getAdminId() != null) {
                String title = String.format("【查寝通知】%s 有 %d 名学生未归", 
                    building.getName(), entry.getValue().size());
                String content = buildAbsentStudentContent(entry.getValue(), date);
                messageService.sendAttendanceNotification(building.getAdminId(), title, content);
                notified++;
            }
        }
        return notified;
    }

    /**
     * 通知辅导员（本班级有学生未归）
     */
    private int notifyCounselorsOfAbsentStudents(List<Attendance> students, LocalDate date) {
        // 按班级分组
        Map<String, List<Attendance>> byClass = new HashMap<>();
        for (Attendance student : students) {
            byClass.computeIfAbsent(student.getClassName(), k -> new ArrayList<>()).add(student);
        }
        
        int notified = 0;
        // 给每个班级的辅导员发送通知
        for (Map.Entry<String, List<Attendance>> entry : byClass.entrySet()) {
            // TODO: 根据班级找到辅导员 ID
            // 这里简化处理，假设辅导员 ID 已知
            // 实际应该查询 student 表中该班级所有学生的 counselor_id
            Long counselorId = 4L; // 示例：辅导员 ID
            
            if (counselorId != null) {
                String title = String.format("【查寝通知】%s 有 %d 名学生未归", 
                    entry.getKey(), entry.getValue().size());
                String content = buildAbsentStudentContent(entry.getValue(), date);
                messageService.sendAttendanceNotification(counselorId, title, content);
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
