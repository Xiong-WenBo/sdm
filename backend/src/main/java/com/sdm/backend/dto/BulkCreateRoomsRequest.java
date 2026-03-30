package com.sdm.backend.dto;

import lombok.Data;

@Data
public class BulkCreateRoomsRequest {
    private Long buildingId;
    private Integer totalFloors;
    private Integer roomsPerFloor;
    private Integer capacity;
    private String gender;
    private String status;
}
