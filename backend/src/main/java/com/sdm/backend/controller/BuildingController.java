package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
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
@RequestMapping("/api/building")
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private RoomService roomService;

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

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> getBuildingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name
    ) {
        List<Building> buildings;
        int total;

        if (name != null && !name.isEmpty()) {
            buildings = buildingService.findByPageAndFilters(page, size, name);
            total = buildingService.countByFilters(name);
        } else {
            buildings = buildingService.findByPage(page, size);
            total = buildingService.countAll();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", buildings);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/accessible")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DORM_ADMIN')")
    public ResponseEntity<Result<List<Building>>> getAccessibleBuildings() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(Result.success(
                buildingService.findAccessibleBuildings(currentUser.getRole(), currentUser.getId())
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Building>> getBuildingById(@PathVariable Long id) {
        Building building = buildingService.findById(id);
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "Building not found"));
        }
        return ResponseEntity.ok(Result.success(building));
    }

    @GetMapping("/available-admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<Building>>> getAvailableAdmins() {
        return ResponseEntity.ok(Result.success(buildingService.findAvailableAdmins()));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "BUILDING", operation = "CREATE", description = "Create building")
    public ResponseEntity<Result<Void>> createBuilding(@RequestBody Building building) {
        if (building.getName() == null || building.getName().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "Building name is required"));
        }

        if (building.getAdminId() != null) {
            Building existingBuilding = buildingService.findByAdminId(building.getAdminId());
            if (existingBuilding != null) {
                return ResponseEntity.ok(Result.error(400, "Admin is already assigned to another building"));
            }
        }

        buildingService.insert(building);
        return ResponseEntity.ok(Result.success(null, "Building created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "BUILDING", operation = "UPDATE", description = "Update building")
    public ResponseEntity<Result<Void>> updateBuilding(@PathVariable Long id, @RequestBody Building building) {
        Building existingBuilding = buildingService.findById(id);
        if (existingBuilding == null) {
            return ResponseEntity.ok(Result.error(404, "Building not found"));
        }

        if (building.getAdminId() != null) {
            Building assignedBuilding = buildingService.findByAdminId(building.getAdminId());
            if (assignedBuilding != null && !assignedBuilding.getId().equals(id)) {
                return ResponseEntity.ok(Result.error(400, "Admin is already assigned to another building"));
            }
        }

        building.setId(id);
        buildingService.update(building);
        return ResponseEntity.ok(Result.success(null, "Building updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "BUILDING", operation = "DELETE", description = "Delete building")
    public ResponseEntity<Result<Void>> deleteBuilding(@PathVariable Long id) {
        Building building = buildingService.findById(id);
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "Building not found"));
        }
        if (roomService.countByBuildingId(id) > 0) {
            return ResponseEntity.ok(Result.error(400, "Cannot delete a building that still has rooms"));
        }

        buildingService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "Building deleted successfully"));
    }
}
