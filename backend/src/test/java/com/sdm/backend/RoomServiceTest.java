package com.sdm.backend;

import com.sdm.backend.entity.Room;
import com.sdm.backend.mapper.RoomMapper;
import com.sdm.backend.service.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    @Test
    void canAccommodateShouldRejectCapacityBelowOccupancy() {
        Room room = new Room();
        room.setCurrentOccupancy(3);

        assertFalse(roomService.canAccommodate(room, 2));
    }

    @Test
    void canAccommodateShouldAllowMatchingCapacity() {
        Room room = new Room();
        room.setCurrentOccupancy(3);

        assertTrue(roomService.canAccommodate(room, 3));
    }

    @Test
    void bulkCreateShouldGenerateRoomNumbersByFloor() {
        when(roomMapper.findByBuildingId(1L)).thenReturn(List.of());
        when(roomMapper.insert(any(Room.class))).thenReturn(1);

        int created = roomService.bulkCreate(1L, 2, 3, 4, "UNISEX", "AVAILABLE");

        assertEquals(6, created);
        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper, times(6)).insert(captor.capture());
        List<Room> createdRooms = captor.getAllValues();
        assertEquals("101", createdRooms.get(0).getRoomNumber());
        assertEquals("103", createdRooms.get(2).getRoomNumber());
        assertEquals("201", createdRooms.get(3).getRoomNumber());
        assertEquals(4, createdRooms.get(0).getCapacity());
    }

    @Test
    void bulkCreateShouldRejectExistingRoomNumber() {
        Room existing = new Room();
        existing.setRoomNumber("101");
        when(roomMapper.findByBuildingId(1L)).thenReturn(List.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> roomService.bulkCreate(1L, 1, 2, 4, "UNISEX", "AVAILABLE")
        );

        assertEquals("房间号已存在：101", exception.getMessage());
    }
}
