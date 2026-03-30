package com.sdm.backend;

import com.sdm.backend.dto.CreateUserRequest;
import com.sdm.backend.mapper.StudentMapper;
import com.sdm.backend.mapper.UserMapper;
import com.sdm.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StudentMapper studentMapper;

    @Spy
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    @Test
    void createUserWithOptionalStudentShouldInsertStudentProfile() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("20230001");
        request.setPassword("secret123");
        request.setRealName("Test Student");
        request.setRole("STUDENT");
        request.setStatus(1);
        request.setStudentNumber("20230001");
        request.setClassName("CS2301");
        request.setMajor("Computer Science");
        request.setCounselorId(99L);

        userService.createUserWithOptionalStudent(request);

        verify(userMapper).insert(argThat(user ->
                "20230001".equals(user.getUsername())
                        && "STUDENT".equals(user.getRole())
                        && user.getPassword() != null
                        && !user.getPassword().equals("secret123")
        ));
        verify(studentMapper).insert(argThat(student ->
                "20230001".equals(student.getStudentNumber())
                        && "CS2301".equals(student.getClassName())
                        && Long.valueOf(99L).equals(student.getCounselorId())
        ));
    }
}
