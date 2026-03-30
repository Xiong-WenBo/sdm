package com.sdm.backend.service;

import com.sdm.backend.entity.Repair;
import com.sdm.backend.mapper.RepairMapper;
import com.sdm.backend.mapper.StudentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

    @Mock
    private RepairMapper repairMapper;

    @Mock
    private StudentMapper studentMapper;

    private RepairService service;

    @BeforeEach
    void setUp() {
        service = new RepairService();
        ReflectionTestUtils.setField(service, "repairMapper", repairMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
    }

    @Test
    void findAllByStatusDelegatesToMapper() {
        when(repairMapper.findAllByStatus("PROCESSING")).thenReturn(List.of(new Repair()));

        List<Repair> result = service.findAllByStatus("PROCESSING");

        assertEquals(1, result.size());
        verify(repairMapper).findAllByStatus("PROCESSING");
    }

    @Test
    void handleRepairRejectsCompletedSourceStatus() {
        Repair repair = new Repair();
        repair.setId(6L);
        repair.setStatus("COMPLETED");
        when(repairMapper.findById(6L)).thenReturn(repair);

        int result = service.handleRepair(6L, 9L, "retry", "PROCESSING");

        assertEquals(0, result);
        verify(repairMapper, never()).update(repair);
    }

    @Test
    void handleRepairRejectsInvalidTargetStatus() {
        Repair repair = new Repair();
        repair.setId(7L);
        repair.setStatus("PENDING");
        when(repairMapper.findById(7L)).thenReturn(repair);

        int result = service.handleRepair(7L, 9L, "note", "PENDING");

        assertEquals(0, result);
        verify(repairMapper, never()).update(repair);
    }
}
