package com.sdm.backend.mapper;

import com.sdm.backend.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomMapper {
    List<Room> findAll();
    
    List<Room> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    List<Room> findByPageAndFilters(@Param("offset") int offset,
                                   @Param("limit") int limit,
                                   @Param("buildingId") Long buildingId,
                                   @Param("status") String status);
    
    int countAll();
    
    int countByFilters(@Param("buildingId") Long buildingId,
                      @Param("status") String status);
    
    Room findById(@Param("id") Long id);
    
    int insert(Room room);
    
    int update(Room room);
    
    int deleteById(@Param("id") Long id);
    
    List<Room> findByBuildingId(@Param("buildingId") Long buildingId);
    
    int updateCurrentOccupancy(@Param("roomId") Long roomId, @Param("count") int count);
    
    int incrementOccupancy(@Param("roomId") Long roomId);
    
    int decrementOccupancy(@Param("roomId") Long roomId);
}
