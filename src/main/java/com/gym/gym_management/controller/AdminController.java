package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.UserDTO;
import com.gym.gym_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión de usuarios administradores del sistema.
 * <p>
 * Este controlador expone endpoints para consultar la lista de usuarios con rol ADMIN.
 * Todos los endpoints están protegidos y solo son accesibles por usuarios autenticados con rol ADMIN.
 *
 * @author GymManagement
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param userService servicio para la gestión de usuarios
     */
    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Obtiene la lista de todos los administradores registrados en el sistema.
     * <p>
     * Este endpoint retorna todos los usuarios con rol ADMIN, representados mediante DTOs
     * que no exponen información sensible como contraseñas.
     *
     * @return lista de administradores en formato DTO
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllAdmins() {
        return ResponseEntity.ok(userService.findAllAdmins());
    }
}
