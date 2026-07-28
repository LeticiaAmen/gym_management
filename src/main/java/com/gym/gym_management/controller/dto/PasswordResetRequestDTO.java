package com.gym.gym_management.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la solicitud de restablecimiento de contraseña.
 * <p>
 * Contiene el correo electrónico del usuario que solicita restablecer su contraseña.
 *
 * @author GymManagement
 * @version 1.0
 */
/** DTO que inicia la recuperación a partir de un correo electrónico. */
public class PasswordResetRequestDTO {

    // @NotBlank y @Email validan el correo antes de iniciar el flujo.
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
