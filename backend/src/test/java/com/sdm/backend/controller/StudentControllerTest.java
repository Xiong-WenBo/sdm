package com.sdm.backend.controller;

import com.sdm.backend.dto.BulkAssignCounselorRequest;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Student;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentStudentProfile() {
        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setUsername("student");

        Student student = new Student();
        student.setId(99L);
        student.setUserId(10L);
        student.setStudentNumber("20240001");

        when(studentService.findByUsername("student")).thenReturn(currentUser);
        when(studentService.findByUserId(10L)).thenReturn(student);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("student", null)
        );

        ResponseEntity<Result<Student>> response = studentController.getCurrentStudent();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getStudentNumber()).isEqualTo("20240001");
    }

    @Test
    void shouldBulkAssignCounselorsForSuperAdmin() {
        BulkAssignCounselorRequest request = new BulkAssignCounselorRequest();
        request.setCounselorIds(List.of(11L, 12L));
        request.setOverwriteExisting(false);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("candidateCount", 12);
        summary.put("updatedCount", 12);
        when(studentService.bulkAssignCounselors(List.of(11L, 12L), false)).thenReturn(summary);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(() -> "ROLE_SUPER_ADMIN"))
        );

        ResponseEntity<Result<Map<String, Object>>> response = studentController.bulkAssignCounselors(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().get("updatedCount")).isEqualTo(12);
    }
}
