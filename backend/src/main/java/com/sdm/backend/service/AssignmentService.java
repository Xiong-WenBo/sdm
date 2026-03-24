package com.sdm.backend.service;

import com.sdm.backend.entity.Assignment;
import com.sdm.backend.mapper.AssignmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private RoomService roomService;

    public List<Assignment> findAll() {
        return assignmentMapper.findAll();
    }

    public List<Assignment> findByPage(int page, int size) {
        int offset = (page - 1) * size;
        return assignmentMapper.findByPage(offset, size);
    }

    public List<Assignment> findByPageAndFilters(int page, int size, Long studentId, Long roomId, Long buildingId, String status) {
        int offset = (page - 1) * size;
        return assignmentMapper.findByPageAndFilters(offset, size, studentId, roomId, buildingId, status);
    }

    public int countAll() {
        return assignmentMapper.countAll();
    }

    public int countByFilters(Long studentId, Long roomId, Long buildingId, String status) {
        return assignmentMapper.countByFilters(studentId, roomId, buildingId, status);
    }

    public Assignment findById(Long id) {
        return assignmentMapper.findById(id);
    }

    public List<Assignment> findByStudentId(Long studentId) {
        return assignmentMapper.findByStudentId(studentId);
    }

    public List<Assignment> findByRoomId(Long roomId) {
        return assignmentMapper.findByRoomId(roomId);
    }

    public List<Assignment> findByBuildingId(Long buildingId) {
        return assignmentMapper.findByBuildingId(buildingId);
    }

    @Transactional
    public int insert(Assignment assignment) {
        roomService.incrementOccupancy(assignment.getRoomId());
        return assignmentMapper.insert(assignment);
    }

    public int update(Assignment assignment) {
        return assignmentMapper.update(assignment);
    }

    @Transactional
    public int deleteById(Long id) {
        Assignment assignment = assignmentMapper.findById(id);
        if (assignment == null) {
            return 0;
        }

        if (isActive(assignment)) {
            roomService.decrementOccupancy(assignment.getRoomId());
        }
        return assignmentMapper.deleteById(id);
    }

    @Transactional
    public int checkOut(Long assignmentId, LocalDate checkOutDate) {
        Assignment assignment = assignmentMapper.findById(assignmentId);
        if (assignment == null || !isActive(assignment)) {
            return 0;
        }

        assignment.setCheckOutDate(checkOutDate);
        assignment.setStatus("INACTIVE");
        assignmentMapper.update(assignment);
        roomService.decrementOccupancy(assignment.getRoomId());

        return 1;
    }

    @Transactional
    public int transfer(Long assignmentId, Long newRoomId, String newBedNumber) {
        Assignment assignment = assignmentMapper.findById(assignmentId);
        if (assignment == null || !isActive(assignment)) {
            return 0;
        }

        Long oldRoomId = assignment.getRoomId();
        boolean sameRoom = oldRoomId != null && oldRoomId.equals(newRoomId);
        boolean sameBed = assignment.getBedNumber() != null && assignment.getBedNumber().equals(newBedNumber);
        if (sameRoom && sameBed) {
            return 0;
        }

        assignment.setRoomId(newRoomId);
        assignment.setBedNumber(newBedNumber);
        assignmentMapper.update(assignment);

        if (!sameRoom) {
            roomService.decrementOccupancy(oldRoomId);
            roomService.incrementOccupancy(newRoomId);
        }

        return 1;
    }

    public Assignment findActiveByStudentId(Long studentId) {
        return assignmentMapper.findActiveByStudentId(studentId);
    }

    public Long findBuildingIdByRoomId(Long roomId) {
        if (roomId == null) {
            return null;
        }
        com.sdm.backend.entity.Room room = roomService.findById(roomId);
        return room != null ? room.getBuildingId() : null;
    }

    private boolean isActive(Assignment assignment) {
        return "ACTIVE".equals(assignment.getStatus());
    }
}
