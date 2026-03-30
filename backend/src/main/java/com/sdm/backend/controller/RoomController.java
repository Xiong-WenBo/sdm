package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.BulkCreateRoomsRequest;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.Room;
import com.sdm.backend.entity.User;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private UserService userService;

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

    private Long getDormAdminBuildingId(User user) {
        if (user == null || !"DORM_ADMIN".equals(user.getRole())) {
            return null;
        }
        Building building = buildingService.findByAdminUserId(user.getId());
        return building != null ? building.getId() : null;
    }

    @GetMapping("/list")
    public ResponseEntity<Result<Map<String, Object>>> getRoomList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        if (dormAdminBuildingId != null) {
            buildingId = dormAdminBuildingId;
        }

        List<Room> rooms;
        int total;

        if (buildingId != null || status != null) {
            rooms = roomService.findByPageAndFilters(page, size, buildingId, status);
            total = roomService.countByFilters(buildingId, status);
        } else {
            rooms = roomService.findByPage(page, size);
            total = roomService.countAll();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", rooms);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Room>> getRoomById(@PathVariable Long id) {
        Room room = roomService.findById(id);
        if (room == null) {
            return ResponseEntity.ok(Result.error(404, "Room not found"));
        }
        return ResponseEntity.ok(Result.success(room));
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<Result<List<Room>>> getRoomsByBuildingId(@PathVariable Long buildingId) {
        return ResponseEntity.ok(Result.success(roomService.findByBuildingId(buildingId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ROOM", operation = "CREATE", description = "Create room")
    public ResponseEntity<Result<Void>> createRoom(@RequestBody Room room) {
        User currentUser = getCurrentUser();

        if (currentUser != null && "DORM_ADMIN".equals(currentUser.getRole())) {
            Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
            if (dormAdminBuildingId == null) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin has no assigned building"));
            }
            if (room.getBuildingId() != null && !room.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin can only create rooms in the assigned building"));
            }
            room.setBuildingId(dormAdminBuildingId);
        }

        if (room.getBuildingId() == null) {
            return ResponseEntity.ok(Result.error(400, "Building is required"));
        }
        if (room.getRoomNumber() == null || room.getRoomNumber().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "Room number is required"));
        }

        roomService.insert(room);
        return ResponseEntity.ok(Result.success(null, "Room created successfully"));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ROOM", operation = "CREATE", description = "Bulk create rooms")
    public ResponseEntity<Result<Void>> bulkCreateRooms(@RequestBody BulkCreateRoomsRequest request) {
        User currentUser = getCurrentUser();

        if (currentUser != null && "DORM_ADMIN".equals(currentUser.getRole())) {
            Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
            if (dormAdminBuildingId == null) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin has no assigned building"));
            }
            if (request.getBuildingId() != null && !request.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin can only create rooms in the assigned building"));
            }
            request.setBuildingId(dormAdminBuildingId);
        }

        try {
            int created = roomService.bulkCreate(
                    request.getBuildingId(),
                    request.getTotalFloors(),
                    request.getRoomsPerFloor(),
                    request.getCapacity(),
                    request.getGender(),
                    request.getStatus()
            );
            return ResponseEntity.ok(Result.success(null, "Bulk created " + created + " rooms successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(Result.error(400, ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ROOM", operation = "UPDATE", description = "Update room")
    public ResponseEntity<Result<Void>> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        User currentUser = getCurrentUser();
        Room existingRoom = roomService.findById(id);

        if (currentUser != null && "DORM_ADMIN".equals(currentUser.getRole())) {
            Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
            if (dormAdminBuildingId == null) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin has no assigned building"));
            }
            if (existingRoom == null || !existingRoom.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin can only manage rooms in the assigned building"));
            }
        }

        if (existingRoom == null) {
            return ResponseEntity.ok(Result.error(404, "Room not found"));
        }
        if (!roomService.canAccommodate(existingRoom, room.getCapacity())) {
            return ResponseEntity.ok(Result.error(400, "Room capacity cannot be lower than current occupancy"));
        }

        room.setId(id);
        roomService.update(room);
        return ResponseEntity.ok(Result.success(null, "Room updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ROOM", operation = "DELETE", description = "Delete room")
    public ResponseEntity<Result<Void>> deleteRoom(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Room room = roomService.findById(id);

        if (currentUser != null && "DORM_ADMIN".equals(currentUser.getRole())) {
            Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
            if (dormAdminBuildingId == null) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin has no assigned building"));
            }
            if (room == null || !room.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "Dorm admin can only manage rooms in the assigned building"));
            }
        }

        if (room == null) {
            return ResponseEntity.ok(Result.error(404, "Room not found"));
        }
        if (room.getCurrentOccupancy() != null && room.getCurrentOccupancy() > 0) {
            return ResponseEntity.ok(Result.error(400, "Occupied rooms cannot be deleted"));
        }

        roomService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "Room deleted successfully"));
    }
}
