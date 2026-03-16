package com.sdm.backend.mapper;

import com.sdm.backend.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogMapper {
    int insert(LoginLog loginLog);

    LoginLog findById(@Param("id") Long id);

    List<LoginLog> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    List<LoginLog> findByPageAndFilters(@Param("offset") int offset,
                                         @Param("limit") int limit,
                                         @Param("username") String username,
                                         @Param("status") String status,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    int countAll();

    int countByFilters(@Param("username") String username,
                       @Param("status") String status,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    int deleteById(@Param("id") Long id);
}
