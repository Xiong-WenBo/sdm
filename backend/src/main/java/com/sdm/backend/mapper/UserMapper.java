package com.sdm.backend.mapper;

import com.sdm.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    
    List<User> findAll();
    
    List<User> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    List<User> findByPageAndFilters(@Param("offset") int offset, 
                                   @Param("limit") int limit, 
                                   @Param("username") String username, 
                                   @Param("role") String role, 
                                   @Param("status") Integer status);
    
    int countAll();
    
    int countByFilters(@Param("username") String username, 
                       @Param("role") String role, 
                       @Param("status") Integer status);
    
    int insert(User user);
    
    int update(User user);
    
    int deleteById(@Param("id") Long id);
    
    User findById(@Param("id") Long id);
}