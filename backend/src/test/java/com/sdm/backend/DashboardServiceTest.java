package com.sdm.backend;

import com.sdm.backend.entity.Assignment;
import com.sdm.backend.entity.Attendance;
import com.sdm.backend.entity.Room;
import com.sdm.backend.mapper.AssignmentMapper;
import com.sdm.backend.mapper.AttendanceMapper;
import com.sdm.backend.mapper.BuildingMapper;
import com.sdm.backend.mapper.LeaveRequestMapper;
import com.sdm.backend.mapper.RepairMapper;
import com.sdm.backend.mapper.RoomMapper;
import com.sdm.backend.mapper.StudentMapper;
import com.sdm.backend.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private BuildingMapper buildingMapper;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private RepairMapper repairMapper;

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @Mock
    private AssignmentMapper assignmentMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void occupancyTrendShouldIgnoreFutureAndInactiveAssignments() {
        Room room = new Room();
        room.setCapacity(4);
        when(roomMapper.findAll()).thenReturn(List.of(room));

        Assignment activeAssignment = new Assignment();
        activeAssignment.setStatus("ACTIVE");
        activeAssignment.setCheckInDate(LocalDate.now().minusDays(2));
        activeAssignment.setCheckOutDate(null);

        Assignment futureAssignment = new Assignment();
        futureAssignment.setStatus("ACTIVE");
        futureAssignment.setCheckInDate(LocalDate.now().plusDays(1));
        futureAssignment.setCheckOutDate(null);

        Assignment inactiveAssignment = new Assignment();
        inactiveAssignment.setStatus("INACTIVE");
        inactiveAssignment.setCheckInDate(LocalDate.now().minusDays(3));
        inactiveAssignment.setCheckOutDate(null);

        when(assignmentMapper.findAll()).thenReturn(List.of(activeAssignment, futureAssignment, inactiveAssignment));

        Map<String, Object> result = dashboardService.getOccupancyTrend(null);
        @SuppressWarnings("unchecked")
        List<Double> rates = (List<Double>) result.get("rates");

        assertEquals(7, rates.size());
        assertEquals(25.0, rates.get(6));
    }

    @Test
    void attendanceStatusShouldCountPresentAsNormal() {
        Attendance present = new Attendance();
        present.setStatus("PRESENT");
        Attendance absent = new Attendance();
        absent.setStatus("ABSENT");
        Attendance leave = new Attendance();
        leave.setStatus("LEAVE");

        Map<String, Object> result = dashboardService.getAttendanceStatus(List.of(present, absent, leave));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> status = (List<Map<String, Object>>) result.get("status");

        assertEquals(3, status.size());
        assertEquals(1L, status.get(0).get("value"));
        assertEquals(1L, status.get(1).get("value"));
        assertEquals(1L, status.get(2).get("value"));
    }
}
