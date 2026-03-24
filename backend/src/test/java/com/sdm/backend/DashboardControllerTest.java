package com.sdm.backend;

import com.sdm.backend.controller.DashboardController;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.Repair;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.DashboardService;
import com.sdm.backend.service.StudentService;
import com.sdm.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private UserService userService;

    @Mock
    private StudentService studentService;

    @Mock
    private BuildingService buildingService;

    @InjectMocks
    private DashboardController dashboardController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void repairStatusShouldUseDormAdminBuildingScope() {
        User dormAdmin = new User();
        dormAdmin.setId(7L);
        dormAdmin.setUsername("dorm-admin");
        dormAdmin.setRole("DORM_ADMIN");

        Building building = new Building();
        building.setId(3L);

        Repair repair = new Repair();
        repair.setStatus("PENDING");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dorm-admin", "token")
        );
        when(userService.findByUsername("dorm-admin")).thenReturn(dormAdmin);
        when(buildingService.findByAdminUserId(7L)).thenReturn(building);
        when(dashboardService.getRepairsByBuildingId(3L)).thenReturn(List.of(repair));
        when(dashboardService.getRepairStatus(List.of(repair))).thenReturn(Map.of("status", List.of()));

        Result<Map<String, Object>> result = dashboardController.getRepairStatus().getBody();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(dashboardService).getRepairsByBuildingId(3L);
    }
}
