package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Assignment;
import com.sdm.backend.entity.Room;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AssignmentService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.RoomService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private RoomService roomService;

    @Mock
    private BuildingService buildingService;

    @Mock
    private UserService userService;

    private AssignmentController controller;

    @BeforeEach
    void setUp() {
        controller = new AssignmentController();
        ReflectionTestUtils.setField(controller, "assignmentService", assignmentService);
        ReflectionTestUtils.setField(controller, "roomService", roomService);
        ReflectionTestUtils.setField(controller, "buildingService", buildingService);
        ReflectionTestUtils.setField(controller, "userService", userService);

        User user = new User();
        user.setId(1L);
        user.setUsername("admin01");
        user.setRole("SUPER_ADMIN");
        when(userService.findByUsername("admin01")).thenReturn(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin01", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkoutRejectsInactiveAssignment() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setRoomId(10L);
        assignment.setStatus("INACTIVE");
        when(assignmentService.findById(1L)).thenReturn(assignment);

        ResponseEntity<Result<Void>> response = controller.checkOut(1L, LocalDate.now());

        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        verify(assignmentService, never()).checkOut(1L, LocalDate.now());
    }

    @Test
    void transferRejectsUnchangedTargetBed() {
        Assignment assignment = new Assignment();
        assignment.setId(2L);
        assignment.setRoomId(20L);
        assignment.setBedNumber("A1");
        assignment.setStatus("ACTIVE");
        when(assignmentService.findById(2L)).thenReturn(assignment);

        Room room = new Room();
        room.setId(20L);
        room.setCapacity(4);
        room.setCurrentOccupancy(3);
        room.setBuildingId(100L);
        when(roomService.findById(20L)).thenReturn(room);

        ResponseEntity<Result<Void>> response = controller.transfer(2L, 20L, "A1");

        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        verify(assignmentService, never()).transfer(2L, 20L, "A1");
    }

    @Test
    void transferRejectsFullTargetRoomWhenChangingRooms() {
        Assignment assignment = new Assignment();
        assignment.setId(3L);
        assignment.setRoomId(21L);
        assignment.setBedNumber("A1");
        assignment.setStatus("ACTIVE");
        when(assignmentService.findById(3L)).thenReturn(assignment);

        Room room = new Room();
        room.setId(22L);
        room.setCapacity(4);
        room.setCurrentOccupancy(4);
        room.setBuildingId(100L);
        when(roomService.findById(22L)).thenReturn(room);

        ResponseEntity<Result<Void>> response = controller.transfer(3L, 22L, "B1");

        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        verify(assignmentService, never()).transfer(3L, 22L, "B1");
    }
}
