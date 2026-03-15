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
    
    int countUnread(@Param("userId") Long userId);
    
    Message findById(@Param("id") Long id);
    
    int insert(Message message);
    
    int update(Message message);
    
    int deleteById(@Param("id") Long id);
    
    int markAsRead(@Param("id") Long id);
    
    int deleteByUserId(@Param("userId") Long userId);
}
