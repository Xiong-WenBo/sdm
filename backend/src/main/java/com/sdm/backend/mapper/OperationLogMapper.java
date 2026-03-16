package com.sdm.backend.mapper;

import com.sdm.backend.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {
    int insert(OperationLog operationLog);

    OperationLog findById(@Param("id") Long id);

    List<OperationLog> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    List<OperationLog> findByPageAndFilters(@Param("offset") int offset,
                                             @Param("limit") int limit,
                                             @Param("username") String username,
                                             @Param("module") String module,
                                             @Param("operation") String operation,
                                             @Param("status") String status,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    int countAll();

    int countByFilters(@Param("username") String username,
                       @Param("module") String module,
                       @Param("operation") String operation,
                       @Param("status") String status,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    int deleteById(@Param("id") Long id);
}
