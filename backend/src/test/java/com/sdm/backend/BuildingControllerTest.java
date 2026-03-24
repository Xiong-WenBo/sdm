package com.sdm.backend;

import com.sdm.backend.controller.BuildingController;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.RoomService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingControllerTest {

    @Mock
    private BuildingService buildingService;

    @Mock
    private RoomService roomService;

    @Mock
    private UserService userService;

    @InjectMocks
    private BuildingController buildingController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessibleBuildingsShouldReturnDormAdminScope() {
        User user = new User();
        user.setId(12L);
        user.setUsername("dorm-admin");
        user.setRole("DORM_ADMIN");

        Building building = new Building();
        building.setId(5L);
        building.setName("1号楼");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dorm-admin", "token")
        );
        when(userService.findByUsername("dorm-admin")).thenReturn(user);
        when(buildingService.findAccessibleBuildings("DORM_ADMIN", 12L)).thenReturn(List.of(building));

        Result<List<Building>> result = buildingController.getAccessibleBuildings().getBody();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals(5L, result.getData().get(0).getId());
    }

    @Test
    void deleteBuildingShouldRejectWhenRoomsExist() {
        Building building = new Building();
        building.setId(9L);

        when(buildingService.findById(9L)).thenReturn(building);
        when(roomService.countByBuildingId(9L)).thenReturn(2);

        Result<Void> result = buildingController.deleteBuilding(9L).getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(buildingService, never()).deleteById(9L);
    }
}
