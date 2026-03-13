package com.sdm.backend.service;

import com.sdm.backend.entity.Building;
import com.sdm.backend.mapper.BuildingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildingService {

    @Autowired
    private BuildingMapper buildingMapper;

    public List<Building> findAll() {
        return buildingMapper.findAll();
    }

    public List<Building> findByPage(int page, int size) {
        int offset = (page - 1) * size;
        return buildingMapper.findByPage(offset, size);
    }

    public List<Building> findByPageAndFilters(int page, int size, String name) {
        int offset = (page - 1) * size;
        return buildingMapper.findByPageAndFilters(offset, size, name);
    }

    public int countAll() {
        return buildingMapper.countAll();
    }

    public int countByFilters(String name) {
        return buildingMapper.countByFilters(name);
    }

    public Building findById(Long id) {
        return buildingMapper.findById(id);
    }

    public int insert(Building building) {
        return buildingMapper.insert(building);
    }

    public int update(Building building) {
        return buildingMapper.update(building);
    }

    public int deleteById(Long id) {
        return buildingMapper.deleteById(id);
    }

    public Building findByAdminId(Long adminId) {
        return buildingMapper.findByAdminId(adminId);
    }

    public List<Building> findAvailableAdmins() {
        return buildingMapper.findAvailableAdmins();
    }
}
