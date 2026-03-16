package com.sdm.backend.service;

import com.sdm.backend.entity.Repair;
import com.sdm.backend.entity.User;
import com.sdm.backend.mapper.RepairMapper;
import com.sdm.backend.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepairService {

    @Autowired
    private RepairMapper repairMapper;

    @Autowired
    private StudentMapper studentMapper;

    public List<Repair> findByStudentId(Long studentId) {
        return repairMapper.findByStudentId(studentId);
    }

    public List<Repair> findByBuildingId(Long buildingId) {
        return repairMapper.findByBuildingId(buildingId);
    }

    public List<Repair> findByStudentIdAndStatus(Long studentId, String status) {
        return repairMapper.findByStudentIdAndStatus(studentId, status);
    }

    public List<Repair> findByBuildingIdAndStatus(Long buildingId, String status) {
        return repairMapper.findByBuildingIdAndStatus(buildingId, status);
    }

    public List<Repair> findAll() {
        return repairMapper.findAll();
    }

    public Repair findById(Long id) {
        return repairMapper.findById(id);
    }

    @Transactional
    public int insert(Repair repair) {
        if (repair.getCreatedAt() == null) {
            repair.setCreatedAt(LocalDateTime.now());
        }
        if (repair.getStatus() == null) {
            repair.setStatus("PENDING");
        }
        return repairMapper.insert(repair);
    }

    @Transactional
    public int update(Repair repair) {
        return repairMapper.update(repair);
    }

    @Transactional
    public int deleteById(Long id) {
        return repairMapper.deleteById(id);
    }

    @Transactional
    public int handleRepair(Long id, Long adminId, String handleNote, String status) {
        Repair repair = repairMapper.findById(id);
        if (repair == null) {
            return 0;
        }
        
        repair.setAdminId(adminId);
        repair.setHandleNote(handleNote);
        repair.setHandleTime(LocalDateTime.now());
        repair.setStatus(status);
        
        return repairMapper.update(repair);
    }

    @Transactional
    public int cancelRepair(Long id) {
        Repair repair = repairMapper.findById(id);
        if (repair == null || !"PENDING".equals(repair.getStatus())) {
            return 0;
        }
        
        repair.setStatus("REJECTED");
        return repairMapper.update(repair);
    }
}
