package com.sdm.backend.service;

import com.sdm.backend.entity.LeaveRequest;
import com.sdm.backend.mapper.LeaveRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    private LeaveRequestService service;

    @BeforeEach
    void setUp() {
        service = new LeaveRequestService();
        ReflectionTestUtils.setField(service, "leaveRequestMapper", leaveRequestMapper);
    }

    @Test
    void findOverlappingLeavesDelegatesToMapper() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 24, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 24, 12, 0);
        when(leaveRequestMapper.findOverlappingLeaves(8L, start, end)).thenReturn(List.of(new LeaveRequest()));

        List<LeaveRequest> result = service.findOverlappingLeaves(8L, start, end);

        assertEquals(1, result.size());
        verify(leaveRequestMapper).findOverlappingLeaves(8L, start, end);
    }
}
