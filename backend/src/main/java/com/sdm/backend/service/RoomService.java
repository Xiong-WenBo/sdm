package com.sdm.backend.service;

import com.sdm.backend.entity.Room;
import com.sdm.backend.mapper.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
