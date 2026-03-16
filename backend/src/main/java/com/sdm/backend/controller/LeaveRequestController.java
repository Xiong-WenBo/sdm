package com.sdm.backend.controller;

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
import org.springframework.web.bind.annotation.*;

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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) principal;
            return studentService.findByUsername(username);
        }
        return null;
    }

    @GetMapping("/my/list")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Result<List<LeaveRequest>>> getMyLeaves(
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
        
        List<LeaveRequest> leaves;
        if (status != null && !status.isEmpty()) {
            leaves = leaveRequestService.findByStudentIdAndStatus(studentId, status);
        } else {
            leaves = leaveRequestService.findByStudentId(studentId);
        }
        
        return ResponseEntity.ok(Result.success(leaves));
    }

    @GetMapping("/counselor/list")
    @PreAuthorize("hasRole('COUNSELOR') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<LeaveRequest>>> getCounselorLeaves(
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        
        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            List<LeaveRequest> leaves = leaveRequestService.findAll();
            return ResponseEntity.ok(Result.success(leaves));
        }
        
        List<LeaveRequest> leaves;
        if (status != null && !status.isEmpty()) {
            leaves = leaveRequestService.findByCounselorIdAndStatus(currentUser.getId(), status);
        } else {
            leaves = leaveRequestService.findByCounselorId(currentUser.getId());
        }
        
        return ResponseEntity.ok(Result.success(leaves));
    }

    @GetMapping("/building/list")
    @PreAuthorize("hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<List<LeaveRequest>>> getBuildingLeaves(
            @RequestParam(required = false) String status
    ) {
        User currentUser = getCurrentUser();
        
        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            List<LeaveRequest> leaves = leaveRequestService.findAll();
            return ResponseEntity.ok(Result.success(leaves));
        }
        
        Building building = buildingService.findByAdminUserId(currentUser.getId());
        if (building == null) {
            return ResponseEntity.ok(Result.error(404, "未找到您管理的楼栋"));
        }
        
        List<LeaveRequest> leaves = leaveRequestService.findByBuildingId(building.getId());
        
        if (status != null && !status.isEmpty()) {
            leaves = leaves.stream()
                .filter(l -> status.equals(l.getStatus()))
                .toList();
        }
        
        return ResponseEntity.ok(Result.success(leaves));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COUNSELOR') or hasRole('DORM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<LeaveRequest>> getLeaveById(@PathVariable Long id) {
        LeaveRequest leaveRequest = leaveRequestService.findById(id);
        if (leaveRequest == null) {
            return ResponseEntity.ok(Result.error(404, "请假记录不存在"));
        }
        
        User currentUser = getCurrentUser();
        
        if ("STUDENT".equals(currentUser.getRole())) {
            Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
            if (!leaveRequest.getStudentId().equals(studentId)) {
                return ResponseEntity.ok(Result.error(403, "无权查看此请假记录"));
            }
        } else if ("COUNSELOR".equals(currentUser.getRole())) {
            if (!currentUser.getId().equals(leaveRequest.getApproverId()) && 
                !"PENDING".equals(leaveRequest.getStatus())) {
                return ResponseEntity.ok(Result.error(403, "无权查看此请假记录"));
            }
        }
        
        return ResponseEntity.ok(Result.success(leaveRequest));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Result<Long>> submitLeave(@RequestBody LeaveRequest leaveRequest) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
        
        leaveRequest.setStudentId(studentId);
        leaveRequest.setStatus("PENDING");
        
        int result = leaveRequestService.insert(leaveRequest);
        if (result > 0) {
            return ResponseEntity.ok(Result.success(leaveRequest.getId(), "请假提交成功"));
        } else {
            return ResponseEntity.ok(Result.error(500, "请假提交失败"));
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<Result<Void>> approveLeave(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String approveNote
    ) {
        User currentUser = getCurrentUser();
        
        LeaveRequest leaveRequest = leaveRequestService.findById(id);
        if (leaveRequest == null) {
            return ResponseEntity.ok(Result.error(404, "请假记录不存在"));
        }
        
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "只能审批待处理的请假"));
        }
        
        leaveRequestService.approveLeave(id, currentUser.getId(), approveNote, status);
        return ResponseEntity.ok(Result.success(null, "审批成功"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Result<Void>> cancelLeave(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Long studentId = studentService.getStudentIdByUserId(currentUser.getId());
        
        LeaveRequest leaveRequest = leaveRequestService.findById(id);
        if (leaveRequest == null) {
            return ResponseEntity.ok(Result.error(404, "请假记录不存在"));
        }
        
        if (!leaveRequest.getStudentId().equals(studentId)) {
            return ResponseEntity.ok(Result.error(403, "无权取消此请假记录"));
        }
        
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "只能取消待审批的请假"));
        }
        
        leaveRequestService.cancelLeave(id);
        return ResponseEntity.ok(Result.success(null, "取消成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Result<Void>> deleteLeave(@PathVariable Long id) {
        leaveRequestService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "删除成功"));
    }
}
