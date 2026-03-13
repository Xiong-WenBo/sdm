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
}
