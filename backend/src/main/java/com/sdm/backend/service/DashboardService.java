package com.sdm.backend.service;

import com.sdm.backend.entity.*;
import com.sdm.backend.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private RepairMapper repairMapper;

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    /**
     * 获取超级管理员统计数据
     */
    public Map<String, Object> getSuperAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 基础统计
        List<Building> buildings = buildingMapper.findAll();
        stats.put("totalBuildings", buildings.size());
        
        List<Room> rooms = roomMapper.findAll();
        stats.put("totalRooms", rooms.size());
        
        int totalOccupancy = rooms.stream().mapToInt(r -> r.getCurrentOccupancy() != null ? r.getCurrentOccupancy() : 0).sum();
        stats.put("totalOccupancy", totalOccupancy);
        
        // 计算入住率
        double occupancyRate = rooms.size() > 0 ? (double) totalOccupancy / rooms.size() * 100 : 0;
        stats.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);
        
        List<Student> students = studentMapper.findAll();
        stats.put("totalStudents", students.size());
        
        // 今日统计
        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendance = attendanceMapper.findByDate(today);
        stats.put("todayAttendance", todayAttendance.size());
        
        long todayAbsent = todayAttendance.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        stats.put("todayAbsent", todayAbsent);
        
        // 待处理统计
        List<Repair> allRepairs = repairMapper.findAll();
        long pendingRepairs = allRepairs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        stats.put("pendingRepairs", pendingRepairs);
        
        List<LeaveRequest> allLeaves = leaveRequestMapper.findAll();
        long pendingLeaves = allLeaves.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        stats.put("pendingLeaves", pendingLeaves);
        
        return stats;
    }

    /**
     * 获取宿管统计数据（本楼栋）
     */
    public Map<String, Object> getDormAdminStats(Long buildingId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (buildingId == null) {
            return stats;
        }
        
        // 基础统计
        List<Room> rooms = roomMapper.findByBuildingId(buildingId);
        stats.put("totalRooms", rooms.size());
        
        long occupiedRooms = rooms.stream().filter(r -> r.getCurrentOccupancy() != null && r.getCurrentOccupancy() > 0).count();
        int totalOccupancy = rooms.stream().mapToInt(r -> r.getCurrentOccupancy() != null ? r.getCurrentOccupancy() : 0).sum();
        
        stats.put("occupiedRooms", occupiedRooms);
        stats.put("totalOccupancy", totalOccupancy);
        
        // 计算入住率
        double occupancyRate = rooms.size() > 0 ? (double) occupiedRooms / rooms.size() * 100 : 0;
        stats.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);
        
        // 今日统计 - 通过楼栋 ID 查询本楼栋学生
        LocalDate today = LocalDate.now();
        List<Assignment> assignments = assignmentMapper.findByBuildingId(buildingId);
        List<Long> studentIds = assignments.stream().map(Assignment::getStudentId).toList();
        
        List<Attendance> todayAttendance = attendanceMapper.findByDate(today);
        long buildingAttendance = todayAttendance.stream()
            .filter(a -> studentIds.contains(a.getStudentId()))
            .count();
        stats.put("todayAttendance", buildingAttendance);
        
        long buildingAbsent = todayAttendance.stream()
            .filter(a -> studentIds.contains(a.getStudentId()) && "ABSENT".equals(a.getStatus()))
            .count();
        stats.put("todayAbsent", buildingAbsent);
        
        // 待处理统计
        List<Repair> buildingRepairs = repairMapper.findByBuildingId(buildingId);
        long pendingRepairs = buildingRepairs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        stats.put("pendingRepairs", pendingRepairs);
        
        // 本楼栋请假人数
        List<LeaveRequest> buildingLeaves = leaveRequestMapper.findByBuildingId(buildingId);
        stats.put("buildingLeaves", buildingLeaves.size());
        
        return stats;
    }

    /**
     * 获取辅导员统计数据（本班级）
     */
    public Map<String, Object> getCounselorStats(Long counselorId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (counselorId == null) {
            return stats;
        }
        
        // 基础统计 - 查询本班级学生
        List<Student> students = studentMapper.findByCounselorId(counselorId);
        stats.put("totalStudents", students.size());
        
        // 今日统计
        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendance = attendanceMapper.findByDate(today);
        
        // 过滤本班级学生
        List<Long> studentIds = students.stream().map(Student::getId).toList();
        List<Attendance> classAttendance = todayAttendance.stream()
            .filter(a -> studentIds.contains(a.getStudentId()))
            .toList();
        
        stats.put("todayAttendance", classAttendance.size());
        
        long present = classAttendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long leave = classAttendance.stream().filter(a -> "LEAVE".equals(a.getStatus())).count();
        long absent = classAttendance.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        
        stats.put("todayPresent", present);
        stats.put("todayLeave", leave);
        stats.put("todayAbsent", absent);
        
        // 请假统计
        List<LeaveRequest> classLeaves = leaveRequestMapper.findByCounselorId(counselorId);
        long pendingLeaves = classLeaves.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        long approvedLeaves = classLeaves.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        
        stats.put("pendingLeaves", pendingLeaves);
        stats.put("approvedLeaves", approvedLeaves);
        
        return stats;
    }

    /**
     * 获取学生统计数据
     */
    public Map<String, Object> getStudentStats(Long studentId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (studentId == null) {
            return stats;
        }
        
        // 住宿信息
        Assignment assignment = assignmentMapper.findActiveByStudentId(studentId);
        if (assignment != null) {
            Room room = roomMapper.findById(assignment.getRoomId());
            Building building = buildingMapper.findById(room.getBuildingId());
            
            Map<String, String> housingInfo = new HashMap<>();
            housingInfo.put("buildingName", building != null ? building.getName() : "");
            housingInfo.put("roomNumber", room != null ? room.getRoomNumber() : "");
            housingInfo.put("bedNumber", assignment.getBedNumber());
            stats.put("housingInfo", housingInfo);
        } else {
            stats.put("housingInfo", null);
        }
        
        // 报修统计
        List<Repair> studentRepairs = repairMapper.findByStudentId(studentId);
        long pendingRepairs = studentRepairs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        long processingRepairs = studentRepairs.stream().filter(r -> "PROCESSING".equals(r.getStatus())).count();
        
        stats.put("pendingRepairs", pendingRepairs);
        stats.put("processingRepairs", processingRepairs);
        
        // 请假统计
        List<LeaveRequest> studentLeaves = leaveRequestMapper.findByStudentId(studentId);
        long pendingLeaves = studentLeaves.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        long approvedLeaves = studentLeaves.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        
        stats.put("pendingLeaves", pendingLeaves);
        stats.put("approvedLeaves", approvedLeaves);
        
        // 最近查寝状态
        List<Attendance> attendances = attendanceMapper.findByStudentId(studentId);
        if (!attendances.isEmpty()) {
            Attendance latest = attendances.get(0);
            Map<String, Object> lastAttendance = new HashMap<>();
            lastAttendance.put("date", latest.getCheckDate());
            lastAttendance.put("status", latest.getStatus());
            stats.put("lastAttendance", lastAttendance);
        } else {
            stats.put("lastAttendance", null);
        }
        
        return stats;
    }
}
