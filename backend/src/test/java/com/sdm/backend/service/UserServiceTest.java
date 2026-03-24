package com.sdm.backend.service;

import com.sdm.backend.entity.User;
import com.sdm.backend.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldFilterInactiveAndCurrentUserFromMessagingDirectory() {
        User currentUser = buildUser(1L, "self", 1, "secret");
        User activeUser = buildUser(2L, "alice", 1, "secret");
        User inactiveUser = buildUser(3L, "bob", 0, "secret");
        when(userMapper.findAll()).thenReturn(List.of(currentUser, activeUser, inactiveUser));

        List<User> result = userService.findMessagingDirectory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(0).getPassword()).isNull();
    }

    private User buildUser(Long id, String username, Integer status, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setStatus(status);
        user.setPassword(password);
        return user;
    }
}
