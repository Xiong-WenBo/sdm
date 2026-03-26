package com.sdm.backend.service;

import com.sdm.backend.entity.Assignment;
import com.sdm.backend.mapper.AssignmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private RoomService roomService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService();
        ReflectionTestUtils.setField(assignmentService, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(assignmentService, "roomService", roomService);
    }

    @Test
    void deleteInactiveAssignmentDoesNotChangeOccupancy() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setRoomId(10L);
        assignment.setStatus("INACTIVE");
        when(assignmentMapper.findById(1L)).thenReturn(assignment);
        when(assignmentMapper.deleteById(1L)).thenReturn(1);

        int result = assignmentService.deleteById(1L);

        assertEquals(1, result);
        verify(roomService, never()).decrementOccupancy(10L);
    }

    @Test
    void checkoutInactiveAssignmentDoesNothing() {
        Assignment assignment = new Assignment();
        assignment.setId(2L);
        assignment.setRoomId(11L);
        assignment.setStatus("INACTIVE");
        when(assignmentMapper.findById(2L)).thenReturn(assignment);

        int result = assignmentService.checkOut(2L, LocalDate.now());

        assertEquals(0, result);
        verify(assignmentMapper, never()).update(assignment);
        verify(roomService, never()).decrementOccupancy(11L);
    }

    @Test
    void transferWithinSameBedDoesNothing() {
        Assignment assignment = new Assignment();
        assignment.setId(3L);
        assignment.setRoomId(12L);
        assignment.setBedNumber("A1");
        assignment.setStatus("ACTIVE");
        when(assignmentMapper.findById(3L)).thenReturn(assignment);

        int result = assignmentService.transfer(3L, 12L, "A1");

        assertEquals(0, result);
        verify(assignmentMapper, never()).update(assignment);
        verify(roomService, never()).decrementOccupancy(12L);
        verify(roomService, never()).incrementOccupancy(12L);
    }

    @Test
    void transferAcrossRoomsUpdatesBothOccupancies() {
        Assignment assignment = new Assignment();
        assignment.setId(4L);
        assignment.setRoomId(12L);
        assignment.setBedNumber("A1");
        assignment.setStatus("ACTIVE");
        when(assignmentMapper.findById(4L)).thenReturn(assignment);
        when(assignmentMapper.update(assignment)).thenReturn(1);

        int result = assignmentService.transfer(4L, 13L, "B2");

        assertEquals(1, result);
        assertEquals(13L, assignment.getRoomId());
        assertEquals("B2", assignment.getBedNumber());
        verify(roomService).decrementOccupancy(12L);
        verify(roomService).incrementOccupancy(13L);
    }
}
