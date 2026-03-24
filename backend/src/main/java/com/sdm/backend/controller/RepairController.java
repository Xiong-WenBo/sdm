package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Assignment;
import com.sdm.backend.entity.Repair;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AssignmentService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.RepairService;
import com.sdm.backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair")
public class RepairController {

    @Autowired
    private RepairService repairService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private AssignmentService assignmentService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) principal;
            return studentService.findByUsername(username);
        }
        return null;
    }

    @GetMapping("/my/list")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Result<List<Repair>>> getMyRepairs(
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
        
        List<Repair> repairs;
        if (status != null && !status.isEmpty()) {
            repairs = repairService.findByStudentIdAndStatus(studentId, status);
        } else {
            repairs = repairService.findByStudentId(studentId);
        }
        
        return ResponseEntity.ok(Result.success(repairs));
    }

    @GetMapping("/building/list")
    @PreAuthorize("hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<Repair>>> getBuildingRepairs(
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        
        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            List<Repair> repairs;
            if (status != null && !status.isEmpty()) {
                repairs = repairService.findAll().stream()
                    .filter(repair -> status.equals(repair.getStatus()))
                    .toList();
            } else {
                repairs = repairService.findAll();
            }
            return ResponseEntity.ok(Result.success(repairs));
        }
        
        com.sdm.backend.entity.Building building = buildingService.findByAdminUserId(currentUser.getId());
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "未找到您管理的楼栋"));
        }
        
        List<Repair> repairs;
        if (status != null && !status.isEmpty()) {
            repairs = repairService.findByBuildingIdAndStatus(building.getId(), status);
        } else {
            repairs = repairService.findByBuildingId(building.getId());
        }
        
        return ResponseEntity.ok(Result.success(repairs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Repair>> getRepairById(@PathVariable Long id) {
        Repair repair = repairService.findById(id);
        if (repair == null) {
            return ResponseEntity.ok(Result.error(404, "报修记录不存在"));
        }
        
        User currentUser = getCurrentUser();
        
        if ("STUDENT".equals(currentUser.getRole())) {
            Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
            if (!repair.getStudentId().equals(studentId)) {
                return ResponseEntity.ok(Result.error(403, "无权查看此报修记录"));
            }
        } else if ("DORM_ADMIN".equals(currentUser.getRole())) {
            com.sdm.backend.entity.Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null || !repair.getBuildingId().equals(building.getId())) {
                return ResponseEntity.ok(Result.error(403, "无权查看此报修记录"));
            }
        }
        
        return ResponseEntity.ok(Result.success(repair));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Log(module = "REPAIR", operation = "CREATE", description = "提交报修")
    public ResponseEntity<Result<Long>> submitRepair(@RequestBody Repair repair) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
        
        com.sdm.backend.entity.Student student = studentService.findById(studentId);
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "未找到学生信息"));
        }
        
        // 自动获取学生当前住宿的房间
        Assignment assignment = assignmentService.findActiveByStudentId(studentId);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(400, "您尚未分配宿舍，无法提交报修"));
        }
        
        repair.setStudentId(studentId);
        repair.setRoomId(assignment.getRoomId());
        repair.setStatus("PENDING");
        
        int result = repairService.insert(repair);
        if (result > 0) {
            return ResponseEntity.ok(Result.success(repair.getId(), "报修提交成功"));
        } else {
            return ResponseEntity.ok(Result.error(500, "报修提交失败"));
        }
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    @Log(module = "REPAIR", operation = "UPDATE", description = "处理报修")
    public ResponseEntity<Result<Void>> handleRepair(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String handleNote
    ) {
        User currentUser = getCurrentUser();
        
        Repair repair = repairService.findById(id);
        if (repair == null) {
            return ResponseEntity.ok(Result.error(404, "报修记录不存在"));
        }
        
        if ("DORM_ADMIN".equals(currentUser.getRole())) {
            com.sdm.backend.entity.Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null || !repair.getBuildingId().equals(building.getId())) {
                return ResponseEntity.ok(Result.error(403, "无权处理此报修记录"));
            }
        }
        
        repairService.handleRepair(id, currentUser.getId(), handleNote, status);
        return ResponseEntity.ok(Result.success(null, "处理成功"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Log(module = "REPAIR", operation = "UPDATE", description = "取消报修")
    public ResponseEntity<Result<Void>> cancelRepair(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
        
        Repair repair = repairService.findById(id);
        if (repair == null) {
            return ResponseEntity.ok(Result.error(404, "报修记录不存在"));
        }
        
        if (!repair.getStudentId().equals(studentId)) {
            return ResponseEntity.ok(Result.error(403, "无权取消此报修记录"));
        }
        
        if (!"PENDING".equals(repair.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "只能取消待处理的报修"));
        }
        
        repairService.cancelRepair(id);
        return ResponseEntity.ok(Result.success(null, "取消成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "REPAIR", operation = "DELETE", description = "删除报修记录")
    public ResponseEntity<Result<Void>> deleteRepair(@PathVariable Long id) {
        repairService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "删除成功"));
    }
}
