package com.gym.gym_management.service;

import com.gym.gym_management.authentication.RegisterRequest;
import com.gym.gym_management.controller.dto.UserDTO;
import com.gym.gym_management.model.Role;
import com.gym.gym_management.model.User;
import com.gym.gym_management.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio que gestiona la lógica de negocio relacionada con usuarios del sistema.
 *
 * Funciones principales:
 * - Registrar nuevos usuarios con validaciones y cifrado de contraseña.
 * - Consultar todos los usuarios registrados.
 * - Filtrar usuarios por rol ADMIN.
 *
 * Relación con los requerimientos:
 * - "Usuarios y Roles": este servicio permite registrar usuarios con roles específicos
 *   (USER para administradores, CLIENT para clientes).
 * - "Autenticación y Seguridad":
 *   - Cifra las contraseñas con BCrypt antes de guardarlas.
 *   - Valida que no se registren correos duplicados.
 */
@Service
public class UserService {

    // Repositorio para acceder a los datos de usuarios en la base de datos.
    private final IUserRepository userRepository;

    // Codificador de contraseñas para almacenar passwords de forma segura.
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor con inyección de dependencias.
     */
    @Autowired
    public UserService(IUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * Pasos:
     * 1. Verifica si ya existe un usuario con el mismo email.
     * 2. Crea un nuevo objeto User.
     * 3. Cifra la contraseña antes de guardarla.
     * 4. Convierte el rol a mayúsculas y lo valida contra el enum Role.
     * 5. Guarda el usuario en la base de datos.
     *
     * @param request datos del usuario a registrar (email, password, rol).
     * @throws RuntimeException si el usuario ya existe o el rol es inválido.
     */
    public void registerUser(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if(existingUser.isPresent()){
            throw new RuntimeException("El usuario ya existe");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Role role;
        try {
            // Convertimos el String a mayúsculas para que coincida con los nombres del enum
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("El rol ingresado no es válido");
        }
        user.setRole(role);
        userRepository.save(user);
    }

    //obtiene todos los usuarios registrados en el sistema
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Nuevo: devuelve usuarios como DTO (sin password)
    public List<UserDTO> findAllDTO() {
        return userRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene todos los usuarios con rol ADMIN del sistema.
     * <p>
     * Este método filtra la lista completa de usuarios para devolver solo aquellos
     * que tienen el rol ADMIN, transformándolos a DTOs para evitar exponer
     * información sensible como contraseñas.
     *
     * @return lista de administradores en formato DTO
     */
    public List<UserDTO> findAllAdmins() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }

    /**
     * Cambia la contraseña de un usuario administrador.
     * <p>
     * Este método valida que la contraseña actual sea correcta antes de realizar el cambio,
     * y comprueba que las nuevas contraseñas coincidan.
     *
     * @param email correo electrónico del usuario
     * @param currentPassword contraseña actual para validación
     * @param newPassword nueva contraseña a establecer
     * @param confirmPassword confirmación de la nueva contraseña
     * @throws IllegalArgumentException si las contraseñas no coinciden o la contraseña actual es incorrecta
     * @throws RuntimeException si el usuario no existe
     */
    public void changePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
        // Verificar que las nuevas contraseñas coincidan
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // Buscar el usuario por email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        // Cambiar la contraseña
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
