package com.sdm.backend;

import com.sdm.backend.controller.AuthController;
import com.sdm.backend.dto.LoginRequest;
import com.sdm.backend.dto.LoginResponse;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.LoginLog;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.LoginLogService;
import com.sdm.backend.service.UserService;
import com.sdm.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LoginLogService loginLogService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginShouldReturnTokenAndSaveSuccessLog() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("secret123");

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setRealName("Admin");
        user.setRole("SUPER_ADMIN");

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(userService.findByUsername("admin")).thenReturn(user);
        when(userService.checkPassword("secret123", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken("admin")).thenReturn("jwt-token");

        Result<LoginResponse> result = authController.login(loginRequest, request).getBody();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("jwt-token", result.getData().getToken());

        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogService).save(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getStatus());
        assertEquals("admin", captor.getValue().getUsername());
    }

    @Test
    void loginShouldSaveFailureLogWhenPasswordIsWrong() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrong");

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setRealName("Admin");
        user.setRole("SUPER_ADMIN");

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(userService.findByUsername("admin")).thenReturn(user);
        when(userService.checkPassword("wrong", "encoded")).thenReturn(false);

        Result<LoginResponse> result = authController.login(loginRequest, request).getBody();

        assertNotNull(result);
        assertEquals(401, result.getCode());

        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogService).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
        assertTrue(captor.getValue().getMessage() != null && !captor.getValue().getMessage().isEmpty());
    }
}
