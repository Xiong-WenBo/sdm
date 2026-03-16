package com.sdm.backend.scheduler;

import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.Message;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AttendanceService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.MessageService;
import com.sdm.backend.service.StudentService;
import com.sdm.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class AttendanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceScheduler.class);

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    /**
     * 每晚 10:30 自动筛选未归学生并发送通知
     * Cron 表达式：秒 分 时 日 月 周
     * 0 30 22 * * ? = 每天 22:30 执行
     */
    @Scheduled(cron = "0 30 22 * * ?")
    public void checkAbsentStudents() {
        log.info("开始执行定时任务：检查今日未归学生");
        
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // 获取所有楼栋
        List<com.sdm.backend.entity.Building> buildings = buildingService.findAll();
        
        int totalAbsent = 0;
        
        for (com.sdm.backend.entity.Building building : buildings) {
            // 查询本楼栋今日晚上未归且未请假的学生
            List<Attendance> absentWithoutLeaveStudents = getAbsentWithoutLeaveStudents(
                building.getId(), today
            );
            
            if (!absentWithoutLeaveStudents.isEmpty()) {
                log.info("楼栋 {} 有 {} 名学生未归且未请假", building.getName(), absentWithoutLeaveStudents.size());
                
                // 给楼栋管理员发送通知
                if (building.getAdminId() != null) {
                    String title = String.format("【查寝通知】%s 有 %d 名学生未归且未请假", 
                        building.getName(), absentWithoutLeaveStudents.size());
                    StringBuilder content = new StringBuilder("查寝时间：" + today + " 晚上\n\n");
                    content.append("未归且未请假学生名单：\n");
                    
                    for (Attendance student : absentWithoutLeaveStudents) {
                        content.append("- ")
                               .append(student.getStudentName())
                               .append(" (")
                               .append(student.getStudentNumber())
                               .append(") - ")
                               .append(student.getClassName())
                               .append("\n");
                    }
                    
                    messageService.sendAttendanceNotification(
                        building.getAdminId(), 
                        title, 
                        content.toString()
                    );
                    
                    log.info("已向楼栋管理员 {} 发送通知", building.getAdminId());
                }
                
                totalAbsent += absentWithoutLeaveStudents.size();
            }
        }
        
        log.info("定时任务执行完成：今日共有 {} 名学生未归且未请假", totalAbsent);
    }

    /**
     * 查询未归且未请假的学生列表
     */
    private List<Attendance> getAbsentWithoutLeaveStudents(Long buildingId, LocalDate checkDate) {
        // 查询未归学生
        List<Attendance> absentStudents = attendanceService.findByPageAndFilters(
            1, 1000, null, buildingId, checkDate, "EVENING", "ABSENT"
        );
        
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
     * 测试用：每分钟执行一次（开发环境使用）
     * 使用时取消 @Scheduled 注释
     */
    // @Scheduled(cron = "0 */1 * * * ?")
    public void testCheckAbsentStudents() {
        log.info("测试任务：检查未归且未请假学生");
        checkAbsentStudents();
    }
}
