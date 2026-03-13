package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
import com.sdm.backend.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/building")
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Building>> getBuildingById(@PathVariable Long id) {
        Building building = buildingService.findById(id);
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "楼栋不存在"));
        }
        return ResponseEntity.ok(Result.success(building));
    }

    @GetMapping("/available-admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<Building>>> getAvailableAdmins() {
        List<Building> admins = buildingService.findAvailableAdmins();
        return ResponseEntity.ok(Result.success(admins));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Void>> createBuilding(@RequestBody Building building) {
        if (building.getName() == null || building.getName().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "楼栋名称不能为空"));
        }
        
        // 检查管理员是否已被分配
        if (building.getAdminId() != null) {
            Building existingBuilding = buildingService.findByAdminId(building.getAdminId());
            if (existingBuilding != null) {
                return ResponseEntity.ok(Result.error(400, "该管理员已被分配到其他楼栋"));
            }
        }
        
        buildingService.insert(building);
        return ResponseEntity.ok(Result.success(null, "楼栋创建成功"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Void>> updateBuilding(@PathVariable Long id, @RequestBody Building building) {
        Building existingBuilding = buildingService.findById(id);
        if (existingBuilding == null) {
            return ResponseEntity.ok(Result.error(404, "楼栋不存在"));
        }
        
        // 检查管理员是否已被分配（排除当前楼栋）
        if (building.getAdminId() != null) {
            Building assignedBuilding = buildingService.findByAdminId(building.getAdminId());
            if (assignedBuilding != null && !assignedBuilding.getId().equals(id)) {
                return ResponseEntity.ok(Result.error(400, "该管理员已被分配到其他楼栋"));
            }
        }
        
        building.setId(id);
        buildingService.update(building);
        return ResponseEntity.ok(Result.success(null, "楼栋更新成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Void>> deleteBuilding(@PathVariable Long id) {
        Building building = buildingService.findById(id);
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "楼栋不存在"));
        }
        buildingService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "楼栋删除成功"));
    }
}
