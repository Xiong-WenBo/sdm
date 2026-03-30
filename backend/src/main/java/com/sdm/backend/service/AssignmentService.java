package com.sdm.backend.service;

import com.sdm.backend.entity.Assignment;
import com.sdm.backend.entity.Room;
import com.sdm.backend.entity.Student;
import com.sdm.backend.mapper.AssignmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private RoomService roomService;

    @Autowired
    private StudentService studentService;

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

    @Transactional
    public Map<String, Object> bulkAutoAssign(Long buildingId, LocalDate checkInDate, Long createdBy) {
        if (buildingId == null) {
            throw new IllegalArgumentException("Building is required");
        }

        List<Student> candidates = studentService.findUnassignedForDormAssignment();
        List<Room> rooms = roomService.findByBuildingId(buildingId).stream()
                .filter(room -> room.getCapacity() != null && room.getCapacity() > 0)
                .filter(room -> room.getCurrentOccupancy() == null || room.getCurrentOccupancy() == 0)
                .filter(room -> "AVAILABLE".equals(room.getStatus()))
                .sorted(Comparator
                        .comparing((Room room) -> room.getFloor() == null ? Integer.MAX_VALUE : room.getFloor())
                        .thenComparing(room -> room.getRoomNumber() == null ? "" : room.getRoomNumber()))
                .toList();

        Map<String, List<Student>> groupedStudents = new LinkedHashMap<>();
        for (Student student : candidates) {
            String key = (student.getClassName() == null ? "" : student.getClassName().trim())
                    + "||"
                    + (student.getMajor() == null ? "" : student.getMajor().trim());
            groupedStudents.computeIfAbsent(key, ignored -> new ArrayList<>()).add(student);
        }

        int roomIndex = 0;
        int assignedCount = 0;
        int usedRooms = 0;
        LocalDate assignmentDate = checkInDate != null ? checkInDate : LocalDate.now();

        outer:
        for (List<Student> group : groupedStudents.values()) {
            if (roomIndex >= rooms.size()) {
                break;
            }

            Room currentRoom = rooms.get(roomIndex);
            usedRooms++;
            int nextBed = 1;

            for (Student student : group) {
                if (nextBed > currentRoom.getCapacity()) {
                    roomIndex++;
                    if (roomIndex >= rooms.size()) {
                        break outer;
                    }
                    currentRoom = rooms.get(roomIndex);
                    usedRooms++;
                    nextBed = 1;
                }

                Assignment assignment = new Assignment();
                assignment.setStudentId(student.getId());
                assignment.setRoomId(currentRoom.getId());
                assignment.setBedNumber("A" + nextBed);
                assignment.setCheckInDate(assignmentDate);
                assignment.setStatus("ACTIVE");
                assignment.setCreatedBy(createdBy);

                roomService.incrementOccupancy(currentRoom.getId());
                assignmentMapper.insert(assignment);
                assignedCount++;
                nextBed++;
            }

            roomIndex++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidateCount", candidates.size());
        result.put("assignedCount", assignedCount);
        result.put("unassignedCount", candidates.size() - assignedCount);
        result.put("usedRooms", usedRooms);
        return result;
    }
}
