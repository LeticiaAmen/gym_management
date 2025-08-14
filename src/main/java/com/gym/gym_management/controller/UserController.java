package com.gym.gym_management.controller;

import com.gym.gym_management.model.User;
import com.gym.gym_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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
@RequestMapping("/users") //Ruta base
public class UserController {

    // Servicio que encapsula la lógica de negocio de los usuarios.
    private UserService userService;

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
     * Acceso: actualmente público para usuarios autenticados, pero puede restringirse
     * a rol USER si solo los administradores deben ver la lista.
     *
     * @return ResponseEntity con la lista de usuarios y estado 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<User>> findAll(){
        return ResponseEntity.ok(userService.findAll());
    }
}
