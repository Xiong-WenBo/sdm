package com.sdm.backend.mapper;

import com.sdm.backend.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    List<Message> findByUserId(@Param("userId") Long userId);
    
    List<Message> findByUserIdAndStatus(@Param("userId") Long userId, 
                                        @Param("status") String status);
    
    List<Message> findByUserIdAndCategory(@Param("userId") Long userId, 
                                          @Param("category") String category);
    
    List<Message> findByUserIdWithFilters(@Param("userId") Long userId,
                                         @Param("status") String status,
                                         @Param("category") String category,
                                         @Param("type") String type);
    
    int countUnread(@Param("userId") Long userId);
    
    int countByCategory(@Param("userId") Long userId, @Param("category") String category);
    
    Message findById(@Param("id") Long id);
    
    int insert(Message message);
    
    int update(Message message);
    
    int deleteById(@Param("id") Long id);
    
    int markAsRead(@Param("id") Long id);
    
    int markAllAsRead(@Param("userId") Long userId);
    
    int deleteByUserId(@Param("userId") Long userId);
    
    int batchInsert(@Param("list") List<Message> messages);
}
