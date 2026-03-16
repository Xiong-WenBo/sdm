package com.sdm.backend.mapper;

import com.sdm.backend.entity.Repair;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RepairMapper {
    
    List<Repair> findByStudentId(@Param("studentId") Long studentId);
    
    List<Repair> findByBuildingId(@Param("buildingId") Long buildingId);
    
    List<Repair> findAll();
    
    Repair findById(@Param("id") Long id);
    
    int insert(Repair repair);
    
    int update(Repair repair);
    
    int deleteById(@Param("id") Long id);
    
    List<Repair> findByStudentIdAndStatus(@Param("studentId") Long studentId, 
                                          @Param("status") String status);
    
    List<Repair> findByBuildingIdAndStatus(@Param("buildingId") Long buildingId, 
                                           @Param("status") String status);
}
