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
        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

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
            return ResponseEntity.ok(Result.error(404, "Assignment not found"));
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

    @GetMapping("/available-rooms")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<List<Room>>> getAvailableRooms(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String gender
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

        List<Room> rooms = buildingId != null ? roomService.findByBuildingId(buildingId) : roomService.findAll();
        List<Room> availableRooms = rooms.stream()
                .filter(room -> room.getCurrentOccupancy() < room.getCapacity())
                .filter(room -> "AVAILABLE".equals(room.getStatus()))
                .filter(room -> gender == null || gender.isEmpty()
                        || "UNISEX".equals(room.getGender())
                        || gender.equals(room.getGender()))
                .toList();

        return ResponseEntity.ok(Result.success(availableRooms));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "CREATE", description = "Create assignment")
    public ResponseEntity<Result<Void>> createAssignment(@RequestBody Assignment assignment) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        Room room = roomService.findById(assignment.getRoomId());
        if (room == null) {
            return ResponseEntity.ok(Result.error(400, "Room not found"));
        }
        if (dormAdminBuildingId != null && !room.getBuildingId().equals(dormAdminBuildingId)) {
            return ResponseEntity.ok(Result.error(403, "Access denied"));
        }
        if (room.getCurrentOccupancy() >= room.getCapacity()) {
            return ResponseEntity.ok(Result.error(400, "Room is already full"));
        }

        List<Assignment> existingAssignments = assignmentService.findByStudentId(assignment.getStudentId());
        for (Assignment existing : existingAssignments) {
            if ("ACTIVE".equals(existing.getStatus())) {
                return ResponseEntity.ok(Result.error(400, "Student already has an active assignment"));
            }
        }

        if (assignment.getCheckInDate() == null) {
            assignment.setCheckInDate(LocalDate.now());
        }
        if (assignment.getStatus() == null) {
            assignment.setStatus("ACTIVE");
        }
        assignment.setCreatedBy(currentUser.getId());

        assignmentService.insert(assignment);
        return ResponseEntity.ok(Result.success(null, "Assignment created successfully"));
    }

    @PutMapping("/{id}/checkout")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "UPDATE", description = "Check out assignment")
    public ResponseEntity<Result<Void>> checkOut(@PathVariable Long id,
                                                 @RequestParam(required = false) LocalDate checkOutDate) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        Assignment assignment = assignmentService.findById(id);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(404, "Assignment not found"));
        }
        if (!"ACTIVE".equals(assignment.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "Assignment is not active"));
        }

        if (dormAdminBuildingId != null) {
            Room room = roomService.findById(assignment.getRoomId());
            if (room == null || !room.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "Access denied"));
            }
        }

        assignmentService.checkOut(id, checkOutDate != null ? checkOutDate : LocalDate.now());
        return ResponseEntity.ok(Result.success(null, "Checked out successfully"));
    }

    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "UPDATE", description = "Transfer assignment")
    public ResponseEntity<Result<Void>> transfer(@PathVariable Long id,
                                                 @RequestParam Long newRoomId,
                                                 @RequestParam String newBedNumber) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        Assignment assignment = assignmentService.findById(id);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(404, "Assignment not found"));
        }
        if (!"ACTIVE".equals(assignment.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "Assignment is not active"));
        }

        Room newRoom = roomService.findById(newRoomId);
        if (newRoom == null) {
            return ResponseEntity.ok(Result.error(400, "Target room not found"));
        }

        if (dormAdminBuildingId != null) {
            Room oldRoom = roomService.findById(assignment.getRoomId());
            if (oldRoom == null
                    || !oldRoom.getBuildingId().equals(dormAdminBuildingId)
                    || !newRoom.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "Access denied"));
            }
        }

        boolean sameRoom = newRoomId.equals(assignment.getRoomId());
        boolean sameBed = newBedNumber.equals(assignment.getBedNumber());
        if (sameRoom && sameBed) {
            return ResponseEntity.ok(Result.error(400, "Target bed is unchanged"));
        }
        if (!sameRoom && newRoom.getCurrentOccupancy() >= newRoom.getCapacity()) {
            return ResponseEntity.ok(Result.error(400, "Target room is already full"));
        }

        assignmentService.transfer(id, newRoomId, newBedNumber);
        return ResponseEntity.ok(Result.success(null, "Transferred successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "ASSIGNMENT", operation = "DELETE", description = "Delete assignment")
    public ResponseEntity<Result<Void>> deleteAssignment(@PathVariable Long id) {
        Assignment assignment = assignmentService.findById(id);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(404, "Assignment not found"));
        }
        assignmentService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "Deleted successfully"));
    }
}
