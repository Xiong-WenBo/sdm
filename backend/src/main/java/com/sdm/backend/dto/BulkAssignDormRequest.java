package com.sdm.backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BulkAssignDormRequest {
    private Long buildingId;
    private LocalDate checkInDate;
}
