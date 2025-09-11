package com.gym.gym_management.service;

import com.gym.gym_management.authentication.RegisterRequest;
import com.gym.gym_management.controller.dto.UserDTO;
import com.gym.gym_management.model.Role;
import com.gym.gym_management.model.User;
import com.gym.gym_management.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private IUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        // Construimos explícitamente el SUT con los mocks para evitar problemas de inyección
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerUser_shouldSaveUser_whenEmailNotExistsAndRoleValid() {
        RegisterRequest request = new RegisterRequest("admin@email.com", "plain", "ADMIN");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hash");

        userService.registerUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(request.getEmail(), saved.getEmail());
        assertEquals("hash", saved.getPassword());
        assertEquals(Role.ADMIN, saved.getRole());
    }

    @Test
    void registerUser_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("admin@email.com", "plain", "ADMIN");
        User existing = new User();
        existing.setEmail(request.getEmail());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.registerUser(request));
        assertEquals("El usuario ya existe", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "AdMiN", "ADMIN"})
    void registerUser_shouldAcceptRoleCaseInsensitive(String roleStr) {
        RegisterRequest request = new RegisterRequest("admin@email.com", "plain", roleStr);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hash");

        userService.registerUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }

    @Test
    void registerUser_shouldThrow_whenRoleIsInvalid() {
        RegisterRequest request = new RegisterRequest("admin@email.com", "plain", "FOO");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.registerUser(request));
        assertEquals("El rol ingresado no es válido", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_shouldEncodePassword_beforeSaving() {
        RegisterRequest request = new RegisterRequest("admin@email.com", "plain", "ADMIN");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain")).thenReturn("hash");

        userService.registerUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hash", captor.getValue().getPassword());
        assertNotEquals("plain", captor.getValue().getPassword());
    }

    @Test
    void registerUser_shouldCallRepositoryInOrder_findThenSave() {
        RegisterRequest request = new RegisterRequest("admin@email.com", "plain", "ADMIN");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hash");

        userService.registerUser(request);

        InOrder inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findByEmail(request.getEmail());
        inOrder.verify(userRepository).save(any(User.class));
    }

    @Test
    void findAll_shouldReturnAllUsersFromRepository() {
        User u1 = new User(); u1.setEmail("a@a.com");
        User u2 = new User(); u2.setEmail("b@b.com");
        List<User> users = Arrays.asList(u1, u2);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();
        assertEquals(2, result.size());
        assertEquals("a@a.com", result.get(0).getEmail());
        assertEquals("b@b.com", result.get(1).getEmail());
    }

    @Test
    void findAllDTO_shouldMapFields_andHidePassword() {
        User u1 = new User(); u1.setId(1L); u1.setEmail("a@a.com"); u1.setRole(Role.ADMIN); u1.setPassword("secret");
        User u2 = new User(); u2.setId(2L); u2.setEmail("b@b.com"); u2.setRole(Role.ADMIN); u2.setPassword("secret2");
        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        List<UserDTO> dtos = userService.findAllDTO();
        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals("a@a.com", dtos.get(0).getEmail());
        assertEquals(Role.ADMIN, dtos.get(0).getRole());
        // No hay campo password en UserDTO
        assertFalse(dtos.get(0).getClass().getDeclaredFields().toString().contains("password"));
    }

    @Test
    void findAllDTO_shouldHandleEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        List<UserDTO> dtos = userService.findAllDTO();
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }
}
