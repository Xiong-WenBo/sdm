package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.StudentService;
import com.sdm.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private StudentService studentService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "studentService", studentService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin01", null, List.of())
        );

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin01");
        currentUser.setRole("SUPER_ADMIN");
        currentUser.setStatus(1);
        currentUser.setPassword("secret");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserDirectoryOnlyReturnsActiveOtherUsers() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin01");
        currentUser.setRole("SUPER_ADMIN");
        currentUser.setStatus(1);
        when(userService.findByUsername("admin01")).thenReturn(currentUser);

        User self = new User();
        self.setId(1L);
        self.setUsername("admin01");
        self.setStatus(1);
        self.setPassword("pw1");

        User activeUser = new User();
        activeUser.setId(2L);
        activeUser.setUsername("student01");
        activeUser.setStatus(1);
        activeUser.setPassword("pw2");

        User inactiveUser = new User();
        inactiveUser.setId(3L);
        inactiveUser.setUsername("student02");
        inactiveUser.setStatus(0);
        inactiveUser.setPassword("pw3");

        when(userService.findAll()).thenReturn(List.of(self, activeUser, inactiveUser));

        ResponseEntity<Result<List<User>>> response = controller.getUserDirectory();

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(2L, response.getBody().getData().getFirst().getId());
        assertNull(response.getBody().getData().getFirst().getPassword());
    }

    @Test
    void getCurrentUserRemovesPassword() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin01");
        currentUser.setRole("SUPER_ADMIN");
        currentUser.setStatus(1);
        currentUser.setPassword("secret");
        when(userService.findByUsername("admin01")).thenReturn(currentUser);

        ResponseEntity<Result<User>> response = controller.getCurrentUser();

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals("admin01", response.getBody().getData().getUsername());
        assertNull(response.getBody().getData().getPassword());
    }

    @Test
    void getUserDirectoryReturnsUnauthorizedWhenNoAuthenticatedUser() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", null, List.of())
        );
        when(userService.findByUsername("ghost")).thenReturn(null);

        ResponseEntity<Result<List<User>>> response = controller.getUserDirectory();

        assertEquals(401, response.getBody().getCode());
        assertTrue(response.getBody().getData() == null);
    }
}
