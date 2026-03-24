package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.StudentService;
import com.sdm.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private UserController userController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectAccessingAnotherUsersProfile() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");
        currentUser.setRole("STUDENT");
        when(userService.findByUsername("alice")).thenReturn(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", null)
        );

        ResponseEntity<Result<User>> response = userController.getUserById(2L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(403);
    }

    @Test
    void shouldReturnCurrentUsersProfileWithoutPassword() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");
        currentUser.setRole("STUDENT");
        currentUser.setPassword("secret");
        when(userService.findByUsername("alice")).thenReturn(currentUser);
        when(userService.findById(1L)).thenReturn(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", null)
        );

        ResponseEntity<Result<User>> response = userController.getUserById(1L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getPassword()).isNull();
    }
}
