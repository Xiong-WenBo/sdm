package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.LeaveRequest;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.LeaveRequestService;
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
@RequestMapping("/api/leave")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private BuildingService buildingService;

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String username) {
            return studentService.findByUsername(username);
        }
        return null;
    }

    @GetMapping("/my/list")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Result<List<LeaveRequest>>> getMyLeaves(@RequestParam(required = false) String status) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());

        List<LeaveRequest> leaves = (status != null && !status.isEmpty())
                ? leaveRequestService.findByStudentIdAndStatus(studentId, status)
                : leaveRequestService.findByStudentId(studentId);

        return ResponseEntity.ok(Result.success(leaves));
    }

    @GetMapping("/counselor/list")
    @PreAuthorize("hasRole('COUNSELOR') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<LeaveRequest>>> getCounselorLeaves(@RequestParam(required = false) String status) {
        User currentUser = getCurrentUser();

        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.success(leaveRequestService.findAll()));
        }

        List<LeaveRequest> leaves = (status != null && !status.isEmpty())
                ? leaveRequestService.findByCounselorIdAndStatus(currentUser.getId(), status)
                : leaveRequestService.findByCounselorId(currentUser.getId());

        return ResponseEntity.ok(Result.success(leaves));
    }

    @GetMapping("/building/list")
    @PreAuthorize("hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<LeaveRequest>>> getBuildingLeaves(@RequestParam(required = false) String status) {
        User currentUser = getCurrentUser();

        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.ok(Result.success(leaveRequestService.findAll()));
        }

        Building building = buildingService.findByAdminUserId(currentUser.getId());
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "Assigned building not found"));
        }

        List<LeaveRequest> leaves = leaveRequestService.findByBuildingId(building.getId());
        if (status != null && !status.isEmpty()) {
            leaves = leaves.stream().filter(leave -> status.equals(leave.getStatus())).toList();
        }

        return ResponseEntity.ok(Result.success(leaves));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COUNSELOR') or hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<LeaveRequest>> getLeaveById(@PathVariable Long id) {
        LeaveRequest leaveRequest = leaveRequestService.findById(id);
        if (leaveRequest == null) {
            return ResponseEntity.ok(Result.error(404, "Leave request not found"));
        }

        User currentUser = getCurrentUser();

        if ("STUDENT".equals(currentUser.getRole())) {
            Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
            if (!leaveRequest.getStudentId().equals(studentId)) {
                return ResponseEntity.ok(Result.error(403, "Cannot view this leave request"));
            }
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            Long counselorId = studentService.getCounselorIdByStudentId(leaveRequest.getStudentId());
            if (counselorId == null || !counselorId.equals(currentUser.getId())) {
                return ResponseEntity.ok(Result.error(403, "Cannot view this leave request"));
            }
        }

        return ResponseEntity.ok(Result.success(leaveRequest));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Log(module = "LEAVE", operation = "CREATE", description = "Submit leave request")
    public ResponseEntity<Result<Long>> submitLeave(@RequestBody LeaveRequest leaveRequest) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());

        leaveRequest.setStudentId(studentId);
        leaveRequest.setStatus("PENDING");

        int result = leaveRequestService.insert(leaveRequest);
        if (result > 0) {
            return ResponseEntity.ok(Result.success(leaveRequest.getId(), "Leave request submitted successfully"));
        }
        return ResponseEntity.ok(Result.error(500, "Leave request submission failed"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('COUNSELOR')")
    @Log(module = "LEAVE", operation = "UPDATE", description = "Approve leave request")
    public ResponseEntity<Result<Void>> approveLeave(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String approveNote
    ) {
        User currentUser = getCurrentUser();

        LeaveRequest leaveRequest = leaveRequestService.findById(id);
        if (leaveRequest == null) {
            return ResponseEntity.ok(Result.error(404, "Leave request not found"));
        }
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "Only pending leave requests can be approved"));
        }

        Long counselorId = studentService.getCounselorIdByStudentId(leaveRequest.getStudentId());
        if (counselorId == null || !counselorId.equals(currentUser.getId())) {
            return ResponseEntity.ok(Result.error(403, "Cannot approve leave requests outside your students"));
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            return ResponseEntity.ok(Result.error(400, "Invalid approval status"));
        }

        leaveRequestService.approveLeave(id, currentUser.getId(), approveNote, status);
        return ResponseEntity.ok(Result.success(null, "Leave request processed successfully"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Log(module = "LEAVE", operation = "UPDATE", description = "Cancel leave request")
    public ResponseEntity<Result<Void>> cancelLeave(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());

        LeaveRequest leaveRequest = leaveRequestService.findById(id);
        if (leaveRequest == null) {
            return ResponseEntity.ok(Result.error(404, "Leave request not found"));
        }
        if (!leaveRequest.getStudentId().equals(studentId)) {
            return ResponseEntity.ok(Result.error(403, "Cannot cancel this leave request"));
        }
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "Only pending leave requests can be canceled"));
        }

        leaveRequestService.cancelLeave(id);
        return ResponseEntity.ok(Result.success(null, "Leave request canceled successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "LEAVE", operation = "DELETE", description = "Delete leave request")
    public ResponseEntity<Result<Void>> deleteLeave(@PathVariable Long id) {
        leaveRequestService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "Leave request deleted successfully"));
    }
}
