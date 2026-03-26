package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AttendanceService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private BuildingService buildingService;

    @Mock
    private UserService userService;

    private AttendanceController controller;

    @BeforeEach
    void setUp() {
        controller = new AttendanceController();
        ReflectionTestUtils.setField(controller, "attendanceService", attendanceService);
        ReflectionTestUtils.setField(controller, "buildingService", buildingService);
        ReflectionTestUtils.setField(controller, "userService", userService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUsesCounselorFilterForCounselorRole() {
        User counselor = new User();
        counselor.setId(9L);
        counselor.setUsername("counselor01");
        counselor.setRole("COUNSELOR");
        when(userService.findByUsername("counselor01")).thenReturn(counselor);
        when(attendanceService.findByPageAndFilters(1, 10, null, null, null, null, null, 9L)).thenReturn(List.of());
        when(attendanceService.countByFilters(null, null, null, null, null, 9L)).thenReturn(0);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("counselor01", null, List.of())
        );

        ResponseEntity<Result<Map<String, Object>>> response = controller.getAttendanceList(1, 10, null, null, null, null, null);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        verify(attendanceService).findByPageAndFilters(1, 10, null, null, null, null, null, 9L);
        verify(attendanceService).countByFilters(null, null, null, null, null, 9L);
    }

    @Test
    void getStudentsForCheckInRequiresBuildingForAdminRoles() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("super01");
        admin.setRole("SUPER_ADMIN");
        when(userService.findByUsername("super01")).thenReturn(admin);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("super01", null, List.of())
        );

        ResponseEntity<Result<List<Attendance>>> response = controller.getStudentsForCheckIn(null);

        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    void absentTodayUsesCounselorFilter() {
        User counselor = new User();
        counselor.setId(7L);
        counselor.setUsername("counselor02");
        counselor.setRole("COUNSELOR");
        when(userService.findByUsername("counselor02")).thenReturn(counselor);
        when(attendanceService.findByPageAndFilters(1, 1000, null, null, LocalDate.of(2026, 3, 24), "EVENING", "ABSENT", 7L))
                .thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("counselor02", null, List.of())
        );

        ResponseEntity<Result<List<Attendance>>> response =
                controller.getAbsentStudentsToday(LocalDate.of(2026, 3, 24));

        assertEquals(200, response.getBody().getCode());
        verify(attendanceService).findByPageAndFilters(1, 1000, null, null, LocalDate.of(2026, 3, 24), "EVENING", "ABSENT", 7L);
    }
}
