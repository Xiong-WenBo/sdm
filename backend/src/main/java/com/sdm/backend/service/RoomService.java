package com.sdm.backend.service;

import com.sdm.backend.entity.Room;
import com.sdm.backend.mapper.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
public class RoomService {

    @Autowired
    private RoomMapper roomMapper;

    public List<Room> findAll() {
        return roomMapper.findAll();
    }

    public List<Room> findByPage(int page, int size) {
        int offset = (page - 1) * size;
        return roomMapper.findByPage(offset, size);
    }

    public List<Room> findByPageAndFilters(int page, int size, Long buildingId, String status) {
        int offset = (page - 1) * size;
        return roomMapper.findByPageAndFilters(offset, size, buildingId, status);
    }

    public int countAll() {
        return roomMapper.countAll();
    }

    public int countByFilters(Long buildingId, String status) {
        return roomMapper.countByFilters(buildingId, status);
    }

    public Room findById(Long id) {
        return roomMapper.findById(id);
    }

    @Transactional
    public int insert(Room room) {
        if (room.getCurrentOccupancy() == null) {
            room.setCurrentOccupancy(0);
        }
        if (room.getStatus() == null) {
            room.setStatus("AVAILABLE");
        }
        return roomMapper.insert(room);
    }

    public int update(Room room) {
        return roomMapper.update(room);
    }

    @Transactional
    public int deleteById(Long id) {
        return roomMapper.deleteById(id);
    }

    public List<Room> findByBuildingId(Long buildingId) {
        return roomMapper.findByBuildingId(buildingId);
    }

    public boolean canAccommodate(Room room, Integer capacity) {
        if (room == null || capacity == null) {
            return false;
        }
        int currentOccupancy = room.getCurrentOccupancy() != null ? room.getCurrentOccupancy() : 0;
        return capacity >= currentOccupancy;
    }

    @Transactional
    public void incrementOccupancy(Long roomId) {
        roomMapper.incrementOccupancy(roomId);
    }

    @Transactional
    public void decrementOccupancy(Long roomId) {
        roomMapper.decrementOccupancy(roomId);
    }

    @Transactional
    public void recalculateOccupancy(Long roomId) {
        // 这里可以根据 assignment 表重新计算入住人数
        // 暂时简单实现，后续在宿舍分配模块完善
    }

    public int countByBuildingId(Long buildingId) {
        List<Room> rooms = roomMapper.findByBuildingId(buildingId);
        return rooms.size();
    }

    public int countOccupiedByBuildingId(Long buildingId) {
        List<Room> rooms = roomMapper.findByBuildingId(buildingId);
        return (int) rooms.stream().filter(r -> r.getCurrentOccupancy() != null && r.getCurrentOccupancy() > 0).count();
    }

    public int countOccupancyByBuildingId(Long buildingId) {
        List<Room> rooms = roomMapper.findByBuildingId(buildingId);
        return rooms.stream().mapToInt(r -> r.getCurrentOccupancy() != null ? r.getCurrentOccupancy() : 0).sum();
    }

    public int countTotalOccupancy() {
        List<Room> rooms = roomMapper.findAll();
        return rooms.stream().mapToInt(r -> r.getCurrentOccupancy() != null ? r.getCurrentOccupancy() : 0).sum();
    }

    @Transactional
    public int bulkCreate(Long buildingId, Integer totalFloors, Integer roomsPerFloor, Integer capacity, String gender, String status) {
        if (buildingId == null) {
            throw new IllegalArgumentException("请选择楼栋");
        }
        if (totalFloors == null || totalFloors < 1) {
            throw new IllegalArgumentException("总楼层必须大于 0");
        }
        if (roomsPerFloor == null || roomsPerFloor < 1) {
            throw new IllegalArgumentException("每层房间数量必须大于 0");
        }
        if (capacity == null || capacity < 1) {
            throw new IllegalArgumentException("房间容量必须大于 0");
        }

        List<Room> existingRooms = roomMapper.findByBuildingId(buildingId);
        Set<String> existingRoomNumbers = new HashSet<>();
        for (Room room : existingRooms) {
            existingRoomNumbers.add(room.getRoomNumber());
        }

        int roomNumberWidth = Math.max(2, String.valueOf(roomsPerFloor).length());
        Set<String> generatedRoomNumbers = new HashSet<>();

        for (int floor = 1; floor <= totalFloors; floor++) {
            for (int index = 1; index <= roomsPerFloor; index++) {
                String roomNumber = floor + String.format("%0" + roomNumberWidth + "d", index);
                if (!generatedRoomNumbers.add(roomNumber) || existingRoomNumbers.contains(roomNumber)) {
                    throw new IllegalArgumentException("房间号已存在：" + roomNumber);
                }
            }
        }

        int createdCount = 0;
        for (int floor = 1; floor <= totalFloors; floor++) {
            for (int index = 1; index <= roomsPerFloor; index++) {
                Room room = new Room();
                room.setBuildingId(buildingId);
                room.setRoomNumber(floor + String.format("%0" + roomNumberWidth + "d", index));
                room.setFloor(floor);
                room.setCapacity(capacity);
                room.setCurrentOccupancy(0);
                room.setGender(gender == null || gender.isBlank() ? "UNISEX" : gender);
                room.setStatus(status == null || status.isBlank() ? "AVAILABLE" : status);
                createdCount += roomMapper.insert(room);
            }
        }

        return createdCount;
    }
}
