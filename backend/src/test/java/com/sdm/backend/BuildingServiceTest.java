package com.sdm.backend;

import com.sdm.backend.entity.Building;
import com.sdm.backend.mapper.BuildingMapper;
import com.sdm.backend.service.BuildingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingMapper buildingMapper;

    @InjectMocks
    private BuildingService buildingService;

    @Test
    void accessibleBuildingsShouldReturnDormAdminBuildingOnly() {
        Building building = new Building();
        building.setId(4L);

        when(buildingMapper.findByAdminId(11L)).thenReturn(building);

        List<Building> result = buildingService.findAccessibleBuildings("DORM_ADMIN", 11L);

        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).getId());
    }

    @Test
    void accessibleBuildingsShouldReturnEmptyWhenDormAdminHasNoBuilding() {
        when(buildingMapper.findByAdminId(11L)).thenReturn(null);

        List<Building> result = buildingService.findAccessibleBuildings("DORM_ADMIN", 11L);

        assertTrue(result.isEmpty());
    }
}
