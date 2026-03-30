package com.sdm.backend;

import com.sdm.backend.controller.UserController;
import com.sdm.backend.dto.CreateUserRequest;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Building;
import com.sdm.backend.entity.Student;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUserShouldRejectDisabledUsernameDuplicate() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("disabled-user");
        request.setPassword("secret123");
        request.setRealName("Dorm Admin");
        request.setRole("DORM_ADMIN");

        User existingUser = new User();
        existingUser.setId(8L);
        existingUser.setUsername("disabled-user");
        existingUser.setStatus(0);

        when(userService.findAnyByUsername("disabled-user")).thenReturn(existingUser);

        Result<Void> result = userController.createUser(request).getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(userService, never()).createUserWithOptionalStudent(request);
    }

    @Test
    void updateUserShouldBlockStudentRoleChange() {
        User existingUser = new User();
        existingUser.setId(3L);
        existingUser.setRole("STUDENT");

        User update = new User();
        update.setRole("COUNSELOR");

        when(userService.findById(3L)).thenReturn(existingUser);

        Result<Void> result = userController.updateUser(3L, update).getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(userService, never()).update(update);
    }

    @Test
    void deleteUserShouldBlockAssignedDormAdmin() {
        User existingUser = new User();
        existingUser.setId(5L);
        existingUser.setRole("DORM_ADMIN");

        Building building = new Building();
        building.setId(10L);
        building.setAdminId(5L);

        when(userService.findById(5L)).thenReturn(existingUser);
        when(studentService.findByUserId(5L)).thenReturn(null);
        when(buildingService.findByAdminId(5L)).thenReturn(building);

        Result<Void> result = userController.deleteUser(5L).getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(userService, never()).deleteById(5L);
    }

    @Test
    void deleteUserShouldBlockCurrentUser() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin");

        User targetUser = new User();
        targetUser.setId(1L);
        targetUser.setRole("SUPER_ADMIN");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "token")
        );

        when(userService.findByUsername("admin")).thenReturn(currentUser);
        when(userService.findById(1L)).thenReturn(targetUser);

        Result<Void> result = userController.deleteUser(1L).getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        verify(userService, never()).deleteById(1L);
    }
}
