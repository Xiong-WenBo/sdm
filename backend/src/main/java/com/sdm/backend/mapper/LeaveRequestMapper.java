package com.sdm.backend.mapper;

import com.sdm.backend.entity.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LeaveRequestMapper {
    
    List<LeaveRequest> findByStudentId(@Param("studentId") Long studentId);
    
    List<LeaveRequest> findByCounselorId(@Param("counselorId") Long counselorId);
    
    List<LeaveRequest> findByBuildingId(@Param("buildingId") Long buildingId);
    
    List<LeaveRequest> findAll();
    
    LeaveRequest findById(@Param("id") Long id);
    
    int insert(LeaveRequest leaveRequest);
    
    int update(LeaveRequest leaveRequest);
    
    int deleteById(@Param("id") Long id);
    
    List<LeaveRequest> findByStudentIdAndStatus(@Param("studentId") Long studentId, 
                                                 @Param("status") String status);
    
    List<LeaveRequest> findByCounselorIdAndStatus(@Param("counselorId") Long counselorId, 
                                                   @Param("status") String status);

    List<LeaveRequest> findOverlappingLeaves(@Param("studentId") Long studentId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);
}
