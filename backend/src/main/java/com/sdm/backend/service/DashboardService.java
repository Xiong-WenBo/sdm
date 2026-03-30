package com.sdm.backend.service;

import com.sdm.backend.entity.*;
import com.sdm.backend.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        
        // 计算入住率（入住人数/总床位数）
        int totalCapacity = rooms.stream().mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
        double occupancyRate = totalCapacity > 0 ? (double) totalOccupancy / totalCapacity * 100 : 0;
        stats.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);
        stats.put("totalCapacity", totalCapacity);
        
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
        stats.put("totalRepairs", allRepairs.size());
        
        // 今日请假统计（过滤非今日数据，同一人合并）
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        List<LeaveRequest> allLeaves = leaveRequestMapper.findAll();
        long pendingLeaves = allLeaves.stream()
            .filter(l -> "PENDING".equals(l.getStatus()))
            .filter(l -> {
                // 过滤出今日在请假期间内的记录
                return !(l.getEndTime().isBefore(startOfDay) || l.getStartTime().isAfter(endOfDay));
            })
            .map(LeaveRequest::getStudentId)
            .distinct()
            .count();
        
        stats.put("pendingLeaves", pendingLeaves);
        long approvedLeaves = allLeaves.stream()
            .filter(l -> "APPROVED".equals(l.getStatus()))
            .filter(l -> !(l.getEndTime().isBefore(startOfDay) || l.getStartTime().isAfter(endOfDay)))
            .map(LeaveRequest::getStudentId)
            .distinct()
            .count();
        stats.put("approvedLeaves", approvedLeaves);
        
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
        
        // 计算入住率（入住人数/总床位数）
        int totalCapacity = rooms.stream().mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
        double occupancyRate = totalCapacity > 0 ? (double) totalOccupancy / totalCapacity * 100 : 0;
        stats.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);
        stats.put("totalCapacity", totalCapacity);
        
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
        stats.put("totalRepairs", buildingRepairs.size());
        
        // 本楼栋今日请假人数（过滤非今日数据，同一人合并）
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
        List<LeaveRequest> buildingLeaves = leaveRequestMapper.findByBuildingId(buildingId);
        long todayLeaves = buildingLeaves.stream()
            .filter(l -> "PENDING".equals(l.getStatus()) || "APPROVED".equals(l.getStatus()))
            .filter(l -> !(l.getEndTime().isBefore(startOfDay) || l.getStartTime().isAfter(endOfDay)))
            .map(LeaveRequest::getStudentId)
            .distinct()
            .count();
        stats.put("buildingLeaves", todayLeaves);
        
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
        
        // 请假统计（过滤非今日数据，同一人合并）
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        List<LeaveRequest> classLeaves = leaveRequestMapper.findByCounselorId(counselorId);
        long pendingLeaves = classLeaves.stream()
            .filter(l -> "PENDING".equals(l.getStatus()))
            .filter(l -> !(l.getEndTime().isBefore(startOfDay) || l.getStartTime().isAfter(endOfDay)))
            .map(LeaveRequest::getStudentId)
            .distinct()
            .count();
        
        long approvedLeaves = classLeaves.stream()
            .filter(l -> "APPROVED".equals(l.getStatus()))
            .filter(l -> !(l.getEndTime().isBefore(startOfDay) || l.getStartTime().isAfter(endOfDay)))
            .map(LeaveRequest::getStudentId)
            .distinct()
            .count();
        
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

    /**
     * 获取近 7 天入住率趋势
     */
    public Map<String, Object> getOccupancyTrend(Long buildingId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.format(formatter));
            
            // 计算当天的入住率（简化处理，实际应该按天统计）
            List<Room> rooms = buildingId == null ? roomMapper.findAll() : roomMapper.findByBuildingId(buildingId);
            int totalCapacity = rooms.stream().mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
            
            List<Assignment> assignments = buildingId == null ? assignmentMapper.findAll() : assignmentMapper.findByBuildingId(buildingId);
            long occupancy = assignments.stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .filter(a -> a.getCheckInDate() != null && !a.getCheckInDate().isAfter(date))
                .filter(a -> a.getCheckOutDate() == null || a.getCheckOutDate().isAfter(date))
                .count();
            
            double rate = totalCapacity > 0 ? (double) occupancy / totalCapacity * 100 : 0;
            rates.add(Math.round(rate * 100.0) / 100.0);
        }
        
        result.put("dates", dates);
        result.put("rates", rates);
        return result;
    }

    /**
     * 获取请假类型分布
     */
    public Map<String, Object> getLeaveTypeDistribution(List<LeaveRequest> leaveRequests) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> types = new ArrayList<>();
        
        Map<String, Long> typeCount = leaveRequests.stream()
            .filter(l -> l.getType() != null)
            .collect(Collectors.groupingBy(LeaveRequest::getType, Collectors.counting()));
        
        typeCount.forEach((type, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", getLeaveTypeName(type));
            item.put("value", count);
            types.add(item);
        });
        
        result.put("types", types);
        return result;
    }

    /**
     * 获取查寝情况统计
     */
    public Map<String, Object> getAttendanceStatus(List<Attendance> attendances) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> status = new ArrayList<>();

        long normal = attendances.stream()
            .filter(a -> "NORMAL".equals(a.getStatus()) || "PRESENT".equals(a.getStatus()))
            .count();
        long absent = attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long leave = attendances.stream().filter(a -> "LEAVE".equals(a.getStatus())).count();
        
        status.add(createPieItem("正常", normal));
        status.add(createPieItem("未归", absent));
        status.add(createPieItem("请假", leave));
        
        result.put("status", status);
        return result;
    }

    /**
     * 获取报修处理统计
     */
    public Map<String, Object> getRepairStatus(List<Repair> repairs) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> status = new ArrayList<>();

        long pending = repairs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        long processing = repairs.stream().filter(r -> "PROCESSING".equals(r.getStatus())).count();
        long completed = repairs.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
        
        status.add(createPieItem("待处理", pending, "#F56C6C"));
        status.add(createPieItem("处理中", processing, "#E6A23C"));
        status.add(createPieItem("已完成", completed, "#67C23A"));
        
        result.put("status", status);
        return result;
    }

    public List<LeaveRequest> getAllLeaves() {
        return leaveRequestMapper.findAll();
    }

    public List<LeaveRequest> getLeavesByBuildingId(Long buildingId) {
        return leaveRequestMapper.findByBuildingId(buildingId);
    }

    public List<LeaveRequest> getLeavesByCounselorId(Long counselorId) {
        return leaveRequestMapper.findByCounselorId(counselorId);
    }

    public List<Attendance> getAttendancesByDate(LocalDate date) {
        return attendanceMapper.findByDate(date);
    }

    public List<Repair> getAllRepairs() {
        return repairMapper.findAll();
    }

    public List<Repair> getRepairsByBuildingId(Long buildingId) {
        return repairMapper.findByBuildingId(buildingId);
    }

    /**
     * 获取各楼栋入住率对比
     */
    public Map<String, Object> getBuildingOccupancy() {
        Map<String, Object> result = new HashMap<>();
        List<String> buildings = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        
        List<Building> buildingList = buildingMapper.findAll();
        
        for (Building building : buildingList) {
            buildings.add(building.getName());
            
            List<Room> rooms = roomMapper.findByBuildingId(building.getId());
            int totalCapacity = rooms.stream().mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
            
            List<Assignment> assignments = assignmentMapper.findByBuildingId(building.getId());
            long occupancy = assignments.stream()
                .filter(a -> a.getCheckInDate() != null)
                .filter(a -> a.getCheckOutDate() == null)
                .count();
            
            double rate = totalCapacity > 0 ? (double) occupancy / totalCapacity * 100 : 0;
            rates.add(Math.round(rate * 100.0) / 100.0);
        }
        
        result.put("buildings", buildings);
        result.put("rates", rates);
        return result;
    }

    private String getLeaveTypeName(String type) {
        switch (type) {
            case "SICK_LEAVE": return "病假";
            case "PERSONAL_LEAVE": return "事假";
            case "OFFICIAL_LEAVE": return "公假";
            default: return type;
        }
    }

    private Map<String, Object> createPieItem(String name, Number value) {
        return createPieItem(name, value, null);
    }

    private Map<String, Object> createPieItem(String name, Number value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        if (color != null) {
            item.put("itemStyle", Map.of("color", color));
        }
        return item;
    }
}
