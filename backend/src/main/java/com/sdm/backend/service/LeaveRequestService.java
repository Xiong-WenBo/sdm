package com.sdm.backend.service;

import com.sdm.backend.entity.LeaveRequest;
import com.sdm.backend.mapper.LeaveRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveRequestService {

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    public List<LeaveRequest> findByStudentId(Long studentId) {
        return leaveRequestMapper.findByStudentId(studentId);
    }

    public List<LeaveRequest> findByCounselorId(Long counselorId) {
        return leaveRequestMapper.findByCounselorId(counselorId);
    }

    public List<LeaveRequest> findByBuildingId(Long buildingId) {
        return leaveRequestMapper.findByBuildingId(buildingId);
    }

    public List<LeaveRequest> findAll() {
        return leaveRequestMapper.findAll();
    }

    public LeaveRequest findById(Long id) {
        return leaveRequestMapper.findById(id);
    }

    @Transactional
    public int insert(LeaveRequest leaveRequest) {
        if (leaveRequest.getCreatedAt() == null) {
            leaveRequest.setCreatedAt(LocalDateTime.now());
        }
        if (leaveRequest.getStatus() == null) {
            leaveRequest.setStatus("PENDING");
        }
        return leaveRequestMapper.insert(leaveRequest);
    }

    @Transactional
    public int update(LeaveRequest leaveRequest) {
        return leaveRequestMapper.update(leaveRequest);
    }

    @Transactional
    public int deleteById(Long id) {
        return leaveRequestMapper.deleteById(id);
    }

    @Transactional
    public int approveLeave(Long id, Long approverId, String approveNote, String status) {
        LeaveRequest leaveRequest = leaveRequestMapper.findById(id);
        if (leaveRequest == null || !"PENDING".equals(leaveRequest.getStatus())) {
            return 0;
        }
        
        leaveRequest.setApproverId(approverId);
        leaveRequest.setApproveNote(approveNote);
        leaveRequest.setStatus(status);
        
        return leaveRequestMapper.update(leaveRequest);
    }

    @Transactional
    public int cancelLeave(Long id) {
        LeaveRequest leaveRequest = leaveRequestMapper.findById(id);
        if (leaveRequest == null || !"PENDING".equals(leaveRequest.getStatus())) {
            return 0;
        }
        
        leaveRequest.setStatus("CANCELED");
        return leaveRequestMapper.update(leaveRequest);
    }

    public List<LeaveRequest> findByStudentIdAndStatus(Long studentId, String status) {
        return leaveRequestMapper.findByStudentIdAndStatus(studentId, status);
    }

    public List<LeaveRequest> findByCounselorIdAndStatus(Long counselorId, String status) {
        return leaveRequestMapper.findByCounselorIdAndStatus(counselorId, status);
    }
}
