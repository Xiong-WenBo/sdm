package com.sdm.backend.mapper;

import com.sdm.backend.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {
    int insert(Student student);
    
    Student findByUserId(@Param("userId") Long userId);
    
    Student findByStudentNumber(@Param("studentNumber") String studentNumber);
    
    int update(Student student);
    
    int deleteByUserId(@Param("userId") Long userId);
    
    List<Student> findAll();
    
    List<Student> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    List<Student> findByPageAndFilters(@Param("offset") int offset,
                                       @Param("limit") int limit,
                                       @Param("className") String className,
                                       @Param("major") String major,
                                       @Param("counselorId") Long counselorId);
    
    int countAll();
    
    int countByFilters(@Param("className") String className,
                      @Param("major") String major,
                      @Param("counselorId") Long counselorId);
    
    Student findById(@Param("id") Long id);
    
    Student findByStudentNumberExclude(@Param("studentNumber") String studentNumber,
                                       @Param("excludeUserId") Long excludeUserId);
    
    /**
     * 查询在指定时间段内有请假申请的学生 ID 列表
     */
    List<Long> findStudentsOnLeave(@Param("studentIds") List<Long> studentIds,
                                   @Param("startTime") java.time.LocalDateTime startTime,
                                   @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 根据辅导员 ID 查询学生列表
     */
    List<Student> findByCounselorId(@Param("counselorId") Long counselorId);

    List<Student> findUnassignedForDormAssignment();
}
