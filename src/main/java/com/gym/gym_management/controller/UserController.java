package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.UserDTO;
import com.gym.gym_management.model.User;
import com.gym.gym_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Controlador REST para gestionar operaciones relacionadas con usuarios del sistema.
 *
 * Endpoints:
 * - GET /users → Lista todos los usuarios registrados.
 *
 * Relación con los requerimientos:
 * - "Usuarios y Roles":
 *   Permite consultar la lista de usuarios (administradores y clientes).
 * - "Panel de Administrador":
 *   Este endpoint puede ser útil para que un administrador vea todos los usuarios registrados.
 * - "Autenticación y Seguridad":
 *   Se puede restringir este endpoint para que solo los administradores tengan acceso.
 */

@RestController
@RequestMapping("/api/users") //Ruta base
public class UserController {

    // Servicio que encapsula la lógica de negocio de los usuarios.
    private final UserService userService;

    /**
     * Constructor que inyecta el servicio de usuarios.
     * @param userService servicio para la gestión de usuarios.
     */
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Obtiene la lista de todos los usuarios registrados en el sistema.
     * Acceso: solo usuarios con rol ADMIN.
     * @return ResponseEntity con la lista de usuarios (DTO) y estado 200 OK.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> findAll() {
        List<UserDTO> users = userService.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}
