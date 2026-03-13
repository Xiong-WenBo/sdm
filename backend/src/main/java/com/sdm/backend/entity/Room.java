package com.sdm.backend.entity;

import lombok.Data;

@Data
public class Room {
    private Long id;
    private Long buildingId;
    private String buildingName; // 楼栋名称（关联查询）
    private String roomNumber;
    private Integer floor;
    private Integer capacity;
    private Integer currentOccupancy;
    private String gender;
    private String status;
}
