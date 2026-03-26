package com.sdm.backend.service;

import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.Building;
import com.sdm.backend.mapper.AttendanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private BuildingService buildingService;

    public List<Attendance> findAll() {
        return attendanceMapper.findAll();
    }

    public List<Attendance> findByPage(int page, int size) {
        int offset = (page - 1) * size;
        return attendanceMapper.findByPage(offset, size);
    }

    public List<Attendance> findByPageAndFilters(int page, int size, Long studentId, Long buildingId,
                                                 LocalDate checkDate, String checkTime, String status,
                                                 Long counselorId) {
        int offset = (page - 1) * size;
        return attendanceMapper.findByPageAndFilters(offset, size, studentId, buildingId, checkDate, checkTime, status, counselorId);
    }

    public int countAll() {
        return attendanceMapper.countAll();
    }

    public int countByFilters(Long studentId, Long buildingId, LocalDate checkDate, String checkTime, String status,
                              Long counselorId) {
        return attendanceMapper.countByFilters(studentId, buildingId, checkDate, checkTime, status, counselorId);
    }

    public Attendance findById(Long id) {
        return attendanceMapper.findById(id);
    }

    public Attendance findByStudentAndDate(Long studentId, LocalDate checkDate, String checkTime) {
        return attendanceMapper.findByStudentAndDate(studentId, checkDate, checkTime);
    }

    @Transactional
    public int insert(Attendance attendance) {
        return attendanceMapper.insert(attendance);
    }

    @Transactional
    public int batchInsert(List<Attendance> list) {
        return attendanceMapper.batchInsert(list);
    }

    public int update(Attendance attendance) {
        return attendanceMapper.update(attendance);
    }

    @Transactional
    public int deleteById(Long id) {
        return attendanceMapper.deleteById(id);
    }

    public List<Attendance> findStudentsInBuilding(Long buildingId) {
        return attendanceMapper.findStudentsInBuilding(buildingId);
    }

    public List<Attendance> findStudentsByCounselor(Long counselorId) {
        return attendanceMapper.findStudentsByCounselor(counselorId);
    }

    public List<Building> findAccessibleBuildings(String role, Long userId) {
        if ("DORM_ADMIN".equals(role)) {
            Building building = buildingService.findByAdminUserId(userId);
            return building == null ? List.of() : List.of(building);
        }

        if ("SUPER_ADMIN".equals(role)) {
            return buildingService.findAll();
        }

        return List.of();
    }

    public List<Attendance> findStudentsForCheckIn(String role, Long userId, Long buildingId) {
        if ("COUNSELOR".equals(role)) {
            return attendanceMapper.findStudentsByCounselor(userId);
        }

        if (buildingId == null) {
            return List.of();
        }

        return attendanceMapper.findStudentsInBuilding(buildingId);
    }
}
