package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.BuildingService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private StudentService studentService;

    @Mock
    private BuildingService buildingService;

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

    @Test
    void shouldReturnDirectoryWithoutCurrentUser() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin01");
        currentUser.setRole("SUPER_ADMIN");
        when(userService.findByUsername("admin01")).thenReturn(currentUser);

        User activeUser = new User();
        activeUser.setId(2L);
        activeUser.setUsername("student01");
        activeUser.setPassword(null);

        when(userService.findMessagingDirectory(1L)).thenReturn(List.of(activeUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin01", null)
        );

        ResponseEntity<Result<List<User>>> response = userController.getUserDirectory();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().getFirst().getId()).isEqualTo(2L);
    }

    @Test
    void shouldRequireAuthenticatedUserForDirectory() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", null)
        );
        when(userService.findByUsername("ghost")).thenReturn(null);

        ResponseEntity<Result<List<User>>> response = userController.getUserDirectory();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(401);
    }
}
