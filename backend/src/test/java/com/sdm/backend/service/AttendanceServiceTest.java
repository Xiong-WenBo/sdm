package com.sdm.backend.service;

import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.Building;
import com.sdm.backend.mapper.AttendanceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private StudentService studentService;

    @Mock
    private BuildingService buildingService;

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService();
        ReflectionTestUtils.setField(attendanceService, "attendanceMapper", attendanceMapper);
        ReflectionTestUtils.setField(attendanceService, "studentService", studentService);
        ReflectionTestUtils.setField(attendanceService, "buildingService", buildingService);
    }

    @Test
    void findByPageAndFiltersPassesCounselorIdToMapper() {
        when(attendanceMapper.findByPageAndFilters(0, 10, null, null, LocalDate.of(2026, 3, 24), "EVENING", "ABSENT", 3L))
                .thenReturn(List.of());

        List<Attendance> result = attendanceService.findByPageAndFilters(
                1, 10, null, null, LocalDate.of(2026, 3, 24), "EVENING", "ABSENT", 3L
        );

        assertEquals(0, result.size());
        verify(attendanceMapper).findByPageAndFilters(0, 10, null, null, LocalDate.of(2026, 3, 24), "EVENING", "ABSENT", 3L);
    }

    @Test
    void findAccessibleBuildingsReturnsDormAdminBuildingOnly() {
        Building building = new Building();
        building.setId(6L);
        building.setName("A栋");
        when(buildingService.findByAdminUserId(11L)).thenReturn(building);

        List<Building> result = attendanceService.findAccessibleBuildings("DORM_ADMIN", 11L);

        assertEquals(1, result.size());
        assertEquals(6L, result.getFirst().getId());
    }

    @Test
    void findStudentsForCheckInUsesCounselorScope() {
        when(attendanceMapper.findStudentsByCounselor(8L)).thenReturn(List.of(new Attendance()));

        List<Attendance> result = attendanceService.findStudentsForCheckIn("COUNSELOR", 8L, null);

        assertEquals(1, result.size());
        verify(attendanceMapper).findStudentsByCounselor(8L);
    }
}
