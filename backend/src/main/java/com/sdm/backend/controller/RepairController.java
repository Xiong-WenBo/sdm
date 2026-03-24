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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        if (principal instanceof String username) {
            return studentService.findByUsername(username);
        }
        return null;
    }

    @GetMapping("/my/list")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Result<List<Repair>>> getMyRepairs(@RequestParam(required = false) String status) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());

        List<Repair> repairs = (status != null && !status.isEmpty())
                ? repairService.findByStudentIdAndStatus(studentId, status)
                : repairService.findByStudentId(studentId);

        return ResponseEntity.ok(Result.success(repairs));
    }

    @GetMapping("/building/list")
    @PreAuthorize("hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<Repair>>> getBuildingRepairs(@RequestParam(required = false) String status) {
        User currentUser = getCurrentUser();

        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            List<Repair> repairs = (status != null && !status.isEmpty())
                    ? repairService.findAllByStatus(status)
                    : repairService.findAll();
            return ResponseEntity.ok(Result.success(repairs));
        }

        com.sdm.backend.entity.Building building = buildingService.findByAdminUserId(currentUser.getId());
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "Managed building not found"));
        }

        List<Repair> repairs = (status != null && !status.isEmpty())
                ? repairService.findByBuildingIdAndStatus(building.getId(), status)
                : repairService.findByBuildingId(building.getId());

        return ResponseEntity.ok(Result.success(repairs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Repair>> getRepairById(@PathVariable Long id) {
        Repair repair = repairService.findById(id);
        if (repair == null) {
            return ResponseEntity.ok(Result.error(404, "Repair record not found"));
        }

        User currentUser = getCurrentUser();
        if ("STUDENT".equals(currentUser.getRole())) {
            Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
            if (!repair.getStudentId().equals(studentId)) {
                return ResponseEntity.ok(Result.error(403, "Access denied"));
            }
        } else if ("DORM_ADMIN".equals(currentUser.getRole())) {
            com.sdm.backend.entity.Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null || !repair.getBuildingId().equals(building.getId())) {
                return ResponseEntity.ok(Result.error(403, "Access denied"));
            }
        }

        return ResponseEntity.ok(Result.success(repair));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Log(module = "REPAIR", operation = "CREATE", description = "Create repair request")
    public ResponseEntity<Result<Long>> submitRepair(@RequestBody Repair repair) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());

        com.sdm.backend.entity.Student student = studentService.findById(studentId);
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "Student not found"));
        }

        Assignment assignment = assignmentService.findActiveByStudentId(studentId);
        if (assignment == null) {
            return ResponseEntity.ok(Result.error(400, "Dorm assignment is required before submitting a repair"));
        }

        repair.setStudentId(studentId);
        repair.setRoomId(assignment.getRoomId());
        repair.setStatus("PENDING");

        int result = repairService.insert(repair);
        if (result > 0) {
            return ResponseEntity.ok(Result.success(repair.getId(), "Repair submitted successfully"));
        }
        return ResponseEntity.ok(Result.error(500, "Repair submission failed"));
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    @Log(module = "REPAIR", operation = "UPDATE", description = "Handle repair request")
    public ResponseEntity<Result<Void>> handleRepair(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String handleNote
    ) {
        User currentUser = getCurrentUser();
        Repair repair = repairService.findById(id);
        if (repair == null) {
            return ResponseEntity.ok(Result.error(404, "Repair record not found"));
        }

        if ("DORM_ADMIN".equals(currentUser.getRole())) {
            com.sdm.backend.entity.Building building = buildingService.findByAdminUserId(currentUser.getId());
            if (building == null || !repair.getBuildingId().equals(building.getId())) {
                return ResponseEntity.ok(Result.error(403, "Access denied"));
            }
        }

        if (!"PENDING".equals(repair.getStatus()) && !"PROCESSING".equals(repair.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "Only pending or processing repairs can be handled"));
        }
        if (!"PROCESSING".equals(status) && !"COMPLETED".equals(status) && !"REJECTED".equals(status)) {
            return ResponseEntity.ok(Result.error(400, "Invalid repair status"));
        }

        int updated = repairService.handleRepair(id, currentUser.getId(), handleNote, status);
        if (updated == 0) {
            return ResponseEntity.ok(Result.error(400, "Repair state update was rejected"));
        }
        return ResponseEntity.ok(Result.success(null, "Repair updated successfully"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Log(module = "REPAIR", operation = "UPDATE", description = "Cancel repair request")
    public ResponseEntity<Result<Void>> cancelRepair(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());

        Repair repair = repairService.findById(id);
        if (repair == null) {
            return ResponseEntity.ok(Result.error(404, "Repair record not found"));
        }
        if (!repair.getStudentId().equals(studentId)) {
            return ResponseEntity.ok(Result.error(403, "Access denied"));
        }
        if (!"PENDING".equals(repair.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "Only pending repairs can be canceled"));
        }

        repairService.cancelRepair(id);
        return ResponseEntity.ok(Result.success(null, "Canceled successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "REPAIR", operation = "DELETE", description = "Delete repair record")
    public ResponseEntity<Result<Void>> deleteRepair(@PathVariable Long id) {
        repairService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "Deleted successfully"));
    }
}
