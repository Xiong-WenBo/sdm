package com.sdm.backend.mapper;

import com.sdm.backend.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceMapper {
    List<Attendance> findAll();
    
    List<Attendance> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    List<Attendance> findByPageAndFilters(@Param("offset") int offset,
                                         @Param("limit") int limit,
                                         @Param("studentId") Long studentId,
                                         @Param("buildingId") Long buildingId,
                                         @Param("checkDate") LocalDate checkDate,
                                         @Param("checkTime") String checkTime,
                                         @Param("status") String status);
    
    int countAll();
    
    int countByFilters(@Param("studentId") Long studentId,
                      @Param("buildingId") Long buildingId,
                      @Param("checkDate") LocalDate checkDate,
                      @Param("checkTime") String checkTime,
                      @Param("status") String status);
    
    Attendance findById(@Param("id") Long id);
    
    Attendance findByStudentAndDate(@Param("studentId") Long studentId,
                                   @Param("checkDate") LocalDate checkDate,
                                   @Param("checkTime") String checkTime);
    
    int insert(Attendance attendance);
    
    int update(Attendance attendance);
    
    int deleteById(@Param("id") Long id);
    
    /**
     * 批量插入查寝记录
     */
    int batchInsert(@Param("list") List<Attendance> list);
    
    /**
     * 查询楼栋入住学生列表（用于查寝录入）
     */
    List<Attendance> findStudentsInBuilding(@Param("buildingId") Long buildingId);
    
    /**
     * 查询班级学生列表（用于辅导员查寝）
     */
    List<Attendance> findStudentsByCounselor(@Param("counselorId") Long counselorId);
    
    /**
     * 根据日期查询查寝记录
     */
    List<Attendance> findByDate(@Param("checkDate") LocalDate checkDate);
    
    /**
     * 根据学生 ID 查询查寝记录
     */
    List<Attendance> findByStudentId(@Param("studentId") Long studentId);
}
