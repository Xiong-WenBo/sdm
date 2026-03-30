package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.LeaveRequest;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.LeaveRequestService;
import com.sdm.backend.service.StudentService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestControllerTest {

    @Mock
    private LeaveRequestService leaveRequestService;

    @Mock
    private StudentService studentService;

    @Mock
    private BuildingService buildingService;

    private LeaveRequestController controller;

    @BeforeEach
    void setUp() {
        controller = new LeaveRequestController();
        ReflectionTestUtils.setField(controller, "leaveRequestService", leaveRequestService);
        ReflectionTestUtils.setField(controller, "studentService", studentService);
        ReflectionTestUtils.setField(controller, "buildingService", buildingService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitLeaveRejectsInvalidTimeRange() {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setRole("STUDENT");
        when(studentService.findByUsername("student01")).thenReturn(studentUser);
        when(studentService.getStudentIdByUserId(10L)).thenReturn(88L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student01", null, List.of())
        );

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setStartTime(LocalDateTime.of(2026, 3, 24, 10, 0));
        leaveRequest.setEndTime(LocalDateTime.of(2026, 3, 24, 9, 0));

        ResponseEntity<Result<Long>> response = controller.submitLeave(leaveRequest);

        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        verify(leaveRequestService, never()).insert(leaveRequest);
    }

    @Test
    void submitLeaveRejectsOverlappingRequests() {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setRole("STUDENT");
        when(studentService.findByUsername("student01")).thenReturn(studentUser);
        when(studentService.getStudentIdByUserId(10L)).thenReturn(88L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student01", null, List.of())
        );

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setStartTime(LocalDateTime.of(2026, 3, 24, 10, 0));
        leaveRequest.setEndTime(LocalDateTime.of(2026, 3, 24, 12, 0));
        when(leaveRequestService.findOverlappingLeaves(88L, leaveRequest.getStartTime(), leaveRequest.getEndTime()))
                .thenReturn(List.of(new LeaveRequest()));

        ResponseEntity<Result<Long>> response = controller.submitLeave(leaveRequest);

        assertEquals(400, response.getBody().getCode());
        verify(leaveRequestService, never()).insert(leaveRequest);
    }

    @Test
    void approveLeaveRejectsOutOfScopeCounselor() {
        User counselor = new User();
        counselor.setId(5L);
        counselor.setRole("COUNSELOR");
        when(studentService.findByUsername("counselor01")).thenReturn(counselor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("counselor01", null, List.of())
        );

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(3L);
        leaveRequest.setStudentId(77L);
        leaveRequest.setStatus("PENDING");
        when(leaveRequestService.findById(3L)).thenReturn(leaveRequest);
        when(studentService.getCounselorIdByStudentId(77L)).thenReturn(9L);

        ResponseEntity<Result<Void>> response = controller.approveLeave(3L, "APPROVED", "ok");

        assertEquals(403, response.getBody().getCode());
        verify(leaveRequestService, never()).approveLeave(3L, 5L, "ok", "APPROVED");
    }
}
