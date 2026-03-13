package com.sdm.backend.mapper;

import com.sdm.backend.entity.Assignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentMapper {
    List<Assignment> findAll();
    
    List<Assignment> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    List<Assignment> findByPageAndFilters(@Param("offset") int offset,
                                         @Param("limit") int limit,
                                         @Param("studentId") Long studentId,
                                         @Param("roomId") Long roomId,
                                         @Param("buildingId") Long buildingId,
                                         @Param("status") String status);
    
    int countAll();
    
    int countByFilters(@Param("studentId") Long studentId,
                      @Param("roomId") Long roomId,
                      @Param("buildingId") Long buildingId,
                      @Param("status") String status);
    
    Assignment findById(@Param("id") Long id);
    
    List<Assignment> findByStudentId(@Param("studentId") Long studentId);
    
    List<Assignment> findByRoomId(@Param("roomId") Long roomId);
    
    List<Assignment> findByBuildingId(@Param("buildingId") Long buildingId);
    
    int insert(Assignment assignment);
    
    int update(Assignment assignment);
    
    int deleteById(@Param("id") Long id);
    
    /**
     * 查询可分配的房间（有空床位）
     */
    List<Assignment> findAvailableRooms(@Param("buildingId") Long buildingId,
                                       @Param("gender") String gender);
}
