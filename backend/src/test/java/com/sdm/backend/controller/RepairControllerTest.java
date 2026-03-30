package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Repair;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AssignmentService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.RepairService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairControllerTest {

    @Mock
    private RepairService repairService;

    @Mock
    private StudentService studentService;

    @Mock
    private BuildingService buildingService;

    @Mock
    private AssignmentService assignmentService;

    private RepairController controller;

    @BeforeEach
    void setUp() {
        controller = new RepairController();
        ReflectionTestUtils.setField(controller, "repairService", repairService);
        ReflectionTestUtils.setField(controller, "studentService", studentService);
        ReflectionTestUtils.setField(controller, "buildingService", buildingService);
        ReflectionTestUtils.setField(controller, "assignmentService", assignmentService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildingListUsesStatusFilterForSuperAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("SUPER_ADMIN");
        when(studentService.findByUsername("admin01")).thenReturn(admin);
        when(repairService.findAllByStatus("PENDING")).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin01", null, List.of())
        );

        ResponseEntity<Result<List<Repair>>> response = controller.getBuildingRepairs("PENDING");

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        verify(repairService).findAllByStatus("PENDING");
        verify(repairService, never()).findAll();
    }

    @Test
    void handleRepairRejectsCompletedRecord() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("SUPER_ADMIN");
        when(studentService.findByUsername("admin01")).thenReturn(admin);

        Repair repair = new Repair();
        repair.setId(3L);
        repair.setStatus("COMPLETED");
        when(repairService.findById(3L)).thenReturn(repair);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin01", null, List.of())
        );

        ResponseEntity<Result<Void>> response = controller.handleRepair(3L, "PROCESSING", "retry");

        assertEquals(400, response.getBody().getCode());
        verify(repairService, never()).handleRepair(3L, 1L, "retry", "PROCESSING");
    }

    @Test
    void handleRepairRejectsInvalidTargetStatus() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("SUPER_ADMIN");
        when(studentService.findByUsername("admin01")).thenReturn(admin);

        Repair repair = new Repair();
        repair.setId(4L);
        repair.setStatus("PENDING");
        when(repairService.findById(4L)).thenReturn(repair);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin01", null, List.of())
        );

        ResponseEntity<Result<Void>> response = controller.handleRepair(4L, "PENDING", "stay");

        assertEquals(400, response.getBody().getCode());
        verify(repairService, never()).handleRepair(4L, 1L, "stay", "PENDING");
    }
}
