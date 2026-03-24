package com.sdm.backend.controller;

import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Message;
import com.sdm.backend.entity.User;
import com.sdm.backend.service.MessageService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    private MessageController controller;

    @BeforeEach
    void setUp() {
        controller = new MessageController();
        ReflectionTestUtils.setField(controller, "messageService", messageService);
        ReflectionTestUtils.setField(controller, "userService", userService);

        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setUsername("student01");
        currentUser.setRole("STUDENT");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student01", null, List.of())
        );
        when(userService.findByUsername("student01")).thenReturn(currentUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMessageListReturnsEmptyPageWhenPaginationOverflows() {
        List<Message> messages = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            Message message = new Message();
            message.setId(i);
            message.setUserId(10L);
            messages.add(message);
        }
        when(messageService.findByUserId(10L)).thenReturn(messages);

        ResponseEntity<Result<Map<String, Object>>> response = controller.getMessageList(2, 10, null, null, null);

        assertEquals(200, response.getBody().getCode());
        assertEquals(3, response.getBody().getData().get("total"));
        assertTrue(((List<?>) response.getBody().getData().get("list")).isEmpty());
    }

    @Test
    void getMessageListUsesCombinedFiltersWhenAnyFilterExists() {
        when(messageService.findByUserIdWithFilters(10L, "UNREAD", "", null)).thenReturn(List.of());

        controller.getMessageList(1, 10, "UNREAD", "", null);

        verify(messageService).findByUserIdWithFilters(10L, "UNREAD", "", null);
        verify(messageService, never()).findByUserId(10L);
    }

    @Test
    void sendMessageAllowsMissingRelatedId() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 22L);
        params.put("title", "hello");
        params.put("content", "world");
        params.put("type", "SYSTEM");
        params.put("category", "REPLY");
        params.put("relatedType", "REPAIR");

        ResponseEntity<Result<Void>> response = controller.sendMessage(params);

        assertEquals(200, response.getBody().getCode());
        verify(messageService).sendBusinessNotification(
                eq(22L), eq(10L), eq("SYSTEM"), eq("REPLY"), eq("hello"), eq("world"), eq("REPAIR"), isNull()
        );
    }

    @Test
    void sendMessageRejectsBlankRelatedIdWithoutCrashing() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 22L);
        params.put("title", "hello");
        params.put("content", "world");
        params.put("type", "SYSTEM");
        params.put("relatedType", "REPAIR");
        params.put("relatedId", "");

        ResponseEntity<Result<Void>> response = controller.sendMessage(params);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        verify(messageService).sendBusinessNotification(
                eq(22L), eq(10L), eq("SYSTEM"), eq("SYSTEM"), eq("hello"), eq("world"), eq("REPAIR"), isNull()
        );
    }
}
