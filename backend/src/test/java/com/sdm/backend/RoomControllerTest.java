package com.sdm.backend;

import com.sdm.backend.controller.RoomController;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Room;
import com.sdm.backend.service.BuildingService;
import com.sdm.backend.service.RoomService;
import com.sdm.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private BuildingService buildingService;

    @Mock
    private UserService userService;

    @InjectMocks
    private RoomController roomController;

    @Test
    void updateRoomShouldRejectCapacityBelowOccupancy() {
        Room existingRoom = new Room();
        existingRoom.setId(3L);
        existingRoom.setBuildingId(1L);
        existingRoom.setCurrentOccupancy(4);

        Room updateRequest = new Room();
        updateRequest.setCapacity(3);

        when(roomService.findById(3L)).thenReturn(existingRoom);
        when(roomService.canAccommodate(existingRoom, 3)).thenReturn(false);

        Result<Void> result = roomController.updateRoom(3L, updateRequest).getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(roomService, never()).update(updateRequest);
    }

    @Test
    void deleteRoomShouldRejectOccupiedRoom() {
        Room room = new Room();
        room.setId(8L);
        room.setBuildingId(1L);
        room.setCurrentOccupancy(1);

        when(roomService.findById(8L)).thenReturn(room);

        Result<Void> result = roomController.deleteRoom(8L).getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(roomService, never()).deleteById(8L);
    }
}
