package com.sdm.backend.mapper;

import com.sdm.backend.entity.MessageTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageTemplateMapper {
    List<MessageTemplate> findAll();
    
    List<MessageTemplate> findByEnabled(@Param("enabled") Integer enabled);
    
    List<MessageTemplate> findByType(@Param("type") String type);
    
    MessageTemplate findByTemplateCode(@Param("templateCode") String templateCode);
    
    MessageTemplate findById(@Param("id") Long id);
    
    int insert(MessageTemplate template);
    
    int update(MessageTemplate template);
    
    int deleteById(@Param("id") Long id);
    
    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);
}
