package com.eatzy.auth.service;

import com.eatzy.auth.domain.Role;
import com.eatzy.auth.domain.User;
import com.eatzy.auth.event.UserStatusLocalEvent;
import com.eatzy.auth.mapper.UserMapper;
import com.eatzy.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // ==================== handleCreateUser ====================

    @Test
    void handleCreateUser_setsIsActiveToFalse() {
        User user = User.builder().email("test@example.com").password("secret").build();
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.handleCreateUser(user);

        assertThat(result.getIsActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void handleCreateUser_assignsRoleWhenRoleExists() {
        Role role = new Role();
        role.setId(1L);
        role.setName("CUSTOMER");

        User user = User.builder().email("test@example.com").password("secret").role(role).build();

        when(roleService.getRoleById(1L)).thenReturn(role);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.handleCreateUser(user);

        assertThat(result.getRole()).isEqualTo(role);
    }

    @Test
    void handleCreateUser_setsRoleNullWhenRoleNotFound() {
        Role role = new Role();
        role.setId(99L);

        User user = User.builder().email("test@example.com").password("secret").role(role).build();

        when(roleService.getRoleById(99L)).thenReturn(null);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.handleCreateUser(user);

        assertThat(result.getRole()).isNull();
    }

    // ==================== checkEmailExists ====================

    @Test
    void checkEmailExists_returnsTrueWhenEmailExists() {
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThat(userService.checkEmailExists("exists@example.com")).isTrue();
    }

    @Test
    void checkEmailExists_returnsFalseWhenEmailNotExists() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        assertThat(userService.checkEmailExists("new@example.com")).isFalse();
    }

    // ==================== getUserById ====================

    @Test
    void getUserById_returnsUserWhenFound() {
        User user = User.builder().build();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.getUserById(1L)).isEqualTo(user);
    }

    @Test
    void getUserById_returnsNullWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.getUserById(99L)).isNull();
    }

    // ==================== handleDeleteUser ====================

    @Test
    void handleDeleteUser_callsDeleteById() {
        userService.handleDeleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    // ==================== handleGetUserByUsername ====================

    @Test
    void handleGetUserByUsername_returnsUser() {
        User user = User.builder().email("user@example.com").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);

        assertThat(userService.handleGetUserByUsername("user@example.com")).isEqualTo(user);
    }

    // ==================== updateUserToken ====================

    @Test
    void updateUserToken_updatesTokenForExistingUser() {
        User user = User.builder().email("user@example.com").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);

        userService.updateUserToken("new-token", "user@example.com");

        assertThat(user.getRefreshToken()).isEqualTo("new-token");
        verify(userRepository).save(user);
    }

    @Test
    void updateUserToken_doesNothingWhenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(null);

        userService.updateUserToken("token", "ghost@example.com");

        verify(userRepository, never()).save(any());
    }

    // ==================== setUserActiveStatus ====================

    @Test
    void setUserActiveStatus_activatesUserAndPublishesEvent() {
        Role role = new Role();
        role.setName("CUSTOMER");

        User user = User.builder().build();
        user.setId(1L);
        user.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.setUserActiveStatus(1L, true);

        assertThat(result.getIsActive()).isTrue();
        verify(eventPublisher).publishEvent(any(UserStatusLocalEvent.class));
    }

    @Test
    void setUserActiveStatus_deactivatesUserClearsRefreshToken() {
        Role role = new Role();
        role.setName("DRIVER");

        User user = User.builder().build();
        user.setId(2L);
        user.setRefreshToken("old-token");
        user.setRole(role);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.setUserActiveStatus(2L, false);

        assertThat(result.getIsActive()).isFalse();
        assertThat(result.getRefreshToken()).isNull();
        verify(eventPublisher).publishEvent(any(UserStatusLocalEvent.class));
    }

    @Test
    void setUserActiveStatus_returnsNullWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        User result = userService.setUserActiveStatus(99L, true);

        assertThat(result).isNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ==================== handleUpdateUser ====================

    @Test
    void handleUpdateUser_updatesUserFields() {
        User existing = User.builder().email("user@example.com").build();
        existing.setId(1L);
        existing.setName("Old Name");

        User updated = User.builder().build();
        updated.setId(1L);
        updated.setName("New Name");
        updated.setAge(25);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.handleUpdateUser(updated);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getAge()).isEqualTo(25);
        verify(userRepository).save(existing);
    }

    @Test
    void handleUpdateUser_returnsNullWhenUserNotFound() {
        User updated = User.builder().build();
        updated.setId(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.handleUpdateUser(updated)).isNull();
        verify(userRepository, never()).save(any());
    }
}
