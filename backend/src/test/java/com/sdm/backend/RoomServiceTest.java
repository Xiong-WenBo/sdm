package com.sdm.backend;

import com.sdm.backend.entity.Room;
import com.sdm.backend.service.RoomService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomServiceTest {

    private final RoomService roomService = new RoomService();

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
}
