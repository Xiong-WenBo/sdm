package com.sdm.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkAssignCounselorRequest {
    private List<Long> counselorIds;
    private Boolean overwriteExisting;
}
