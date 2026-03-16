package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
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
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Result<Map<String, Object>>> getRoomList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);

        // 如果是宿管，强制使用其管理的楼栋 ID
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
            return ResponseEntity.ok(Result.error(404, "房间不存在"));
        }
        return ResponseEntity.ok(Result.success(room));
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<Result<List<Room>>> getRoomsByBuildingId(@PathVariable Long buildingId) {
        List<Room> rooms = roomService.findByBuildingId(buildingId);
        return ResponseEntity.ok(Result.success(rooms));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ROOM", operation = "CREATE", description = "新增房间")
    public ResponseEntity<Result<Void>> createRoom(@RequestBody Room room) {
        User currentUser = getCurrentUser();
        
        // 如果是宿管，验证楼栋权限
        if (currentUser != null && "DORM_ADMIN".equals(currentUser.getRole())) {
            Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
            if (dormAdminBuildingId == null) {
                return ResponseEntity.ok(Result.error(403, "宿管未分配楼栋，无法创建房间"));
            }
            if (room.getBuildingId() != null && !room.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "宿管只能在自己管理的楼栋创建房间"));
            }
            // 强制设置楼栋 ID 为宿管管理的楼栋
            room.setBuildingId(dormAdminBuildingId);
        }
        
        if (room.getBuildingId() == null) {
            return ResponseEntity.ok(Result.error(400, "所属楼栋不能为空"));
        }
        if (room.getRoomNumber() == null || room.getRoomNumber().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "房间号不能为空"));
        }
        
        roomService.insert(room);
        return ResponseEntity.ok(Result.success(null, "房间创建成功"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ROOM", operation = "UPDATE", description = "修改房间信息")
    public ResponseEntity<Result<Void>> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        User currentUser = getCurrentUser();
        
        // 如果是宿管，验证楼栋权限
        if (currentUser != null && "DORM_ADMIN".equals(currentUser.getRole())) {
            Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
            if (dormAdminBuildingId == null) {
                return ResponseEntity.ok(Result.error(403, "宿管未分配楼栋，无法操作"));
            }
            
            // 验证房间是否属于宿管管理的楼栋
            Room existingRoom = roomService.findById(id);
            if (existingRoom == null || !existingRoom.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "宿管只能操作自己管理的楼栋的房间"));
            }
        }
        
        Room existingRoom = roomService.findById(id);
        if (existingRoom == null) {
            return ResponseEntity.ok(Result.error(404, "房间不存在"));
        }
        
        room.setId(id);
        roomService.update(room);
        return ResponseEntity.ok(Result.success(null, "房间更新成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    @Log(module = "ROOM", operation = "DELETE", description = "删除房间")
    public ResponseEntity<Result<Void>> deleteRoom(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        
        // 如果是宿管，验证楼栋权限
        if (currentUser != null && "DORM_ADMIN".equals(currentUser.getRole())) {
            Long dormAdminBuildingId = getDormAdminBuildingId(currentUser);
            if (dormAdminBuildingId == null) {
                return ResponseEntity.ok(Result.error(403, "宿管未分配楼栋，无法操作"));
            }
            
            // 验证房间是否属于宿管管理的楼栋
            Room existingRoom = roomService.findById(id);
            if (existingRoom == null || !existingRoom.getBuildingId().equals(dormAdminBuildingId)) {
                return ResponseEntity.ok(Result.error(403, "宿管只能操作自己管理的楼栋的房间"));
            }
        }
        
        Room room = roomService.findById(id);
        if (room == null) {
            return ResponseEntity.ok(Result.error(404, "房间不存在"));
        }
        roomService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "房间删除成功"));
    }
}
