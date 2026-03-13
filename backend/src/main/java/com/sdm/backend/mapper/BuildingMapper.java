package com.sdm.backend.mapper;

import com.sdm.backend.entity.Building;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BuildingMapper {
    List<Building> findAll();
    
    List<Building> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    List<Building> findByPageAndFilters(@Param("offset") int offset,
                                       @Param("limit") int limit,
                                       @Param("name") String name);
    
    int countAll();
    
    int countByFilters(@Param("name") String name);
    
    Building findById(@Param("id") Long id);
    
    int insert(Building building);
    
    int update(Building building);
    
    int deleteById(@Param("id") Long id);
    
    Building findByAdminId(@Param("adminId") Long adminId);
    
    List<Building> findAvailableAdmins();
}
