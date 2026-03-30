package com.sdm.backend.service;

import com.sdm.backend.entity.Assignment;
import com.sdm.backend.entity.Room;
import com.sdm.backend.entity.Student;
import com.sdm.backend.mapper.AssignmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private RoomService roomService;

    @Mock
    private StudentService studentService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService();
        ReflectionTestUtils.setField(assignmentService, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(assignmentService, "roomService", roomService);
        ReflectionTestUtils.setField(assignmentService, "studentService", studentService);
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

    @Test
    void bulkAutoAssignShouldKeepGroupsSeparated() {
        Student first = student(1L, "软件一班", "软件工程", "20240001");
        Student second = student(2L, "软件一班", "软件工程", "20240002");
        Student third = student(3L, "软件二班", "软件工程", "20240003");

        Room room101 = room(101L, "101", 1, 4, 0, "AVAILABLE");
        Room room102 = room(102L, "102", 1, 4, 0, "AVAILABLE");

        when(studentService.findUnassignedForDormAssignment()).thenReturn(List.of(first, second, third));
        when(roomService.findByBuildingId(1L)).thenReturn(List.of(room102, room101));

        Map<String, Object> result = assignmentService.bulkAutoAssign(1L, LocalDate.of(2026, 3, 30), 9L);

        assertEquals(3, result.get("candidateCount"));
        assertEquals(3, result.get("assignedCount"));
        assertEquals(0, result.get("unassignedCount"));
        assertEquals(2, result.get("usedRooms"));
        verify(roomService, times(3)).incrementOccupancy(org.mockito.ArgumentMatchers.anyLong());
        verify(assignmentMapper).insert(matchesAssignment(1L, 101L, "A1"));
        verify(assignmentMapper).insert(matchesAssignment(2L, 101L, "A2"));
        verify(assignmentMapper).insert(matchesAssignment(3L, 102L, "A1"));
    }

    @Test
    void bulkAutoAssignShouldStopWhenNoMoreRoomsRemain() {
        Student first = student(1L, "软件一班", "软件工程", "20240001");
        Student second = student(2L, "软件一班", "软件工程", "20240002");
        Student third = student(3L, "软件一班", "软件工程", "20240003");
        Student fourth = student(4L, "软件二班", "软件工程", "20240004");

        Room room101 = room(101L, "101", 1, 2, 0, "AVAILABLE");

        when(studentService.findUnassignedForDormAssignment()).thenReturn(List.of(first, second, third, fourth));
        when(roomService.findByBuildingId(1L)).thenReturn(List.of(room101));

        Map<String, Object> result = assignmentService.bulkAutoAssign(1L, LocalDate.of(2026, 4, 1), 7L);

        assertEquals(4, result.get("candidateCount"));
        assertEquals(2, result.get("assignedCount"));
        assertEquals(2, result.get("unassignedCount"));
        assertEquals(1, result.get("usedRooms"));
        verify(assignmentMapper).insert(matchesAssignment(1L, 101L, "A1"));
        verify(assignmentMapper).insert(matchesAssignment(2L, 101L, "A2"));
    }

    private Student student(Long id, String className, String major, String studentNumber) {
        Student student = new Student();
        student.setId(id);
        student.setClassName(className);
        student.setMajor(major);
        student.setStudentNumber(studentNumber);
        return student;
    }

    private Room room(Long id, String roomNumber, Integer floor, Integer capacity, Integer currentOccupancy, String status) {
        Room room = new Room();
        room.setId(id);
        room.setRoomNumber(roomNumber);
        room.setFloor(floor);
        room.setCapacity(capacity);
        room.setCurrentOccupancy(currentOccupancy);
        room.setStatus(status);
        return room;
    }

    private Assignment matchesAssignment(Long studentId, Long roomId, String bedNumber) {
        return argThat(new ArgumentMatcher<>() {
            @Override
            public boolean matches(Assignment assignment) {
                return assignment != null
                        && studentId.equals(assignment.getStudentId())
                        && roomId.equals(assignment.getRoomId())
                        && bedNumber.equals(assignment.getBedNumber());
            }
        });
    }
}
