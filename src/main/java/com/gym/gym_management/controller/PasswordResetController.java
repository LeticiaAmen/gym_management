package com.gym.gym_management.controller;

import com.gym.gym_management.controller.dto.PasswordResetRequestDTO;
import com.gym.gym_management.controller.dto.PasswordResetConfirmDTO;
import com.gym.gym_management.service.PasswordResetService;
import com.gym.gym_management.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la gestión de recuperación de contraseñas.
 * <p>
 * Este controlador expone endpoints para solicitar la recuperación de contraseña
 * y para confirmar el cambio de contraseña mediante un token de validación.
 *
 * @author GymManagement
 * @version 1.0
 */
@RestController
@RequestMapping("/api/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RateLimitService rateLimitService; // Nuevo servicio para rate limiting

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService, RateLimitService rateLimitService) {
        this.passwordResetService = passwordResetService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Inicia el proceso de recuperación de contraseña.
     * <p>
     * Este endpoint recibe el correo electrónico del usuario y envía un email
     * con instrucciones para restablecer la contraseña si el correo existe.
     *
     * @param requestDTO objeto que contiene el email del usuario
     * @return respuesta indicando que se ha iniciado el proceso si el email existe
     */
    @PostMapping("/request-reset")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO requestDTO, HttpServletRequest httpRequest) {
        String email = requestDTO.getEmail();
        String ip = httpRequest.getRemoteAddr();
        // verificar rate limit antes de procesar
        rateLimitService.assertPasswordResetAllowed(email, ip);
        passwordResetService.initiatePasswordReset(email);

        // registrar siempre la solicitud (exista o no el mail) para prevenir enumeración
        rateLimitService.registerPasswordResetRequest(email, ip);

        // Por seguridad, siempre retornamos un mensaje exitoso aunque el email no exista
        return ResponseEntity.ok("Si el correo existe, recibirás un mensaje con instrucciones para recuperar tu contraseña.");
    }

    /**
     * Valida un token de recuperación de contraseña.
     * <p>
     * Este endpoint verifica si el token proporcionado es válido y no ha expirado.
     *
     * @param token token de recuperación a validar
     * @return respuesta indicando si el token es válido
     */
    @GetMapping("/validate-token/{token}")
    public ResponseEntity<Boolean> validateToken(@PathVariable String token) {
        String email = passwordResetService.validateToken(token);
        return ResponseEntity.ok(email != null);
    }

    /**
     * Completa el proceso de recuperación de contraseña.
     * <p>
     * Este endpoint recibe el token de recuperación y la nueva contraseña,
     * y actualiza la contraseña del usuario si el token es válido.
     *
     * @param confirmDTO objeto que contiene el token y la nueva contraseña
     * @return respuesta indicando si el cambio fue exitoso
     */
    @PostMapping("/confirm-reset")
    public ResponseEntity<String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO confirmDTO) {
        boolean success = passwordResetService.resetPassword(
            confirmDTO.getToken(),
            confirmDTO.getNewPassword()
        );

        if (success) {
            return ResponseEntity.ok("Contraseña actualizada correctamente.");
        } else {
            return ResponseEntity.badRequest().body("El token es inválido o ha expirado. Por favor, solicita nuevamente la recuperación de contraseña.");
        }
    }
}
