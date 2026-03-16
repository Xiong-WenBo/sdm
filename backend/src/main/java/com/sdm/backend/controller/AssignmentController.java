package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Assignment;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.Room;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AssignmentService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.RoomService;
import com.sdm.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignment")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private RoomService roomService;

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
    public ResponseEntity<Result<Map<String, Object>>> getAssignmentList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 如果是宿管，强制使用其管理的楼栋 ID
        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

        // 如果是辅导员，需要过滤本班级学生（这里简化处理，暂不实现）
        // TODO: 根据辅导员 ID 过滤学生

        List<Assignment> assignments;
        int total;

        if (studentId != null || roomId != null || buildingId != null || status != null) {
            assignments = assignmentService.findByPageAndFilters(page, size, studentId, roomId, buildingId, status);
            total = assignmentService.countByFilters(studentId, roomId, buildingId, status);
        } else {
            assignments = assignmentService.findByPage(page, size);
            total = assignmentService.countAll();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", assignments);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Assignment>> getAssignmentById(@PathVariable Long id) {
        Assignment assignment = assignmentService.findById(id);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(404, "住宿记录不存在"));
        }
        return ResponseEntity.ok(Result.success(assignment));
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<List<Assignment>>> getAssignmentsByRoomId(@PathVariable Long roomId) {
        List<Assignment> assignments = assignmentService.findByRoomId(roomId);
        return ResponseEntity.ok(Result.success(assignments));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<List<Assignment>>> getAssignmentsByStudentId(@PathVariable Long studentId) {
        List<Assignment> assignments = assignmentService.findByStudentId(studentId);
        return ResponseEntity.ok(Result.success(assignments));
    }

    /**
     * 获取可分配的房间列表
     */
    @GetMapping("/available-rooms")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<List<Room>>> getAvailableRooms(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String gender
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 如果是宿管，强制使用其管理的楼栋 ID
        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

        // 查询可分配的房间（current_occupancy < capacity 且状态为 AVAILABLE）
        // 这里简化处理，实际应该用专门的查询方法
        // 暂时返回所有房间，前端过滤
        List<Room> rooms;
        if (buildingId != null) {
            rooms = roomService.findByBuildingId(buildingId);
        } else {
            rooms = roomService.findAll();
        }

        // 过滤出可分配的房间
        List<Room> availableRooms = rooms.stream()
            .filter(room -> room.getCurrentOccupancy() < room.getCapacity() 
                         && "AVAILABLE".equals(room.getStatus()))
            .filter(room -> gender == null || gender.isEmpty() 
                         || "UNISEX".equals(room.getGender()) 
                         || gender.equals(room.getGender()))
            .toList();

        return ResponseEntity.ok(Result.success(availableRooms));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "CREATE", description = "分配宿舍")
    public ResponseEntity<Result<Void>> createAssignment(@RequestBody Assignment assignment) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 验证房间权限
        if (dormAdminBuildingId != null) {
            Room room = roomService.findById(assignment.getRoomId());
            if (room == null || !room.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "宿管只能操作自己管理的楼栋"));
            }
        }

        // 验证房间是否有空床位
        Room room = roomService.findById(assignment.getRoomId());
        if (room == null) {
            return ResponseEntity.ok(Result.error(400, "房间不存在"));
        }
        if (room.getCurrentOccupancy() >= room.getCapacity()) {
            return ResponseEntity.ok(Result.error(400, "房间已满，无法分配"));
        }

        // 检查学生是否已有住宿记录
        List<Assignment> existingAssignments = assignmentService.findByStudentId(assignment.getStudentId());
        for (Assignment existing : existingAssignments) {
            if ("ACTIVE".equals(existing.getStatus())) {
                return ResponseEntity.ok(Result.error(400, "该学生已有在住的住宿记录"));
            }
        }

        // 设置默认值
        if (assignment.getCheckInDate() == null) {
            assignment.setCheckInDate(LocalDate.now());
        }
        if (assignment.getStatus() == null) {
            assignment.setStatus("ACTIVE");
        }
        assignment.setCreatedBy(currentUser.getId());

        assignmentService.insert(assignment);
        return ResponseEntity.ok(Result.success(null, "分配成功"));
    }

    @PutMapping("/{id}/checkout")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "UPDATE", description = "退宿处理")
    public ResponseEntity<Result<Void>> checkOut(@PathVariable Long id,
                                                 @RequestParam(required = false) LocalDate checkOutDate) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        Assignment assignment = assignmentService.findById(id);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(404, "住宿记录不存在"));
        }

        // 验证房间权限
        if (dormAdminBuildingId != null) {
            Room room = roomService.findById(assignment.getRoomId());
            if (room == null || !room.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "宿管只能操作自己管理的楼栋"));
            }
        }

        if (checkOutDate == null) {
            checkOutDate = LocalDate.now();
        }

        assignmentService.checkOut(id, checkOutDate);
        return ResponseEntity.ok(Result.success(null, "退宿成功"));
    }

    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "UPDATE", description = "调宿处理")
    public ResponseEntity<Result<Void>> transfer(@PathVariable Long id,
                                                 @RequestParam Long newRoomId,
                                                 @RequestParam String newBedNumber) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        Assignment assignment = assignmentService.findById(id);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(404, "住宿记录不存在"));
        }

        // 验证原房间权限
        if (dormAdminBuildingId != null) {
            Room oldRoom = roomService.findById(assignment.getRoomId());
            Room newRoom = roomService.findById(newRoomId);
            if (oldRoom == null || newRoom == null || 
                !oldRoom.getBuildingId().equals(dormAdminBuildingId) ||
                !newRoom.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "宿管只能操作自己管理的楼栋"));
            }
        }

        // 验证新房间是否有空床位
        Room newRoom = roomService.findById(newRoomId);
        if (newRoom.getCurrentOccupancy() >= newRoom.getCapacity()) {
            return ResponseEntity.ok(Result.error(400, "目标房间已满，无法调宿"));
        }

        assignmentService.transfer(id, newRoomId, newBedNumber);
        return ResponseEntity.ok(Result.success(null, "调宿成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "DELETE", description = "删除住宿记录")
    public ResponseEntity<Result<Void>> deleteAssignment(@PathVariable Long id) {
        Assignment assignment = assignmentService.findById(id);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(404, "住宿记录不存在"));
        }
        assignmentService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "删除成功"));
    }
}
