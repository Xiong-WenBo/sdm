package com.sdm.backend.service;

import com.sdm.backend.entity.Student;
import com.sdm.backend.entity.User;
import com.sdm.backend.mapper.StudentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private StudentService studentService;

    @Test
    void shouldGeneratePasswordFromLastSixDigits() {
        ReflectionTestUtils.setField(studentService, "initialPasswordFallback", "123123");

        assertThat(studentService.generateInitialPassword("202410123456")).isEqualTo("123456");
        assertThat(studentService.generateInitialPassword("12345")).isEqualTo("12345");
        assertThat(studentService.generateInitialPassword("   ")).isEqualTo("123123");
    }

    @Test
    void shouldDelegateCounselorQueryToMapper() {
        Student student = new Student();
        student.setId(1L);
        when(studentMapper.findByCounselorId(7L)).thenReturn(List.of(student));

        List<Student> result = studentService.findByCounselorId(7L);

        assertThat(result).containsExactly(student);
        verify(studentMapper).findByCounselorId(7L);
    }

    @Test
    void shouldAssignSameClassToSameCounselorAndBalanceLoads() {
        User counselorA = new User();
        counselorA.setId(101L);
        counselorA.setRealName("Counselor A");
        counselorA.setStatus(1);

        User counselorB = new User();
        counselorB.setId(102L);
        counselorB.setRealName("Counselor B");
        counselorB.setStatus(1);

        Student existing = new Student();
        existing.setId(1L);
        existing.setCounselorId(101L);
        existing.setClassName("软件一班");
        existing.setStudentNumber("20230001");

        Student classOneA = new Student();
        classOneA.setId(2L);
        classOneA.setClassName("软件二班");
        classOneA.setMajor("软件工程");
        classOneA.setStudentNumber("20240001");

        Student classOneB = new Student();
        classOneB.setId(3L);
        classOneB.setClassName("软件二班");
        classOneB.setMajor("软件工程");
        classOneB.setStudentNumber("20240002");

        Student classTwo = new Student();
        classTwo.setId(4L);
        classTwo.setClassName("软件三班");
        classTwo.setMajor("软件工程");
        classTwo.setStudentNumber("20240003");

        when(userService.findByRole("COUNSELOR")).thenReturn(List.of(counselorA, counselorB));
        when(studentMapper.findAll()).thenReturn(List.of(existing, classOneA, classOneB, classTwo));

        Map<String, Object> result = studentService.bulkAssignCounselors(List.of(101L, 102L), false);

        assertThat(result.get("candidateCount")).isEqualTo(3);
        assertThat(result.get("updatedCount")).isEqualTo(3);

        ArgumentCaptor<Long> studentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> counselorIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(studentMapper, times(3)).updateCounselorById(studentIdCaptor.capture(), counselorIdCaptor.capture());

        Map<Long, Long> assignedMap = java.util.stream.IntStream.range(0, studentIdCaptor.getAllValues().size())
                .boxed()
                .collect(Collectors.toMap(
                        index -> studentIdCaptor.getAllValues().get(index),
                        index -> counselorIdCaptor.getAllValues().get(index)
                ));

        assertThat(assignedMap.get(2L)).isEqualTo(assignedMap.get(3L));
        assertThat(assignedMap.get(4L)).isNotNull();
        assertThat(List.copyOf(assignedMap.values()))
                .containsOnly(101L, 102L);
    }
}
