package com.sdm.backend;

import com.sdm.backend.controller.RepairController;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Repair;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.AssignmentService;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.RepairService;
import com.sdm.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @InjectMocks
    private RepairController repairController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void superAdminBuildingListShouldHonorStatusFilter() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole("SUPER_ADMIN");

        Repair pending = new Repair();
        pending.setStatus("PENDING");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "token")
        );
        when(studentService.findByUsername("admin")).thenReturn(user);
        when(repairService.findAllByStatus("PENDING")).thenReturn(List.of(pending));

        Result<List<Repair>> result = repairController.getBuildingRepairs("PENDING").getBody();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("PENDING", result.getData().get(0).getStatus());
    }
}
