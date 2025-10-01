package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.ChangePasswordDTO;
import com.gym.gym_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controlador para gestionar las operaciones relacionadas con usuarios del sistema.
 * <p>
 * Este controlador expone endpoints para consultar la información de usuarios
 * y realizar operaciones como cambiar la contraseña.
 *
 * @author GymManagement
 * @version 1.0
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * <p>
     * Este endpoint permite a un usuario cambiar su propia contraseña.
     * Verifica que la contraseña actual sea correcta antes de realizar el cambio.
     *
     * @param changePasswordDTO datos para el cambio de contraseña (contraseña actual y nueva)
     * @param authentication objeto que contiene la información del usuario autenticado
     * @return respuesta vacía con código 200 si el cambio fue exitoso
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO,
            Authentication authentication) {

        try {
            // Obtenemos el email del usuario autenticado
            String userEmail = authentication.getName();

            // Llamamos al servicio para cambiar la contraseña
            userService.changePassword(
                userEmail,
                changePasswordDTO.getCurrentPassword(),
                changePasswordDTO.getNewPassword(),
                changePasswordDTO.getConfirmPassword()
            );

            return ResponseEntity.ok("Contraseña cambiada exitosamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Error al cambiar la contraseña: " + e.getMessage());
        }
    }
}
