package com.sdm.backend.service;

import com.sdm.backend.entity.Student;
import com.sdm.backend.mapper.StudentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
