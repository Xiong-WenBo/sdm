package com.sdm.backend;

import com.sdm.backend.controller.LeaveRequestController;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.LeaveRequest;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.LeaveRequestService;
import com.sdm.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @InjectMocks
    private LeaveRequestController leaveRequestController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveLeaveShouldRejectNonOwnedStudent() {
        User counselor = new User();
        counselor.setId(5L);
        counselor.setUsername("counselor");
        counselor.setRole("COUNSELOR");

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(10L);
        leaveRequest.setStudentId(22L);
        leaveRequest.setStatus("PENDING");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("counselor", "token")
        );
        when(studentService.findByUsername("counselor")).thenReturn(counselor);
        when(leaveRequestService.findById(10L)).thenReturn(leaveRequest);
        when(studentService.getCounselorIdByStudentId(22L)).thenReturn(99L);

        Result<Void> result = leaveRequestController.approveLeave(10L, "APPROVED", "ok").getBody();

        assertNotNull(result);
        assertEquals(403, result.getCode());
        verify(leaveRequestService, never()).approveLeave(10L, 5L, "ok", "APPROVED");
    }

    @Test
    void approveLeaveShouldRejectInvalidStatus() {
        User counselor = new User();
        counselor.setId(5L);
        counselor.setUsername("counselor");
        counselor.setRole("COUNSELOR");

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(10L);
        leaveRequest.setStudentId(22L);
        leaveRequest.setStatus("PENDING");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("counselor", "token")
        );
        when(studentService.findByUsername("counselor")).thenReturn(counselor);
        when(leaveRequestService.findById(10L)).thenReturn(leaveRequest);
        when(studentService.getCounselorIdByStudentId(22L)).thenReturn(5L);

        Result<Void> result = leaveRequestController.approveLeave(10L, "PENDING", "ok").getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(leaveRequestService, never()).approveLeave(10L, 5L, "ok", "PENDING");
    }
}
